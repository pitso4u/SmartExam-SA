package com.smartexam.subscription;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.smartexam.models.UserSubscription;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Manages subscription state by reading from Firebase.
 * WinDev never decides subscription status - Firebase does.
 */
public class SubscriptionManager {
    private static final String TAG = "SubscriptionManager";
    private static final String USERS_COLLECTION = "users";
    private static final long CACHE_DURATION_MS = TimeUnit.HOURS.toMillis(1); // Cache for 1 hour
    
    private static SubscriptionManager instance;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    
    private UserSubscription cachedSubscription;
    private long cacheTimestamp = 0;
    private SubscriptionListener listener;
    private ListenerRegistration firestoreListener;

    public interface SubscriptionListener {
        void onSubscriptionChanged(UserSubscription subscription);
        void onError(String error);
    }

    private SubscriptionManager() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized SubscriptionManager getInstance() {
        if (instance == null) {
            instance = new SubscriptionManager();
        }
        return instance;
    }

    /**
     * Get current subscription state with caching
     */
    public void getSubscription(SubscriptionListener callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("User not authenticated");
            return;
        }

        // Check cache first
        if (isCacheValid()) {
            Log.d(TAG, "Returning cached subscription data");
            callback.onSubscriptionChanged(cachedSubscription);
            return;
        }

        DocumentReference userRef = db.collection(USERS_COLLECTION).document(user.getUid());
        userRef.get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    UserSubscription subscription = documentSnapshot.toObject(UserSubscription.class);
                    updateCache(subscription);
                    callback.onSubscriptionChanged(subscription);
                } else {
                    // User document doesn't exist, create trial user
                    createTrialUser(callback);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to fetch subscription", e);
                callback.onError("Failed to fetch subscription: " + e.getMessage());
            });
    }

    /**
     * Listen for real-time subscription updates
     */
    public void listenForSubscriptionUpdates(SubscriptionListener callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("User not authenticated");
            return;
        }

        // Remove existing listener
        if (firestoreListener != null) {
            firestoreListener.remove();
        }

        this.listener = callback;
        DocumentReference userRef = db.collection(USERS_COLLECTION).document(user.getUid());
        
        firestoreListener = userRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Listen failed", e);
                callback.onError("Listen failed: " + e.getMessage());
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                UserSubscription subscription = documentSnapshot.toObject(UserSubscription.class);
                updateCache(subscription);
                callback.onSubscriptionChanged(subscription);
            }
        });
    }

    /**
     * Stop listening for subscription updates
     */
    public void stopListening() {
        if (firestoreListener != null) {
            firestoreListener.remove();
            firestoreListener = null;
        }
        listener = null;
    }

    /**
     * Create trial user in Firebase
     */
    private void createTrialUser(SubscriptionListener callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("User not authenticated");
            return;
        }

        UserSubscription newSubscription = new UserSubscription();
        newSubscription.setEmail(user.getEmail());
        newSubscription.setRole("teacher");
        newSubscription.setTrialStartDate(getCurrentISOTime());
        newSubscription.setCreatedAt(getCurrentISOTime());
        
        UserSubscription.SubscriptionInfo subInfo = new UserSubscription.SubscriptionInfo("trial");
        newSubscription.setSubscription(subInfo);

        DocumentReference userRef = db.collection(USERS_COLLECTION).document(user.getUid());
        userRef.set(newSubscription)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Trial user created successfully");
                updateCache(newSubscription);
                callback.onSubscriptionChanged(newSubscription);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to create trial user", e);
                callback.onError("Failed to create trial user: " + e.getMessage());
            });
    }

    /**
     * Check if cached data is still valid
     */
    private boolean isCacheValid() {
        return cachedSubscription != null && 
               (System.currentTimeMillis() - cacheTimestamp) < CACHE_DURATION_MS;
    }

    /**
     * Update cached subscription data
     */
    private void updateCache(UserSubscription subscription) {
        this.cachedSubscription = subscription;
        this.cacheTimestamp = System.currentTimeMillis();
    }

    /**
     * Get current ISO timestamp
     */
    private String getCurrentISOTime() {
        return java.time.Instant.now().toString();
    }

    /**
     * Clear cache (useful for testing or forced refresh)
     */
    public void clearCache() {
        cachedSubscription = null;
        cacheTimestamp = 0;
    }

    /**
     * Get cached subscription synchronously (may be null)
     */
    public UserSubscription getCachedSubscription() {
        return isCacheValid() ? cachedSubscription : null;
    }

    /**
     * Check if user can print without watermark
     * In free mode, always return true until 100+ users
     */
    public boolean canPrintClean() {
        // Free mode: no watermark restrictions
        return true;
    }

    /**
     * Check if user is on trial
     */
    public boolean isOnTrial() {
        UserSubscription sub = getCachedSubscription();
        return sub != null && sub.isValidTrial();
    }
}
