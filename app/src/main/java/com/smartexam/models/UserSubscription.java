package com.smartexam.models;

import com.google.gson.annotations.SerializedName;

/**
 * User subscription model for Firebase Firestore.
 * This represents the subscription state that Firebase decides, not WinDev.
 */
public class UserSubscription {
    @SerializedName("email")
    private String email;

    @SerializedName("role")
    private String role;

    @SerializedName("trialStartDate")
    private String trialStartDate;

    @SerializedName("subscription")
    private SubscriptionInfo subscription;

    @SerializedName("createdAt")
    private String createdAt;

    public static class SubscriptionInfo {
        @SerializedName("status")
        private String status; // trial, active, expired, cancelled

        @SerializedName("paystackCustomerCode")
        private String paystackCustomerCode;

        @SerializedName("paystackSubscriptionCode")
        private String paystackSubscriptionCode;

        @SerializedName("currentPeriodEnd")
        private String currentPeriodEnd;

        public SubscriptionInfo() {}

        public SubscriptionInfo(String status) {
            this.status = status;
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getPaystackCustomerCode() { return paystackCustomerCode; }
        public void setPaystackCustomerCode(String paystackCustomerCode) { this.paystackCustomerCode = paystackCustomerCode; }

        public String getPaystackSubscriptionCode() { return paystackSubscriptionCode; }
        public void setPaystackSubscriptionCode(String paystackSubscriptionCode) { this.paystackSubscriptionCode = paystackSubscriptionCode; }

        public String getCurrentPeriodEnd() { return currentPeriodEnd; }
        public void setCurrentPeriodEnd(String currentPeriodEnd) { this.currentPeriodEnd = currentPeriodEnd; }
    }

    public UserSubscription() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getTrialStartDate() { return trialStartDate; }
    public void setTrialStartDate(String trialStartDate) { this.trialStartDate = trialStartDate; }

    public SubscriptionInfo getSubscription() { return subscription; }
    public void setSubscription(SubscriptionInfo subscription) { this.subscription = subscription; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    /**
     * Convenience method to check if user has active subscription
     */
    public boolean hasActiveSubscription() {
        return subscription != null && 
               ("active".equals(subscription.getStatus()) || 
                isValidTrial());
    }

    /**
     * Check if trial is still valid (Firebase-driven)
     */
    public boolean isValidTrial() {
        return subscription != null && 
               "trial".equals(subscription.getStatus()) &&
               trialStartDate != null;
    }

    /**
     * Check if user should see watermark
     */
    public boolean shouldApplyWatermark() {
        return !hasActiveSubscription();
    }
}
