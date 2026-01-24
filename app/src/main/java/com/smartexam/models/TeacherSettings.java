package com.smartexam.models;

public class TeacherSettings {

    private String teacherName;
    private String phoneNumber;
    private String emailAddress;
    private String address;
    private String schoolName;
    private String schoolLogoPath;
    private String academicYear;
    private String learnerCount;
    private String subscriptionPlan;
    private boolean darkThemeEnabled;

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }

    public String getSchoolLogoPath() {
        return schoolLogoPath;
    }

    public void setSchoolLogoPath(String schoolLogoPath) {
        this.schoolLogoPath = schoolLogoPath;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public String getLearnerCount() {
        return learnerCount;
    }

    public void setLearnerCount(String learnerCount) {
        this.learnerCount = learnerCount;
    }

    public String getSubscriptionPlan() {
        return subscriptionPlan;
    }

    public void setSubscriptionPlan(String subscriptionPlan) {
        this.subscriptionPlan = subscriptionPlan;
    }

    public boolean isDarkThemeEnabled() {
        return darkThemeEnabled;
    }

    public void setDarkThemeEnabled(boolean darkThemeEnabled) {
        this.darkThemeEnabled = darkThemeEnabled;
    }
}
