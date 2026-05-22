package com.uit.vitour.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.uit.vitour.model.Tour;
import com.uit.vitour.repository.TourRepository;
import com.uit.vitour.utils.Resource;

public class TourDetailViewModel extends ViewModel {

    private final TourRepository repository;
    private final MutableLiveData<String> tourIdLiveData = new MutableLiveData<>();
    private final LiveData<Resource<Tour>> tourLiveData;

    public TourDetailViewModel() {
        this.repository = new TourRepository();
        
        // When tourId changes, fetch the new tour details
        this.tourLiveData = Transformations.switchMap(tourIdLiveData, id -> {
            if (id == null || id.isEmpty()) {
                MutableLiveData<Resource<Tour>> error = new MutableLiveData<>();
                error.setValue(Resource.error("Invalid Tour ID", null));
                return error;
            }
            return repository.getTourById(id);
        });
    }

    public void setTourId(String tourId) {
        if (tourIdLiveData.getValue() == null || !tourIdLiveData.getValue().equals(tourId)) {
            tourIdLiveData.setValue(tourId);
        }
    }

    public LiveData<Resource<Tour>> getTour() {
        return tourLiveData;
    }
}
