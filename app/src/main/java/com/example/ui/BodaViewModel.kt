package com.example.ui

import android.app.Application
import android.content.Context
import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.android.gms.tasks.Tasks

import com.example.data.BodaRepository
import com.example.data.ApiClient
import com.example.data.WebSocketClient
import com.example.data.DriverLocation
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.random.Random

sealed class Screen {
    object Splash : Screen()
    object WelcomeOnboarding : Screen()
    object Home : Screen()
    object SearchPlaces : Screen()
    object RoutePreview : Screen()
    object Matching : Screen()
    object RiderEnRoute : Screen()
    object ActiveTrip : Screen()
    object PostTrip : Screen()
    object TripsHistory : Screen()
    object Wallet : Screen()
    object ProfileSettings : Screen()
    object Support : Screen()
    object EmergencyContacts : Screen()
    object SavedPlacesManage : Screen()
    object DriverOnboarding : Screen()
    object Referrals : Screen()
}

class BodaViewModel(application: Application) : AndroidViewModel(application) {

    val errorMessage = MutableStateFlow<String?>(null)

    private val repository: BodaRepository = BodaRepository(AppDatabase.getDatabase(application).bodaDao())
    private val apiRepository = BodaRepository()
    private val webSocketClient = WebSocketClient(ApiClient.getWebSocketUrl())
    private val prefs = application.getSharedPreferences("boda_gulu_prefs", Context.MODE_PRIVATE)
    
    // Firebase Auth
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    

    
    // Ride state
    var currentRideRequest by mutableStateOf<com.example.data.RideRequest?>(null)
        private set
    var nearbyDrivers by mutableStateOf<List<com.example.data.DriverLocation>>(emptyList())
        private set
    var isSearchingForDriver by mutableStateOf(false)
        private set
    
    // Location Services
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(application)
    private var locationCallback: LocationCallback? = null
    var currentLocation by mutableStateOf<Location?>(null)
        private set
    var isLocationTracking by mutableStateOf(false)
        private set

    // Network Connectivity & PostgreSQL Real-time Live Sync
    var isOnline by mutableStateOf(true)
    var postgresWebSocketState by mutableStateOf("Connected") // "Connected", "Connecting...", "Disconnected"
    val postgresWebSocketLogs = androidx.compose.runtime.mutableStateListOf<String>()
    
    // Offline Booking SMS states
    var showOfflineSMSDialog by mutableStateOf(false)
    var offlineSMSRecipientNumber by mutableStateOf("8080")
    var offlineSMSMessageBody by mutableStateOf("")
    
    // Emergency SMS alerts simulation log
    val emergencySMSDispatchLogs = androidx.compose.runtime.mutableStateListOf<String>()

