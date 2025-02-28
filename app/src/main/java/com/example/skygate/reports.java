package com.example.skygate;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.skygate.ProductSummary;
import com.example.skygate.SalesSummaryAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class reports extends AppCompatActivity {
    private AutoCompleteTextView driverNameInput;
    private EditText dateFromInput;
    private EditText dateToInput;
    private TextView summaryTitle;
    private RecyclerView salesSummaryRecycler;
    private FirebaseFirestore db;
    private SimpleDateFormat dateFormat;
    private SalesSummaryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Initialize views
        initializeViews();
        setupDatePickers();
        loadDriverNames();

        // Setup RecyclerView
        salesSummaryRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SalesSummaryAdapter();
        salesSummaryRecycler.setAdapter(adapter);

        // Generate report button click listener
        findViewById(R.id.btn_generate_report).setOnClickListener(v -> generateReport());
    }

    private void initializeViews() {
        driverNameInput = findViewById(R.id.driver_name);
        dateFromInput = findViewById(R.id.date_from);
        dateToInput = findViewById(R.id.date_to);
        summaryTitle = findViewById(R.id.summary_title);
        salesSummaryRecycler = findViewById(R.id.recycler_sales_summary);
    }

    private void setupDatePickers() {
        View.OnClickListener dateClickListener = v -> {
            final EditText clickedDate = (EditText) v;
            final Calendar calendar = Calendar.getInstance();

            new DatePickerDialog(this, (view, year, month, day) -> {
                calendar.set(year, month, day);
                clickedDate.setText(dateFormat.format(calendar.getTime()));
            },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)).show();
        };

        dateFromInput.setOnClickListener(dateClickListener);
        dateToInput.setOnClickListener(dateClickListener);
    }

    private void loadDriverNames() {
        db.collection("sales")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Set<String> driverNames = new HashSet<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String driverId = document.getString("driverId");
                        if (driverId != null) {
                            driverNames.add(driverId);
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_dropdown_item_1line,
                            new ArrayList<>(driverNames)
                    );
                    driverNameInput.setAdapter(adapter);
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Error loading drivers: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    private void generateReport() {
        String driverId = driverNameInput.getText().toString();
        String fromDate = dateFromInput.getText().toString();
        String toDate = dateToInput.getText().toString();

        if (driverId.isEmpty() || fromDate.isEmpty() || toDate.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Date startDate = dateFormat.parse(fromDate);
            Date endDate = dateFormat.parse(toDate);

            db.collection("sales")
                    .whereEqualTo("driverId", driverId)
                    .whereGreaterThanOrEqualTo("date", startDate)
                    .whereLessThanOrEqualTo("date", endDate)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        Map<String, ProductSummary> productSummaries = new HashMap<>();
                        double totalAmount = 0;

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String product = document.getString("product");
                            int quantity = document.getLong("quantity").intValue();
                            double amount = document.getDouble("amount");

                            ProductSummary summary = productSummaries.getOrDefault(product,
                                    new ProductSummary(product, 0, 0.0));
                            summary.addQuantity(quantity);
                            summary.addAmount(amount);
                            productSummaries.put(product, summary);

                            totalAmount += amount;
                        }

                        summaryTitle.setVisibility(View.VISIBLE);
                        adapter.updateData(new ArrayList<>(productSummaries.values()), totalAmount);
                    })
                    .addOnFailureListener(e -> Toast.makeText(this,
                            "Error generating report: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show());

        } catch (Exception e) {
            Toast.makeText(this, "Error with date format", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}