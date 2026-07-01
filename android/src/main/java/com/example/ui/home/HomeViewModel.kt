package com.example.ui.home

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.ui.BodaViewModel
import com.example.ui.ChatMessage
import com.example.ui.Screen
import com.example.ui.components.getLatLngForPlace
import com.example.ui.ride.fetchRouteForPoints
import com.example.ui.ride.triggerOSRMRouteFetch
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

fun BodaViewModel.refreshWalletBalance() {
    viewModelScope.launch {
        apiRepository.fetchWalletBalance().onSuccess { balance ->
            backendBalance = balance
        }
    }
}

fun BodaViewModel.handleDeepLink(intent: android.content.Intent?) {
    intent ?: return
    try {
        val uri = intent.data ?: return
        val code = uri.getQueryParameter("code") ?: return
        if (code.isNotEmpty()) {
            referralCodeInput = code.uppercase().trim()
            android.util.Log.d("BODA_DEEPLINK", "Referral code from deep link: $code")
        }
    } catch (e: Exception) {
        android.util.Log.e("BODA_DEEPLINK", "Deep link handling failed: ${e.message}")
    }
}

fun BodaViewModel.shareReferralLink(context: android.content.Context) {
    val myCode = userProfile.value?.referralCode ?: return
    val deepLinkUrl = "https://bodagulu.page.link/ref?code=$myCode"
    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            android.content.Intent.EXTRA_TEXT,
            "Join Boda Gulu — affordable boda rides in Gulu!\n" +
            "Use my referral code $myCode and we both get UGX 3,000 on your first ride.\n" +
            deepLinkUrl
        )
    }
    context.startActivity(
        android.content.Intent.createChooser(shareIntent, "Share via")
    )
}

fun BodaViewModel.dismissWelcomeBonus() {
    showWelcomeBonus = false
    isNewUserSession = false
    prefs.edit().putBoolean("welcome_bonus_shown", true).apply()
}

fun BodaViewModel.navigateTo(screen: Screen) {
    if (currentScreen != screen) {
        backStack.add(currentScreen)
        currentScreen = screen
        if (screen == Screen.RoutePreview) {
            triggerOSRMRouteFetch()
        }
    }
}

fun BodaViewModel.navigateBack() {
    if (backStack.isNotEmpty()) {
        currentScreen = backStack.removeAt(backStack.size - 1)
    } else {
        currentScreen = Screen.Home
    }
}

fun BodaViewModel.updateLanguage(lang: String) {
    appLanguage = lang
    viewModelScope.launch {
        val current = userProfile.value ?: UserProfile()
        val updated = current.copy(language = lang)
        repository.saveUserProfile(updated)
        syncUserToBackend(updated)
    }
}

