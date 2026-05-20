package com.uit.vitour;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.badge.BadgeDrawable;
import com.uit.vitour.databinding.ActivityMainBinding;

/**
 * MainActivity.java — Shell activity for the authenticated part of the app.
 *
 * ┌─────────────────────────────────────────┐
 * │           NavHostFragment               │  ← fills entire screen
 * │  (HomeFragment / SearchFragment /       │
 * │   BookingFragment / ProfileFragment)    │
 * ├─────────────────────────────────────────┤
 * │  Home  │  Search  │  Booking  │ Profile │  ← BottomNavigationView
 * └─────────────────────────────────────────┘
 *
 * RESPONSIBILITIES (and NOTHING else):
 *   1. Inflate the shell layout via View Binding
 *   2. Wire NavController ↔ BottomNavigationView via NavigationUI
 *   3. Optionally show/hide the nav bar for full-screen destinations
 *   4. Expose a badge API so Fragments can show notification counts
 *
 * RULE: No business logic here. Fragments own their own ViewModels.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // View Binding eliminates all findViewById() calls
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupNavigation();
    }

    // ── Navigation Setup ──────────────────────────────────────────────────────

    /**
     * Wires the BottomNavigationView to the Navigation Component.
     *
     * How it works:
     *   • NavigationUI reads each menu item's android:id
     *   • It matches those IDs to fragment destinations in nav_graph.xml
     *   • Tapping a tab calls NavController.navigate(destinationId)
     *   • NavController swaps the Fragment inside NavHostFragment
     *
     * Result: zero custom tab-switching logic needed here.
     */
    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment == null) return;

        navController = navHostFragment.getNavController();

        // Connect BottomNav ↔ NavController
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);

        // Optional: hide the bottom bar on detail/full-screen destinations
        // Add destination IDs to this listener as the app grows.
        navController.addOnDestinationChangedListener(this::onDestinationChanged);
    }

    /**
     * Called every time the NavController navigates to a new destination.
     * Use this to show/hide the BottomNavigationView for full-screen screens.
     *
     * Example: hide nav bar on a photo gallery or map full-screen fragment:
     *   if (destination.getId() == R.id.tourDetailFragment) {
     *       hideBottomNav();
     *   } else {
     *       showBottomNav();
     *   }
     */
    private void onDestinationChanged(NavController controller,
                                      NavDestination destination,
                                      Bundle arguments) {
        // Currently all 4 tab fragments keep the bottom bar visible.
        // Add full-screen fragment IDs here when needed.
        showBottomNav();
    }

    // ── Public API for Fragments ──────────────────────────────────────────────

    /**
     * Show a numbered badge on the Booking tab.
     * Call this from a ViewModel observer when a new booking arrives.
     *
     * Example (from a Fragment):
     *   ((MainActivity) requireActivity()).showBookingBadge(2);
     */
    public void showBookingBadge(int count) {
        BadgeDrawable badge = binding.bottomNavigation
                .getOrCreateBadge(R.id.bookingFragment);
        badge.setVisible(true);
        if (count > 0) badge.setNumber(count);
    }

    /** Remove the badge from the Booking tab (call after user views bookings). */
    public void clearBookingBadge() {
        binding.bottomNavigation.removeBadge(R.id.bookingFragment);
    }

    /** Hide the entire BottomNavigationView (e.g. for full-screen fragments). */
    public void hideBottomNav() {
        binding.bottomNavigation.setVisibility(View.GONE);
    }

    /** Restore the BottomNavigationView. */
    public void showBottomNav() {
        binding.bottomNavigation.setVisibility(View.VISIBLE);
    }

    // ── Static Helpers ────────────────────────────────────────────────────────

    /**
     * Creates an Intent that returns to LoginActivity and clears the back stack.
     * Called from ProfileFragment after logout.
     */
    public static Intent createLogoutIntent(android.content.Context context) {
        Intent intent = new Intent(context, com.uit.vitour.ui.auth.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }
}
