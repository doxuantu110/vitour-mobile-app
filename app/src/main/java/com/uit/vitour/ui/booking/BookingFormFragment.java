package com.uit.vitour.ui.booking;

import android.os.Bundle;
import com.uit.vitour.model.PaymentRequest;
import com.uit.vitour.model.PaymentResponse;
import com.uit.vitour.network.ApiClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
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
    private ListenerRegistration paymentListener;
    private android.os.CountDownTimer paymentTimer;

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
                    requestSePayPayment(resource.data);
                    break;
                case ERROR:
                    btnConfirm.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    Snackbar.make(requireView(), resource.message != null ? resource.message : "Error", Snackbar.LENGTH_LONG).show();
                    break;
            }
        });
    }

    private void requestSePayPayment(Booking booking) {
        progressBar.setVisibility(View.VISIBLE);
        btnConfirm.setEnabled(false);

        long paymentAmount = Math.round(booking.getTotalPrice());
        Log.d(TAG, "[SEPAY_CREATE] Preparing SePay Payment. BookingId: " + booking.getBookingId() + 
                   ", Amount: " + paymentAmount);

        PaymentRequest request = new PaymentRequest(
                booking.getBookingId(),
                paymentAmount,
                "Payment for tour: " + booking.getTourName()
        );

        ApiClient.getInstance().getPaymentApiService().createPayment(request)
                .enqueue(new Callback<PaymentResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<PaymentResponse> call, @NonNull Response<PaymentResponse> response) {
                        progressBar.setVisibility(View.GONE);
                        btnConfirm.setEnabled(true);

                        if (response.isSuccessful() && response.body() != null) {
                            String qrCodeUrl = response.body().getQrCodeUrl();
                            if (qrCodeUrl != null) {
                                showSePayQrDialog(qrCodeUrl, booking.getBookingId(), paymentAmount, response.body().getBankCode(), response.body().getAccountNumber());
                            } else {
                                Toast.makeText(requireContext(), "No QR code URL received", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(requireContext(), "Failed to create payment: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<PaymentResponse> call, @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        btnConfirm.setEnabled(true);
                        Log.e(TAG, "Network Error: ", t);
                        Toast.makeText(requireContext(), "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showSePayQrDialog(String qrCodeUrl, String bookingId, long paymentAmount, String bankCode, String accountNumber) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_sepay_qr, null);
        ImageView ivQrCode = dialogView.findViewById(R.id.iv_qr_code);
        ProgressBar progressQrLoading = dialogView.findViewById(R.id.progress_qr_loading);
        TextView tvPaymentAmount = dialogView.findViewById(R.id.tv_payment_amount);
        TextView tvTransferContent = dialogView.findViewById(R.id.tv_transfer_content);
        TextView tvBankInfo = dialogView.findViewById(R.id.tv_bank_info);
        TextView tvCountdown = dialogView.findViewById(R.id.tv_countdown);
        MaterialButton btnSaveQr = dialogView.findViewById(R.id.btn_save_qr);
        MaterialButton btnCancelPayment = dialogView.findViewById(R.id.btn_cancel_payment);
        MaterialButton btnRetryQr = dialogView.findViewById(R.id.btn_retry_qr);

        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        tvPaymentAmount.setText("Số tiền: " + fmt.format(paymentAmount));
        
        String shortBookingId = bookingId.length() >= 8 ? bookingId.substring(0, 8) : bookingId;
        tvTransferContent.setText("Nội dung: BOOKING_" + shortBookingId);
        
        if (bankCode != null && accountNumber != null) {
            tvBankInfo.setText("Ngân hàng: " + bankCode + " - STK: " + accountNumber);
        } else {
            tvBankInfo.setVisibility(View.GONE);
        }

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.setCancelable(false);
        bottomSheetDialog.setCanceledOnTouchOutside(false);

        btnCancelPayment.setOnClickListener(v -> {
            cleanupPaymentListeners();
            bottomSheetDialog.dismiss();
            
            // Mark as cancelled
            FirebaseFirestore.getInstance().collection("bookings").document(bookingId)
                .update("status", "CANCELLED", "paymentStatus", "CANCELLED");
        });

        btnSaveQr.setOnClickListener(v -> {
            saveQrCodeToGallery(ivQrCode, bookingId);
        });

        Runnable loadQrCode = new Runnable() {
            @Override
            public void run() {
                progressQrLoading.setVisibility(View.VISIBLE);
                btnRetryQr.setVisibility(View.GONE);
                Log.d(TAG, "[SEPAY_REQUEST] Starting Glide load: " + qrCodeUrl);
                
                Glide.with(BookingFormFragment.this)
                        .load(qrCodeUrl)
                        .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                                progressQrLoading.setVisibility(View.GONE);
                                btnRetryQr.setVisibility(View.VISIBLE);
                                Log.e(TAG, "[SEPAY_RESPONSE] Glide load failed", e);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                                progressQrLoading.setVisibility(View.GONE);
                                btnRetryQr.setVisibility(View.GONE);
                                Log.d(TAG, "[SEPAY_RESPONSE] Glide load success");
                                return false;
                            }
                        })
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_dialog_alert)
                        .into(ivQrCode);
            }
        };

        btnRetryQr.setOnClickListener(v -> loadQrCode.run());
        
        // Initial load
        loadQrCode.run();

        // 10 minute countdown timer
        paymentTimer = new android.os.CountDownTimer(600000, 1000) {
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                long minutes = seconds / 60;
                long remainingSeconds = seconds % 60;
                tvCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds));
            }

            public void onFinish() {
                tvCountdown.setText("00:00");
                cleanupPaymentListeners();
                bottomSheetDialog.dismiss();
                
                // Update firestore that it expired
                FirebaseFirestore.getInstance().collection("bookings").document(bookingId)
                    .update("status", "CANCELLED", "paymentStatus", "CANCELLED");
                    
                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Hết thời gian thanh toán")
                    .setMessage("Phiên thanh toán của bạn đã hết hạn. Vui lòng thử lại.")
                    .setPositiveButton("Đóng", null)
                    .show();
            }
        }.start();

        Log.d(TAG, "[PAYMENT_LISTENER] Listening for payment status...");
        paymentListener = FirebaseFirestore.getInstance()
                .collection("bookings")
                .document(bookingId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "[PAYMENT_LISTENER] Listen failed.", error);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        String status = snapshot.getString("paymentStatus");
                        if ("PAID".equals(status)) {
                            Log.d(TAG, "[PAYMENT_SUCCESS] Payment confirmed for booking: " + bookingId);
                            cleanupPaymentListeners();
                            
                            if (bottomSheetDialog.isShowing()) {
                                bottomSheetDialog.dismiss();
                            }
                            
                            new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Thanh toán thành công")
                                .setMessage("Booking đã được xác nhận. Chúc bạn có một chuyến đi tuyệt vời!")
                                .setIcon(android.R.drawable.ic_dialog_info)
                                .setPositiveButton("Awesome!", (d, w) -> {
                                    Navigation.findNavController(requireView()).popBackStack(R.id.homeFragment, false);
                                    com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
                                            requireActivity().findViewById(R.id.bottom_navigation);
                                    if (bottomNav != null) {
                                        bottomNav.setSelectedItemId(R.id.bookingFragment);
                                    }
                                })
                                .setCancelable(false)
                                .show();
                        }
                    }
                });
                
        bottomSheetDialog.show();
    }
    
    private void cleanupPaymentListeners() {
        if (paymentListener != null) {
            paymentListener.remove();
            paymentListener = null;
        }
        if (paymentTimer != null) {
            paymentTimer.cancel();
            paymentTimer = null;
        }
    }

    private void saveQrCodeToGallery(ImageView imageView, String bookingId) {
        if (imageView.getDrawable() == null) {
            Toast.makeText(requireContext(), "QR code is not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            android.graphics.Bitmap bitmap = ((android.graphics.drawable.BitmapDrawable) imageView.getDrawable()).getBitmap();
            
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "QR_BOOKING_" + bookingId + ".jpg");
            values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 1);
            }

            android.content.ContentResolver resolver = requireContext().getContentResolver();
            android.net.Uri collection = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
                    ? android.provider.MediaStore.Images.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    : android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            
            android.net.Uri itemUri = resolver.insert(collection, values);
            if (itemUri != null) {
                try (java.io.OutputStream out = resolver.openOutputStream(itemUri)) {
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, out);
                }
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    values.clear();
                    values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0);
                    resolver.update(itemUri, values, null, null);
                }
                
                Toast.makeText(requireContext(), "QR saved successfully", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to save QR code", e);
            Toast.makeText(requireContext(), "Failed to save QR code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
        
        booking.setStatus("PENDING_PAYMENT");
        booking.setPaymentStatus("UNPAID");

        bookingViewModel.submitBooking(requireContext(), booking);
    }
}
