package com.example.skygate;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StaffAdapter extends RecyclerView.Adapter<StaffAdapter.StaffViewHolder> {
    private List<Staff> staffList;
    private OnStaffClickListener listener;
    private Map<String, Double> currentMonthSales;
    private Map<String, Double> previousMonthSales;
    private SimpleDateFormat dateFormat;
    private DecimalFormat currencyFormat;
    private Context context;

    public interface OnStaffClickListener {
        void onEditClick(Staff staff, int position);
        void onDeleteClick(Staff staff, int position);
    }

    public StaffAdapter(List<Staff> staffList, OnStaffClickListener listener) {
        this.staffList = staffList;
        this.listener = listener;
        this.currentMonthSales = new HashMap<>();
        this.previousMonthSales = new HashMap<>();
        this.dateFormat = new SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault());

        // Custom Nigerian Naira formatting with thousand separators
        this.currencyFormat = new DecimalFormat("₦#,##0.00");
        currencyFormat.setGroupingSize(3);
        currencyFormat.setGroupingUsed(true);
    }

    @NonNull
    @Override
    public StaffViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_staff, parent, false);
        return new StaffViewHolder(view);
    }

    public void updateSalesData(Map<String, Double> currentMonthSales, Map<String, Double> previousMonthSales) {
        this.currentMonthSales = currentMonthSales;
        this.previousMonthSales = previousMonthSales;
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull StaffViewHolder holder, int position) {
        Staff staff = staffList.get(position);

        // Set name and role
        holder.nameTextView.setText(staff.getName());
        holder.roleTextView.setText(staff.getRole());

        // Format and display current month sales
        double currentSales = currentMonthSales.getOrDefault(staff.getName(), 0.0);
        holder.currentMonthSalesView.setText(String.format("Current Month Sales: ₦%.2f", currentSales));

        // Format and display previous month sales
        double previousSales = previousMonthSales.getOrDefault(staff.getName(), 0.0);
        holder.previousMonthSalesView.setText(String.format("Previous Month Sales: ₦%.2f", previousSales));

        // Set active status with color
        String activeStatus = staff.isActive() ? "Active" : "Inactive";
        holder.statusTextView.setText(activeStatus);

        // Set status color
        if (staff.isActive()) {
            holder.statusTextView.setTextColor(Color.parseColor("#4CAF50")); // Material Green
        } else {
            holder.statusTextView.setTextColor(Color.parseColor("#F44336")); // Material Red
        }

        // Format join date
        String joinDate = "Joined: " +
                (staff.getJoinDate() != null ? dateFormat.format(staff.getJoinDate().toDate()) : "N/A");
        holder.joinDateTextView.setText(joinDate);

        // Set performance metrics with improved Nigerian Naira formatting
        if (staff.getPerformanceMetrics() != null) {
            String lastActive = "Last Active: " +
                    (staff.getPerformanceMetrics().getLastActive() != null ?
                            dateFormat.format(staff.getPerformanceMetrics().getLastActive().toDate()) : "N/A");
            holder.lastActiveTextView.setText(lastActive);

            // Format total sales with thousand separators
//            double totalSales = staff.getPerformanceMetrics().getTotalSales();
//            String formattedSales = currencyFormat.format(totalSales);
//
//            // Format sales count with thousand separators
//            int salesCount = staff.getPerformanceMetrics().getSalesCount();
//            String formattedCount = NumberFormat.getNumberInstance(Locale.getDefault()).format(salesCount);

//            String salesInfo = String.format("Sales: %s (Total: %s)",
//                    formattedCount,
//                    formattedSales);
//            holder.salesInfoTextView.setText(salesInfo);
//        } else {
//            holder.lastActiveTextView.setText("Last Active: N/A");
//            holder.salesInfoTextView.setText("Sales: 0 (Total: ₦0.00)");
        }

        // Set click listeners
        holder.editButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(staff, position);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(staff, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return staffList.size();
    }

    public void updateList(List<Staff> newList) {
        staffList = newList;
        notifyDataSetChanged();
    }

    static class StaffViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView roleTextView;
        TextView statusTextView;
        TextView joinDateTextView;
        TextView lastActiveTextView;
        TextView salesInfoTextView;
        TextView currentMonthSalesView;
        TextView previousMonthSalesView;
        ImageButton editButton;
        ImageButton deleteButton;

        StaffViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.text_staff_name);
            roleTextView = itemView.findViewById(R.id.text_staff_role);
            statusTextView = itemView.findViewById(R.id.text_staff_status);
            joinDateTextView = itemView.findViewById(R.id.text_join_date);
            lastActiveTextView = itemView.findViewById(R.id.text_last_active);
            //salesInfoTextView = itemView.findViewById(R.id.text_sales_info);
            editButton = itemView.findViewById(R.id.btn_edit_staff);
            deleteButton = itemView.findViewById(R.id.btn_delete_staff);
            currentMonthSalesView = itemView.findViewById(R.id.text_current_month_sales);
            previousMonthSalesView = itemView.findViewById(R.id.text_previous_month_sales);
        }
    }
}