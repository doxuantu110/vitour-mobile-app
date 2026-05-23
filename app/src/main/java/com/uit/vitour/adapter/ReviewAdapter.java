package com.uit.vitour.adapter;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.uit.vitour.R;
import com.uit.vitour.databinding.ItemReviewBinding;
import com.uit.vitour.model.Review;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * ReviewAdapter.java — ListAdapter for displaying a list of Review objects.
 *
 * Features:
 *   - DiffUtil for efficient updates (only changed items re-render)
 *   - Circular user avatar loaded via Glide (fallback to initials-style placeholder)
 *   - Dynamic star coloring (filled = gold, empty = light gray)
 *   - Optional review photo (GONE when null, VISIBLE when present)
 *
 * Usage:
 *   ReviewAdapter adapter = new ReviewAdapter();
 *   binding.rvReviews.setAdapter(adapter);
 *   adapter.submitList(reviews);   // also safe with null — treated as empty list
 */
public class ReviewAdapter extends ListAdapter<Review, ReviewAdapter.ReviewViewHolder> {

    private static final String TAG = "ReviewAdapter";

    private static final int STAR_FILLED_COLOR  = 0xFFFFC107;  // amber
    private static final int STAR_EMPTY_COLOR   = 0xFFDDDDDD;  // light gray

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    public ReviewAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Review> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Review>() {
                @Override
                public boolean areItemsTheSame(@NonNull Review a, @NonNull Review b) {
                    return Objects.equals(a.getId(), b.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Review a, @NonNull Review b) {
                    return Objects.equals(a.getComment(), b.getComment())
                            && a.getRating() == b.getRating()
                            && Objects.equals(a.getImageUrl(), b.getImageUrl());
                }
            };

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReviewBinding binding = ItemReviewBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ReviewViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ReviewViewHolder extends RecyclerView.ViewHolder {

        private final ItemReviewBinding b;

        ReviewViewHolder(ItemReviewBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(Review review) {
            // ── Reviewer name ──────────────────────────────────────────
            String name = review.getUserName();
            b.tvReviewerName.setText((name != null && !name.isEmpty()) ? name : "Anonymous");

            // ── Date ───────────────────────────────────────────────────
            if (review.getCreatedAt() != null) {
                b.tvReviewDate.setText(DATE_FORMAT.format(review.getCreatedAt()));
                b.tvReviewDate.setVisibility(View.VISIBLE);
            } else {
                b.tvReviewDate.setVisibility(View.GONE);
            }

            // ── Avatar ─────────────────────────────────────────────────
            if (review.getUserAvatar() != null && !review.getUserAvatar().isEmpty()) {
                Glide.with(b.ivUserAvatar.getContext())
                        .load(review.getUserAvatar())
                        .circleCrop()
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .placeholder(R.drawable.ic_nav_profile)
                        .error(R.drawable.ic_nav_profile)
                        .into(b.ivUserAvatar);
            } else {
                // No avatar URL — show generic placeholder icon
                b.ivUserAvatar.setImageResource(R.drawable.ic_nav_profile);
            }

            // ── Stars ──────────────────────────────────────────────────
            ImageView[] stars = {
                b.ivStar1, b.ivStar2, b.ivStar3, b.ivStar4, b.ivStar5
            };
            int rating = Math.max(0, Math.min(5, review.getRating())); // clamp 0-5
            for (int i = 0; i < stars.length; i++) {
                stars[i].setColorFilter(
                        i < rating ? STAR_FILLED_COLOR : STAR_EMPTY_COLOR,
                        PorterDuff.Mode.SRC_IN
                );
            }

            // ── Comment ────────────────────────────────────────────────
            String comment = review.getComment();
            if (comment != null && !comment.isEmpty()) {
                b.tvReviewComment.setText(comment);
                b.tvReviewComment.setVisibility(View.VISIBLE);
            } else {
                b.tvReviewComment.setVisibility(View.GONE);
            }

            // ── Optional review photo ──────────────────────────────────
            if (review.getImageUrl() != null && !review.getImageUrl().isEmpty()) {
                b.ivReviewImage.setVisibility(View.VISIBLE);
                Glide.with(b.ivReviewImage.getContext())
                        .load(review.getImageUrl())
                        .placeholder(R.drawable.ic_image)
                        .error(R.drawable.ic_image)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(b.ivReviewImage);
            } else {
                b.ivReviewImage.setVisibility(View.GONE);
                b.ivReviewImage.setImageDrawable(null); // release previous Glide target
            }
        }
    }
}
