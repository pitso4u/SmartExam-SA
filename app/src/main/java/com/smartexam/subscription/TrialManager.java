package com.smartexam.subscription;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Enterprise-grade trial management with server-side tracking and device
 * binding.
 * Prevents trial abuse through multi-layer validation.
 */
public class TrialManager {

    private static final String TAG = "TrialManager";
    private static final String PREFS_NAME = "trial_management";
    private static final String KEY_TRIAL_START = "trial_start_timestamp";
    private static final String KEY_TERMS_ACCEPTED = "terms_accepted_timestamp";
    private static final String KEY_DEVICE_HASH = "device_hash";
    private static final String KEY_LAST_SERVER_SYNC = "last_server_sync_timestamp";
    private static final String KEY_TRIAL_VERIFIED = "trial_verified_with_server";

    // Trial configuration
    private static final long TRIAL_DURATION_DAYS = 3650; // 10 years (Effective Free access for Pilot)
    private static final long TRIAL_DURATION_MS = TimeUnit.DAYS.toMillis(TRIAL_DURATION_DAYS);
    private static final long SYNC_THRESHOLD_MS = TimeUnit.HOURS.toMillis(1); // Sync every hour

    private static TrialManager instance;
    private final Context context;
    private final SharedPreferences preferences;
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;

    private TrialManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public static synchronized TrialManager getInstance(Context context) {
        if (instance == null) {
            instance = new TrialManager(context);
        }
        return instance;
    }

