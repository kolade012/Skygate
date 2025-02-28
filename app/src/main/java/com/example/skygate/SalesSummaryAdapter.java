package com.example.skygate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SalesSummaryAdapter extends RecyclerView.Adapter<SalesSummaryAdapter.ViewHolder> {
    private List<ProductSummary> summaries = new ArrayList<>();
    private double totalAmount = 0.0;

    public void updateData(List<ProductSummary> newSummaries, double newTotalAmount) {
        this.summaries = newSummaries;
        this.totalAmount = newTotalAmount;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sales_summary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (position == 0) {
            // Header for total amount
            holder.productName.setText("Total Sales Amount");
            holder.quantity.setVisibility(View.GONE);
            holder.amount.setText(String.format(Locale.getDefault(), "%.2f", totalAmount));
            holder.itemView.setBackgroundResource(R.color.colorPrimary);
        } else {
            ProductSummary summary = summaries.get(position - 1);
            holder.productName.setText(summary.getProduct());
            holder.quantity.setVisibility(View.VISIBLE);
            holder.quantity.setText("Qty: " + summary.getTotalQuantity());
            holder.amount.setText(String.format(Locale.getDefault(), "%.2f", summary.getTotalAmount()));
            holder.itemView.setBackgroundResource(android.R.color.transparent);
        }
    }

    @Override
    public int getItemCount() {
        return summaries.size() + 1; // +1 for the total amount header
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView productName;
        TextView quantity;
        TextView amount;

        ViewHolder(View view) {
            super(view);
            productName = view.findViewById(R.id.text_product);
            quantity = view.findViewById(R.id.text_quantity);
            amount = view.findViewById(R.id.text_amount);
        }
    }
}