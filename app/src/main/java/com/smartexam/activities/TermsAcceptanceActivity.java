package com.smartexam.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.smartexam.R;
import com.smartexam.subscription.TrialManager;

/**
 * Mandatory Terms & Conditions acceptance screen.
 * This is a LEGAL requirement, not a marketing screen.
 * 
 * CRITICAL: Trial starts ONLY after acceptance.
 */
public class TermsAcceptanceActivity extends AppCompatActivity {

    private CheckBox termsCheckbox;
    private CheckBox privacyCheckbox;
    private CheckBox trialCheckbox;
    private Button continueButton;
    private TrialManager trialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_acceptance);

        trialManager = TrialManager.getInstance(this);

        initViews();
        setupTermsText();
        setupListeners();
        setupOnBackPressed();
    }

    private void initViews() {
        termsCheckbox = findViewById(R.id.checkboxTerms);
        privacyCheckbox = findViewById(R.id.checkboxPrivacy);
        trialCheckbox = findViewById(R.id.checkboxTrial);
        continueButton = findViewById(R.id.btnContinue);

        // Initially disable continue button
        continueButton.setEnabled(false);
        continueButton.setAlpha(0.5f);
    }

    private void setupTermsText() {
        // Set up terms and conditions link
        TextView termsText = findViewById(R.id.tvTermsLink);
        String termsContent = "Read our full Terms & Conditions";
        SpannableString termsSpannable = new SpannableString(termsContent);
        Linkify.addLinks(termsSpannable, Linkify.WEB_URLS);
        termsText.setText(termsSpannable);
        termsText.setMovementMethod(LinkMovementMethod.getInstance());

        // Set up privacy policy link
        TextView privacyText = findViewById(R.id.tvPrivacyLink);
        String privacyContent = "Read our Privacy Policy";
        SpannableString privacySpannable = new SpannableString(privacyContent);
        Linkify.addLinks(privacySpannable, Linkify.WEB_URLS);
        privacyText.setText(privacySpannable);
        privacyText.setMovementMethod(LinkMovementMethod.getInstance());

        // Set up trial disclosure text
        TextView trialDisclosureText = findViewById(R.id.tvTrialDisclosure);
        String trialDisclosure = "This app is currently in its Pilot Phase and is provided free of charge. Future updates and premium question packs may be charged for within the marketplace.";
        trialDisclosureText.setText(trialDisclosure);
    }

    private void setupListeners() {
        // Checkbox change listeners
        termsCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> validateContinueButton());
        privacyCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> validateContinueButton());
        trialCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> validateContinueButton());

        // Continue button click listener
        continueButton.setOnClickListener(v -> handleTermsAcceptance());
    }

    private void setupOnBackPressed() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Prevent back navigation - terms must be accepted
                // This is a legal requirement for trial activation
                Toast.makeText(TermsAcceptanceActivity.this, "You must accept the terms to continue",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void validateContinueButton() {
        boolean allChecked = termsCheckbox.isChecked() &&
                privacyCheckbox.isChecked() &&
                trialCheckbox.isChecked();

        continueButton.setEnabled(allChecked);
        continueButton.setAlpha(allChecked ? 1.0f : 0.5f);
    }

    private void handleTermsAcceptance() {
        // Show loading state
        continueButton.setEnabled(false);
        continueButton.setText("Activating Pilot...");

        // Start trial with server-side tracking
        trialManager.startTrialAfterTermsAcceptance(new TrialManager.TrialCallback() {
            @Override
            public void onSuccess(TrialManager.TrialStatus status) {
                runOnUiThread(() -> {
                    if (status.isActive) {
                        // Trial started successfully - go to welcome screen
                        navigateToTrialWelcome();
                    } else {
                        // Trial creation failed
                        showError("Failed to start trial. Please try again.");
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    showError("Error: " + errorMessage);
                    resetContinueButton();
                });
            }
        });
    }

    private void navigateToTrialWelcome() {
        // Navigate to trial welcome screen (separate from marketing)
        Intent intent = new Intent(TermsAcceptanceActivity.this, TrialWelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void resetContinueButton() {
        continueButton.setEnabled(true);
        continueButton.setText("Continue");
        validateContinueButton(); // Re-check checkbox state
    }
}
