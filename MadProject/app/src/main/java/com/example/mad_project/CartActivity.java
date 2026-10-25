package com.example.mad_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_project.adapter.CartAdapter;
import com.example.mad_project.model.CartItem;
import com.example.mad_project.util.CartManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.Locale;

public class CartActivity extends AppCompatActivity implements CartManager.CartChangeListener {

    private ImageButton btnBack;
    private TextView btnClearCart, tvSubtotal, tvTotal;
    private RecyclerView rvCartItems;
    private LinearLayout layoutEmptyCart;
    private MaterialCardView cardSummary;
    private MaterialButton btnCheckout;

    private CartAdapter cartAdapter;
    private CartManager cartManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        cartManager = CartManager.getInstance(this);

        initViews();
        setupRecyclerView();
        updateUI();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackCart);
        btnClearCart = findViewById(R.id.btnClearCart);
        tvSubtotal = findViewById(R.id.tvCartSubtotal);
        tvTotal = findViewById(R.id.tvCartTotal);

        rvCartItems = findViewById(R.id.rvCartItems);
        layoutEmptyCart = findViewById(R.id.layoutEmptyCart);
        cardSummary = findViewById(R.id.cardCartSummary);
        btnCheckout = findViewById(R.id.btnProceedCheckout);

        btnBack.setOnClickListener(v -> finish());

        btnClearCart.setOnClickListener(v -> {
            if (cartManager.getItemCount() > 0) {
                cartManager.clearCart();
                Toast.makeText(this, "Cart cleared", Toast.LENGTH_SHORT).show();
            }
        });

        btnCheckout.setOnClickListener(v -> {
            if (cartManager.getItemCount() > 0) {
                startActivity(new Intent(CartActivity.this, CheckoutActivity.class));
            } else {
                Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecyclerView() {
        cartAdapter = new CartAdapter(this, new CartAdapter.OnCartQuantityChangeListener() {
            @Override
            public void onQuantityChanged(CartItem item, int newQuantity) {
                cartManager.updateQuantity(item.getFoodId(), newQuantity);
            }
        });

        rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        rvCartItems.setAdapter(cartAdapter);
    }

    private void updateUI() {
        List<CartItem> items = cartManager.getCartItems();
        cartAdapter.setCartItems(items);

        double subtotal = cartManager.getSubtotal();
        tvSubtotal.setText(String.format(Locale.getDefault(), "₹ %.2f", subtotal));
        tvTotal.setText(String.format(Locale.getDefault(), "₹ %.2f", subtotal));

        if (items.isEmpty()) {
            layoutEmptyCart.setVisibility(View.VISIBLE);
            rvCartItems.setVisibility(View.GONE);
            cardSummary.setVisibility(View.GONE);
            btnClearCart.setVisibility(View.GONE);
        } else {
            layoutEmptyCart.setVisibility(View.GONE);
            rvCartItems.setVisibility(View.VISIBLE);
            cardSummary.setVisibility(View.VISIBLE);
            btnClearCart.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCartChanged(int itemCount, double totalAmount) {
        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cartManager.addListener(this);
        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cartManager.removeListener(this);
    }
}
