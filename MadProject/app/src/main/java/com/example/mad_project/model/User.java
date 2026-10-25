package com.example.mad_project.model;

import java.io.Serializable;

public class User implements Serializable {
    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_SERVER = "SERVER";

    private String userId;
    private String username;
    private String displayName;
    private String phoneNumber;
    private String role;
    private String canteenName; // e.g. "Main Canteen", "South Canteen"
    private String canteenImageUrl;
    private String passwordHash;
    private String salt;
    private String fcmToken;
    private long createdAt;

    public User() {
        // Required empty constructor for Firestore
    }

    public User(String userId, String username, String displayName, String phoneNumber, String role, String canteenName, String canteenImageUrl, String passwordHash, String salt, long createdAt) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.canteenName = canteenName;
        this.canteenImageUrl = canteenImageUrl;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.createdAt = createdAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCanteenName() {
        return canteenName;
    }

    public void setCanteenName(String canteenName) {
        this.canteenName = canteenName;
    }

    public String getCanteenImageUrl() {
        return canteenImageUrl;
    }

    public void setCanteenImageUrl(String canteenImageUrl) {
        this.canteenImageUrl = canteenImageUrl;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
