package com.example.produceapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ProduceAdapter extends RecyclerView.Adapter<ProduceAdapter.ViewHolder> {
    private List<ProduceItem> items;
    private boolean isRetailMode = false;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ProduceItem item);
    }

    public ProduceAdapter(List<ProduceItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setRetailMode(boolean isRetailMode) {
        this.isRetailMode = isRetailMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_produce, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProduceItem item = items.get(position);
        holder.nameText.setText(item.name);
        holder.categoryText.setText(item.category);
        
        // 批發價(公斤) 轉 零售價(台斤)
        double displayPrice = isRetailMode ? (item.currentPrice * 2.5 * 0.6) : item.currentPrice;
        holder.priceText.setText(String.format("$%.1f", displayPrice));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView categoryText;
        TextView priceText;

        public ViewHolder(View view) {
            super(view);
            nameText = view.findViewById(R.id.nameText);
            categoryText = view.findViewById(R.id.categoryText);
            priceText = view.findViewById(R.id.priceText);
        }
    }
}
