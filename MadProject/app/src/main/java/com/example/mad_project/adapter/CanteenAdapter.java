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
import com.example.mad_project.model.Canteen;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class CanteenAdapter extends RecyclerView.Adapter<CanteenAdapter.CanteenViewHolder> {

    public interface OnCanteenClickListener {
        void onCanteenClick(Canteen canteen);
    }

    private final Context context;
    private List<Canteen> canteenList = new ArrayList<>();
    private final OnCanteenClickListener listener;

    public CanteenAdapter(Context context, OnCanteenClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setCanteenList(List<Canteen> list) {
        this.canteenList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CanteenViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_canteen_card, parent, false);
        return new CanteenViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CanteenViewHolder holder, int position) {
        Canteen canteen = canteenList.get(position);

        holder.tvName.setText(canteen.getName());
        holder.tvLocation.setText("📍 " + (canteen.getLocation() != null ? canteen.getLocation() : "Campus Block"));
        holder.tvDesc.setText(canteen.getDescription() != null ? canteen.getDescription() : "Fresh meals & snacks");

        if (canteen.isOpen()) {
            holder.tvStatus.setText("OPEN NOW");
            holder.tvStatus.setTextColor(context.getColor(R.color.canteen_green));
            holder.tvStatus.getBackground().setTint(context.getColor(R.color.status_ready_bg));
        } else {
            holder.tvStatus.setText("CLOSED");
            holder.tvStatus.setTextColor(context.getColor(R.color.canteen_red));
            holder.tvStatus.getBackground().setTint(context.getColor(R.color.status_cancelled_bg));
        }

        if (canteen.getImageUrl() != null && !canteen.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(canteen.getImageUrl())
                    .placeholder(R.drawable.ic_food_placeholder)
                    .error(R.drawable.ic_food_placeholder)
                    .into(holder.imgCanteen);
        } else {
            holder.imgCanteen.setImageResource(R.drawable.ic_food_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCanteenClick(canteen);
        });

        holder.btnViewMenu.setOnClickListener(v -> {
            if (listener != null) listener.onCanteenClick(canteen);
        });
    }

    @Override
    public int getItemCount() {
        return canteenList.size();
    }

    static class CanteenViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCanteen;
        TextView tvName, tvLocation, tvDesc, tvStatus;
        MaterialButton btnViewMenu;

        public CanteenViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCanteen = itemView.findViewById(R.id.imgCanteen);
            tvName = itemView.findViewById(R.id.tvCanteenName);
            tvLocation = itemView.findViewById(R.id.tvCanteenLocation);
            tvDesc = itemView.findViewById(R.id.tvCanteenDesc);
            tvStatus = itemView.findViewById(R.id.tvCanteenStatus);
            btnViewMenu = itemView.findViewById(R.id.btnViewMenu);
        }
    }
}
