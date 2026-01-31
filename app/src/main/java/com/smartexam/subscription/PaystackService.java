package com.smartexam.subscription;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Service for Paystack integration - checkout URL generation and payment processing.
 * This handles the WinDev → Paystack flow via Firebase Functions.
 */
public class PaystackService {
    private static final String TAG = "PaystackService";
    private static final String FUNCTION_CREATE_CHECKOUT = "createPaystackCheckout";
    private static final String FUNCTION_VERIFY_PAYMENT = "verifyPaystackPayment";
    
    private static PaystackService instance;
    private FirebaseFunctions functions;
    private FirebaseAuth auth;

    public interface CheckoutCallback {
        void onSuccess(String authorizationUrl);
        void onError(String error);
    }

    public interface VerificationCallback {
        void onSuccess(boolean verified);
        void onError(String error);
    }

    private PaystackService() {
        functions = FirebaseFunctions.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized PaystackService getInstance() {
        if (instance == null) {
            instance = new PaystackService();
        }
        return instance;
    }

    /**
     * Create Paystack checkout URL for subscription purchase
     */
    public void createCheckoutUrl(String email, CheckoutCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("User not authenticated");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUid());
        data.put("email", email != null ? email : user.getEmail());
        data.put("amount", 5000); // R50 in cents
        data.put("currency", "ZAR");
        data.put("plan", "monthly_teacher"); // Plan identifier

        functions
            .getHttpsCallable(FUNCTION_CREATE_CHECKOUT)
            .call(data)
            .addOnSuccessListener(task -> {
                try {
                    Map<String, Object> result = (Map<String, Object>) task.getData();
                    String authorizationUrl = (String) result.get("authorizationUrl");
                    if (authorizationUrl != null) {
                        Log.d(TAG, "Checkout URL created successfully");
                        callback.onSuccess(authorizationUrl);
                    } else {
                        callback.onError("Invalid response from server");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse checkout response", e);
                    callback.onError("Failed to parse response: " + e.getMessage());
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to create checkout URL", e);
                callback.onError("Failed to create checkout: " + e.getMessage());
            });
    }

    /**
     * Verify payment status with Paystack
     */
    public void verifyPayment(String reference, VerificationCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("reference", reference);

        functions
            .getHttpsCallable(FUNCTION_VERIFY_PAYMENT)
            .call(data)
            .addOnSuccessListener(task -> {
                try {
                    Map<String, Object> result = (Map<String, Object>) task.getData();
                    Boolean verified = (Boolean) result.get("verified");
                    if (verified != null) {
                        Log.d(TAG, "Payment verification completed");
                        callback.onSuccess(verified);
                    } else {
                        callback.onError("Invalid verification response");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse verification response", e);
                    callback.onError("Failed to parse verification: " + e.getMessage());
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to verify payment", e);
                callback.onError("Failed to verify payment: " + e.getMessage());
            });
    }

    /**
     * Synchronous version for testing purposes
     */
    public String createCheckoutUrlSync(String email) {
        try {
            FirebaseUser user = auth.getCurrentUser();
            if (user == null) {
                throw new RuntimeException("User not authenticated");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("userId", user.getUid());
            data.put("email", email != null ? email : user.getEmail());
            data.put("amount", 5000);
            data.put("currency", "ZAR");
            data.put("plan", "monthly_teacher");

            Task<HttpsCallableResult> task = functions
                .getHttpsCallable(FUNCTION_CREATE_CHECKOUT)
                .call(data);

            HttpsCallableResult result = Tasks.await(task);
            Map<String, Object> resultMap = (Map<String, Object>) result.getData();
            return (String) resultMap.get("authorizationUrl");

        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Sync checkout creation failed", e);
            throw new RuntimeException("Failed to create checkout: " + e.getMessage());
        }
    }

    /**
     * Get current user ID for Paystack integration
     */
    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    /**
     * Get current user email for Paystack integration
     */
    public String getCurrentUserEmail() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getEmail() : null;
    }
}
