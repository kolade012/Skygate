package com.example.skygate;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class Staff {
    private String id;
    private String name;
    private String role;
    private boolean active;
    private Timestamp joinDate;
    private Timestamp lastUpdated;
    private PerformanceMetrics performanceMetrics;

    public Staff() {
        // Empty constructor needed for Firebase
    }

    public void setLastUpdated(Timestamp timestamp) {
        this.lastUpdated = timestamp;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public Task<Void> updatePerformanceMetrics() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Query all completed orders for this staff member
        return db.collection("orders")
                .whereEqualTo("driver", this.id)
                .whereEqualTo("status", "completed")
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    QuerySnapshot querySnapshot = task.getResult();
                    int totalCount = 0;
                    double totalAmount = 0.0;

                    // Calculate totals
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        totalCount++;
                        Double amount = document.getDouble("amount");
                        if (amount != null) {
                            totalAmount += amount;
                        }
                    }

                    // Update performance metrics
                    PerformanceMetrics metrics = getPerformanceMetrics();
                    if (metrics == null) {
                        metrics = new PerformanceMetrics();
                    }

                    metrics.setLastActive(Timestamp.now());
//                    metrics.setSalesCount(totalCount);
//                    metrics.setTotalSales((int) totalAmount);
                    setPerformanceMetrics(metrics);

                    // Create update map
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("performanceMetrics", metrics);
                    updates.put("lastUpdated", Timestamp.now());

                    // Update the staff document
                    return db.collection("staff")
                            .document(this.id)
                            .update(updates);
                });
    }

    public static class PerformanceMetrics {
        private Timestamp lastActive;
        private int salesCount;
        private int totalSales;

        public PerformanceMetrics() {
            // Empty constructor needed for Firebase
        }

        // Getters and Setters
        public Timestamp getLastActive() { return lastActive; }
        public void setLastActive(Timestamp lastActive) { this.lastActive = lastActive; }
//        public int getSalesCount() { return salesCount; }
//        public void setSalesCount(int salesCount) { this.salesCount = salesCount; }
//        public int getTotalSales() { return totalSales; }
//        public void setTotalSales(int totalSales) { this.totalSales = totalSales; }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Timestamp getJoinDate() { return joinDate; }
    public void setJoinDate(Timestamp joinDate) { this.joinDate = joinDate; }
    public PerformanceMetrics getPerformanceMetrics() { return performanceMetrics; }
    public void setPerformanceMetrics(PerformanceMetrics performanceMetrics) {
        this.performanceMetrics = performanceMetrics;
    }
}