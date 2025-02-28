package com.example.skygate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.skygate.models.User;
import com.example.skygate.utils.SessionManager;
import com.example.skygate.utils.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class StaffLoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_login);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        // Initialize views
        etEmail = findViewById(R.id.etStaffEmail);
        etPassword = findViewById(R.id.etStaffPassword);
        btnLogin = findViewById(R.id.btnStaffLogin);
        progressBar = findViewById(R.id.progressBar);

        // Set click listener for login button
        btnLogin.setOnClickListener(v -> loginStaff());
    }

    private void loginStaff() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate input
        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        // Show progress and disable login button
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        // Attempt login
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        verifyStaffRole(mAuth.getCurrentUser().getUid());
                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Login failed";
                        showError(errorMessage);
                    }
                });
    }

    private void verifyStaffRole(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null && "staff".equals(user.getRole())) {
                        handleSuccessfulLogin(user);
                    } else {
                        handleUnauthorizedAccess();
                    }
                })
                .addOnFailureListener(e -> showError(e.getMessage()));
    }

    private void handleSuccessfulLogin(User user) {
        sessionManager.setUserRole(user.getRole());
        startActivity(new Intent(StaffLoginActivity.this, StaffDashboardActivity.class));
        finish();
    }

    private void handleUnauthorizedAccess() {
        mAuth.signOut();
        showError("This account does not have staff access");
    }

    private void showError(String message) {
        Toast.makeText(StaffLoginActivity.this, message, Toast.LENGTH_LONG).show();
        progressBar.setVisibility(View.GONE);
        btnLogin.setEnabled(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() != null) {
            verifyStaffRole(mAuth.getCurrentUser().getUid());
        }
    }
}