package com.uit.vitour.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MockData.java
 * Provides sample data for the Home screen UI until backend is fully hooked up.
 */
public class MockData {

    public static List<Tour> getTours() {
        List<Tour> tours = new ArrayList<>();

        Tour t1 = new Tour(
                "t1",
                "Ha Long Bay Cruise",
                "Quang Ninh, Vietnam",
                "https://images.unsplash.com/photo-1528127269322-539801943592?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                2500000.0,
                4.8
        );
        t1.setFeatured(true);
        t1.setTags(Arrays.asList("beach", "cultural"));
        tours.add(t1);

        Tour t2 = new Tour(
                "t2",
                "Hoi An Ancient Town",
                "Quang Nam, Vietnam",
                "https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                1200000.0,
                4.9
        );
        t2.setFeatured(true);
        t2.setTags(Arrays.asList("cultural", "food"));
        tours.add(t2);

        Tour t3 = new Tour(
                "t3",
                "Sapa Trekking & Homestay",
                "Lao Cai, Vietnam",
                "https://images.unsplash.com/photo-1543661858-6fb2362a934a?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                1800000.0,
                4.7
        );
        t3.setTags(Arrays.asList("mountain", "adventure"));
        tours.add(t3);

        Tour t4 = new Tour(
                "t4",
                "Phong Nha Cave Exploration",
                "Quang Binh, Vietnam",
                "https://images.unsplash.com/photo-1598463388701-a185125bb7c4?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                3000000.0,
                4.9
        );
        t4.setTags(Arrays.asList("adventure"));
        tours.add(t4);

        Tour t5 = new Tour(
                "t5",
                "Da Nang Beach Resort",
                "Da Nang, Vietnam",
                "https://images.unsplash.com/photo-1583417646543-9828d0ee54e1?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                4500000.0,
                4.6
        );
        t5.setTags(Arrays.asList("beach"));
        tours.add(t5);

        return tours;
    }

    public static List<String> getCategories() {
        return Arrays.asList("All", "Beach", "Mountain", "Cultural", "Adventure", "Food");
    }
}
