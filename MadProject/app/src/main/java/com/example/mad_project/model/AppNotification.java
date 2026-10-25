package com.example.mad_project.model;

import java.io.Serializable;

public class AppNotification implements Serializable {
    private String id;
    private String recipientId;
    private String orderId;
    private String title;
    private String message;
    private boolean read;
    private long timestamp;

    public AppNotification() {
    }

    public AppNotification(String id, String recipientId, String orderId, String title, String message, boolean read, long timestamp) {
        this.id = id;
        this.recipientId = recipientId;
        this.orderId = orderId;
        this.title = title;
        this.message = message;
        this.read = read;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
