package com.uit.vitour.ui.booking;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.uit.vitour.R;
import com.uit.vitour.model.Booking;
import com.uit.vitour.model.Tour;
import com.uit.vitour.utils.Resource;
import com.uit.vitour.viewmodel.BookingViewModel;
import com.uit.vitour.viewmodel.TourDetailViewModel;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BookingFormFragment extends Fragment {

    private static final String TAG = "BookingFormFragment";

    private String tourId;
    private Tour currentTour;
    private BookingViewModel bookingViewModel;
    private TourDetailViewModel tourViewModel;

    private int adultsCount = 1;
    private int childrenCount = 0;
    private Date selectedDate = null;

    private TextView tvSelectedDate;
    private TextView tvAdultsCount;
    private TextView tvChildrenCount;
    private TextView tvTotalPrice;
    private ProgressBar progressBar;
    private MaterialButton btnConfirm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_booking_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            tourId = getArguments().getString("tourId");
        }

        if (tourId == null) {
            Toast.makeText(getContext(), "Error: Invalid Tour ID", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).navigateUp();
            return;
        }

        initViews(view);
        setupViewModels();
        setupListeners();
    }

    private void initViews(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        tvSelectedDate = view.findViewById(R.id.tv_selected_date);
        tvAdultsCount = view.findViewById(R.id.tv_adults_count);
        tvChildrenCount = view.findViewById(R.id.tv_children_count);
        tvTotalPrice = view.findViewById(R.id.tv_total_price);
        progressBar = view.findViewById(R.id.progress_booking);
        btnConfirm = view.findViewById(R.id.btn_confirm_booking);
        
        updateCounts();
    }

    private void setupViewModels() {
        bookingViewModel = new ViewModelProvider(this).get(BookingViewModel.class);
        tourViewModel = new ViewModelProvider(this).get(TourDetailViewModel.class);
        
        tourViewModel.setTourId(tourId);
        tourViewModel.getTour().observe(getViewLifecycleOwner(), resource -> {
            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                currentTour = resource.data;
                updateTotalPrice();
            } else if (resource.status == Resource.Status.ERROR) {
                Toast.makeText(getContext(), "Failed to load tour details", Toast.LENGTH_SHORT).show();
            }
        });

        bookingViewModel.getSubmitBookingResult().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            switch (resource.status) {
                case LOADING:
                    btnConfirm.setEnabled(false);
                    progressBar.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    btnConfirm.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Booking Successful!", Toast.LENGTH_SHORT).show();
                    
                    // Pop all the way back to Home safely without destroying the BottomNav state
                    Navigation.findNavController(requireView()).popBackStack(R.id.homeFragment, false);
                    
                    // Programmatically switch the BottomNavigationView to the Bookings tab
                    com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
                            requireActivity().findViewById(R.id.bottom_navigation);
                    if (bottomNav != null) {
                        bottomNav.setSelectedItemId(R.id.bookingFragment);
                    }
                    break;
                case ERROR:
                    btnConfirm.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    Snackbar.make(requireView(), resource.message != null ? resource.message : "Error", Snackbar.LENGTH_LONG).show();
                    break;
            }
        });
    }

    private void setupListeners() {
        View cardDatePicker = getView().findViewById(R.id.card_date_picker);
        cardDatePicker.setOnClickListener(v -> showDatePicker());

        ImageButton btnAdultsMinus = getView().findViewById(R.id.btn_adults_minus);
        ImageButton btnAdultsPlus = getView().findViewById(R.id.btn_adults_plus);
        ImageButton btnChildrenMinus = getView().findViewById(R.id.btn_children_minus);
        ImageButton btnChildrenPlus = getView().findViewById(R.id.btn_children_plus);

        btnAdultsMinus.setOnClickListener(v -> {
            if (adultsCount > 1) {
                adultsCount--;
                updateCounts();
            }
        });

        btnAdultsPlus.setOnClickListener(v -> {
            if (adultsCount + childrenCount < 10) {
                adultsCount++;
                updateCounts();
            } else {
                Toast.makeText(getContext(), "Maximum 10 people allowed", Toast.LENGTH_SHORT).show();
            }
        });

        btnChildrenMinus.setOnClickListener(v -> {
            if (childrenCount > 0) {
                childrenCount--;
                updateCounts();
            }
        });

        btnChildrenPlus.setOnClickListener(v -> {
            if (adultsCount + childrenCount < 10) {
                childrenCount++;
                updateCounts();
            } else {
                Toast.makeText(getContext(), "Maximum 10 people allowed", Toast.LENGTH_SHORT).show();
            }
        });

        btnConfirm.setOnClickListener(v -> submitBooking());
    }

    private void showDatePicker() {
        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
        constraintsBuilder.setValidator(DateValidatorPointForward.now());

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Booking Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(constraintsBuilder.build())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            // MaterialDatePicker uses UTC. We just store the Date.
            selectedDate = new Date(selection);
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            tvSelectedDate.setText(sdf.format(selectedDate));
            tvSelectedDate.setTextColor(getResources().getColor(R.color.vitour_text_primary, null));
        });

        datePicker.show(getParentFragmentManager(), "DATE_PICKER");
    }

    private void updateCounts() {
        tvAdultsCount.setText(String.valueOf(adultsCount));
        tvChildrenCount.setText(String.valueOf(childrenCount));
        updateTotalPrice();
    }

    private void updateTotalPrice() {
        if (currentTour == null) return;
        
        // Children pay half price for simplicity
        double total = (adultsCount * currentTour.getPrice()) + (childrenCount * currentTour.getPrice() * 0.5);
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        tvTotalPrice.setText(fmt.format(total));
    }

    private void submitBooking() {
        if (currentTour == null) return;
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please log in to book", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Booking booking = new Booking();
        booking.setTourId(currentTour.getId());
        booking.setUserId(userId);
        booking.setTourName(currentTour.getName());
        booking.setCoverImageUrl(currentTour.getCoverImageUrl());
        booking.setAdultsCount(adultsCount);
        booking.setChildrenCount(childrenCount);
        
        // Price calculation matching updateTotalPrice
        double total = (adultsCount * currentTour.getPrice()) + (childrenCount * currentTour.getPrice() * 0.5);
        booking.setTotalPrice(total);
        booking.setSelectedDate(selectedDate);

        bookingViewModel.submitBooking(requireContext(), booking);
    }
}
