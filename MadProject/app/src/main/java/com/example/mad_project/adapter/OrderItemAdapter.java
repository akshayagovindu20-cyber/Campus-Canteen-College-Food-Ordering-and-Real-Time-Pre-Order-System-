package com.example.mad_project.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mad_project.R;
import com.example.mad_project.model.OrderItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.OrderItemViewHolder> {

    private final Context context;
    private List<OrderItem> items = new ArrayList<>();

    public OrderItemAdapter(Context context, List<OrderItem> items) {
        this.context = context;
        this.items = items != null ? items : new ArrayList<>();
    }

    public void setItems(List<OrderItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_food_snapshot, parent, false);
        return new OrderItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderItemViewHolder holder, int position) {
        OrderItem item = items.get(position);

        holder.tvName.setText(item.getFoodName());
        holder.tvPriceQty.setText(String.format(Locale.getDefault(), "₹ %.2f  x  %d", item.getUnitPrice(), item.getQuantity()));
        holder.tvTotal.setText(String.format(Locale.getDefault(), "₹ %.2f", item.getItemTotal()));

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.ic_food_placeholder)
                    .error(R.drawable.ic_food_placeholder)
                    .into(holder.imgItem);
        } else {
            holder.imgItem.setImageResource(R.drawable.ic_food_placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class OrderItemViewHolder extends RecyclerView.ViewHolder {
        ImageView imgItem;
        TextView tvName, tvPriceQty, tvTotal;

        public OrderItemViewHolder(@NonNull View itemView) {
            super(itemView);
            imgItem = itemView.findViewById(R.id.imgOrderItem);
            tvName = itemView.findViewById(R.id.tvSnapshotName);
            tvPriceQty = itemView.findViewById(R.id.tvSnapshotPriceQty);
            tvTotal = itemView.findViewById(R.id.tvSnapshotTotal);
        }
    }
}
