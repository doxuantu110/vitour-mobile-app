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
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.snackbar.Snackbar;
import com.uit.vitour.adapter.TourImageAdapter;
import com.uit.vitour.databinding.FragmentTourDetailBinding;
import com.uit.vitour.model.Tour;
import com.uit.vitour.viewmodel.TourDetailViewModel;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class TourDetailFragment extends Fragment {

    private static final String TAG = "TourDetailFragment";

    private FragmentTourDetailBinding binding;
    private TourDetailViewModel viewModel;
    private TourImageAdapter imageAdapter;

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

        String tourId = null;
        if (getArguments() != null) {
            tourId = getArguments().getString("tourId");
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
        setupViewModel(tourId);

        binding.btnBookNow.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Booking feature coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        binding.toolbar.setNavigationOnClickListener(v -> {
            NavController nav = Navigation.findNavController(requireView());
            nav.navigateUp();
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
        binding.tvDetailLocation.setText(tour.getLocation());
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
