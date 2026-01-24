package com.smartexam.config;

import android.util.Log;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.smartexam.R;

public class RemoteConfigManager {
    private static final String TAG = "RemoteConfigManager";
    private static RemoteConfigManager instance;
    private final FirebaseRemoteConfig remoteConfig;

    private RemoteConfigManager() {
        remoteConfig = FirebaseRemoteConfig.getInstance();

        // 1. Set configuration settings (e.g., minimum fetch interval)
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600) // Fetch hourly in production
                .build();
        remoteConfig.setConfigSettingsAsync(configSettings);

        // 2. Set defaults from XML
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
    }

    public static synchronized RemoteConfigManager getInstance() {
        if (instance == null) {
            instance = new RemoteConfigManager();
        }
        return instance;
    }

    /**
     * Fetches current values from Firebase.
     */
    public void fetchAndActivate() {
        remoteConfig.fetchAndActivate()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean updated = task.getResult();
                        Log.d(TAG, "Config params updated: " + updated);
                    } else {
                        Log.e(TAG, "Fetch failed");
                    }
                });
    }

    // --- Key Feature Flags ---

    public boolean isMarketplaceEnabled() {
        return remoteConfig.getBoolean("marketplace_enabled");
    }

    public boolean isMaintenanceMode() {
        return remoteConfig.getBoolean("maintenance_mode");
    }

    public long getMaxLocalPapers() {
        return remoteConfig.getLong("max_local_papers");
    }

    public boolean isCapsValidationStrict() {
        return remoteConfig.getBoolean("caps_validation_strict");
    }
}
