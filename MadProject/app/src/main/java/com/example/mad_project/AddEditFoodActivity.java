package com.example.mad_project;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.mad_project.model.FoodItem;
import com.example.mad_project.util.AuthManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class AddEditFoodActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvTitle, tvFoodImageStatus;
    private ImageView imgFoodPhotoPreview;
    private MaterialButton btnSelectFoodImage, btnSave, btnDelete;
    private TextInputEditText etName, etDescription, etPrice, etImageUrl, etStock;
    private Spinner spinnerCategory;
    private MaterialSwitch switchAvailable;
    private ProgressBar progressBar;

    private FoodItem existingFood;
    private FirebaseFirestore db;
    private Uri selectedImageUri = null;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private final List<String> categories = Arrays.asList("Breakfast", "Meals", "Snacks", "Beverages", "Desserts");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_food);

        db = FirebaseFirestore.getInstance();

        if (getIntent() != null && getIntent().hasExtra("FOOD_ITEM")) {
            existingFood = (FoodItem) getIntent().getSerializableExtra("FOOD_ITEM");
        }

        initViews();
        setupCategorySpinner();
        setupImagePickerLauncher();

        if (existingFood != null) {
            bindExistingData();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackAddFood);
        tvTitle = findViewById(R.id.tvAddEditTitle);

        imgFoodPhotoPreview = findViewById(R.id.imgFoodPhotoPreview);
        btnSelectFoodImage = findViewById(R.id.btnSelectFoodImage);
        tvFoodImageStatus = findViewById(R.id.tvFoodImageStatus);

        etName = findViewById(R.id.etFoodName);
        etDescription = findViewById(R.id.etFoodDescription);
        etPrice = findViewById(R.id.etFoodPrice);
        etImageUrl = findViewById(R.id.etFoodImageUrl);
        etStock = findViewById(R.id.etFoodStock);

        spinnerCategory = findViewById(R.id.spinnerCategory);
        switchAvailable = findViewById(R.id.switchFoodAvailable);

        btnSave = findViewById(R.id.btnSaveFood);
        btnDelete = findViewById(R.id.btnDeleteFood);
        progressBar = findViewById(R.id.progressAddFood);

        btnBack.setOnClickListener(v -> finish());
        btnSelectFoodImage.setOnClickListener(v -> openGalleryPicker());
        imgFoodPhotoPreview.setOnClickListener(v -> openGalleryPicker());

        btnSave.setOnClickListener(v -> saveFoodItem());
        btnDelete.setOnClickListener(v -> deleteFoodItem());
    }

    private void setupImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        selectedImageUri = result.getData().getData();
                        imgFoodPhotoPreview.setImageURI(selectedImageUri);
                        tvFoodImageStatus.setText("Food Photo Selected! Will upload on save.");
                        tvFoodImageStatus.setTextColor(getColor(R.color.canteen_green));
                    }
                }
        );
    }

    private void openGalleryPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void setupCategorySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, categories);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void bindExistingData() {
        tvTitle.setText("Edit Canteen Food Item");
        etName.setText(existingFood.getName());
        etDescription.setText(existingFood.getDescription());
        etPrice.setText(String.valueOf(existingFood.getPrice()));
        etImageUrl.setText(existingFood.getImageUrl());
        etStock.setText(String.valueOf(existingFood.getStockQuantity()));
        switchAvailable.setChecked(existingFood.isAvailable());

        if (existingFood.getCategory() != null) {
            int pos = categories.indexOf(existingFood.getCategory());
            if (pos >= 0) {
                spinnerCategory.setSelection(pos);
            }
        }

        if (existingFood.getImageUrl() != null && !existingFood.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(existingFood.getImageUrl())
                    .placeholder(R.drawable.ic_food_placeholder)
                    .into(imgFoodPhotoPreview);
        }

        btnDelete.setVisibility(View.VISIBLE);
    }

    private void saveFoodItem() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        String priceStr = etPrice.getText() != null ? etPrice.getText().toString().trim() : "";
        String imageUrl = etImageUrl.getText() != null ? etImageUrl.getText().toString().trim() : "";
        String stockStr = etStock.getText() != null ? etStock.getText().toString().trim() : "-1";

        if (name.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "Food name and price are required", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (Exception e) {
            Toast.makeText(this, "Invalid price amount", Toast.LENGTH_SHORT).show();
            return;
        }

        int stock = -1;
        try {
            if (!stockStr.isEmpty()) stock = Integer.parseInt(stockStr);
        } catch (Exception ignored) {}

        String category = spinnerCategory.getSelectedItem() != null ? spinnerCategory.getSelectedItem().toString() : "Meals";
        boolean isAvailable = switchAvailable.isChecked();

        setLoading(true);

        if (selectedImageUri != null) {
            // Upload selected photo from gallery first
            uploadFoodImageAndSave(name, description, category, price, stock, isAvailable, imageUrl);
        } else {
            performFoodSave(name, description, category, price, stock, isAvailable, imageUrl);
        }
    }

    private void uploadFoodImageAndSave(String name, String description, String category, double price, int stock, boolean isAvailable, String existingUrl) {
        tvFoodImageStatus.setText("Uploading food photo...");
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("food_images/" + UUID.randomUUID().toString() + ".jpg");

        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String uploadedUrl = uri.toString();
                    performFoodSave(name, description, category, price, stock, isAvailable, uploadedUrl);
                }).addOnFailureListener(e -> performFoodSave(name, description, category, price, stock, isAvailable, existingUrl)))
                .addOnFailureListener(e -> {
                    Toast.makeText(AddEditFoodActivity.this, "Image upload warning: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    performFoodSave(name, description, category, price, stock, isAvailable, existingUrl);
                });
    }

    private void performFoodSave(String name, String description, String category, double price, int stock, boolean isAvailable, String imageUrl) {
        String id = existingFood != null ? existingFood.getId() : "food_" + UUID.randomUUID().toString();
        long created = existingFood != null ? existingFood.getCreatedAt() : System.currentTimeMillis();

        AuthManager auth = AuthManager.getInstance(this);
        String serverId = existingFood != null ? existingFood.getServerId() : auth.getCurrentUserId();
        String canteenName = existingFood != null ? existingFood.getCanteenName() : auth.getCurrentCanteenName();

        FoodItem item = new FoodItem(id, name, description, category, price, imageUrl, isAvailable, stock, serverId, canteenName, created, System.currentTimeMillis());

        db.collection("food_items").document(id).set(item)
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Toast.makeText(AddEditFoodActivity.this, "Food item saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(AddEditFoodActivity.this, "Failed to save food: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteFoodItem() {
        if (existingFood == null) return;

        setLoading(true);
        db.collection("food_items").document(existingFood.getId()).delete()
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Toast.makeText(AddEditFoodActivity.this, "Food item deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(AddEditFoodActivity.this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnSave.setEnabled(false);
            btnDelete.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnSave.setEnabled(true);
            btnDelete.setEnabled(true);
        }
    }
}
