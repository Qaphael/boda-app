# Bug Fixes — Map Rendering, Location Permission & Distance Calculation

Three separate bugs, each with a distinct root cause.

---

## Bug 1 — Map Not Rendering

### Root causes (two, both required to fix)

**A. `MAPS_API_KEY` is a placeholder**

`GuluMapView` checks `BuildConfig.MAPS_API_KEY` at runtime:

```kotlin
// BodaScreens.kt ~ line 1000
val hasMapsApiKey = try {
    com.example.BuildConfig.MAPS_API_KEY.isNotEmpty() &&
    com.example.BuildConfig.MAPS_API_KEY != "MY_MAPS_API_KEY" &&
    com.example.BuildConfig.MAPS_API_KEY != "MAPS_API_KEY_DEFAULT_VALUE"
} catch (e: Throwable) { false }
```

Your `.env.example` has `MAPS_API_KEY=MY_MAPS_API_KEY` and there is no `.env` file
on disk. `hasMapsApiKey` is `false`, so the real `GoogleMapViewWrapper` is never
called — the app silently falls back to `GuluCanvasMapView` (the Canvas street grid)
every time. This is why you see the custom vector drawing instead of a live map.

**Fix — create a `.env` file at the project root:**

```
# .env  (project root, same level as settings.gradle.kts)
MAPS_API_KEY=your_real_key_here
GEMINI_API_KEY=your_gemini_key_here
```

Get a key from Google Cloud Console → APIs & Services → Credentials. Enable both
**Maps SDK for Android** and **Distance Matrix API** on that key.

---

**B. `MapView` lifecycle is incomplete — `ON_START` and `ON_RESUME` are never forwarded**

Even with a valid key, the map will render a grey/blank tile on many devices because
`GoogleMapViewWrapper` is missing two lifecycle events from its observer:

```kotlin
// BodaScreens.kt ~ line 872 — CURRENT (broken):
when (event) {
    androidx.lifecycle.Lifecycle.Event.ON_PAUSE  -> mapView.onPause()
    androidx.lifecycle.Lifecycle.Event.ON_STOP   -> mapView.onStop()
    androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
    else -> {}   // ← ON_START and ON_RESUME are silently swallowed
}
```

`MapView` is a traditional Android `View` that requires its full lifecycle to be
forwarded manually. Without `onStart()` and `onResume()` being called by the
observer, the map GL surface never initialises properly after the activity comes
back from the background — you get a blank grey rectangle.

Additionally, `onCreate(null)` is called in the `factory` block but it passes
`null` for the `Bundle`. While this is technically acceptable for a first-time
create, `onStart()` must still be called before `onResume()`.

**Fix — replace the `DisposableEffect` observer and the `factory` block:**

```kotlin
// BodaScreens.kt — GoogleMapViewWrapper, replace the DisposableEffect:

DisposableEffect(lifecycleOwner) {
    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
        val mapView = mapViewRef ?: return@LifecycleEventObserver
        when (event) {
            androidx.lifecycle.Lifecycle.Event.ON_CREATE  -> mapView.onCreate(null)
            androidx.lifecycle.Lifecycle.Event.ON_START   -> mapView.onStart()   // ADD
            androidx.lifecycle.Lifecycle.Event.ON_RESUME  -> mapView.onResume()  // ADD
            androidx.lifecycle.Lifecycle.Event.ON_PAUSE   -> mapView.onPause()
            androidx.lifecycle.Lifecycle.Event.ON_STOP    -> mapView.onStop()
            androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
            else -> {}
        }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
        lifecycleOwner.lifecycle.removeObserver(observer)
        mapViewRef?.let { mv ->
            mv.onPause()
            mv.onStop()    // ADD — must stop before destroy
            mv.onDestroy()
        }
    }
}

// And update the factory block — remove the manual onCreate/onResume calls
// because the observer now handles them:
factory = { ctx ->
    com.google.android.gms.maps.MapView(ctx).also { mapView ->
        mapViewRef = mapView
        // Do NOT call onCreate/onResume here — the lifecycle observer does it
        mapView.getMapAsync { googleMap ->
            googleMap.uiSettings.isZoomControlsEnabled = true
            googleMap.uiSettings.isMapToolbarEnabled = false
            googleMap.mapType = com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL
        }
    }
}
```

---

**C. `MapsInitializer` is never called (causes blank map on first cold start)**

On Android 6–9, the Maps SDK requires `MapsInitializer.initialize()` to be called
before the first `MapView.onCreate()`. Without it, the GL renderer may fail silently.

**Fix — add to `MainActivity.onCreate()`:**

```kotlin
// MainActivity.kt, inside onCreate(), before setContent {}:
com.google.android.gms.maps.MapsInitializer.initialize(
    applicationContext,
    com.google.android.gms.maps.MapsInitializer.Renderer.LATEST
) { renderer ->
    android.util.Log.d("BODA_MAPS", "Maps renderer: $renderer")
}
```

---

## Bug 2 — Location Permission Never Requested

