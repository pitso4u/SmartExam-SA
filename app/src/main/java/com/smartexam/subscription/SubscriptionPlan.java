package com.smartexam.subscription;

public class SubscriptionPlan {
    private final String sku;
    private final String title;
    private final String price;
    private final String description;
    private final String billingPeriod;

    public SubscriptionPlan(String sku, String title, String price, String description, String billingPeriod) {
        this.sku = sku;
        this.title = title;
        this.price = price;
        this.description = description;
        this.billingPeriod = billingPeriod;
    }

    public String getSku() {
        return sku;
    }

    public String getTitle() {
        return title;
    }

    public String getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }

    public String getBillingPeriod() {
        return billingPeriod;
    }
}
