package com.example.mad_project;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.mad_project.model.FoodItem;
import com.example.mad_project.util.CartManager;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class FoodDetailActivity extends AppCompatActivity {

    private ImageButton btnBack, btnMinus, btnPlus;
    private ImageView imgFood;
    private TextView tvCategory, tvName, tvPrice, tvDescription, tvAvailability, tvQty;
    private MaterialButton btnAddToCart;

    private FoodItem foodItem;
    private int quantity = 1;
    private CartManager cartManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_detail);

        cartManager = CartManager.getInstance(this);

        if (getIntent() != null && getIntent().hasExtra("FOOD_ITEM")) {
            foodItem = (FoodItem) getIntent().getSerializableExtra("FOOD_ITEM");
        }

        if (foodItem == null) {
            Toast.makeText(this, "Invalid food item details", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        bindData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackFoodDetail);
        btnMinus = findViewById(R.id.btnDetailMinus);
        btnPlus = findViewById(R.id.btnDetailPlus);

        imgFood = findViewById(R.id.imgDetailFood);
        tvCategory = findViewById(R.id.tvDetailCategory);
        tvName = findViewById(R.id.tvDetailName);
        tvPrice = findViewById(R.id.tvDetailPrice);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvAvailability = findViewById(R.id.tvDetailAvailability);
        tvQty = findViewById(R.id.tvDetailQty);

        btnAddToCart = findViewById(R.id.btnDetailAddToCart);

        btnBack.setOnClickListener(v -> finish());

        btnMinus.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                updateQuantityAndButton();
            }
        });

        btnPlus.setOnClickListener(v -> {
            quantity++;
            updateQuantityAndButton();
        });

        btnAddToCart.setOnClickListener(v -> {
            cartManager.addItem(foodItem, quantity);
            Toast.makeText(this, quantity + "x " + foodItem.getName() + " added to cart!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void bindData() {
        tvName.setText(foodItem.getName());
        tvCategory.setText(foodItem.getCategory());
        tvDescription.setText(foodItem.getDescription());
        tvPrice.setText(String.format(Locale.getDefault(), "₹ %.2f", foodItem.getPrice()));

        if (foodItem.isAvailable()) {
            tvAvailability.setText("Available");
            tvAvailability.setTextColor(getColor(R.color.canteen_green));
            tvAvailability.getBackground().setTint(getColor(R.color.status_ready_bg));
            btnAddToCart.setEnabled(true);
        } else {
            tvAvailability.setText("Sold Out");
            tvAvailability.setTextColor(getColor(R.color.canteen_red));
            tvAvailability.getBackground().setTint(getColor(R.color.status_cancelled_bg));
            btnAddToCart.setEnabled(false);
            btnAddToCart.setText("Currently Unavailable");
        }

        if (foodItem.getImageUrl() != null && !foodItem.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(foodItem.getImageUrl())
                    .placeholder(R.drawable.ic_food_placeholder)
                    .error(R.drawable.ic_food_placeholder)
                    .into(imgFood);
        } else {
            imgFood.setImageResource(R.drawable.ic_food_placeholder);
        }

        updateQuantityAndButton();
    }

    private void updateQuantityAndButton() {
        tvQty.setText(String.valueOf(quantity));
        double total = foodItem.getPrice() * quantity;
        if (foodItem.isAvailable()) {
            btnAddToCart.setText(String.format(Locale.getDefault(), "Add to Cart • ₹ %.2f", total));
        }
    }
}
