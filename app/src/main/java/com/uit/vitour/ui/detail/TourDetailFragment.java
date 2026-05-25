package com.uit.vitour.ui.detail;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.snackbar.Snackbar;
import com.uit.vitour.adapter.ReviewAdapter;
import com.uit.vitour.adapter.TourImageAdapter;
import com.uit.vitour.databinding.FragmentTourDetailBinding;
import com.uit.vitour.model.Tour;
import com.uit.vitour.viewmodel.ReviewViewModel;
import com.uit.vitour.viewmodel.TourDetailViewModel;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class TourDetailFragment extends Fragment {

    private static final String TAG = "TourDetailFragment";

    private FragmentTourDetailBinding binding;
    private TourDetailViewModel viewModel;
    private ReviewViewModel reviewViewModel;
    private TourImageAdapter imageAdapter;
    private ReviewAdapter reviewAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTourDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String tourId;
        if (getArguments() != null) {
            tourId = getArguments().getString("tourId");
        } else {
            tourId = null;
        }

        Log.d(TAG, "onViewCreated: Received tourId = " + tourId);

        if (tourId == null || tourId.isEmpty()) {
            Toast.makeText(getContext(), "Error: Invalid Tour ID", Toast.LENGTH_SHORT).show();
            NavController nav = Navigation.findNavController(view);
            nav.navigateUp();
            return;
        }

        setupToolbar();
        setupGallery();
        setupReviews();
        setupViewModel(tourId);
        setupReviewViewModel(tourId);
        setupWindowInsets();

        binding.btnBookNow.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("tourId", tourId);
            Navigation.findNavController(v).navigate(com.uit.vitour.R.id.action_tourDetailFragment_to_bookingFormFragment, bundle);
        });

        binding.btnWriteReview.setOnClickListener(v ->
                AddReviewDialogFragment.newInstance(tourId)
                        .show(getChildFragmentManager(), "add_review"));
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        binding.toolbar.setNavigationOnClickListener(v -> {
            NavController nav = Navigation.findNavController(requireView());
            nav.navigateUp();
        });
    }

    private void setupWindowInsets() {
        // Ensure the bottom booking bar avoids the system navigation bar (edge-to-edge)
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomBookingBar, (v, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottomInset);
            return insets;
        });

        // Dynamically apply bottom bar height + insets to the scroll view
        binding.bottomBookingBar.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int bottomBarHeight = bottom - top;
            if (bottomBarHeight > 0) {
                // Set the exact height of the bottom bar as the scroll view's padding
                NestedScrollView scrollView = requireView().findViewById(com.uit.vitour.R.id.scroll_view);
                if (scrollView != null) {
                    scrollView.setPadding(
                            scrollView.getPaddingLeft(),
                            scrollView.getPaddingTop(),
                            scrollView.getPaddingRight(),
                            bottomBarHeight + 16 // Adding 16px extra breathing room
                    );
                }
            }
        });
    }

    private void setupGallery() {
        imageAdapter = new TourImageAdapter();
        binding.vpGallery.setAdapter(imageAdapter);

        // Update dot indicator text when page changes
        binding.vpGallery.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                int total = imageAdapter.getItemCount();
                binding.tvGalleryIndicator.setText(String.format(Locale.getDefault(), "%d / %d", position + 1, total));
            }
        });
    }

    private void setupReviews() {
        reviewAdapter = new ReviewAdapter();
        binding.rvReviews.setAdapter(reviewAdapter);
        // LayoutManager is set via XML (app:layoutManager), but set explicitly as a safeguard
        binding.rvReviews.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    private void setupReviewViewModel(String tourId) {
        reviewViewModel = new ViewModelProvider(this).get(ReviewViewModel.class);
        reviewViewModel.setTourId(tourId);

        reviewViewModel.getReviews().observe(getViewLifecycleOwner(), resource -> {
            Log.d(TAG, "Reviews state: " + resource.status);
            switch (resource.status) {
                case LOADING:
                    binding.progressReviews.setVisibility(View.VISIBLE);
                    binding.tvReviewsEmpty.setVisibility(View.GONE);
                    binding.rvReviews.setVisibility(View.GONE);
                    break;
                case SUCCESS:
                    binding.progressReviews.setVisibility(View.GONE);
                    if (resource.data != null && !resource.data.isEmpty()) {
                        Log.d(TAG, "Submitting " + resource.data.size() + " reviews to adapter");
                        reviewAdapter.submitList(resource.data);
                        binding.rvReviews.setVisibility(View.VISIBLE);
                        binding.tvReviewsEmpty.setVisibility(View.GONE);
                        String summary = resource.data.size() == 1
                                ? "1 traveller reviewed this tour"
                                : resource.data.size() + " travellers reviewed this tour";
                        binding.tvReviewCountSummary.setText(summary);
                    } else {
                        reviewAdapter.submitList(null);
                        binding.rvReviews.setVisibility(View.GONE);
                        binding.tvReviewsEmpty.setVisibility(View.VISIBLE);
                        binding.tvReviewCountSummary.setText("No reviews yet");
                    }
                    break;
                case ERROR:
                    binding.progressReviews.setVisibility(View.GONE);
                    Log.e(TAG, "Reviews error: " + resource.message);
                    binding.tvReviewsEmpty.setVisibility(View.VISIBLE);
                    binding.tvReviewsEmpty.setText("Could not load reviews");
                    break;
            }
        });
    }

    private void setupViewModel(String tourId) {
        viewModel = new ViewModelProvider(this).get(TourDetailViewModel.class);
        viewModel.setTourId(tourId);

        viewModel.getTour().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING:
                    Log.d(TAG, "ViewModel state: LOADING");
                    // Optional: show a skeleton loader or progress bar
                    break;
                case SUCCESS:
                    Log.d(TAG, "ViewModel state: SUCCESS");
                    if (resource.data != null) {
                        Log.d(TAG, "Firestore document loaded: " + resource.data.getName());
                        bindTourData(resource.data);
                    } else {
                        Log.e(TAG, "Data is null inside SUCCESS state");
                        showError("Data error");
                    }
                    break;
                case ERROR:
                    Log.e(TAG, "ViewModel state: ERROR -> " + resource.message);
                    showError(resource.message);
                    break;
            }
        });
    }

    private void bindTourData(Tour tour) {
        // Toolbar Title
        binding.collapsingToolbar.setTitle(tour.getName());
        binding.tvDetailTitle.setText(tour.getName());
        if (tour.getLocationName() != null && !tour.getLocationName().isEmpty()) {
            binding.tvDetailLocation.setText(tour.getLocationName());
            binding.tvDetailLocation.setVisibility(View.VISIBLE);
        } else {
            binding.tvDetailLocation.setVisibility(View.GONE);
        }
        binding.tvDetailRating.setText(String.format(Locale.getDefault(), "%.1f", tour.getRating()));
        binding.tvDetailReviews.setText(String.format(Locale.getDefault(), "(%d reviews)", tour.getReviewCount()));
        binding.tvDetailDuration.setText(String.format(Locale.getDefault(), "%d Days", tour.getDurationDays()));
        binding.tvDetailDescription.setText(tour.getDescription());

        // Price formatting
        NumberFormat format = NumberFormat.getInstance(new Locale("vi", "VN"));
        binding.tvDetailPrice.setText(format.format(tour.getPrice()) + " VND");

        // Handle Image Gallery Fallback safely
        List<String> images = tour.getImageUrls();
        if (images == null || images.isEmpty()) {
            Log.d(TAG, "imageUrls is empty/null, falling back to coverImageUrl");
            if (tour.getCoverImageUrl() != null) {
                images = Collections.singletonList(tour.getCoverImageUrl());
            } else {
                images = Collections.emptyList();
            }
        }
        
        Log.d(TAG, "Submitting gallery size: " + images.size());
        imageAdapter.submitList(images);
        
        if (!images.isEmpty()) {
            binding.tvGalleryIndicator.setText("1 / " + images.size());
        } else {
            binding.tvGalleryIndicator.setText("0 / 0");
        }

        // Map Fallback (Postponed full integration, but check nullability)
        Double lat = tour.getLatitude();
        Double lng = tour.getLongitude();

        if (lat == null || lng == null) {
            Log.d(TAG, "Latitude or Longitude is null -> hiding map container");
            binding.mapContainer.setVisibility(View.GONE);
        } else {
            Log.d(TAG, "Coordinates found (" + lat + ", " + lng + ") -> Map integration postponed, keeping placeholder visible");
            binding.mapContainer.setVisibility(View.VISIBLE);
            // Future integration: instantiate GoogleMap, add marker at lat, lng.
        }
    }

    private void showError(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message != null ? message : "Error loading tour", Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
