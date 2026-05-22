package com.uit.vitour;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.badge.BadgeDrawable;
import com.google.firebase.firestore.FirebaseFirestore;
import com.uit.vitour.databinding.ActivityMainBinding;
import com.uit.vitour.model.FirebaseSeeder;

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

    private static final String TAG = "MainActivity";

    private ActivityMainBinding binding;
    private NavController navController;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // View Binding eliminates all findViewById() calls
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ── FIREBASE DEBUG: verify Firestore connection at startup ──────────
        // This runs a direct query to 'tours' to confirm:
        //   1. Firestore is initialized and reachable
        //   2. The 'tours' collection exists
        //   3. Documents are readable (security rules allow it)
        // Filter Logcat by "FIREBASE_DEBUG" to see results.
        runFirestoreStartupCheck();

        // Seed Firestore with sample tours if the collection is empty.
        // This runs once per install; subsequent calls are no-ops.
        FirebaseSeeder.seedIfEmpty();

        setupNavigation();
    }

    /**
     * Direct Firestore query at startup — completely independent of the
     * MVVM pipeline. If this succeeds but tours still don't show in the UI,
     * the bug is in ViewModel/Fragment/Adapter. If this fails, the bug is
     * in Firebase config, network, or security rules.
     */
    private void runFirestoreStartupCheck() {
        Log.d("FIREBASE_DEBUG", "=== Firestore startup check BEGIN ===");
        Log.d("FIREBASE_DEBUG", "Firestore initialized: " + FirebaseFirestore.getInstance());
        Log.d("FIREBASE_DEBUG", "Project ID: "
                + FirebaseFirestore.getInstance().getApp().getOptions().getProjectId());

        FirebaseFirestore.getInstance()
                .collection("tours")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Log.d("FIREBASE_DEBUG", "=== Firestore startup check RESULT ===");
                    Log.d("FIREBASE_DEBUG", "Documents count = " + snapshot.size());
                    Log.d("FIREBASE_DEBUG", "fromCache = " + snapshot.getMetadata().isFromCache());

                    if (snapshot.isEmpty()) {
                        Log.w("FIREBASE_DEBUG", "WARNING: 'tours' collection is EMPTY! "
                                + "Seeder may not have run, or Firestore rules block reads.");
                    } else {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot) {
                            Log.d("FIREBASE_DEBUG", "  doc[" + doc.getId() + "] = " + doc.getData());
                        }
                        Log.d("FIREBASE_DEBUG", "✅ Firestore connection CONFIRMED. "
                                + snapshot.size() + " tour documents found.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE_DEBUG", "❌ Firestore startup check FAILED: " + e.getMessage(), e);
                    Log.e("FIREBASE_DEBUG", "CAUSE: Check Firestore security rules, "
                            + "internet connection, and google-services.json project ID.");
                });
    }

    // ── Navigation Setup ──────────────────────────────────────────────────────

    /**
     * Wires the BottomNavigationView to the Navigation Component.
     */
    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment == null) {
            Log.e(TAG, "NavHostFragment is null — check activity_main.xml nav_host_fragment id");
            return;
        }

        navController = navHostFragment.getNavController();

        // Connect BottomNav ↔ NavController
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);

        // Optional: hide the bottom bar on detail/full-screen destinations
        navController.addOnDestinationChangedListener(this::onDestinationChanged);
    }

    /**
     * Called every time the NavController navigates to a new destination.
     */
    private void onDestinationChanged(NavController controller,
                                      NavDestination destination,
                                      Bundle arguments) {
        showBottomNav();
    }

    // ── Public API for Fragments ──────────────────────────────────────────────

    /**
     * Show a numbered badge on the Booking tab.
     */
    public void showBookingBadge(int count) {
        BadgeDrawable badge = binding.bottomNavigation
                .getOrCreateBadge(R.id.bookingFragment);
        badge.setVisible(true);
        if (count > 0) badge.setNumber(count);
    }

    /** Remove the badge from the Booking tab. */
    public void clearBookingBadge() {
        binding.bottomNavigation.removeBadge(R.id.bookingFragment);
    }

    /** Hide the entire BottomNavigationView. */
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
