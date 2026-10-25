package com.example.mad_project;

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
import com.example.mad_project.model.Order;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Date;
import java.util.Locale;

public class OrderDetailsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvStatusBadge, tvStatusDescription, tvTotal, tvFullOrderId, tvOrderTime, tvPickupSlot;
    private RecyclerView rvSnapshotItems;

    // Hero Ready Card
    private MaterialCardView cardReadyHero;
    private TextView tvHeroOrderId;

    private OrderItemAdapter adapter;
    private FirebaseFirestore db;
    private ListenerRegistration orderListener;
    private String orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        db = FirebaseFirestore.getInstance();

        if (getIntent() != null && getIntent().hasExtra("ORDER_ID")) {
            orderId = getIntent().getStringExtra("ORDER_ID");
        }

        if (orderId == null || orderId.isEmpty()) {
            Toast.makeText(this, "Order ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        listenToOrderDetails();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackOrderDetails);
        tvStatusBadge = findViewById(R.id.tvDetailStatusBadge);
        tvStatusDescription = findViewById(R.id.tvStatusDescription);
        tvTotal = findViewById(R.id.tvDetailTotal);
        tvFullOrderId = findViewById(R.id.tvDetailFullOrderId);
        tvOrderTime = findViewById(R.id.tvDetailOrderTime);
        tvPickupSlot = findViewById(R.id.tvDetailPickupSlot);
        rvSnapshotItems = findViewById(R.id.rvOrderSnapshotItems);

        cardReadyHero = findViewById(R.id.cardOrderReadyHero);
        tvHeroOrderId = findViewById(R.id.tvHeroOrderId);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new OrderItemAdapter(this, null);
        rvSnapshotItems.setLayoutManager(new LinearLayoutManager(this));
        rvSnapshotItems.setAdapter(adapter);
    }

    private void listenToOrderDetails() {
        orderListener = db.collection("orders").document(orderId)
                .addSnapshotListener((doc, e) -> {
                    if (e != null) {
                        Toast.makeText(OrderDetailsActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (doc != null && doc.exists()) {
                        Order order = doc.toObject(Order.class);
                        if (order != null) {
                            bindOrderData(order);
                        }
                    } else {
                        Toast.makeText(OrderDetailsActivity.this, "Order not found", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void bindOrderData(Order order) {
        String status = order.getStatus() != null ? order.getStatus().toUpperCase() : Order.STATUS_PLACED;
        tvStatusBadge.setText(status);

        String shortId = order.getOrderId() != null ? order.getOrderId().substring(0, Math.min(8, order.getOrderId().length())).toUpperCase() : "";
        tvFullOrderId.setText("Order ID: " + order.getOrderId());
        tvHeroOrderId.setText("Order #" + shortId);

        if (order.getCreatedAt() > 0) {
            String timeStr = DateFormat.format("dd MMM yyyy, hh:mm a", new Date(order.getCreatedAt())).toString();
            tvOrderTime.setText("Order Time: " + timeStr);
        }

        tvPickupSlot.setText("Pickup Slot: " + (order.getPickupTime() != null ? order.getPickupTime() : "ASAP"));
        tvTotal.setText(String.format(Locale.getDefault(), "₹ %.2f", order.getTotalAmount()));

        if (order.getItems() != null) {
            adapter.setItems(order.getItems());
        }

        if (Order.STATUS_READY.equalsIgnoreCase(status)) {
            // PROMINENT ORDER READY HERO BANNER VISIBLE
            cardReadyHero.setVisibility(View.VISIBLE);
            tvStatusBadge.setTextColor(getColor(R.color.canteen_green));
            tvStatusBadge.getBackground().setTint(getColor(R.color.status_ready_bg));
            tvStatusDescription.setText("YOUR ORDER IS READY! Please collect your meal from Counter 1 at the College Canteen.");
        } else {
            cardReadyHero.setVisibility(View.GONE);
            switch (status) {
                case Order.STATUS_PREPARING:
                    tvStatusBadge.setTextColor(getColor(R.color.status_preparing));
                    tvStatusBadge.getBackground().setTint(getColor(R.color.status_preparing_bg));
                    tvStatusDescription.setText("The canteen staff is currently preparing your fresh food order.");
                    break;
                case Order.STATUS_COMPLETED:
                    tvStatusBadge.setTextColor(getColor(R.color.status_completed));
                    tvStatusBadge.getBackground().setTint(getColor(R.color.status_completed_bg));
                    tvStatusDescription.setText("Order completed. Thank you for eating at Campus Canteen!");
                    break;
                case Order.STATUS_CANCELLED:
                    tvStatusBadge.setTextColor(getColor(R.color.status_cancelled));
                    tvStatusBadge.getBackground().setTint(getColor(R.color.status_cancelled_bg));
                    tvStatusDescription.setText("This order was cancelled. " + (order.getCancelReason() != null ? "Reason: " + order.getCancelReason() : ""));
                    break;
                default: // PLACED
                    tvStatusBadge.setTextColor(getColor(R.color.status_placed));
                    tvStatusBadge.getBackground().setTint(getColor(R.color.status_placed_bg));
                    tvStatusDescription.setText("Your order has been placed successfully and sent to the canteen server.");
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (orderListener != null) {
            orderListener.remove();
        }
    }
}
