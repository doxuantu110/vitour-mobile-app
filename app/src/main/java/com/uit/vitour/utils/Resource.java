package com.uit.vitour.utils;

/**
 * Resource.java — A generic wrapper that exposes data + UI state.
 *
 * WHY: ViewModels post a Resource<T> via LiveData so fragments can
 * react to three states without checking nulls everywhere.
 *
 * Usage in a Fragment:
 *
 *   viewModel.getTours().observe(getViewLifecycleOwner(), resource -> {
 *       switch (resource.status) {
 *           case LOADING: showLoading(); break;
 *           case SUCCESS: showData(resource.data); break;
 *           case ERROR:   showError(resource.message); break;
 *       }
 *   });
 *
 * Based on Google's recommended architecture guide pattern.
 */
public class Resource<T> {

    public enum Status { SUCCESS, ERROR, LOADING }

    public final Status status;
    public final T data;
    public final String message;

    private Resource(Status status, T data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    /** Call when data is available. */
    public static <T> Resource<T> success(T data) {
        return new Resource<>(Status.SUCCESS, data, null);
    }

    /** Call when an operation fails. */
    public static <T> Resource<T> error(String message, T data) {
        return new Resource<>(Status.ERROR, data, message);
    }

    /** Call while waiting for data (show spinner). */
    public static <T> Resource<T> loading(T data) {
        return new Resource<>(Status.LOADING, data, null);
    }
}
