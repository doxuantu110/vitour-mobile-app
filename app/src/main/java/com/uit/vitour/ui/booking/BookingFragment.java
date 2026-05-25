package com.uit.vitour.ui.booking;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.uit.vitour.adapter.BookingAdapter;
import com.uit.vitour.databinding.FragmentBookingBinding;
import com.uit.vitour.model.Booking;
import com.uit.vitour.viewmodel.BookingViewModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BookingFragment extends Fragment {

    private FragmentBookingBinding binding;
    private BookingViewModel viewModel;
    private BookingAdapter bookingAdapter;

    private List<Booking> allBookings = new ArrayList<>();
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBookingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(BookingViewModel.class);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        setupRecyclerView();
        setupTabFilter();
        
        binding.btnRetryBooking.setOnClickListener(v -> {
            if (currentUser != null) {
                binding.layoutBookingError.setVisibility(View.GONE);
                viewModel.retryUserBookings(requireContext(), currentUser.getUid());
                observeBookings(); // re-observe if necessary, but livedata usually pushes updates
            }
        });

        loadBookings();
    }

    private void setupRecyclerView() {
        bookingAdapter = new BookingAdapter(this::showCancelConfirmationDialog);
        binding.rvBookings.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvBookings.setAdapter(bookingAdapter);
    }

    private void setupTabFilter() {
        binding.tabLayoutBooking.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterByTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadBookings() {
        if (currentUser == null) return;
        observeBookings();
    }
    
    private void observeBookings() {
        viewModel.getUserBookings(requireContext(), currentUser.getUid())
                 .observe(getViewLifecycleOwner(), resource -> {
                     switch (resource.status) {
                         case LOADING:
                             // Optionally show a loading spinner
                             break;

                         case SUCCESS:
                             binding.layoutBookingError.setVisibility(View.GONE);
                             allBookings = resource.data != null ? resource.data : new ArrayList<>();

                             if (allBookings.isEmpty()) {
                                 showEmptyState();
                             } else {
                                 int selectedTab = binding.tabLayoutBooking.getSelectedTabPosition();
                                 filterByTab(selectedTab);
                             }
                             break;

                         case ERROR:
                             showErrorState(resource.message != null ? resource.message : "Failed to load bookings");
                             break;
                     }
                 });
    }

    private void filterByTab(int tabPosition) {
        if (allBookings.isEmpty()) {
            showEmptyState();
            return;
        }

        List<Booking> filtered = new ArrayList<>();
        
        java.util.Calendar calNow = java.util.Calendar.getInstance();
        calNow.setTime(new Date());
        calNow.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calNow.set(java.util.Calendar.MINUTE, 0);
        calNow.set(java.util.Calendar.SECOND, 0);
        calNow.set(java.util.Calendar.MILLISECOND, 0);
        Date today = calNow.getTime();

        for (Booking b : allBookings) {
            boolean isCancelledOrFailed = Booking.STATUS_CANCELLED.equals(b.getStatus()) || "FAILED".equals(b.getStatus());
            boolean isPastDate = b.getSelectedDate() == null || b.getSelectedDate().before(today);
            
            boolean isPast = isPastDate || isCancelledOrFailed;
                                 
            if (tabPosition == 0) { // Upcoming
                if (!isPast) filtered.add(b);
            } else { // Past
                if (isPast) filtered.add(b);
            }
        }

        if (filtered.isEmpty()) {
            showEmptyState();
        } else {
            showList(filtered);
        }
    }

    private void showCancelConfirmationDialog(Booking booking) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cancel Booking")
                .setMessage("Are you sure you want to cancel this booking?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    viewModel.cancelBooking(requireContext(), booking.getBookingId())
                            .observe(getViewLifecycleOwner(), resource -> {
                                switch (resource.status) {
                                    case LOADING:
                                        // Show loading
                                        break;
                                    case SUCCESS:
                                        Snackbar.make(requireView(), "Booking cancelled successfully", Snackbar.LENGTH_SHORT).show();
                                        // The snapshot listener in getUserBookings will automatically refresh the list.
                                        break;
                                    case ERROR:
                                        Snackbar.make(requireView(), "Error: " + resource.message, Snackbar.LENGTH_LONG).show();
                                        break;
                                }
                            });
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showList(List<Booking> bookings) {
        binding.rvBookings.setVisibility(View.VISIBLE);
        binding.layoutBookingEmpty.setVisibility(View.GONE);
        binding.layoutBookingError.setVisibility(View.GONE);
        bookingAdapter.submitList(new ArrayList<>(bookings)); 
    }

    private void showEmptyState() {
        binding.rvBookings.setVisibility(View.GONE);
        binding.layoutBookingEmpty.setVisibility(View.VISIBLE);
        binding.layoutBookingError.setVisibility(View.GONE);
    }
    
    private void showErrorState(String errorMsg) {
        binding.rvBookings.setVisibility(View.GONE);
        binding.layoutBookingEmpty.setVisibility(View.GONE);
        binding.layoutBookingError.setVisibility(View.VISIBLE);
        binding.tvBookingError.setText(errorMsg);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
