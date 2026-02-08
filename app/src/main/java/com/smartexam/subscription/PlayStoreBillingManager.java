package com.smartexam.subscription;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Play Store Billing integration with offline support and robust error handling.
 * Manages subscription purchases, trial conversions, and offline queuing.
 */
public class PlayStoreBillingManager {
    
    private static final String TAG = "PlayStoreBillingManager";
    
    // Subscription product IDs
    public static final String MONTHLY_SUBSCRIPTION = "smartexam_monthly";
    public static final String YEARLY_SUBSCRIPTION = "smartexam_yearly";
    
    // Billing client
    private BillingClient billingClient;
    private final Context context;
    private final FirebaseFirestore firestore;
    private final TrialStateManager trialStateManager;
    private final Map<String, ProductDetails> productDetailsCache = new HashMap<>();
    
    // Callbacks
    private BillingConnectionCallback connectionCallback;
    private PurchaseCallback purchaseCallback;
    
    // Offline queue
    private final List<PurchaseOperation> offlineQueue = new ArrayList<>();
    
    public PlayStoreBillingManager(Context context) {
        this.context = context.getApplicationContext();
        this.firestore = FirebaseFirestore.getInstance();
        this.trialStateManager = TrialStateManager.getInstance(context);
        
        initializeBillingClient();
    }
    
