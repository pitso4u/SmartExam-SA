package com.smartexam.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.smartexam.R;
import com.smartexam.subscription.TrialManager;

/**
 * Friendly welcome screen AFTER legal terms acceptance.
 * This is where users understand their trial benefits.
 */
public class TrialWelcomeActivity extends AppCompatActivity {
    
    private static final long AUTO_ADVANCE_DELAY = 3000; // 3 seconds
    
    private TextView tvWelcomeTitle;
    private TextView tvTrialDays;
    private TextView tvTrialFeatures;
    private Button btnGetStarted;
    private TrialManager trialManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trial_welcome);
        
        trialManager = TrialManager.getInstance(this);
        
        initViews();
        loadTrialStatus();
        setupListeners();
        
        // Auto-advance after delay
        autoAdvanceToMainApp();
    }
    
    private void initViews() {
        tvWelcomeTitle = findViewById(R.id.tvWelcomeTitle);
        tvTrialDays = findViewById(R.id.tvTrialDays);
        tvTrialFeatures = findViewById(R.id.tvTrialFeatures);
        btnGetStarted = findViewById(R.id.btnGetStarted);
    }
    
    private void loadTrialStatus() {
        trialManager.getTrialStatus(new TrialManager.TrialStatusCallback() {
            @Override
            public void onStatusReceived(TrialManager.TrialStatus status) {
                runOnUiThread(() -> {
                    if (status.isActive) {
                        updateWelcomeUI(status);
                    } else {
                        // Trial not active - shouldn't happen, but handle gracefully
                        navigateToSubscription();
                    }
                });
            }
        });
    }
    
    private void updateWelcomeUI(TrialManager.TrialStatus status) {
        // Update trial days remaining
        long daysRemaining = status.getDaysRemaining();
        tvTrialDays.setText(daysRemaining + " days remaining");
        
        // Set up welcome message
        tvWelcomeTitle.setText("Welcome to SmartExam SA! 🎓");
        
        // Set up trial features
        String features = "Your 14-day trial includes:\n\n" +
                         "✓ Create unlimited questions\n" +
                         "✓ Generate professional assessments\n" +
                         "✓ Export PDFs with memos\n" +
                         "✓ Browse marketplace content\n" +
                         "✓ Full access to all features";
        
        tvTrialFeatures.setText(features);
    }
    
    private void setupListeners() {
        btnGetStarted.setOnClickListener(v -> {
            navigateToMainApp();
        });
    }
    
    private void autoAdvanceToMainApp() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            navigateToMainApp();
        }, AUTO_ADVANCE_DELAY);
    }
    
    private void navigateToMainApp() {
        Intent intent = new Intent(TrialWelcomeActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void navigateToSubscription() {
        Intent intent = new Intent(TrialWelcomeActivity.this, SubscriptionActivity.class);
        startActivity(intent);
        finish();
    }
}
