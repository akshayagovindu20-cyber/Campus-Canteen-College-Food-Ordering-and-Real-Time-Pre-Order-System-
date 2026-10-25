package com.example.mad_project;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mad_project.util.AuthManager;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Ensure single server account exists in background
        AuthManager authManager = AuthManager.getInstance(this);
        authManager.ensureServerAccountExists(null);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (authManager.isLoggedIn()) {
                if (authManager.isServer()) {
                    startActivity(new Intent(SplashActivity.this, ServerDashboardActivity.class));
                } else {
                    startActivity(new Intent(SplashActivity.this, SelectCanteenActivity.class));
                }
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
            finish();
        }, 1500);
    }
}
