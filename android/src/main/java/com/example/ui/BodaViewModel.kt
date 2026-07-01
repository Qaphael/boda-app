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
import com.example.ui.ride.*
import com.example.ui.home.connectPostgresWebSocket
import com.example.ui.home.connectToBackend
import com.example.ui.home.stopLocationTracking
import com.example.ui.home.navigateTo
import com.example.ui.home.syncUserToBackend
import kotlin.random.Random

// MOVED TO: ui/navigation/Screen.kt

class BodaViewModel(application: Application) : AndroidViewModel(application) {

    val errorMessage = MutableStateFlow<String?>(null)

    internal val repository: BodaRepository = BodaRepository(AppDatabase.getDatabase(application).bodaDao())
    internal val apiRepository = BodaRepository()
    internal val webSocketClient = WebSocketClient(ApiClient.getWebSocketUrl())
    internal val prefs = application.getSharedPreferences("boda_gulu_prefs", Context.MODE_PRIVATE)

    // Firebase Auth
    internal val auth: FirebaseAuth = FirebaseAuth.getInstance()
    internal var verificationId: String? = null
    internal var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private var hasRestoredSession = false
    

    
    // Ride state
    var currentRideRequest by mutableStateOf<com.example.data.RideRequest?>(null)
        internal set
    var nearbyDrivers by mutableStateOf<List<com.example.data.DriverLocation>>(emptyList())
        internal set
    var isSearchingForDriver by mutableStateOf(false)
        internal set
    
    // Location Services
    internal val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(application)
    internal var locationCallback: LocationCallback? = null
    var currentLocation by mutableStateOf<Location?>(null)
        internal set
    var isLocationTracking by mutableStateOf(false)
        internal set

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
        internal set
    val effectiveBalance: StateFlow<Double> = walletBalance.map { local ->
        backendBalance ?: local
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    var isLoadingData by mutableStateOf(true)
        private set

    var isOtpVerified by mutableStateOf(false)
    var phoneInput by mutableStateOf("")
    var otpInput by mutableStateOf("")
    var otpSent by mutableStateOf(false)

    var triggerPhoneHint by mutableStateOf(false)

    var showWelcomeBonus by mutableStateOf(false)
        internal set
    var isNewUserSession by mutableStateOf(false)
        internal set

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

    internal var lastBackendFetchMs = 0L

    init {
        // Use AuthStateListener to wait for Firebase to resolve auth state before navigating
        authStateListener = object : FirebaseAuth.AuthStateListener {
            override fun onAuthStateChanged(firebaseAuth: FirebaseAuth) {
                val currentUser = firebaseAuth.currentUser
                isOtpVerified = currentUser != null
                if (currentUser != null) {
                    phoneInput = currentUser.phoneNumber?.removePrefix("+256") ?: ""
                    if (!hasRestoredSession) {
                        hasRestoredSession = true
                        viewModelScope.launch {
                            restoreSessionFromBackend()
                        }
                    }
                } else {
                    isLoadingData = false
                }
                // Remove listener after first resolution
                firebaseAuth.removeAuthStateListener(this)
            }
        }
        auth.addAuthStateListener(authStateListener!!)
        connectPostgresWebSocket()
        connectToBackend()
        registerFcmToken()
    }

    internal suspend fun restoreSessionFromBackend() {
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

    internal suspend fun fetchBackendData(force: Boolean = false) {
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

    internal fun decodePolyline(encoded: String): List<com.google.android.gms.maps.model.LatLng> {
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

    // Screen State




    // Screen State
    var currentScreen by mutableStateOf<Screen>(Screen.Splash)
        internal set

    // Navigation Stack (Manual simple backstack for reliability)
    internal val backStack = mutableListOf<Screen>()

    // Language selector state ("en", "ach", "luo")
    var appLanguage by mutableStateOf("en")
        internal set

    // Backend API Functions
    internal fun registerFcmToken() {
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

    // Persistent theme settings: "system", "dark", "light"
    // Persistent theme settings: "system", "dark", "light"
    var appThemeSetting by mutableStateOf(prefs.getString("app_theme_setting", "system") ?: "system")
        internal set

    // Persistent onboarding state flags
    var onboardingCarouselCompleted by mutableStateOf(prefs.getBoolean("onboarding_carousel_completed", false))
        internal set

    var onboardingLanguageSelected by mutableStateOf(prefs.getBoolean("onboarding_language_selected", false))
        internal set

    // Onboarding slide index
    var onboardingSlideIndex by mutableStateOf(0)

    // Phone number input
    var otpResendTimer by mutableStateOf(45)
    var isSendingOtp by mutableStateOf(false)
    var isVerifyingOtp by mutableStateOf(false)
    internal var otpTimerJob: Job? = null

    // Location & notification permission simulation
    var locationPermissionGranted by mutableStateOf(false)
    var notificationPermissionGranted by mutableStateOf(false)

    // Profile inputs
    var signupName by mutableStateOf("")
    var selectedAvatarRes by mutableStateOf(1)

    // Google Sign-In state
    var isSigningInWithGoogle by mutableStateOf(false)
        internal set
    var googleSignInError by mutableStateOf<String?>(null)
        internal set

    // Booking Inputs
    var serviceType by mutableStateOf("ride") // "ride" or "delivery"
    var pickupPlace by mutableStateOf<SavedPlace?>(null)
    var dropoffPlace by mutableStateOf<SavedPlace?>(null)
    var pickupText by mutableStateOf("")
    var dropoffText by mutableStateOf("")
    var searchResults by mutableStateOf<List<SavedPlace>>(emptyList())
    var isSearchingPlaces by mutableStateOf(false)
    var searchError by mutableStateOf<String?>(null)
    internal var searchJob: kotlinx.coroutines.Job? = null
    
    // Google Maps Distance Matrix API states
    var googleDistanceKm by mutableStateOf<Double?>(null)
    var googleDurationMins by mutableStateOf<Int?>(null)
    var isFetchingDistanceMatrix by mutableStateOf(false)
    var distanceMatrixError by mutableStateOf<String?>(null)

    var selectedPaymentMethod by mutableStateOf("MTN") // "MTN", "Airtel", "Wallet"
    var parcelDetails by mutableStateOf("")
    var recipientName by mutableStateOf("")
    var recipientPhone by mutableStateOf("")
    var scheduledBookingDateTime by mutableStateOf<String?>(null)

    internal fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
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
            val rawFare = base + distanceCharge
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
    internal var simulationJob: Job? = null

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
    internal var callTimerJob: Job? = null

    // Interactive Rider Chat Overlay state
    var showRiderChatOverlay by mutableStateOf(false)
    val riderChatMessages = androidx.compose.runtime.mutableStateListOf<ChatMessage>()
    var riderChatInputText by mutableStateOf("")
    var riderIsTyping by mutableStateOf(false)
    internal var currentChatTripId = 0

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
    internal var driverSimulationJob: Job? = null

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

    // --- POSTGRESQL WEBSOCKET REPLICATION SYNC PIPELINE & SMS OFFLINE FLOWS ---
    internal var postgresWebSocketJob: Job? = null

    internal fun addPostgresLog(message: String) {
        val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        postgresWebSocketLogs.add("[$timeStr] $message")
        if (postgresWebSocketLogs.size > 30) {
            postgresWebSocketLogs.removeAt(0)
        }
    }

    // triggerOfflineSMSBookingFlow() → moved to ui/offline/OfflineViewModel.kt

    // confirmOfflineSMSBooking(), dispatchSOSSMS() → moved to ui/offline/OfflineViewModel.kt

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
