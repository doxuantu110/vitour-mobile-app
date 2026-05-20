package com.uit.vitour.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.uit.vitour.databinding.ItemTourFeaturedBinding;
import com.uit.vitour.model.Tour;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * FeaturedTourAdapter.java — Adapter for the horizontal featured tours carousel.
 * Uses item_tour_featured.xml layout.
 */
public class FeaturedTourAdapter extends ListAdapter<Tour, FeaturedTourAdapter.ViewHolder> {

    public interface OnTourClickListener {
        void onTourClick(Tour tour);
    }

    private final OnTourClickListener listener;

    public FeaturedTourAdapter(OnTourClickListener listener) {
        super(new DiffUtil.ItemCallback<Tour>() {
            @Override
            public boolean areItemsTheSame(@NonNull Tour oldItem, @NonNull Tour newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Tour oldItem, @NonNull Tour newItem) {
                return oldItem.getName().equals(newItem.getName())
                        && oldItem.getPrice() == newItem.getPrice()
                        && oldItem.getRating() == newItem.getRating();
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTourFeaturedBinding binding = ItemTourFeaturedBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemTourFeaturedBinding binding;

        ViewHolder(ItemTourFeaturedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Tour tour, OnTourClickListener listener) {
            binding.tvTourName.setText(tour.getName());
            binding.tvLocation.setText(tour.getLocation());
            binding.tvRating.setText(String.valueOf(tour.getRating()));

            NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            binding.tvPrice.setText(fmt.format(tour.getPrice()));

            Glide.with(binding.ivCover.getContext())
                    .load(tour.getCoverImageUrl())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(binding.ivCover);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onTourClick(tour);
            });
        }
    }
}
