# RydLink Android — UI Audit & Fix Plan

---

## The Core Problem in One Sentence

The app has **580 hardcoded hex color values** across its UI files and **zero references to `MaterialTheme.colorScheme`**. It has a Material 3 theme set up in `Theme.kt` that is never actually used by any screen.

---

## Issue 1 — Dual Color System (Critical)

### What's happening
`Theme.kt` defines a proper Material 3 color scheme with `lightColorScheme` / `darkColorScheme`. But every screen ignores it entirely and instead uses a parallel system:

- `object Color` in `DesignSystem.kt` — a custom delegator that manually maps dark hex values to light equivalents
- Raw `Color(0xFF...)` hex literals scattered directly in composables — **580 occurrences**, 27 unique values

The `object Color` system is a workaround built on top of a global mutable `var isAppInDarkMode` — this is an anti-pattern in Compose. It breaks recomposition guarantees and bypasses the Material theme entirely.

### The fix
Delete `object Color` in `DesignSystem.kt`. Delete the `isAppInDarkMode` global var. Map every hardcoded hex to its equivalent Material 3 color token:

| Hardcoded value | Role | Material 3 token |
|---|---|---|
| `0xFF0F172A` | Background (dark) | `MaterialTheme.colorScheme.background` |
| `0xFF1E293B` | Card/surface (dark) | `MaterialTheme.colorScheme.surface` |
| `0xFF334155` | Border/divider | `MaterialTheme.colorScheme.surfaceVariant` |
| `0xFFFDB913` | Primary action (yellow) | `MaterialTheme.colorScheme.primary` |
| `0xFF0061A4` | Secondary (blue) | `MaterialTheme.colorScheme.secondary` |
| `0xFF64748B` | Muted text | `MaterialTheme.colorScheme.onSurfaceVariant` |
| `0xFF94A3B8` | Placeholder text | `MaterialTheme.colorScheme.outline` |
| `0xFF10B981` | Success/online green | `MaterialTheme.colorScheme.tertiary` |
| `0xFFE4002B` | Error/danger | `MaterialTheme.colorScheme.error` |
| `Color.White` (text) | Primary text | `MaterialTheme.colorScheme.onBackground` |
| `Color.Black` (on yellow) | Text on primary | `MaterialTheme.colorScheme.onPrimary` |

After this change, dark/light mode works automatically — no `isAppInDarkMode` tracking needed.

---

## Issue 2 — Dynamic Color is Disabled

### What's happening
```kotlin
// Theme.kt
dynamicColor: Boolean = false, // Disabled for performance optimization on budget devices
```

This comment is incorrect. Dynamic Color (Material You) has no meaningful performance overhead — it simply reads the device wallpaper palette at startup once. Budget Android devices on Android 12+ support it fine.

### The fix
```kotlin
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
```

On Android 12+ (API 31+), users get colors derived from their wallpaper. On older devices, they get your brand palette. This is exactly what Material You is designed to do.

---

## Issue 3 — BodaCard Ignores the Theme

### What's happening
```kotlin
// BodaCard.kt
Card(
    colors = CardDefaults.cardColors(
        containerColor = Color(0xFF1E293B)  // hardcoded dark slate
    ),
    border = border ?: BorderStroke(1.dp, Color(0xFF334155))  // hardcoded border
)
```

In light mode, this card is still dark — the light theme has no effect on it.

### The fix
```kotlin
Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    ),
    border = border ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
)
```

---

## Issue 4 — BodaButton Hardcodes Primary Color

### What's happening
```kotlin
containerColor: ComposeColor = Color(0xFFFDB913),  // hardcoded yellow
contentColor: ComposeColor = ComposeColor.Black,
```

If dynamic color is enabled and the user's wallpaper generates a different primary color, this button stays yellow regardless.

### The fix
```kotlin
containerColor: ComposeColor = MaterialTheme.colorScheme.primary,
contentColor: ComposeColor = MaterialTheme.colorScheme.onPrimary,
```

---

## Issue 5 — NavigationBar Ignores Theme

