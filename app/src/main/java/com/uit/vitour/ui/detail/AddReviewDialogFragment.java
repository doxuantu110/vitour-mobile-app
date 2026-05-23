package com.uit.vitour.ui.detail;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;
import com.uit.vitour.R;
import com.uit.vitour.databinding.DialogAddReviewBinding;
import com.uit.vitour.viewmodel.ReviewViewModel;

/**
 * AddReviewDialogFragment.java — BottomSheetDialogFragment for writing a review.
 *
 * Features:
 *   - Interactive star RatingBar (1–5)
 *   - Multi-line comment text input
 *   - Optional image picker (gallery) with thumbnail preview
 *   - Submits via ReviewViewModel → ReviewRepository
 *   - Shows LinearProgressIndicator while uploading/writing
 *   - Dismisses automatically on success
 *
 * Usage:
 *   AddReviewDialogFragment.newInstance(tourId).show(childFragmentManager, "add_review");
 */
public class AddReviewDialogFragment extends BottomSheetDialogFragment {

    private static final String TAG = "AddReviewDialog";
    private static final String ARG_TOUR_ID = "tour_id";

    private DialogAddReviewBinding binding;
    private ReviewViewModel reviewViewModel;

    private String tourId;
    private Uri selectedImageUri = null;    // null = no image selected
    private boolean isSubmitting = false;

    // ── Factory method ─────────────────────────────────────────────────────

    public static AddReviewDialogFragment newInstance(String tourId) {
        AddReviewDialogFragment fragment = new AddReviewDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TOUR_ID, tourId);
        fragment.setArguments(args);
        return fragment;
    }

    // ── Image picker launcher ──────────────────────────────────────────────

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Log.d(TAG, "Image picked: " + selectedImageUri);
                    showImagePreview(selectedImageUri);
                } else {
                    Log.d(TAG, "Image picker canceled");
                }
            });

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tourId = getArguments().getString(ARG_TOUR_ID);
        }
        Log.d(TAG, "onCreate — tourId=" + tourId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DialogAddReviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Shared with parent TourDetailFragment (scoped to parent fragment, NOT this dialog)
        reviewViewModel = new ViewModelProvider(requireParentFragment())
                .get(ReviewViewModel.class);

        setupRatingBar();
        setupImagePicker();
        setupButtons();
    }

    // ── Setup methods ──────────────────────────────────────────────────────

    private void setupRatingBar() {
        binding.ratingBar.setRating(0f);
        binding.tvRatingLabel.setText("Tap to rate");

        binding.ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            Log.d(TAG, "Rating changed: " + rating);
            String[] labels = {"", "Poor", "Fair", "Good", "Very Good", "Excellent"};
            int idx = Math.round(rating);
            if (idx > 0 && idx <= 5) {
                binding.tvRatingLabel.setText(labels[idx] + " (" + idx + "/5)");
            } else {
                binding.tvRatingLabel.setText("Tap to rate");
            }
        });
    }

    private void setupImagePicker() {
        binding.btnPickImage.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });

        binding.btnRemoveImage.setOnClickListener(v -> {
            selectedImageUri = null;
            binding.ivSelectedImage.setVisibility(View.GONE);
            binding.btnRemoveImage.setVisibility(View.GONE);
            binding.btnPickImage.setText("Add Photo");
            Log.d(TAG, "Image selection cleared");
        });
    }

    private void setupButtons() {
        binding.btnCancelReview.setOnClickListener(v -> dismiss());

        binding.btnSubmitReview.setOnClickListener(v -> submitReview());
    }

    // ── Business logic ─────────────────────────────────────────────────────

    private void submitReview() {
        if (tourId == null || tourId.isEmpty()) {
            Toast.makeText(getContext(), "Error: no tour ID", Toast.LENGTH_SHORT).show();
            return;
        }

        int rating = Math.round(binding.ratingBar.getRating());
        if (rating < 1) {
            Snackbar.make(requireView(), "Please select a star rating", Snackbar.LENGTH_SHORT).show();
            return;
        }

        String comment = "";
        if (binding.etComment.getText() != null) {
            comment = binding.etComment.getText().toString().trim();
        }

        if (comment.isEmpty()) {
            binding.tilComment.setError("Please write a comment");
            return;
        }
        binding.tilComment.setError(null);

        Log.d(TAG, "submitReview() — rating=" + rating
                + ", comment.length=" + comment.length()
                + ", hasImage=" + (selectedImageUri != null));

        setSubmitting(true);

        reviewViewModel.addReview(requireContext(), tourId, rating, comment, selectedImageUri)
                .observe(getViewLifecycleOwner(), resource -> {
                    switch (resource.status) {
                        case LOADING:
                            setSubmitting(true);
                            break;
                        case SUCCESS:
                            Log.d(TAG, "Review submitted successfully");
                            setSubmitting(false);
                            dismiss();
                            break;
                        case ERROR:
                            Log.e(TAG, "Review submission failed: " + resource.message);
                            setSubmitting(false);
                            if (getView() != null) {
                                Snackbar.make(getView(),
                                        resource.message != null ? resource.message : "Submission failed",
                                        Snackbar.LENGTH_LONG).show();
                            }
                            break;
                    }
                });
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void showImagePreview(Uri uri) {
        binding.ivSelectedImage.setVisibility(View.VISIBLE);
        binding.btnRemoveImage.setVisibility(View.VISIBLE);
        binding.btnPickImage.setText("Change Photo");
        Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_image)
                .error(R.drawable.ic_image)
                .diskCacheStrategy(DiskCacheStrategy.NONE) // Local URI, no need to cache
                .into(binding.ivSelectedImage);
    }

    private void setSubmitting(boolean submitting) {
        this.isSubmitting = submitting;
        binding.progressSubmit.setVisibility(submitting ? View.VISIBLE : View.GONE);
        binding.btnSubmitReview.setEnabled(!submitting);
        binding.btnCancelReview.setEnabled(!submitting);
        binding.ratingBar.setIsIndicator(submitting);
        binding.etComment.setEnabled(!submitting);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isSubmitting && tourId != null && reviewViewModel != null) {
            Log.w(TAG, "onDestroyView() called while submitting — canceling upload");
            reviewViewModel.cancelCurrentUpload(tourId);
        }
        binding = null;
    }
}
