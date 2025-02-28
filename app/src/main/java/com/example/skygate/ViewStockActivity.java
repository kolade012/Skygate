package com.example.skygate;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ViewStockActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvNoStock;
    private FirebaseFirestore db;
    private StockAdapter stockAdapter; // Reference to the adapter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_stock);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Find views
        recyclerView = findViewById(R.id.recyclerViewStock);
        progressBar = findViewById(R.id.progressBar);
        tvNoStock = findViewById(R.id.tvNoStock);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        stockAdapter = new StockAdapter(this); // Create an instance of StockAdapter
        recyclerView.setAdapter(stockAdapter);

        // Load stock data
        loadStockData();

        // Handle back button
//        findViewById(R.id.backButton).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                finish(); // Finish the current activity and return to the previous one
//            }
//        });
    }

    private void loadStockData() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("inventory")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    List<StockItem> stockItems = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String productName = document.getString("product");
                        if (productName == null) {
                            productName = document.getId(); // Use document ID as product name if "product" field is missing
                        }
                        Long quantity = document.getLong("quantity");

                        if (productName != null && quantity != null) {
                            stockItems.add(new StockItem(productName, quantity.intValue()));
                        }
                    }

                    if (stockItems.isEmpty()) {
                        tvNoStock.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        tvNoStock.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        stockAdapter.setStockItems(stockItems);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvNoStock.setText("Error loading stock: " + e.getMessage());
                    tvNoStock.setVisibility(View.VISIBLE);
                });
    }
}