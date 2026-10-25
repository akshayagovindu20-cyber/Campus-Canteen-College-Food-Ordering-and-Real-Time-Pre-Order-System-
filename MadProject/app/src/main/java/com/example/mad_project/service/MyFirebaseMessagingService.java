package com.example.mad_project.service;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.mad_project.model.AppNotification;
import com.example.mad_project.util.AuthManager;
import com.example.mad_project.util.NotificationHelper;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.UUID;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "CampusCanteenFCM";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM Token: " + token);
        AuthManager.getInstance(getApplicationContext()).saveFcmToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "Campus Canteen Alert";
        String message = "Order status updated";
        String orderId = null;

        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                message = remoteMessage.getNotification().getBody();
            }
        }

        Map<String, String> data = remoteMessage.getData();
        if (data != null && !data.isEmpty()) {
            if (data.containsKey("title")) title = data.get("title");
            if (data.containsKey("message")) message = data.get("message");
            if (data.containsKey("orderId")) orderId = data.get("orderId");
        }

        NotificationHelper.showNotification(getApplicationContext(), title, message, orderId);

        // Save to Firestore notifications collection for in-app Notifications Activity
        String currentUserId = AuthManager.getInstance(getApplicationContext()).getCurrentUserId();
        if (currentUserId != null) {
            String notifId = UUID.randomUUID().toString();
            AppNotification notif = new AppNotification(notifId, currentUserId, orderId, title, message, false, System.currentTimeMillis());
            FirebaseFirestore.getInstance().collection("notifications").document(notifId).set(notif);
        }
    }
}
