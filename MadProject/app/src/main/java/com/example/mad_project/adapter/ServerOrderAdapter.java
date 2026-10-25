package com.example.mad_project.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mad_project.R;
import com.example.mad_project.model.Order;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ServerOrderAdapter extends RecyclerView.Adapter<ServerOrderAdapter.ServerOrderViewHolder> {

    public interface OnServerOrderActionListener {
        void onOrderClick(Order order);
        void onUpdateStatus(Order order, String newStatus);
    }

    private final Context context;
    private List<Order> orders = new ArrayList<>();
    private final OnServerOrderActionListener listener;

    public ServerOrderAdapter(Context context, OnServerOrderActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setOrders(List<Order> list) {
        this.orders = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ServerOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_server_order, parent, false);
        return new ServerOrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServerOrderViewHolder holder, int position) {
        Order order = orders.get(position);

        String shortId = order.getOrderId() != null ? order.getOrderId().substring(0, Math.min(8, order.getOrderId().length())).toUpperCase() : "";
        holder.tvOrderId.setText("Order #" + shortId);
        holder.tvCustomerName.setText("Customer: " + (order.getCustomerName() != null ? order.getCustomerName() : "Student"));

        String phone = order.getCustomerPhone() != null && !order.getCustomerPhone().isEmpty() ? order.getCustomerPhone() : "N/A";
        holder.tvCustomerPhone.setText("📞 " + phone);

        holder.tvItemsList.setText(order.getFormattedItemsSummary());
        holder.tvTotal.setText(String.format(Locale.getDefault(), "Total: ₹ %.2f", order.getTotalAmount()));

        String timeStr = order.getCreatedAt() > 0 ? DateFormat.format("hh:mm a", new Date(order.getCreatedAt())).toString() : "Recent";
        String slotStr = order.getPickupTime() != null ? order.getPickupTime() : "ASAP";
        holder.tvTime.setText("Received at " + timeStr + " | Slot: " + slotStr);

        String status = order.getStatus() != null ? order.getStatus().toUpperCase() : Order.STATUS_PLACED;
        holder.tvStatusBadge.setText(status);

        if (!"N/A".equals(phone)) {
            holder.btnCall.setVisibility(View.VISIBLE);
            holder.btnCall.setOnClickListener(v -> {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                context.startActivity(dialIntent);
            });
        } else {
            holder.btnCall.setVisibility(View.GONE);
        }

        switch (status) {
            case Order.STATUS_PLACED:
                holder.tvStatusBadge.setTextColor(context.getColor(R.color.status_placed));
                holder.tvStatusBadge.getBackground().setTint(context.getColor(R.color.status_placed_bg));
                holder.btnPrimary.setVisibility(View.VISIBLE);
                holder.btnPrimary.setText("Accept & Prepare");
                holder.btnPrimary.setBackgroundTintList(context.getColorStateList(R.color.canteen_orange));
                holder.btnSecondary.setVisibility(View.VISIBLE);
                break;

            case Order.STATUS_PREPARING:
                holder.tvStatusBadge.setTextColor(context.getColor(R.color.status_preparing));
                holder.tvStatusBadge.getBackground().setTint(context.getColor(R.color.status_preparing_bg));
                holder.btnPrimary.setVisibility(View.VISIBLE);
                holder.btnPrimary.setText("Mark READY");
                holder.btnPrimary.setBackgroundTintList(context.getColorStateList(R.color.canteen_green));
                holder.btnSecondary.setVisibility(View.VISIBLE);
                break;

            case Order.STATUS_READY:
                holder.tvStatusBadge.setTextColor(context.getColor(R.color.canteen_green));
                holder.tvStatusBadge.getBackground().setTint(context.getColor(R.color.status_ready_bg));
                holder.btnPrimary.setVisibility(View.VISIBLE);
                holder.btnPrimary.setText("Mark COMPLETED");
                holder.btnPrimary.setBackgroundTintList(context.getColorStateList(R.color.status_completed));
                holder.btnSecondary.setVisibility(View.GONE);
                break;

            default: // COMPLETED / CANCELLED
                if (Order.STATUS_COMPLETED.equalsIgnoreCase(status)) {
                    holder.tvStatusBadge.setTextColor(context.getColor(R.color.status_completed));
                    holder.tvStatusBadge.getBackground().setTint(context.getColor(R.color.status_completed_bg));
                } else {
                    holder.tvStatusBadge.setTextColor(context.getColor(R.color.status_cancelled));
                    holder.tvStatusBadge.getBackground().setTint(context.getColor(R.color.status_cancelled_bg));
                }
                holder.btnPrimary.setVisibility(View.GONE);
                holder.btnSecondary.setVisibility(View.GONE);
                break;
        }

        holder.btnPrimary.setOnClickListener(v -> {
            if (listener == null) return;
            if (Order.STATUS_PLACED.equalsIgnoreCase(status)) {
                listener.onUpdateStatus(order, Order.STATUS_PREPARING);
            } else if (Order.STATUS_PREPARING.equalsIgnoreCase(status)) {
                listener.onUpdateStatus(order, Order.STATUS_READY);
            } else if (Order.STATUS_READY.equalsIgnoreCase(status)) {
                listener.onUpdateStatus(order, Order.STATUS_COMPLETED);
            }
        });

        holder.btnSecondary.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUpdateStatus(order, Order.STATUS_CANCELLED);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOrderClick(order);
        });
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class ServerOrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvStatusBadge, tvCustomerName, tvCustomerPhone, tvTime, tvItemsList, tvTotal;
        MaterialButton btnCall, btnSecondary, btnPrimary;

        public ServerOrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvServerOrderId);
            tvStatusBadge = itemView.findViewById(R.id.tvServerStatusBadge);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvCustomerPhone = itemView.findViewById(R.id.tvCustomerPhone);
            tvTime = itemView.findViewById(R.id.tvServerOrderTime);
            tvItemsList = itemView.findViewById(R.id.tvServerItemsList);
            tvTotal = itemView.findViewById(R.id.tvServerTotal);
            btnCall = itemView.findViewById(R.id.btnCallCustomer);
            btnSecondary = itemView.findViewById(R.id.btnServerActionSecondary);
            btnPrimary = itemView.findViewById(R.id.btnServerActionPrimary);
        }
    }
}