### What's happening
```kotlin
NavigationBar(
    containerColor = Color(0xFF0F172A),  // hardcoded dark
)
NavigationBarItem(
    colors = NavigationBarItemDefaults.colors(
        selectedIconColor = Color.Black,
        indicatorColor = Color(0xFFFDB913),  // hardcoded yellow
        unselectedIconColor = Color.White,
    )
)
```

### The fix — use defaults, they are already correct:
```kotlin
NavigationBar(
    // No containerColor — defaults to MaterialTheme.colorScheme.surfaceContainer
) {
    NavigationBarItem(
        // No custom colors — M3 defaults are correct:
        // indicator = primary, selected = onSecondaryContainer, unselected = onSurfaceVariant
    )
}
```

---

## Issue 6 — HomeScreen Uses Raw Hex in Inline Composables

### What's happening
The bottom sheet card in `PassengerHomeScreen`:
```kotlin
Card(
    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
)
```

The segment tab control:
```kotlin
.background(Color(0xFF1E293B))
// active tab:
.background(if (isRide) Color(0xFFFDB913) else Color.Transparent)
```

Every interactive element has its own hardcoded color scheme that doesn't respond to theme changes.

### The fix — replace with token references:
```kotlin
Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface))

// Tab control:
.background(MaterialTheme.colorScheme.surfaceVariant)
// active:
.background(if (isRide) MaterialTheme.colorScheme.primary else Color.Transparent)
// active text:
color = if (isRide) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
```

---

## Issue 7 — Typography Is Defined But Inconsistently Used

### What's happening
`Type.kt` defines a complete Nunito type scale: `displayLarge`, `headlineLarge`, `titleLarge`, `bodyMedium`, etc. But screens use raw `fontSize = 12.sp`, `fontSize = 14.sp` etc. inline everywhere instead of `MaterialTheme.typography.bodyMedium`.

Examples:
```kotlin
Text("Status", color = ..., fontSize = 11.sp)  // should be labelMedium
Text("Active Trip", fontWeight = FontWeight.Bold, fontSize = 14.sp)  // should be titleMedium
Text(greeting, fontSize = 10.sp)  // should be labelSmall
```

### The fix — use the type scale consistently:
```kotlin
Text("Status", style = MaterialTheme.typography.labelMedium)
Text("Active Trip", style = MaterialTheme.typography.titleMedium)
Text(greeting, style = MaterialTheme.typography.labelSmall)
```

---

## Issue 8 — Location is Always Stuck (the Bug You Reported)

### Root cause
There are three layers of the problem:

**Layer 1 — `getLastKnownLocation()` is called but never defined.**
In `HomeViewModel.kt` line 261, `getLastKnownLocation()` is called inside `startLocationTracking()` but the function doesn't exist anywhere in the codebase. This is a silent no-op — no crash, but location is never seeded from the last known position.

**Layer 2 — `currentLocation` never flows into `pickupPlace`.**
Even when `locationCallback` fires and updates `currentLocation` (the `Location` object), nothing converts it into `pickupPlace`. The `pickupPlace` shown on the map and bottom sheet stays `null` or whatever it was last set to manually.

**Layer 3 — The "Current Location" button is hardcoded.**
In `SearchPlacesScreen.kt` line 148:
```kotlin
val currentLocation = SavedPlace(
    label = "Current Location",
    name = "My Current Location (Pece, Gulu)",
    latitude = 2.7750,    // ← hardcoded Pece coordinates
    longitude = 32.2950   // ← never uses the actual GPS fix
)
```
This is why the pin never moves — even when the user taps "Current Location", it places a hardcoded point at Pece roundabout, ignoring whatever `fusedLocationClient` returned.

### The fix

**Step 1 — Add the missing `getLastKnownLocation` function to `HomeViewModel.kt`:**
```kotlin
@SuppressLint("MissingPermission")
fun BodaViewModel.getLastKnownLocation() {
    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            currentLocation = location
            // Auto-set pickup to real GPS position
            pickupPlace = SavedPlace(
                label = "My Location",
                name = "Current Location",
                latitude = location.latitude,
                longitude = location.longitude
            )
            pickupText = "Current Location"
        }
    }
}
```

