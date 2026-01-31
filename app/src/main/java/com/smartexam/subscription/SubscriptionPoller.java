package com.smartexam.subscription;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Handles subscription status polling after payment.
 * Polls Firebase every few seconds for up to 30 seconds to verify subscription activation.
 */
public class SubscriptionPoller {
    private static final String TAG = "SubscriptionPoller";
    private static final long POLL_INTERVAL_MS = 3000; // 3 seconds
    private static final long MAX_POLL_DURATION_MS = 30000; // 30 seconds
    private static final int MAX_ATTEMPTS = (int) (MAX_POLL_DURATION_MS / POLL_INTERVAL_MS);

    private Handler handler;
    private SubscriptionManager subscriptionManager;
    private boolean isPolling = false;
    private int pollAttempts = 0;

    public interface PollingCallback {
        void onSubscriptionActivated();
        void onPollingTimeout();
        void onError(String error);
    }

    public SubscriptionPoller() {
        handler = new Handler(Looper.getMainLooper());
        subscriptionManager = SubscriptionManager.getInstance();
    }

    /**
     * Start polling for subscription activation
     */
    public void startPolling(PollingCallback callback) {
        if (isPolling) {
            Log.w(TAG, "Polling already in progress");
            return;
        }

        Log.d(TAG, "Starting subscription status polling");
        isPolling = true;
        pollAttempts = 0;

        pollSubscriptionStatus(callback);
    }

    /**
     * Stop polling
     */
    public void stopPolling() {
        isPolling = false;
        handler.removeCallbacksAndMessages(null);
        Log.d(TAG, "Subscription polling stopped");
    }

    /**
     * Poll subscription status recursively
     */
    private void pollSubscriptionStatus(PollingCallback callback) {
        if (!isPolling) {
            return;
        }

        pollAttempts++;
        Log.d(TAG, "Polling attempt " + pollAttempts + "/" + MAX_ATTEMPTS);

        subscriptionManager.getSubscription(new SubscriptionManager.SubscriptionListener() {
            @Override
            public void onSubscriptionChanged(com.smartexam.models.UserSubscription subscription) {
                if (subscription != null && subscription.hasActiveSubscription()) {
                    Log.d(TAG, "Subscription activated successfully");
                    isPolling = false;
                    callback.onSubscriptionActivated();
                } else if (pollAttempts >= MAX_ATTEMPTS) {
                    Log.w(TAG, "Polling timeout - subscription not activated");
                    isPolling = false;
                    callback.onPollingTimeout();
                } else {
                    // Continue polling
                    handler.postDelayed(() -> pollSubscriptionStatus(callback), POLL_INTERVAL_MS);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error during polling: " + error);
                if (pollAttempts >= MAX_ATTEMPTS) {
                    isPolling = false;
                    callback.onError("Polling failed: " + error);
                } else {
                    // Continue polling on error
                    handler.postDelayed(() -> pollSubscriptionStatus(callback), POLL_INTERVAL_MS);
                }
            }
        });
    }

    /**
     * Check if currently polling
     */
    public boolean isPolling() {
        return isPolling;
    }

    /**
     * Get current poll attempt count
     */
    public int getPollAttempts() {
        return pollAttempts;
    }
}
