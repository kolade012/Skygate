package com.example.skygate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class StockAdapter extends RecyclerView.Adapter<StockAdapter.StockViewHolder> {

    private List<StockItem> stockItems;
    private Context context;

    public StockAdapter(Context context) {
        this.context = context;
        this.stockItems = new ArrayList<>();
    }

    public void setStockItems(List<StockItem> stockItems) {
        this.stockItems = stockItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.stock_item_layout, parent, false);
        return new StockViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StockViewHolder holder, int position) {
        StockItem stockItem = stockItems.get(position);
        holder.productName.setText(stockItem.getProduct());
        holder.productQuantity.setText("Quantity: " + stockItem.getQuantity());
    }

    @Override
    public int getItemCount() {
        return stockItems.size();
    }

    static class StockViewHolder extends RecyclerView.ViewHolder {
        TextView productName, productQuantity;

        public StockViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.productName);
            productQuantity = itemView.findViewById(R.id.productQuantity);
        }
    }
}