**Step 2 — Fix the "Current Location" button in `SearchPlacesScreen.kt`:**
```kotlin
val loc = viewModel.currentLocation
val currentLocationPlace = if (loc != null) {
    SavedPlace(
        label = "Current Location",
        name = "Current Location",
        latitude = loc.latitude,
        longitude = loc.longitude
    )
} else {
    // fallback only when GPS not yet available
    SavedPlace(
        label = "Current Location",
        name = "Gulu City Centre",
        latitude = 2.7750,
        longitude = 32.2950
    )
}
// Then use currentLocationPlace instead of the hardcoded SavedPlace
```

**Step 3 — Also update `LocationCallback` to auto-refresh pickup if it's still the default:**
```kotlin
locationCallback = object : LocationCallback() {
    override fun onLocationResult(result: LocationResult) {
        result.lastLocation?.let { location ->
            currentLocation = location
            // If pickup hasn't been set by user yet, keep it updated
            if (pickupPlace == null || pickupPlace?.label == "My Location") {
                pickupPlace = SavedPlace(
                    label = "My Location",
                    name = "Current Location",
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            }
        }
    }
}
```

---

## Issue 9 — Map Camera Never Follows the User

### What's happening
`GoogleMapViewWrapper` animates the camera to `pickupLatLng` on update — but `pickupLatLng` on the home screen defaults to `LatLng(2.775, 32.295)` (hardcoded Gulu centre) when `pickup` is null. So the map opens centered on that fixed point regardless of where the user actually is.

### The fix
Pass `viewModel.currentLocation` into `GuluMapView`:
```kotlin
GuluMapView(
    modifier = Modifier.fillMaxSize(),
    pickup = viewModel.pickupPlace,
    dropoff = viewModel.dropoffPlace,
    userLocation = viewModel.currentLocation?.let {
        LatLng(it.latitude, it.longitude)
    },
    viewModel = viewModel
)
```

And in `GoogleMapViewWrapper`, when there's no pickup, center on `userLocation`:
```kotlin
val cameraTarget = userLocation ?: pickupLatLng
googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(cameraTarget, 15f))
```

Also enable the native blue dot:
```kotlin
googleMap.isMyLocationEnabled = true  // requires permission check before this line
googleMap.uiSettings.isMyLocationButtonEnabled = true
```

---

## Issue 10 — Missing Material 3 Component Upgrades

Several places use raw `Box + clickable` where a proper M3 component would give ripple, accessibility, and theming for free:

| Current | Should be |
|---|---|
| `Box + clickable + background(yellow)` for segment tabs | `SingleChoiceSegmentedButtonRow` (M3) |
| `Box + clip + background` for the avatar | `FilledIconButton` or `IconButton` |
| `Card + Column + clickable` for the pickup/dropoff rows | `ListItem` (M3 component) |
| Manual `Row + Icon + Text` for nav items | Already using `NavigationBarItem` ✓ |
| `Switch` with manual thumb color | `Switch` with `SwitchDefaults.colors()` from theme ✓ |

The most impactful one: replace the ride/delivery tab toggle with `SingleChoiceSegmentedButtonRow`:
```kotlin
SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
    SegmentedButton(
        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        onClick = { viewModel.serviceType = "ride" },
        selected = viewModel.serviceType == "ride",
        icon = { Icon(Icons.Default.TwoWheeler, contentDescription = null) }
    ) { Text("Ride") }
    SegmentedButton(
        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        onClick = { viewModel.serviceType = "delivery" },
        selected = viewModel.serviceType == "delivery",
        icon = { Icon(Icons.Default.LocalShipping, contentDescription = null) }
    ) { Text("Delivery") }
}
```

---

## Issue 11 — The `object Color` Shadow Hides Compose's Color

### What's happening
```kotlin
import androidx.compose.ui.graphics.Color as ComposeColor
// ...
object Color {
    operator fun invoke(value: Long): ComposeColor { ... }
}
```

