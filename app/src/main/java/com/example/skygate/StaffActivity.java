package com.example.skygate;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** @noinspection ALL*/
public class StaffActivity extends AppCompatActivity implements StaffAdapter.OnStaffClickListener {
    private RecyclerView recyclerView;
    private StaffAdapter adapter;
    private List<Staff> staffList;
    private EditText searchEditText;
    private FirebaseFirestore db;
    private Map<String, Double> currentMonthSales;
    private Map<String, Double> previousMonthSales;
    private ListenerRegistration ordersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff);

        // Initialize maps for sales data
        currentMonthSales = new HashMap<>();
        previousMonthSales = new HashMap<>();

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_staff);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Staff Management");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize Views
        recyclerView = findViewById(R.id.recycler_staff);
        searchEditText = findViewById(R.id.search_staff);

        // Initialize RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        staffList = new ArrayList<>();
        adapter = new StaffAdapter(staffList, this);
        recyclerView.setAdapter(adapter);

        // Setup search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStaff(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Setup Add Staff button
        findViewById(R.id.btn_add_staff).setOnClickListener(v -> showAddStaffDialog());

        // Load staff data from Firestore
        loadStaffData();
        loadMonthlySales();
        setupOrdersListener();
    }

    private void setupOrdersListener() {
        if (ordersListener != null) {
            ordersListener.remove();
        }

        ordersListener = db.collection("orders")
                .whereEqualTo("status", "completed")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Error listening to orders: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        Map<String, PerformanceData> driverMetrics = new HashMap<>();

                        // Calculate totals for each driver
                        for (QueryDocumentSnapshot doc : snapshots) {
                            String driverId = doc.getString("driverId"); // Make sure this matches your order document field
                            Double amount = doc.getDouble("amount");

                            if (driverId != null && amount != null) {
                                PerformanceData data = driverMetrics.computeIfAbsent(driverId,
                                        k -> new PerformanceData());
                                data.salesCount++;
                                data.totalSales += amount;
                            }
                        }

                        // Update each staff member's performance metrics
                        for (Staff staff : staffList) {
                            String staffId = staff.getId();
                            PerformanceData data = driverMetrics.get(staffId);
                            if (data != null) {
                                updateStaffMetrics(staff, data);
                            }
                        }
                    }
                });
    }

    // Modify the updateStaffMetrics method:
    private void updateStaffMetrics(Staff staff, PerformanceData data) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("performanceMetrics", new HashMap<String, Object>() {{
            put("lastActive", Timestamp.now());
            put("salesCount", data.salesCount);
            put("totalSales", (int)data.totalSales);
        }});
        updates.put("lastUpdated", Timestamp.now());

        db.collection("staff")
                .document(staff.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Update local object to reflect changes
                    Staff.PerformanceMetrics metrics = new Staff.PerformanceMetrics();
                    metrics.setLastActive(Timestamp.now());
//                    metrics.setSalesCount(data.salesCount);
//                    metrics.setTotalSales((int)data.totalSales);
                    staff.setPerformanceMetrics(metrics);
                    staff.setLastUpdated(Timestamp.now());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error updating metrics: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    // Also modify the PerformanceData class to use double for totalSales:
    private static class PerformanceData {
        int salesCount = 0;
        double totalSales = 0.0;  // Changed to double to match order amounts
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove the listener when the activity is destroyed
        if (ordersListener != null) {
            ordersListener.remove();
        }
    }

    private void loadMonthlySales() {
        Calendar cal = Calendar.getInstance();

        // Get current month start and end
        Calendar currentMonthStart = Calendar.getInstance();
        currentMonthStart.set(Calendar.DAY_OF_MONTH, 1);
        currentMonthStart.set(Calendar.HOUR_OF_DAY, 0);
        currentMonthStart.set(Calendar.MINUTE, 0);
        currentMonthStart.set(Calendar.SECOND, 0);

        Calendar currentMonthEnd = Calendar.getInstance();
        currentMonthEnd.set(Calendar.DAY_OF_MONTH, currentMonthEnd.getActualMaximum(Calendar.DAY_OF_MONTH));
        currentMonthEnd.set(Calendar.HOUR_OF_DAY, 23);
        currentMonthEnd.set(Calendar.MINUTE, 59);
        currentMonthEnd.set(Calendar.SECOND, 59);

        // Get previous month start and end
        Calendar previousMonthStart = (Calendar) currentMonthStart.clone();
        previousMonthStart.add(Calendar.MONTH, -1);
        Calendar previousMonthEnd = (Calendar) previousMonthStart.clone();
        previousMonthEnd.set(Calendar.DAY_OF_MONTH, previousMonthEnd.getActualMaximum(Calendar.DAY_OF_MONTH));
        previousMonthEnd.set(Calendar.HOUR_OF_DAY, 23);
        previousMonthEnd.set(Calendar.MINUTE, 59);
        previousMonthEnd.set(Calendar.SECOND, 59);

        // Query for current month
        db.collection("orders")
                .whereGreaterThanOrEqualTo("date", previousMonthStart.getTime())
                .whereLessThanOrEqualTo("date", currentMonthEnd.getTime())
                .whereEqualTo("status", "completed")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    currentMonthSales.clear();
                    previousMonthSales.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String driver = document.getString("driver");
                        Double amount = document.getDouble("amount");
                        Date date = document.getDate("date");

                        if (driver != null && amount != null && date != null) {
                            Calendar orderDate = Calendar.getInstance();
                            orderDate.setTime(date);

                            if (orderDate.after(currentMonthStart) && orderDate.before(currentMonthEnd)) {
                                // Current month sales
                                currentMonthSales.put(driver,
                                        currentMonthSales.getOrDefault(driver, 0.0) + amount);
                            } else if (orderDate.after(previousMonthStart) && orderDate.before(previousMonthEnd)) {
                                // Previous month sales
                                previousMonthSales.put(driver,
                                        previousMonthSales.getOrDefault(driver, 0.0) + amount);
                            }
                        }
                    }

                    // Update the adapter with new sales data
                    if (adapter != null) {
                        adapter.updateSalesData(currentMonthSales, previousMonthSales);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading sales data: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void loadStaffData() {
        db.collection("staff")
                .orderBy("joinDate", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading staff: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    staffList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            Staff staff = document.toObject(Staff.class);
                            staff.setId(document.getId());
                            staffList.add(staff);
                        }
                        adapter.updateList(staffList);
                        // Update sales data after staff list is loaded
                        adapter.updateSalesData(currentMonthSales, previousMonthSales);
                    }
                });
    }

    private void filterStaff(String query) {
        List<Staff> filteredList = new ArrayList<>();
        for (Staff staff : staffList) {
            if (staff.getName().toLowerCase().contains(query.toLowerCase()) ||
                    staff.getRole().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(staff);
            }
        }
        adapter.updateList(filteredList);
    }

    private void showAddStaffDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_staff, null);

        // Get input fields from the dialog layout
        EditText etName = dialogView.findViewById(R.id.etName);
        EditText etRole = dialogView.findViewById(R.id.etRole);

        builder.setView(dialogView)
                .setTitle("Add New Staff")
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = etName.getText().toString().trim();
                        String role = etRole.getText().toString().trim();

                        if (TextUtils.isEmpty(name)) {
                            Toast.makeText(StaffActivity.this, "Please enter staff name", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (TextUtils.isEmpty(role)) {
                            Toast.makeText(StaffActivity.this, "Please enter staff role", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Staff newStaff = new Staff();
                        newStaff.setName(name);
                        newStaff.setRole(role);
                        newStaff.setActive(true);
                        newStaff.setJoinDate(Timestamp.now());

                        Staff.PerformanceMetrics metrics = new Staff.PerformanceMetrics();
                        metrics.setLastActive(Timestamp.now());
//                        metrics.setSalesCount(0);
//                        metrics.setTotalSales(0);
                        newStaff.setPerformanceMetrics(metrics);

                        db.collection("staff")
                                .add(newStaff)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(StaffActivity.this, "Staff added successfully",
                                            Toast.LENGTH_SHORT).show();
                                    // Optionally, finish the activity after successful addition
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(StaffActivity.this, "Error adding staff: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onEditClick(Staff staff, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_staff, null);

        // Get input fields from the dialog layout
        EditText etName = dialogView.findViewById(R.id.etName2);
        EditText etRole = dialogView.findViewById(R.id.etRole2);

        // Set initial values in the dialog
        etName.setText(staff.getName());
        etRole.setText(staff.getRole());

        builder.setView(dialogView)
                .setTitle("Edit Staff")
                .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = etName.getText().toString().trim();
                        String role = etRole.getText().toString().trim();

                        if (TextUtils.isEmpty(name)) {
                            Toast.makeText(StaffActivity.this, "Please enter staff name", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (TextUtils.isEmpty(role)) {
                            Toast.makeText(StaffActivity.this, "Please enter staff role", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        staff.setName(name);
                        staff.setRole(role);
                        staff.setLastUpdated(Timestamp.now()); // Update last updated timestamp

                        db.collection("staff")
                                .document(staff.getId())
                                .set(staff)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(StaffActivity.this, "Staff updated successfully",
                                            Toast.LENGTH_SHORT).show();
                                    adapter.notifyItemChanged(position); // Update the RecyclerView item
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(StaffActivity.this, "Error updating staff: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onDeleteClick(Staff staff, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Staff")
                .setMessage("Are you sure you want to delete this staff member?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    db.collection("staff")
                            .document(staff.getId())
                            .delete()
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(this, "Staff deleted successfully",
                                            Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error deleting staff: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}