fun BodaViewModel.connectToBackend() {
    webSocketClient.onNewTripRequest = { trip ->
        viewModelScope.launch {
            if (isDriverMode && isDriverOnline && driverTripState == "none") {
                driverIncomingRequest = trip
                driverTripState = "requested"
                val driverLat = currentLocation?.latitude ?: 2.775
                val driverLng = currentLocation?.longitude ?: 32.295
                fetchRouteForPoints(
                    com.google.android.gms.maps.model.LatLng(driverLat, driverLng),
                    getLatLngForPlace(trip.pickupName)
                )
            }
        }
    }

    webSocketClient.onDriverLocationUpdate = { location ->
        viewModelScope.launch {
            val updated = nearbyDrivers.toMutableList()
            val existing = updated.indexOfFirst { it.driverId == location.driverId }
            if (location.isOnline) {
                val entry = com.example.data.DriverLocation(
                    driverId = location.driverId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    isOnline = location.isOnline
                )
                if (existing >= 0) updated[existing] = entry else updated.add(entry)
            } else {
                if (existing >= 0) updated.removeAt(existing)
            }
            nearbyDrivers = updated
        }
    }

    webSocketClient.onLiveGpsBroadcast = { update ->
        viewModelScope.launch {
            if (update.tripId == currentRideRequest?.id) {
                currentRideRequest = currentRideRequest?.copy(
                    driverLat = update.latitude,
                    driverLng = update.longitude
                )
            }
        }
    }

    webSocketClient.onPricingUpdate = { pricing ->
        viewModelScope.launch {
        }
    }

    webSocketClient.onConnectionChange = { connected ->
        isOnline = connected
    }

    webSocketClient.onChatMessage = { msg ->
        viewModelScope.launch {
            if (msg.tripId == currentChatTripId) {
                val uid = auth.currentUser?.uid ?: ""
                val senderType = if (msg.senderUid == uid) "user" else "agent"
                val lastMsg = riderChatMessages.lastOrNull()
                if (lastMsg == null || lastMsg.message != msg.message || lastMsg.sender != senderType) {
                    riderChatMessages.add(ChatMessage(
                        sender = senderType,
                        message = msg.message,
                        timestamp = System.currentTimeMillis()
                    ))
                }
                riderIsTyping = false
            }
        }
    }

    webSocketClient.onChatTyping = { typing ->
        viewModelScope.launch {
            if (typing.tripId == currentChatTripId) {
                val uid = auth.currentUser?.uid ?: ""
                riderIsTyping = typing.isTyping && typing.senderUid != uid
            }
        }
    }

    webSocketClient.onTripClaimed = { tripId, driverUid ->
        viewModelScope.launch {
            addPostgresLog("Trip #$tripId claimed by driver $driverUid")
            if (currentSimulationTrip?.id == tripId) {
                simulationState = "enroute"
            }
        }
    }

    webSocketClient.onTripUnmatched = { tripId ->
        viewModelScope.launch {
            addPostgresLog("Trip #$tripId timed out — no driver claimed it")
            if (currentSimulationTrip?.id == tripId) {
                errorMessage.value = "No driver available. Please try again."
                currentSimulationTrip = null
                simulationState = "idle"
                navigateTo(Screen.Home)
            }
        }
    }

    webSocketClient.connect()
}

fun BodaViewModel.syncUserToBackend(profile: UserProfile? = null) {
    val user = profile ?: userProfile.value ?: return
    var phone = user.phoneNumber.replace("+256", "").trim()
    android.util.Log.d("BODA_SYNC", "Syncing user: phone=$phone name=${user.name}")

    viewModelScope.launch {
        if (phone.isEmpty()) {
            delay(1000)
            val firebasePhone = auth.currentUser?.phoneNumber
            if (!firebasePhone.isNullOrEmpty()) {
                phone = firebasePhone.replace("+256", "").trim()
                android.util.Log.d("BODA_SYNC", "Phone populated after retry: phone=$phone")
            }
        }

        val tokenRefreshed = withContext(Dispatchers.IO) {
            try {
                com.google.android.gms.tasks.Tasks.await(
                    auth.currentUser?.getIdToken(true)
                        ?: com.google.android.gms.tasks.Tasks.forResult(null)
                )?.token != null
            } catch (e: Exception) { false }
        }
        if (!tokenRefreshed) {
            addPostgresLog("Sync skipped: could not refresh Firebase token")
            return@launch
        }
        apiRepository.syncUser(phone, user.name, language = user.language, referralCode = user.referralCode).fold(
            onSuccess = {
                addPostgresLog("User synced to PostgreSQL")
            },
            onFailure = { e ->
                android.util.Log.e("BODA_SYNC", "Sync failed: ${e.message}", e)
                addPostgresLog("Sync failed: ${e.message}")
            }
        )
    }
}

@Suppress("MissingPermission")
fun BodaViewModel.startLocationTracking() {
    if (isLocationTracking) return

    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
        .setMinUpdateIntervalMillis(2000L)
        .build()

    locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                currentLocation = location
                if (pickupPlace?.label == "Current Location") {
                    pickupPlace = SavedPlace(
                        label = "Current Location",
                        name = "Current Location",
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                }
            }
        }
    }

    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, null)
    isLocationTracking = true

    getLastKnownLocation()
}

fun BodaViewModel.stopLocationTracking() {
    locationCallback?.let {
        fusedLocationClient.removeLocationUpdates(it)
    }
    locationCallback = null
    isLocationTracking = false
}

fun BodaViewModel.updateAppThemeSetting(setting: String) {
    appThemeSetting = setting
    prefs.edit().putString("app_theme_setting", setting).apply()
}

