package com.example.mad_project.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.example.mad_project.model.Canteen;
import com.example.mad_project.model.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.UUID;

public class AuthManager {
    private static final String PREF_NAME = "campus_canteen_prefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_PHONE_NUMBER = "phone_number";
    private static final String KEY_ROLE = "role";
    private static final String KEY_CANTEEN_NAME = "canteen_name";
    private static final String KEY_CANTEEN_IMAGE_URL = "canteen_image_url";
    private static final String KEY_FCM_TOKEN = "fcm_token";

    public static final String SERVER_USERNAME = "server";
    public static final String DEFAULT_CANTEEN_IMAGE = "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=500";

    private static AuthManager instance;
    private final SharedPreferences prefs;
    private final FirebaseFirestore db;

    public interface AuthCallback {
        void onSuccess(User user);
        void onError(String errorMessage);
    }

    private AuthManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context);
        }
        return instance;
    }

    public boolean isLoggedIn() {
        return prefs.contains(KEY_USER_ID) && prefs.getString(KEY_USER_ID, null) != null;
    }

    public String getCurrentUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getCurrentUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public String getCurrentDisplayName() {
        return prefs.getString(KEY_DISPLAY_NAME, "User");
    }

    public String getCurrentUserPhone() {
        return prefs.getString(KEY_PHONE_NUMBER, "");
    }

    public String getCurrentRole() {
        return prefs.getString(KEY_ROLE, User.ROLE_CUSTOMER);
    }

    public String getCurrentCanteenName() {
        return prefs.getString(KEY_CANTEEN_NAME, "Campus Canteen");
    }

    public String getCurrentCanteenImageUrl() {
        return prefs.getString(KEY_CANTEEN_IMAGE_URL, DEFAULT_CANTEEN_IMAGE);
    }

    public boolean isServer() {
        return User.ROLE_SERVER.equalsIgnoreCase(getCurrentRole());
    }

    public void saveUserSession(User user) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_USER_ID, user.getUserId());
        editor.putString(KEY_USERNAME, user.getUsername());
        editor.putString(KEY_DISPLAY_NAME, user.getDisplayName());
        editor.putString(KEY_PHONE_NUMBER, user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
        editor.putString(KEY_ROLE, user.getRole());
        editor.putString(KEY_CANTEEN_NAME, user.getCanteenName() != null ? user.getCanteenName() : "Campus Canteen");
        editor.putString(KEY_CANTEEN_IMAGE_URL, user.getCanteenImageUrl() != null && !user.getCanteenImageUrl().isEmpty() ? user.getCanteenImageUrl() : DEFAULT_CANTEEN_IMAGE);
        editor.apply();

        fetchAndSaveFcmToken();
    }

    public void fetchAndSaveFcmToken() {
        String userId = getCurrentUserId();
        if (userId == null) return;

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (token != null && !token.isEmpty()) {
                        saveFcmToken(token);
                    }
                });
    }

    public void saveFcmToken(String token) {
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply();
        String userId = getCurrentUserId();
        if (userId != null) {
            db.collection("users").document(userId).update("fcmToken", token);
        }
    }

    public void logout() {
        prefs.edit().clear().apply();
    }

    public void registerUser(String fullName, String phoneNumber, String password, String role, String canteenName, String canteenImageUrl, AuthCallback callback) {
        String displayName = fullName.trim();
        String phone = phoneNumber != null ? phoneNumber.trim() : "";
        String normalizedUsername = displayName.toLowerCase().replaceAll("\\s+", "_");

        if (displayName.isEmpty()) {
            callback.onError("Full name is required");
            return;
        }

        if (phone.isEmpty()) {
            callback.onError("Phone number is required");
            return;
        }

        if (password == null || password.length() < 6) {
            callback.onError("Password must be at least 6 characters long");
            return;
        }

        boolean isServerRole = User.ROLE_SERVER.equalsIgnoreCase(role);
        String assignedCanteen = isServerRole ? (canteenName != null && !canteenName.trim().isEmpty() ? canteenName.trim() : displayName + "'s Canteen") : "";
        String assignedCanteenImg = isServerRole ? (canteenImageUrl != null && !canteenImageUrl.trim().isEmpty() ? canteenImageUrl.trim() : DEFAULT_CANTEEN_IMAGE) : "";

        // Query Firestore to check username uniqueness
        db.collection("users")
                .whereEqualTo("username", normalizedUsername)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String uniqueUsername = normalizedUsername + "_" + System.currentTimeMillis() % 1000;
                        createUserRecord(displayName, uniqueUsername, phone, password, role, assignedCanteen, assignedCanteenImg, callback);
                    } else {
                        createUserRecord(displayName, normalizedUsername, phone, password, role, assignedCanteen, assignedCanteenImg, callback);
                    }
                })
                .addOnFailureListener(e -> callback.onError("Database error: " + e.getMessage()));
    }

    public void login(String nameOrUsername, String password, AuthCallback callback) {
        String input = nameOrUsername.trim().toLowerCase().replaceAll("\\s+", "_");
        String rawInputName = nameOrUsername.trim();

        if (input.isEmpty() || password.isEmpty()) {
            callback.onError("Name/Username and password are required");
            return;
        }

        // First check by username
        db.collection("users")
                .whereEqualTo("username", input)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        verifyUserAndLogin(doc, password, callback);
                    } else {
                        // Try searching by displayName if not found by username
                        db.collection("users")
                                .whereEqualTo("displayName", rawInputName)
                                .get()
                                .addOnSuccessListener(snapshots -> {
                                    if (!snapshots.isEmpty()) {
                                        DocumentSnapshot doc = snapshots.getDocuments().get(0);
                                        verifyUserAndLogin(doc, password, callback);
                                    } else {
                                        // Fallback default server seed check
                                        if (SERVER_USERNAME.equalsIgnoreCase(input) || "server".equalsIgnoreCase(rawInputName)) {
                                            ensureServerAccountExistsAndLogin(password, callback);
                                        } else {
                                            callback.onError("Account not found. Please register first.");
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> callback.onError("Login error: " + e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> callback.onError("Login error: " + e.getMessage()));
    }

    public void ensureServerAccountExists(Runnable onComplete) {
        db.collection("users")
                .whereEqualTo("username", SERVER_USERNAME)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        seedAllDefaultServerAccounts(onComplete);
                    } else {
                        if (onComplete != null) onComplete.run();
                    }
                })
                .addOnFailureListener(e -> {
                    if (onComplete != null) onComplete.run();
                });
    }

    private void seedAllDefaultServerAccounts(Runnable onComplete) {
        String salt = generateSalt();
        String passwordHash = hashPassword("server123", salt);

        User mainServer = new User("server_main_id", "server", "Main Canteen Staff", "9876543210", User.ROLE_SERVER, "Main College Canteen", DEFAULT_CANTEEN_IMAGE, passwordHash, salt, System.currentTimeMillis());
        User southServer = new User("server_south_id", "server_south", "South Canteen Staff", "9876543211", User.ROLE_SERVER, "South Block Canteen", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=500", passwordHash, salt, System.currentTimeMillis());
        User enggServer = new User("server_engg_id", "server_engg", "Engg Canteen Staff", "9876543212", User.ROLE_SERVER, "Engineering Block Canteen", "https://images.unsplash.com/photo-1552566626-52f8b828add9?w=500", passwordHash, salt, System.currentTimeMillis());
        User juiceServer = new User("server_juice_id", "server_juice", "Juice Bar Staff", "9876543213", User.ROLE_SERVER, "Juice & Snack Corner", "https://images.unsplash.com/photo-1534353473418-4cfa6c56fd38?w=500", passwordHash, salt, System.currentTimeMillis());

        db.collection("users").document(mainServer.getUserId()).set(mainServer);
        db.collection("users").document(southServer.getUserId()).set(southServer);
        db.collection("users").document(enggServer.getUserId()).set(enggServer);
        db.collection("users").document(juiceServer.getUserId()).set(juiceServer);

        // Also seed canteens collection
        Canteen c1 = new Canteen("canteen_main", "Main College Canteen", "Ground Floor, Admin Block", "South Indian, Thalis, Snacks & Fresh Juices", DEFAULT_CANTEEN_IMAGE, true);
        Canteen c2 = new Canteen("canteen_south", "South Block Canteen", "Ground Floor, South Block", "Dosa, Parathas, Tea, Coffee & Quick Bites", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=500", true);
        Canteen c3 = new Canteen("canteen_engg", "Engineering Block Canteen", "Block B, Near Computer Lab", "Burgers, Sandwiches, Rolls & Cold Beverages", "https://images.unsplash.com/photo-1552566626-52f8b828add9?w=500", true);
        Canteen c4 = new Canteen("canteen_juice", "Juice & Snack Corner", "Central Lawn - Kiosk 3", "Fresh Fruit Juices, Smoothies, Brownies & Shakes", "https://images.unsplash.com/photo-1534353473418-4cfa6c56fd38?w=500", true);

        db.collection("canteens").document(c1.getCanteenId()).set(c1);
        db.collection("canteens").document(c2.getCanteenId()).set(c2);
        db.collection("canteens").document(c3.getCanteenId()).set(c3);
        db.collection("canteens").document(c4.getCanteenId()).set(c4);

        if (onComplete != null) onComplete.run();
    }

    private void ensureServerAccountExistsAndLogin(String password, AuthCallback callback) {
        ensureServerAccountExists(() -> {
            db.collection("users")
                    .whereEqualTo("username", SERVER_USERNAME)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                            verifyUserAndLogin(doc, password, callback);
                        } else {
                            callback.onError("Server account configuration failed.");
                        }
                    })
                    .addOnFailureListener(e -> callback.onError("Server login failed: " + e.getMessage()));
        });
    }

    private void verifyUserAndLogin(DocumentSnapshot doc, String password, AuthCallback callback) {
        User user = doc.toObject(User.class);
        if (user == null) {
            callback.onError("Invalid user record.");
            return;
        }

        String salt = user.getSalt();
        String storedHash = user.getPasswordHash();
        String computedHash = hashPassword(password, salt);

        if (storedHash != null && storedHash.equals(computedHash)) {
            saveUserSession(user);
            callback.onSuccess(user);
        } else {
            callback.onError("Invalid password.");
        }
    }

    private void createUserRecord(String displayName, String username, String phoneNumber, String password, String role, String canteenName, String canteenImageUrl, AuthCallback callback) {
        String salt = generateSalt();
        String passwordHash = hashPassword(password, salt);
        String userId = UUID.randomUUID().toString();

        User user = new User(userId, username, displayName, phoneNumber, role, canteenName, canteenImageUrl, passwordHash, salt, System.currentTimeMillis());

        db.collection("users").document(userId).set(user)
                .addOnSuccessListener(aVoid -> {
                    // If server role, also publish to canteens collection
                    if (User.ROLE_SERVER.equalsIgnoreCase(role)) {
                        String canteenId = "canteen_" + userId;
                        Canteen canteen = new Canteen(canteenId, canteenName, "Campus Block - " + displayName, "Fresh food & pre-orders", canteenImageUrl, true);
                        db.collection("canteens").document(canteenId).set(canteen);
                    }
                    saveUserSession(user);
                    callback.onSuccess(user);
                })
                .addOnFailureListener(e -> callback.onError("Registration failed: " + e.getMessage()));
    }

    public static String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }

    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes());
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return password;
        }
    }
}
