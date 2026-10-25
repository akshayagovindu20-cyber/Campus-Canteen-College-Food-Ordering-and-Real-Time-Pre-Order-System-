package com.example.mad_project.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.mad_project.model.CartItem;
import com.example.mad_project.model.FoodItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartManager {
    private static final String PREF_NAME = "campus_canteen_cart_prefs";
    private static final String KEY_CART_DATA = "cart_items_data";

    private static CartManager instance;
    private final Map<String, CartItem> cartMap = new HashMap<>();
    private final SharedPreferences prefs;

    public interface CartChangeListener {
        void onCartChanged(int itemCount, double totalAmount);
    }

    private final List<CartChangeListener> listeners = new ArrayList<>();

    private CartManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadCartFromPrefs();
    }

    public static synchronized CartManager getInstance(Context context) {
        if (instance == null) {
            instance = new CartManager(context);
        }
        return instance;
    }

    public void addListener(CartChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(CartChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        int count = getItemCount();
        double total = getSubtotal();
        for (CartChangeListener l : listeners) {
            if (l != null) l.onCartChanged(count, total);
        }
        saveCartToPrefs();
    }

    public void addItem(FoodItem food, int quantity) {
        if (food == null || !food.isAvailable()) return;

        if (cartMap.containsKey(food.getId())) {
            CartItem existing = cartMap.get(food.getId());
            if (existing != null) {
                existing.setQuantity(existing.getQuantity() + quantity);
            }
        } else {
            CartItem item = new CartItem(
                    food.getId(),
                    food.getName(),
                    food.getPrice(),
                    food.getImageUrl(),
                    quantity
            );
            cartMap.put(food.getId(), item);
        }
        notifyListeners();
    }

    public void updateQuantity(String foodId, int quantity) {
        if (quantity <= 0) {
            cartMap.remove(foodId);
        } else if (cartMap.containsKey(foodId)) {
            CartItem item = cartMap.get(foodId);
            if (item != null) item.setQuantity(quantity);
        }
        notifyListeners();
    }

    public void removeItem(String foodId) {
        cartMap.remove(foodId);
        notifyListeners();
    }

    public void clearCart() {
        cartMap.clear();
        notifyListeners();
    }

    public List<CartItem> getCartItems() {
        return new ArrayList<>(cartMap.values());
    }

    public int getItemCount() {
        int count = 0;
        for (CartItem item : cartMap.values()) {
            count += item.getQuantity();
        }
        return count;
    }

    public double getSubtotal() {
        double subtotal = 0;
        for (CartItem item : cartMap.values()) {
            subtotal += item.getItemTotal();
        }
        return subtotal;
    }

    public CartItem getCartItem(String foodId) {
        return cartMap.get(foodId);
    }

    private void saveCartToPrefs() {
        StringBuilder sb = new StringBuilder();
        for (CartItem item : cartMap.values()) {
            sb.append(item.getFoodId()).append("::")
                    .append(item.getFoodName()).append("::")
                    .append(item.getPrice()).append("::")
                    .append(item.getQuantity()).append("::")
                    .append(item.getImageUrl() == null ? "" : item.getImageUrl())
                    .append(";;");
        }
        prefs.edit().putString(KEY_CART_DATA, sb.toString()).apply();
    }

    private void loadCartFromPrefs() {
        cartMap.clear();
        String data = prefs.getString(KEY_CART_DATA, "");
        if (data.isEmpty()) return;

        String[] items = data.split(";;");
        for (String itemStr : items) {
            if (itemStr.trim().isEmpty()) continue;
            String[] parts = itemStr.split("::");
            if (parts.length >= 4) {
                String foodId = parts[0];
                String foodName = parts[1];
                double price = Double.parseDouble(parts[2]);
                int qty = Integer.parseInt(parts[3]);
                String img = parts.length > 4 ? parts[4] : "";
                cartMap.put(foodId, new CartItem(foodId, foodName, price, img, qty));
            }
        }
    }
}
