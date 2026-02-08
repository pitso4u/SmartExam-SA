package com.smartexam.subscription;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.firestore.FirebaseFirestore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Advanced trial abuse detection and prevention.
 * Protects against common trial abuse vectors.
 */
public class TrialAbuseDetector {
    
    private static final String TAG = "TrialAbuseDetector";
    private static final int MAX_TRIALS_PER_DEVICE = 1;
    private static final int MAX_TRIALS_PER_IP_RANGE = 10; // Basic IP-based protection
    
    private final Context context;
    private final FirebaseFirestore firestore;
    
    public TrialAbuseDetector(Context context) {
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
    }
    
    /**
     * Checks if device is eligible for a new trial
     */
    public void checkDeviceEligibility(String deviceHash, EligibilityCallback callback) {
        Log.d(TAG, "Checking device eligibility for: " + deviceHash.substring(0, 8));
        
        // Query for existing trials with this device hash
        firestore.collection("trials")
            .whereEqualTo("device_hash", deviceHash)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                int existingTrials = querySnapshot.size();
                boolean isEligible = existingTrials < MAX_TRIALS_PER_DEVICE;
                
                Log.d(TAG, "Device eligibility check: " + existingTrials + " existing trials, eligible: " + isEligible);
                
                if (isEligible) {
                    callback.onEligible("Device eligible for trial");
                } else {
                    callback.onIneligible("Device has already used trial period");
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to check device eligibility", e);
                callback.onError("Failed to verify eligibility: " + e.getMessage());
            });
    }
    
    /**
     * Reports suspicious activity for monitoring
     */
    public void reportSuspiciousActivity(String type, String details, String deviceHash) {
        Map<String, Object> report = new HashMap<>();
        report.put("type", type);
        report.put("details", details);
        report.put("device_hash", deviceHash);
        report.put("timestamp", System.currentTimeMillis());
        report.put("app_version", 1); // Use hardcoded version for now
        
        firestore.collection("abuse_reports")
            .add(report)
            .addOnSuccessListener(aVoid -> 
                Log.d(TAG, "Suspicious activity reported: " + type))
            .addOnFailureListener(e -> 
                Log.e(TAG, "Failed to report suspicious activity", e));
    }
    
    /**
     * Checks for patterns indicating trial abuse
     */
    public void analyzeUsagePatterns(String uid, PatternAnalysisCallback callback) {
        // This would analyze user behavior patterns
        // For now, implement basic checks
        
        firestore.collection("trials")
            .document(uid)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Long trialStart = documentSnapshot.getLong("trial_start");
                    String deviceHash = documentSnapshot.getString("device_hash");
                    
                    // Check for suspicious patterns
                    boolean isSuspicious = analyzeForSuspiciousPatterns(trialStart, deviceHash);
                    
                    if (isSuspicious) {
                        reportSuspiciousActivity("suspicious_pattern", 
                            "Unusual usage pattern detected", deviceHash);
                    }
                    
                    callback.onAnalysisComplete(!isSuspicious);
                } else {
                    callback.onAnalysisComplete(true);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to analyze usage patterns", e);
                callback.onError("Analysis failed: " + e.getMessage());
            });
    }
    
    /**
     * Basic suspicious pattern detection
     */
    private boolean analyzeForSuspiciousPatterns(Long trialStart, String deviceHash) {
        if (trialStart == null) return false;
        
        long currentTime = System.currentTimeMillis();
        long trialAge = currentTime - trialStart;
        
        // Suspicious: Multiple trial attempts in short time
        if (trialAge < TimeUnit.MINUTES.toMillis(5)) {
            return true;
        }
        
        // Add more pattern detection logic here
        return false;
    }
    
    /**
     * Validates trial integrity across multiple factors
     */
    public void validateTrialIntegrity(String uid, String deviceHash, IntegrityCallback callback) {
        Map<String, Object> validationResults = new HashMap<>();
        
        // Check 1: Device binding
        checkDeviceEligibility(deviceHash, new EligibilityCallback() {
            @Override
            public void onEligible(String message) {
                validationResults.put("device_binding", "valid");
                continueValidation();
            }
            
            @Override
            public void onIneligible(String message) {
                validationResults.put("device_binding", "invalid: " + message);
                continueValidation();
            }
            
            @Override
            public void onError(String error) {
                validationResults.put("device_binding", "error: " + error);
                continueValidation();
            }
            
            private void continueValidation() {
                // Check 2: Usage patterns
                analyzeUsagePatterns(uid, new PatternAnalysisCallback() {
                    @Override
                    public void onAnalysisComplete(boolean isNormal) {
                        validationResults.put("usage_pattern", isNormal ? "normal" : "suspicious");
                        completeValidation();
                    }
                    
                    @Override
                    public void onError(String error) {
                        validationResults.put("usage_pattern", "error: " + error);
                        completeValidation();
                    }
                });
            }
            
            private void completeValidation() {
                boolean isValid = "valid".equals(validationResults.get("device_binding")) &&
                                  "normal".equals(validationResults.get("usage_pattern"));
                
                callback.onIntegrityComplete(isValid, validationResults);
            }
        });
    }
    
    /**
     * Callback interfaces
     */
    public interface EligibilityCallback {
        void onEligible(String message);
        void onIneligible(String reason);
        void onError(String error);
    }
    
    public interface PatternAnalysisCallback {
        void onAnalysisComplete(boolean isNormal);
        void onError(String error);
    }
    
    public interface IntegrityCallback {
        void onIntegrityComplete(boolean isValid, Map<String, Object> results);
    }
}
