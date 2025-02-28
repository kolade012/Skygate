package com.example.skygate;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.skygate.adapters.EntriesAdapter;
import com.example.skygate.models.Entry;
import com.example.skygate.models.Product;
import com.example.skygate.adapters.EntriesAdapter;
import com.example.skygate.models.Entry;
import com.example.skygate.models.Product;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** @noinspection ALL */
public class AllEntriesActivity extends AppCompatActivity implements EntriesAdapter.OnEntryClickListener {
    private static final String TAG = "AllEntriesActivity";
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private EntriesAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvNoEntries;
    private TextView tvCurrentFilter;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat monthYearFormat;
    private Calendar selectedMonth;
    private boolean isInitialLoad = true;
    private Set<String> availableMonths = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_entries);

        // Initialize Firestore and date formatters
        db = FirebaseFirestore.getInstance();
        dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
        monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        selectedMonth = Calendar.getInstance();

        initializeViews();
        setupToolbar();
        setupScrollSync();
        setupRecyclerView();
        updateFilterDisplay();
        loadEntriesWithRealtimeUpdates();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewEntries);
        progressBar = findViewById(R.id.progressBar);
        tvNoEntries = findViewById(R.id.tvNoEntries);
        tvCurrentFilter = findViewById(R.id.tvCurrentFilter);
        ImageButton filterButton = findViewById(R.id.filterButton);

        filterButton.setOnClickListener(v -> showMonthYearPicker());

        // Always show filter controls
        filterButton.setVisibility(View.VISIBLE);
        tvCurrentFilter.setVisibility(View.VISIBLE);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar); // This line was missing
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("All Entries");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupScrollSync() {
        HorizontalScrollView headerScroll = findViewById(R.id.headerScrollView);
        HorizontalScrollView contentScroll = findViewById(R.id.contentScrollView);

        // Prevent null pointer exceptions with null checks
        if (headerScroll != null && contentScroll != null) {
            headerScroll.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) ->
                    contentScroll.scrollTo(scrollX, 0));

            contentScroll.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) ->
                    headerScroll.scrollTo(scrollX, 0));
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EntriesAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);
    }

    private void showMonthYearPicker() {
        try {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_month_year_picker, null);
            NumberPicker monthPicker = dialogView.findViewById(R.id.monthPicker);
            NumberPicker yearPicker = dialogView.findViewById(R.id.yearPicker);

            // Setup month picker
            String[] months = new DateFormatSymbols().getMonths();
            monthPicker.setMinValue(0);
            monthPicker.setMaxValue(11);
            monthPicker.setDisplayedValues(months);
            monthPicker.setValue(selectedMonth.get(Calendar.MONTH));

            // Setup year picker
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            yearPicker.setMinValue(currentYear - 5);
            yearPicker.setMaxValue(currentYear);
            yearPicker.setValue(selectedMonth.get(Calendar.YEAR));

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Select Month and Year")
                    .setView(dialogView)
                    .setPositiveButton("Apply", null) // Set to null initially
                    .setNegativeButton("Cancel", null)
                    .setNeutralButton("Clear Filter", null)
                    .create();

            dialog.setOnShowListener(dialogInterface -> {
                Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                Button neutralButton = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);

                positiveButton.setOnClickListener(v -> {
                    selectedMonth.set(Calendar.YEAR, yearPicker.getValue());
                    selectedMonth.set(Calendar.MONTH, monthPicker.getValue());
                    selectedMonth.set(Calendar.DAY_OF_MONTH, 1); // Reset to first day of month
                    updateFilterDisplay();
                    loadEntriesWithRealtimeUpdates();
                    dialog.dismiss();
                });

                neutralButton.setOnClickListener(v -> {
                    selectedMonth = Calendar.getInstance();
                    updateFilterDisplay();
                    loadEntriesWithRealtimeUpdates();
                    dialog.dismiss();
                });
            });

            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing picker: " + e.getMessage());
            Toast.makeText(this, "Error showing date picker", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateFilterDisplay() {
        try {
            String filterText = getString(R.string.current_filter, monthYearFormat.format(selectedMonth.getTime()));
            tvCurrentFilter.setText(filterText);
        } catch (Exception e) {
            Log.e(TAG, "Error updating filter display: " + e.getMessage());
            tvCurrentFilter.setText("Current Month");
        }
    }

    private void loadEntriesWithRealtimeUpdates() {
        if (!isInitialLoad && !isNetworkAvailable()) {
            showError("No internet connection");
            return;
        }

        showLoading(true);
        availableMonths.clear(); // Clear previous months

        try {
            // First query to get all available months
            db.collection("entries")
                    .orderBy("createdAt")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            for (DocumentSnapshot doc : task.getResult()) {
                                Long timestamp = doc.getLong("createdAt");
                                if (timestamp != null) {
                                    Calendar cal = Calendar.getInstance();
                                    cal.setTimeInMillis(timestamp);
                                    // Create a unique key for each month-year combination
                                    String monthYear = cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH);
                                    availableMonths.add(monthYear);
                                }
                            }

                            // If there's only one month available, set it as the selected month
                            if (availableMonths.size() == 1) {
                                String[] monthYear = availableMonths.iterator().next().split("-");
                                selectedMonth = Calendar.getInstance();
                                selectedMonth.set(Calendar.YEAR, Integer.parseInt(monthYear[0]));
                                selectedMonth.set(Calendar.MONTH, Integer.parseInt(monthYear[1]));
                                updateFilterDisplay();
                            }

                            // Load the filtered entries
                            loadFilteredEntries();
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up query: " + e.getMessage());
            showLoading(false);
            showError("Error loading entries");
        }
    }

    private void loadFilteredEntries() {
        // Calculate start and end of selected month
        Calendar startOfMonth = (Calendar) selectedMonth.clone();
        startOfMonth.set(Calendar.DAY_OF_MONTH, 1);
        startOfMonth.set(Calendar.HOUR_OF_DAY, 0);
        startOfMonth.set(Calendar.MINUTE, 0);
        startOfMonth.set(Calendar.SECOND, 0);

        Calendar endOfMonth = (Calendar) selectedMonth.clone();
        endOfMonth.set(Calendar.DAY_OF_MONTH, endOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH));
        endOfMonth.set(Calendar.HOUR_OF_DAY, 23);
        endOfMonth.set(Calendar.MINUTE, 59);
        endOfMonth.set(Calendar.SECOND, 59);

        Query query = db.collection("entries")
                .whereGreaterThanOrEqualTo("createdAt", startOfMonth.getTimeInMillis())
                .whereLessThanOrEqualTo("createdAt", endOfMonth.getTimeInMillis())
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .orderBy("controlNumber", Query.Direction.ASCENDING);

        query.addSnapshotListener((value, error) -> {
            showLoading(false);
            isInitialLoad = false;

            if (error != null) {
                Log.e(TAG, "Error loading entries: ", error);
                showError("Error loading entries");
                updateVisibility(true);
                return;
            }

            List<Entry> entries = new ArrayList<>();
            if (value != null) {
                for (DocumentSnapshot doc : value.getDocuments()) {
                    Entry entry = parseEntryFromDocument(doc);
                    if (entry != null) {
                        entries.add(entry);
                    }
                }
            }

            updateUI(entries);
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        }
        return false;
    }

    private Entry parseEntryFromDocument(DocumentSnapshot doc) {
        if (!doc.exists()) return null;

        try {
            String entryId = doc.getId();
            Long createdAtLong = doc.getLong("createdAt");
            String entryDate = formatDate(createdAtLong);
            int controlNum = getIntValue(doc.getLong("controlNumber"));
            String entryType = getStringValue(doc.getString("entryType"));
            String driver = getStringValue(doc.getString("driverId"));
            List<Product> products = parseProducts(doc.get("products"));

            return new Entry(
                    entryId,
                    entryDate,
                    controlNum,
                    entryType,
                    driver,
                    createdAtLong,
                    products
            );
        } catch (Exception e) {
            Log.e(TAG, "Error creating entry: " + e.getMessage());
            return null;
        }
    }

    private String formatDate(Long timestamp) {
        if (timestamp == null) return "N/A";
        try {
            return dateFormat.format(new Date(timestamp));
        } catch (Exception e) {
            Log.e(TAG, "Error formatting date: " + e.getMessage());
            return "N/A";
        }
    }

    private int getIntValue(Long value) {
        return value != null ? value.intValue() : 0;
    }

    private String getStringValue(String value) {
        return value != null ? value : "N/A";
    }

    @SuppressWarnings("unchecked")
    private List<Product> parseProducts(Object productsObj) {
        List<Product> productList = new ArrayList<>();
        if (!(productsObj instanceof List<?>)) return productList;

        List<?> productsList = (List<?>) productsObj;
        for (Object item : productsList) {
            if (!(item instanceof Map<?, ?>)) continue;

            try {
                Map<String, Object> productMap = (Map<String, Object>) item;
                String productName = (String) productMap.getOrDefault("product", "N/A");
                Long soldLong = (Long) productMap.getOrDefault("sold", 0L);
                int soldQuantity = soldLong != null ? soldLong.intValue() : 0;
                productList.add(new Product(productName, soldQuantity));
            } catch (Exception e) {
                Log.e(TAG, "Error parsing product: " + e.getMessage());
            }
        }
        return productList;
    }

    private void updateUI(List<Entry> entries) {
        showLoading(false);
        adapter.setEntries(entries);
        updateVisibility(entries.isEmpty());
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void updateVisibility(boolean isEmpty) {
        if (tvNoEntries != null) {
            tvNoEntries.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onEntryClick(Entry entry) {
        try {
            Intent intent = new Intent(this, EntryDetailsActivity.class);
            intent.putExtra("entry", entry);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error launching details: " + e.getMessage());
            showError("Error opening entry details");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}