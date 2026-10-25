package com.example.mad_project;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.mad_project.adapter.CategoryAdapter;
import com.example.mad_project.adapter.FoodAdapter;
import com.example.mad_project.model.FoodItem;
import com.example.mad_project.model.Order;
import com.example.mad_project.util.AuthManager;
import com.example.mad_project.util.CartManager;
import com.example.mad_project.util.NotificationHelper;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CustomerDashboardActivity extends AppCompatActivity implements CartManager.CartChangeListener {

    private TextView tvWelcomeUser, tvCartBadge, tvNotifBadge, tvEmptyMenu;
    private ImageButton btnMyOrdersNav, btnProfileNav;
    private RelativeLayout btnCartNav, btnNotificationsNav;
    private EditText etSearchFood;
    private RecyclerView rvCategories, rvFoodMenu;
    private SwipeRefreshLayout swipeRefresh;

    // Ready Order Banner
    private MaterialCardView cardActiveReadyOrder;
    private TextView tvActiveReadyOrderId, tvActiveReadyDetails;

    private FoodAdapter foodAdapter;
    private CategoryAdapter categoryAdapter;

    private final List<FoodItem> allFoodItems = new ArrayList<>();
    private final List<FoodItem> filteredFoodItems = new ArrayList<>();
    private String selectedCategory = "All";
    private String searchQuery = "";
    private String selectedCanteenName = "Main College Canteen";

    private FirebaseFirestore db;
    private AuthManager authManager;
    private CartManager cartManager;

    private ListenerRegistration readyOrderListener;
    private ListenerRegistration foodMenuListener;
    private String lastNotifiedReadyOrderId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_dashboard);

        db = FirebaseFirestore.getInstance();
        authManager = AuthManager.getInstance(this);
        cartManager = CartManager.getInstance(this);

        authManager.fetchAndSaveFcmToken();
        requestNotificationPermission();

        if (getIntent() != null && getIntent().hasExtra("CANTEEN_NAME")) {
            selectedCanteenName = getIntent().getStringExtra("CANTEEN_NAME");
        }

        initViews();
        setupCategories();
        setupFoodRecyclerView();
        setupListeners();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void initViews() {
        tvWelcomeUser = findViewById(R.id.tvWelcomeUser);
        tvCartBadge = findViewById(R.id.tvCartBadge);
        tvNotifBadge = findViewById(R.id.tvNotifBadge);
        tvEmptyMenu = findViewById(R.id.tvEmptyMenu);

        btnMyOrdersNav = findViewById(R.id.btnMyOrdersNav);
        btnNotificationsNav = findViewById(R.id.btnNotificationsNav);
        btnCartNav = findViewById(R.id.btnCartNav);
        btnProfileNav = findViewById(R.id.btnProfileNav);

        etSearchFood = findViewById(R.id.etSearchFood);
        rvCategories = findViewById(R.id.rvCategories);
        rvFoodMenu = findViewById(R.id.rvFoodMenu);
        swipeRefresh = findViewById(R.id.swipeRefreshCustomer);

        cardActiveReadyOrder = findViewById(R.id.cardActiveReadyOrder);
        tvActiveReadyOrderId = findViewById(R.id.tvActiveReadyOrderId);
        tvActiveReadyDetails = findViewById(R.id.tvActiveReadyDetails);

        String name = authManager.getCurrentDisplayName();
        tvWelcomeUser.setText("Hello, " + name + " 👋");
    }

    private void setupCategories() {
        List<String> categories = Arrays.asList("All", "Breakfast", "Meals", "Snacks", "Beverages", "Desserts");
        categoryAdapter = new CategoryAdapter(this, categories, category -> {
            selectedCategory = category;
            filterFoodItems();
        });
        rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(categoryAdapter);
    }

    private void setupFoodRecyclerView() {
        foodAdapter = new FoodAdapter(this, new FoodAdapter.OnFoodClickListener() {
            @Override
            public void onFoodClick(FoodItem food) {
                Intent intent = new Intent(CustomerDashboardActivity.this, FoodDetailActivity.class);
                intent.putExtra("FOOD_ITEM", food);
                startActivity(intent);
            }

            @Override
            public void onAddToCartClick(FoodItem food) {
                cartManager.addItem(food, 1);
                Toast.makeText(CustomerDashboardActivity.this, food.getName() + " added to cart!", Toast.LENGTH_SHORT).show();
            }
        });

        rvFoodMenu.setLayoutManager(new GridLayoutManager(this, 2));
        rvFoodMenu.setAdapter(foodAdapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadFoodItemsFromFirestore);

        etSearchFood.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim();
                filterFoodItems();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnCartNav.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        btnMyOrdersNav.setOnClickListener(v -> startActivity(new Intent(this, MyOrdersActivity.class)));
        btnNotificationsNav.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));
        btnProfileNav.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
    }

    private void listenToReadyOrders() {
        String userId = authManager.getCurrentUserId();
        if (userId == null) return;

        readyOrderListener = db.collection("orders")
                .whereEqualTo("customerId", userId)
                .whereEqualTo("status", Order.STATUS_READY)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null || querySnapshot == null || querySnapshot.isEmpty()) {
                        cardActiveReadyOrder.setVisibility(View.GONE);
                        return;
                    }

                    DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                    Order readyOrder = doc.toObject(Order.class);
                    if (readyOrder != null) {
                        cardActiveReadyOrder.setVisibility(View.VISIBLE);
                        String shortId = readyOrder.getOrderId() != null ? readyOrder.getOrderId().substring(0, Math.min(8, readyOrder.getOrderId().length())).toUpperCase() : "";
                        tvActiveReadyOrderId.setText("#" + shortId);

                        String details = readyOrder.getFormattedItemsSummary() + " • Total: ₹ " + String.format(Locale.getDefault(), "%.2f", readyOrder.getTotalAmount()) + "\nPlease collect from Counter 1 at Canteen.";
                        tvActiveReadyDetails.setText(details);

                        if (!readyOrder.getOrderId().equals(lastNotifiedReadyOrderId)) {
                            lastNotifiedReadyOrderId = readyOrder.getOrderId();
                            NotificationHelper.showNotification(
                                    CustomerDashboardActivity.this,
                                    "Your Order Is Ready!",
                                    "Your order #" + shortId + " is ready for collection at Counter 1 in the College Canteen!",
                                    readyOrder.getOrderId()
                            );
                        }

                        cardActiveReadyOrder.setOnClickListener(v -> {
                            Intent intent = new Intent(CustomerDashboardActivity.this, OrderDetailsActivity.class);
                            intent.putExtra("ORDER_ID", readyOrder.getOrderId());
                            startActivity(intent);
                        });
                    } else {
                        cardActiveReadyOrder.setVisibility(View.GONE);
                    }
                });
    }

    private void loadFoodItemsFromFirestore() {
        swipeRefresh.setRefreshing(true);
        foodMenuListener = db.collection("food_items")
                .addSnapshotListener((querySnapshot, e) -> {
                    swipeRefresh.setRefreshing(false);
                    if (e != null) {
                        Toast.makeText(CustomerDashboardActivity.this, "Error loading menu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    allFoodItems.clear();
                    if (querySnapshot != null) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            FoodItem item = doc.toObject(FoodItem.class);
                            if (item != null) {
                                item.setId(doc.getId());
                                allFoodItems.add(item);
                            }
                        }
                    }

                    if (allFoodItems.isEmpty()) {
                        seedInitialCanteenMenu();
                    } else {
                        filterFoodItems();
                    }
                });
    }

    private void filterFoodItems() {
        filteredFoodItems.clear();
        for (FoodItem item : allFoodItems) {
            boolean matchesCanteen = selectedCanteenName == null || selectedCanteenName.isEmpty() ||
                    item.getCanteenName() == null || item.getCanteenName().isEmpty() ||
                    item.getCanteenName().equalsIgnoreCase(selectedCanteenName);

            boolean matchesCategory = "All".equalsIgnoreCase(selectedCategory) || (item.getCategory() != null && item.getCategory().equalsIgnoreCase(selectedCategory));
            boolean matchesSearch = searchQuery.isEmpty() || (item.getName() != null && item.getName().toLowerCase().contains(searchQuery.toLowerCase())) || (item.getDescription() != null && item.getDescription().toLowerCase().contains(searchQuery.toLowerCase()));

            if (matchesCanteen && matchesCategory && matchesSearch) {
                filteredFoodItems.add(item);
            }
        }

        foodAdapter.setFoodList(filteredFoodItems);

        if (filteredFoodItems.isEmpty()) {
            tvEmptyMenu.setText("No food items available in " + selectedCanteenName + " for this category.");
            tvEmptyMenu.setVisibility(View.VISIBLE);
            rvFoodMenu.setVisibility(View.GONE);
        } else {
            tvEmptyMenu.setVisibility(View.GONE);
            rvFoodMenu.setVisibility(View.VISIBLE);
        }
    }

    private void seedInitialCanteenMenu() {
        List<FoodItem> initialMenu = Arrays.asList(
                new FoodItem("item_1", "Masala Dosa", "Crispy golden dosa filled with spiced potato masala, served with sambar & coconut chutney.", "Breakfast", 60.0, "https://images.unsplash.com/photo-1668236543090-82eba5ee5976?w=500", true, -1, "server_master_id", "Main College Canteen", System.currentTimeMillis(), System.currentTimeMillis()),
                new FoodItem("item_2", "Idli Sambar", "3 steamed fluffy rice cakes served with hot lentil sambar & fresh coconut chutney.", "Breakfast", 40.0, "https://images.unsplash.com/photo-1589301760014-d929f3979dbc?w=500", true, -1, "server_master_id", "Main College Canteen", System.currentTimeMillis(), System.currentTimeMillis()),
                new FoodItem("item_3", "South Indian Thali", "Full meal with Chapati, Steamed Rice, Sambar, Rasam, Paneer Curry, Papad & Sweet Gulab Jamun.", "Meals", 120.0, "https://images.unsplash.com/photo-1610192244261-3f33de3f55e4?w=500", true, -1, "server_master_id", "Main College Canteen", System.currentTimeMillis(), System.currentTimeMillis()),
                new FoodItem("item_4", "Veg Cheese Burger", "Juicy vegetable patty topped with melted cheese, lettuce and signature mayo.", "Snacks", 70.0, "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500", true, -1, "server_master_id", "Main College Canteen", System.currentTimeMillis(), System.currentTimeMillis()),
                new FoodItem("item_5", "Paneer Grilled Sandwich", "Crispy toasted bread stuffed with spiced paneer, onions, capsicum & green chutney.", "Snacks", 65.0, "https://images.unsplash.com/photo-1528735602780-2552fd46c7af?w=500", true, -1, "server_master_id", "Main College Canteen", System.currentTimeMillis(), System.currentTimeMillis()),
                new FoodItem("item_6", "Chilled Cold Coffee", "Rich espresso blended with chilled milk, ice cream and chocolate drizzle.", "Beverages", 50.0, "https://images.unsplash.com/photo-1517701604599-bb29b565090c?w=500", true, -1, "server_master_id", "Main College Canteen", System.currentTimeMillis(), System.currentTimeMillis()),
                new FoodItem("item_7", "Masala Chai", "Hot aromatic Indian tea brewed with cardamom, ginger and spices.", "Beverages", 15.0, "https://images.unsplash.com/photo-1571934811356-5cc061b6821f?w=500", true, -1, "server_master_id", "Main College Canteen", System.currentTimeMillis(), System.currentTimeMillis()),
                new FoodItem("item_8", "Chocolate Brownie", "Warm chocolate fudge brownie topped with chocolate sauce.", "Desserts", 80.0, "https://images.unsplash.com/photo-1606313564200-e75d5e30476c?w=500", true, -1, "server_master_id", "Main College Canteen", System.currentTimeMillis(), System.currentTimeMillis())
        );

        for (FoodItem item : initialMenu) {
            db.collection("food_items").document(item.getId()).set(item);
        }
    }

    @Override
    public void onCartChanged(int itemCount, double totalAmount) {
        if (itemCount > 0) {
            tvCartBadge.setVisibility(View.VISIBLE);
            tvCartBadge.setText(String.valueOf(itemCount));
        } else {
            tvCartBadge.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cartManager.addListener(this);
        onCartChanged(cartManager.getItemCount(), cartManager.getSubtotal());
        loadFoodItemsFromFirestore();
        listenToReadyOrders();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cartManager.removeListener(this);
        if (readyOrderListener != null) readyOrderListener.remove();
        if (foodMenuListener != null) foodMenuListener.remove();
    }
}
