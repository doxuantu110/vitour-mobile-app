package com.uit.vitour.ui.booking;

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
import com.uit.vitour.adapter.TourAdapter;
import com.uit.vitour.databinding.FragmentBookingBinding;
import com.uit.vitour.model.Tour;
import com.uit.vitour.viewmodel.ProfileViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * BookingFragment.java — Tab 3: Booking
 * ─────────────────────────────────────────────────────────────────
 * PURPOSE: Shows the signed-in user's upcoming and past bookings.
 *
 * TABS: Upcoming (default) | Past
 *   • Upcoming → bookings with future dates / "confirmed" / "pending" status
 *   • Past     → bookings with past dates / "completed" / "cancelled" status
 *
 * DATA FLOW:
 *   ProfileViewModel.getBookmarks(uid)       ← Firestore sub-collection
 *       │
 *       ▼ LiveData<Resource<List<Tour>>>
 *   Fragment filters list by tab selection
 *       │
 *       ▼
 *   TourAdapter.submitList() → RecyclerView
 *
 * NOTE: When you build a dedicated Booking model (with date, status, etc.),
 *       replace ProfileViewModel.getBookmarks() with a BookingRepository
 *       and a dedicated BookingViewModel. The Fragment code stays the same.
 *
 * BEGINNER TIP — when to create a new ViewModel:
 *   • Use an existing ViewModel if the data and logic are identical.
 *   • Create a new ViewModel when the data shape or business rules differ.
 */
public class BookingFragment extends Fragment {

    private FragmentBookingBinding binding;
    private ProfileViewModel viewModel;
    private TourAdapter bookingAdapter;

    // Full list fetched from Firestore — filtered into tabs locally
    private List<Tour> allBookings = new ArrayList<>();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

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

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        setupRecyclerView();
        setupTabFilter();
        loadBookings();
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        bookingAdapter = new TourAdapter(tour -> {
            // TODO: Navigate to BookingDetailFragment
            // Navigation.findNavController(requireView())
            //           .navigate(R.id.action_booking_to_detail, args);
        });
        binding.rvBookings.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvBookings.setAdapter(bookingAdapter);
    }

    /**
     * Wire the Upcoming / Past tab strip to filter the booking list.
     *
     * Tab 0 → Upcoming: first half of the list (placeholder logic).
     * Tab 1 → Past: second half of the list (placeholder logic).
     *
     * Replace with real status-based filtering once you have a Booking model
     * with a `status` or `date` field.
     */
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

    // ── Data Loading ──────────────────────────────────────────────────────────

    private void loadBookings() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        viewModel.getBookmarks(currentUser.getUid())
                 .observe(getViewLifecycleOwner(), resource -> {
                     switch (resource.status) {
                         case LOADING:
                             // Optional: show a loading spinner
                             break;

                         case SUCCESS:
                             allBookings = resource.data != null
                                     ? resource.data
                                     : new ArrayList<>();

                             if (allBookings.isEmpty()) {
                                 showEmptyState();
                             } else {
                                 // Apply filter for the currently selected tab
                                 int selectedTab = binding.tabLayoutBooking
                                         .getSelectedTabPosition();
                                 filterByTab(selectedTab);
                             }
                             break;

                         case ERROR:
                             if (getView() != null) {
                                 Snackbar.make(getView(),
                                         resource.message != null
                                                 ? resource.message
                                                 : "Failed to load bookings",
                                         Snackbar.LENGTH_LONG).show();
                             }
                             break;
                     }
                 });
    }

    // ── Filter Logic ──────────────────────────────────────────────────────────

    /**
     * Filters allBookings by tab position and updates the RecyclerView.
     *
     * PLACEHOLDER logic: splits the list in half for demonstration.
     * Replace with: tour.getStatus().equals("upcoming") filtering
     * once your Booking model has a status/date field.
     *
     * @param tabPosition 0 = Upcoming, 1 = Past
     */
    private void filterByTab(int tabPosition) {
        if (allBookings.isEmpty()) {
            showEmptyState();
            return;
        }

        List<Tour> filtered;
        if (tabPosition == 0) {
            // Upcoming: first half as placeholder
            int mid = (allBookings.size() + 1) / 2;
            filtered = allBookings.subList(0, mid);
        } else {
            // Past: second half as placeholder
            int mid = (allBookings.size() + 1) / 2;
            filtered = allBookings.subList(mid, allBookings.size());
        }

        if (filtered.isEmpty()) {
            showEmptyState();
        } else {
            showList(filtered);
        }
    }

    // ── View State Helpers ────────────────────────────────────────────────────

    private void showList(List<Tour> tours) {
        binding.rvBookings.setVisibility(View.VISIBLE);
        binding.layoutBookingEmpty.setVisibility(View.GONE);
        bookingAdapter.submitList(new ArrayList<>(tours)); // copy to avoid subList issues
    }

    private void showEmptyState() {
        binding.rvBookings.setVisibility(View.GONE);
        binding.layoutBookingEmpty.setVisibility(View.VISIBLE);
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
