package com.example.mad_project;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.mad_project.adapter.CanteenAdapter;
import com.example.mad_project.model.Canteen;
import com.example.mad_project.model.User;
import com.example.mad_project.util.AuthManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SelectCanteenActivity extends AppCompatActivity {

    private EditText etSearchCanteen;
    private RecyclerView rvCanteens;
    private TextView tvEmptyCanteens;
    private SwipeRefreshLayout swipeRefresh;
    private ImageButton btnProfile;

    private CanteenAdapter adapter;
    private final List<Canteen> allCanteens = new ArrayList<>();
    private final List<Canteen> filteredCanteens = new ArrayList<>();
    private String searchQuery = "";

    private FirebaseFirestore db;
    private AuthManager authManager;
    private ListenerRegistration canteensListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_canteen);

        db = FirebaseFirestore.getInstance();
        authManager = AuthManager.getInstance(this);

        // Security check: if logged in user is SERVER, open Server Dashboard directly!
        if (authManager.isServer()) {
            startActivity(new Intent(this, ServerDashboardActivity.class));
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        setupSearch();
        loadCanteensFromFirestore();
    }

    private void initViews() {
        etSearchCanteen = findViewById(R.id.etSearchCanteen);
        rvCanteens = findViewById(R.id.rvCanteens);
        tvEmptyCanteens = findViewById(R.id.tvEmptyCanteens);
        swipeRefresh = findViewById(R.id.swipeRefreshCanteen);
        btnProfile = findViewById(R.id.btnProfileFromCanteenSelect);

        btnProfile.setOnClickListener(v -> startActivity(new Intent(SelectCanteenActivity.this, ProfileActivity.class)));
        swipeRefresh.setOnRefreshListener(this::loadCanteensFromFirestore);
    }

    private void setupRecyclerView() {
        adapter = new CanteenAdapter(this, canteen -> {
            Intent intent = new Intent(SelectCanteenActivity.this, CustomerDashboardActivity.class);
            intent.putExtra("CANTEEN_NAME", canteen.getName());
            startActivity(intent);
        });

        rvCanteens.setLayoutManager(new LinearLayoutManager(this));
        rvCanteens.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearchCanteen.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim();
                filterCanteens();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadCanteensFromFirestore() {
        swipeRefresh.setRefreshing(true);

        // Fetch from canteens collection & registered server users
        canteensListener = db.collection("canteens")
                .addSnapshotListener((querySnapshot, e) -> {
                    swipeRefresh.setRefreshing(false);
                    if (e != null) {
                        Toast.makeText(SelectCanteenActivity.this, "Error loading canteens: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    allCanteens.clear();
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Canteen c = doc.toObject(Canteen.class);
                            if (c != null) {
                                c.setCanteenId(doc.getId());
                                allCanteens.add(c);
                            }
                        }
                    }

                    if (allCanteens.isEmpty()) {
                        // Seed default canteens if collection is brand new!
                        seedDefaultCanteens();
                    } else {
                        // Also merge any newly registered server accounts
                        loadServerUserCanteens();
                    }
                });
    }

    private void loadServerUserCanteens() {
        db.collection("users")
                .whereEqualTo("role", User.ROLE_SERVER)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            User serverUser = doc.toObject(User.class);
                            if (serverUser != null && serverUser.getCanteenName() != null && !serverUser.getCanteenName().isEmpty()) {
                                boolean exists = false;
                                for (Canteen c : allCanteens) {
                                    if (c.getName().equalsIgnoreCase(serverUser.getCanteenName())) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    allCanteens.add(new Canteen(
                                            "canteen_" + serverUser.getUserId(),
                                            serverUser.getCanteenName(),
                                            "Campus Block - " + serverUser.getDisplayName(),
                                            "Fresh items & pre-orders",
                                            "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=500",
                                            true
                                    ));
                                }
                            }
                        }
                    }
                    filterCanteens();
                })
                .addOnFailureListener(err -> filterCanteens());
    }

    private void filterCanteens() {
        filteredCanteens.clear();
        for (Canteen c : allCanteens) {
            boolean matchesSearch = searchQuery.isEmpty() ||
                    (c.getName() != null && c.getName().toLowerCase().contains(searchQuery.toLowerCase())) ||
                    (c.getLocation() != null && c.getLocation().toLowerCase().contains(searchQuery.toLowerCase())) ||
                    (c.getDescription() != null && c.getDescription().toLowerCase().contains(searchQuery.toLowerCase()));

            if (matchesSearch) {
                filteredCanteens.add(c);
            }
        }

        adapter.setCanteenList(filteredCanteens);

        if (filteredCanteens.isEmpty()) {
            tvEmptyCanteens.setVisibility(View.VISIBLE);
            rvCanteens.setVisibility(View.GONE);
        } else {
            tvEmptyCanteens.setVisibility(View.GONE);
            rvCanteens.setVisibility(View.VISIBLE);
        }
    }

    private void seedDefaultCanteens() {
        List<Canteen> defaultCanteens = Arrays.asList(
                new Canteen("canteen_main", "Main College Canteen", "Ground Floor, Admin Block", "South Indian, Thalis, Snacks & Fresh Juices", "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=500", true),
                new Canteen("canteen_south", "South Block Canteen", "Ground Floor, South Block", "Dosa, Parathas, Tea, Coffee & Quick Bites", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=500", true),
                new Canteen("canteen_engg", "Engineering Block Canteen", "Block B, Near Computer Lab", "Burgers, Sandwiches, Rolls & Cold Beverages", "https://images.unsplash.com/photo-1552566626-52f8b828add9?w=500", true),
                new Canteen("canteen_juice", "Juice & Snack Corner", "Central Lawn - Kiosk 3", "Fresh Fruit Juices, Smoothies, Brownies & Shakes", "https://images.unsplash.com/photo-1534353473418-4cfa6c56fd38?w=500", true)
        );

        for (Canteen c : defaultCanteens) {
            db.collection("canteens").document(c.getCanteenId()).set(c);
        }

        allCanteens.addAll(defaultCanteens);
        filterCanteens();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (canteensListener != null) {
            canteensListener.remove();
        }
    }
}
