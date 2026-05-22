package com.uit.vitour.ui.home;

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

import com.google.android.material.snackbar.Snackbar;
import com.uit.vitour.R;
import com.uit.vitour.adapter.FeaturedTourAdapter;
import com.uit.vitour.adapter.TourAdapter;
import com.uit.vitour.databinding.FragmentHomeBinding;
import com.uit.vitour.viewmodel.HomeViewModel;

/**
 * HomeFragment.java — Tab 1.
 * Now fully powered by real-time Firestore listeners via HomeViewModel.
 */
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private FragmentHomeBinding binding;
    private HomeViewModel homeViewModel;

    private FeaturedTourAdapter featuredAdapter;
    private TourAdapter popularAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize ViewModel
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        // 2. Setup UI Components
        setupRecyclerViews();
        setupSearchBar();

        // 3. Observe LiveData
        observeViewModel();
    }

    private void setupRecyclerViews() {
        // Featured (Horizontal)
        featuredAdapter = new FeaturedTourAdapter(tour -> {
            Bundle args = new Bundle();
            args.putString("tourId", tour.getId());
            NavController nav = Navigation.findNavController(requireView());
            nav.navigate(R.id.action_home_to_tour_detail, args);
        });
        binding.rvFeatured.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvFeatured.setAdapter(featuredAdapter);

        // Popular/Recommended (Vertical)
        popularAdapter = new TourAdapter(tour -> {
            Bundle args = new Bundle();
            args.putString("tourId", tour.getId());
            NavController nav = Navigation.findNavController(requireView());
            nav.navigate(R.id.action_home_to_tour_detail, args);
        });
        binding.rvPopular.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPopular.setAdapter(popularAdapter);
        
        // Swipe to refresh — mostly just a UI affordance now since Firestore is real-time.
        // We can just stop the refreshing animation immediately.
        binding.swipeRefresh.setOnRefreshListener(() -> {
            binding.swipeRefresh.setRefreshing(false);
        });
    }

    private void setupSearchBar() {
        binding.etSearch.setOnClickListener(v -> {
            NavController nav = Navigation.findNavController(requireView());
            nav.navigate(R.id.searchFragment);
        });
    }

    private void observeViewModel() {
        Log.d(TAG, "observeViewModel() — setting up observers");

        // Observe Featured Tours
        homeViewModel.getFeaturedTours().observe(getViewLifecycleOwner(), resource -> {
            Log.d(TAG, "FeaturedTours observer fired. status=" + resource.status
                    + ", data=" + (resource.data != null ? resource.data.size() + " items" : "null"));
            switch (resource.status) {
                case LOADING:
                    binding.progressFeatured.setVisibility(View.VISIBLE);
                    binding.tvFeaturedEmpty.setVisibility(View.GONE);
                    binding.rvFeatured.setVisibility(View.INVISIBLE);
                    break;
                case SUCCESS:
                    binding.progressFeatured.setVisibility(View.GONE);
                    if (resource.data != null && !resource.data.isEmpty()) {
                        Log.d(TAG, "Submitting " + resource.data.size() + " featured tours to adapter");
                        featuredAdapter.submitList(resource.data);
                        binding.rvFeatured.setVisibility(View.VISIBLE);
                        binding.tvFeaturedEmpty.setVisibility(View.GONE);
                    } else {
                        Log.w(TAG, "Featured tours list is null or empty");
                        binding.rvFeatured.setVisibility(View.INVISIBLE);
                        binding.tvFeaturedEmpty.setVisibility(View.VISIBLE);
                    }
                    break;
                case ERROR:
                    binding.progressFeatured.setVisibility(View.GONE);
                    Log.e(TAG, "Featured tours error: " + resource.message);
                    showError(resource.message);
                    break;
            }
        });

        // Observe Recommended Tours
        homeViewModel.getRecommendedTours().observe(getViewLifecycleOwner(), resource -> {
            Log.d(TAG, "RecommendedTours observer fired. status=" + resource.status
                    + ", data=" + (resource.data != null ? resource.data.size() + " items" : "null"));
            switch (resource.status) {
                case LOADING:
                    binding.progressPopular.setVisibility(View.VISIBLE);
                    binding.tvPopularEmpty.setVisibility(View.GONE);
                    binding.rvPopular.setVisibility(View.INVISIBLE);
                    break;
                case SUCCESS:
                    binding.progressPopular.setVisibility(View.GONE);
                    if (resource.data != null && !resource.data.isEmpty()) {
                        Log.d(TAG, "Submitting " + resource.data.size() + " recommended tours to adapter");
                        popularAdapter.submitList(resource.data);
                        binding.rvPopular.setVisibility(View.VISIBLE);
                        binding.tvPopularEmpty.setVisibility(View.GONE);
                    } else {
                        Log.w(TAG, "Recommended tours list is null or empty");
                        binding.rvPopular.setVisibility(View.INVISIBLE);
                        binding.tvPopularEmpty.setVisibility(View.VISIBLE);
                    }
                    break;
                case ERROR:
                    binding.progressPopular.setVisibility(View.GONE);
                    Log.e(TAG, "Recommended tours error: " + resource.message);
                    showError(resource.message);
                    break;
            }
        });
    }

    private void showError(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message != null ? message : "Error loading data",
                    Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Prevent memory leaks
        binding = null;
    }
}