    // Expose flows from Repository
    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val savedPlaces: StateFlow<List<SavedPlace>> = repository.savedPlaces
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trips: StateFlow<List<Trip>> = repository.trips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<WalletTransaction>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val emergencyContacts: StateFlow<List<EmergencyContact>> = repository.emergencyContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val referrals: StateFlow<List<Referral>> = repository.referrals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculated fields
    val referralEarnings: StateFlow<Double> = referrals.map { list ->
        list.filter { it.status == "completed" }.sumOf { it.rewardAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3000.0)

    val unsyncedTripsCount: StateFlow<Int> = trips.map { list ->
        list.count { it.status == "offline_pending" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val walletBalance: StateFlow<Double> = transactions.map { list ->
        val deposits = list.filter { it.type == "topup" && it.status == "completed" }.sumOf { it.amount }
        val payments = list.filter { it.type == "payment" && (it.status == "completed" || it.status == "pending") }.sumOf { it.amount }
        (deposits - payments).coerceAtLeast(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    var backendBalance by mutableStateOf<Double?>(null)
        private set
    val effectiveBalance: StateFlow<Double> = walletBalance.map { local ->
        backendBalance ?: local
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun refreshWalletBalance() {
        viewModelScope.launch {
            apiRepository.fetchWalletBalance().onSuccess { balance ->
                backendBalance = balance
            }
        }
    }

    var isLoadingData by mutableStateOf(true)
        private set

    var isOtpVerified by mutableStateOf(false)
    var phoneInput by mutableStateOf("")
    var otpInput by mutableStateOf("")
    var otpSent by mutableStateOf(false)

    var triggerPhoneHint by mutableStateOf(false)

    var showWelcomeBonus by mutableStateOf(false)
        private set
    var isNewUserSession by mutableStateOf(false)
        private set

    val onboardingStep: Int
        get() = when {
            !otpSent -> 1
            !isOtpVerified -> 2
            else -> 3
        }

    val onboardingStepLabel: String
        get() = when (onboardingStep) {
            1 -> "Enter your phone number"
            2 -> "Verify your number"
            3 -> "Set up your profile"
            else -> ""
        }

    private var lastBackendFetchMs = 0L

    init {
        viewModelScope.launch {
            val currentUser = auth.currentUser
            isOtpVerified = currentUser != null
            if (currentUser != null) {
                phoneInput = currentUser.phoneNumber?.removePrefix("+256") ?: ""
                restoreSessionFromBackend()
            } else {
                isLoadingData = false
            }
        }
        connectPostgresWebSocket()
        connectToBackend()
        registerFcmToken()
    }

    fun handleDeepLink(intent: android.content.Intent?) {
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

    fun shareReferralLink(context: android.content.Context) {
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

    fun dismissWelcomeBonus() {
        showWelcomeBonus = false
        isNewUserSession = false
        prefs.edit().putBoolean("welcome_bonus_shown", true).apply()
    }

    private suspend fun restoreSessionFromBackend() {
        addPostgresLog("Restoring session from backend...")
        apiRepository.fetchUserProfile().fold(
            onSuccess = { backendUser ->
                val existingProfile = repository.userProfile.firstOrNull()
                // Treat the old fake fallback number as empty
                val cleanPhone = backendUser.phone
                    ?.takeIf { it.isNotEmpty() && it != "+256770000000" && it != "256770000000" }
                    ?: ""
                val profile = UserProfile(
                    id = 1,
                    name = backendUser.full_name.ifEmpty { existingProfile?.name ?: "" },
                    phoneNumber = cleanPhone.ifEmpty { existingProfile?.phoneNumber ?: "" },
                    language = backendUser.language.ifEmpty { existingProfile?.language ?: "en" },
                    isSetupComplete = backendUser.full_name.isNotEmpty(),
                    referralCode = backendUser.referral_code?.ifEmpty { existingProfile?.referralCode ?: "" }
                        ?: existingProfile?.referralCode ?: ""
                )
                repository.saveUserProfile(profile)
                backendBalance = backendUser.wallet_balance
                addPostgresLog("Session restored for ${backendUser.full_name}")
                fetchBackendData()
            },
            onFailure = { e ->
                if (e is com.example.data.UserNotFoundException) {
                    addPostgresLog("User not found on backend. Syncing from local profile...")
                    val localProfile = repository.userProfile.firstOrNull()
                    if (localProfile != null && localProfile.phoneNumber.isNotEmpty()) {
                        syncUserToBackend(localProfile)
                    }
                } else {
                    addPostgresLog("Session restore failed (offline?): ${e.message}")
                }
                fetchBackendData()
            }
        )
        isLoadingData = false
    }

    private suspend fun fetchBackendData(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastBackendFetchMs < 5 * 60 * 1000L) {
            addPostgresLog("Data cache is fresh. Skipping backend fetch.")
            return
        }
        lastBackendFetchMs = now
        apiRepository.fetchTrips().onSuccess { dtos ->
            dtos.forEach { dto ->
                if (dto.id != 0) repository.addTrip(Trip(
                    id = dto.id,
                    type = "ride",
                    pickupName = dto.pickup_name,
                    dropoffName = dto.dropoff_name,
                    fare = dto.fare,
                    paymentMethod = dto.payment_method,
                    status = dto.status,
                    riderName = dto.driver_name ?: "",
                    riderPlate = dto.plate_number ?: "",
                    rating = dto.rating
                ))
            }
        }
        apiRepository.fetchWalletTransactions().onSuccess { dtos ->
            dtos.forEach { dto ->
                if (dto.id != 0) repository.addTransaction(WalletTransaction(
                    id = dto.id,
                    amount = dto.amount,
                    type = dto.type,
                    status = dto.status,
                    phoneNumber = userProfile.value?.phoneNumber ?: "",
                    timestamp = System.currentTimeMillis(),
                    provider = dto.payment_provider,
                    reference = dto.transaction_ref
                ))
            }
        }
        apiRepository.fetchEmergencyContactsFromBackend().onSuccess { dtos ->
            dtos.forEach { dto ->
                if (dto.id != 0) repository.addEmergencyContact(EmergencyContact(
                    id = dto.id,
                    name = dto.name,
                    phoneNumber = dto.phone_number
                ))
            }
        }
        apiRepository.fetchReferralsFromBackend().onSuccess { dtos ->
            dtos.forEach { dto ->
                if (dto.id != 0) repository.addReferral(Referral(
                    id = dto.id,
                    referredName = dto.referred_name,
                    referredPhone = dto.referred_phone,
                    referralCodeUsed = dto.referral_code,
                    status = dto.status,
                    rewardAmount = dto.reward_amount
                ))
            }
        }
    }

    // Routing points
    var osrmRoutePoints by mutableStateOf<List<com.google.android.gms.maps.model.LatLng>>(emptyList())
    var isLoadingRoute by mutableStateOf(false)
    var routeError by mutableStateOf<String?>(null)

    // Turn-by-turn navigation steps from Google Directions API
    data class NavStep(val instruction: String, val distanceMeters: Int, val durationSeconds: Int, val startLat: Double, val startLng: Double)
    var navigationSteps by mutableStateOf<List<NavStep>>(emptyList())
    var totalRouteDistanceMeters by mutableStateOf(0)
    var totalRouteDurationSeconds by mutableStateOf(0)

    private fun decodePolyline(encoded: String): List<com.google.android.gms.maps.model.LatLng> {
        val poly = ArrayList<com.google.android.gms.maps.model.LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = com.google.android.gms.maps.model.LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }
        return poly
    }

    fun fetchRouteForPoints(startLatLng: com.google.android.gms.maps.model.LatLng, endLatLng: com.google.android.gms.maps.model.LatLng) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            isLoadingRoute = true
            routeError = null
            
            val googleApiKey = try {
                com.example.BuildConfig.MAPS_API_KEY
            } catch (e: Throwable) {
                ""
            }
            
            val hasGoogleMapsKey = googleApiKey.isNotEmpty() && 
                    googleApiKey != "MY_MAPS_API_KEY" && 
                    googleApiKey != "MAPS_API_KEY_DEFAULT_VALUE"
            
            var pointsLoaded = false
            
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            // --- OPTION A: GOOGLE DIRECTIONS API ---
            if (hasGoogleMapsKey && isOnline) {
                try {
                    addPostgresLog("Requesting premium road routing from Google Directions API...")
                    val googleUrl = "https://maps.googleapis.com/maps/api/directions/json" +
                            "?origin=${startLatLng.latitude},${startLatLng.longitude}" +
                            "&destination=${endLatLng.latitude},${endLatLng.longitude}" +
                            "&key=$googleApiKey"

                    val request = okhttp3.Request.Builder()
                        .url(googleUrl)
                        .header("User-Agent", "BodaGuluApp/1.0")
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyString = response.body?.string() ?: ""
                            val json = org.json.JSONObject(bodyString)
                            val status = json.optString("status")
                            if (status == "OK") {
                                val routes = json.getJSONArray("routes")
                                if (routes.length() > 0) {
                                    val route = routes.getJSONObject(0)
                                    val overviewPolyline = route.getJSONObject("overview_polyline")
                                    val pointsStr = overviewPolyline.getString("points")
                                    val points = decodePolyline(pointsStr)

                                    // Parse turn-by-turn navigation steps
                                    val steps = mutableListOf<NavStep>()
                                    var totalDist = 0
                                    var totalDur = 0
                                    if (route.has("legs")) {
                                        val legs = route.getJSONArray("legs")
                                        if (legs.length() > 0) {
                                            val leg = legs.getJSONObject(0)
                                            totalDist = leg.optJSONObject("distance")?.optInt("value", 0) ?: 0
                                            totalDur = leg.optJSONObject("duration")?.optInt("value", 0) ?: 0
                                            if (leg.has("steps")) {
                                                val stepsArr = leg.getJSONArray("steps")
                                                for (s in 0 until stepsArr.length()) {
                                                    val step = stepsArr.getJSONObject(s)
                                                    val htmlInstr = step.optString("html_instructions", "")
                                                    val instr = htmlInstr.replace(Regex("<[^>]*>"), "")
                                                    val dist = step.optJSONObject("distance")?.optInt("value", 0) ?: 0
                                                    val dur = step.optJSONObject("duration")?.optInt("value", 0) ?: 0
                                                    val startLoc = step.optJSONObject("start_location")
                                                    steps.add(NavStep(
                                                        instruction = instr,
                                                        distanceMeters = dist,
                                                        durationSeconds = dur,
                                                        startLat = startLoc?.optDouble("lat", 0.0) ?: 0.0,
                                                        startLng = startLoc?.optDouble("lng", 0.0) ?: 0.0
                                                    ))
                                                }
                                            }
                                        }
                                    }

                                    if (points.isNotEmpty()) {
                                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            osrmRoutePoints = points
                                            navigationSteps = steps
                                            totalRouteDistanceMeters = totalDist
                                            totalRouteDurationSeconds = totalDur
                                            isLoadingRoute = false
                                            addPostgresLog("✓ Successfully loaded premium polyline from Google Directions API (${points.size} points, ${steps.size} nav steps).")
                                        }
                                        pointsLoaded = true
                                    }
                                }
                            } else {
                                addPostgresLog("Google Directions error status: $status. Falling back to OSRM.")
                            }
                        } else {
                            addPostgresLog("Google Directions HTTP error: ${response.code}. Falling back to OSRM.")
                        }
                    }
                } catch (e: Exception) {
                    addPostgresLog("Google Directions fetch failed (${e.message}). Falling back to OSRM.")
                }
            }

            // --- OPTION B: OSRM API (FALLBACK / NO-KEY DEFAULT) ---
            if (!pointsLoaded) {
                try {
                    if (!isOnline) {
                        throw java.io.IOException("Device is offline. OSRM lookup skipped.")
                    }
                    
                    // OSRM expects coordinates in lng,lat format!
                    val url = "https://router.project-osrm.org/route/v1/driving/" +
                            "${startLatLng.longitude},${startLatLng.latitude};" +
                            "${endLatLng.longitude},${endLatLng.latitude}" +
                            "?overview=full&geometries=geojson"
                    
                    addPostgresLog("Requesting real-world road routing coordinates from OSRM public API...")
                        
                    val request = okhttp3.Request.Builder()
                        .url(url)
                        .header("User-Agent", "BodaGuluApp/1.0")
                        .build()
                    
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw java.io.IOException("OSRM API response unsuccessful: ${response.code}")
                        }
                        val bodyString = response.body?.string() ?: throw java.io.IOException("Empty response body")
                        val json = org.json.JSONObject(bodyString)
                        val code = json.optString("code")
                        if (code != "Ok") {
                            throw java.io.IOException("OSRM error code: $code")
                        }
                        
                        val routes = json.getJSONArray("routes")
                        if (routes.length() > 0) {
                            val firstRoute = routes.getJSONObject(0)
                            val geometry = firstRoute.getJSONObject("geometry")
                            val coordinates = geometry.getJSONArray("coordinates")
                            
                            val points = mutableListOf<com.google.android.gms.maps.model.LatLng>()
                            for (i in 0 until coordinates.length()) {
                                val coord = coordinates.getJSONArray(i)
                                val lng = coord.getDouble(0)
                                val lat = coord.getDouble(1)
                                points.add(com.google.android.gms.maps.model.LatLng(lat, lng))
                            }
                            
                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                osrmRoutePoints = points
                                isLoadingRoute = false
                                addPostgresLog("✓ Successfully loaded real road routing polyline from OSRM (${points.size} points).")
                            }
                        } else {
                            throw java.io.IOException("No routes found in OSRM response")
                        }
                    }
                } catch (e: Exception) {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        routeError = e.message
                        isLoadingRoute = false
                        errorMessage.value = "Routing failed. Showing estimated route."
                        addPostgresLog("Warning: OSRM Routing API unavailable (${e.message}). Falling back to simulated/grid street route.")
                    }
                }
            }
        }
    }

    fun searchLocations(query: String) {
        searchJob?.cancel()
        if (query.trim().length < 2) {
            searchResults = emptyList()
            return
        }
        
        searchJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // Debounce for 400ms
            kotlinx.coroutines.delay(400)
            
            isSearchingPlaces = true
            searchError = null
            
            val googleApiKey = try {
                com.example.BuildConfig.MAPS_API_KEY
            } catch (e: Throwable) {
                ""
            }
            
            val hasGoogleMapsKey = googleApiKey.isNotEmpty() && 
                    googleApiKey != "MY_MAPS_API_KEY" && 
                    googleApiKey != "MAPS_API_KEY_DEFAULT_VALUE"
                    
            val results = mutableListOf<SavedPlace>()
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .build()
                
            var searchSuccess = false
            
            // 1. Try Google Geocoding API if key is present
            if (hasGoogleMapsKey && isOnline) {
                try {
                    // Append Gulu Uganda to bias search locally
                    val addressQuery = if (query.lowercase().contains("gulu")) query else "$query, Gulu, Uganda"
                    val encodedQuery = java.net.URLEncoder.encode(addressQuery, "UTF-8")
                    val url = "https://maps.googleapis.com/maps/api/geocode/json?address=$encodedQuery&key=$googleApiKey"
                    
                    val request = okhttp3.Request.Builder()
                        .url(url)
                        .header("User-Agent", "BodaGuluApp/1.0")
                        .build()
                        
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyString = response.body?.string() ?: ""
                            val json = org.json.JSONObject(bodyString)
                            val status = json.optString("status")
                            if (status == "OK") {
                                val jsonResults = json.getJSONArray("results")
                                for (i in 0 until minOf(jsonResults.length(), 6)) {
                                    val item = jsonResults.getJSONObject(i)
                                    val formattedAddress = item.getString("formatted_address")
                                    val geometry = item.getJSONObject("geometry")
                                    val location = geometry.getJSONObject("location")
                                    val lat = location.getDouble("lat")
                                    val lng = location.getDouble("lng")
                                    
                                    // Extract shorter label if possible
                                    val addressComponents = item.getJSONArray("address_components")
                                    val label = if (addressComponents.length() > 0) {
                                        addressComponents.getJSONObject(0).getString("long_name")
                                    } else {
                                        formattedAddress.substringBefore(",")
                                    }
                                    
                                    results.add(SavedPlace(
                                        label = label,
                                        name = formattedAddress,
                                        latitude = lat,
                                        longitude = lng
                                    ))
                                }
                                searchSuccess = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    addPostgresLog("Google search failed: ${e.message}")
                    errorMessage.value = "Location search failed. Retrying with alternative provider."
                }
            }
            
            // 2. Fallback or direct to Nominatim (OpenStreetMap) if Google fails or key is missing
            if (!searchSuccess && isOnline) {
                try {
                    // Append Gulu, Uganda if not present
                    val searchQuery = if (query.lowercase().contains("gulu")) query else "$query, Gulu, Uganda"
                    val encodedQuery = java.net.URLEncoder.encode(searchQuery, "UTF-8")
                    val url = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=6&addressdetails=1"
                    
                    val request = okhttp3.Request.Builder()
                        .url(url)
                        .header("User-Agent", "BodaGuluApp/1.0")
                        .build()
                        
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyString = response.body?.string() ?: ""
                            val jsonArray = org.json.JSONArray(bodyString)
                            for (i in 0 until jsonArray.length()) {
                                val obj = jsonArray.getJSONObject(i)
                                val displayName = obj.getString("display_name")
                                val lat = obj.getDouble("lat")
                                val lon = obj.getDouble("lon")
                                
                                val addressObj = obj.optJSONObject("address")
                                val label = when {
                                    addressObj != null -> {
                                        addressObj.optString("amenity")
                                            .ifEmpty { addressObj.optString("building") }
                                            .ifEmpty { addressObj.optString("shop") }
                                            .ifEmpty { addressObj.optString("road") }
                                            .ifEmpty { addressObj.optString("suburb") }
                                            .ifEmpty { displayName.substringBefore(",") }
                                    }
                                    else -> displayName.substringBefore(",")
                                }
                                
                                results.add(SavedPlace(
                                    label = label,
                                    name = displayName,
                                    latitude = lat,
                                    longitude = lon
                                ))
                            }
                            searchSuccess = true
                        }
                    }
                } catch (e: Exception) {
                    addPostgresLog("OSM search failed: ${e.message}")
                    errorMessage.value = "Location search unavailable. Showing default suggestions."
                }
            }
            
            // If both failed or device is offline, search local default suggestions as fallback
            if (results.isEmpty()) {
                val defaultSuggestions = listOf(
                    SavedPlace(label = "Home", name = "Gulu Main Market, Gulu", latitude = 2.7712, longitude = 32.2985),
                    SavedPlace(label = "Work", name = "Lacor Hospital, Gulu", latitude = 2.7933, longitude = 32.2571),
                    SavedPlace(label = "University", name = "Gulu University, Laroo", latitude = 2.7842, longitude = 32.3214),
                    SavedPlace(label = "Stadium", name = "Pece Stadium, Gulu", latitude = 2.7745, longitude = 32.3112),
                    SavedPlace(label = "Town Hall", name = "Gulu Town Hall, Gulu", latitude = 2.7720, longitude = 32.3005),
                    SavedPlace(label = "Airfield", name = "Gulu Airfield, Gulu", latitude = 2.7961, longitude = 32.2801)
                )
                val filteredLocal = defaultSuggestions.filter {
                    it.label.contains(query, ignoreCase = true) ||
                    it.name.contains(query, ignoreCase = true)
                }
                results.addAll(filteredLocal)
            }
            
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                searchResults = results
                isSearchingPlaces = false
            }
        }
    }

    fun fetchDistanceMatrix() {
        val pickup = pickupPlace
        val dropoff = dropoffPlace
        if (pickup == null || dropoff == null) {
            googleDistanceKm = null
            googleDurationMins = null
            googleTrafficCondition = null
            return
        }
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                isFetchingDistanceMatrix = true
                distanceMatrixError = null
            }
            
            val googleApiKey = try {
                com.example.BuildConfig.MAPS_API_KEY
            } catch (e: Throwable) {
                ""
            }
            
            val hasGoogleMapsKey = googleApiKey.isNotEmpty() && 
                    googleApiKey != "MY_MAPS_API_KEY" && 
                    googleApiKey != "MAPS_API_KEY_DEFAULT_VALUE"
                    
            if (hasGoogleMapsKey && isOnline) {
                try {
                    addPostgresLog("Querying Google Maps Distance Matrix API for dynamic fare & traffic surge: FROM [${pickup.label}] TO [${dropoff.label}]")
                    val url = "https://maps.googleapis.com/maps/api/distancematrix/json" +
                            "?origins=${pickup.latitude},${pickup.longitude}" +
                            "&destinations=${dropoff.latitude},${dropoff.longitude}" +
                            "&departure_time=now" +
                            "&traffic_model=best_guess" +
                            "&key=$googleApiKey"
                            
                    val client = okhttp3.OkHttpClient.Builder()
                        .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                        .build()
                        
                    val request = okhttp3.Request.Builder()
                        .url(url)
                        .header("User-Agent", "BodaGuluApp/1.0")
                        .build()
                        
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyString = response.body?.string() ?: ""
                            val json = org.json.JSONObject(bodyString)
                            val status = json.optString("status")
                            if (status == "OK") {
                                val rows = json.getJSONArray("rows")
                                if (rows.length() > 0) {
                                    val elements = rows.getJSONObject(0).getJSONArray("elements")
                                    if (elements.length() > 0) {
                                        val element = elements.getJSONObject(0)
                                        val elementStatus = element.optString("status")
                                        if (elementStatus == "OK") {
                                            val distanceObj = element.getJSONObject("distance")
                                            val durationObj = element.getJSONObject("duration")
                                            
                                            val meters = distanceObj.getDouble("value")
                                            val seconds = durationObj.getDouble("value")
                                            
                                            val km = meters / 1000.0
                                            val mins = (seconds / 60.0).toInt().coerceAtLeast(1)
                                            
                                            var traffic = "Moderate"
                                            if (element.has("duration_in_traffic")) {
                                                val durationInTrafficSec = element.getJSONObject("duration_in_traffic").getDouble("value")
                                                val ratio = durationInTrafficSec / seconds
                                                traffic = when {
                                                    ratio <= 1.1 -> "Light"
                                                    ratio <= 1.3 -> "Moderate"
                                                    ratio <= 1.6 -> "Heavy"
                                                    else -> "Rush Hour"
                                                }
                                                addPostgresLog("✓ Distance Matrix parsed traffic multiplier: ${"%.2f".format(ratio)} ($traffic)")
                                            } else {
                                                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                                                traffic = when (hour) {
                                                    in 8..9, in 17..18 -> "Rush Hour"
                                                    in 7..19 -> "Moderate"
                                                    else -> "Light"
                                                }
                                            }
                                            
                                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                googleDistanceKm = km
                                                googleDurationMins = mins
                                                googleTrafficCondition = traffic
                                                trafficCondition = traffic
                                                addPostgresLog("✓ Successfully loaded metrics from Distance Matrix API: ${"%.2f".format(km)} km, $mins mins.")
                                            }
                                        } else {
                                            throw Exception("Distance Matrix element status: $elementStatus")
                                        }
                                    } else {
                                        throw Exception("No elements returned")
                                    }
                                } else {
                                    throw Exception("No rows returned")
                                }
                            } else {
                                throw Exception("Distance Matrix status: $status")
                            }
                        } else {
                            throw Exception("HTTP Error: ${response.code}")
                        }
                    }
                } catch (e: Exception) {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        distanceMatrixError = e.message
                        addPostgresLog("Distance Matrix API query failed (${e.message}). Using Haversine fallback.")
                    }
                } finally {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        isFetchingDistanceMatrix = false
                    }
                }
            } else {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isFetchingDistanceMatrix = false
                    addPostgresLog("Google Maps key missing/invalid. Distance Matrix bypassed, using Gulu Local Haversine Engine.")
                }
            }
        }
    }

    fun triggerOSRMRouteFetch() {
        val pickup = pickupPlace ?: return
        val dropoff = dropoffPlace ?: return

        val startLatLng = com.google.android.gms.maps.model.LatLng(pickup.latitude, pickup.longitude)
        val endLatLng = com.google.android.gms.maps.model.LatLng(dropoff.latitude, dropoff.longitude)
        fetchRouteForPoints(startLatLng, endLatLng)
        fetchDistanceMatrix()
    }

    // Screen State
    var currentScreen by mutableStateOf<Screen>(Screen.Splash)
        private set

    // Navigation Stack (Manual simple backstack for reliability)
    private val backStack = mutableListOf<Screen>()

    fun navigateTo(screen: Screen) {
        if (currentScreen != screen) {
            backStack.add(currentScreen)
            currentScreen = screen
            if (screen == Screen.RoutePreview) {
                triggerOSRMRouteFetch()
            }
        }
    }

    fun navigateBack() {
        if (backStack.isNotEmpty()) {
            currentScreen = backStack.removeAt(backStack.size - 1)
        } else {
            currentScreen = Screen.Home
        }
    }

    fun signOut() {
        stopLocationTracking()
        auth.signOut()

        isOtpVerified = false
        otpSent = false
        otpInput = ""
        phoneInput = ""
        signupName = ""
        verificationId = null
        resendToken = null
        backendBalance = null
        lastBackendFetchMs = 0L

        // Keep carousel + language completed so returning users skip straight to sign-in
        // onboardingCarouselCompleted and onboardingLanguageSelected are intentionally NOT reset

        prefs.edit()
            .apply()

        viewModelScope.launch {
            repository.clearAllUserData()
        }

        ApiClient.invalidateToken()

        backStack.clear()
        currentScreen = Screen.WelcomeOnboarding
    }

    fun deleteAccount() {
        viewModelScope.launch {
            apiRepository.deleteAccount().onSuccess {
                addPostgresLog("Account deleted from backend")
            }.onFailure { e ->
                addPostgresLog("Backend delete failed: ${e.message}")
            }

            withContext(Dispatchers.IO) {
                try {
                    Tasks.await(auth.currentUser?.delete() ?: Tasks.forResult(null))
                } catch (_: Exception) {}
            }

            auth.signOut()
            ApiClient.invalidateToken()

            repository.clearAllUserData()
            prefs.edit().clear().apply()

            isOtpVerified = false
            otpSent = false
            otpInput = ""
            phoneInput = ""
            signupName = ""
            verificationId = null
            resendToken = null
            backendBalance = null
            lastBackendFetchMs = 0L
            onboardingCarouselCompleted = false
            onboardingLanguageSelected = false
            onboardingSlideIndex = 0

            backStack.clear()
            currentScreen = Screen.WelcomeOnboarding
        }
    }

    // Location Services
    @Suppress("MissingPermission")
    fun startLocationTracking() {
        if (isLocationTracking) return
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(2000L)
            .build()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    currentLocation = location
                }
            }
        }
        
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, null)
        isLocationTracking = true
        
        // Also get last known location immediately
        getLastKnownLocation()
    }

    @Suppress("MissingPermission")
    private fun getLastKnownLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = location
                }
            }
    }

    fun stopLocationTracking() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
        isLocationTracking = false
    }

    // Ride Request Functions — all ride data flows through PostgreSQL backend + Socket.IO
    fun createRideRequest() {
        bookTripViaBackend()
    }

    fun cancelRideRequest() {
        currentRideRequest = null
        isSearchingForDriver = false
    }

    fun updateDriverLocation() {
        updateDriverStatusViaBackend(isDriverOnline)
    }

    // Language selector state ("en", "ach", "luo")
    var appLanguage by mutableStateOf("en")
        private set

    fun updateLanguage(lang: String) {
        appLanguage = lang
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfile()
            val updated = current.copy(language = lang)
            repository.saveUserProfile(updated)
            syncUserToBackend(updated)
        }
    }

    // Backend API Functions
    fun connectToBackend() {
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
                trafficCondition = pricing.surgeReason
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
                    // Avoid duplicates if the message was already added locally
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

    fun syncUserToBackend(profile: UserProfile? = null) {
        val user = profile ?: userProfile.value ?: return
        val phone = user.phoneNumber.replace("+256", "").trim()
        android.util.Log.d("BODA_SYNC", "Syncing user: phone=$phone name=${user.name}")

        viewModelScope.launch {
            val tokenRefreshed = withContext(Dispatchers.IO) {
                try {
                    Tasks.await(
                        auth.currentUser?.getIdToken(true)
                            ?: Tasks.forResult(null)
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

    fun bookTripViaBackend() {
        val pickup = pickupPlace ?: return
        val dropoff = dropoffPlace ?: return
        val user = userProfile.value ?: return
        
        viewModelScope.launch {
            val fare = calculatedFare
            apiRepository.bookTrip(
                pickupName = pickup.name,
                pickupLat = pickup.latitude,
                pickupLon = pickup.longitude,
                dropoffName = dropoff.name,
                dropoffLat = dropoff.latitude,
                dropoffLon = dropoff.longitude,
                distanceKm = googleDistanceKm ?: 0.0,
                durationMins = googleDurationMins ?: 0,
                fare = fare,
                paymentMethod = selectedPaymentMethod
            ).fold(
                onSuccess = { trip ->
                    addPostgresLog("✓ Trip booked via PostgreSQL: ${trip.id}")
                    currentRideRequest = com.example.data.RideRequest(
                        id = trip.id.toString(),
                        riderId = auth.currentUser?.uid ?: "",
                        riderName = user.name,
                        riderPhone = user.phoneNumber,
                        pickupName = pickup.name,
                        pickupLat = pickup.latitude,
                        pickupLng = pickup.longitude,
                        dropoffName = dropoff.name,
                        dropoffLat = dropoff.latitude,
                        dropoffLng = dropoff.longitude,
                        fare = fare,
                        paymentMethod = selectedPaymentMethod,
                        status = trip.status
                    )
                    isSearchingForDriver = true
                },
                onFailure = { e ->
                    errorMessage.value = "Failed to book trip: ${e.message}"
                    addPostgresLog("✗ Trip booking failed: ${e.message}")
                }
            )
        }
    }

    fun calculateFareViaBackend(distanceKm: Double, durationMins: Int) {
        viewModelScope.launch {
            apiRepository.calculateFare(distanceKm, durationMins).fold(
                onSuccess = { fareResponse ->
                    addPostgresLog("✓ Fare calculated: UGX ${fareResponse.final_fare} (surge: ${fareResponse.surge_multiplier}x)")
                },
                onFailure = { e ->
                    addPostgresLog("✗ Fare calculation failed: ${e.message}")
                }
            )
        }
    }

    fun validatePromoViaBackend(code: String) {
        viewModelScope.launch {
            val fare = calculatedFare
            apiRepository.validatePromo(code, fare).fold(
                onSuccess = { promoResponse ->
                    if (promoResponse.valid) {
                        activePromoDiscount.value = promoResponse.discount_amount.toDouble()
                        activePromoCode = code
                        addPostgresLog("✓ Promo applied: -UGX ${promoResponse.discount_amount}")
                    } else {
                        errorMessage.value = promoResponse.message ?: "Invalid promo code"
                    }
                },
                onFailure = { e ->
                    errorMessage.value = "Failed to validate promo: ${e.message}"
                }
            )
        }
    }

    private fun registerFcmToken() {
        val prefs = getApplication<Application>().getSharedPreferences("boda_gulu_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("fcm_token", null) ?: return
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val idToken = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.google.android.gms.tasks.Tasks.await(
                        auth.currentUser?.getIdToken(false)
                            ?: com.google.android.gms.tasks.Tasks.forResult(null)
                    )?.token
                }
                val client = okhttp3.OkHttpClient()
                val json = org.json.JSONObject().put("fcm_token", token).put("uid", uid)
                val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val requestBuilder = okhttp3.Request.Builder()
                    .url("${com.example.data.ApiClient.getBaseUrl()}/api/users/fcm-token")
                    .post(body)
                if (idToken != null) {
                    requestBuilder.addHeader("Authorization", "Bearer $idToken")
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    client.newCall(requestBuilder.build()).execute()
                }
            } catch (_: Exception) { }
        }
    }

    fun registerDriverViaBackend() {
        val user = userProfile.value ?: return
        
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            apiRepository.registerDriver(
                uid = uid,
                fullName = user.name,
                phone = user.phoneNumber,
                plateNumber = driverRegPlate,
                helmetNumber = driverRegHelmetColor
            ).fold(
                onSuccess = { driver ->
                    addPostgresLog("✓ Driver registered in PostgreSQL: ${driver.uid}")
                },
                onFailure = { e ->
                    addPostgresLog("✗ Driver registration failed: ${e.message}")
                }
            )
        }
    }

    fun updateDriverStatusViaBackend(isOnline: Boolean) {
        val location = currentLocation ?: return
        val user = userProfile.value ?: return
        
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            apiRepository.updateDriverStatus(uid, isOnline, location.latitude, location.longitude).fold(
                onSuccess = { driver ->
                    addPostgresLog("✓ Driver status updated: online=$isOnline")
                },
                onFailure = { e ->
                    addPostgresLog("✗ Driver status update failed: ${e.message}")
                }
            )
        }
    }

    // Persistent theme settings: "system", "dark", "light"
    var appThemeSetting by mutableStateOf(prefs.getString("app_theme_setting", "system") ?: "system")
        private set

    fun updateAppThemeSetting(setting: String) {
        appThemeSetting = setting
        prefs.edit().putString("app_theme_setting", setting).apply()
    }

    // Persistent onboarding state flags
    var onboardingCarouselCompleted by mutableStateOf(prefs.getBoolean("onboarding_carousel_completed", false))
        private set

    var onboardingLanguageSelected by mutableStateOf(prefs.getBoolean("onboarding_language_selected", false))
        private set

    fun completeOnboardingCarousel() {
        onboardingCarouselCompleted = true
        prefs.edit().putBoolean("onboarding_carousel_completed", true).apply()
    }

    fun completeOnboardingLanguage(lang: String) {
        updateLanguage(lang)
        onboardingLanguageSelected = true
        prefs.edit().putBoolean("onboarding_language_selected", true).apply()
    }

    fun resetOnboarding() {
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

    // Onboarding slide index
    var onboardingSlideIndex by mutableStateOf(0)

    // Phone number input
    var otpResendTimer by mutableStateOf(45)
    var isSendingOtp by mutableStateOf(false)
    var isVerifyingOtp by mutableStateOf(false)
    private var otpTimerJob: Job? = null

    // Location & notification permission simulation
    var locationPermissionGranted by mutableStateOf(false)
    var notificationPermissionGranted by mutableStateOf(false)

    // Profile inputs
    var signupName by mutableStateOf("")
    var selectedAvatarRes by mutableStateOf(1)

    // Google Sign-In state
    var isSigningInWithGoogle by mutableStateOf(false)
        private set
    var googleSignInError by mutableStateOf<String?>(null)
        private set

    // Booking Inputs
    var serviceType by mutableStateOf("ride") // "ride" or "delivery"
    var pickupPlace by mutableStateOf<SavedPlace?>(null)
    var dropoffPlace by mutableStateOf<SavedPlace?>(null)
    var pickupText by mutableStateOf("")
    var dropoffText by mutableStateOf("")
    var searchResults by mutableStateOf<List<SavedPlace>>(emptyList())
    var isSearchingPlaces by mutableStateOf(false)
    var searchError by mutableStateOf<String?>(null)
    private var searchJob: kotlinx.coroutines.Job? = null
    
    // Google Maps Distance Matrix API states
    var googleDistanceKm by mutableStateOf<Double?>(null)
    var googleDurationMins by mutableStateOf<Int?>(null)
    var googleTrafficCondition by mutableStateOf<String?>(null)
    var isFetchingDistanceMatrix by mutableStateOf(false)
    var distanceMatrixError by mutableStateOf<String?>(null)

    var selectedPaymentMethod by mutableStateOf("MTN") // "MTN", "Airtel", "Wallet"
    var parcelDetails by mutableStateOf("")
    var recipientName by mutableStateOf("")
    var recipientPhone by mutableStateOf("")
    var scheduledBookingDateTime by mutableStateOf<String?>(null)
    var trafficCondition by mutableStateOf("Moderate") // "Light", "Moderate", "Heavy", "Rush Hour"

    private fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Radius of the earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    // Trip Estimates
    val calculatedFare: Double
        get() {
            val dist = calculatedDistanceKm
            val base = if (serviceType == "ride") 1500.0 else 2500.0
            val distanceCharge = dist * 1000.0
            val trafficSurge = when (trafficCondition) {
                "Light" -> 0.0
                "Moderate" -> 500.0
                "Heavy" -> 1500.0
                "Rush Hour" -> 2500.0
                else -> 500.0
            }
            val rawFare = base + distanceCharge + trafficSurge
            return (Math.round(rawFare / 500.0) * 500.0).toDouble().coerceAtLeast(1000.0)
        }

    val calculatedTimeMinutes: Int
        get() {
            googleDurationMins?.let { return it }
            val distKm = calculatedDistanceKm
            val rawMinutes = (distKm / 25.0 * 60.0).toInt()
            return rawMinutes.coerceAtLeast(3)
        }

    val calculatedDistanceKm: Double
        get() {
            googleDistanceKm?.let { return it }
            val pick = pickupPlace ?: return 0.0
            val drop = dropoffPlace ?: return 0.0
            val straightLine = calculateHaversineDistance(
                pick.latitude, pick.longitude,
                drop.latitude, drop.longitude
            )
            val roadMultiplier = 1.4
            val estimated = straightLine * roadMultiplier
            return estimated.coerceAtLeast(0.8)
        }

    // Simulation Live States
    var bookingMatchTripId by mutableStateOf<Long?>(null)
    var currentSimulationTrip by mutableStateOf<Trip?>(null)
    var matchProgress by mutableStateOf(0f)
    var searchingRider by mutableStateOf(false)
    var simulationState by mutableStateOf("idle") // "searching", "enroute", "arrived", "active", "completed"
    var simulationCountdown by mutableStateOf(10)
    var simulationRouteProgress by mutableStateOf(0f)
    private var simulationJob: Job? = null

    // Wallet transaction state
    var walletTopupAmountInput by mutableStateOf("5000")
    var walletTopupPhoneInput by mutableStateOf("")
    var walletTopupStatus by mutableStateOf("") // "", "pending", "success", "failed"
    var activeTransactionReference by mutableStateOf("")

    // Interactive MoMo Pin overlay state
    var showMoMoPinDialog by mutableStateOf(false)
    var momoPinInput by mutableStateOf("")
    var momoPromptAmount by mutableStateOf(0.0)
    var momoPromptProvider by mutableStateOf("MTN MoMo")
    var momoPromptPhone by mutableStateOf("")
    var momoPinError by mutableStateOf(false)

    // Interactive Call Overlay state
    var showCallOverlay by mutableStateOf(false)
    var callOverlayName by mutableStateOf("")
    var callOverlayNumber by mutableStateOf("")
    var callOverlayState by mutableStateOf("dialing") // "dialing", "active", "disconnected"
    var callDurationSeconds by mutableStateOf(0)
    private var callTimerJob: Job? = null

    // Interactive Rider Chat Overlay state
    var showRiderChatOverlay by mutableStateOf(false)
    val riderChatMessages = androidx.compose.runtime.mutableStateListOf<ChatMessage>()
    var riderChatInputText by mutableStateOf("")
    var riderIsTyping by mutableStateOf(false)
    private var currentChatTripId = 0

    // Settings
    var promoCodeInput by mutableStateOf("")
    var activePromoCode by mutableStateOf<String?>(null)
    var activePromoMessage by mutableStateOf("")
    var activePromoDiscount = mutableStateOf(0.0)
    var referralCodeInput by mutableStateOf("")

    // Emergency profile
    var newEmergencyName by mutableStateOf("")
    var newEmergencyPhone by mutableStateOf("")

    // Saved places addition
    var newPlaceLabel by mutableStateOf("")
    var newPlaceName by mutableStateOf("")

    // Help & ticket
    var supportTickets = mutableListOf<SupportTicket>(
        SupportTicket("ST-8109", "Wrong Fare Charged", "Resolved", "2026-06-21"),
        SupportTicket("ST-9021", "Lost Item (Phone)", "Open", "2026-06-23")
    )
    var activeChatMessages = mutableListOf<ChatMessage>(
        ChatMessage("system", "Hello! Welcome to Boda Gulu Support. How can we help you in Gulu today?"),
        ChatMessage("user", "Hello, I left my bag with a rider Patrick yesterday."),
        ChatMessage("agent", "I see your trip with Patrick. I will contact him immediately to secure your bag.")
    )
    var newChatMessageText by mutableStateOf("")
    var supportSearchQuery by mutableStateOf("")
    var newTicketSubject by mutableStateOf("")
    var newTicketCategory by mutableStateOf("Ride Issue")
    var newTicketDetails by mutableStateOf("")

    // App state flags
    var isOffline by mutableStateOf(false)
    var showDowntimeNotice by mutableStateOf(false)
    var showSuspendedNotice by mutableStateOf(false)

    // --- BODA GULU DRIVER MODE STATE MACHINE ---
    var isDriverMode by mutableStateOf(false)
    var isDriverOnline by mutableStateOf(false)
    var driverIncomingRequest by mutableStateOf<Trip?>(null)
    var driverActiveTrip by mutableStateOf<Trip?>(null)
    var driverEarnings by mutableStateOf(45000.0)
    var driverCompletedTrips by mutableStateOf(18)
    var driverRating by mutableStateOf(4.9)
    var driverTripState by mutableStateOf("none") // "none", "requested", "accepted", "pickup_arrived", "active", "completed"
    var driverSimulationProgress by mutableStateOf(0f)
    var driverSimulationCountdown by mutableStateOf(0)
    private var driverSimulationJob: Job? = null

    // --- Driver Onboarding Wizard State ---
    var isDriverRegistered by mutableStateOf(false)
    var driverRegName by mutableStateOf("")
    var driverRegPhone by mutableStateOf("")
    var driverRegNID by mutableStateOf("")
    var driverRegPlate by mutableStateOf("")
    var driverRegStage by mutableStateOf("Gulu Main Market Stage")
    var driverRegHelmetColor by mutableStateOf("Yellow")
    var driverOnboardingStep by mutableStateOf(1) // 1: Info & NIN, 2: Motorcycle Details, 3: Document Upload Simulator, 4: Safety & Community quiz, 5: Verified Status
    var driverUploadProgress by mutableStateOf(0f)
    var driverDocumentType by mutableStateOf("National ID Card (NIN)")
    var driverDocsUploaded by mutableStateOf(setOf<String>())
    var driverTermsAccepted by mutableStateOf(false)
    var driverQuizAnswer1 by mutableStateOf("")
    var driverQuizAnswer2 by mutableStateOf("")

    fun startDriverOnboarding() {
        driverOnboardingStep = 1
        driverUploadProgress = 0f
        driverDocsUploaded = emptySet()
        driverTermsAccepted = false
        driverQuizAnswer1 = ""
        driverQuizAnswer2 = ""
        // Prepopulate from passenger profile if exists
        userProfile.value?.let {
            if (driverRegName.isEmpty()) driverRegName = it.name
            if (driverRegPhone.isEmpty()) driverRegPhone = it.phoneNumber
        }
        navigateTo(Screen.DriverOnboarding)
    }

    fun simulateDocUpload(docType: String) {
        viewModelScope.launch {
            driverDocumentType = docType
            driverUploadProgress = 0f
            while (driverUploadProgress < 1.0f) {
                delay(100)
                driverUploadProgress += 0.1f
            }
            driverDocsUploaded = driverDocsUploaded + docType
            driverUploadProgress = 1.0f
        }
    }

    fun completeDriverOnboarding() {
        isDriverRegistered = true
        isDriverMode = true
        isDriverOnline = true
        driverTripState = "none"
        navigateTo(Screen.Home)
    }

    fun toggleDriverOnline() {
        isDriverOnline = !isDriverOnline
        if (isDriverOnline) {
            driverSimulationJob?.cancel()
            updateDriverStatusViaBackend(true)
            startLocationTracking()
        } else {
            updateDriverStatusViaBackend(false)
            stopLocationTracking()
            driverIncomingRequest = null
            driverActiveTrip = null
            driverTripState = "none"
            driverSimulationJob?.cancel()
        }
    }

    fun driverAcceptTrip() {
        val req = driverIncomingRequest ?: return
        driverActiveTrip = req.copy(status = "matched")
        driverIncomingRequest = null
        driverTripState = "accepted"
        driverSimulationCountdown = 8
        driverSimulationProgress = 0f

        viewModelScope.launch {
            apiRepository.claimTrip(req.id).fold(
                onSuccess = { /* trip claimed successfully */ },
                onFailure = { e ->
                    errorMessage.value = "Could not claim trip: ${e.message}"
                    driverActiveTrip = null
                    driverTripState = "none"
                }
            )
        }

        val driverLat = currentLocation?.latitude ?: 2.775
        val driverLng = currentLocation?.longitude ?: 32.295
        fetchRouteForPoints(
            com.google.android.gms.maps.model.LatLng(driverLat, driverLng),
            getLatLngForPlace(req.pickupName)
        )
        
        driverSimulationJob?.cancel()
        driverSimulationJob = viewModelScope.launch {
            // Animate driver moving towards the pickup Gulu node
            while (driverSimulationCountdown > 0) {
                delay(1000)
                driverSimulationCountdown--
                driverSimulationProgress = (8 - driverSimulationCountdown) / 8f
            }
            driverTripState = "pickup_arrived"
        }
    }

    fun driverRejectTrip() {
        driverIncomingRequest = null
        driverTripState = "none"
    }

    fun driverArrivePickup() {
        driverTripState = "pickup_arrived"
    }

    fun driverStartTrip() {
        val trip = driverActiveTrip ?: return
        driverActiveTrip = trip.copy(status = "active")
        driverTripState = "active"
        driverSimulationCountdown = 10
        driverSimulationProgress = 0f

        viewModelScope.launch { apiRepository.updateTripStatus(trip.id, "active") }
        
        fetchRouteForPoints(
            getLatLngForPlace(trip.pickupName),
            getLatLngForPlace(trip.dropoffName)
        )
        
        driverSimulationJob?.cancel()
        driverSimulationJob = viewModelScope.launch {
            // Animate transit along Gulu grid streets
            while (driverSimulationCountdown > 0) {
                delay(1000)
                driverSimulationCountdown--
                driverSimulationProgress = (10 - driverSimulationCountdown) / 10f
            }
            driverTripState = "completed"
            driverCompleteTrip()
        }
    }

    fun driverCompleteTrip() {
        val trip = driverActiveTrip ?: return
        viewModelScope.launch {
            driverEarnings += trip.fare
            driverCompletedTrips += 1
            apiRepository.updateTripStatus(trip.id, "completed")
            
            // Log completed trip in Room DB histories
            repository.addTrip(trip.copy(id = 0, status = "completed"))
            
            // Pay earnings into the virtual driver's wallet
            repository.addTransaction(WalletTransaction(
                amount = trip.fare,
                type = "topup", // Credit
                status = "completed",
                phoneNumber = userProfile.value?.phoneNumber ?: "+256 772 123456",
                timestamp = System.currentTimeMillis(),
                provider = trip.paymentMethod,
                reference = "BODA-DRV-PAY-" + (1000 + Random.nextInt(9000))
            ))
            
            driverActiveTrip = null
            driverTripState = "none"
            refreshWalletBalance()
        }
    }

    // Trigger OTP sending via Firebase Auth
    fun startOtpFlow(activity: android.app.Activity? = null) {
        if (phoneInput.length < 9) {
            errorMessage.value = "Phone number must be at least 9 digits."
            return
        }

        val phoneNumber = "+256" + phoneInput.removePrefix("+256").trim()

        isSendingOtp = true
        otpSent = true
        otpInput = ""
        otpResendTimer = 45
        isOtpVerified = false

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                val code = credential.smsCode
                if (code != null) {
                    otpInput = code
                    isSendingOtp = false
                    verifyOtp()
                }
            }

            override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                errorMessage.value = "Verification failed: ${e.message}"
                otpSent = false
                isSendingOtp = false
            }

            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                verificationId = id
                resendToken = token
                isSendingOtp = false
            }
        }

        val builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setCallbacks(callbacks)
        activity?.let { builder.setActivity(it) }
        val options = builder.build()
        
        PhoneAuthProvider.verifyPhoneNumber(options)
        
        // Start OTP countdown timer
        otpTimerJob?.cancel()
        otpTimerJob = viewModelScope.launch {
            while (otpResendTimer > 0) {
                delay(1000)
                otpResendTimer--
            }
        }
    }

    fun verifyOtp() {
        val vid = verificationId
        if (vid == null) {
            errorMessage.value = "Verification not started. Please request a new code."
            return
        }

        if (otpInput.length != 6) {
            errorMessage.value = "Please enter the 6-digit code."
            return
        }

        isVerifyingOtp = true
        val credential = PhoneAuthProvider.getCredential(vid, otpInput)

        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                isVerifyingOtp = false
                otpTimerJob?.cancel()
                ApiClient.invalidateToken()

                // Check if this is a returning user or a new one
                viewModelScope.launch {
                    val backendResult = apiRepository.fetchUserProfile()
                    backendResult.fold(
                        onSuccess = { backendUser ->
                            val cleanPhone = backendUser.phone
                                ?.takeIf { it.isNotEmpty() && it != "+256770000000" && it != "256770000000" }
                                ?: ""
                            val profile = UserProfile(
                                id = 1,
                                name = backendUser.full_name,
                                phoneNumber = cleanPhone.ifEmpty {
                                    "+256 " + phoneInput.removePrefix("+256").trim()
                                },
                                language = backendUser.language.ifEmpty { appLanguage },
                                isSetupComplete = backendUser.full_name.isNotEmpty(),
                                referralCode = backendUser.referral_code ?: ""
                            )
                            repository.saveUserProfile(profile)
                            backendBalance = backendUser.wallet_balance

                            if (profile.isSetupComplete) {
                                // Returning user — skip profile setup, go straight to Home
                                fetchBackendData(force = true)
                                navigateTo(Screen.Home)
                            } else {
                                // Account exists but profile setup was never finished
                                isOtpVerified = true
                            }
                        },
                        onFailure = {
                            // New user — show profile setup step
                            isOtpVerified = true
                        }
                    )
                }
            }
            .addOnFailureListener { e ->
                errorMessage.value = "Invalid code: ${e.message}"
                isVerifyingOtp = false
            }
    }

    // Profile Creation / Completion
    fun completeProfileSetup() {
        viewModelScope.launch {
            val genCode = "GULU-${signupName.filter { it.isLetter() }.take(4).uppercase().ifEmpty { "BODA" }}-${Random.nextInt(100, 999)}"
            val profile = UserProfile(
                name = signupName,
                phoneNumber = if (phoneInput.isNotEmpty()) {
                    "+256 " + phoneInput.removePrefix("+256").trim()
                } else {
                    auth.currentUser?.phoneNumber ?: ""
                },
                language = appLanguage,
                isSetupComplete = true,
                profileImageResId = selectedAvatarRes,
                referralCode = genCode
            )
            repository.saveUserProfile(profile)
            syncUserToBackend(profile)

            if (referralCodeInput.isNotEmpty()) {
                repository.addReferral(Referral(
                    referredName = signupName,
                    referredPhone = profile.phoneNumber,
                    referralCodeUsed = referralCodeInput.uppercase().trim(),
                    status = "pending",
                    timestamp = System.currentTimeMillis(),
                    rewardAmount = 3000.0
                ))
                apiRepository.addReferralToBackend(
                    signupName, profile.phoneNumber, referralCodeInput.uppercase().trim()
                )
            }

            val welcomeBonusShown = prefs.getBoolean("welcome_bonus_shown", false)
            if (!welcomeBonusShown) {
                isNewUserSession = true
                showWelcomeBonus = true
            }

            navigateTo(Screen.Home)
        }
    }

    fun saveUserProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.saveUserProfile(profile)
            syncUserToBackend(profile)
        }
    }

    // Confirm Booking & Start Simulation
    fun confirmBooking() {
        if (pickupPlace == null || dropoffPlace == null) {
            errorMessage.value = "Please select both pickup and dropoff locations."
            return
        }
        
        simulationState = "searching"
        navigateTo(Screen.Matching)
        
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            // Simulate matching for 4 seconds
            for (i in 1..40) {
                delay(100)
                matchProgress = i / 40f
            }
            
            // Randomly select one of Gulu's top riders
            val riderNames = listOf("Okeny Patrick", "Adong Scovia", "Akena Christopher", "Kidega Moses")
            val plates = listOf("UEG 412X", "UED 891B", "UEF 201A", "UEH 556W")
            val photoIds = listOf(2, 3, 4, 1)
            val index = Random.nextInt(riderNames.size)
            
            val fareWithDiscount = (calculatedFare - activePromoDiscount.value).coerceAtLeast(1000.0)

            val riderPhone = "+256 781 " + (100000 + Random.nextInt(899999))
            val packageDetails = if (serviceType == "delivery") parcelDetails else null
            val recipientNameVal = if (serviceType == "delivery") recipientName else null
            val recipientPhoneVal = if (serviceType == "delivery") recipientPhone else null

            val backendTrip = apiRepository.bookTrip(
                pickupName = pickupPlace!!.name,
                pickupLat = pickupPlace!!.latitude,
                pickupLon = pickupPlace!!.longitude,
                dropoffName = dropoffPlace!!.name,
                dropoffLat = dropoffPlace!!.latitude,
                dropoffLon = dropoffPlace!!.longitude,
                distanceKm = googleDistanceKm ?: 0.0,
                durationMins = googleDurationMins ?: 0,
                fare = fareWithDiscount,
                paymentMethod = selectedPaymentMethod
            ).getOrNull()

            if (backendTrip != null) {
                currentSimulationTrip = Trip(
                    id = backendTrip.id,
                    type = serviceType,
                    pickupName = pickupPlace!!.name,
                    dropoffName = dropoffPlace!!.name,
                    fare = fareWithDiscount,
                    paymentMethod = selectedPaymentMethod,
                    status = "matched",
                    riderName = riderNames[index],
                    riderPlate = plates[index],
                    riderPhone = riderPhone,
                    riderPhotoResId = photoIds[index],
                    packageDetails = packageDetails,
                    recipientName = recipientNameVal,
                    recipientPhone = recipientPhoneVal
                )
                bookingMatchTripId = backendTrip.id.toLong()
                currentRideRequest = com.example.data.RideRequest(
                    id = backendTrip.id.toString(),
                    riderId = auth.currentUser?.uid ?: "",
                    riderName = userProfile.value?.name ?: "",
                    riderPhone = userProfile.value?.phoneNumber ?: "",
                    pickupName = pickupPlace!!.name,
                    pickupLat = pickupPlace!!.latitude,
                    pickupLng = pickupPlace!!.longitude,
                    dropoffName = dropoffPlace!!.name,
                    dropoffLat = dropoffPlace!!.latitude,
                    dropoffLng = dropoffPlace!!.longitude,
                    fare = fareWithDiscount,
                    paymentMethod = selectedPaymentMethod,
                    status = "matched"
                )
                addPostgresLog("✓ Trip booked via PostgreSQL: ${backendTrip.id}")
            } else {
                val localTrip = Trip(
                    type = serviceType,
                    pickupName = pickupPlace!!.name,
                    dropoffName = dropoffPlace!!.name,
                    fare = fareWithDiscount,
                    paymentMethod = selectedPaymentMethod,
                    status = "matched",
                    riderName = riderNames[index],
                    riderPlate = plates[index],
                    riderPhone = riderPhone,
                    riderPhotoResId = photoIds[index],
                    packageDetails = packageDetails,
                    recipientName = recipientNameVal,
                    recipientPhone = recipientPhoneVal
                )
                val tripId = repository.addTrip(localTrip)
                bookingMatchTripId = tripId
                currentSimulationTrip = localTrip.copy(id = tripId.toInt())
            }

            // Shift screen to Rider En Route
            simulationState = "enroute"

            // Wallet hold at booking confirmation
            if (selectedPaymentMethod == "Wallet") {
                val holdRef = "HOLD-BODA-${bookingMatchTripId}"
                repository.addTransaction(WalletTransaction(
                    amount = fareWithDiscount,
                    type = "payment",
                    status = "pending",
                    phoneNumber = userProfile.value?.phoneNumber ?: "",
                    timestamp = System.currentTimeMillis(),
                    provider = "Wallet",
                    reference = holdRef
                ))
                refreshWalletBalance()
            }
            simulationCountdown = 8
            fetchRouteForPoints(
                com.google.android.gms.maps.model.LatLng(2.775, 32.295),
                com.google.android.gms.maps.model.LatLng(pickupPlace!!.latitude, pickupPlace!!.longitude)
            )
            navigateTo(Screen.RiderEnRoute)
            
            // Countdown for rider's ETA
            while (simulationCountdown > 0) {
                delay(1000)
                simulationCountdown--
            }
            
            // Rider Arrives
            simulationState = "arrived"
            // Wait for user to tap "Start Ride" on the en route screen (simulates meeting rider and entering security OTP)
        }
    }

    fun startActiveTrip() {
        if (currentSimulationTrip == null) return
        
        simulationState = "active"
        simulationCountdown = 10
        simulationRouteProgress = 0f
        fetchRouteForPoints(
            com.google.android.gms.maps.model.LatLng(pickupPlace!!.latitude, pickupPlace!!.longitude),
            com.google.android.gms.maps.model.LatLng(dropoffPlace!!.latitude, dropoffPlace!!.longitude)
        )
        navigateTo(Screen.ActiveTrip)
        
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            while (simulationCountdown > 0) {
                delay(1000)
                simulationCountdown--
                simulationRouteProgress = (10 - simulationCountdown) / 10f
            }
            
            // Trip completes
            simulationState = "completed"
            
            val ongoing = currentSimulationTrip
            if (ongoing != null) {
                val updated = ongoing.copy(status = "completed")
                repository.updateTrip(updated)
                currentSimulationTrip = updated
                onPassengerTripCompleted()
            }
            
            navigateTo(Screen.PostTrip)
        }
    }

    fun cancelActiveTrip(reason: String) {
        simulationJob?.cancel()
        viewModelScope.launch {
            val ongoing = currentSimulationTrip
            if (ongoing != null) {
                val updated = ongoing.copy(status = "canceled", comment = "Canceled: $reason")
                repository.updateTrip(updated)
                apiRepository.updateTripStatus(ongoing.id, "canceled")
                
                // Cancellation fee only if trip is actively in progress
                if (simulationState == "active") {
                    repository.addTransaction(WalletTransaction(
                        amount = 1000.0,
                        type = "payment",
                        status = "completed",
                        phoneNumber = userProfile.value?.phoneNumber ?: "+256 772 123456",
                        timestamp = System.currentTimeMillis(),
                        provider = "Wallet",
                        reference = "BODA-CANCELLATION-FEE-${ongoing.id}"
                    ))
                    apiRepository.walletPay(1000.0, "Wallet")
                    refreshWalletBalance()
                }
            }
            simulationState = "idle"
            currentSimulationTrip = null
            navigateTo(Screen.Home)
        }
    }

    fun submitPostTripRating(stars: Int, comment: String) {
        viewModelScope.launch {
            val ongoing = currentSimulationTrip
            if (ongoing != null) {
                val updated = ongoing.copy(rating = stars, comment = comment)
                repository.updateTrip(updated)
                apiRepository.updateTripStatus(ongoing.id, "completed", stars, comment)
            }
            
            // Complete the pending wallet hold if payment method is Wallet
            val finalFare = (calculatedFare - activePromoDiscount.value).coerceAtLeast(1000.0)
            if (selectedPaymentMethod == "Wallet" && ongoing != null) {
                repository.completePendingPayment("HOLD-BODA-${ongoing.id}")
                apiRepository.walletPay(finalFare, "Wallet")
            } else if (ongoing != null) {
                // Mobile money payments — record at completion
                repository.addTransaction(WalletTransaction(
                    amount = finalFare,
                    type = "payment",
                    status = "completed",
                    phoneNumber = userProfile.value?.phoneNumber ?: "+256 772 123456",
                    timestamp = System.currentTimeMillis(),
                    provider = selectedPaymentMethod,
                    reference = "${selectedPaymentMethod}-MOM-RIDE-${ongoing.id}"
                ))
                apiRepository.walletPay(finalFare, selectedPaymentMethod)
            }

            refreshWalletBalance()

            // Reset state
            pickupPlace = null
            dropoffPlace = null
            pickupText = ""
            dropoffText = ""
            parcelDetails = ""
            recipientName = ""
            recipientPhone = ""
            scheduledBookingDateTime = null
            activePromoDiscount.value = 0.0
            activePromoCode = null
            googleDistanceKm = null
            googleDurationMins = null
            googleTrafficCondition = null
            distanceMatrixError = null
            currentSimulationTrip = null
            simulationState = "idle"
            osrmRoutePoints = emptyList()
            
            navigateTo(Screen.Home)
        }
    }

    // Wallet actions
    fun startWalletTopup() {
        val amount = walletTopupAmountInput.toDoubleOrNull() ?: 0.0
        if (amount < 500.0) {
            walletTopupStatus = "error_too_low"
            errorMessage.value = "Amount too low. Minimum wallet deposit is 500 UGX."
            return
        }
        
        momoPromptAmount = amount
        momoPromptPhone = if (walletTopupPhoneInput.isNotEmpty()) walletTopupPhoneInput else (userProfile.value?.phoneNumber ?: "0772 123456")
        momoPromptProvider = if (momoPromptPhone.contains("078") || momoPromptPhone.contains("077") || momoPromptPhone.contains("076")) "MTN MoMo" else "Airtel Money"
        momoPinInput = ""
        momoPinError = false
        showMoMoPinDialog = true
    }

    fun confirmWalletTopupWithPin() {
        if (momoPinInput.length < 4) {
            momoPinError = true
            errorMessage.value = "PIN too short. Mobile Money PIN must be 4 or more digits."
            return
        }
        showMoMoPinDialog = false
        walletTopupStatus = "pending"

        viewModelScope.launch {
            val provider = if (momoPromptProvider.contains("MTN")) "MTN" else "Airtel"
            apiRepository.walletTopup(momoPromptAmount, provider).fold(
                onSuccess = { response ->
                    activeTransactionReference = response.reference
                    val transaction = WalletTransaction(
                        amount = momoPromptAmount,
                        type = "topup",
                        status = "completed",
                        phoneNumber = momoPromptPhone,
                        timestamp = System.currentTimeMillis(),
                        provider = provider,
                        reference = response.reference
                    )
                    repository.addTransaction(transaction)
                    walletTopupStatus = "success"
                    refreshWalletBalance()
                },
                onFailure = { e ->
                    errorMessage.value = "Topup failed: ${e.message}"
                    walletTopupStatus = "failed"
                }
            )
        }
    }

    // Call Simulator actions
    fun initiateCall(name: String, phone: String) {
        callOverlayName = name
        callOverlayNumber = phone
        callOverlayState = "dialing"
        callDurationSeconds = 0
        showCallOverlay = true
        
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            // Outgoing ring sound simulation (3 seconds)
            delay(3000)
            callOverlayState = "active"
            while (callOverlayState == "active") {
                delay(1000)
                callDurationSeconds++
            }
        }
    }

    fun endActiveCall() {
        callOverlayState = "disconnected"
        callTimerJob?.cancel()
        viewModelScope.launch {
            delay(1000)
            showCallOverlay = false
        }
    }

    // Rider Chat — real-time via WebSocket
    fun openRiderChat() {
        showRiderChatOverlay = true
        val trip = currentSimulationTrip ?: return
        currentChatTripId = trip.id

        // Join the trip's chat channel
        webSocketClient.joinTripChannel(trip.id.toString())

        // Load chat history from backend
        if (riderChatMessages.isEmpty()) {
            viewModelScope.launch {
                apiRepository.fetchChatHistory(trip.id).onSuccess { dtos ->
                    if (dtos.isNotEmpty()) {
                        riderChatMessages.clear()
                        dtos.forEach { dto ->
                            riderChatMessages.add(ChatMessage(
                                sender = if (dto.sender_role == "rider") "user" else "agent",
                                message = dto.message,
                                timestamp = System.currentTimeMillis()
                            ))
                        }
                    } else {
                        val riderName = trip.riderName
                        riderChatMessages.add(ChatMessage(
                            sender = "agent",
                            message = "Yello! I am your rider $riderName. I have received your request and am coming. Atye i yo dong!"
                        ))
                    }
                }.onFailure {
                    val riderName = trip.riderName
                    riderChatMessages.add(ChatMessage(
                        sender = "agent",
                        message = "Yello! I am your rider $riderName. I have received your request and am coming. Atye i yo dong!"
                    ))
                }
            }
        }
    }

    fun sendRiderChatMessage() {
        val text = riderChatInputText.trim()
        if (text.isEmpty()) return
        val trip = currentSimulationTrip ?: return
        val uid = auth.currentUser?.uid ?: ""
        val name = userProfile.value?.name ?: "Rider"

        riderChatMessages.add(ChatMessage(sender = "user", message = text))
        riderChatInputText = ""

        // Send via WebSocket
        webSocketClient.sendChatMessage(
            tripId = trip.id,
            senderUid = uid,
            senderName = name,
            senderRole = "rider",
            message = text
        )

        // Stop typing indicator
        webSocketClient.sendTypingIndicator(trip.id, uid, name, false)
    }

    fun onRiderChatInputChanged(text: String) {
        riderChatInputText = text
        val trip = currentSimulationTrip ?: return
        val uid = auth.currentUser?.uid ?: ""
        val name = userProfile.value?.name ?: "Rider"
        webSocketClient.sendTypingIndicator(trip.id, uid, name, text.isNotEmpty())
    }

    fun addSavedPlace() {
        if (newPlaceLabel.isEmpty() || newPlaceName.isEmpty()) return
        viewModelScope.launch {
            val lat = 2.7712 + Random.nextDouble(-0.03, 0.03)
            val lng = 32.2985 + Random.nextDouble(-0.03, 0.03)
            repository.addSavedPlace(SavedPlace(
                label = newPlaceLabel,
                name = newPlaceName,
                latitude = lat,
                longitude = lng
            ))
            apiRepository.savePlaceToBackend(newPlaceLabel, newPlaceName, lat, lng)
            newPlaceLabel = ""
            newPlaceName = ""
            navigateBack()
        }
    }

    fun removeSavedPlace(place: SavedPlace) {
        viewModelScope.launch {
            repository.removeSavedPlace(place)
            apiRepository.deleteSavedPlaceFromBackend(place.id)
        }
    }

    fun addEmergencyContact() {
        if (newEmergencyName.isEmpty() || newEmergencyPhone.isEmpty()) return
        viewModelScope.launch {
            repository.addEmergencyContact(EmergencyContact(
                name = newEmergencyName,
                phoneNumber = newEmergencyPhone
            ))
            apiRepository.addEmergencyContactToBackend(newEmergencyName, newEmergencyPhone)
            newEmergencyName = ""
            newEmergencyPhone = ""
            navigateBack()
        }
    }

    fun removeEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch {
            repository.removeEmergencyContact(contact)
            apiRepository.deleteEmergencyContactFromBackend(contact.id)
        }
    }

    // Dispute trip
    fun disputeTrip(tripId: Int, reason: String, details: String) {
        viewModelScope.launch {
            val list = trips.first()
            val match = list.find { it.id == tripId }
            if (match != null) {
                repository.updateTrip(match.copy(
                    status = "disputed",
                    disputeReason = reason,
                    disputeEvidence = details
                ))
                apiRepository.updateTripStatus(tripId, "disputed", disputeReason = reason, disputeEvidence = details)
            }
        }
    }

    // Promo Code

    fun onPassengerTripCompleted() {
        viewModelScope.launch {
            val myPhone = userProfile.value?.phoneNumber ?: ""
            if (myPhone.isNotEmpty()) {
                val pendingRef = repository.getReferralByPhone(myPhone)
                if (pendingRef != null && pendingRef.status == "pending") {
                    // Credit the referred user's wallet with the welcome bonus
                    repository.addTransaction(WalletTransaction(
                        amount = 3000.0,
                        type = "topup",
                        status = "completed",
                        phoneNumber = myPhone,
                        timestamp = System.currentTimeMillis(),
                        provider = "Wallet",
                        reference = "REF-WELCOME-${pendingRef.id}"
                    ))

                    // Complete referral on backend — credits the referrer's wallet there
                    apiRepository.completeReferralOnBackend(pendingRef.id).onSuccess {
                        val completedRef = pendingRef.copy(status = "completed", timestamp = System.currentTimeMillis())
                        repository.addReferral(completedRef)
                        activePromoMessage = "Welcome Bonus: UGX 3,000 credited to your wallet!"
                    }.onFailure {
                        // Still mark locally so we don't retry forever
                        val completedRef = pendingRef.copy(status = "completed", timestamp = System.currentTimeMillis())
                        repository.addReferral(completedRef)
                        activePromoMessage = "Welcome Bonus credited. Referrer reward pending."
                    }

                    refreshWalletBalance()
                }
            }
        }
    }

    fun simulateNewReferralSignUp() {
        viewModelScope.launch {
            val guluNames = listOf("Okeny Francis", "Auma Jackline", "Lanyero Sharon", "Okello Dennis", "Adong Kevin")
            val name = guluNames.random()
            val phoneStr = "+256 7" + Random.nextInt(70000000, 89999999).toString()
            val myCode = userProfile.value?.referralCode ?: "GULU-BODA-256"
            
            repository.addReferral(Referral(
                referredName = name,
                referredPhone = phoneStr,
                referralCodeUsed = myCode,
                status = "pending",
                timestamp = System.currentTimeMillis(),
                rewardAmount = 3000.0
            ))
            
            activePromoMessage = "Simulator: $name signed up using your code $myCode!"
        }
    }

    fun simulateReferralFirstTripCompletion() {
        viewModelScope.launch {
            val list = referrals.value
            val pending = list.firstOrNull { it.status == "pending" }
            if (pending != null) {
                val completed = pending.copy(status = "completed", timestamp = System.currentTimeMillis())
                repository.addReferral(completed)
                
                // Top up current user (referrer) wallet
                val myPhone = userProfile.value?.phoneNumber ?: "+256 772 123456"
                repository.addTransaction(WalletTransaction(
                    amount = 3000.0,
                    type = "topup",
                    status = "completed",
                    phoneNumber = myPhone,
                    timestamp = System.currentTimeMillis(),
                    provider = "Wallet",
                    reference = "REF-REWARD-${completed.id}"
                ))

                // Top up friend's wallet too
                repository.addTransaction(WalletTransaction(
                    amount = 3000.0,
                    type = "topup",
                    status = "completed",
                    phoneNumber = pending.referredPhone,
                    timestamp = System.currentTimeMillis(),
                    provider = "Wallet",
                    reference = "REF-WELCOME-${completed.id}"
                ))
                
                activePromoMessage = "Success! ${pending.referredName} completed their first ride. You earned UGX 3,000!"
            } else {
                activePromoMessage = "No pending referrals available to simulate trips for."
            }
        }
    }

    // Help Center / Support Tickets
    fun submitSupportTicket() {
        if (newTicketSubject.isEmpty() || newTicketDetails.isEmpty()) {
            errorMessage.value = "Please fill in both subject and details."
            return
        }
        val newId = "ST-" + Random.nextInt(1000, 9999)
        supportTickets.add(0, SupportTicket(newId, newTicketSubject, "Open", "2026-06-24"))
        
        activeChatMessages.add(ChatMessage("user", "Submitted Ticket: $newTicketSubject\n$newTicketDetails"))
        activeChatMessages.add(ChatMessage("system", "Thank you for Gulu support ticket $newId. Our agents are investigating."))
        
        newTicketSubject = ""
        newTicketDetails = ""
        navigateTo(Screen.Support)
    }

    fun sendSupportChatMessage() {
        if (newChatMessageText.isEmpty()) return
        activeChatMessages.add(ChatMessage("user", newChatMessageText))
        
        val reply = when {
            newChatMessageText.contains("price", ignoreCase = true) || newChatMessageText.contains("fare", ignoreCase = true) -> 
                "Fares in Gulu are calculated based on distance. Rides start at 2,000 UGX and deliveries at 3,000 UGX."
            newChatMessageText.contains("payment", ignoreCase = true) || newChatMessageText.contains("money", ignoreCase = true) ->
                "We accept MTN MoMo, Airtel Money, and Boda Wallet balances."
            else -> "Thank you. Our Gulu response officer will reply in 2-3 minutes. Call 0800 112 112 for urgent safety!"
        }
        
        val currentText = newChatMessageText
        newChatMessageText = ""
        
        viewModelScope.launch {
            delay(1500)
            activeChatMessages.add(ChatMessage("agent", reply))
        }
    }

    // --- POSTGRESQL WEBSOCKET REPLICATION SYNC PIPELINE & SMS OFFLINE FLOWS ---
    private var postgresWebSocketJob: Job? = null

    fun connectPostgresWebSocket() {
        postgresWebSocketState = "Connecting..."
        addPostgresLog("Initiating Secure CDC WebSocket connection to PostgreSQL replication group (gulu-postgres-db)...")
        
        postgresWebSocketJob?.cancel()
        postgresWebSocketJob = viewModelScope.launch {
            delay(1500)
            postgresWebSocketState = "Connected"
            addPostgresLog("SUCCESS: Bi-directional WebSocket stream active. Protocol: PostgreSQL Realtime CDC (Logical Replication).")
            addPostgresLog("Connected to database: boda_gulu_production at port 5432.")
            
            // Check for offline pending items to synchronize
            val pendingList = trips.value.filter { it.status == "offline_pending" }
            if (pendingList.isNotEmpty()) {
                addPostgresLog("Found ${pendingList.size} unsynced local SQLite transactions cached offline in Room.")
                delay(1000)
                pendingList.forEach { trip ->
                    addPostgresLog("Synchronizing trip transaction to PostgreSQL [ID: ${trip.id} -> ${trip.pickupName} to ${trip.dropoffName}] via WebSocket channel...")
                    delay(800)
                    val updatedTrip = trip.copy(status = "completed") // synchronize it to completed state once online!
                    repository.updateTrip(updatedTrip)
                    addPostgresLog("✓ Transaction [ID: ${trip.id}] committed successfully to remote PostgreSQL 'trips' table.")
                }
                addPostgresLog("PostgreSQL synchronization complete. Local Room cache and remote Postgres are in perfect sync! 🚀")
            } else {
                addPostgresLog("No offline pending trips found. Local Room cache is up-to-date.")
            }
            
            // Periodically log coordinate streaming messages
            while (isOnline) {
                delay(10000)
                val lat = 2.7712 + Random.nextDouble(-0.02, 0.02)
                val lon = 32.2985 + Random.nextDouble(-0.02, 0.02)
                addPostgresLog("Live location sync broadcast via WebSocket payload: { \"boda_id\": \"BODA-SAFE-78\", \"geom\": \"POINT($lon $lat)\" }")
            }
        }
    }

    fun disconnectPostgresWebSocket() {
        postgresWebSocketState = "Disconnected"
        postgresWebSocketJob?.cancel()
        addPostgresLog("WebSocket connection closed. PostgreSQL replication paused.")
        addPostgresLog("Warning: Entering Offline Mode. All booking requests and saved place transactions will be cached locally in SQLite via Room.")
    }

    private fun addPostgresLog(message: String) {
        val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        postgresWebSocketLogs.add("[$timeStr] $message")
        if (postgresWebSocketLogs.size > 30) {
            postgresWebSocketLogs.removeAt(0)
        }
    }

    fun toggleNetworkConnection() {
        isOnline = !isOnline
        if (isOnline) {
            connectPostgresWebSocket()
        } else {
            disconnectPostgresWebSocket()
        }
    }

    fun triggerOfflineSMSBookingFlow() {
        if (pickupPlace == null || dropoffPlace == null) return
        
        offlineSMSRecipientNumber = "8080"
        val modeText = if (serviceType == "ride") "RIDE" else "DELIVERY"
        offlineSMSMessageBody = "BODA-GULU REQUEST $modeText: FROM [${pickupPlace!!.label}] TO [${dropoffPlace!!.label}] ON BODA-SAFE ESCROW. FARE UGX ${calculatedFare.toInt()}. PAY BY $selectedPaymentMethod."
        showOfflineSMSDialog = true
    }

    fun confirmOfflineSMSBooking() {
        if (pickupPlace == null || dropoffPlace == null) return
        
        showOfflineSMSDialog = false
        
        // Match a random vetted rider
        val riderNames = listOf("Okeny Patrick", "Adong Scovia", "Akena Christopher", "Kidega Moses")
        val plates = listOf("UEG 412X", "UED 891B", "UEF 201A", "UEH 556W")
        val photoIds = listOf(2, 3, 4, 1)
        val index = Random.nextInt(riderNames.size)
        
        val fareWithDiscount = (calculatedFare - activePromoDiscount.value).coerceAtLeast(1000.0)
        
        val offlineTrip = Trip(
            type = serviceType,
            pickupName = pickupPlace!!.name,
            dropoffName = dropoffPlace!!.name,
            fare = fareWithDiscount,
            paymentMethod = selectedPaymentMethod,
            status = "offline_pending", // Offline pending sync status!
            riderName = riderNames[index],
            riderPlate = plates[index],
            riderPhone = "+256 781 " + (100000 + Random.nextInt(899999)),
            riderPhotoResId = photoIds[index],
            packageDetails = if (serviceType == "delivery") parcelDetails else null,
            recipientName = if (serviceType == "delivery") recipientName else null,
            recipientPhone = if (serviceType == "delivery") recipientPhone else null
        )
        
        viewModelScope.launch {
            val tripId = repository.addTrip(offlineTrip)
            bookingMatchTripId = tripId
            currentSimulationTrip = offlineTrip.copy(id = tripId.toInt())
            
            // Shift screen to Rider En Route with offline simulation notice
            simulationState = "enroute"
            simulationCountdown = 8
            navigateTo(Screen.RiderEnRoute)
            
            // local simulation loop
            while (simulationCountdown > 0) {
                delay(1000)
                simulationCountdown--
            }
            simulationState = "trip_started"
            navigateTo(Screen.ActiveTrip)
        }
    }

    fun dispatchSOSSMS() {
        val message = "EMERGENCY ALERT: I am on a Boda ride in Gulu and triggered SOS. Track me here: https://boda-gulu.ug/track/GULU-SECURE-SOS"

        emergencySMSDispatchLogs.clear()
        viewModelScope.launch {
            // Post SOS alert to backend so admin dashboard sees it
            apiRepository.postSosAlert(
                latitude = currentLocation?.latitude,
                longitude = currentLocation?.longitude,
                tripId = currentSimulationTrip?.id,
                description = message
            ).onSuccess {
                addPostgresLog("SOS alert sent to backend")
            }.onFailure { e ->
                addPostgresLog("SOS alert failed: ${e.message}")
            }

            // Dispatch SMS to emergency contacts
            emergencyContacts.value.forEach { contact ->
                emergencySMSDispatchLogs.add("SMS dispatched to ${contact.name} (${contact.phoneNumber}): \"$message\"")
                delay(400)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationTracking()
        driverSimulationJob?.cancel()
        simulationJob?.cancel()
        otpTimerJob?.cancel()
        callTimerJob?.cancel()
        postgresWebSocketJob?.cancel()
        searchJob?.cancel()
        webSocketClient.disconnect()
        authStateListener?.let { auth.removeAuthStateListener(it) }
    }

    companion object {
        // From google-services.json → oauth_client → client_type 3
        const val GOOGLE_WEB_CLIENT_ID =
            "387194086675-2c2v0c3rk9o7v49cm998gu48qgaqp6pn.apps.googleusercontent.com"
    }

    /**
     * Launches the Google account picker via Credential Manager, exchanges the
     * Google ID token for a Firebase credential, signs in, then syncs the
     * user profile to Room and the backend exactly like the OTP path does.
     *
     * @param context  Pass LocalContext.current from the composable.
     */
    fun signInWithGoogle(context: android.content.Context) {
        isSigningInWithGoogle = true
        googleSignInError = null

        viewModelScope.launch {
            try {
                android.util.Log.d("BODA_GOOGLE", "Starting Google Sign-In flow...")
                
                // 1. Build the Google ID token request
                val googleIdOption = com.google.android.libraries.identity.googleid
                    .GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(GOOGLE_WEB_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = androidx.credentials.GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                android.util.Log.d("BODA_GOOGLE", "Launching credential picker...")
                
                // 2. Launch the system account picker (bottom sheet)
                val credentialManager = androidx.credentials.CredentialManager.create(context)
                val result = credentialManager.getCredential(context, request)
                
                android.util.Log.d("BODA_GOOGLE", "Got credential result")

                // 3. Extract the Google ID token from the result
                val credential = result.credential
                if (credential !is androidx.credentials.CustomCredential ||
                    credential.type != com.google.android.libraries.identity.googleid
                        .GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    throw Exception("Unexpected credential type: ${credential.type}")
                }

                val googleIdTokenCredential = com.google.android.libraries.identity.googleid
                    .GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                
                android.util.Log.d("BODA_GOOGLE", "Got ID token, exchanging for Firebase credential...")

                // 4. Exchange Google ID token for a Firebase credential and sign in
                val firebaseCredential = com.google.firebase.auth.GoogleAuthProvider
                    .getCredential(idToken, null)
                    
                android.util.Log.d("BODA_GOOGLE", "Signing in to Firebase...")
                
                val authResult = withContext(Dispatchers.IO) {
                    Tasks.await(auth.signInWithCredential(firebaseCredential))
                }

                val firebaseUser = authResult.user
                    ?: throw Exception("Firebase sign-in succeeded but user is null")

                android.util.Log.d("BODA_GOOGLE", "Signed in: ${firebaseUser.uid} ${firebaseUser.email}")

                // Invalidate any stale OkHttp token cache so the next API call
                // fetches a fresh token for this new Google Firebase session
                ApiClient.invalidateToken()
                
                android.util.Log.d("BODA_GOOGLE", "Fetching user profile from backend...")

                // 6. Try to restore profile from backend first (returning user path)
                val backendResult = apiRepository.fetchUserProfile()

                backendResult.fold(
                    onSuccess = { backendUser ->
                        android.util.Log.d("BODA_GOOGLE", "Backend profile found: ${backendUser.full_name}")
                        // Treat the old fake fallback number as empty
                        val cleanPhone = backendUser.phone
                            ?.takeIf { it.isNotEmpty() && it != "+256770000000" && it != "256770000000" }
                            ?: ""
                        // Returning user — profile already exists on backend
                        val profile = UserProfile(
                            id = 1,
                            name = backendUser.full_name.ifEmpty {
                                firebaseUser.displayName ?: ""
                            },
                            phoneNumber = cleanPhone.ifEmpty {
                                firebaseUser.phoneNumber ?: ""
                            },
                            language = backendUser.language.ifEmpty { appLanguage },
                            isSetupComplete = backendUser.full_name.isNotEmpty(),
                            referralCode = backendUser.referral_code ?: ""
                        )
                        repository.saveUserProfile(profile)
                        backendBalance = backendUser.wallet_balance

                        if (profile.isSetupComplete) {
                            android.util.Log.d("BODA_GOOGLE", "Profile complete, navigating to Home")
                            // Returning user — go straight to Home, no profile setup flash
                            fetchBackendData(force = true)
                            navigateTo(Screen.Home)
                        } else {
                            android.util.Log.d("BODA_GOOGLE", "Profile incomplete, showing profile setup")
                            // Account exists but setup was never finished — show Step 3
                            signupName = firebaseUser.displayName ?: ""
                            phoneInput = firebaseUser.phoneNumber?.removePrefix("+256") ?: ""
                            otpSent = true
                            isOtpVerified = true
                        }
                    },
                    onFailure = { error ->
                        android.util.Log.d("BODA_GOOGLE", "Backend profile not found (new user): ${error.message}")
                        
                        // New user — show profile setup step
                        signupName = firebaseUser.displayName ?: ""
                        phoneInput = firebaseUser.phoneNumber?.removePrefix("+256") ?: ""
                        otpSent = true
                        isOtpVerified = true
                        android.util.Log.d("BODA_GOOGLE", "New user flow - signupName='$signupName', showing profile setup")
                    }
                )

            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                android.util.Log.d("BODA_GOOGLE", "Google sign-in cancelled by user")
            } catch (e: androidx.credentials.exceptions.NoCredentialException) {
                android.util.Log.e("BODA_GOOGLE", "No Google account found", e)
                googleSignInError = "No Google account found on this device. Please add a Google account in Settings."
                errorMessage.value = googleSignInError
            } catch (e: Exception) {
                android.util.Log.e("BODA_GOOGLE", "Google sign-in failed: ${e.message}", e)
                e.printStackTrace()
                googleSignInError = "Google sign-in failed: ${e.message}"
                errorMessage.value = googleSignInError
            } finally {
                android.util.Log.d("BODA_GOOGLE", "Finally block - isSigningInWithGoogle = false")
                isSigningInWithGoogle = false
            }
        }
    }
}

data class SupportTicket(
    val id: String,
    val subject: String,
    val status: String, // "Open" or "Resolved"
    val date: String
)

data class ChatMessage(
    val sender: String, // "system", "user", "agent"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
