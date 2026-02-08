package com.smartexam.subscription;

import android.content.Context;
import android.content.SharedPreferences;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Robust trial state management with offline support and conflict resolution.
 * Handles trial lifecycle, state synchronization, and edge cases.
 */
public class TrialStateManager {
    
    private static final String TAG = "TrialStateManager";
    private static final String PREFS_NAME = "trial_state";
    private static final String KEY_TRIAL_STATE = "trial_state_json";
    private static final String KEY_LAST_SYNC = "last_sync_timestamp";
    private static final String KEY_CONFLICT_RESOLUTION = "conflict_resolution_count";
    
    // Trial states
    public enum TrialState {
        NONE,           // No trial ever started
        ACTIVE,         // Trial is currently active
        EXPIRED,        // Trial has expired
        CONVERTED,      // Trial converted to subscription
        SUSPENDED,      // Trial suspended due to abuse
        CANCELLED       // Trial cancelled by user
    }
    
    // Trial state data
    public static class TrialStateData {
        public final TrialState state;
        public final long trialStart;
        public final long trialEnd;
        public final long lastSync;
        public final String deviceHash;
        public final boolean serverVerified;
        public final Map<String, Object> metadata;
        
        public TrialStateData(TrialState state, long trialStart, long trialEnd, 
                             long lastSync, String deviceHash, boolean serverVerified,
                             Map<String, Object> metadata) {
            this.state = state;
            this.trialStart = trialStart;
            this.trialEnd = trialEnd;
            this.lastSync = lastSync;
            this.deviceHash = deviceHash;
            this.serverVerified = serverVerified;
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }
        
        public boolean isValid() {
            return trialStart > 0 && trialEnd > trialStart;
        }
        
        public boolean isActive() {
            return state == TrialState.ACTIVE && System.currentTimeMillis() < trialEnd;
        }
        
        public long getDaysRemaining() {
            if (!isActive()) return 0;
            long remainingMs = trialEnd - System.currentTimeMillis();
            return TimeUnit.MILLISECONDS.toDays(remainingMs);
        }
        
        public long getHoursRemaining() {
            if (!isActive()) return 0;
            long remainingMs = trialEnd - System.currentTimeMillis();
            return TimeUnit.MILLISECONDS.toHours(remainingMs);
        }
    }
    
    private static TrialStateManager instance;
    private final Context context;
    private final SharedPreferences preferences;
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    private final TrialManager trialManager;
    
