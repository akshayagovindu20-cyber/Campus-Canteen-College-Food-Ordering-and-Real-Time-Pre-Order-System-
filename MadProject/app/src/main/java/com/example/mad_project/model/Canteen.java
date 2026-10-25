package com.example.mad_project.model;

import java.io.Serializable;

public class Canteen implements Serializable {
    private String canteenId;
    private String name;
    private String location;
    private String description;
    private String imageUrl;
    private boolean open;

    public Canteen() {
    }

    public Canteen(String canteenId, String name, String location, String description, String imageUrl, boolean open) {
        this.canteenId = canteenId;
        this.name = name;
        this.location = location;
        this.description = description;
        this.imageUrl = imageUrl;
        this.open = open;
    }

    public String getCanteenId() {
        return canteenId;
    }

    public void setCanteenId(String canteenId) {
        this.canteenId = canteenId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }
}
