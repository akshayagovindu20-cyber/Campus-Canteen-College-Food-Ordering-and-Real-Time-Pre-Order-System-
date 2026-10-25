package com.example.mad_project.model;

import java.io.Serializable;

public class OrderItem implements Serializable {
    private String foodId;
    private String foodName;
    private double unitPrice;
    private int quantity;
    private String imageUrl;

    public OrderItem() {
    }

    public OrderItem(String foodId, String foodName, double unitPrice, int quantity, String imageUrl) {
        this.foodId = foodId;
        this.foodName = foodName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
    }

    public String getFoodId() {
        return foodId;
    }

    public void setFoodId(String foodId) {
        this.foodId = foodId;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public double getItemTotal() {
        return unitPrice * quantity;
    }
}
