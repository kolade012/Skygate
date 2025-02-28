package com.example.skygate;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.skygate.adapters.EntriesAdapter;
import com.example.skygate.adapters.EntriesAdapter;
import com.example.skygate.models.Entry;
import com.example.skygate.models.Product;
import com.example.skygate.models.Product;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StaffDashboardActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RecyclerView recyclerViewEntries;
    private EntriesAdapter entriesAdapter;
    private ProgressBar progressBar;
    private TextView tvNoEntries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        progressBar = findViewById(R.id.progressBar);
        tvNoEntries = findViewById(R.id.tvNoEntries);
        recyclerViewEntries = findViewById(R.id.recyclerViewEntries);

        // Find the ScrollViews
        HorizontalScrollView headerScroll = findViewById(R.id.headerScrollView);
        HorizontalScrollView contentScroll = findViewById(R.id.contentScrollView);

        // Sync horizontal scrolling
        headerScroll.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                contentScroll.scrollTo(scrollX, 0);
            }
        });

        contentScroll.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                headerScroll.scrollTo(scrollX, 0);
            }
        });

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup navigation drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        // Setup cards
        MaterialCardView cardCreateEntry = findViewById(R.id.cardCreateEntry);
        MaterialCardView cardViewStock = findViewById(R.id.cardViewStock);
        MaterialCardView cardViewMyEntries = findViewById(R.id.cardViewMyEntries);

        cardCreateEntry.setOnClickListener(v -> startActivity(new Intent(this, CreateEntryActivity.class)));
        cardViewStock.setOnClickListener(v -> startActivity(new Intent(this, ViewStockActivity.class)));

        // Setup RecyclerView
        recyclerViewEntries = findViewById(R.id.recyclerViewEntries);
        recyclerViewEntries.setLayoutManager(new LinearLayoutManager(this));
        loadRecentEntries();
    }

    private void loadRecentEntries() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        // Check if db is initialized
        if (db == null) {
            Log.e("LoadEntries", "Database reference is null");
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            return;
        }

        db.collection("entries")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .orderBy("controlNumber", Query.Direction.ASCENDING)
                .limit(10)
                .addSnapshotListener((value, error) -> {
                    // Always ensure progressBar is hidden
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }

                    // Handle potential errors
                    if (error != null) {
                        Log.e("LoadEntries", "Error loading entries: " + error.getMessage());
                        Toast.makeText(StaffDashboardActivity.this, "Error loading entries: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        updateVisibility(true);
                        return;
                    }

                    // Check for null or empty snapshot
                    if (value == null || value.isEmpty()) {
                        updateVisibility(true);
                        return;
                    }

                    try {
                        List<Entry> entries = new ArrayList<>();
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());

                        for (DocumentSnapshot doc : value.getDocuments()) {
                            try {
                                if (!doc.exists()) {
                                    continue;
                                }

                                // Get basic fields with defaults
                                String entryId = doc.getId();
                                Long createdAtLong = doc.getLong("createdAt");
                                String entryDate = "N/A";

                                // Format date if available
                                if (createdAtLong != null) {
                                    try {
                                        entryDate = dateFormat.format(new Date(createdAtLong));
                                    } catch (Exception e) {
                                        Log.e("LoadEntries", "Date formatting error: " + e.getMessage());
                                    }
                                }

                                // Get other fields with defaults
                                int controlNum = doc.getLong("controlNumber").intValue();
                                String entryType = doc.getString("entryType");
                                if (entryType == null) entryType = "N/A";

                                String driver = doc.getString("driverId");
                                if (driver == null) driver = "N/A";

                                // Process products safely
                                List<Product> productList = new ArrayList<>();
                                Object productsObj = doc.get("products");
                                if (productsObj instanceof List<?>) {
                                    List<?> productsList = (List<?>) productsObj;
                                    for (Object item : productsList) {
                                        if (item instanceof Map<?, ?>) {
                                            try {
                                                Map<String, Object> productMap = (Map<String, Object>) item;
                                                String productName = (String) productMap.getOrDefault("product", "N/A");
                                                Long soldLong = (Long) productMap.getOrDefault("sold", 0L);
                                                int soldQuantity = soldLong != null ? soldLong.intValue() : 0;
                                                productList.add(new Product(productName, soldQuantity));
                                            } catch (Exception e) {
                                                Log.e("LoadEntries", "Error parsing product: " + e.getMessage());
                                            }
                                        }
                                    }
                                }

                                // Create entry with null-safe constructor
                                Entry entry = new Entry(
                                        entryId,
                                        entryDate,
                                        controlNum,
                                        entryType,
                                        driver,
                                        createdAtLong != null ? createdAtLong : 0L,
                                        productList
                                );
                                entries.add(entry);

                            } catch (Exception e) {
                                Log.e("LoadEntries", "Error parsing entry: " + e.getMessage());
                                // Continue to next document
                                continue;
                            }
                        }

                        // Update UI on main thread
                        if (recyclerViewEntries != null) {
                            if (entriesAdapter == null) {
                                entriesAdapter = new EntriesAdapter(entries, entry -> {
                                    try {
                                        Intent intent = new Intent(StaffDashboardActivity.this, EntryDetailsActivity.class);
                                        intent.putExtra("entry", entry);
                                        startActivity(intent);
                                    } catch (Exception e) {
                                        Log.e("LoadEntries", "Error launching details: " + e.getMessage());
                                        Toast.makeText(StaffDashboardActivity.this,
                                                "Error opening entry details", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                recyclerViewEntries.setAdapter(entriesAdapter);
                            } else {
                                entriesAdapter.setEntries(entries);
                            }
                        }

                        updateVisibility(entries.isEmpty());
                    } catch (Exception e) {
                        Log.e("LoadEntries", "Fatal error in entry processing: " + e.getMessage());
                        updateVisibility(true);
                    }
                });
    }

    private void updateVisibility(boolean isEmpty) {
        if (tvNoEntries != null && recyclerViewEntries != null) {
            tvNoEntries.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            recyclerViewEntries.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_all_entries) {
            startActivity(new Intent(this, AllEntriesActivity.class));
        } else if (id == R.id.nav_change_password) {
            showChangePasswordDialog();
        } else if (id == R.id.nav_logout) {
            logout();
        } else if (id == R.id.nav_dashboard_2) {
            startActivity(new Intent(this, StaffDashboardActivity.class));
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showChangePasswordDialog() {
        // Create dialog using Material Design
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);

        // Initialize the TextInputEditText fields
        TextInputEditText etCurrentPassword = dialogView.findViewById(R.id.etCurrentPassword);
        TextInputEditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmNewPassword);
        ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.setTitle("Change Password");

        // Add buttons
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Change", (DialogInterface.OnClickListener) null); // Set null click listener initially
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", (dialogInterface, i) -> dialog.dismiss());

        dialog.show();

        // Set the positive button click listener after showing the dialog
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String currentPassword = etCurrentPassword.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            // Validate inputs
            if (currentPassword.isEmpty()) {
                etCurrentPassword.setError("Current password is required");
                return;
            }
            if (newPassword.isEmpty()) {
                etNewPassword.setError("New password is required");
                return;
            }
            if (confirmPassword.isEmpty()) {
                etConfirmPassword.setError("Please confirm new password");
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                etConfirmPassword.setError("Passwords do not match");
                return;
            }
            if (newPassword.length() < 6) {
                etNewPassword.setError("Password must be at least 6 characters");
                return;
            }

            // Show progress and disable buttons
            progressBar.setVisibility(View.VISIBLE);
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);

            // Get current user
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                // Re-authenticate user before changing password
                AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

                user.reauthenticate(credential)
                        .addOnSuccessListener(aVoid -> {
                            // Authentication successful, proceed with password change
                            user.updatePassword(newPassword)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(StaffDashboardActivity.this,
                                                    "Password updated successfully", Toast.LENGTH_SHORT).show();
                                            dialog.dismiss();
                                        } else {
                                            showError(task.getException(), etNewPassword);
                                            enableDialogButtons(dialog);
                                        }
                                        progressBar.setVisibility(View.GONE);
                                    });
                        })
                        .addOnFailureListener(e -> {
                            showError(e, etCurrentPassword);
                            enableDialogButtons(dialog);
                            progressBar.setVisibility(View.GONE);
                        });
            }
        });
    }

    // Helper method to show errors
    private void showError(Exception e, TextInputEditText editText) {
        String message = e != null ? e.getMessage() : "An error occurred";
        if (message.contains("password")) {
            editText.setError(message);
        } else {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    // Helper method to re-enable dialog buttons
    private void enableDialogButtons(AlertDialog dialog) {
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(true);
    }

    private void logout() {
        mAuth.signOut();
        startActivity(new Intent(this, UserTypeActivity.class));
        finish();
    }
}