// ─────────────────────────────────────────────────────────────────────────────
// app/build.gradle.kts  —  Module-level build configuration
//
// Stack: Android Native Java + MVVM + Repository
// Plugins applied:
//   • android.application  → builds the APK
//   • google.services      → reads google-services.json (Firebase + Maps API key)
// ─────────────────────────────────────────────────────────────────────────────

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace   = "com.uit.vitour"
    compileSdk  = 35

    defaultConfig {
        applicationId   = "com.uit.vitour"
        minSdk          = 24        // Android 7.0 — covers 98 %+ of active devices
        targetSdk       = 35
        versionCode     = 1
        versionName     = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Embed the Maps API key from local.properties so it is NOT committed to git.
        // In local.properties add:  MAPS_API_KEY=AIza...
        // Then reference it in AndroidManifest.xml:
        //   <meta-data android:name="com.google.android.geo.API_KEY"
        //              android:value="${MAPS_API_KEY}" />
        manifestPlaceholders["MAPS_API_KEY"] =
            (project.findProperty("MAPS_API_KEY") as String?) ?: ""
    }

    buildTypes {
        debug {
            // Keep full logs in debug; OkHttp logging interceptor is active
            isMinifyEnabled = false
            isDebuggable    = true
        }
        release {
            isMinifyEnabled = true          // Enable ProGuard/R8 for production
            isShrinkResources = true        // Remove unused resources too
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        // Java 11 source/target — required by newer Retrofit and RxJava3
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true     // Replaces findViewById() across all layouts
        buildConfig = true     // Allows BuildConfig.DEBUG checks at runtime
    }
}

dependencies {

    // ── AndroidX Core & UI ────────────────────────────────────────────────
    // AppCompat    : Activity/Fragment base classes, Toolbar support
    // core-ktx     : provides WindowCompat, core utils (KTX is safe in Java too)
    // activity     : ActivityResultContracts, edge-to-edge API
    // constraintlayout : flexible XML layouts (used in all screens)
    // recyclerview : efficient scrollable lists (HomeFragment, ExploreFragment)
    // cardview     : item_tour_card.xml uses MaterialCardView (subclass)
    // swiperefresh : pull-to-refresh on HomeFragment
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.swiperefreshlayout)

    // ── Material Design 3 ─────────────────────────────────────────────────
    // Provides: BottomNavigationView, MaterialCardView, TextInputLayout,
    //           MaterialButton, SearchBar, ShapeableImageView (avatar),
    //           MaterialAlertDialog (logout confirm), CircularProgressIndicator
    implementation(libs.material)
    implementation("com.google.android.material:material:1.12.0")
    // ── Navigation Component ──────────────────────────────────────────────
    // navigation-fragment : NavHostFragment, NavController, back-stack mgmt
    // navigation-ui       : NavigationUI.setupWithNavController() for BottomNav
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    // ── Lifecycle: ViewModel + LiveData ───────────────────────────────────
    // lifecycle-viewmodel : ViewModel base — survives rotation
    // lifecycle-livedata  : MutableLiveData, Transformations.switchMap (Explore search)
    // lifecycle-runtime   : LifecycleOwner, DefaultLifecycleObserver
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.runtime)

    // ── Image Loading: Glide ──────────────────────────────────────────────
    // Used in: TourAdapter (tour cover images), ProfileFragment (circular avatar)
    // Features: disk cache, memory cache, circleCrop(), crossfade animation
    implementation(libs.glide)

    // ── Networking: Retrofit + OkHttp ─────────────────────────────────────
    // retrofit               : turns TourApiService interface into real HTTP calls
    // retrofit-converter-gson: auto-maps JSON responses → Tour / User Java POJOs
    // okhttp-logging         : logs HTTP request/response to Logcat (debug builds)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

    // ── JSON Parsing: Gson ────────────────────────────────────────────────
    // Used by: Retrofit converter + any manual JSON parsing you need
    implementation(libs.gson)

    // ── Reactive Programming: RxJava 3 ────────────────────────────────────
    //
    // WHY RxJava3 alongside LiveData?
    //   - LiveData: best for UI state (lifecycle-safe, simple observers)
    //   - RxJava3:  best for chaining complex async Retrofit pipelines,
    //               debouncing search input, combining multiple streams
    //
    // rxjava3               : Observable<T>, Single<T>, Flowable<T>, operators
    // rxandroid3            : AndroidSchedulers.mainThread() — post to UI thread
    // retrofit-adapter-rxjava3: Retrofit returns Single<T>/Observable<T>
    //
    // Typical Retrofit + RxJava3 call in a Repository:
    //   apiService.getTours(page, size, null)
    //       .subscribeOn(Schedulers.io())         // network on IO thread
    //       .observeOn(AndroidSchedulers.mainThread()) // result on main thread
    //       .subscribe(tours -> { ... }, error -> { ... });
    implementation(libs.rxjava3)
    implementation(libs.rxandroid3)
    implementation(libs.retrofit.adapter.rxjava3)

    // ── Firebase (BoM manages ALL versions) ───────────────────────────────
    // firebase-auth      : email/password login, register, signOut()
    // firebase-firestore : tours collection, users collection, bookmarks sub-collection
    // firebase-storage   : upload/download profile photos and tour images
    // firebase-analytics : optional event tracking (screen views, clicks)
    //
    // IMPORTANT: with BoM, do NOT specify version on individual Firebase libs.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.analytics)

    // ── Google Maps ───────────────────────────────────────────────────────
    // google-maps    : SupportMapFragment, GoogleMap, LatLng, Marker
    //                  Used in: TourDetailFragment to show destination on map
    // google-location: FusedLocationProviderClient
    //                  Used in: ExploreFragment to center map on user location
    //                  Requires ACCESS_FINE_LOCATION permission at runtime
    implementation(libs.google.maps)
    implementation(libs.google.location)

    // ── Testing ───────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}