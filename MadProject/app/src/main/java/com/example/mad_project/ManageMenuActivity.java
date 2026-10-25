package com.example.mad_project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_project.adapter.ManageMenuAdapter;
import com.example.mad_project.model.FoodItem;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ManageMenuActivity extends AppCompatActivity {

    private ImageButton btnBack, btnAddTop;
    private RecyclerView rvManageMenu;

    private ManageMenuAdapter adapter;
    private final List<FoodItem> foodList = new ArrayList<>();

    private FirebaseFirestore db;
    private ListenerRegistration menuListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_menu);

        db = FirebaseFirestore.getInstance();

        btnBack = findViewById(R.id.btnBackManageMenu);
        btnAddTop = findViewById(R.id.btnAddFoodTop);
        rvManageMenu = findViewById(R.id.rvManageMenu);

        btnBack.setOnClickListener(v -> finish());
        btnAddTop.setOnClickListener(v -> startActivity(new Intent(ManageMenuActivity.this, AddEditFoodActivity.class)));

        setupRecyclerView();
        listenToMenu();
    }

    private void setupRecyclerView() {
        adapter = new ManageMenuAdapter(this, new ManageMenuAdapter.OnManageFoodActionListener() {
            @Override
            public void onToggleAvailability(FoodItem food, boolean isAvailable) {
                db.collection("food_items").document(food.getId()).update("available", isAvailable);
            }

            @Override
            public void onEditFood(FoodItem food) {
                Intent intent = new Intent(ManageMenuActivity.this, AddEditFoodActivity.class);
                intent.putExtra("FOOD_ITEM", food);
                startActivity(intent);
            }
        });

        rvManageMenu.setLayoutManager(new LinearLayoutManager(this));
        rvManageMenu.setAdapter(adapter);
    }

    private void listenToMenu() {
        menuListener = db.collection("food_items")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(ManageMenuActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    foodList.clear();
                    if (querySnapshot != null) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            FoodItem item = doc.toObject(FoodItem.class);
                            if (item != null) {
                                item.setId(doc.getId());
                                foodList.add(item);
                            }
                        }
                    }

                    adapter.setFoodList(foodList);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (menuListener != null) {
            menuListener.remove();
        }
    }
}
