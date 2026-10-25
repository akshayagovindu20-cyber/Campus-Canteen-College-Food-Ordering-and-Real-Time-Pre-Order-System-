package com.example.mad_project;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_project.adapter.OrderItemAdapter;
import com.example.mad_project.model.AppNotification;
import com.example.mad_project.model.Order;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class ServerOrderDetailsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvOrderId, tvStatusBadge, tvCustomerName, tvCustomerPhone, tvPickupSlot, tvTotal;
    private RecyclerView rvSnapshot;
    private MaterialButton btnCallStudent, btnPreparing, btnReady, btnCompleted, btnCancel;

    private OrderItemAdapter adapter;
    private FirebaseFirestore db;
    private ListenerRegistration orderListener;
    private String orderId;
    private Order currentOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_order_details);

        db = FirebaseFirestore.getInstance();

        if (getIntent() != null && getIntent().hasExtra("ORDER_ID")) {
            orderId = getIntent().getStringExtra("ORDER_ID");
        }

        if (orderId == null || orderId.isEmpty()) {
            Toast.makeText(this, "Missing Order ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        listenToOrder();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackServerDetails);
        tvOrderId = findViewById(R.id.tvServerDetailOrderId);
        tvStatusBadge = findViewById(R.id.tvServerDetailStatusBadge);
        tvCustomerName = findViewById(R.id.tvServerDetailCustomer);
        tvCustomerPhone = findViewById(R.id.tvServerDetailPhone);
        tvPickupSlot = findViewById(R.id.tvServerDetailPickup);
        tvTotal = findViewById(R.id.tvServerDetailTotal);
        rvSnapshot = findViewById(R.id.rvServerDetailSnapshot);

        btnCallStudent = findViewById(R.id.btnServerDetailCall);
        btnPreparing = findViewById(R.id.btnDetailStatusPreparing);
        btnReady = findViewById(R.id.btnDetailStatusReady);
        btnCompleted = findViewById(R.id.btnDetailStatusCompleted);
        btnCancel = findViewById(R.id.btnDetailStatusCancel);

        btnBack.setOnClickListener(v -> finish());

        btnPreparing.setOnClickListener(v -> updateOrderStatus(Order.STATUS_PREPARING));
        btnReady.setOnClickListener(v -> updateOrderStatus(Order.STATUS_READY));
        btnCompleted.setOnClickListener(v -> updateOrderStatus(Order.STATUS_COMPLETED));
        btnCancel.setOnClickListener(v -> updateOrderStatus(Order.STATUS_CANCELLED));
    }

    private void setupRecyclerView() {
        adapter = new OrderItemAdapter(this, null);
        rvSnapshot.setLayoutManager(new LinearLayoutManager(this));
        rvSnapshot.setAdapter(adapter);
    }

    private void listenToOrder() {
        orderListener = db.collection("orders").document(orderId)
                .addSnapshotListener((doc, e) -> {
                    if (e != null) {
                        Toast.makeText(ServerOrderDetailsActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (doc != null && doc.exists()) {
                        currentOrder = doc.toObject(Order.class);
                        if (currentOrder != null) {
                            bindOrderData(currentOrder);
                        }
                    } else {
                        Toast.makeText(ServerOrderDetailsActivity.this, "Order not found", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void bindOrderData(Order order) {
        String shortId = order.getOrderId() != null ? order.getOrderId().substring(0, Math.min(8, order.getOrderId().length())).toUpperCase() : "";
        tvOrderId.setText("Order #" + shortId);

        String status = order.getStatus() != null ? order.getStatus().toUpperCase() : Order.STATUS_PLACED;
        tvStatusBadge.setText(status);

        tvCustomerName.setText("Customer Name: " + (order.getCustomerName() != null ? order.getCustomerName() : "Student"));

        String phone = order.getCustomerPhone() != null && !order.getCustomerPhone().isEmpty() ? order.getCustomerPhone() : "N/A";
        tvCustomerPhone.setText("Phone: 📞 " + phone);

        if (!"N/A".equals(phone)) {
            btnCallStudent.setVisibility(View.VISIBLE);
            btnCallStudent.setOnClickListener(v -> {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                startActivity(dialIntent);
            });
        } else {
            btnCallStudent.setVisibility(View.GONE);
        }

        String timeStr = order.getCreatedAt() > 0 ? DateFormat.format("hh:mm a", new Date(order.getCreatedAt())).toString() : "Recent";
        String slotStr = order.getPickupTime() != null ? order.getPickupTime() : "ASAP";
        tvPickupSlot.setText("Received at " + timeStr + "  |  Slot: " + slotStr);

        tvTotal.setText(String.format(Locale.getDefault(), "₹ %.2f", order.getTotalAmount()));

        if (order.getItems() != null) {
            adapter.setItems(order.getItems());
        }

        switch (status) {
            case Order.STATUS_PLACED:
                tvStatusBadge.setTextColor(getColor(R.color.status_placed));
                tvStatusBadge.getBackground().setTint(getColor(R.color.status_placed_bg));
                break;
            case Order.STATUS_PREPARING:
                tvStatusBadge.setTextColor(getColor(R.color.status_preparing));
                tvStatusBadge.getBackground().setTint(getColor(R.color.status_preparing_bg));
                break;
            case Order.STATUS_READY:
                tvStatusBadge.setTextColor(getColor(R.color.canteen_green));
                tvStatusBadge.getBackground().setTint(getColor(R.color.status_ready_bg));
                break;
            default:
                if (Order.STATUS_COMPLETED.equalsIgnoreCase(status)) {
                    tvStatusBadge.setTextColor(getColor(R.color.status_completed));
                    tvStatusBadge.getBackground().setTint(getColor(R.color.status_completed_bg));
                } else {
                    tvStatusBadge.setTextColor(getColor(R.color.status_cancelled));
                    tvStatusBadge.getBackground().setTint(getColor(R.color.status_cancelled_bg));
                }
                break;
        }
    }

    private void updateOrderStatus(String newStatus) {
        if (currentOrder == null) return;

        db.collection("orders").document(orderId)
                .update("status", newStatus, "updatedAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ServerOrderDetailsActivity.this, "Order marked as " + newStatus, Toast.LENGTH_SHORT).show();
                    sendCustomerNotification(currentOrder, newStatus);
                })
                .addOnFailureListener(e -> Toast.makeText(ServerOrderDetailsActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void sendCustomerNotification(Order order, String status) {
        if (order.getCustomerId() == null) return;

        String title;
        String message;

        if (Order.STATUS_READY.equalsIgnoreCase(status)) {
            title = "Your Order Is Ready!";
            message = "Your order #" + order.getOrderId().substring(0, Math.min(8, order.getOrderId().length())).toUpperCase() + " is ready for collection at Counter 1 in the College Canteen!";
        } else if (Order.STATUS_PREPARING.equalsIgnoreCase(status)) {
            title = "Order Accepted";
            message = "The canteen server is now preparing your order #" + order.getOrderId().substring(0, Math.min(8, order.getOrderId().length())).toUpperCase();
        } else if (Order.STATUS_COMPLETED.equalsIgnoreCase(status)) {
            title = "Order Completed";
            message = "Thank you for collecting your order! Enjoy your meal.";
        } else {
            title = "Order Cancelled";
            message = "Your order #" + order.getOrderId().substring(0, Math.min(8, order.getOrderId().length())).toUpperCase() + " was cancelled.";
        }

        String notifId = UUID.randomUUID().toString();
        AppNotification notif = new AppNotification(notifId, order.getCustomerId(), order.getOrderId(), title, message, false, System.currentTimeMillis());
        db.collection("notifications").document(notifId).set(notif);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (orderListener != null) {
            orderListener.remove();
        }
    }
}
