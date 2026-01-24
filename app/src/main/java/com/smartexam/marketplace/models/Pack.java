package com.smartexam.marketplace.models;

import java.util.List;

/**
 * Represents a Question Pack available in the Marketplace.
 * This is a read-only model derived from remote/mock data.
 */
public class Pack {
    private String id;
    private String title;
    private String description;
    private String subject;
    private String gradeRange; // e.g., "Grade 8-9"
    private int questionCount;
    private double price; // In ZAR
    private boolean isPurchased;

    public Pack(String id, String title, String description, String subject, String gradeRange, int questionCount,
            double price) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.subject = subject;
        this.gradeRange = gradeRange;
        this.questionCount = questionCount;
        this.price = price;
        this.isPurchased = false;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getSubject() {
        return subject;
    }

    public String getGradeRange() {
        return gradeRange;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public double getPrice() {
        return price;
    }

    public boolean isPurchased() {
        return isPurchased;
    }

    public void setPurchased(boolean purchased) {
        isPurchased = purchased;
    }

    public String getFormattedPrice() {
        return String.format("R%.2f", price);
    }
}
