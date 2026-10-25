package com.example.mad_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_project.adapter.OrderItemAdapter;
import com.example.mad_project.model.CartItem;
import com.example.mad_project.model.Order;
import com.example.mad_project.model.OrderItem;
import com.example.mad_project.util.AuthManager;
import com.example.mad_project.util.CartManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class CheckoutActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private RecyclerView rvCheckoutItems;
    private Spinner spinnerPickupSlot;
    private TextView tvCheckoutTotal;
    private MaterialButton btnPlaceOrder;
    private ProgressBar progressBar;

    private OrderItemAdapter orderItemAdapter;
    private CartManager cartManager;
    private AuthManager authManager;
    private FirebaseFirestore db;

    private boolean isSubmitting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        cartManager = CartManager.getInstance(this);
        authManager = AuthManager.getInstance(this);
        db = FirebaseFirestore.getInstance();

        if (cartManager.getItemCount() == 0) {
            Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupPickupSpinner();
        setupItemsRecyclerView();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackCheckout);
        rvCheckoutItems = findViewById(R.id.rvCheckoutItems);
        spinnerPickupSlot = findViewById(R.id.spinnerPickupSlot);
        tvCheckoutTotal = findViewById(R.id.tvCheckoutTotal);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);
        progressBar = findViewById(R.id.progressCheckout);

        btnBack.setOnClickListener(v -> finish());

        double total = cartManager.getSubtotal();
        tvCheckoutTotal.setText(String.format(Locale.getDefault(), "₹ %.2f", total));

        btnPlaceOrder.setOnClickListener(v -> placeOrder());
    }

    private void setupPickupSpinner() {
        List<String> slots = Arrays.asList(
                "ASAP (10-15 mins)",
                "12:30 PM Slot",
                "01:00 PM Slot",
                "01:30 PM Slot",
                "02:00 PM Slot",
                "04:30 PM Evening Snack Slot"
        );
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, slots);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerPickupSlot.setAdapter(adapter);
    }

    private void setupItemsRecyclerView() {
        List<OrderItem> orderItems = convertCartToOrderItems(cartManager.getCartItems());
        orderItemAdapter = new OrderItemAdapter(this, orderItems);
        rvCheckoutItems.setLayoutManager(new LinearLayoutManager(this));
        rvCheckoutItems.setAdapter(orderItemAdapter);
    }

    private List<OrderItem> convertCartToOrderItems(List<CartItem> cartItems) {
        List<OrderItem> list = new ArrayList<>();
        for (CartItem ci : cartItems) {
            list.add(new OrderItem(
                    ci.getFoodId(),
                    ci.getFoodName(),
                    ci.getPrice(),
                    ci.getQuantity(),
                    ci.getImageUrl()
            ));
        }
        return list;
    }

    private void placeOrder() {
        if (isSubmitting) return; // Prevent double taps

        List<CartItem> currentCart = cartManager.getCartItems();
        if (currentCart.isEmpty()) {
            Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        isSubmitting = true;
        setLoading(true);

        String orderId = "ord_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 4);
        String customerId = authManager.getCurrentUserId();
        String customerName = authManager.getCurrentDisplayName();
        String customerPhone = authManager.getCurrentUserPhone();
        String pickupSlot = spinnerPickupSlot.getSelectedItem() != null ? spinnerPickupSlot.getSelectedItem().toString() : "ASAP";
        double totalAmount = cartManager.getSubtotal();

        List<OrderItem> itemsSnapshot = convertCartToOrderItems(currentCart);

        String serverId = "";
        String canteenName = "College Canteen";

        Order newOrder = new Order(
                orderId,
                customerId,
                customerName,
                customerPhone,
                serverId,
                canteenName,
                itemsSnapshot,
                totalAmount,
                Order.STATUS_PLACED,
                pickupSlot,
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        db.collection("orders").document(orderId).set(newOrder)
                .addOnSuccessListener(aVoid -> {
                    cartManager.clearCart();
                    isSubmitting = false;
                    setLoading(false);

                    Toast.makeText(CheckoutActivity.this, "Order Placed Successfully! Order #" + orderId.substring(0, 8), Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(CheckoutActivity.this, OrderDetailsActivity.class);
                    intent.putExtra("ORDER_ID", orderId);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    isSubmitting = false;
                    setLoading(false);
                    Toast.makeText(CheckoutActivity.this, "Failed to place order: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnPlaceOrder.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnPlaceOrder.setEnabled(true);
        }
    }
}
