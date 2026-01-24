package com.smartexam.config;

/**
 * Global configuration for feature flags.
 * Used to toggle features on/off for different build variants or testing.
 */
public class FeatureFlags {

    /**
     * Master switch for the Marketplace module.
     * Set to TRUE to enable the "Marketplace" button in the dashboard.
     * Set to FALSE for production builds until fully ready.
     */
    public static final boolean MARKETPLACE_ENABLED = false;

}
