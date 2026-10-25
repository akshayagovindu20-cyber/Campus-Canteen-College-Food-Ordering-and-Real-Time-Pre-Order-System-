package com.example.mad_project.model;

import java.io.Serializable;

public class CartItem implements Serializable {
    private String foodId;
    private String foodName;
    private double price;
    private String imageUrl;
    private int quantity;

    public CartItem() {
    }

    public CartItem(String foodId, String foodName, double price, String imageUrl, int quantity) {
        this.foodId = foodId;
        this.foodName = foodName;
        this.price = price;
        this.imageUrl = imageUrl;
        this.quantity = quantity;
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

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getItemTotal() {
        return price * quantity;
    }
}