    private TrialStateManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.trialManager = TrialManager.getInstance(context);
    }
    
    public static synchronized TrialStateManager getInstance(Context context) {
        if (instance == null) {
            instance = new TrialStateManager(context);
        }
        return instance;
    }
    
    /**
     * Gets current trial state with automatic conflict resolution
     */
    public void getTrialState(@NonNull TrialStateCallback callback) {
        TrialStateData localState = getLocalTrialState();
        
        if (localState == null || !localState.isValid()) {
            // No valid local state - fetch from server
            fetchTrialStateFromServer(callback);
        } else {
            // Have local state - check if sync is needed
            if (shouldSyncWithServer(localState)) {
                syncTrialStateWithServer(localState, callback);
            } else {
                // Return cached state
                callback.onStateReceived(localState);
            }
        }
    }
    
    /**
     * Updates trial state with conflict resolution
     */
    public void updateTrialState(@NonNull TrialStateData newState, @NonNull TrialStateCallback callback) {
        Log.d(TAG, "Updating trial state to: " + newState.state);
        
        // Validate state transition
        if (!isValidStateTransition(getLocalTrialState(), newState)) {
            callback.onError("Invalid state transition");
            return;
        }
        
        // Update locally first for immediate response
        storeTrialStateLocally(newState);
        
        // Then sync to server
        syncTrialStateToServer(newState, callback);
    }
    
    /**
     * Forces immediate server synchronization
     */
    public void forceSyncWithServer(@NonNull TrialStateCallback callback) {
        fetchTrialStateFromServer(callback);
    }
    
    /**
     * Gets trial state from local storage
     */
    @Nullable
    private TrialStateData getLocalTrialState() {
        try {
            String stateJson = preferences.getString(KEY_TRIAL_STATE, null);
            if (stateJson == null) return null;
            
            // Parse JSON (simplified - in production, use Gson)
            Map<String, Object> data = parseStateJson(stateJson);
            if (data == null) return null;
            
            TrialState state = TrialState.valueOf((String) data.get("state"));
            long trialStart = (long) data.get("trialStart");
            long trialEnd = (long) data.get("trialEnd");
            long lastSync = (long) data.get("lastSync");
            String deviceHash = (String) data.get("deviceHash");
            boolean serverVerified = (boolean) data.get("serverVerified");
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) data.get("metadata");
            
            return new TrialStateData(state, trialStart, trialEnd, lastSync, 
                                   deviceHash, serverVerified, metadata);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse local trial state", e);
            return null;
        }
    }
    
    /**
     * Stores trial state locally
     */
    private void storeTrialStateLocally(@NonNull TrialStateData state) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("state", state.state.name());
            data.put("trialStart", state.trialStart);
            data.put("trialEnd", state.trialEnd);
            data.put("lastSync", state.lastSync);
            data.put("deviceHash", state.deviceHash);
            data.put("serverVerified", state.serverVerified);
            data.put("metadata", state.metadata);
            
            String stateJson = serializeStateData(data);
            
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(KEY_TRIAL_STATE, stateJson);
            editor.putLong(KEY_LAST_SYNC, System.currentTimeMillis());
            editor.apply();
            
            Log.d(TAG, "Trial state stored locally: " + state.state);
        } catch (Exception e) {
            Log.e(TAG, "Failed to store trial state locally", e);
        }
    }
    
    /**
     * Fetches trial state from server
     */
    private void fetchTrialStateFromServer(@NonNull TrialStateCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onStateReceived(new TrialStateData(TrialState.NONE, 0, 0, 0, "", false, new HashMap<>()));
            return;
        }
        
        firestore.collection("trials")
            .document(currentUser.getUid())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    TrialStateData serverState = parseTrialStateFromDocument(documentSnapshot);
                    storeTrialStateLocally(serverState);
                    callback.onStateReceived(serverState);
                } else {
                    // No trial found on server
                    TrialStateData noTrialState = new TrialStateData(TrialState.NONE, 0, 0, 
                        System.currentTimeMillis(), "", false, new HashMap<>());
                    storeTrialStateLocally(noTrialState);
                    callback.onStateReceived(noTrialState);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to fetch trial state from server", e);
                // Return local state if available
                TrialStateData localState = getLocalTrialState();
                if (localState != null) {
                    callback.onStateReceived(localState);
                } else {
                    callback.onError("Failed to fetch trial state: " + e.getMessage());
                }
            });
    }
    
    /**
     * Syncs local state with server
     */
    private void syncTrialStateWithServer(@NonNull TrialStateData localState, @NonNull TrialStateCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onStateReceived(localState);
            return;
        }
        
        firestore.collection("trials")
            .document(currentUser.getUid())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    TrialStateData serverState = parseTrialStateFromDocument(documentSnapshot);
                    
                    // Resolve conflicts
                    TrialStateData resolvedState = resolveStateConflict(localState, serverState);
                    
                    if (resolvedState != localState) {
                        storeTrialStateLocally(resolvedState);
                    }
                    
                    callback.onStateReceived(resolvedState);
                } else {
                    // Server has no record - possible abuse or new device
                    handleMissingServerRecord(localState, callback);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to sync trial state with server", e);
                callback.onStateReceived(localState);
            });
    }
    
    /**
     * Syncs trial state to server
     */
    private void syncTrialStateToServer(@NonNull TrialStateData state, @NonNull TrialStateCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("No authenticated user");
            return;
        }
        
        Map<String, Object> serverData = new HashMap<>();
        serverData.put("state", state.state.name());
        serverData.put("trial_start", state.trialStart);
        serverData.put("trial_end", state.trialEnd);
        serverData.put("device_hash", state.deviceHash);
        serverData.put("last_sync", System.currentTimeMillis());
        serverData.put("app_version", 1); // Temporarily hardcoded
        serverData.put("metadata", state.metadata);
        
        firestore.collection("trials")
            .document(currentUser.getUid())
            .set(serverData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Trial state synced to server");
                callback.onStateReceived(state);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to sync trial state to server", e);
                callback.onError("Failed to sync state: " + e.getMessage());
            });
    }
    
    /**
     * Resolves conflicts between local and server states
     */
    private TrialStateData resolveStateConflict(@NonNull TrialStateData localState, @NonNull TrialStateData serverState) {
        // Conflict resolution strategy:
        // 1. Server takes precedence for most fields
        // 2. Local metadata is merged
        // 3. Timestamps are compared for accuracy
        
        if (serverState.lastSync > localState.lastSync) {
            // Server is more recent - use server state
            Map<String, Object> mergedMetadata = new HashMap<>(serverState.metadata);
            mergedMetadata.putAll(localState.metadata); // Merge local metadata
            
            return new TrialStateData(serverState.state, serverState.trialStart, serverState.trialEnd,
                                       serverState.lastSync, serverState.deviceHash, true, mergedMetadata);
        } else {
            // Local is more recent - sync to server
            syncTrialStateToServer(localState, new TrialStateCallback() {
                @Override
                public void onStateReceived(TrialStateData state) {
                    // Sync completed
                }
                
                @Override
                public void onError(String errorMessage) {
                    // Sync failed
                }
            });
            return localState;
        }
    }
    
    /**
     * Handles missing server record
     */
    private void handleMissingServerRecord(@NonNull TrialStateData localState, @NonNull TrialStateCallback callback) {
        // Check if this is legitimate (new device) or abuse
        if (localState.serverVerified && localState.trialStart > 0) {
            // Legitimate local state - sync to server
            syncTrialStateToServer(localState, callback);
        } else {
            // Possible abuse - reset to NONE
            TrialStateData resetState = new TrialStateData(TrialState.NONE, 0, 0, 
                System.currentTimeMillis(), "", false, new HashMap<>());
            storeTrialStateLocally(resetState);
            callback.onStateReceived(resetState);
        }
    }
    
    /**
     * Checks if sync with server is needed
     */
    private boolean shouldSyncWithServer(@NonNull TrialStateData state) {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastSync = currentTime - state.lastSync;
        
        // Sync if:
        // - Never synced before
        // - Last sync was more than 1 hour ago
        // - State is not server verified
        return state.lastSync == 0 || 
               timeSinceLastSync > TimeUnit.HOURS.toMillis(1) || 
               !state.serverVerified;
    }
    
    /**
     * Validates state transition
     */
    private boolean isValidStateTransition(@Nullable TrialStateData fromState, @NonNull TrialStateData toState) {
        // Allow any transition to/from NONE
        if (fromState == null || fromState.state == TrialState.NONE) {
            return true;
        }
        
        // Allow same state updates
        if (fromState.state == toState.state) {
            return true;
        }
        
        // Define valid transitions
        switch (fromState.state) {
            case ACTIVE:
                return toState.state == TrialState.EXPIRED || 
                       toState.state == TrialState.CONVERTED ||
                       toState.state == TrialState.SUSPENDED ||
                       toState.state == TrialState.CANCELLED;
            case EXPIRED:
                return toState.state == TrialState.CONVERTED;
            case SUSPENDED:
                return toState.state == TrialState.ACTIVE || toState.state == TrialState.CANCELLED;
            case CONVERTED:
            case CANCELLED:
                return false; // Terminal states
            default:
                return false;
        }
    }
    
    /**
     * Parses trial state from Firestore document
     */
    private TrialStateData parseTrialStateFromDocument(@NonNull DocumentSnapshot document) {
        String stateStr = document.getString("state");
        TrialState state = stateStr != null ? TrialState.valueOf(stateStr) : TrialState.NONE;
        long trialStart = document.getLong("trial_start") != null ? document.getLong("trial_start") : 0;
        long trialEnd = document.getLong("trial_end") != null ? document.getLong("trial_end") : 0;
        long lastSync = document.getLong("last_sync") != null ? document.getLong("last_sync") : System.currentTimeMillis();
        String deviceHash = document.getString("device_hash") != null ? document.getString("device_hash") : "";
        
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) document.get("metadata");
        
        return new TrialStateData(state, trialStart, trialEnd, lastSync, deviceHash, true, metadata);
    }
    
    /**
     * Simplified JSON parsing (use Gson in production)
     */
    private Map<String, Object> parseStateJson(String json) {
        // This is a simplified implementation
        // In production, use Gson or similar JSON library
        Map<String, Object> data = new HashMap<>();
        try {
            // Basic parsing - implement proper JSON parsing
            data.put("state", "ACTIVE");
            data.put("trialStart", System.currentTimeMillis());
            data.put("trialEnd", System.currentTimeMillis() + TimeUnit.DAYS.toMillis(14));
            data.put("lastSync", System.currentTimeMillis());
            data.put("deviceHash", "");
            data.put("serverVerified", false);
            data.put("metadata", new HashMap<>());
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse JSON", e);
            return null;
        }
        return data;
    }
    
    /**
     * Simplified JSON serialization (use Gson in production)
     */
    private String serializeStateData(Map<String, Object> data) {
        // This is a simplified implementation
        // In production, use Gson or similar JSON library
        return data.toString();
    }
    
    /**
     * Callback interface for trial state operations
     */
    public interface TrialStateCallback {
        void onStateReceived(TrialStateData state);
        void onError(String errorMessage);
    }
}
