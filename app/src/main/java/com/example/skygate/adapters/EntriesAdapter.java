package com.example.skygate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.skygate.R;
import com.example.skygate.models.Entry;
import com.example.skygate.models.Product;
import com.example.skygate.models.Product;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class EntriesAdapter extends RecyclerView.Adapter<EntriesAdapter.EntryViewHolder> {
    private List<Entry> entries;
    private OnEntryClickListener listener;

    public interface OnEntryClickListener {
        void onEntryClick(Entry entry);
    }

    public EntriesAdapter(List<Entry> entries, OnEntryClickListener listener) {
        this.entries = entries;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_entry, parent, false); // Using item_entry.xml
        return new EntryViewHolder(view, parent.getContext());
    }

    @Override
    public void onBindViewHolder(@NonNull EntryViewHolder holder, int position) {
        // Check if the position is valid
        if (position < 0 || position >= entries.size()) {
            return; // Handle invalid position
        }

        Entry entry = entries.get(position);

        // Check if entry is null before accessing properties
        if (entry == null) {
            return; // Or handle the null case in another way
        }

        // Set the date
        holder.tvDate.setText(entry.getDate());

        // Set the control number
        holder.tvControlNumber.setText(String.format("%s", entry.getControlNumber()));

        // Set the entry type with appropriate background color
        holder.tvEntryType.setText(entry.getEntryType());
        int backgroundColorRes;

        switch (entry.getEntryType().toLowerCase()) {
            case "stock received":
                backgroundColorRes = R.color.green_color; // Assuming you have green_color defined in your colors.xml
                break;
            case "sales":
            case "stock out":
                backgroundColorRes = R.color.red_color; // Assuming you have red_color defined in your colors.xml
                break;
            default:
                backgroundColorRes = R.color.gray; // Or any other default color
        }

        holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.context, backgroundColorRes));

        // Set the driver name
        holder.tvDriver.setText(entry.getDriver());

        // Set product quantities
        holder.tv30CL.setText(String.valueOf(getProductQuantityByName(entry.getProducts(), "30CL")));
        holder.tv35CL_7UP.setText(String.valueOf(getProductQuantityByName(entry.getProducts(), "35CL 7UP")));
        holder.tv35CL_M_D.setText(String.valueOf(getProductQuantityByName(entry.getProducts(), "35CL M.D")));
        holder.tv50CL.setText(String.valueOf(getProductQuantityByName(entry.getProducts(), "50CL")));
        holder.tvPEPSI.setText(String.valueOf(getProductQuantityByName(entry.getProducts(), "PEPSI")));
        holder.tvTBL.setText(String.valueOf(getProductQuantityByName(entry.getProducts(), "TBL")));
        holder.tvG_APPLE.setText(String.valueOf(getProductQuantityByName(entry.getProducts(), "G.APPLE")));
        holder.tvPINEAPPLE.setText(String.valueOf(getProductQuantityByName(entry.getProducts(), "PINEAPPLE")));
        holder.tv75CL_AQUAFINA.setText(String.valueOf(getProductQuantityByName(entry.getProducts(), "75CL AQUAFINA")));
        holder.tvRED_APPLE.setText(String.valueOf(getProductQuantityByName(entry.getProducts(), "RED APPLE")));
        holder.tv7UP.setText(String.valueOf(getProductQuantityByName(entry.getProducts(), "7UP")));
        holder.tvORANGE.setText(String.valueOf(getProductQuantityByName(entry.getProducts(), "ORANGE")));
        holder.tvSODA.setText(String.valueOf(getProductQuantityByName(entry.getProducts(), "SODA")));
        holder.tvS_ORANGE.setText(String.valueOf(getProductQuantityByName(entry.getProducts(), "S.ORANGE")));
        holder.tvS_7UP.setText(String.valueOf(getProductQuantityByName(entry.getProducts(), "S.7UP")));
        holder.tvSK_50CL.setText(String.valueOf(getProductQuantityByName(entry.getProducts(), "SK 50CL")));
        holder.tvSK_30CL.setText(String.valueOf(getProductQuantityByName(entry.getProducts(), "SK 30CL")));

        // Set click listener
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEntryClick(entry);
            }
        });
    }

    @Override
    public int getItemCount() {
        return entries != null ? entries.size() : 0;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
        notifyDataSetChanged();
    }

    static class EntryViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView tvDate;
        TextView tvControlNumber;
        TextView tvEntryType;
        TextView tvDriver;
        TextView tv30CL;
        TextView tv35CL_7UP;
        TextView tv35CL_M_D;
        TextView tv50CL;
        TextView tvPEPSI;
        TextView tvTBL;
        TextView tvG_APPLE;
        TextView tvPINEAPPLE;
        TextView tv75CL_AQUAFINA;
        TextView tvRED_APPLE;
        TextView tv7UP;
        TextView tvORANGE;
        TextView tvSODA;
        TextView tvS_ORANGE;
        TextView tvS_7UP;
        TextView tvSK_50CL;
        TextView tvSK_30CL;
        private Context context; // Add context field

        EntryViewHolder(View itemView, Context context) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            tvDate = itemView.findViewById(R.id.tvDate);
            tvControlNumber = itemView.findViewById(R.id.tvControlNumber);
            tvEntryType = itemView.findViewById(R.id.tvEntryType);
            tvDriver = itemView.findViewById(R.id.tvDriver);
            tv30CL = itemView.findViewById(R.id.tv30CL);
            tv35CL_7UP = itemView.findViewById(R.id.tv35CL_7UP);
            tv35CL_M_D = itemView.findViewById(R.id.tv35CL_M_D);
            tv50CL = itemView.findViewById(R.id.tv50CL);
            tvPEPSI = itemView.findViewById(R.id.tvPEPSI);
            tvTBL = itemView.findViewById(R.id.tvTBL);
            tvG_APPLE = itemView.findViewById(R.id.tvG_APPLE);
            tvPINEAPPLE = itemView.findViewById(R.id.tvPINEAPPLE);
            tv75CL_AQUAFINA = itemView.findViewById(R.id.tv75CL_AQUAFINA);
            tvRED_APPLE = itemView.findViewById(R.id.tvRED_APPLE);
            tv7UP = itemView.findViewById(R.id.tv7UP);
            tvORANGE = itemView.findViewById(R.id.tvORANGE);
            tvSODA = itemView.findViewById(R.id.tvSODA);
            tvS_ORANGE = itemView.findViewById(R.id.tvS_ORANGE);
            tvS_7UP = itemView.findViewById(R.id.tvS_7UP);
            tvSK_50CL = itemView.findViewById(R.id.tvSK_50CL);
            tvSK_30CL = itemView.findViewById(R.id.tvSK_30CL);
            this.context = context;
        }
    }

    private int getProductQuantityByName(List<Product> products, String productName) {
        for (Product product : products) {
            if (product.getName().equals(productName)) {
                return product.getSoldQuantity();
            }
        }
        return 0; // If product not found, return 0
    }
}