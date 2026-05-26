package com.uit.vitour.viewmodel;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.uit.vitour.model.Booking;
import com.uit.vitour.repository.BookingRepository;
import com.uit.vitour.utils.Resource;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class BookingViewModel extends ViewModel {

    private final BookingRepository repository;
    
    // Caches for the LiveData results
    private LiveData<Resource<List<Booking>>> userBookingsLiveData;
    private String currentUserId;

    // Booking Submission State
    private final androidx.lifecycle.MediatorLiveData<Resource<Booking>> submitBookingResult = new androidx.lifecycle.MediatorLiveData<>();
    private boolean isSubmitting = false;

    public LiveData<Resource<Booking>> getSubmitBookingResult() {
        return submitBookingResult;
    }

    public BookingViewModel() {
        repository = new BookingRepository();
    }

    public LiveData<Resource<List<Booking>>> getUserBookings(Context context, String userId) {
        if (userBookingsLiveData == null || !userId.equals(currentUserId)) {
            currentUserId = userId;
            userBookingsLiveData = repository.getUserBookings(context, userId);
        }
        return userBookingsLiveData;
    }

    // Retry fetching bookings (clears cache and fetches again)
    public LiveData<Resource<List<Booking>>> retryUserBookings(Context context, String userId) {
        currentUserId = userId;
        userBookingsLiveData = repository.getUserBookings(context, userId);
        return userBookingsLiveData;
    }

    public void submitBooking(Context context, Booking booking) {
        if (isSubmitting) return;

        // 1. Validation: Pax Count
        int totalPax = booking.getAdultsCount() + booking.getChildrenCount();
        if (totalPax <= 0) {
            submitBookingResult.setValue(Resource.error("Must have at least 1 person.", null));
            return;
        }
        if (totalPax > 10) {
            submitBookingResult.setValue(Resource.error("Maximum 10 people allowed per booking.", null));
            return;
        }

        // 2. Validation: Date
        if (booking.getSelectedDate() == null) {
            submitBookingResult.setValue(Resource.error("Please select a date.", null));
            return;
        }
        
        Calendar calNow = Calendar.getInstance();
        calNow.set(Calendar.HOUR_OF_DAY, 0);
        calNow.set(Calendar.MINUTE, 0);
        calNow.set(Calendar.SECOND, 0);
        calNow.set(Calendar.MILLISECOND, 0);
        Date today = calNow.getTime();

        if (booking.getSelectedDate().before(today)) {
            submitBookingResult.setValue(Resource.error("Selected date must be today or in the future.", null));
            return;
        }
        
        Calendar cal = Calendar.getInstance();
        cal.setTime(today);
        cal.add(Calendar.YEAR, 1);
        Date oneYearFromNow = cal.getTime();
        
        if (booking.getSelectedDate().after(oneYearFromNow)) {
            submitBookingResult.setValue(Resource.error("Selected date cannot be more than 1 year from today.", null));
            return;
        }

        // 3. Submit to Repository
        isSubmitting = true;
        submitBookingResult.setValue(Resource.loading(null));

        LiveData<Resource<Booking>> repoData = repository.createBooking(context, booking);
        submitBookingResult.addSource(repoData, res -> {
            submitBookingResult.setValue(res);
            if (res.status == Resource.Status.SUCCESS || res.status == Resource.Status.ERROR) {
                isSubmitting = false;
                submitBookingResult.removeSource(repoData);
            }
        });
    }
    
    public LiveData<Resource<Boolean>> cancelBooking(Context context, String bookingId) {
        return repository.cancelBooking(context, bookingId);
    }

    public LiveData<Resource<Boolean>> updateBookingStatus(String bookingId, String newStatus) {
        return repository.updateBookingStatus(bookingId, newStatus);
    }
}
