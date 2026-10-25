package com.example.mad_project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mad_project.util.AuthManager;
import com.google.android.material.button.MaterialButton;

public class ProfileActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvName, tvUsername, tvRoleBadge;
    private LinearLayout btnSelectCanteen, btnMyOrders, btnNotifications;
    private MaterialButton btnLogout;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        authManager = AuthManager.getInstance(this);

        btnBack = findViewById(R.id.btnBackProfile);
        tvName = findViewById(R.id.tvProfileName);
        tvUsername = findViewById(R.id.tvProfileUsername);
        tvRoleBadge = findViewById(R.id.tvProfileRoleBadge);

        btnSelectCanteen = findViewById(R.id.btnProfileSelectCanteen);
        btnMyOrders = findViewById(R.id.btnProfileOrders);
        btnNotifications = findViewById(R.id.btnProfileNotifications);
        btnLogout = findViewById(R.id.btnLogout);

        btnBack.setOnClickListener(v -> finish());

        tvName.setText(authManager.getCurrentDisplayName());
        tvUsername.setText("@" + authManager.getCurrentUsername());
        tvRoleBadge.setText(authManager.getCurrentRole() + " ACCOUNT");

        btnSelectCanteen.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, SelectCanteenActivity.class)));
        btnMyOrders.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, MyOrdersActivity.class)));
        btnNotifications.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, NotificationsActivity.class)));

        btnLogout.setOnClickListener(v -> {
            authManager.logout();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