    /**
     * Starts a trial after terms acceptance with server-side tracking.
     * This is the ONLY method that should start a trial.
     */
    public void startTrialAfterTermsAcceptance(@NonNull TrialCallback callback) {
        Log.d(TAG, "Starting trial after terms acceptance");

        // Generate device hash for binding
        String deviceHash = generateDeviceHash();

        // Get current Firebase user
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            // Anonymous user - create one for trial tracking
            startAnonymousTrial(deviceHash, callback);
        } else {
            // Existing user - start trial with their UID
            startAuthenticatedTrial(currentUser.getUid(), deviceHash, callback);
        }
    }

    /**
     * Starts trial for anonymous user (new installs)
     */
    private void startAnonymousTrial(@NonNull String deviceHash, @NonNull TrialCallback callback) {
        auth.signInAnonymously()
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    createTrialRecord(uid, deviceHash, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create anonymous user for trial", e);
                    callback.onError("Failed to initialize trial");
                });
    }

    /**
     * Starts trial for authenticated user
     */
    private void startAuthenticatedTrial(@NonNull String uid, @NonNull String deviceHash,
            @NonNull TrialCallback callback) {
        createTrialRecord(uid, deviceHash, callback);
    }

    /**
     * Creates trial record on server with device binding
     */
    private void createTrialRecord(@NonNull String uid, @NonNull String deviceHash, @NonNull TrialCallback callback) {
        long currentTime = System.currentTimeMillis();

        // Prepare trial data
        Map<String, Object> trialData = new HashMap<>();
        trialData.put("trial_start", currentTime);
        trialData.put("trial_end", currentTime + TRIAL_DURATION_MS);
        trialData.put("device_hash", deviceHash);
        trialData.put("terms_accepted", currentTime);
        trialData.put("app_version", 1); // Temporarily hardcoded
        trialData.put("created_at", currentTime);
        trialData.put("status", "active");

        // Create server record
        firestore.collection("trials")
                .document(uid)
                .set(trialData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Trial record created successfully");

                    // Store locally for offline access
                    storeTrialLocally(currentTime, deviceHash, true);

                    callback.onSuccess(new TrialStatus(
                            currentTime,
                            currentTime + TRIAL_DURATION_MS,
                            true,
                            "Trial started successfully"));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create trial record", e);
                    callback.onError("Failed to start trial: " + e.getMessage());
                });
    }

    /**
     * Stores trial data locally for offline access
     */
    private void storeTrialLocally(long trialStart, String deviceHash, boolean serverVerified) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(KEY_TRIAL_START, trialStart);
        editor.putLong(KEY_TERMS_ACCEPTED, System.currentTimeMillis());
        editor.putString(KEY_DEVICE_HASH, deviceHash);
        editor.putLong(KEY_LAST_SERVER_SYNC, System.currentTimeMillis());
        editor.putBoolean(KEY_TRIAL_VERIFIED, serverVerified);
        editor.apply();

        Log.d(TAG, "Trial stored locally: start=" + trialStart + ", verified=" + serverVerified);
    }

    /**
     * Gets current trial status with server verification when possible
     */
    public void getTrialStatus(@NonNull TrialStatusCallback callback) {
        // Check local status first
        TrialStatus localStatus = getLocalTrialStatus();

        if (localStatus == null) {
            // No trial found locally
            callback.onStatusReceived(new TrialStatus(0, 0, false, "No trial found"));
            return;
        }

        // Try to verify with server if online and due for sync
        if (shouldSyncWithServer()) {
            verifyTrialWithServer(localStatus, callback);
        } else {
            // Return local status
            callback.onStatusReceived(localStatus);
        }
    }

    /**
     * Gets trial status from local storage
     */
    @Nullable
    private TrialStatus getLocalTrialStatus() {
        if (!preferences.contains(KEY_TRIAL_START)) {
            return null;
        }

        long trialStart = preferences.getLong(KEY_TRIAL_START, 0);
        long trialEnd = trialStart + TRIAL_DURATION_MS;
        boolean isActive = System.currentTimeMillis() < trialEnd;
        boolean serverVerified = preferences.getBoolean(KEY_TRIAL_VERIFIED, false);

        return new TrialStatus(trialStart, trialEnd, isActive, "Local trial status");
    }

    /**
     * Verifies trial status with server
     */
    private void verifyTrialWithServer(@NonNull TrialStatus localStatus, @NonNull TrialStatusCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            // Can't verify server-side, return local status
            callback.onStatusReceived(localStatus);
            return;
        }

        firestore.collection("trials")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        TrialStatus serverStatus = parseTrialStatusFromDocument(documentSnapshot);

                        // Update local cache
                        storeTrialLocally(
                                serverStatus.trialStart,
                                preferences.getString(KEY_DEVICE_HASH, ""),
                                true);

                        callback.onStatusReceived(serverStatus);
                    } else {
                        // Server has no record - possible abuse
                        Log.w(TAG, "No trial record found on server for user: " + currentUser.getUid());
                        callback.onStatusReceived(new TrialStatus(0, 0, false, "Trial not found on server"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to verify trial with server", e);
                    // Return local status on server failure
                    callback.onStatusReceived(localStatus);
                });
    }

    /**
     * Parses trial status from Firestore document
     */
    private TrialStatus parseTrialStatusFromDocument(@NonNull DocumentSnapshot document) {
        long trialStart = document.getLong("trial_start") != null ? document.getLong("trial_start") : 0;
        long trialEnd = document.getLong("trial_end") != null ? document.getLong("trial_end") : 0;
        String status = document.getString("status");
        boolean isActive = "active".equals(status) && System.currentTimeMillis() < trialEnd;

        return new TrialStatus(trialStart, trialEnd, isActive, "Server verified trial status");
    }

    /**
     * Checks if trial should sync with server
     */
    private boolean shouldSyncWithServer() {
        long lastSync = preferences.getLong(KEY_LAST_SERVER_SYNC, 0);
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastSync) > SYNC_THRESHOLD_MS;
    }

    /**
     * Generates device hash for binding
     */
    private String generateDeviceHash() {
        String deviceId = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(deviceId.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Failed to generate device hash", e);
            return deviceId; // Fallback to raw device ID
        }
    }

    /**
     * Checks if current device matches trial device
     */
    public boolean isDeviceValidForTrial() {
        String storedDeviceHash = preferences.getString(KEY_DEVICE_HASH, "");
        String currentDeviceHash = generateDeviceHash();

        boolean isValid = storedDeviceHash.equals(currentDeviceHash);
        Log.d(TAG, "Device validation: stored=" + storedDeviceHash.substring(0, 8) +
                ", current=" + currentDeviceHash.substring(0, 8) + ", valid=" + isValid);

        return isValid;
    }

    /**
     * Forces server sync of trial status
     */
    public void forceSyncWithServer(@NonNull TrialStatusCallback callback) {
        TrialStatus localStatus = getLocalTrialStatus();
        if (localStatus != null) {
            verifyTrialWithServer(localStatus, callback);
        } else {
            callback.onStatusReceived(new TrialStatus(0, 0, false, "No trial to sync"));
        }
    }

    /**
     * Clears all trial data (for testing only)
     */
    public void clearTrialData() {
        preferences.edit().clear().apply();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            firestore.collection("trials")
                    .document(currentUser.getUid())
                    .delete()
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Trial data cleared"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to clear trial data", e));
        }
    }

    /**
     * Trial status data class
     */
    public static class TrialStatus {
        public final long trialStart;
        public final long trialEnd;
        public final boolean isActive;
        public final String message;

        public TrialStatus(long trialStart, long trialEnd, boolean isActive, String message) {
            this.trialStart = trialStart;
            this.trialEnd = trialEnd;
            this.isActive = isActive;
            this.message = message;
        }

        public long getDaysRemaining() {
            if (!isActive)
                return 0;
            long remainingMs = trialEnd - System.currentTimeMillis();
            return TimeUnit.MILLISECONDS.toDays(remainingMs);
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > trialEnd;
        }
    }

    /**
     * Callback interface for trial operations
     */
    public interface TrialCallback {
        void onSuccess(TrialStatus status);

        void onError(String errorMessage);
    }

    /**
     * Callback interface for trial status queries
     */
    public interface TrialStatusCallback {
        void onStatusReceived(TrialStatus status);
    }
}
