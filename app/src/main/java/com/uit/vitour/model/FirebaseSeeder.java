package com.uit.vitour.model;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * FirebaseSeeder.java — One-time seeder for the "tours" Firestore collection.
 *
 * HOW IT WORKS:
 *   1. Checks if the "tours" collection already has documents.
 *   2. If empty → inserts 8 realistic Vietnam tour documents.
 *   3. If non-empty → does nothing (prevents duplicate insertions on every launch).
 *
 * USAGE (call once, e.g. in MainActivity.onCreate after auth check):
 *   FirebaseSeeder.seedIfEmpty();
 *
 * FIELDS match Tour.java exactly:
 *   id, name, location, description, coverImageUrl,
 *   price, rating, reviewCount, durationDays, isFeatured, tags
 */
public class FirebaseSeeder {

    private static final String TAG = "FirebaseSeeder";
    private static final String COLLECTION_TOURS = "tours";

    /** Call once from application startup. Safe to call repeatedly — idempotent. */
    public static void seedIfEmpty() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection(COLLECTION_TOURS).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        Log.d(TAG, "Tours collection already has " + snapshot.size()
                                + " documents. Skipping seed.");
                        return;
                    }
                    Log.d(TAG, "Tours collection is empty. Seeding sample data...");
                    insertAllTours(db);
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to check tours collection: " + e.getMessage()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static void insertAllTours(FirebaseFirestore db) {
        // 8 Vietnam tours. Each insertTour() is async; logs confirm success/failure.
        insertTour(db, buildDaLat());
        insertTour(db, buildHoiAn());
        insertTour(db, buildPhuQuoc());
        insertTour(db, buildSaPa());
        insertTour(db, buildNhaTrang());
        insertTour(db, buildHaLong());
        insertTour(db, buildDaNang());
        insertTour(db, buildHue());
        Log.d(TAG, "Seeder inserted 8 tours into '" + COLLECTION_TOURS + "' collection (async — check individual success logs)");
    }

    private static void insertTour(FirebaseFirestore db, Map<String, Object> tourData) {
        String id = (String) tourData.get("id");
        db.collection(COLLECTION_TOURS).document(id)
                .set(tourData)
                .addOnSuccessListener(unused ->
                        Log.d(TAG, "Seeder: inserted tour '" + tourData.get("name") + "' (id=" + id + ")"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Seeder: FAILED to insert tour '" + tourData.get("name")
                                + "' (id=" + id + "): " + e.getMessage(), e));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tour data builders — 8 realistic Vietnam destinations
    // ─────────────────────────────────────────────────────────────────────────

    private static Map<String, Object> buildDaLat() {
        Map<String, Object> t = new HashMap<>();
        t.put("id", "tour_dalat");
        t.put("name", "Đà Lạt City of Flowers");
        t.put("location", "Đà Lạt, Lâm Đồng");
        t.put("description",
                "Discover the romantic \"City of Flowers\" nestled in the Central Highlands. "
                + "Explore pine forests, misty valleys, French colonial villas, and vibrant "
                + "flower gardens. Enjoy fresh strawberries, artichoke tea, and cool mountain air.");
        t.put("coverImageUrl",
                "https://images.unsplash.com/photo-1600093463592-8e36ae95ef56"
                + "?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80");
        t.put("price", 1800000.0);
        t.put("rating", 4.7);
        t.put("reviewCount", 312);
        t.put("durationDays", 3);
        t.put("isFeatured", true);
        t.put("tags", Arrays.asList("mountain", "romantic", "cultural", "food"));
        return t;
    }

    private static Map<String, Object> buildHoiAn() {
        Map<String, Object> t = new HashMap<>();
        t.put("id", "tour_hoian");
        t.put("name", "Hội An Ancient Town");
        t.put("location", "Hội An, Quảng Nam");
        t.put("description",
                "Step back in time in the UNESCO World Heritage Ancient Town of Hội An. "
                + "Wander lantern-lit streets, taste white rose dumplings, rent a bicycle to "
                + "rice paddies, and join a cooking class learning authentic Central Vietnamese cuisine.");
        t.put("coverImageUrl",
                "https://images.unsplash.com/photo-1559592413-7cec4d0cae2b"
                + "?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80");
        t.put("price", 1200000.0);
        t.put("rating", 4.9);
        t.put("reviewCount", 587);
        t.put("durationDays", 2);
        t.put("isFeatured", true);
        t.put("tags", Arrays.asList("cultural", "food", "heritage", "lantern"));
        return t;
    }

    private static Map<String, Object> buildPhuQuoc() {
        Map<String, Object> t = new HashMap<>();
        t.put("id", "tour_phuquoc");
        t.put("name", "Phú Quốc Pearl Island");
        t.put("location", "Phú Quốc, Kiên Giang");
        t.put("description",
                "Vietnam's largest island paradise beckons with crystal-clear turquoise waters, "
                + "pristine white sand beaches, and world-class resorts. Explore coral reefs, "
                + "visit pepper farms, and savor fresh seafood at the night market.");
        t.put("coverImageUrl",
                "https://images.unsplash.com/photo-1506905925346-21bda4d32df4"
                + "?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80");
        t.put("price", 4500000.0);
        t.put("rating", 4.8);
        t.put("reviewCount", 429);
        t.put("durationDays", 4);
        t.put("isFeatured", true);
        t.put("tags", Arrays.asList("beach", "island", "snorkeling", "resort"));
        return t;
    }

    private static Map<String, Object> buildSaPa() {
        Map<String, Object> t = new HashMap<>();
        t.put("id", "tour_sapa");
        t.put("name", "Sa Pa Trekking & Homestay");
        t.put("location", "Sa Pa, Lào Cai");
        t.put("description",
                "Trek through dramatic rice terraces carved into the Hoàng Liên Son mountains. "
                + "Stay in a traditional H'Mong village homestay, meet local hill tribes, "
                + "and witness breathtaking sunrise views over Fansipan — Vietnam's highest peak.");
        t.put("coverImageUrl",
                "https://images.unsplash.com/photo-1543661858-6fb2362a934a"
                + "?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80");
        t.put("price", 2200000.0);
        t.put("rating", 4.7);
        t.put("reviewCount", 356);
        t.put("durationDays", 3);
        t.put("isFeatured", false);
        t.put("tags", Arrays.asList("mountain", "adventure", "trekking", "cultural"));
        return t;
    }

    private static Map<String, Object> buildNhaTrang() {
        Map<String, Object> t = new HashMap<>();
        t.put("id", "tour_nhatrang");
        t.put("name", "Nha Trang Beach Getaway");
        t.put("location", "Nha Trang, Khánh Hòa");
        t.put("description",
                "Vietnam's beach capital offers a stunning bay with 19 islands, "
                + "vibrant coral reefs for snorkeling and diving, healing mud baths, "
                + "and the ancient Po Nagar Cham Towers. Enjoy fresh seafood and energetic nightlife.");
        t.put("coverImageUrl",
                "https://images.unsplash.com/photo-1583417646543-9828d0ee54e1"
                + "?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80");
        t.put("price", 2800000.0);
        t.put("rating", 4.5);
        t.put("reviewCount", 478);
        t.put("durationDays", 3);
        t.put("isFeatured", false);
        t.put("tags", Arrays.asList("beach", "diving", "island", "nightlife"));
        return t;
    }

    private static Map<String, Object> buildHaLong() {
        Map<String, Object> t = new HashMap<>();
        t.put("id", "tour_halong");
        t.put("name", "Hạ Long Bay Cruise");
        t.put("location", "Hạ Long, Quảng Ninh");
        t.put("description",
                "Cruise through 1,600 limestone karst islands rising dramatically from "
                + "UNESCO-listed Hạ Long Bay. Kayak into hidden sea caves, watch the sunset "
                + "from the deck of a traditional junk boat, and explore fishing villages.");
        t.put("coverImageUrl",
                "https://images.unsplash.com/photo-1528127269322-539801943592"
                + "?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80");
        t.put("price", 3500000.0);
        t.put("rating", 4.9);
        t.put("reviewCount", 631);
        t.put("durationDays", 2);
        t.put("isFeatured", true);
        t.put("tags", Arrays.asList("beach", "cruise", "kayaking", "heritage"));
        return t;
    }

    private static Map<String, Object> buildDaNang() {
        Map<String, Object> t = new HashMap<>();
        t.put("id", "tour_danang");
        t.put("name", "Đà Nẵng City & Beach Break");
        t.put("location", "Đà Nẵng");
        t.put("description",
                "Vietnam's third-largest city combines urban energy with pristine beaches. "
                + "Walk the iconic Golden Bridge held by giant stone hands, explore Marble Mountain, "
                + "dine on bánh mì and mì Quảng, and swim in clear waters of My Khe Beach.");
        t.put("coverImageUrl",
                "https://images.unsplash.com/photo-1547036967-23d11aacaee0"
                + "?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80");
        t.put("price", 2000000.0);
        t.put("rating", 4.6);
        t.put("reviewCount", 389);
        t.put("durationDays", 3);
        t.put("isFeatured", false);
        t.put("tags", Arrays.asList("beach", "city", "food", "adventure"));
        return t;
    }

    private static Map<String, Object> buildHue() {
        Map<String, Object> t = new HashMap<>();
        t.put("id", "tour_hue");
        t.put("name", "Huế Imperial City");
        t.put("location", "Huế, Thừa Thiên Huế");
        t.put("description",
                "Immerse yourself in the grandeur of Vietnam's last imperial capital. "
                + "Explore the UNESCO Imperial Citadel, visit elaborately decorated royal tombs, "
                + "and taste royal court cuisine — a delicate culinary tradition preserved for centuries.");
        t.put("coverImageUrl",
                "https://images.unsplash.com/photo-1598977726547-9ded064d4e3c"
                + "?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80");
        t.put("price", 1500000.0);
        t.put("rating", 4.6);
        t.put("reviewCount", 271);
        t.put("durationDays", 2);
        t.put("isFeatured", false);
        t.put("tags", Arrays.asList("cultural", "heritage", "history", "food"));
        return t;
    }
}