    /**
     * Initializes billing client
     */
    private void initializeBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener((billingResult, purchases) -> {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (Purchase purchase : purchases) {
                        handlePurchase(purchase);
                    }
                }
            })
            .enablePendingPurchases()
            .build();
        
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected");
                // Attempt to reconnect
                initializeBillingClient();
            }
            
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                int responseCode = billingResult.getResponseCode();
                String debugMessage = billingResult.getDebugMessage();
                
                if (responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "Billing setup successful");
                    
                    // Process offline queue
                    processOfflineQueue();
                    
                    // Query existing purchases
                    queryExistingPurchases();
                    queryProductDetails();
                    
                    if (connectionCallback != null) {
                        connectionCallback.onBillingSetupFinished(true);
                    }
                } else {
                    Log.e(TAG, "Billing setup failed: " + responseCode + " - " + debugMessage);
                    if (connectionCallback != null) {
                        connectionCallback.onBillingSetupFinished(false);
                    }
                }
            }
        });
    }
    
    /**
     * Sets connection callback
     */
    public void setConnectionCallback(@NonNull BillingConnectionCallback callback) {
        this.connectionCallback = callback;
    }
    
    /**
     * Sets purchase callback
     */
    public void setPurchaseCallback(@NonNull PurchaseCallback callback) {
        this.purchaseCallback = callback;
    }
    
    private void queryProductDetails() {
        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(QueryProductDetailsParams.Product.newBuilder()
            .setProductId(MONTHLY_SUBSCRIPTION)
            .setProductType(BillingClient.ProductType.SUBS)
            .build());
        productList.add(QueryProductDetailsParams.Product.newBuilder()
            .setProductId(YEARLY_SUBSCRIPTION)
            .setProductType(BillingClient.ProductType.SUBS)
            .build());

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && productDetailsList != null) {
                for (ProductDetails productDetails : productDetailsList) {
                    productDetailsCache.put(productDetails.getProductId(), productDetails);
                }
            }
        });
    }

    /**
     * Queries subscription details
     */
    public void querySubscriptionDetails(@NonNull String sku, @NonNull SubscriptionDetailsCallback callback) {
        ProductDetails productDetails = productDetailsCache.get(sku);
        if (productDetails != null) {
            callback.onDetailsReceived(productDetails);
        } else {
            callback.onError("Product details not found");
        }
    }
    
    /**
     * Launches subscription purchase flow
     */
    public void launchSubscriptionPurchase(@NonNull Activity activity, @NonNull String sku, 
                                         @Nullable String trialToken, @NonNull PurchaseCallback callback) {
        this.purchaseCallback = callback;

        ProductDetails productDetails = productDetailsCache.get(sku);
        if (productDetails == null) {
            callback.onPurchaseFailed("Product not available for purchase");
            return;
        }

        BillingFlowParams.ProductDetailsParams productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(productDetails.getSubscriptionOfferDetails().get(0).getOfferToken())
            .build();

        BillingFlowParams.Builder builder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(Collections.singletonList(productDetailsParams));
        
        BillingFlowParams billingFlowParams = builder.build();
        
        billingClient.launchBillingFlow(activity, billingFlowParams);
    }
    
    /**
     * Handles purchase result
     */
    public void handlePurchaseResult(@NonNull BillingResult billingResult, 
                                   @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else {
            Log.e(TAG, "Purchase failed: " + billingResult.getDebugMessage());
            if (purchaseCallback != null) {
                purchaseCallback.onPurchaseFailed(billingResult.getDebugMessage());
            }
        }
    }
    
    /**
     * Handles individual purchase
     */
    private void handlePurchase(@NonNull Purchase purchase) {
        Log.i(TAG, "Handling purchase: " + purchase.getOrderId() + " - " + purchase.getProducts());
        
        // Verify purchase
        if (!verifyPurchase(purchase)) {
            Log.e(TAG, "Purchase verification failed");
            if (purchaseCallback != null) {
                purchaseCallback.onPurchaseFailed("Purchase verification failed");
            }
            return;
        }
        
        // Acknowledge purchase
        acknowledgePurchase(purchase);
        
        // Process subscription
        processSubscription(purchase);
        
        // Notify callback
        if (purchaseCallback != null) {
            purchaseCallback.onPurchaseSuccess(purchase);
        }
    }
    
    /**
     * Verifies purchase
     */
    private boolean verifyPurchase(@NonNull Purchase purchase) {
        // Basic verification checks
        if (purchase.getPurchaseToken() == null || purchase.getPurchaseToken().isEmpty()) {
            return false;
        }
        
        if (purchase.getProducts() == null || purchase.getProducts().isEmpty()) {
            return false;
        }
        
        // Verify signature (in production, implement server-side verification)
        // For now, accept all purchases
        
        return true;
    }
    
    /**
     * Acknowledges purchase
     */
    private void acknowledgePurchase(@NonNull Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            AcknowledgePurchaseParams acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();
            
            billingClient.acknowledgePurchase(acknowledgeParams, billingResult -> {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "Purchase acknowledged successfully");
                } else {
                    Log.e(TAG, "Failed to acknowledge purchase: " + billingResult.getDebugMessage());
                }
            });
        }
    }
    
    /**
     * Processes subscription
     */
    private void processSubscription(@NonNull Purchase purchase) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No authenticated user for subscription processing");
            return;
        }
        
        String sku = purchase.getProducts().get(0);
        String purchaseToken = purchase.getPurchaseToken();
        long purchaseTime = purchase.getPurchaseTime();
        
        // Create subscription record
        Map<String, Object> subscriptionData = new HashMap<>();
        subscriptionData.put("user_id", currentUser.getUid());
        subscriptionData.put("sku", sku);
        subscriptionData.put("purchase_token", purchaseToken);
        subscriptionData.put("purchase_time", purchaseTime);
        subscriptionData.put("order_id", purchase.getOrderId());
        subscriptionData.put("is_auto_renewing", purchase.isAutoRenewing());
        subscriptionData.put("status", "active");
        subscriptionData.put("created_at", System.currentTimeMillis());
        
        // Store in Firestore
        firestore.collection("subscriptions")
            .document(currentUser.getUid())
            .set(subscriptionData)
            .addOnSuccessListener(aVoid -> {
                Log.i(TAG, "Subscription stored successfully");
                
                // Convert trial if active
                convertTrialToSubscription();
                
                // Update local state
                updateLocalSubscriptionState(sku, true);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to store subscription", e);
                
                // Queue for offline processing
                queueOfflineOperation(new PurchaseOperation(
                    PurchaseOperation.Type.STORE_SUBSCRIPTION,
                    subscriptionData
                ));
            });
    }
    
    /**
     * Converts trial to subscription
     */
    private void convertTrialToSubscription() {
        trialStateManager.getTrialState(new TrialStateManager.TrialStateCallback() {
            @Override
            public void onStateReceived(TrialStateManager.TrialStateData state) {
                if (state.state == TrialStateManager.TrialState.ACTIVE) {
                    // Convert trial to subscription
                    TrialStateManager.TrialStateData convertedState = new TrialStateManager.TrialStateData(
                        TrialStateManager.TrialState.CONVERTED,
                        state.trialStart,
                        state.trialEnd,
                        System.currentTimeMillis(),
                        state.deviceHash,
                        true,
                        state.metadata
                    );
                    
                    trialStateManager.updateTrialState(convertedState, new TrialStateManager.TrialStateCallback() {
                        @Override
                        public void onStateReceived(TrialStateManager.TrialStateData state) {
                            Log.i(TAG, "Trial converted to subscription");
                        }
                        
                        @Override
                        public void onError(String errorMessage) {
                            Log.e(TAG, "Failed to convert trial: " + errorMessage);
                        }
                    });
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Failed to get trial state for conversion: " + errorMessage);
            }
        });
    }
    
    /**
     * Queries existing purchases
     */
    private void queryExistingPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            new PurchasesResponseListener() {
                @Override
                public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, 
                                                    @NonNull List<Purchase> purchasesList) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        for (Purchase purchase : purchasesList) {
                            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                handlePurchase(purchase);
                            }
                        }
                    }
                }
            });
    }
    
    /**
     * Processes offline queue
     */
    private void processOfflineQueue() {
        if (offlineQueue.isEmpty()) {
            return;
        }
        
        Log.i(TAG, "Processing offline queue: " + offlineQueue.size() + " operations");
        
        // Process operations in order
        for (PurchaseOperation operation : offlineQueue) {
            switch (operation.type) {
                case STORE_SUBSCRIPTION:
                    processStoredSubscription(operation.data);
                    break;
                case ACKNOWLEDGE_PURCHASE:
                    processAcknowledgement(operation.data);
                    break;
            }
        }
        
        // Clear processed operations
        offlineQueue.clear();
    }
    
    /**
     * Queues operation for offline processing
     */
    private void queueOfflineOperation(@NonNull PurchaseOperation operation) {
        offlineQueue.add(operation);
        Log.i(TAG, "Queued offline operation: " + operation.type);
    }
    
    /**
     * Updates local subscription state
     */
    private void updateLocalSubscriptionState(@NonNull String sku, boolean isActive) {
        // Store subscription state locally for offline access
        android.content.SharedPreferences prefs = context.getSharedPreferences("subscription", Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putString("current_sku", sku);
        editor.putBoolean("is_active", isActive);
        editor.putLong("last_check", System.currentTimeMillis());
        editor.apply();
    }
    
    /**
     * Gets current subscription status
     */
    public void getSubscriptionStatus(@NonNull SubscriptionStatusCallback callback) {
        // Check local state first
        android.content.SharedPreferences prefs = context.getSharedPreferences("subscription", Context.MODE_PRIVATE);
        String currentSku = prefs.getString("current_sku", null);
        boolean isActive = prefs.getBoolean("is_active", false);
        
        if (currentSku != null) {
            callback.onStatusReceived(currentSku, isActive);
        } else {
            callback.onStatusReceived(null, false);
        }
    }
    
    /**
     * Gets user ID for billing
     */
    private String getUserId() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null ? currentUser.getUid() : "anonymous";
    }
    
    
    /**
     * Processes stored subscription from offline queue
     */
    private void processStoredSubscription(@NonNull Map<String, Object> data) {
        firestore.collection("subscriptions")
            .document((String) data.get("user_id"))
            .set(data)
            .addOnSuccessListener(aVoid -> Log.i(TAG, "Offline subscription stored"))
            .addOnFailureListener(e -> Log.e(TAG, "Failed to store offline subscription", e));
    }
    
    /**
     * Processes acknowledgement from offline queue
     */
    private void processAcknowledgement(@NonNull Map<String, Object> data) {
        String purchaseToken = (String) data.get("purchase_token");
        if (purchaseToken != null) {
            AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build();
            
            billingClient.acknowledgePurchase(params, billingResult -> {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "Offline purchase acknowledged");
                }
            });
        }
    }
    
    /**
     * Disconnects billing client
     */
    public void disconnect() {
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }
    
    /**
     * Purchase operation for offline queue
     */
    private static class PurchaseOperation {
        enum Type {
            STORE_SUBSCRIPTION,
            ACKNOWLEDGE_PURCHASE
        }
        
        final Type type;
        final Map<String, Object> data;
        
        PurchaseOperation(Type type, Map<String, Object> data) {
            this.type = type;
            this.data = data;
        }
    }
    
    /**
     * Callback interfaces
     */
    public interface BillingConnectionCallback {
        void onBillingSetupFinished(boolean success);
    }
    
    public interface PurchaseCallback {
        void onPurchaseSuccess(Purchase purchase);
        void onPurchaseFailed(String errorMessage);
    }
    
    public interface SubscriptionDetailsCallback {
        void onDetailsReceived(ProductDetails details);
        void onError(String errorMessage);
    }
    
    public interface SubscriptionStatusCallback {
        void onStatusReceived(String sku, boolean isActive);
    }
}
