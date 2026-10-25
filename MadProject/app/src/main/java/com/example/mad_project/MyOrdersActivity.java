package com.example.mad_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.mad_project.adapter.CustomerOrderAdapter;
import com.example.mad_project.model.Order;
import com.example.mad_project.util.AuthManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class MyOrdersActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private RecyclerView rvMyOrders;
    private LinearLayout layoutEmptyOrders;
    private SwipeRefreshLayout swipeRefresh;

    private CustomerOrderAdapter orderAdapter;
    private final List<Order> orderList = new ArrayList<>();

    private FirebaseFirestore db;
    private AuthManager authManager;
    private ListenerRegistration ordersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_orders);

        db = FirebaseFirestore.getInstance();
        authManager = AuthManager.getInstance(this);

        btnBack = findViewById(R.id.btnBackMyOrders);
        rvMyOrders = findViewById(R.id.rvMyOrders);
        layoutEmptyOrders = findViewById(R.id.layoutEmptyOrders);
        swipeRefresh = findViewById(R.id.swipeRefreshOrders);

        btnBack.setOnClickListener(v -> finish());

        setupRecyclerView();

        swipeRefresh.setOnRefreshListener(this::listenToCustomerOrders);
    }

    private void setupRecyclerView() {
        orderAdapter = new CustomerOrderAdapter(this, new CustomerOrderAdapter.OnOrderClickListener() {
            @Override
            public void onOrderClick(Order order) {
                Intent intent = new Intent(MyOrdersActivity.this, OrderDetailsActivity.class);
                intent.putExtra("ORDER_ID", order.getOrderId());
                startActivity(intent);
            }
        });

        rvMyOrders.setLayoutManager(new LinearLayoutManager(this));
        rvMyOrders.setAdapter(orderAdapter);
    }

    private void listenToCustomerOrders() {
        String userId = authManager.getCurrentUserId();
        if (userId == null) return;

        swipeRefresh.setRefreshing(true);

        ordersListener = db.collection("orders")
                .whereEqualTo("customerId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    swipeRefresh.setRefreshing(false);

                    if (e != null) {
                        Toast.makeText(MyOrdersActivity.this, "Error fetching orders: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    orderList.clear();
                    if (querySnapshot != null) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Order order = doc.toObject(Order.class);
                            if (order != null) {
                                orderList.add(order);
                            }
                        }
                    }

                    orderAdapter.setOrders(orderList);

                    if (orderList.isEmpty()) {
                        layoutEmptyOrders.setVisibility(View.VISIBLE);
                        rvMyOrders.setVisibility(View.GONE);
                    } else {
                        layoutEmptyOrders.setVisibility(View.GONE);
                        rvMyOrders.setVisibility(View.VISIBLE);
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        listenToCustomerOrders();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (ordersListener != null) {
            ordersListener.remove();
        }
    }
}
