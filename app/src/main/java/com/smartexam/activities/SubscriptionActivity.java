package com.smartexam.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.smartexam.subscription.PaystackService;
import com.smartexam.R;
import com.smartexam.subscription.SubscriptionAdapter;
import com.smartexam.subscription.SubscriptionPlan;
import com.smartexam.subscription.TrialStateManager;
import java.util.ArrayList;
import java.util.List;

public class SubscriptionActivity extends AppCompatActivity {
    
    private static final String TAG = "SubscriptionActivity";
    
    private PaystackService paystackService;
    private TrialStateManager trialStateManager;
    
    // UI Components
    private TextView tvTrialStatus;
    private TextView tvTrialDays;
    private RecyclerView rvSubscriptionPlans;
    private Button btnRestorePurchases;
    private SubscriptionAdapter subscriptionAdapter;
    
    // Data
    private List<SubscriptionPlan> subscriptionPlans = new ArrayList<>();
    private TrialStateManager.TrialStateData currentTrialState;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription);
        
        initializeManagers();
        initViews();
        setupSubscriptionPlans();
        loadTrialStatus();
        setupBilling();
    }
    
    private void initializeManagers() {
        paystackService = PaystackService.getInstance();
        trialStateManager = TrialStateManager.getInstance(this);
    }
    
    private void initViews() {
        tvTrialStatus = findViewById(R.id.tvTrialStatus);
        tvTrialDays = findViewById(R.id.tvTrialDays);
        rvSubscriptionPlans = findViewById(R.id.rvSubscriptionPlans);
        btnRestorePurchases = findViewById(R.id.btnRestorePurchases);
        
        // Setup RecyclerView
        subscriptionAdapter = new SubscriptionAdapter(subscriptionPlans, this::onSubscriptionSelected);
        rvSubscriptionPlans.setLayoutManager(new LinearLayoutManager(this));
        rvSubscriptionPlans.setAdapter(subscriptionAdapter);
        
        // Restore purchases button
        btnRestorePurchases.setOnClickListener(v -> restorePurchases());
    }
    
    private void setupSubscriptionPlans() {
        subscriptionPlans.add(new SubscriptionPlan(
            "monthly_teacher",
            "Monthly Plan",
            "R50.00/month",
            "Perfect for individual teachers",
            "Billed monthly"
        ));
        
        subscriptionPlans.add(new SubscriptionPlan(
            "yearly_teacher",
            "Annual Plan",
            "R500.00/year",
            "Save 17% with annual billing",
            "Billed annually"
        ));
        
        subscriptionAdapter.notifyDataSetChanged();
    }
    
    private void loadTrialStatus() {
        trialStateManager.getTrialState(new TrialStateManager.TrialStateCallback() {
            @Override
            public void onStateReceived(TrialStateManager.TrialStateData state) {
                currentTrialState = state;
                runOnUiThread(() -> updateTrialUI(state));
            }
            
            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(SubscriptionActivity.this, 
                        "Failed to load trial status", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void setupBilling() {
        // Paystack integration handled in onSubscriptionSelected
        // No setup needed for Paystack as it uses Firebase Functions
    }
    
    private void onSubscriptionSelected(SubscriptionPlan plan) {
        Log.d(TAG, "Selected subscription: " + plan.getSku());
        
        String email = paystackService.getCurrentUserEmail();
        if (email == null) {
            Toast.makeText(this, "Please sign in to make a purchase", Toast.LENGTH_SHORT).show();
            return;
        }

        // Determine amount based on plan
        long amount;
        if ("monthly_teacher".equals(plan.getSku())) {
            amount = 5000; // R50
        } else if ("yearly_teacher".equals(plan.getSku())) {
            amount = 50000; // R500
        } else {
            Toast.makeText(this, "Invalid subscription plan", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create Paystack checkout URL
        paystackService.createCheckoutUrl(email, amount, plan.getSku(), new PaystackService.CheckoutCallback() {
            @Override
            public void onSuccess(String authorizationUrl) {
                runOnUiThread(() -> {
                    // Open the checkout URL in browser
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(authorizationUrl));
                    startActivity(browserIntent);

                    // For now, simulate successful payment after opening browser
                    // In production, handle callback from Paystack
                    simulateSuccessfulSubscription();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(SubscriptionActivity.this, "Failed to initiate subscription: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void simulateSuccessfulSubscription() {
        // For demonstration - in production, wait for Paystack callback
        btnRestorePurchases.postDelayed(() -> {
            runOnUiThread(() -> {
                Toast.makeText(SubscriptionActivity.this, 
                    "Subscription successful!", Toast.LENGTH_LONG).show();
                
                // Reload trial status to show conversion
                loadTrialStatus();
                
                // Navigate to main app
                navigateToMainApp();
            });
        }, 5000); // Simulate 5 seconds for payment completion
    }
    
    private void restorePurchases() {
        // For Paystack, subscription status is managed via Firestore
        // In production, query user's subscription status from Firestore
        Toast.makeText(SubscriptionActivity.this, 
            "Subscription status check not implemented yet", Toast.LENGTH_SHORT).show();
    }
    
    private String getTrialToken() {
        // In a real implementation, this would come from the trial system
        // For now, return null to start fresh subscription
        return null;
    }
    
    private void navigateToMainApp() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Paystack service doesn't need explicit disconnect
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Paystack payment is handled via browser, no result handling needed
    }
    
    private void updateTrialUI(TrialStateManager.TrialStateData state) {
        if (state.isActive()) {
            tvTrialStatus.setText("Trial Active");
            tvTrialDays.setText(String.format("%d days remaining", state.getDaysRemaining()));
        } else {
            tvTrialStatus.setText("Trial Expired");
            tvTrialDays.setText("Please subscribe to continue");
        }
    }
}
