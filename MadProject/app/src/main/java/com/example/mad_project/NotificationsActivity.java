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

import com.example.mad_project.adapter.NotificationAdapter;
import com.example.mad_project.model.AppNotification;
import com.example.mad_project.util.AuthManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private RecyclerView rvNotifications;
    private LinearLayout layoutEmptyNotif;

    private NotificationAdapter adapter;
    private final List<AppNotification> notifList = new ArrayList<>();

    private FirebaseFirestore db;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        db = FirebaseFirestore.getInstance();
        authManager = AuthManager.getInstance(this);

        btnBack = findViewById(R.id.btnBackNotif);
        rvNotifications = findViewById(R.id.rvNotifications);
        layoutEmptyNotif = findViewById(R.id.layoutEmptyNotif);

        btnBack.setOnClickListener(v -> finish());

        setupRecyclerView();
        loadNotifications();
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(this, notification -> {
            if (notification.getOrderId() != null && !notification.getOrderId().isEmpty()) {
                Intent intent = new Intent(NotificationsActivity.this, OrderDetailsActivity.class);
                intent.putExtra("ORDER_ID", notification.getOrderId());
                startActivity(intent);
            }
        });

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);
    }

    private void loadNotifications() {
        String userId = authManager.getCurrentUserId();
        if (userId == null) return;

        db.collection("notifications")
                .whereEqualTo("recipientId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(NotificationsActivity.this, "Error loading notifications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    notifList.clear();
                    if (querySnapshot != null) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            AppNotification notif = doc.toObject(AppNotification.class);
                            if (notif != null) {
                                notifList.add(notif);
                            }
                        }
                    }

                    adapter.setNotifications(notifList);

                    if (notifList.isEmpty()) {
                        layoutEmptyNotif.setVisibility(View.VISIBLE);
                        rvNotifications.setVisibility(View.GONE);
                    } else {
                        layoutEmptyNotif.setVisibility(View.GONE);
                        rvNotifications.setVisibility(View.VISIBLE);
                    }
                });
    }
}
