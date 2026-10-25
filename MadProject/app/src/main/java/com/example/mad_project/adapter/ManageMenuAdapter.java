package com.example.mad_project.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mad_project.R;
import com.example.mad_project.model.FoodItem;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManageMenuAdapter extends RecyclerView.Adapter<ManageMenuAdapter.ManageFoodViewHolder> {

    public interface OnManageFoodActionListener {
        void onToggleAvailability(FoodItem food, boolean isAvailable);
        void onEditFood(FoodItem food);
    }

    private final Context context;
    private List<FoodItem> foodList = new ArrayList<>();
    private final OnManageFoodActionListener listener;

    public ManageMenuAdapter(Context context, OnManageFoodActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setFoodList(List<FoodItem> list) {
        this.foodList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ManageFoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_manage_food, parent, false);
        return new ManageFoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ManageFoodViewHolder holder, int position) {
        FoodItem food = foodList.get(position);

        holder.tvName.setText(food.getName());
        holder.tvCategoryPrice.setText(String.format(Locale.getDefault(), "%s  •  ₹ %.2f", food.getCategory(), food.getPrice()));

        // Temporarily detach listener before setChecked
        holder.switchAvailability.setOnCheckedChangeListener(null);
        holder.switchAvailability.setChecked(food.isAvailable());

        holder.switchAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onToggleAvailability(food, isChecked);
            }
        });

        if (food.getImageUrl() != null && !food.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(food.getImageUrl())
                    .placeholder(R.drawable.ic_food_placeholder)
                    .error(R.drawable.ic_food_placeholder)
                    .into(holder.imgFood);
        } else {
            holder.imgFood.setImageResource(R.drawable.ic_food_placeholder);
        }

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditFood(food);
        });
    }

    @Override
    public int getItemCount() {
        return foodList.size();
    }

    static class ManageFoodViewHolder extends RecyclerView.ViewHolder {
        ImageView imgFood;
        TextView tvName, tvCategoryPrice;
        MaterialSwitch switchAvailability;
        ImageButton btnEdit;

        public ManageFoodViewHolder(@NonNull View itemView) {
            super(itemView);
            imgFood = itemView.findViewById(R.id.imgManageFood);
            tvName = itemView.findViewById(R.id.tvManageFoodName);
            tvCategoryPrice = itemView.findViewById(R.id.tvManageCategoryPrice);
            switchAvailability = itemView.findViewById(R.id.switchAvailability);
            btnEdit = itemView.findViewById(R.id.btnEditFood);
        }
    }
}