And in every screen file:
```kotlin
import com.example.ui.components.Color
```

This means `Color(0xFF...)` looks like Compose's `Color()` but it's actually the custom delegator. Any developer (or AI agent) reading the code can't tell at a glance which Color system is being used. It makes debugging theme issues very hard.

### The fix
Delete `object Color`. Remove all imports of it. Replace all usages with direct `MaterialTheme.colorScheme.*` references. This is a prerequisite for Issues 1–6 above.

---

## Issue 12 — `Scaffold` Is Not Used

Every screen builds its own layout with a `Column` or `Box`, manually placing a bottom nav bar and content. None of them use `Scaffold`. This means:

- Window insets are handled inconsistently (some screens use `WindowInsets.navigationBars`, some don't)
- The bottom navigation bar overlaps content on some screens
- Status bar content protection varies screen to screen

### The fix
In `AppNavigation.kt`, wrap the nav host in a `Scaffold`:
```kotlin
Scaffold(
    bottomBar = {
        if (showBottomNav) BodaBottomNavigation(viewModel)
    },
    contentWindowInsets = WindowInsets.safeDrawing
) { innerPadding ->
    NavHost(
        modifier = Modifier.padding(innerPadding),
        // ...
    )
}
```

Control `showBottomNav` based on the current route — hide it on screens like ActiveTrip, Matching, SearchPlaces.

---

## Advice for This Type of Application

**1. Map-first layout is the right call, don't abandon it.**
Showing the map full-screen with a bottom sheet overlay is exactly how Uber, Bolt, and inDrive are built. Your current `Box` with map behind and `Card` anchored at the bottom is architecturally correct. The issue is purely theming.

**2. Prioritise perceived performance over actual performance.**
On budget Android devices in Gulu, the user will notice layout jank immediately. Avoid `verticalScroll` on screens that don't need it (most of them don't). The home screen bottom sheet is 340dp max-height — it doesn't need to scroll. Remove the `rememberScrollState` there.

**3. The canvas map fallback is valuable — make it look better.**
`GuluCanvasMapView` is what most users will see since Maps API keys are expensive. Invest in making it look clean: a proper road grid, landmark labels, a pulsing user location dot. Right now it's functional but looks like a debug overlay.

**4. Keep the Acholi/Luganda localisation — it's a real differentiator.**
`BodaLang` is a strong feature. Make sure it extends to accessibility labels (`contentDescription` on icons) and error messages, not just visible UI strings.

**5. The ViewModel is still doing too much.**
`BodaViewModel` at 634 lines is the coordinator — that's acceptable — but it still holds UI state like `serviceType`, `pickupText`, `dropoffText` that belong in the ride feature's own state holder. This will cause unnecessary recompositions on the home screen when a trip updates.

**6. Add `LocalContentColor` and `LocalTextStyle` providers.**
Instead of setting `color = MaterialTheme.colorScheme.onBackground` on every `Text`, wrap screens in:
```kotlin
CompositionLocalProvider(
    LocalContentColor provides MaterialTheme.colorScheme.onBackground
) {
    // all Text composables inside inherit this automatically
}
```

**7. Consider `ModalBottomSheet` for the home bottom sheet.**
The bottom sheet card is currently a `Card` pinned with `Alignment.BottomCenter`. The proper M3 component is `ModalBottomSheet` or `BottomSheetScaffold` — it gives you drag-to-expand, peek height control, and the handle indicator built-in.

---

## Priority Order for Fixes

1. **Fix location bug** (Issues 8 + 9) — functional, immediate user impact
2. **Delete `object Color`, use `MaterialTheme.colorScheme`** (Issues 1, 3, 4, 5, 6, 11) — do this in one sweep, not piecemeal
3. **Enable dynamic color** (Issue 2) — one-line change, big visual impact on Android 12+
4. **Add Scaffold** (Issue 12) — fixes inset inconsistencies across all screens at once
5. **Typography tokens** (Issue 7) — replace inline `fontSize` with `MaterialTheme.typography.*`
6. **M3 component upgrades** (Issue 10) — segmented button, ListItem, etc.
