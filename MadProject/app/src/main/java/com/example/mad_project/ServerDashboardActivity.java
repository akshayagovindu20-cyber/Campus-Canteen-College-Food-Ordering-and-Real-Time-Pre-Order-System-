package com.example.mad_project;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.mad_project.adapter.ManageMenuAdapter;
import com.example.mad_project.adapter.ServerOrderAdapter;
import com.example.mad_project.model.AppNotification;
import com.example.mad_project.model.FoodItem;
import com.example.mad_project.model.Order;
import com.example.mad_project.util.AuthManager;
import com.example.mad_project.util.NotificationHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ServerDashboardActivity extends AppCompatActivity {

    private MaterialButton btnAddFoodNav;
    private ImageButton btnLogout;
    private TabLayout tabLayout;
    private RecyclerView rvContent;
    private TextView tvEmptyState;
    private SwipeRefreshLayout swipeRefresh;

    private ServerOrderAdapter orderAdapter;
    private ManageMenuAdapter menuAdapter;

    private final List<Order> allOrdersList = new ArrayList<>();
    private final List<Order> filteredOrdersList = new ArrayList<>();
    private final List<FoodItem> foodList = new ArrayList<>();

    private FirebaseFirestore db;
    private AuthManager authManager;
    private ListenerRegistration ordersListener;
    private ListenerRegistration menuListener;

    private int selectedTab = 0; // 0: Live Orders, 1: Ready Orders, 2: Menu, 3: History

    private String lastNotifiedPlacedOrderId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_dashboard);

        db = FirebaseFirestore.getInstance();
        authManager = AuthManager.getInstance(this);

        authManager.fetchAndSaveFcmToken();
        requestNotificationPermission();

        // Security check: verify logged in user is actually SERVER!
        if (!authManager.isServer()) {
            Toast.makeText(this, "Unauthorized access. Server account required.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        setupTabs();
        setupAdapters();
        listenToData();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void initViews() {
        btnAddFoodNav = findViewById(R.id.btnAddFoodNav);
        btnLogout = findViewById(R.id.btnServerLogout);
        tabLayout = findViewById(R.id.tabLayoutServer);
        rvContent = findViewById(R.id.rvServerOrders);
        tvEmptyState = findViewById(R.id.tvEmptyServerState);
        swipeRefresh = findViewById(R.id.swipeRefreshServer);

        TextView tvSubtitle = findViewById(R.id.tvServerCanteenSubtitle);
        if (tvSubtitle != null && authManager.getCurrentCanteenName() != null) {
            tvSubtitle.setText(authManager.getCurrentCanteenName() + " • Canteen Server");
        }

        btnAddFoodNav.setOnClickListener(v -> {
            startActivity(new Intent(ServerDashboardActivity.this, AddEditFoodActivity.class));
        });

        btnLogout.setOnClickListener(v -> {
            authManager.logout();
            Toast.makeText(this, "Server logged out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ServerDashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        swipeRefresh.setOnRefreshListener(this::listenToData);
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Live Orders"));
        tabLayout.addTab(tabLayout.newTab().setText("Ready"));
        tabLayout.addTab(tabLayout.newTab().setText("Manage Menu"));
        tabLayout.addTab(tabLayout.newTab().setText("History"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTab = tab.getPosition();
                updateTabContent();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupAdapters() {
        orderAdapter = new ServerOrderAdapter(this, new ServerOrderAdapter.OnServerOrderActionListener() {
            @Override
            public void onOrderClick(Order order) {
                Intent intent = new Intent(ServerDashboardActivity.this, ServerOrderDetailsActivity.class);
                intent.putExtra("ORDER_ID", order.getOrderId());
                startActivity(intent);
            }

            @Override
            public void onUpdateStatus(Order order, String newStatus) {
                updateOrderStatusInFirestore(order, newStatus);
            }
        });

        menuAdapter = new ManageMenuAdapter(this, new ManageMenuAdapter.OnManageFoodActionListener() {
            @Override
            public void onToggleAvailability(FoodItem food, boolean isAvailable) {
                db.collection("food_items").document(food.getId()).update("available", isAvailable);
            }

            @Override
            public void onEditFood(FoodItem food) {
                Intent intent = new Intent(ServerDashboardActivity.this, AddEditFoodActivity.class);
                intent.putExtra("FOOD_ITEM", food);
                startActivity(intent);
            }
        });

        rvContent.setLayoutManager(new LinearLayoutManager(this));
    }

    private void listenToData() {
        swipeRefresh.setRefreshing(true);

        // Listen to orders
        ordersListener = db.collection("orders")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    swipeRefresh.setRefreshing(false);
                    if (e != null) {
                        Toast.makeText(ServerDashboardActivity.this, "Error fetching orders: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    allOrdersList.clear();
                    if (querySnapshot != null) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Order order = doc.toObject(Order.class);
                            if (order != null) {
                                allOrdersList.add(order);

                                // Check if a brand new PLACED order arrived
                                if (Order.STATUS_PLACED.equalsIgnoreCase(order.getStatus()) && !order.getOrderId().equals(lastNotifiedPlacedOrderId)) {
                                    lastNotifiedPlacedOrderId = order.getOrderId();
                                    String shortId = order.getOrderId().substring(0, Math.min(8, order.getOrderId().length())).toUpperCase();
                                    NotificationHelper.showNotification(
                                            ServerDashboardActivity.this,
                                            "New Canteen Order Received!",
                                            "New order #" + shortId + " placed by " + (order.getCustomerName() != null ? order.getCustomerName() : "Customer") + " (Total: ₹" + String.format(Locale.getDefault(), "%.2f", order.getTotalAmount()) + ")",
                                            order.getOrderId()
                                    );
                                }
                            }
                        }
                    }
                    if (selectedTab != 2) {
                        updateTabContent();
                    }
                });

        // Listen to menu items
        menuListener = db.collection("food_items")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e == null && querySnapshot != null) {
                        foodList.clear();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            FoodItem item = doc.toObject(FoodItem.class);
                            if (item != null) {
                                item.setId(doc.getId());
                                foodList.add(item);
                            }
                        }
                        if (selectedTab == 2) {
                            updateTabContent();
                        }
                    }
                });
    }

    private void updateTabContent() {
        if (selectedTab == 2) { // Manage Menu Tab
            rvContent.setAdapter(menuAdapter);
            menuAdapter.setFoodList(foodList);

            if (foodList.isEmpty()) {
                tvEmptyState.setText("No food items found. Click '+ Add Food' above.");
                tvEmptyState.setVisibility(View.VISIBLE);
                rvContent.setVisibility(View.GONE);
            } else {
                tvEmptyState.setVisibility(View.GONE);
                rvContent.setVisibility(View.VISIBLE);
            }
            return;
        }

        // Order Tabs
        rvContent.setAdapter(orderAdapter);
        filteredOrdersList.clear();

        for (Order order : allOrdersList) {
            String status = order.getStatus() != null ? order.getStatus().toUpperCase() : Order.STATUS_PLACED;
            if (selectedTab == 0) { // Live Orders
                if (Order.STATUS_PLACED.equalsIgnoreCase(status) || Order.STATUS_PREPARING.equalsIgnoreCase(status)) {
                    filteredOrdersList.add(order);
                }
            } else if (selectedTab == 1) { // Ready Orders
                if (Order.STATUS_READY.equalsIgnoreCase(status)) {
                    filteredOrdersList.add(order);
                }
            } else if (selectedTab == 3) { // History
                if (Order.STATUS_COMPLETED.equalsIgnoreCase(status) || Order.STATUS_CANCELLED.equalsIgnoreCase(status)) {
                    filteredOrdersList.add(order);
                }
            }
        }

        orderAdapter.setOrders(filteredOrdersList);

        if (filteredOrdersList.isEmpty()) {
            if (selectedTab == 0) tvEmptyState.setText("No active or incoming live orders.");
            else if (selectedTab == 1) tvEmptyState.setText("No orders waiting for collection.");
            else tvEmptyState.setText("No completed or cancelled order history.");
            tvEmptyState.setVisibility(View.VISIBLE);
            rvContent.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvContent.setVisibility(View.VISIBLE);
        }
    }

    private void updateOrderStatusInFirestore(Order order, String newStatus) {
        db.collection("orders").document(order.getOrderId())
                .update("status", newStatus, "updatedAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ServerDashboardActivity.this, "Order #" + order.getOrderId().substring(0, Math.min(8, order.getOrderId().length())) + " marked " + newStatus, Toast.LENGTH_SHORT).show();

                    // Send In-App Notification record to customer
                    sendCustomerNotification(order, newStatus);
                })
                .addOnFailureListener(e -> Toast.makeText(ServerDashboardActivity.this, "Failed to update order: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
        if (ordersListener != null) ordersListener.remove();
        if (menuListener != null) menuListener.remove();
    }
}