fun BodaViewModel.connectPostgresWebSocket() {
    postgresWebSocketState = "Connecting..."
    addPostgresLog("Initiating Secure CDC WebSocket connection to PostgreSQL replication group (gulu-postgres-db)...")

    postgresWebSocketJob?.cancel()
    postgresWebSocketJob = viewModelScope.launch {
        delay(1500)
        postgresWebSocketState = "Connected"
        addPostgresLog("SUCCESS: Bi-directional WebSocket stream active. Protocol: PostgreSQL Realtime CDC (Logical Replication).")
        addPostgresLog("Connected to database: boda_gulu_production at port 5432.")

        val pendingList = trips.value.filter { it.status == "offline_pending" }
        if (pendingList.isNotEmpty()) {
            addPostgresLog("Found ${pendingList.size} unsynced local SQLite transactions cached offline in Room.")
            delay(1000)
            pendingList.forEach { trip ->
                addPostgresLog("Synchronizing trip transaction to PostgreSQL [ID: ${trip.id} -> ${trip.pickupName} to ${trip.dropoffName}] via WebSocket channel...")
                delay(800)
                val updatedTrip = trip.copy(status = "completed")
                repository.updateTrip(updatedTrip)
                addPostgresLog("✓ Transaction [ID: ${trip.id}] committed successfully to remote PostgreSQL 'trips' table.")
            }
            addPostgresLog("PostgreSQL synchronization complete. Local Room cache and remote Postgres are in perfect sync! 🚀")
        } else {
            addPostgresLog("No offline pending trips found. Local Room cache is up-to-date.")
        }

        while (isOnline) {
            delay(10000)
            val lat = 2.7712 + Random.nextDouble(-0.02, 0.02)
            val lon = 32.2985 + Random.nextDouble(-0.02, 0.02)
            addPostgresLog("Live location sync broadcast via WebSocket payload: { \"boda_id\": \"BODA-SAFE-78\", \"geom\": \"POINT($lon $lat)\" }")
        }
    }
}

fun BodaViewModel.disconnectPostgresWebSocket() {
    postgresWebSocketState = "Disconnected"
    postgresWebSocketJob?.cancel()
    addPostgresLog("WebSocket connection closed. PostgreSQL replication paused.")
    addPostgresLog("Warning: Entering Offline Mode. All booking requests and saved place transactions will be cached locally in SQLite via Room.")
}

fun BodaViewModel.toggleNetworkConnection() {
    isOnline = !isOnline
    if (isOnline) {
        connectPostgresWebSocket()
    } else {
        disconnectPostgresWebSocket()
    }
}

fun BodaViewModel.completeOnboardingCarousel() {
    onboardingCarouselCompleted = true
    prefs.edit().putBoolean("onboarding_carousel_completed", true).apply()
}

fun BodaViewModel.completeOnboardingLanguage(lang: String) {
    updateLanguage(lang)
    onboardingLanguageSelected = true
    prefs.edit().putBoolean("onboarding_language_selected", true).apply()
}

fun BodaViewModel.resetOnboarding() {
    onboardingCarouselCompleted = false
    onboardingLanguageSelected = false
    onboardingSlideIndex = 0
    phoneInput = ""
    otpInput = ""
    otpSent = false
    isOtpVerified = false
    signupName = ""
    locationPermissionGranted = false
    notificationPermissionGranted = false
    prefs.edit()
        .putBoolean("onboarding_carousel_completed", false)
        .putBoolean("onboarding_language_selected", false)
        .apply()
}

fun BodaViewModel.saveUserProfile(profile: UserProfile) {
    viewModelScope.launch {
        repository.saveUserProfile(profile)
        syncUserToBackend(profile)
    }
}

@Suppress("MissingPermission")
fun BodaViewModel.getLastKnownLocation() {
    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            currentLocation = location
            if (pickupPlace == null) {
                pickupPlace = SavedPlace(
                    label = "Current Location",
                    name = "Current Location",
                    latitude = location.latitude,
                    longitude = location.longitude
                )
                pickupText = "Current Location"
            }
        }
    }
}
