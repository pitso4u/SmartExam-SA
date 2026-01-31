package com.smartexam.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import com.smartexam.R;
import com.smartexam.subscription.TrialManager;

public class SplashActivity extends AppCompatActivity {

    private static final long MIN_SPLASH_DURATION = 1000; // 1 second minimum for cinematic effect
    private TrialManager trialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Handle splash screen transition for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
            
            // Keep the splash screen visible for a minimum duration to show the cinematic effect
            splashScreen.setKeepOnScreenCondition(() -> !isReadyToStart());
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        trialManager = TrialManager.getInstance(this);

        // Initialize Firebase Production Tools
        com.smartexam.config.RemoteConfigManager.getInstance().fetchAndActivate();
        com.google.firebase.analytics.FirebaseAnalytics.getInstance(this)
                .logEvent(com.google.firebase.analytics.FirebaseAnalytics.Event.APP_OPEN, null);

        // Check trial status and route appropriately after minimum duration
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            checkTrialAndRoute();
        }, MIN_SPLASH_DURATION);
    }

    /**
     * Checks trial status and routes to appropriate screen
     */
    private void checkTrialAndRoute() {
        trialManager.getTrialStatus(new TrialManager.TrialStatusCallback() {
            @Override
            public void onStatusReceived(TrialManager.TrialStatus status) {
                runOnUiThread(() -> {
                    if (status.trialStart == 0) {
                        // No trial found - go to terms acceptance
                        navigateToTermsAcceptance();
                    } else if (!status.isActive && status.isExpired()) {
                        // Trial expired - go to subscription
                        navigateToSubscription();
                    } else {
                        // Trial active or user has subscription - go to main app
                        navigateToMainApp();
                    }
                });
            }
        });
    }

    private void navigateToTermsAcceptance() {
        Intent intent = new Intent(SplashActivity.this, TermsAcceptanceActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToSubscription() {
        Intent intent = new Intent(SplashActivity.this, SubscriptionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToMainApp() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Determines if the app is ready to start.
     * For now, we just use a simple timer, but this could be extended
     * to check for actual initialization completion.
     */
    private boolean isReadyToStart() {
        // In a production app, you might check:
        // - Firebase initialization
        // - Required data loading
        // - Authentication state
        // - Network connectivity
        return true;
    }
}
