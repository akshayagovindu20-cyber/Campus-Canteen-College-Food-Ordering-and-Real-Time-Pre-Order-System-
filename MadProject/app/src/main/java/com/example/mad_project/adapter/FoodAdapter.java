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
import com.example.mad_project.model.FoodItem;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.FoodViewHolder> {

    public interface OnFoodClickListener {
        void onFoodClick(FoodItem food);
        void onAddToCartClick(FoodItem food);
    }

    private final Context context;
    private List<FoodItem> foodList = new ArrayList<>();
    private final OnFoodClickListener listener;

    public FoodAdapter(Context context, OnFoodClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setFoodList(List<FoodItem> list) {
        this.foodList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_food_card, parent, false);
        return new FoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodViewHolder holder, int position) {
        FoodItem food = foodList.get(position);

        holder.tvFoodName.setText(food.getName());
        holder.tvCategory.setText(food.getCategory());
        holder.tvDescription.setText(food.getDescription());
        holder.tvPrice.setText(String.format("₹ %.2f", food.getPrice()));

        if (food.isAvailable()) {
            holder.tvAvailability.setText("Available");
            holder.tvAvailability.setTextColor(context.getColor(R.color.canteen_green));
            holder.tvAvailability.getBackground().setTint(context.getColor(R.color.status_ready_bg));
            holder.btnAddToCart.setEnabled(true);
            holder.btnAddToCart.setText("Add");
        } else {
            holder.tvAvailability.setText("Sold Out");
            holder.tvAvailability.setTextColor(context.getColor(R.color.canteen_red));
            holder.tvAvailability.getBackground().setTint(context.getColor(R.color.status_cancelled_bg));
            holder.btnAddToCart.setEnabled(false);
            holder.btnAddToCart.setText("Unavailable");
        }

        if (food.getImageUrl() != null && !food.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(food.getImageUrl())
                    .placeholder(R.drawable.ic_food_placeholder)
                    .error(R.drawable.ic_food_placeholder)
                    .into(holder.imgFood);
        } else {
            holder.imgFood.setImageResource(R.drawable.ic_food_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onFoodClick(food);
        });

        holder.btnAddToCart.setOnClickListener(v -> {
            if (listener != null) listener.onAddToCartClick(food);
        });
    }

    @Override
    public int getItemCount() {
        return foodList.size();
    }

    static class FoodViewHolder extends RecyclerView.ViewHolder {
        ImageView imgFood;
        TextView tvCategory, tvFoodName, tvDescription, tvPrice, tvAvailability;
        MaterialButton btnAddToCart;

        public FoodViewHolder(@NonNull View itemView) {
            super(itemView);
            imgFood = itemView.findViewById(R.id.imgFood);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvFoodName = itemView.findViewById(R.id.tvFoodName);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvAvailability = itemView.findViewById(R.id.tvAvailabilityBadge);
            btnAddToCart = itemView.findViewById(R.id.btnAddToCart);
        }
    }
}
