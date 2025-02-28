package com.example.skygate;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.LargeValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** @noinspection ALL, deprecation , deprecation , deprecation , deprecation , deprecation , deprecation , deprecation */
public class AdminDashboardActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "AdminDashboard";
    private static final String COLLECTION_SALES = "sales";
    private static final String COLLECTION_INVENTORY = "inventory";
    private static final String COLLECTION_STAFF = "staff";

    private DrawerLayout drawerLayout;
    private LineChart salesChart;
    private BarChart inventoryChart;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView totalSalesText, inventoryValueText, ordersText, staffText, topPerformerText, totalSalesAmountText, averageSalesText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        initializeFirebase();
        initializeViews();
        setupToolbarAndNavigation();
        setupCharts();
        loadDashboardData();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    private void initializeViews() {
        totalSalesText = findViewById(R.id.text_total_sales);
        inventoryValueText = findViewById(R.id.text_inventory_value);
        ordersText = findViewById(R.id.text_orders);
        staffText = findViewById(R.id.text_staff);
        salesChart = findViewById(R.id.sales_chart);
        inventoryChart = findViewById(R.id.inventory_chart);
//        topPerformerText = findViewById(R.id.text_top_performer);
//        totalSalesAmountText = findViewById(R.id.text_total_sales_amount);
//        averageSalesText = findViewById(R.id.text_average_sales);
    }

    private void setupToolbarAndNavigation() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setupNavigationDrawer(toolbar);
    }

    private void setupNavigationDrawer(Toolbar toolbar) {
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setupCharts() {
        setupSalesChart();
        setupInventoryChart();
    }

    private void setupSalesChart() {
        if (salesChart == null) return;

        salesChart.getDescription().setEnabled(false);
        salesChart.setDrawGridBackground(false);
        salesChart.setDrawBorders(true);
        salesChart.setPinchZoom(true);

        // X Axis setup
        XAxis xAxis = salesChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        // Y Axis setup
        YAxis leftAxis = salesChart.getAxisLeft();
        leftAxis.setValueFormatter(new LargeValueFormatter());
        leftAxis.setDrawGridLines(true);
        leftAxis.setSpaceTop(35f);

        // Disable right axis
        salesChart.getAxisRight().setEnabled(false);

        // Legend setup
        salesChart.getLegend().setEnabled(true);
    }

    private void setupInventoryChart() {
        if (inventoryChart == null) return;

        inventoryChart.getDescription().setEnabled(false);
        inventoryChart.setDrawGridBackground(false);
        inventoryChart.setDrawBorders(true);
        inventoryChart.setPinchZoom(true);

        // X Axis setup
        XAxis xAxis = inventoryChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(45f);

        // Y Axis setup
        YAxis leftAxis = inventoryChart.getAxisLeft();
        leftAxis.setValueFormatter(new LargeValueFormatter());
        leftAxis.setDrawGridLines(true);
        leftAxis.setSpaceTop(35f);

        // Disable right axis
        inventoryChart.getAxisRight().setEnabled(false);
    }

    private void loadDashboardData() {
        updateSalesMetrics();
        updateInventoryMetrics();
        updateOrderMetrics();
        updateStaffMetrics();
    }

    private void updateSalesMetrics() {
        // Get date for 15 days ago to match the image
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -15);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

        // Set up real-time listener for sales
        db.collection(COLLECTION_SALES)
                .whereGreaterThanOrEqualTo("date", cal.getTime())
                .orderBy("date", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        showError("Failed to listen for sales updates: " + e.getMessage());
                        return;
                    }

                    if (snapshots != null) {
                        processDailySales(snapshots);
                    }
                });
    }

    private void processDailySales(QuerySnapshot snapshots) {
        TreeMap<String, Float> dailySales = new TreeMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);

        // Initialize last 15 days with zero
        Calendar initCal = Calendar.getInstance();
        initCal.add(Calendar.DAY_OF_MONTH, -15);
        for (int i = 0; i <= 15; i++) {
            dailySales.put(sdf.format(initCal.getTime()), 0f);
            initCal.add(Calendar.DAY_OF_MONTH, 1);
        }

        // Get today's date for filtering today's sales
        String today = sdf.format(Calendar.getInstance().getTime());
        float todaySales = 0f;

        // Process sales data
        for (QueryDocumentSnapshot document : snapshots) {
            Date saleDate = document.getDate("date");
            double amount = document.getDouble("amount") != null ?
                    document.getDouble("amount") : 0.0;

            if (saleDate != null) {
                String dateKey = sdf.format(saleDate);
                float currentAmount = dailySales.getOrDefault(dateKey, 0f);
                dailySales.put(dateKey, currentAmount + (float)amount);

                // Track today's sales separately
                if (dateKey.equals(today)) {
                    todaySales += amount;
                }
            }
        }

        updateSalesUI(todaySales, dailySales);
    }

    private void updateSalesUI(float todaySales, TreeMap<String, Float> dailySales) {
        // Update total sales text with only today's sales
        String formattedSales = NumberFormat.getCurrencyInstance(
                new Locale("en", "NG")).format(todaySales);
        totalSalesText.setText(formattedSales);

        updateSalesChart(dailySales);
    }

    private void updateSalesChart(TreeMap<String, Float> dailySales) {
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Float> entry : dailySales.entrySet()) {
            entries.add(new Entry(index, entry.getValue()));
            // Format date to match the image (MM/dd/yyyy)
            labels.add(entry.getKey());
            index++;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Daily Sales");
        styleLineDataSet(dataSet);

        LineData lineData = new LineData(dataSet);
        salesChart.setData(lineData);

        // Configure X-axis to match the image
        XAxis xAxis = salesChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelRotationAngle(45f);
        xAxis.setLabelCount(labels.size());
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.LTGRAY);

        // Configure Y-axis
        YAxis leftAxis = salesChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);
        salesChart.getAxisRight().setEnabled(false);

        salesChart.setDrawGridBackground(false);
        salesChart.getDescription().setEnabled(false);
        salesChart.setTouchEnabled(true);
        salesChart.setDragEnabled(true);
        salesChart.setScaleEnabled(true);

        salesChart.invalidate();
    }

    /** @noinspection deprecation*/
    private void styleLineDataSet(LineDataSet dataSet) {
        int primaryColor = getResources().getColor(R.color.colorPrimary);
        dataSet.setColor(primaryColor);
        dataSet.setValueTextColor(primaryColor);
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(true);  // Show data points like in the image
        dataSet.setCircleColor(primaryColor);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(true);   // Show values like in the image
        dataSet.setValueTextSize(9f);
        dataSet.setMode(LineDataSet.Mode.LINEAR);  // Straight lines between points like in image
        dataSet.setDrawFilled(false);  // No gradient fill like in the image
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.US, "%.0f", value);  // Show whole numbers like in image
            }
        });
    }

    private void updateInventoryMetrics() {
        // Set up real-time listener for inventory
        db.collection(COLLECTION_INVENTORY)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        showError("Failed to listen for inventory updates: " + e.getMessage());
                        return;
                    }

                    if (snapshots != null) {
                        Map<String, Long> categoryQuantities = new HashMap<>();

                        for (QueryDocumentSnapshot document : snapshots) {
                            String productName = document.getId();
                            Long quantity = document.getLong("quantity");
                            if (quantity == null) quantity = 0L;

                            String category = getProductCategory(productName);
                            categoryQuantities.merge(category, quantity, Long::sum);
                        }

                        updateInventoryUI(categoryQuantities);
                    }
                });
    }

    private void updateInventoryUI(Map<String, Long> categoryQuantities) {
        // Update total quantity text
        long totalQuantity = categoryQuantities.values().stream().mapToLong(Long::longValue).sum();
        inventoryValueText.setText(String.format(Locale.getDefault(), "%d units", totalQuantity));

        // Update inventory chart
        updateInventoryChart(categoryQuantities);
    }

    private String getProductCategory(String productName) {
        if (productName == null) return "Other";

        // Normalize input for consistent comparison
        String normalizedName = productName.trim().toUpperCase();

        // RGB Category (exact matches only)
        if (normalizedName.equals("50CL") ||  // 50CL alone goes to RGB
                normalizedName.equals("30CL") ||
                normalizedName.equals("35CL 7UP") ||
                normalizedName.equals("35CL M.D")) {
            return "RGB";
        }

        // 50CL SK Category - check this before 50CL PET to avoid wrong categorization
        if (normalizedName.contains("SK 50CL")) {
            return "50CL SK";
        }

        // 30CL SK Category
        if (normalizedName.contains("SK 30CL")) {
            return "30CL SK";
        }

        // 50CL PET Category - check after SK to avoid miscategorization
        if ((normalizedName.contains("50CL") && !normalizedName.contains("SK")) ||
                normalizedName.equals("PEPSI") ||
                normalizedName.equals("TBL") ||
                normalizedName.equals("PINEAPPLE") ||
                normalizedName.equals("G.APPLE") ||
                normalizedName.equals("RED APPLE") ||
                normalizedName.equals("7UP") ||
                normalizedName.equals("ORANGE") ||
                normalizedName.equals("SODA")) {
            return "50CL PET";
        }

        // 40CL PET Category
        if (normalizedName.equals("S.ORANGE") ||
                normalizedName.equals("S.7UP") ||
                normalizedName.equals("40CL")) {
            return "40CL PET";
        }

        // Water Category
        if (normalizedName.contains("75CL AQUAFINA")) {
            return "WATER";
        }

        return "Other";
    }

    private void updateInventoryChart(Map<String, Long> categoryQuantities) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        // Create entries in a specific order
        String[] orderedCategories = {"RGB", "50CL PET", "40CL PET", "50CL SK", "30CL SK", "WATER"};

        for (String category : orderedCategories) {
            Long quantity = categoryQuantities.getOrDefault(category, 0L);
            entries.add(new BarEntry(index, quantity));
            labels.add(category);
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Inventory by Category");
        styleBarDataSet(dataSet);

        BarData barData = new BarData(dataSet);
        inventoryChart.setData(barData);

        // Configure X-axis for better label display
        XAxis xAxis = inventoryChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelRotationAngle(45f);
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        inventoryChart.animateY(1000);
        inventoryChart.invalidate();
    }

    private void styleBarDataSet(BarDataSet dataSet) {
        dataSet.setColor(getResources().getColor(R.color.colorAccent));
        dataSet.setValueTextColor(getResources().getColor(R.color.colorAccent));
        dataSet.setDrawValues(true);
        dataSet.setValueFormatter(new LargeValueFormatter());
        dataSet.setValueTextSize(12f);
    }

    private void updateOrderMetrics() {
        // Get start and end of current day
        Calendar startCal = Calendar.getInstance();
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);

        Calendar endCal = Calendar.getInstance();
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);

        // Track orders and their statuses for today only
        db.collection("orders")
                .whereGreaterThanOrEqualTo("date", startCal.getTime())
                .whereLessThanOrEqualTo("date", endCal.getTime())
                .orderBy("date", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        showError("Failed to listen for order updates: " + e.getMessage());
                        return;
                    }

                    if (snapshots != null) {
                        processOrderMetrics(snapshots);
                    }
                });
    }

    private void processOrderMetrics(QuerySnapshot snapshots) {
        int todayCompletedOrders = 0;

        // Process each order
        for (QueryDocumentSnapshot doc : snapshots) {
            String status = doc.getString("status");

            // Count only completed orders
            if ("completed".equals(status)) {
                todayCompletedOrders++;
            }
        }

        // Update UI with today's completed orders count
        ordersText.setText(String.valueOf(todayCompletedOrders));
    }

    private void updateStaffMetrics() {
        db.collection(COLLECTION_STAFF)
                .whereEqualTo("active", true)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        showError("Failed to listen for staff updates: " + e.getMessage());
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        int activeStaffCount = 0;
                        double totalSalesAmount = 0;
                        int totalSalesCount = 0;
                        String topPerformer = "";
                        double highestSales = 0;

                        for (QueryDocumentSnapshot document : snapshots) {
                            activeStaffCount++;

                            // Get staff member data
                            String name = document.getString("name");
                            String role = document.getString("role");

                            // Get performance metrics
                            if (document.contains("performanceMetrics")) {
                                double salesAmount = document.getDouble("performanceMetrics.totalSales") != null ?
                                        document.getDouble("performanceMetrics.totalSales") : 0;
                                long salesCount = document.getLong("performanceMetrics.salesCount") != null ?
                                        document.getLong("performanceMetrics.salesCount") : 0;

                                totalSalesAmount += salesAmount;
                                totalSalesCount += salesCount;

                                // Track top performer
                                if (salesAmount > highestSales) {
                                    highestSales = salesAmount;
                                    topPerformer = name;
                                }
                            }
                        }

                        // Calculate averages
                        double averageSalesPerStaff = activeStaffCount > 0 ?
                                totalSalesAmount / activeStaffCount : 0;

                        // Update UI
                        updateStaffUI(activeStaffCount, totalSalesAmount, averageSalesPerStaff,
                                totalSalesCount, topPerformer);
                    } else {
                        // No active staff members
                        updateStaffUI(0, 0, 0, 0, "No active staff");
                    }
                });
    }

    private void updateStaffUI(int staffCount, double totalSales, double averageSales,
                               int totalSalesCount, String topPerformer) {
        runOnUiThread(() -> {
            // Format currency values
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "NG"));

            // Update TextViews
            staffText.setText(String.format(Locale.getDefault(), "%d Active Staff", staffCount));
//            topPerformerText.setText(String.format("Top Performer: %s", topPerformer));
//            totalSalesAmountText.setText(String.format("Total Sales: %s",
//                    currencyFormat.format(totalSales)));
//            averageSalesText.setText(String.format("Avg Sales per Staff: %s",
//                    currencyFormat.format(averageSales)));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // If you need to store listener registrations to remove them later
        // you can store them in class variables and remove them here
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            startActivity(new Intent(this, AdminDashboardActivity.class));
        } else if (id == R.id.nav_inventory) {
            startActivity(new Intent(this, ViewStockActivity.class));
        } else if (id == R.id.nav_staff) {
            startActivity(new Intent(this, StaffActivity.class));
        } else if (id == R.id.nav_all_entries2) {
            startActivity(new Intent(this, AllEntriesActivity.class));
        } else if (id == R.id.nav_reports) {
            startActivity(new Intent(this, reports.class));
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, settings.class));
        } else if (id == R.id.nav_logout2) {
            logout();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(this, UserTypeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showError(String message) {
        // Create and show a Snackbar with the error message
        View rootView = findViewById(android.R.id.content);
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getResources().getColor(R.color.error_background))
                .setTextColor(getResources().getColor(R.color.error_text))
                .setAction("Dismiss", v -> {})
                .show();

        // Log the error for debugging
        Log.e("ErrorHandler", message);
    }
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}