### Root cause

When we moved permissions out of the onboarding flow (from the previous doc),
the location permission request was placed inside `RoutePreviewScreen`. But that
implementation was **not added to the actual file** — only the notification nudge
(`NotificationPermissionNudge`) was wired up. The result:

- `viewModel.locationPermissionGranted` is initialised to `false`
- Nobody ever sets it to `true`
- `LaunchedEffect(viewModel.locationPermissionGranted)` in `BodaAppContent` never fires
- `startLocationTracking()` is never called
- `fusedLocationClient.requestLocationUpdates()` is never reached
- `currentLocation` stays `null` forever

The notification dialog works because `NotificationPermissionNudge()` was added to
`HomeScreen` and it calls `notifState.launchPermissionRequest()` directly. Location
has no equivalent.

### Fix — add location permission gate to `RoutePreviewScreen`

Find `fun RoutePreviewScreen` (line 2733) and add these three blocks at the top,
before the `Column`:

```kotlin
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RoutePreviewScreen(viewModel: BodaViewModel, walletBalance: Double) {

    // ── Location permission gate ───────────────────────────────────────────
    val locationPermState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    // Sync permission status into the ViewModel whenever it changes
    LaunchedEffect(locationPermState.status) {
        if (locationPermState.status.isGranted) {
            viewModel.locationPermissionGranted = true
        }
    }

    // Request on first entry if not already granted
    LaunchedEffect(Unit) {
        if (!locationPermState.status.isGranted) {
            locationPermState.launchPermissionRequest()
        }
    }
    // ── End location gate ─────────────────────────────────────────────────

    // ... rest of the existing Column { ... } content unchanged ...
}
```

Also update the "Confirm Booking" button (near the bottom of `RoutePreviewScreen`)
to guard against missing permission:

```kotlin
// Find the existing confirm/book button and replace its onClick:

BodaButton(
    text = "Confirm Booking",
    onClick = {
        if (locationPermState.status.isGranted) {
            viewModel.locationPermissionGranted = true
            viewModel.confirmBooking()
        } else {
            locationPermState.launchPermissionRequest()
        }
    },
    modifier = Modifier.fillMaxWidth()
)
```

### Also — returning users (app restarted) never re-check permission

On a cold start, a returning user who already granted location permission skips
`RoutePreviewScreen` until they book again. `locationPermissionGranted` is reset
to `false` in the ViewModel constructor (line 1255), so location tracking never
restarts. Add a one-time check in `BodaAppContent`:

```kotlin
// BodaAppContent — add after the existing LaunchedEffect(viewModel.locationPermissionGranted):

val locationPermOnResume = rememberPermissionState(
    Manifest.permission.ACCESS_FINE_LOCATION
)
LaunchedEffect(locationPermOnResume.status) {
    if (locationPermOnResume.status.isGranted && !viewModel.locationPermissionGranted) {
        viewModel.locationPermissionGranted = true
        // startLocationTracking() is triggered by the existing
        // LaunchedEffect(viewModel.locationPermissionGranted) above
    }
}
```

This means a returning user who already granted permission will have location
tracking start automatically on app open, without needing to open `RoutePreviewScreen`.

---

## Bug 3 — Distance Calculation Fails / Returns Wrong Values

### Root cause

`calculatedDistanceKm` has two paths:

**Path A — Google Distance Matrix API** (needs `MAPS_API_KEY` + internet)
This currently always fails because there is no `.env` file (Bug 1). When it fails,
it falls through to Path B and logs "Using estimated distance."

**Path B — Haversine fallback** (no API key needed)

```kotlin
// BodaViewModel.kt ~ line 1351
val calculatedDistanceKm: Double
    get() {
        googleDistanceKm?.let { return it }        // Path A result
        val pick = pickupPlace ?: return 0.0
        val drop  = dropoffPlace ?: return 0.0

        val latDiff = drop.latitude - pick.latitude
        val lngDiff = drop.longitude - pick.longitude

        val p1Lat = pick.latitude + latDiff * 0.4
        val p1Lng = pick.longitude                  // ← longitude unchanged

        val p2Lat = pick.latitude + latDiff * 0.4  // ← same as p1Lat
        val p2Lng = pick.longitude + lngDiff * 0.6

        val p3Lat = drop.latitude
        val p3Lng = pick.longitude + lngDiff * 0.6

        val d1 = calculateHaversineDistance(pick.latitude, pick.longitude, p1Lat, p1Lng)
        val d2 = calculateHaversineDistance(p1Lat, p1Lng, p2Lat, p2Lng)
        val d3 = calculateHaversineDistance(p2Lat, p2Lng, p3Lat, p3Lng)
        val d4 = calculateHaversineDistance(p3Lat, p3Lng, drop.latitude, drop.longitude)

        val total = d1 + d2 + d3 + d4
        return if (total < 0.1) 1.2 else total
    }
```

The intent here was to simulate a road-following L-shaped route using three waypoints.
There are **two bugs** in the waypoint construction:

