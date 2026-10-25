package com.example.mad_project.adapter;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_project.R;
import com.example.mad_project.model.Order;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CustomerOrderAdapter extends RecyclerView.Adapter<CustomerOrderAdapter.OrderViewHolder> {

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    private final Context context;
    private List<Order> orders = new ArrayList<>();
    private final OnOrderClickListener listener;

    public CustomerOrderAdapter(Context context, OnOrderClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setOrders(List<Order> list) {
        this.orders = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_customer_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);

        holder.tvOrderId.setText("Order #" + (order.getOrderId() != null ? order.getOrderId().substring(0, Math.min(8, order.getOrderId().length())).toUpperCase() : ""));
        holder.tvItemsSummary.setText(order.getFormattedItemsSummary());
        holder.tvTotalAmount.setText(String.format(Locale.getDefault(), "Total: ₹ %.2f", order.getTotalAmount()));

        if (order.getPickupTime() != null && !order.getPickupTime().isEmpty()) {
            holder.tvPickupSlot.setText("Pickup: " + order.getPickupTime());
        } else {
            holder.tvPickupSlot.setText("Pickup: ASAP");
        }

        if (order.getCreatedAt() > 0) {
            String timeStr = DateFormat.format("dd MMM, hh:mm a", new Date(order.getCreatedAt())).toString();
            holder.tvOrderDate.setText("Placed on " + timeStr);
        }

        String status = order.getStatus() != null ? order.getStatus().toUpperCase() : Order.STATUS_PLACED;
        holder.tvStatusBadge.setText(status);

        if (Order.STATUS_READY.equalsIgnoreCase(status)) {
            // PROMINENT ORDER READY BANNER VISIBLE
            holder.layoutReadyBanner.setVisibility(View.VISIBLE);
            holder.tvStatusBadge.setTextColor(context.getColor(R.color.canteen_green));
            holder.tvStatusBadge.getBackground().setTint(context.getColor(R.color.status_ready_bg));
        } else {
            holder.layoutReadyBanner.setVisibility(View.GONE);
            switch (status) {
                case Order.STATUS_PREPARING:
                    holder.tvStatusBadge.setTextColor(context.getColor(R.color.status_preparing));
                    holder.tvStatusBadge.getBackground().setTint(context.getColor(R.color.status_preparing_bg));
                    break;
                case Order.STATUS_COMPLETED:
                    holder.tvStatusBadge.setTextColor(context.getColor(R.color.status_completed));
                    holder.tvStatusBadge.getBackground().setTint(context.getColor(R.color.status_completed_bg));
                    break;
                case Order.STATUS_CANCELLED:
                    holder.tvStatusBadge.setTextColor(context.getColor(R.color.status_cancelled));
                    holder.tvStatusBadge.getBackground().setTint(context.getColor(R.color.status_cancelled_bg));
                    break;
                default: // PLACED
                    holder.tvStatusBadge.setTextColor(context.getColor(R.color.status_placed));
                    holder.tvStatusBadge.getBackground().setTint(context.getColor(R.color.status_placed_bg));
                    break;
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOrderClick(order);
        });
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutReadyBanner;
        TextView tvOrderId, tvStatusBadge, tvOrderDate, tvItemsSummary, tvTotalAmount, tvPickupSlot;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutReadyBanner = itemView.findViewById(R.id.layoutReadyBanner);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvItemsSummary = itemView.findViewById(R.id.tvItemsSummary);
            tvTotalAmount = itemView.findViewById(R.id.tvTotalAmount);
            tvPickupSlot = itemView.findViewById(R.id.tvPickupSlot);
        }
    }
}
