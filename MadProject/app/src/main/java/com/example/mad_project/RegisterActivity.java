package com.example.mad_project;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mad_project.model.User;
import com.example.mad_project.util.AuthManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

public class RegisterActivity extends AppCompatActivity {

    private RadioGroup rgRole;
    private RadioButton rbCustomer, rbServer;
    private TextInputLayout tilCanteenName;
    private LinearLayout layoutCanteenImagePicker, layoutUploadPrompt;
    private ImageView imgCanteenPreview;
    private MaterialButton btnSelectCanteenImage, btnRegister;
    private TextView tvImageStatus, tvLoginLink;
    private TextInputEditText etFullName, etPhone, etCanteenName, etPassword, etConfirmPassword;
    private ProgressBar progressBar;

    private AuthManager authManager;
    private Uri selectedImageUri = null;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authManager = AuthManager.getInstance(this);

        rgRole = findViewById(R.id.rgRole);
        rbCustomer = findViewById(R.id.rbRoleCustomer);
        rbServer = findViewById(R.id.rbRoleServer);

        tilCanteenName = findViewById(R.id.tilCanteenName);
        layoutCanteenImagePicker = findViewById(R.id.layoutCanteenImagePicker);
        layoutUploadPrompt = findViewById(R.id.layoutUploadPrompt);
        imgCanteenPreview = findViewById(R.id.imgCanteenPreview);
        btnSelectCanteenImage = findViewById(R.id.btnSelectCanteenImage);
        tvImageStatus = findViewById(R.id.tvImageStatus);

        etFullName = findViewById(R.id.etRegName);
        etPhone = findViewById(R.id.etRegPhone);
        etCanteenName = findViewById(R.id.etRegCanteenName);
        etPassword = findViewById(R.id.etRegPassword);
        etConfirmPassword = findViewById(R.id.etRegConfirmPassword);

        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        setupImagePickerLauncher();

        rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbRoleServer) {
                tilCanteenName.setVisibility(View.VISIBLE);
                layoutCanteenImagePicker.setVisibility(View.VISIBLE);
            } else {
                tilCanteenName.setVisibility(View.GONE);
                layoutCanteenImagePicker.setVisibility(View.GONE);
            }
        });

        btnSelectCanteenImage.setOnClickListener(v -> openGalleryPicker());
        imgCanteenPreview.setOnClickListener(v -> openGalleryPicker());

        btnRegister.setOnClickListener(v -> handleRegister());

        tvLoginLink.setOnClickListener(v -> finish());
    }

    private void setupImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        selectedImageUri = result.getData().getData();
                        imgCanteenPreview.setImageURI(selectedImageUri);
                        layoutUploadPrompt.setVisibility(View.GONE);
                        tvImageStatus.setText("Canteen Photo Selected! Ready to upload");
                        tvImageStatus.setTextColor(getColor(R.color.canteen_green));
                    }
                }
        );
    }

    private void openGalleryPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void handleRegister() {
        String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String canteenName = etCanteenName.getText() != null ? etCanteenName.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        String role = rbServer.isChecked() ? User.ROLE_SERVER : User.ROLE_CUSTOMER;

        if (fullName.isEmpty()) {
            Toast.makeText(this, "Please enter your full name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (phone.isEmpty() || phone.length() < 10) {
            Toast.makeText(this, "Please enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (User.ROLE_SERVER.equals(role) && canteenName.isEmpty()) {
            canteenName = fullName + "'s Canteen";
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        if (User.ROLE_SERVER.equals(role) && selectedImageUri != null) {
            // Upload selected image file to Firebase Storage first
            uploadCanteenImageAndRegister(fullName, phone, password, role, canteenName);
        } else {
            // Register directly with default image
            performRegistration(fullName, phone, password, role, canteenName, "");
        }
    }

    private void uploadCanteenImageAndRegister(String fullName, String phone, String password, String role, String canteenName) {
        tvImageStatus.setText("Uploading canteen photo...");
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("canteen_images/" + UUID.randomUUID().toString() + ".jpg");

        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String uploadedImageUrl = uri.toString();
                    performRegistration(fullName, phone, password, role, canteenName, uploadedImageUrl);
                }).addOnFailureListener(e -> {
                    performRegistration(fullName, phone, password, role, canteenName, "");
                }))
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Image upload warning: " + e.getMessage() + ". Continuing with default photo.", Toast.LENGTH_SHORT).show();
                    performRegistration(fullName, phone, password, role, canteenName, "");
                });
    }

    private void performRegistration(String fullName, String phone, String password, String role, String canteenName, String canteenImageUrl) {
        authManager.registerUser(fullName, phone, password, role, canteenName, canteenImageUrl, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, "Registration Successful! Welcome " + user.getDisplayName(), Toast.LENGTH_SHORT).show();

                if (User.ROLE_SERVER.equalsIgnoreCase(user.getRole())) {
                    Intent intent = new Intent(RegisterActivity.this, ServerDashboardActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(RegisterActivity.this, SelectCanteenActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnRegister.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnRegister.setEnabled(true);
        }
    }
}