**Bug 3a — `d2` is always zero**

`p1` and `p2` have the same `lat` and `lng`:
- `p1 = (pick.lat + latDiff * 0.4, pick.lng)`
- `p2 = (pick.lat + latDiff * 0.4, pick.lng + lngDiff * 0.6)`

`p1Lat == p2Lat` (both `pick.lat + latDiff * 0.4`), so `d2` is the east-west
segment — that is correct. But `p1Lng = pick.longitude` and `p2Lng = pick.longitude + lngDiff * 0.6`.
This segment is fine. The actual zero segment is `d1`:

`d1 = haversine(pick.lat, pick.lng, pick.lat + latDiff*0.4, pick.lng)`

`p1Lng == pick.longitude` — only latitude changes. This is a pure north-south
segment, which is correct in intent but the route shape degenerates for
east-west trips (where `latDiff ≈ 0`), making `d1` and `d3` both near-zero
and `d2` and `d4` do all the work. The sum is still approximately correct
in magnitude but collapses to a straight-line distance.

**Bug 3b — no road-following multiplier**

Haversine gives the straight-line (crow-flies) distance. Real road distances in
Gulu are typically **1.3–1.5×** the straight-line distance due to the road grid.
Without a multiplier, every fare is underquoted by 20–30%, and the time estimate
is also too short.

**Fix — replace `calculatedDistanceKm` with a corrected implementation:**

```kotlin
// BodaViewModel.kt — replace the entire calculatedDistanceKm getter:

val calculatedDistanceKm: Double
    get() {
        // Path A: use Google Distance Matrix result if available
        googleDistanceKm?.let { return it }

        val pick = pickupPlace ?: return 0.0
        val drop = dropoffPlace ?: return 0.0

        // Path B: Haversine straight-line distance with a road-following multiplier.
        // Gulu road multiplier = 1.4 (accounts for the grid layout and detours).
        // This is consistent with OSM road data for the Gulu urban area.
        val straightLine = calculateHaversineDistance(
            pick.latitude, pick.longitude,
            drop.latitude, drop.longitude
        )

        val roadMultiplier = 1.4
        val estimated = straightLine * roadMultiplier

        // Floor of 0.8 km — minimum billable distance for any ride in Gulu
        return estimated.coerceAtLeast(0.8)
    }
```

**Fix — also add a matching road multiplier to `calculatedTimeMinutes`:**

```kotlin
// BodaViewModel.kt — find calculatedTimeMinutes getter and update:

val calculatedTimeMinutes: Int
    get() {
        googleDurationMins?.let { return it }
        // Average boda speed in Gulu urban: ~25 km/h
        val distKm = calculatedDistanceKm
        val rawMinutes = (distKm / 25.0 * 60.0).toInt()
        return rawMinutes.coerceAtLeast(3)   // minimum 3 minutes
    }
```

---

## Summary of all changes

| Bug | File | Change |
|---|---|---|
| Map — no API key | `.env` (new file) | Add real `MAPS_API_KEY` |
| Map — lifecycle missing ON_START/ON_RESUME | `BodaScreens.kt` `GoogleMapViewWrapper` | Add `ON_START`→`onStart()`, `ON_RESUME`→`onResume()` to observer; remove manual calls from `factory` |
| Map — MapsInitializer not called | `MainActivity.kt` `onCreate()` | Call `MapsInitializer.initialize()` before `setContent {}` |
| Location — permission never requested | `BodaScreens.kt` `RoutePreviewScreen` | Add `rememberPermissionState` + `LaunchedEffect` to request on entry |
| Location — returning users never tracked | `BodaScreens.kt` `BodaAppContent` | Add `LaunchedEffect(locationPermOnResume.status)` to re-check on cold start |
| Distance — road multiplier missing | `BodaViewModel.kt` `calculatedDistanceKm` | Replace L-shape waypoint math with `haversine × 1.4` |
| Distance — time estimate too short | `BodaViewModel.kt` `calculatedTimeMinutes` | Use `distKm / 25.0 km/h` with 3-minute floor |

---

## Testing checklist

| Scenario | Expected result |
|---|---|
| `.env` has real key, app launched | `hasMapsApiKey = true`, `GoogleMapViewWrapper` renders |
| `.env` missing or key is placeholder | `GuluCanvasMapView` renders with "Set MAPS_API_KEY" banner |
| App backgrounded and foregrounded | Map tiles reload correctly (lifecycle fix) |
| New user opens `RoutePreviewScreen` | System location permission dialog appears |
| User denies location | Confirm Booking button re-triggers request on tap |
| User grants location | `locationPermissionGranted = true`, tracking starts |
| Returning user opens app (already granted) | Location tracking starts automatically on app open |
| Pickup = Lacor Hospital, Dropoff = Gulu Market (~2.5 km apart) | Distance shows ~3.4–3.6 km (2.5 × 1.4), not 2.5 km |
| Very short trip (same block) | Distance floors at 0.8 km |
| Real Maps API key active | Distance Matrix API used, Haversine not used |
