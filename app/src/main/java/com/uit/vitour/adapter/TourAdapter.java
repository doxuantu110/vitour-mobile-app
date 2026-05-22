package com.uit.vitour.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.uit.vitour.databinding.ItemTourCardBinding;
import com.uit.vitour.model.Tour;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * TourAdapter.java — RecyclerView adapter for Tour items.
 *
 * Extends ListAdapter (not ArrayAdapter or raw RecyclerView.Adapter) because:
 * - DiffUtil handles efficient list diffing automatically
 * - submitList() is thread-safe and animates changes for free
 *
 * View Binding: ItemTourCardBinding replaces findViewById() calls entirely.
 *
 * Usage in a Fragment:
 *   TourAdapter adapter = new TourAdapter(tour -> {
 *       // Navigate to detail screen with tour.getId()
 *   });
 *   binding.rvPopular.setAdapter(adapter);
 *   adapter.submitList(tours);
 *
 * Naming convention: Adapters always end in "Adapter".
 */
public class TourAdapter extends ListAdapter<Tour, TourAdapter.TourViewHolder> {

    private static final String TAG = "TourAdapter";

    /** Callback interface — click event bubbles up to the Fragment/ViewModel. */
    public interface OnTourClickListener {
        void onTourClick(Tour tour);
    }

    private final OnTourClickListener listener;

    public TourAdapter(OnTourClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    /**
     * Override submitList to add debug logging so we can confirm data reaches the adapter.
     */
    @Override
    public void submitList(List<Tour> list) {
        int count = (list != null) ? list.size() : 0;
        Log.d(TAG, "Adapter received " + count + " tours");
        super.submitList(list);
    }

    // ── DiffUtil — compares old and new lists for efficient updates ────────
    // FIXED: use Objects.equals() instead of direct .equals() to prevent NPE
    // when getId() / getName() returns null (can happen if @DocumentId hasn't
    // been auto-mapped yet by Firestore SDK on the first snapshot delivery).
    private static final DiffUtil.ItemCallback<Tour> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Tour>() {
                @Override
                public boolean areItemsTheSame(@NonNull Tour oldItem, @NonNull Tour newItem) {
                    // Use Objects.equals — null-safe, won't NPE if getId() is null
                    return Objects.equals(oldItem.getId(), newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Tour oldItem, @NonNull Tour newItem) {
                    // Only redraw if visible fields changed; null-safe for all String fields
                    return Objects.equals(oldItem.getName(), newItem.getName())
                            && oldItem.getRating() == newItem.getRating()
                            && oldItem.getPrice() == newItem.getPrice();
                }
            };

    // ── ViewHolder creation ───────────────────────────────────────────────
    @NonNull
    @Override
    public TourViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTourCardBinding binding = ItemTourCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new TourViewHolder(binding);
    }

    // ── Binding data to ViewHolder ────────────────────────────────────────
    @Override
    public void onBindViewHolder(@NonNull TourViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    // ── ViewHolder ────────────────────────────────────────────────────────
    static class TourViewHolder extends RecyclerView.ViewHolder {

        private final ItemTourCardBinding binding;

        TourViewHolder(ItemTourCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Tour tour, OnTourClickListener listener) {
            // Text fields
            binding.tvTourName.setText(tour.getName());
            binding.tvLocation.setText(tour.getLocation());
            binding.tvRating.setText(String.valueOf(tour.getRating()));

            // Format price with Vietnamese locale (e.g. "1.200.000 ₫")
            NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            binding.tvPrice.setText(fmt.format(tour.getPrice()));

            // Load cover image with Glide — crossfade for smooth loading
            Glide.with(binding.ivCover.getContext())
                    .load(tour.getCoverImageUrl())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(binding.ivCover);

            // Click listener
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTourClick(tour);
                }
            });
        }
    }
}
