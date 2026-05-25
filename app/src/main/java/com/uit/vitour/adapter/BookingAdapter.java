package com.uit.vitour.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.uit.vitour.R;
import com.uit.vitour.model.Booking;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private List<Booking> bookings = new ArrayList<>();
    private final OnCancelClickListener cancelClickListener;

    public interface OnCancelClickListener {
        void onCancelClick(Booking booking);
    }

    public BookingAdapter(OnCancelClickListener cancelClickListener) {
        this.cancelClickListener = cancelClickListener;
    }

    public void submitList(List<Booking> newBookings) {
        this.bookings = newBookings;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        holder.bind(bookings.get(position));
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    class BookingViewHolder extends RecyclerView.ViewHolder {

        ImageView ivImage;
        Chip chipStatus;
        TextView tvTourName;
        TextView tvDetails;
        TextView btnCancel;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_booking_image);
            chipStatus = itemView.findViewById(R.id.chip_booking_status);
            tvTourName = itemView.findViewById(R.id.tv_booking_tour_name);
            tvDetails = itemView.findViewById(R.id.tv_booking_details);
            btnCancel = itemView.findViewById(R.id.btn_cancel_booking);
        }

        public void bind(Booking booking) {
            Glide.with(itemView.getContext())
                    .load(booking.getCoverImageUrl())
                    .centerCrop()
                    .into(ivImage);

            tvTourName.setText(booking.getTourName() != null ? booking.getTourName() : "Unknown Tour");

            String status = booking.getStatus() != null ? booking.getStatus() : Booking.STATUS_PENDING;
            chipStatus.setText(status);

            // Style chip based on status
            if (Booking.STATUS_CANCELLED.equals(status)) {
                chipStatus.setChipBackgroundColorResource(R.color.vitour_error);
                btnCancel.setVisibility(View.GONE);
            } else if (Booking.STATUS_CONFIRMED.equals(status)) {
                chipStatus.setChipBackgroundColorResource(R.color.vitour_primary);
                btnCancel.setVisibility(View.VISIBLE);
            } else {
                chipStatus.setChipBackgroundColorResource(R.color.vitour_secondary); // PENDING
                btnCancel.setVisibility(View.VISIBLE);
            }

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String dateStr = booking.getSelectedDate() != null ? sdf.format(booking.getSelectedDate()) : "TBD";
            
            String paxStr = booking.getAdultsCount() + " Adults, " + booking.getChildrenCount() + " Children";
            tvDetails.setText(dateStr + " • " + paxStr);

            btnCancel.setOnClickListener(v -> {
                if (cancelClickListener != null) {
                    cancelClickListener.onCancelClick(booking);
                }
            });
        }
    }
}
