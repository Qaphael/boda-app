package com.example.ui

import android.Manifest
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HelpCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import com.example.ui.theme.NunitoFamily
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.*
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// --- CLEAN MINIMALISM SYSTEM COLOR DELEGATOR ---
object Color {
    operator fun invoke(value: Long): ComposeColor {
        return if (com.example.ui.theme.isAppInDarkMode) {
            // Dark Mode mappings (preserve dark style)
            ComposeColor(value)
        } else {
            // Light Mode mappings
            when (value) {
                0xFF0F172A -> ComposeColor(0xFFF1F5F9) // Background slate -> light grey background
                0xFF131A2A -> ComposeColor(0xFFF1F5F9) // Dark variant -> light grey background
                0xFF1E293B -> ComposeColor(0xFFFFFFFF) // Card slate -> pure white card background
                0xFFFDB913 -> ComposeColor(0xFF0061A4) // MTN Yellow -> Gulu Blue
                0xFF334155 -> ComposeColor(0xFFCBD5E1) // Light Slate -> light grey border
                0xFF475569 -> ComposeColor(0xFF0061A4) // Selected blue highlight
                0xFF64748B -> ComposeColor(0xFF475569) // slate-500 -> slate-600 (dark text)
                0xFF94A3B8 -> ComposeColor(0xFF64748B) // slate-400 -> slate-500
                0xFFEF4444 -> ComposeColor(0xFFF97316) // Error Red -> Vivid Orange
                0xFFE4002B -> ComposeColor(0xFFF97316) // Airtel Red -> Vivid Orange
                else -> ComposeColor(value)
            }
        }
    }

    val White: ComposeColor
        get() = if (com.example.ui.theme.isAppInDarkMode) ComposeColor.White else ComposeColor(0xFF0F172A)

    val Black: ComposeColor
        get() = if (com.example.ui.theme.isAppInDarkMode) ComposeColor(0xFF0F172A) else ComposeColor.White

    val Transparent = ComposeColor.Transparent
    val Unspecified = ComposeColor.Unspecified
    val Red = ComposeColor(0xFFF97316)
    val Green = ComposeColor(0xFF10B981)
    val Blue = ComposeColor(0xFF0061A4)
    val Gray = ComposeColor(0xFF64748B)
    val LightGray = ComposeColor(0xFFE2E8F0)
    val DarkGray = ComposeColor(0xFF475569)
}

// Spacing tokens â€” the only spacing values allowed
object Sp {
    val xs  = 4.dp
    val sm  = 8.dp
    val md  = 16.dp
    val lg  = 24.dp
    val xl  = 32.dp
    val xxl = 48.dp
}

// --- TRANSLATIONS FOR GULU LOCALIZATION ---
object BodaLang {
    private val strings = mapOf(
        "en" to mapOf(
            "splash_tagline" to "Pe Yot Gulu! Safe & Quick",
            "btn_continue" to "Continue",
            "btn_back" to "Back",
            "welcome_title_1" to "Gulu's Choice",
            "welcome_desc_1" to "Secure and quick rides or parcel delivery around Gulu City.",
            "welcome_title_2" to "Mobile Money First",
            "welcome_desc_2" to "Pay safely with MTN MoMo or Airtel Money via Boda Escrow.",
            "welcome_title_3" to "Community Verified",
            "welcome_desc_3" to "All Gulu riders are vetted. Share your OTP and track live.",
            "phone_title" to "Enter Phone Number",
            "phone_sub" to "We will send a 6-digit OTP to verify.",
            "otp_title" to "Verify Number",
            "otp_sub" to "Enter 6-digit code sent. Simulation code is 123456.",
            "otp_resend" to "Resend code in",
            "profile_title" to "Profile Setup",
            "profile_desc" to "Enter your name to register as a new Gulu customer.",
            "perm_location" to "Location Permission Required",
            "perm_location_desc" to "Needed to pinpoint your pickup in Gulu Main Market, Layibi, etc.",
            "perm_notify" to "Notification Permission",
            "perm_notify_desc" to "Needed to alert you when your Boda rider arrives.",
            "home_title" to "Boda Gulu",
            "ride" to "Ride",
            "delivery" to "Delivery",
            "where_to" to "Where are you going?",
            "pickup" to "Pickup Location",
            "dropoff" to "Drop-off Location",
            "confirm_booking" to "Confirm Booking",
            "searching_rider" to "Searching for rider...",
            "cancel" to "Cancel",
            "rider_enroute" to "Rider En Route",
            "rider_arrived" to "Rider Arrived!",
            "active_trip" to "Active Trip",
            "share_location" to "Share Location",
            "sos" to "SOS EMERGENCY",
            "trip_completed" to "Trip Completed!",
            "rate_rider" to "Rate your Rider",
            "comment_hint" to "Write feedback (optional)",
            "wallet" to "Wallet",
            "history" to "History",
            "support" to "Support",
            "profile" to "Profile"
        ),
        "ach" to mapOf(
            "splash_tagline" to "Pe Yot Gulu! Ber ki Yot",
            "btn_continue" to "Deyo",
            "btn_back" to "Cen",
            "welcome_title_1" to "Yer me Gulu",
            "welcome_desc_1" to "Kwo ma ber, kwano ma ror dong i Gulu.",
            "welcome_title_2" to "Mony me Cing",
            "welcome_desc_2" to "Cul me MTN MoMo ki Airtel ma nongo tye mukene.",
            "welcome_title_3" to "Ngat ma oketo nyinge",
            "welcome_desc_3" to "Lu boda duto ki Gulu kitye ma olony. Nyut OTP ni.",
            "phone_title" to "Ket Namba Telepon",
            "phone_sub" to "Wabi oro OTP me dwan namba ni.",
            "otp_title" to "Lony Namba Telepon",
            "otp_sub" to "Ket code namba 6. Code me sim tye 123456.",
            "otp_resend" to "Resend code i",
            "profile_title" to "Yub Profile",
            "profile_desc" to "Ket nyingi me lony i app me Boda Gulu.",
            "perm_location" to "Yero me Location",
            "perm_location_desc" to "Mito me lony kama itye iye i Gulu.",
            "perm_notify" to "Yero me Obot",
            "perm_notify_desc" to "Mito me oro lok i namba ni ka rider oo.",
            "home_title" to "Boda Gulu",
            "ride" to "Bor",
            "delivery" to "Tero",
            "where_to" to "Itye maito kwene?",
            "pickup" to "Kama gonyo iye",
            "dropoff" to "Kama iter iye",
            "confirm_booking" to "Moko Tero",
            "searching_rider" to "Kitye me kwayo rider...",
            "cancel" to "Juko",
            "rider_enroute" to "Rider Tye i Yo",
            "rider_arrived" to "Rider Oo Dong!",
            "active_trip" to "Loko i Yo",
            "share_location" to "Mi Location",
            "sos" to "SOS EMERGENCY",
            "trip_completed" to "Tero otum dong!",
            "rate_rider" to "Ngolo Nying Rider",
            "comment_hint" to "Ket lok me tito (poto pin)",
            "wallet" to "Wallet",
            "history" to "Orek",
            "support" to "Kony",
            "profile" to "Lok me Name"
        ),
        "luo" to mapOf(
            "splash_tagline" to "Pe Yot Gulu! Yot yo Gulu",
            "btn_continue" to "Kadi",
            "btn_back" to "Doki",
            "welcome_title_1" to "Yero me Gulu",
            "welcome_desc_1" to "Boda ride kede parcel delivery duto i Gulu.",
            "welcome_title_2" to "Sim MoMo",
            "welcome_desc_2" to "Culu kede MTN MoMo kede Airtel Money maber.",
            "welcome_title_3" to "Community Olony",
            "welcome_desc_3" to "Olu boda wa duto i Gulu kitye maber.",
            "phone_title" to "Ket Namba Telepon",
            "phone_sub" to "Wabioro OTP me lony.",
            "otp_title" to "Lony Namba",
            "otp_sub" to "Ket code 123456 me sim.",
            "otp_resend" to "Resend code i",
            "profile_title" to "Profile Setup",
            "profile_desc" to "Ket nyingi maber me tyeko profile ni.",
            "perm_location" to "Location Permission Required",
            "perm_location_desc" to "Pinpoint pickup point.",
            "perm_notify" to "Notification Permission",
            "perm_notify_desc" to "Alert you when rider arrives.",
            "home_title" to "Boda Gulu",
            "ride" to "Rider",
            "delivery" to "Parcel",
            "where_to" to "Okene itye maito?",
            "pickup" to "Kama gonyo iye",
            "dropoff" to "Kama itero iye",
            "confirm_booking" to "Confirm Booking",
            "searching_rider" to "Searching Boda...",
            "cancel" to "Cancel",
            "rider_enroute" to "Rider Tye i Yo",
            "rider_arrived" to "Rider Odu dong!",
            "active_trip" to "Active Trip",
            "share_location" to "Share Location",
            "sos" to "SOS EMERGENCY",
            "trip_completed" to "Trip completed!",
            "rate_rider" to "Rate Rider",
            "comment_hint" to "Enter comment",
            "wallet" to "Wallet",
            "history" to "History",
            "support" to "Support",
            "profile" to "Profile"
        )
    )

    fun get(lang: String, key: String): String {
        return strings[lang]?.get(key) ?: strings["en"]?.get(key) ?: key
    }
}

// --- DESIGN SYSTEM REUSABLE COMPONENTS ---
@Composable
fun BodaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    containerColor: ComposeColor = Color(0xFFFDB913),
    contentColor: ComposeColor = ComposeColor.Black,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    testTag: String? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = Color(0xFF1E293B).copy(alpha = 0.5f),
            disabledContentColor = Color(0xFF64748B)
        ),
        modifier = modifier
            .height(52.dp)
            .widthIn(max = 600.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = contentColor,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = contentColor
                    )
                    Spacer(modifier = Modifier.width(Sp.sm))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = contentColor
                    ),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun BodaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: ComposeColor = Color(0xFF334155),
    contentColor: ComposeColor = Color.White,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    testTag: String? = null
) {
    BodaButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        containerColor = containerColor,
        contentColor = contentColor,
        icon = icon,
        testTag = testTag
    )
}

@Composable
fun BodaOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: ComposeColor = Color(0xFF334155),
    contentColor: ComposeColor = Color.White,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    testTag: String? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor,
            disabledContentColor = Color(0xFF64748B)
        ),
        border = BorderStroke(1.5.dp, borderColor),
        modifier = modifier
            .height(52.dp)
            .widthIn(max = 600.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(Sp.sm))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = contentColor
                ),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BodaTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: ComposeColor = Color.White,
    testTag: String? = null
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = contentColor,
            disabledContentColor = Color(0xFF64748B)
        ),
        modifier = modifier
            .heightIn(min = 48.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = contentColor
            ),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
fun BodaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    testTag: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            ),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder?.let { { Text(it, color = Color(0xFF64748B)) } },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            keyboardOptions = keyboardOptions,
            singleLine = singleLine,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF1E293B),
                unfocusedContainerColor = Color(0xFF1E293B),
                focusedBorderColor = Color(0xFFFDB913),
                unfocusedBorderColor = Color(0xFF334155),
                focusedLabelColor = Color(0xFFFDB913),
                unfocusedLabelColor = Color(0xFF94A3B8)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        )
    }
}

@Composable
fun BodaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    border: BorderStroke? = null,
    testTag: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = modifier
        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    
    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        ),
        shape = RoundedCornerShape(16.dp),
        border = border ?: BorderStroke(1.dp, Color(0xFF334155)),
        content = content
    )
}

// --- MAIN ENTRANCE APP VIEW ---
@Composable
fun BodaAppContent(viewModel: BodaViewModel) {
    val context = LocalContext.current
    val user by viewModel.userProfile.collectAsState()
    val savedPlaces by viewModel.savedPlaces.collectAsState()
    val trips by viewModel.trips.collectAsState()
    val txns by viewModel.transactions.collectAsState()
    val contacts by viewModel.emergencyContacts.collectAsState()
    val balance by viewModel.effectiveBalance.collectAsState()
    val referralsList by viewModel.referrals.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val errMsg by viewModel.errorMessage.collectAsState()

    LaunchedEffect(errMsg) {
        errMsg?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
            viewModel.errorMessage.value = null
        }
    }

    // Read initial setup flag and language
    LaunchedEffect(user) {
        user?.let {
            viewModel.updateLanguage(it.language)
            if (viewModel.currentScreen == Screen.Splash) {
                if (it.isSetupComplete) {
                    viewModel.navigateTo(Screen.Home)
                } else {
                    viewModel.navigateTo(Screen.WelcomeOnboarding)
                }
            }
        } ?: run {
            if (viewModel.currentScreen == Screen.Splash) {
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                if (firebaseUser != null) {
                    // Firebase session exists — wait for fetchBackendData() to populate Room
                    // Do NOT navigate yet; LaunchedEffect will fire again when Room emits the profile
                } else {
                    viewModel.navigateTo(Screen.WelcomeOnboarding)
                }
            }
        }
    }
    
    // Start location tracking when permission is granted
    LaunchedEffect(viewModel.locationPermissionGranted) {
        if (viewModel.locationPermissionGranted) {
            viewModel.startLocationTracking()
        }
    }

    // Edge-to-Edge System Navigation Guard
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(Color(0xFF0F172A)) // Custom dark theme background
            },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (viewModel.currentScreen in listOf(Screen.Home, Screen.TripsHistory, Screen.Wallet, Screen.ProfileSettings)) {
                BodaBottomNavigation(viewModel)
            }
        }
    ) { innerPadding ->
        ProvideTextStyle(TextStyle(fontFamily = NunitoFamily)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen Dispatcher
            when (viewModel.currentScreen) {
                is Screen.Splash -> SplashScreen(viewModel)
                is Screen.WelcomeOnboarding -> OnboardingScreen(viewModel)
                is Screen.Home -> HomeScreen(viewModel, savedPlaces)
                is Screen.SearchPlaces -> SearchPlacesScreen(viewModel, savedPlaces)
                is Screen.RoutePreview -> RoutePreviewScreen(viewModel, balance)
                is Screen.Matching -> MatchingScreen(viewModel)
                is Screen.RiderEnRoute -> RiderEnRouteScreen(viewModel)
                is Screen.ActiveTrip -> ActiveTripScreen(viewModel)
                is Screen.PostTrip -> PostTripScreen(viewModel)
                is Screen.TripsHistory -> TripsHistoryScreen(viewModel, trips)
                is Screen.Wallet -> WalletScreen(viewModel, balance, txns)
                is Screen.ProfileSettings -> ProfileSettingsScreen(viewModel, user, contacts)
                is Screen.EmergencyContacts -> EmergencyContactsScreen(viewModel, contacts)
                is Screen.SavedPlacesManage -> SavedPlacesManageScreen(viewModel, savedPlaces)
                is Screen.Support -> SupportScreen(viewModel)
                is Screen.DriverOnboarding -> DriverOnboardingScreen(viewModel)
                is Screen.Referrals -> ReferralsScreen(viewModel, referralsList)
            }

            // System States Overlays (resiliency & security)
            if (viewModel.isOffline) {
                OfflineBanner { viewModel.isOffline = false }
            }
            if (viewModel.showDowntimeNotice) {
                SystemOverlayDialog(
                    title = "System Maintenance",
                    desc = "Boda Gulu is undergoing scheduled network upgrades for Airtel Money. Back in 30 minutes.",
                    cta = "Understand",
                    onDismiss = { viewModel.showDowntimeNotice = false }
                )
            }
            if (viewModel.showSuspendedNotice) {
                SystemOverlayDialog(
                    title = "Account Suspended",
                    desc = "Your Boda account is suspended due to unpaid matching cancellation fees. Contact Gulu Main office to appeal.",
                    cta = "Appeal Suspension",
                    onDismiss = { viewModel.showSuspendedNotice = false }
                )
            }

            // Interactive Mock overlays
            if (viewModel.showOfflineSMSDialog) {
                OfflineSMSBookingOverlay(viewModel)
            }
            if (viewModel.showMoMoPinDialog) {
                MoMoPinDialog(viewModel)
            }
            if (viewModel.showCallOverlay) {
                CallOverlay(viewModel)
            }
            if (viewModel.showRiderChatOverlay) {
                RiderChatOverlay(viewModel)
            }
        }
        } // ProvideTextStyle
    }
}

// --- GOOGLE MAPS LATLNG UTILS ---
fun getLatLngForPlace(name: String): com.google.android.gms.maps.model.LatLng {
    val s = name.lowercase()
    return when {
        s.contains("university") || s.contains("laroo") -> com.google.android.gms.maps.model.LatLng(2.7842, 32.3214)
        s.contains("hospital") || s.contains("lacor") -> com.google.android.gms.maps.model.LatLng(2.7933, 32.2571)
        s.contains("market") || s.contains("home") || s.contains("cereleno") || s.contains("roundabout") -> com.google.android.gms.maps.model.LatLng(2.7712, 32.2985)
        s.contains("stadium") || s.contains("pece") -> com.google.android.gms.maps.model.LatLng(2.7745, 32.3112)
        else -> com.google.android.gms.maps.model.LatLng(2.7750, 32.2950)
    }
}

fun generateDetailedRoute(start: com.google.android.gms.maps.model.LatLng, end: com.google.android.gms.maps.model.LatLng): List<com.google.android.gms.maps.model.LatLng> {
    val path = mutableListOf<com.google.android.gms.maps.model.LatLng>()
    path.add(start)
    val latDiff = end.latitude - start.latitude
    val lngDiff = end.longitude - start.longitude
    
    // Create detailed grid route following streets (3 intermediate waypoints)
    val p1 = com.google.android.gms.maps.model.LatLng(start.latitude + latDiff * 0.4, start.longitude)
    val p2 = com.google.android.gms.maps.model.LatLng(start.latitude + latDiff * 0.4, start.longitude + lngDiff * 0.6)
    val p3 = com.google.android.gms.maps.model.LatLng(end.latitude, start.longitude + lngDiff * 0.6)
    
    path.add(p1)
    path.add(p2)
    path.add(p3)
    path.add(end)
    return path
}

fun getLatLngOnPath(path: List<com.google.android.gms.maps.model.LatLng>, progress: Float): com.google.android.gms.maps.model.LatLng {
    if (path.isEmpty()) return com.google.android.gms.maps.model.LatLng(2.775, 32.295)
    if (path.size == 1) return path[0]
    if (progress <= 0f) return path.first()
    if (progress >= 1f) return path.last()
    
    val totalSegments = path.size - 1
    val segmentProgress = progress * totalSegments
    val currentSegmentIndex = segmentProgress.toInt().coerceAtMost(totalSegments - 1)
    val localProgress = segmentProgress - currentSegmentIndex
    
    val startPt = path[currentSegmentIndex]
    val endPt = path[currentSegmentIndex + 1]
    
    return com.google.android.gms.maps.model.LatLng(
        startPt.latitude + (endPt.latitude - startPt.latitude) * localProgress,
        startPt.longitude + (endPt.longitude - startPt.longitude) * localProgress
    )
}

@Composable
fun GoogleMapViewWrapper(
    modifier: Modifier = Modifier,
    pickupLatLng: com.google.android.gms.maps.model.LatLng,
    dropoffLatLng: com.google.android.gms.maps.model.LatLng?,
    riderProgress: Float,
    simulationState: String,
    routePoints: List<com.google.android.gms.maps.model.LatLng> = emptyList()
) {
    val context = LocalContext.current
    val mapView = remember { com.google.android.gms.maps.MapView(context) }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = {
            mapView.apply {
                onCreate(android.os.Bundle())
                onResume()
                getMapAsync { googleMap ->
                    googleMap.uiSettings.isZoomControlsEnabled = true
                    googleMap.uiSettings.isMapToolbarEnabled = false
                    googleMap.mapType = com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL
                }
            }
        },
        modifier = modifier,
        update = { mapV ->
            mapV.getMapAsync { googleMap ->
                googleMap.clear()

                // Add Pickup Marker
                googleMap.addMarker(
                    com.google.android.gms.maps.model.MarkerOptions()
                        .position(pickupLatLng)
                        .title("Pickup Point")
                        .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN))
                )

                if (dropoffLatLng != null) {
                    // Add Drop-off Marker
                    googleMap.addMarker(
                        com.google.android.gms.maps.model.MarkerOptions()
                            .position(dropoffLatLng)
                            .title("Drop-off Point")
                            .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED))
                    )

                    // Calculate detailed route
                    val detailedRoute = if (routePoints.isNotEmpty()) {
                        routePoints
                    } else {
                        generateDetailedRoute(pickupLatLng, dropoffLatLng)
                    }

                    // Draw route path polyline following streets
                    val polylineOpts = com.google.android.gms.maps.model.PolylineOptions()
                        .color(android.graphics.Color.parseColor("#FDB913"))
                        .width(8f)
                    detailedRoute.forEach { pt ->
                        polylineOpts.add(pt)
                    }
                    googleMap.addPolyline(polylineOpts)

                    // Draw simulated vehicle
                    if (simulationState in listOf("enroute", "accepted", "active")) {
                        val vehicleLatLng = if (simulationState == "active") {
                            getLatLngOnPath(detailedRoute, riderProgress)
                        } else {
                            // En route: moving from Gulu center to pickup
                            val centerLat = 2.775
                            val centerLng = 32.295
                            val enrouteStart = com.google.android.gms.maps.model.LatLng(centerLat, centerLng)
                            val enrouteRoute = generateDetailedRoute(enrouteStart, pickupLatLng)
                            getLatLngOnPath(enrouteRoute, riderProgress)
                        }

                        googleMap.addMarker(
                            com.google.android.gms.maps.model.MarkerOptions()
                                .position(vehicleLatLng)
                                .title("Boda Motorcycle")
                                .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_ORANGE))
                        )
                    }

                    // Setup camera zoom to fit both pickup and dropoff
                    try {
                        val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
                            .include(pickupLatLng)
                            .include(dropoffLatLng)
                            .build()
                        googleMap.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(bounds, 120))
                    } catch (e: Exception) {
                        googleMap.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(pickupLatLng, 14f))
                    }
                } else {
                    // Just zoom to pickup
                    googleMap.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(pickupLatLng, 15f))
                }
            }
        }
    )
}

// --- DUAL MAP SYSTEM DISPATCHER ---
@Composable
fun GuluMapView(
    modifier: Modifier = Modifier,
    pickup: SavedPlace? = null,
    dropoff: SavedPlace? = null,
    riderProgress: Float = 0f,
    simulationState: String = "idle",
    isDriverMode: Boolean = false,
    driverTripState: String = "none",
    driverPickupName: String? = null,
    driverDropoffName: String? = null,
    driverProgress: Float = 0f,
    viewModel: BodaViewModel? = null
) {
    val hasMapsApiKey = try {
        com.example.BuildConfig.MAPS_API_KEY.isNotEmpty() && 
        com.example.BuildConfig.MAPS_API_KEY != "MY_MAPS_API_KEY" && 
        com.example.BuildConfig.MAPS_API_KEY != "MAPS_API_KEY_DEFAULT_VALUE"
    } catch (e: Throwable) {
        false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
    ) {
        if (hasMapsApiKey) {
            val pickupLatLng = if (isDriverMode && driverPickupName != null) {
                getLatLngForPlace(driverPickupName)
            } else if (pickup != null) {
                com.google.android.gms.maps.model.LatLng(pickup.latitude, pickup.longitude)
            } else {
                com.google.android.gms.maps.model.LatLng(2.775, 32.295)
            }

            val dropoffLatLng = if (isDriverMode && driverDropoffName != null) {
                getLatLngForPlace(driverDropoffName)
            } else if (dropoff != null) {
                com.google.android.gms.maps.model.LatLng(dropoff.latitude, dropoff.longitude)
            } else {
                null
            }

            val progress = if (isDriverMode) driverProgress else riderProgress
            val activeState = if (isDriverMode) driverTripState else simulationState

            GoogleMapViewWrapper(
                modifier = Modifier.fillMaxSize(),
                pickupLatLng = pickupLatLng,
                dropoffLatLng = dropoffLatLng,
                riderProgress = progress,
                simulationState = activeState,
                routePoints = viewModel?.osrmRoutePoints ?: emptyList()
            )

            // Live status badge overlay (Removed)
        } else {
            // Render beautiful interactive Vector map
            GuluCanvasMapView(
                modifier = Modifier.fillMaxSize(),
                pickup = pickup,
                dropoff = dropoff,
                riderProgress = riderProgress,
                simulationState = simulationState,
                isDriverMode = isDriverMode,
                driverTripState = driverTripState,
                driverPickupName = driverPickupName,
                driverDropoffName = driverDropoffName,
                driverProgress = driverProgress
            )

            // Education overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B).copy(alpha = 0.9f))
                    .padding(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = ComposeColor(0xFFFDB913), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(Sp.sm))
                    Text(
                        "Set MAPS_API_KEY in Secrets Panel to unlock live Google Maps",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// --- INTERACTIVE GULU VECTOR MAP ---
@Composable
fun GuluCanvasMapView(
    modifier: Modifier = Modifier,
    pickup: SavedPlace? = null,
    dropoff: SavedPlace? = null,
    riderProgress: Float = 0f,
    simulationState: String = "idle",
    isDriverMode: Boolean = false,
    driverTripState: String = "none",
    driverPickupName: String? = null,
    driverDropoffName: String? = null,
    driverProgress: Float = 0f
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A))
    ) {
        val w = size.width
        val h = size.height

        // 1. Draw Gulu Street Grid (Custom Coordinates for budget efficiency)
        val streetColor = Color(0xFF334155)
        val roadStroke = 4.dp.toPx()

        // Ring Road Around Gulu
        drawCircle(
            color = streetColor,
            radius = w * 0.35f,
            center = Offset(w * 0.5f, h * 0.5f),
            style = Stroke(width = roadStroke)
        )

        // Gulu Main Roundabout - Center of Gulu
        drawCircle(
            color = Color(0xFF475569),
            radius = w * 0.08f,
            center = Offset(w * 0.5f, h * 0.5f),
            style = Stroke(width = roadStroke)
        )

        // Gulu Main Streets (Intersecting at Roundabout)
        // Gulu-Kampala Road (North-South)
        drawLine(
            color = streetColor,
            start = Offset(w * 0.5f, 0f),
            end = Offset(w * 0.5f, h),
            strokeWidth = roadStroke
        )
        // Joka Road / Pece Road (East-West)
        drawLine(
            color = streetColor,
            start = Offset(0f, h * 0.5f),
            end = Offset(w, h * 0.5f),
            strokeWidth = roadStroke
        )

        // Laroo Road (Diagonal towards University)
        drawLine(
            color = streetColor,
            start = Offset(w * 0.5f, h * 0.5f),
            end = Offset(w, h * 0.1f),
            strokeWidth = roadStroke
        )

        // Lacor Hospital Road (Diagonal West)
        drawLine(
            color = streetColor,
            start = Offset(w * 0.5f, h * 0.5f),
            end = Offset(0f, h * 0.3f),
            strokeWidth = roadStroke
        )

        // 2. Draw Landmark Reference Nodes in Gulu
        val landmarkColor = Color(0xFF64748B)
        // Gulu Main Market (Center Roundabout)
        drawCircle(Color(0xFFFDB913), radius = 8.dp.toPx(), center = Offset(w * 0.52f, h * 0.52f))
        // Lacor Hospital (West)
        drawCircle(landmarkColor, radius = 6.dp.toPx(), center = Offset(w * 0.12f, h * 0.34f))
        // Gulu University (East Laroo)
        drawCircle(landmarkColor, radius = 6.dp.toPx(), center = Offset(w * 0.88f, h * 0.18f))
        // Pece Stadium (South-East)
        drawCircle(landmarkColor, radius = 6.dp.toPx(), center = Offset(w * 0.68f, h * 0.68f))

        // Coordinates resolver based on name/label keywords
        fun getCoords(label: String, name: String, isPickup: Boolean): Offset {
            val s = (label + " " + name).lowercase()
            return when {
                s.contains("university") || s.contains("laroo") || s.contains("school") -> Offset(w * 0.88f, h * 0.18f)
                s.contains("hospital") || s.contains("lacor") || s.contains("layibi") || s.contains("work") -> Offset(w * 0.12f, h * 0.34f)
                s.contains("market") || s.contains("home") || s.contains("cereleno") || s.contains("roundabout") -> Offset(w * 0.52f, h * 0.52f)
                s.contains("stadium") || s.contains("pece") -> Offset(w * 0.68f, h * 0.68f)
                else -> if (isPickup) Offset(w * 0.4f, h * 0.45f) else Offset(w * 0.65f, h * 0.58f)
            }
        }

        val activePickup: Offset?
        val activeDropoff: Offset?
        val activeProgress: Float
        val activeState: String

        if (isDriverMode) {
            activePickup = if (driverPickupName != null) getCoords("", driverPickupName, true) else null
            activeDropoff = if (driverDropoffName != null) getCoords("", driverDropoffName, false) else null
            activeProgress = driverProgress
            activeState = driverTripState
        } else {
            activePickup = if (pickup != null) getCoords(pickup.label, pickup.name, true) else null
            activeDropoff = if (dropoff != null) getCoords(dropoff.label, dropoff.name, false) else null
            activeProgress = riderProgress
            activeState = simulationState
        }

        fun generateCanvasDetailedRoute(start: Offset, end: Offset): List<Offset> {
            val path = mutableListOf<Offset>()
            path.add(start)
            val dx = end.x - start.x
            val dy = end.y - start.y
            
            val p1 = Offset(start.x + dx * 0.4f, start.y)
            val p2 = Offset(start.x + dx * 0.4f, start.y + dy * 0.6f)
            val p3 = Offset(end.x, start.y + dy * 0.6f)
            
            path.add(p1)
            path.add(p2)
            path.add(p3)
            path.add(end)
            return path
        }

        fun getOffsetOnPath(path: List<Offset>, progress: Float): Offset {
            if (path.isEmpty()) return Offset(0f, 0f)
            if (path.size == 1) return path[0]
            if (progress <= 0f) return path.first()
            if (progress >= 1f) return path.last()
            
            val totalSegments = path.size - 1
            val segmentProgress = progress * totalSegments
            val currentSegmentIndex = segmentProgress.toInt().coerceAtMost(totalSegments - 1)
            val localProgress = segmentProgress - currentSegmentIndex
            
            val startPt = path[currentSegmentIndex]
            val rPt = path[currentSegmentIndex + 1]
            
            return Offset(
                startPt.x + (rPt.x - startPt.x) * localProgress,
                startPt.y + (rPt.y - startPt.y) * localProgress
            )
        }

        // Draw route path line
        if (activePickup != null && activeDropoff != null && activeState != "none" && activeState != "idle") {
            val canvasRoute = generateCanvasDetailedRoute(activePickup, activeDropoff)
            val routePath = androidx.compose.ui.graphics.Path().apply {
                moveTo(canvasRoute.first().x, canvasRoute.first().y)
                for (i in 1 until canvasRoute.size) {
                    lineTo(canvasRoute[i].x, canvasRoute[i].y)
                }
            }
            drawPath(
                path = routePath,
                color = Color(0xFFFDB913),
                style = Stroke(
                    width = 5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                )
            )

            // Draw pickup node (Vibrant Green)
            drawCircle(Color(0xFF10B981), radius = 7.dp.toPx(), center = activePickup)
            // Draw dropoff node (Airtel Red)
            drawCircle(Color(0xFFE4002B), radius = 7.dp.toPx(), center = activeDropoff)

            // Draw animated moving Boda
            if (activeState in listOf("enroute", "accepted", "pickup_arrived", "active")) {
                val currentX: Float
                val currentY: Float

                if (activeState == "enroute" || activeState == "accepted") {
                    val startX = w * 0.5f
                    val startY = h * 0.5f
                    val enrouteStart = Offset(startX, startY)
                    val enrouteRoute = generateCanvasDetailedRoute(enrouteStart, activePickup)
                    val vehicleOffset = getOffsetOnPath(enrouteRoute, activeProgress)
                    currentX = vehicleOffset.x
                    currentY = vehicleOffset.y
                } else if (activeState == "active") {
                    val vehicleOffset = getOffsetOnPath(canvasRoute, activeProgress)
                    currentX = vehicleOffset.x
                    currentY = vehicleOffset.y
                } else {
                    currentX = activePickup.x
                    currentY = activePickup.y
                }

                // Pulsing highlight ring under boda rider
                drawCircle(
                    color = Color(0xFF0061A4).copy(alpha = 0.4f),
                    radius = 16.dp.toPx(),
                    center = Offset(currentX, currentY)
                )
                // Main rider marker
                drawCircle(
                    color = Color(0xFFFDB913),
                    radius = 9.dp.toPx(),
                    center = Offset(currentX, currentY)
                )
                drawCircle(
                    color = Color.Black,
                    radius = 4.dp.toPx(),
                    center = Offset(currentX, currentY)
                )
            }
        }
    }
}

// --- WIDGET: OFFLINE RESILIENCY BANNER ---
@Composable
fun OfflineBanner(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEF4444))
            .padding(vertical = 10.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(Sp.sm))
            Text("Connection lost. Operating in Gulu Offline Cache Mode.", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Text("Dismiss", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onClose() })
    }
}

// --- WIDGET: OVERLAY INFO SYSTEM DIALOG ---
@Composable
fun SystemOverlayDialog(title: String, desc: String, cta: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        BodaCard(
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFDB913), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(Sp.md))
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(Sp.sm))
                Text(desc, color = Color(0xFF94A3B8), fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
                Spacer(modifier = Modifier.height(Sp.lg))
                BodaButton(
                    text = cta,
                    onClick = { onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// --- SCREEN 1: SPLASH SCREEN ---
@Composable
fun SplashScreen(viewModel: BodaViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.boda_logo),
                contentDescription = "Boda Gulu Brand Logo",
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(Sp.md))
            Text(
                "Boda Gulu",
                color = Color(0xFFFDB913),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(Sp.xs))
            Text(
                BodaLang.get(viewModel.appLanguage, "splash_tagline"),
                color = Color(0xFF64748B),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(Sp.xxl))
            CircularProgressIndicator(
                color = Color(0xFFFDB913),
                strokeWidth = 3.dp,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// --- SCREEN 2: ONBOARDING ENGINE ---
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OnboardingScreen(viewModel: BodaViewModel) {
    if (!viewModel.onboardingCarouselCompleted) {
        // Step 1: 3-slide onboarding carousel introducing Boda
        val slideTitle = when (viewModel.onboardingSlideIndex) {
            0 -> BodaLang.get(viewModel.appLanguage, "welcome_title_1")
            1 -> BodaLang.get(viewModel.appLanguage, "welcome_title_2")
            else -> BodaLang.get(viewModel.appLanguage, "welcome_title_3")
        }
        val slideDesc = when (viewModel.onboardingSlideIndex) {
            0 -> BodaLang.get(viewModel.appLanguage, "welcome_desc_1")
            1 -> BodaLang.get(viewModel.appLanguage, "welcome_desc_2")
            else -> BodaLang.get(viewModel.appLanguage, "welcome_desc_3")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Top row: Skip Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Skip",
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable { viewModel.completeOnboardingCarousel() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("skip_onboarding_btn")
                )
            }

            Spacer(modifier = Modifier.height(Sp.xl))

            // Center visual illustration Card/Box based on slide
            BodaCard(
                modifier = Modifier.size(240.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val imageRes = when (viewModel.onboardingSlideIndex) {
                        0 -> R.drawable.img_onboarding_ride
                        1 -> R.drawable.img_onboarding_payment
                        else -> R.drawable.img_onboarding_delivery
                    }
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = slideTitle,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(Sp.xl))

            // Slide title & description
            Text(
                text = slideTitle,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Sp.sm))
            Text(
                text = slideDesc,
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Slide Dots indicator
            Spacer(modifier = Modifier.height(Sp.xl))
            Row(horizontalArrangement = Arrangement.Center) {
                for (i in 0..2) {
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .size(if (viewModel.onboardingSlideIndex == i) 14.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (viewModel.onboardingSlideIndex == i) Color(0xFFFDB913) else Color(0xFF475569))
                    )
                }
            }

            Spacer(modifier = Modifier.height(Sp.lg))

            // Navigation Controls at the bottom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (viewModel.onboardingSlideIndex > 0) {
                    BodaSecondaryButton(
                        text = BodaLang.get(viewModel.appLanguage, "btn_back"),
                        onClick = { viewModel.onboardingSlideIndex-- },
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                }

                BodaButton(
                    text = if (viewModel.onboardingSlideIndex < 2) BodaLang.get(viewModel.appLanguage, "btn_continue") else "Get Started",
                    onClick = {
                        if (viewModel.onboardingSlideIndex < 2) {
                            viewModel.onboardingSlideIndex++
                        } else {
                            viewModel.completeOnboardingCarousel()
                        }
                    },
                    modifier = Modifier.weight(1f).padding(start = if (viewModel.onboardingSlideIndex > 0) 8.dp else 0.dp),
                    testTag = "next_onboarding_btn"
                )
            }
        }
    } else if (!viewModel.onboardingLanguageSelected) {
        // Step 2: Allows the user to choose their preferred app language before registering. Shown after Welcome slides on first launch.
        var selectedLang by remember { mutableStateOf(viewModel.appLanguage) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(Sp.lg))
            
            // Icon / Header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = null,
                    tint = Color(0xFFFDB913),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(Sp.lg))

            Text(
                text = "Select Preferred Language",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(Sp.sm))
            
            Text(
                text = "Choose your preferred dialect for using Boda Gulu. You can also adjust this later in settings.",
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.weight(0.1f))

            // Language cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val languages = listOf(
                    Triple("en", "English", "ðŸ‡¬ðŸ‡§ Standard international English interface"),
                    Triple("ach", "Acholi / Luo", "ðŸ‡ºðŸ‡¬ Local Acholi Gulu dialect localization"),
                    Triple("luo", "Lango / Luo", "ðŸ‡ºðŸ‡¬ Local Lango northern dialect localization")
                )

                languages.forEach { (code, label, desc) ->
                    val isSelected = selectedLang == code
                    BodaCard(
                        onClick = { selectedLang = code },
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFFFDB913) else Color(0xFF334155)
                        ),
                        testTag = "lang_card_$code"
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color(0xFFFDB913) else Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when(code) {
                                        "en" -> "EN"
                                        "ach" -> "ACH"
                                        else -> "LUO"
                                    },
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(Sp.md))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.height(Sp.xs))
                                Text(
                                    text = desc,
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = Color(0xFFFDB913),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Sp.lg))

            BodaButton(
                text = "Confirm & Register",
                onClick = { viewModel.completeOnboardingLanguage(selectedLang) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "confirm_lang_btn"
            )
        }
    } else {
        // Step 3: Standard Registration & Verification Flow
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Clean registration header
            Spacer(modifier = Modifier.height(Sp.md))
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.boda_logo),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(Sp.lg))

            if (!viewModel.otpSent) {
                // STEP 1: Phone number entry (+256)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(BodaLang.get(viewModel.appLanguage, "phone_title"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(Sp.xs))
                    Text(BodaLang.get(viewModel.appLanguage, "phone_sub"), color = Color(0xFF64748B), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(Sp.md))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                                .height(56.dp)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+256", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(Sp.sm))
                        OutlinedTextField(
                            value = viewModel.phoneInput,
                            onValueChange = { viewModel.phoneInput = it.take(9) },
                            placeholder = { Text("772 123456", color = Color(0xFF475569)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedBorderColor = Color(0xFFFDB913),
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("phone_input")
                        )
                    }
                    Spacer(modifier = Modifier.height(Sp.lg))
                    BodaButton(
                        text = "Get Verification Code",
                        onClick = {
                            if (viewModel.phoneInput.length >= 9) {
                                viewModel.startOtpFlow()
                            }
                        },
                        enabled = viewModel.phoneInput.length >= 9 && !viewModel.isSendingOtp,
                        loading = viewModel.isSendingOtp,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "send_otp_btn"
                    )

                    Spacer(modifier = Modifier.height(Sp.md))

                    BodaOutlinedButton(
                        text = "LOGIN / REGISTER AS DRIVER",
                        onClick = { viewModel.startDriverOnboarding() },
                        borderColor = Color(0xFFFDB913),
                        contentColor = Color(0xFFFDB913),
                        icon = Icons.Default.TwoWheeler,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else if (!viewModel.isOtpVerified) {
                // STEP 2: OTP Verification Screen
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(BodaLang.get(viewModel.appLanguage, "otp_title"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(Sp.xs))
                    Text(BodaLang.get(viewModel.appLanguage, "otp_sub"), color = Color(0xFF64748B), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(Sp.md))
                    OutlinedTextField(
                        value = viewModel.otpInput,
                        onValueChange = { viewModel.otpInput = it.take(6) },
                        placeholder = { Text("Enter 123456", color = Color(0xFF475569)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B),
                            focusedBorderColor = Color(0xFFFDB913),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("otp_input")
                    )
                    Spacer(modifier = Modifier.height(Sp.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (viewModel.otpResendTimer > 0) {
                            Text(
                                "${BodaLang.get(viewModel.appLanguage, "otp_resend")} ${viewModel.otpResendTimer}s",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp
                            )
                        } else {
                            Text(
                                "Resend Code Now",
                                color = if (viewModel.isSendingOtp) Color(0xFF64748B) else Color(0xFFFDB913),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable(enabled = !viewModel.isSendingOtp) { viewModel.startOtpFlow() }
                            )
                        }
                        Text(
                            "Change Number",
                            color = if (viewModel.isSendingOtp) Color(0xFF64748B) else Color(0xFFE4002B),
                            fontSize = 12.sp,
                            modifier = Modifier.clickable(enabled = !viewModel.isSendingOtp) { viewModel.otpSent = false }
                        )
                    }
                    Spacer(modifier = Modifier.height(Sp.lg))
                    BodaButton(
                        text = "Verify & Continue",
                        onClick = { viewModel.verifyOtp() },
                        enabled = viewModel.otpInput.length == 6 && !viewModel.isVerifyingOtp,
                        loading = viewModel.isVerifyingOtp,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "verify_otp_btn"
                    )
                }
            } else {
                // STEP 3: Profile Setup, Permissions & Value explanation
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(BodaLang.get(viewModel.appLanguage, "profile_title"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(BodaLang.get(viewModel.appLanguage, "profile_desc"), color = Color(0xFF64748B), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(Sp.md))
                    BodaTextField(
                        value = viewModel.signupName,
                        onValueChange = { viewModel.signupName = it },
                        label = "Full Name",
                        placeholder = "Enter your full name",
                        testTag = "name_input"
                    )

                    Spacer(modifier = Modifier.height(Sp.md))
                    BodaTextField(
                        value = viewModel.referralCodeInput,
                        onValueChange = { viewModel.referralCodeInput = it },
                        label = "Referral Code (Optional)",
                        placeholder = "e.g. GULU-BODA-256",
                        testTag = "referral_code_input"
                    )

                    // Select Avatar Choices
                    Spacer(modifier = Modifier.height(Sp.md))
                    Text("Select Avatar:", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(Sp.sm))
                    Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                        listOf(1, 2, 3, 4).forEach { id ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (viewModel.selectedAvatarRes == id) Color(0xFFFDB913) else Color(0xFF1E293B))
                                    .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                    .clickable { viewModel.selectedAvatarRes = id }
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                            }
                        }
                    }

                    // Explain and grant Location
                    Spacer(modifier = Modifier.height(Sp.md))
                    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
                    BodaCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MyLocation, contentDescription = null, tint = if (locationPermissionState.status.isGranted) Color(0xFF10B981) else Color(0xFFFDB913))
                                Spacer(modifier = Modifier.width(Sp.sm))
                                Text(BodaLang.get(viewModel.appLanguage, "perm_location"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(Sp.sm))
                            Text(BodaLang.get(viewModel.appLanguage, "perm_location_desc"), color = Color(0xFF94A3B8), fontSize = 12.sp, lineHeight = 16.sp)
                            Spacer(modifier = Modifier.height(Sp.sm))
                            BodaButton(
                                text = if (locationPermissionState.status.isGranted) "Location Access Granted!" else "Grant Gulu Location Access",
                                onClick = {
                                    viewModel.locationPermissionGranted = true
                                    if (!locationPermissionState.status.isGranted) {
                                        locationPermissionState.launchPermissionRequest()
                                    }
                                },
                                containerColor = if (locationPermissionState.status.isGranted) Color(0xFF10B981) else Color(0xFF334155),
                                contentColor = Color.White,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Explain and grant Notifications
                    Spacer(modifier = Modifier.height(Sp.md))
                    val notifPermissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
                    BodaCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = if (notifPermissionState.status.isGranted) Color(0xFF10B981) else Color(0xFFFDB913))
                                Spacer(modifier = Modifier.width(Sp.sm))
                                Text(BodaLang.get(viewModel.appLanguage, "perm_notify"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(Sp.sm))
                            Text(BodaLang.get(viewModel.appLanguage, "perm_notify_desc"), color = Color(0xFF94A3B8), fontSize = 12.sp, lineHeight = 16.sp)
                            Spacer(modifier = Modifier.height(Sp.sm))
                            BodaButton(
                                text = if (notifPermissionState.status.isGranted) "Notifications Granted!" else "Enable Arrival Notifications",
                                onClick = {
                                    viewModel.notificationPermissionGranted = true
                                    if (!notifPermissionState.status.isGranted) {
                                        notifPermissionState.launchPermissionRequest()
                                    }
                                },
                                containerColor = if (notifPermissionState.status.isGranted) Color(0xFF10B981) else Color(0xFF334155),
                                contentColor = Color.White,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Sp.lg))
                    val locationGranted = locationPermissionState.status.isGranted
                    val notifGranted = notifPermissionState.status.isGranted
                    LaunchedEffect(locationGranted, notifGranted) {
                        if (locationGranted) viewModel.locationPermissionGranted = true
                        if (notifGranted) viewModel.notificationPermissionGranted = true
                    }
                    BodaButton(
                        text = "Register & Open App",
                        onClick = { viewModel.completeProfileSetup() },
                        enabled = viewModel.signupName.isNotEmpty() && locationGranted && notifGranted,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "complete_setup_btn"
                    )
                }
            }
        }
    }
}

// --- SCREEN 2.5: BODA GULU DRIVER COCKPIT SCREEN ---
@Composable
fun DriverHomeScreen(viewModel: BodaViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        val activeTrip = viewModel.driverActiveTrip
        val incomingReq = viewModel.driverIncomingRequest
        
        val pName = when {
            activeTrip != null -> activeTrip.pickupName
            incomingReq != null -> incomingReq.pickupName
            else -> null
        }
        val dName = when {
            activeTrip != null -> activeTrip.dropoffName
            incomingReq != null -> incomingReq.dropoffName
            else -> null
        }

        // Dedicated Vector Map with Driver Overlays
        GuluMapView(
            modifier = Modifier.fillMaxSize(),
            isDriverMode = true,
            driverTripState = viewModel.driverTripState,
            driverPickupName = pName,
            driverDropoffName = dName,
            driverProgress = viewModel.driverSimulationProgress,
            viewModel = viewModel
        )

        // Top Status Header Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                    .border(1.dp, Color(0xFF334155), CircleShape)
                    .clickable { viewModel.navigateTo(Screen.ProfileSettings) }
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
            }
        }

        // Bottom Board Tray Card
        Card(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 340.dp)
                .align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF334155))
                )
                Spacer(modifier = Modifier.height(Sp.sm))

                when (viewModel.driverTripState) {
                    "none" -> {
                        if (!viewModel.isDriverOnline) {
                            Text(
                                "Apwoyo! Welcome to Gulu Boda Driver",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(Sp.xs))
                            Text(
                                "Go online to start matching with active passenger trips and cargo deliveries in Gulu.",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(Sp.md))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                DriverMiniStat(title = "Trips Done", value = "${viewModel.driverCompletedTrips}", icon = Icons.Default.TwoWheeler)
                                DriverMiniStat(title = "Boda Rating", value = "â­ ${viewModel.driverRating}", icon = Icons.Default.Star)
                                DriverMiniStat(title = "Net Income", value = "UGX ${viewModel.driverEarnings.toInt()}", icon = Icons.Default.Payments)
                            }
                            Spacer(modifier = Modifier.height(Sp.md))

                            BodaButton(
                                text = "GO ONLINE",
                                onClick = { viewModel.toggleDriverOnline() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                "Radar Active - Scanning Gulu...",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(Sp.sm))
                            LinearProgressIndicator(
                                color = Color(0xFFFDB913),
                                trackColor = Color(0xFF334155),
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.height(Sp.sm))
                            Text(
                                "Standing by for orders. New matching Boda requests across Gulu streets will appear shortly.",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(Sp.md))
                            BodaButton(
                                text = "GO OFFLINE",
                                onClick = { viewModel.toggleDriverOnline() },
                                containerColor = Color(0xFFEF4444),
                                contentColor = ComposeColor.White,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    "requested" -> {
                        val req = viewModel.driverIncomingRequest
                        if (req != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFFDB913))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        req.type.uppercase(),
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                Text("INCOMING TRIP OFFER", color = Color(0xFFE4002B), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(Sp.sm))
                            Text(
                                text = req.riderName,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Phone Linkage: ${req.riderPhone}",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(Sp.sm))
                            
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(Sp.sm))
                                Text(req.pickupName, color = Color.White, fontSize = 14.sp, maxLines = 1)
                            }
                            Spacer(modifier = Modifier.height(Sp.sm))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFE4002B), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(Sp.sm))
                                Text(req.dropoffName, color = Color.White, fontSize = 14.sp, maxLines = 1)
                            }

                            if (req.type == "delivery") {
                                Spacer(modifier = Modifier.height(Sp.sm))
                                BodaCard {
                                    Text(
                                        "Delivery Package: ${req.packageDetails ?: "Market Supplies"}",
                                        color = Color(0xFFFDB913),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(Sp.sm))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ESTIMATED PAYOUT", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("UGX ${req.fare.toInt()}", color = Color(0xFF10B981), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.height(Sp.md))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                BodaSecondaryButton(
                                    text = "PASS",
                                    onClick = { viewModel.driverRejectTrip() },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(Sp.sm))
                                BodaButton(
                                    text = "ACCEPT",
                                    onClick = { viewModel.driverAcceptTrip() },
                                    modifier = Modifier.weight(2f)
                                )
                            }
                        }
                    }
                    "accepted" -> {
                        val active = viewModel.driverActiveTrip
                        if (active != null) {
                            Text(
                                "Simulating Rider Pickup Path...",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(Sp.xs))
                            Text(
                                "Client: ${active.riderName} â€¢ ${viewModel.driverSimulationCountdown}s Arrival ETA",
                                color = Color(0xFFFDB913),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(Sp.sm))
                            LinearProgressIndicator(
                                progress = viewModel.driverSimulationProgress,
                                color = Color(0xFF10B981),
                                trackColor = Color(0xFF334155),
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                            )
                            Spacer(modifier = Modifier.height(Sp.sm))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(Sp.sm))
                                Text(active.pickupName, color = Color.White, fontSize = 12.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(Sp.md))
                            BodaButton(
                                text = "FORCE ARRIVED",
                                onClick = { viewModel.driverArrivePickup() },
                                containerColor = Color(0xFF10B981),
                                contentColor = ComposeColor.Black,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    "pickup_arrived" -> {
                        val active = viewModel.driverActiveTrip
                        if (active != null) {
                            Text(
                                "Arrived at Pickup!",
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(Sp.xs))
                            Text(
                                "Passenger is waiting. Verify Security Match OTP: 1234",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(Sp.md))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Passenger Name:", color = Color(0xFF64748B), fontSize = 12.sp)
                                Text(active.riderName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(Sp.sm))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Guaranteed Fare Payout:", color = Color(0xFF64748B), fontSize = 12.sp)
                                Text("UGX ${active.fare.toInt()}", color = Color(0xFFFDB913), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(Sp.md))

                            BodaButton(
                                text = "START TRANSIT",
                                onClick = { viewModel.driverStartTrip() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    "active" -> {
                        val active = viewModel.driverActiveTrip
                        if (active != null) {
                            Text(
                                "En Route to Destination...",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(Sp.xs))
                            Text(
                                "Dropoff: ${active.dropoffName} â€¢ ${viewModel.driverSimulationCountdown}s remaining",
                                color = Color(0xFFFDB913),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(Sp.sm))
                            
                            // Navigation Card for Driver Cockpit
                            BodaCard(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Navigation, contentDescription = null, tint = Color(0xFFFDB913), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(Sp.sm))
                                    Text(
                                        text = getActiveNavigationStep(active.pickupName, active.dropoffName, viewModel.driverSimulationProgress, viewModel.navigationSteps),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Speedometer card
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(Sp.sm))
                                    Text("Cockpit Speed: ${getSimulatedSpeed(viewModel.driverSimulationProgress, viewModel.trafficCondition)} km/h", color = Color.White, fontSize = 11.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("BODA-SAFE LIMIT", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            LinearProgressIndicator(
                                progress = viewModel.driverSimulationProgress,
                                color = Color(0xFFFDB913),
                                trackColor = Color(0xFF334155),
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                            )
                            Spacer(modifier = Modifier.height(Sp.md))
                            BodaButton(
                                text = "ARRIVED & COMPLETE",
                                onClick = { viewModel.driverCompleteTrip() },
                                containerColor = Color(0xFF10B981),
                                contentColor = ComposeColor.Black,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DriverMiniStat(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    BodaCard(
        modifier = Modifier.width(100.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFFFDB913), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(Sp.sm))
            Text(title, color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        }
    }
}

// --- SCREEN 3: HOME & BOOKING MAP SCREEN ---
@Composable
fun HomeScreen(viewModel: BodaViewModel, savedPlaces: List<SavedPlace>) {
    if (viewModel.isDriverMode) {
        DriverHomeScreen(viewModel)
    } else {
        PassengerHomeScreen(viewModel, savedPlaces)
    }
}

@Composable
fun PassengerHomeScreen(viewModel: BodaViewModel, savedPlaces: List<SavedPlace>) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Full Vector Map Backdrop
        GuluMapView(
            modifier = Modifier.fillMaxSize(),
            pickup = viewModel.pickupPlace,
            dropoff = viewModel.dropoffPlace,
            viewModel = viewModel
        )

        // Top Row Status / Language / Profile Avatar Quick-Jump
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFDB913))
                    .clickable { viewModel.navigateTo(Screen.ProfileSettings) },
                contentAlignment = Alignment.Center
            ) {
                val name = viewModel.userProfile.collectAsState().value?.name ?: "?"
                val initials = name.split(" ")
                    .filter { it.isNotEmpty() }
                    .take(2)
                    .joinToString("") { it.first().uppercase() }
                Text(initials, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Column(horizontalAlignment = Alignment.End) {
                val userName = viewModel.userProfile.collectAsState().value?.name?.split(" ")?.firstOrNull() ?: ""
                val bal = viewModel.effectiveBalance.collectAsState().value
                val hour = java.time.LocalTime.now().hour
                val greeting = when {
                    hour < 12 -> "Good morning"
                    hour < 17 -> "Good afternoon"
                    else -> "Good evening"
                }
                Text("$greeting, $userName", color = Color(0xFF94A3B8), fontSize = 10.sp)
                Text("UGX ${bal.toInt().toString().reversed().chunked(3).joinToString(",").reversed()}",
                    color = Color(0xFFFDB913), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Bottom Booking Tray Card
        Card(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 340.dp)
                .align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF334155))
                )
                Spacer(modifier = Modifier.height(Sp.sm))

                // Segmented Selector: Ride vs Delivery
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .padding(4.dp)
                ) {
                    val isRide = viewModel.serviceType == "ride"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isRide) Color(0xFFFDB913) else Color.Transparent)
                            .clickable { viewModel.serviceType = "ride" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = if (isRide) Color.Black else Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(Sp.sm))
                            Text(BodaLang.get(viewModel.appLanguage, "ride"), color = if (isRide) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isRide) Color(0xFFFDB913) else Color.Transparent)
                            .clickable { viewModel.serviceType = "delivery" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, tint = if (!isRide) Color.Black else Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(Sp.sm))
                            Text(BodaLang.get(viewModel.appLanguage, "delivery"), color = if (!isRide) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Sp.md))

                // Pickup location click card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .clickable { viewModel.navigateTo(Screen.SearchPlaces) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF10B981))
                    Spacer(modifier = Modifier.width(Sp.sm))
                    Column {
                        Text(BodaLang.get(viewModel.appLanguage, "pickup"), color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(viewModel.pickupPlace?.name ?: "Set current Gulu pickup...", color = Color.White, fontSize = 14.sp, maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(Sp.sm))

                // Drop-off location click card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .clickable { viewModel.navigateTo(Screen.SearchPlaces) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFE4002B))
                    Spacer(modifier = Modifier.width(Sp.sm))
                    Column {
                        Text(BodaLang.get(viewModel.appLanguage, "dropoff"), color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(viewModel.dropoffPlace?.name ?: BodaLang.get(viewModel.appLanguage, "where_to"), color = Color.White, fontSize = 14.sp, maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(Sp.md))

                if (savedPlaces.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(savedPlaces.take(5)) { place ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (viewModel.dropoffPlace?.id == place.id) Color(0xFFFDB913) else Color(0xFF1E293B))
                                    .border(0.5.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
                                    .clickable {
                                        viewModel.dropoffPlace = place
                                        viewModel.dropoffText = place.name
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(place.label, fontSize = 11.sp,
                                    color = if (viewModel.dropoffPlace?.id == place.id) Color.Black else Color(0xFF94A3B8),
                                    fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // If both are chosen, navigate forward
                if (viewModel.pickupPlace != null && viewModel.dropoffPlace != null) {
                    Spacer(modifier = Modifier.height(Sp.md))
                    BodaButton(
                        text = "Calculate Boda Fare Estimate",
                        onClick = { viewModel.navigateTo(Screen.RoutePreview) },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "preview_route_btn"
                    )
                }
            }
        }
    }
}

// --- SCREEN 4: SEARCH PLACES ---
@Composable
fun SearchPlacesScreen(viewModel: BodaViewModel, savedPlaces: List<SavedPlace>) {
    val defaultSuggestions = listOf(
        SavedPlace(label = "Home", name = "Gulu Main Market, Gulu", latitude = 2.7712, longitude = 32.2985),
        SavedPlace(label = "Work", name = "Lacor Hospital, Gulu", latitude = 2.7933, longitude = 32.2571),
        SavedPlace(label = "University", name = "Gulu University, Laroo", latitude = 2.7842, longitude = 32.3214),
        SavedPlace(label = "Stadium", name = "Pece Stadium, Gulu", latitude = 2.7745, longitude = 32.3112),
        SavedPlace(label = "Town Hall", name = "Gulu Town Hall, Gulu", latitude = 2.7720, longitude = 32.3005),
        SavedPlace(label = "Airfield", name = "Gulu Airfield, Gulu", latitude = 2.7961, longitude = 32.2801)
    )

    // Determine initial focused field based on what's empty
    var activeFocus by remember { 
        mutableStateOf(if (viewModel.pickupPlace == null) "pickup" else "dropoff") 
    }

    // Active query text
    val activeQuery = if (activeFocus == "pickup") viewModel.pickupText else viewModel.dropoffText

    // Trigger debounced real search
    LaunchedEffect(activeQuery) {
        viewModel.searchLocations(activeQuery)
    }

    // Filtered places: use real-time search results if query is typed, otherwise default lists
    val filteredPlaces = if (activeQuery.trim().length >= 2) {
        viewModel.searchResults
    } else {
        (savedPlaces + defaultSuggestions).distinctBy { it.name }.filter {
            it.label.contains(activeQuery, ignoreCase = true) ||
            it.name.contains(activeQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(Sp.sm))
            Text("Select Locations", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(Sp.md))

        // Pickup query
        BodaTextField(
            value = viewModel.pickupText,
            onValueChange = { 
                viewModel.pickupText = it
                activeFocus = "pickup"
            },
            label = "Pickup Location" + if (activeFocus == "pickup") " ðŸŸ¢ (Searching...)" else "",
            placeholder = "Search pickup location...",
            leadingIcon = { Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF10B981)) },
            trailingIcon = if (viewModel.pickupText.isNotEmpty() || viewModel.pickupPlace != null) {
                {
                    IconButton(onClick = {
                        viewModel.pickupText = ""
                        viewModel.pickupPlace = null
                        activeFocus = "pickup"
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (it.isFocused) activeFocus = "pickup" }
        )

        Spacer(modifier = Modifier.height(Sp.sm))

        // Dropoff query
        BodaTextField(
            value = viewModel.dropoffText,
            onValueChange = { 
                viewModel.dropoffText = it
                activeFocus = "dropoff"
            },
            label = "Drop-off Destination" + if (activeFocus == "dropoff") " ðŸ”´ (Searching...)" else "",
            placeholder = "Where are you heading?",
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFE4002B)) },
            trailingIcon = if (viewModel.dropoffText.isNotEmpty() || viewModel.dropoffPlace != null) {
                {
                    IconButton(onClick = {
                        viewModel.dropoffText = ""
                        viewModel.dropoffPlace = null
                        activeFocus = "dropoff"
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (it.isFocused) activeFocus = "dropoff" }
        )

        Spacer(modifier = Modifier.height(Sp.md))

        // --- NEW OPTION: CURRENT LOCATION ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E293B))
                .clickable {
                    val currentLocation = SavedPlace(
                        label = "Current Location",
                        name = "My Current Location (Pece, Gulu)",
                        latitude = 2.7750,
                        longitude = 32.2950
                    )
                    if (activeFocus == "pickup") {
                        viewModel.pickupPlace = currentLocation
                        viewModel.pickupText = currentLocation.name
                        activeFocus = "dropoff"
                    } else {
                        viewModel.dropoffPlace = currentLocation
                        viewModel.dropoffText = currentLocation.name
                        if (viewModel.pickupPlace != null) {
                            viewModel.navigateTo(Screen.RoutePreview)
                        } else {
                            activeFocus = "pickup"
                        }
                    }
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF10B981))
            Spacer(modifier = Modifier.width(Sp.sm))
            Column {
                Text("Use Current Location", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Gulu City Center coordinate (2.7750, 32.2950)", color = Color(0xFF10B981), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(Sp.md))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (activeFocus == "pickup") "Pickup Suggestions (${filteredPlaces.size})" else "Drop-off Suggestions (${filteredPlaces.size})",
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (viewModel.isSearchingPlaces) {
                CircularProgressIndicator(
                    color = Color(0xFFFDB913),
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(Sp.sm))
                Text("Searching map...", color = Color(0xFFFDB913), fontSize = 11.sp)
            }
        }
        Spacer(modifier = Modifier.height(Sp.sm))

        LazyColumn(modifier = Modifier.weight(1f)) {
            if (filteredPlaces.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No matching Gulu locations found", color = Color(0xFF64748B), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(Sp.sm))
                        Text("Try typing Lacor, Market, Pece, or University", color = Color(0xFF475569), fontSize = 12.sp)
                    }
                }
            } else {
                items(filteredPlaces) { place ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (activeFocus == "pickup") {
                                    viewModel.pickupPlace = place
                                    viewModel.pickupText = place.name
                                    activeFocus = "dropoff"
                                } else {
                                    viewModel.dropoffPlace = place
                                    viewModel.dropoffText = place.name
                                    if (viewModel.pickupPlace != null) {
                                        viewModel.navigateTo(Screen.RoutePreview)
                                    } else {
                                        activeFocus = "pickup"
                                    }
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isSaved = savedPlaces.any { it.name == place.name }
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.Place,
                            contentDescription = null,
                            tint = if (isSaved) Color(0xFF10B981) else Color(0xFFFDB913)
                        )
                        Spacer(modifier = Modifier.width(Sp.sm))
                        Column {
                            Text(place.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(place.name, color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(color = Color(0xFF1E293B))
                }
            }
        }

        Spacer(modifier = Modifier.height(Sp.sm))

        // Sim map pin selection option
        BodaSecondaryButton(
            text = "Select Location via Map Pin (Simulator)",
            onClick = {
                val randomPlace = defaultSuggestions.random()
                if (activeFocus == "pickup") {
                    viewModel.pickupPlace = randomPlace
                    viewModel.pickupText = randomPlace.name
                    activeFocus = "dropoff"
                } else {
                    viewModel.dropoffPlace = randomPlace
                    viewModel.dropoffText = randomPlace.name
                    if (viewModel.pickupPlace != null) {
                        viewModel.navigateTo(Screen.RoutePreview)
                    } else {
                        activeFocus = "pickup"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// --- SCREEN 5: ROUTE PREVIEW & PRICE ESTIMATES ---
@Composable
fun RoutePreviewScreen(viewModel: BodaViewModel, walletBalance: Double) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(Sp.sm))
            Text("Confirm Ride Booking", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(Sp.sm))

        // Vector Map showing route preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            GuluMapView(
                modifier = Modifier.fillMaxSize(),
                pickup = viewModel.pickupPlace,
                dropoff = viewModel.dropoffPlace,
                viewModel = viewModel
            )
        }

        Spacer(modifier = Modifier.height(Sp.sm))

        // Pickup / Dropoff nodes summary
        BodaCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF10B981)))
                    Spacer(modifier = Modifier.width(Sp.sm))
                    Text(viewModel.pickupPlace?.name ?: "", color = Color.White, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(Sp.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFF97316)))
                    Spacer(modifier = Modifier.width(Sp.sm))
                    Text(viewModel.dropoffPlace?.name ?: "", color = Color.White, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(Sp.sm))

        // PARCEL FORM IF SERVICE TYPE IS DELIVERY
        if (viewModel.serviceType == "delivery") {
            BodaCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Delivery Package Details", color = Color(0xFFFDB913), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(Sp.sm))
                    BodaTextField(
                        value = viewModel.parcelDetails,
                        onValueChange = { viewModel.parcelDetails = it },
                        label = "Item details",
                        placeholder = "What is in the package? (e.g. food, documents)",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(Sp.sm))
                    Row {
                        BodaTextField(
                            value = viewModel.recipientName,
                            onValueChange = { viewModel.recipientName = it },
                            label = "Name",
                            placeholder = "Recipient Name",
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(Sp.sm))
                        BodaTextField(
                            value = viewModel.recipientPhone,
                            onValueChange = { viewModel.recipientPhone = it },
                            label = "Phone",
                            placeholder = "Recipient Phone",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(Sp.sm))
        }

        // Schedule ride panel
        BodaCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.scheduledBookingDateTime = "Today, 18:30"
                    }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(Sp.sm))
                    Column {
                        Text("Schedule for Later", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(viewModel.scheduledBookingDateTime ?: "Leave now (Immediate booking)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(Sp.sm))

        // PAYMENT METHD SELECTOR: MTN / Airtel / Wallet
        BodaCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Select MTN / Airtel / Wallet Payment", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(Sp.sm))
                val walletSufficient = walletBalance >= viewModel.calculatedFare - viewModel.activePromoDiscount.value
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    listOf("MTN" to "MTN MoMo", "Airtel" to "Airtel", "Wallet" to "Boda Wallet").forEach { (method, label) ->
                        val isSelected = viewModel.selectedPaymentMethod == method
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFFDB913) else Color(0xFF334155))
                                .clickable { viewModel.selectedPaymentMethod = method }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                if (method == "Wallet") {
                                    Text(
                                        text = "UGX ${walletBalance.toInt()}",
                                        color = if (walletSufficient) Color(0xFF10B981) else Color(0xFFF87171),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Sp.sm))

        // Promo Code Row
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BodaButton(
                text = "Apply",
                onClick = { viewModel.validatePromoViaBackend(viewModel.promoCodeInput) },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(Sp.sm))
            Box(modifier = Modifier.weight(2f)) {
                BodaTextField(
                    value = viewModel.promoCodeInput,
                    onValueChange = { viewModel.promoCodeInput = it },
                    label = "Promo Code",
                    placeholder = "e.g. GULU3000",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (viewModel.activePromoMessage.isNotEmpty()) {
            Text(viewModel.activePromoMessage, color = Color(0xFF10B981), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(modifier = Modifier.height(Sp.md))

        // INTERACTIVE TRAFFIC CONDITIONS SELECTOR
        BodaCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Simulate Traffic Conditions",
                    color = Color(0xFFFDB913),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(Sp.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val trafficLevels = listOf(
                        "Light" to "35 km/h",
                        "Moderate" to "25 km/h",
                        "Heavy" to "15 km/h",
                        "Rush Hour" to "8 km/h"
                    )
                    trafficLevels.forEach { (level, speed) ->
                        val isSelected = viewModel.trafficCondition == level
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFFDB913) else Color(0xFF0F172A))
                                .clickable { viewModel.trafficCondition = level }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = level,
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(Sp.xs))
                            Text(
                                text = speed,
                                color = if (isSelected) Color.Black.copy(alpha = 0.7f) else Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Sp.sm))

        // DETAILED ETA & METRICS DASHBOARD
        val arrivalTimeStr = remember(viewModel.calculatedTimeMinutes) {
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.MINUTE, viewModel.calculatedTimeMinutes)
            java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(cal.time)
        }

        BodaCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ESTIMATED ROUTE TIME", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${viewModel.calculatedTimeMinutes} mins",
                            color = Color(0xFFFDB913),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("EST. ARRIVAL TIME", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = arrivalTimeStr,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 10.dp), thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Street Distance", color = Color(0xFF64748B), fontSize = 11.sp)
                        Text(
                            text = "${"%.2f".format(viewModel.calculatedDistanceKm)} km",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Traffic Condition", color = Color(0xFF64748B), fontSize = 11.sp)
                        Text(
                            text = viewModel.trafficCondition,
                            color = when (viewModel.trafficCondition) {
                                "Light" -> Color(0xFF10B981)
                                "Moderate" -> Color(0xFF3B82F6)
                                "Heavy" -> Color(0xFFF97316)
                                else -> Color(0xFFEF4444)
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Delay Factor", color = Color(0xFF64748B), fontSize = 11.sp)
                        val delayVal = when (viewModel.trafficCondition) {
                            "Light" -> "None"
                            "Moderate" -> "+2 mins"
                            "Heavy" -> "+5 mins"
                            else -> "+9 mins"
                        }
                        Text(
                            text = delayVal,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Sp.sm))

        // FARE ESTIMATE — Hero element with breakdown
        BodaCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TOTAL FARE", color = Color(0xFF64748B), fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                val discounted = (viewModel.calculatedFare - viewModel.activePromoDiscount.value).coerceAtLeast(1000.0)
                Text("UGX ${discounted.toInt()}",
                    color = Color(0xFFFDB913), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${viewModel.calculatedDistanceKm.let { "%.1f".format(it) }} km",
                        color = Color(0xFF64748B), fontSize = 11.sp)
                    Text("·", color = Color(0xFF334155), fontSize = 11.sp)
                    Text("${viewModel.calculatedTimeMinutes} min",
                        color = Color(0xFF64748B), fontSize = 11.sp)
                    Text("·", color = Color(0xFF334155), fontSize = 11.sp)
                    Text(viewModel.trafficCondition, color = Color(0xFFF59E0B), fontSize = 11.sp)
                }
                HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 10.dp))
                val baseFare = if (viewModel.serviceType == "ride") 1500.0 else 2500.0
                val distCharge = viewModel.calculatedDistanceKm * 1000.0
                val surge = when (viewModel.trafficCondition) {
                    "Light" -> 0.0; "Moderate" -> 500.0; "Heavy" -> 1500.0; "Rush Hour" -> 2500.0; else -> 500.0
                }
                listOf("Base" to "UGX ${baseFare.toInt()}",
                    "Distance" to "UGX ${distCharge.toInt()}",
                    "Traffic" to "+ UGX ${surge.toInt()}").forEach { (lbl, val_) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(lbl, color = Color(0xFF64748B), fontSize = 11.sp)
                        Text(val_, color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(3.dp))
                }
                if (viewModel.activePromoDiscount.value > 0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Promo ${viewModel.activePromoCode}", color = Color(0xFF64748B), fontSize = 11.sp)
                        Text("- UGX ${viewModel.activePromoDiscount.value.toInt()}",
                            color = Color(0xFF10B981), fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Sp.md))

        // CONFIRM CTA
        val walletSufficient = walletBalance >= viewModel.calculatedFare - viewModel.activePromoDiscount.value
        val walletBlocked = viewModel.selectedPaymentMethod == "Wallet" && !walletSufficient
        BodaButton(
            text = if (walletBlocked) "Insufficient balance — top up first"
                else if (viewModel.isOnline) "Confirm — UGX ${(viewModel.calculatedFare - viewModel.activePromoDiscount.value).toInt()}"
                else "Book via SMS Fallback",
            enabled = !walletBlocked,
            onClick = {
                if (viewModel.isOnline) {
                    viewModel.confirmBooking()
                } else {
                    viewModel.triggerOfflineSMSBookingFlow()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            testTag = "confirm_ride_btn"
        )
    }
}

// --- SCREEN 6: MATCHING & WAITING ONBOARD SPINNER ---
@Composable
fun MatchingScreen(viewModel: BodaViewModel) {
    var showCancelReason by remember { mutableStateOf(false) }
    var cancelReasonText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text("Assigning closest Gulu Rider", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(Sp.sm))
            Text("Boda Escrow secures this payment. You can cancel for free before matching.", color = Color(0xFF64748B), fontSize = 12.sp, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(Sp.xxl))

            // Animated Matching progress radar sweep
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(200.dp)
                    .drawBehind {
                        drawCircle(Color(0xFF1E293B), radius = size.width * 0.5f)
                        drawCircle(Color(0xFFFDB913).copy(alpha = viewModel.matchProgress), radius = size.width * 0.5f * viewModel.matchProgress)
                    }
            ) {
                Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = Color(0xFFFDB913), modifier = Modifier.size(60.dp))
            }

            Spacer(modifier = Modifier.height(Sp.xl))

            LinearProgressIndicator(
                progress = { viewModel.matchProgress },
                color = Color(0xFFFDB913),
                trackColor = Color(0xFF334155),
                modifier = Modifier.fillMaxWidth(0.8f)
            )

            Spacer(modifier = Modifier.height(Sp.xxl))

            BodaButton(
                text = BodaLang.get(viewModel.appLanguage, "cancel"),
                onClick = { showCancelReason = true },
                containerColor = Color(0xFFE4002B),
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        }

        // Cancel dialog asking for reason (Mandatory)
        if (showCancelReason) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                BodaCard(modifier = Modifier.fillMaxWidth(0.85f)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Cancel Boda Request?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(Sp.sm))
                        Text("Late cancelation may charge a 1,000 UGX fee to reimburse the rider's fuel.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(Sp.sm))
                        BodaTextField(
                            value = cancelReasonText,
                            onValueChange = { cancelReasonText = it },
                            label = "Reason for Cancellation",
                            placeholder = "e.g. Changed mind, long wait",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(Sp.md))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            BodaTextButton(
                                text = "Go Back",
                                onClick = { showCancelReason = false },
                                contentColor = Color.White
                            )
                            Spacer(modifier = Modifier.width(Sp.sm))
                            BodaButton(
                                text = "Cancel Request",
                                onClick = {
                                    viewModel.cancelActiveTrip(cancelReasonText)
                                },
                                enabled = cancelReasonText.isNotEmpty(),
                                containerColor = Color(0xFFE4002B),
                                contentColor = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- SCREEN 7: RIDER EN ROUTE ---
@Composable
fun RiderEnRouteScreen(viewModel: BodaViewModel) {
    val trip = viewModel.currentSimulationTrip ?: return
    var displaySafetySheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(20.dp)
    ) {
        Text(BodaLang.get(viewModel.appLanguage, "rider_enroute"), color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(
            if (viewModel.simulationCountdown > 0) "Arriving in ${viewModel.simulationCountdown}s..." else BodaLang.get(viewModel.appLanguage, "rider_arrived"),
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.height(Sp.sm))

        // Gulu map tracking backdrop
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f)
        ) {
            val progress = (8 - viewModel.simulationCountdown) / 8f
            GuluMapView(
                modifier = Modifier.fillMaxSize(),
                pickup = viewModel.pickupPlace,
                dropoff = viewModel.dropoffPlace,
                riderProgress = progress,
                simulationState = "enroute",
                viewModel = viewModel
            )
        }

        Spacer(modifier = Modifier.height(Sp.sm))

        // Rider profile details & security verification code card
        BodaCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFDB913)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.Black)
                }
                Spacer(modifier = Modifier.width(Sp.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(trip.riderName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Bike Plate: ${trip.riderPlate}", color = Color(0xFFFDB913), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Verified community rider", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Security OTP", color = Color(0xFF64748B), fontSize = 11.sp)
                        Text("5892", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Sp.sm))

        // CALL, MESSAGE, SHARE LOCATION & SOS BUTTONS
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            BodaSecondaryButton(
                text = "Call",
                onClick = { viewModel.initiateCall(trip.riderName, trip.riderPhone) },
                icon = Icons.Default.Call,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(Sp.sm))
            BodaSecondaryButton(
                text = "Message",
                onClick = { viewModel.openRiderChat() },
                icon = Icons.Default.Message,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(Sp.sm))
            BodaButton(
                text = "SOS",
                onClick = { displaySafetySheet = true },
                containerColor = Color(0xFFE4002B),
                contentColor = Color.White,
                icon = Icons.Default.Warning,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(Sp.md))

        // ACTIVATE ON RIDER ARRIVAL BUTTON
        if (viewModel.simulationCountdown == 0) {
            BodaButton(
                text = "Enter security OTP & Start Trip",
                onClick = { viewModel.startActiveTrip() },
                modifier = Modifier.fillMaxWidth(),
                testTag = "start_ride_btn"
            )
        } else {
            BodaButton(
                text = "Cancel Booking",
                onClick = { viewModel.cancelActiveTrip("Rider taking too long") },
                containerColor = Color(0xFFE4002B),
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (displaySafetySheet) {
            SafetyActionsOverlay(viewModel = viewModel, onClose = { displaySafetySheet = false })
        }
    }
}

// --- SCREEN 8: ACTIVE TRIP TRACKING MAP ---
fun getSimulatedSpeed(progress: Float, traffic: String): Int {
    val baseSpeed = when (traffic) {
        "Light" -> 38
        "Moderate" -> 28
        "Heavy" -> 16
        "Rush Hour" -> 9
        else -> 26
    }
    val phase = (progress * 50).toInt() % 5
    val offset = when (phase) {
        0 -> -2
        1 -> 1
        2 -> -1
        3 -> 2
        4 -> 0
        else -> 0
    }
    return (baseSpeed + offset).coerceAtLeast(4)
}

fun getActiveNavigationStep(pickupName: String, dropoffName: String, progress: Float, realSteps: List<BodaViewModel.NavStep> = emptyList()): String {
    if (realSteps.isNotEmpty()) {
        val index = (progress * realSteps.size).toInt().coerceIn(0, realSteps.size - 1)
        return realSteps[index].instruction
    }
    val steps = listOf(
        "Depart from $pickupName onto local street grid.",
        "Turn left onto Gulu Commercial Highway.",
        "Pass through Pece Stadium Roundabout, proceed straight.",
        "Almost there! Entering neighborhood street.",
        "Approaching destination. Slowing down to arrive safely at $dropoffName."
    )
    val index = (progress * steps.size).toInt().coerceIn(0, steps.size - 1)
    return steps[index]
}

@Composable
fun ActiveTripScreen(viewModel: BodaViewModel) {
    val trip = viewModel.currentSimulationTrip ?: return
    var displaySafetySheet by remember { mutableStateOf(false) }
    var showAllSteps by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(BodaLang.get(viewModel.appLanguage, "active_trip"), color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Transit to ${trip.dropoffName.take(18)}...", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFDB913))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("${viewModel.simulationCountdown}s ETA", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(Sp.sm))

        // NAVIGATION TURN-BY-TURN CARD (TOP BAR OF MAP OVERLAY)
        BodaCard(
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFDB913).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Nav",
                        tint = Color(0xFFFDB913),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(Sp.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text("LIVE BODA NAVIGATION INSTRUCTION", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = getActiveNavigationStep(trip.pickupName, trip.dropoffName, viewModel.simulationRouteProgress, viewModel.navigationSteps),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Expandable navigation steps list
        if (viewModel.navigationSteps.isNotEmpty()) {
            BodaCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ALL NAV STEPS (${viewModel.navigationSteps.size})", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (showAllSteps) "Hide" else "Show",
                            color = Color(0xFFFDB913),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { showAllSteps = !showAllSteps }
                        )
                    }
                    if (showAllSteps) {
                        Spacer(modifier = Modifier.height(Sp.sm))
                        val currentStepIdx = (viewModel.simulationRouteProgress * viewModel.navigationSteps.size).toInt()
                            .coerceIn(0, viewModel.navigationSteps.size - 1)
                        viewModel.navigationSteps.forEachIndexed { idx, step ->
                            val isCurrent = idx == currentStepIdx
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (isCurrent) Color(0xFFFDB913) else Color(0xFF334155)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${idx + 1}", color = if (isCurrent) Color(0xFF0F172A) else Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(Sp.sm))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = step.instruction,
                                        color = if (isCurrent) Color.White else Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    Text(
                                        text = "${step.distanceMeters / 1000.0} km · ${step.durationSeconds / 60} min",
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Map drawing
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            GuluMapView(
                modifier = Modifier.fillMaxSize(),
                pickup = viewModel.pickupPlace,
                dropoff = viewModel.dropoffPlace,
                riderProgress = viewModel.simulationRouteProgress,
                simulationState = "active",
                viewModel = viewModel
            )
        }

        Spacer(modifier = Modifier.height(Sp.sm))

        // INSTRUMENTATION PANEL: SPEEDOMETER & SAFETY SHIELD STATUS
        BodaCard(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, contentDescription = "Speed", tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(Sp.sm))
                    Column {
                        Text("SIMULATED SPEED", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        val speed = getSimulatedSpeed(viewModel.simulationRouteProgress, viewModel.trafficCondition)
                        Text(
                            text = "$speed km/h",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(Sp.sm))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("BODA-WATCH STATUS", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Speed Safe", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BodaSecondaryButton(
                text = "All Steps",
                onClick = { showAllSteps = true },
                icon = Icons.Default.List,
                modifier = Modifier.weight(1f)
            )

            BodaButton(
                text = "Gulu Safety & SOS",
                onClick = { displaySafetySheet = true },
                containerColor = Color(0xFFE4002B),
                contentColor = Color.White,
                icon = Icons.Default.Security,
                modifier = Modifier.weight(1.5f)
            )
        }

        // All steps list popup dialog
        if (showAllSteps) {
            AlertDialog(
                onDismissRequest = { showAllSteps = false },
                title = { Text("Trip Route Directions", color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val steps = listOf(
                            "Depart from ${trip.pickupName} onto local street grid.",
                            "Turn left onto Gulu Commercial Highway.",
                            "Pass through Pece Stadium Roundabout, proceed straight.",
                            "Almost there! Entering neighborhood street.",
                            "Approaching destination. Slowing down to arrive safely at ${trip.dropoffName}."
                        )
                        steps.forEachIndexed { idx, st ->
                            val isCurrent = (viewModel.simulationRouteProgress * steps.size).toInt().coerceIn(0, steps.size - 1) == idx
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isCurrent) Color(0xFFFDB913).copy(alpha = 0.15f) else Color.Transparent)
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (isCurrent) Color(0xFFFDB913) else Color(0xFF334155)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        color = if (isCurrent) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(Sp.sm))
                                Text(st, color = if (isCurrent) Color(0xFFFDB913) else Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                },
                confirmButton = {
                    BodaButton(
                        text = "Close",
                        onClick = { showAllSteps = false },
                        modifier = Modifier.width(100.dp)
                    )
                },
                containerColor = Color(0xFF1E293B)
            )
        }

        if (displaySafetySheet) {
            SafetyActionsOverlay(viewModel = viewModel, onClose = { displaySafetySheet = false })
        }
    }
}

// --- WIDGET: SAFETY SOS EMERGENCY PANEL ---
@Composable
fun SafetyActionsOverlay(viewModel: BodaViewModel, onClose: () -> Unit) {
    var activeCallContact by remember { mutableStateOf<String?>(null) }
    var activeCallNumber by remember { mutableStateOf<String?>(null) }
    var showShareSuccess by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { 
                if (activeCallContact == null && !showShareSuccess) {
                    onClose() 
                }
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false) {} // prevent dismissing when clicking card content
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .animateContentSize()
            ) {
                if (activeCallContact != null) {
                    // Simulating Active Emergency Call UI
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE4002B).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Active Call",
                                tint = Color(0xFFE4002B),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(Sp.md))
                        Text(
                            text = "DIALING GULU HELPLINE...",
                            color = Color(0xFFE4002B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = activeCallContact ?: "",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = activeCallNumber ?: "",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(Sp.lg))
                        LinearProgressIndicator(
                            color = Color(0xFFE4002B),
                            trackColor = Color(0xFF334155),
                            modifier = Modifier.width(140.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.height(Sp.sm))
                        Text(
                            text = "Simulating encrypted community watch connection...",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        
                        // Render Emergency SMS dispatches dynamically
                        if (viewModel.emergencySMSDispatchLogs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(Sp.md))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Emergency SMS Dispatches:", color = Color(0xFFFDB913), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(Sp.sm))
                                    viewModel.emergencySMSDispatchLogs.forEach { log ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(Sp.sm))
                                            Text(log, color = Color.White, fontSize = 11.sp, style = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace))
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(Sp.lg))
                        BodaSecondaryButton(
                            text = "Cancel Call",
                            onClick = {
                                activeCallContact = null
                                activeCallNumber = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else if (showShareSuccess) {
                    // Live Location Link Copied Screen
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Success",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(Sp.md))
                        Text(
                            text = "Trip Tracking Link Copied",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(Sp.sm))
                        Text(
                            text = "Share this secure link with family or on WhatsApp:\nhttps://boda-gulu.ug/track/BODA-LIVE-SECURE",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(Sp.lg))
                        BodaButton(
                            text = "Done",
                            onClick = { showShareSuccess = false },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    // Primary Gulu Safety Board
                    Text("Gulu Boda Safety Center", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(Sp.xs))
                    Text("Active satellite tracking and immediate local community dispatch.", color = Color(0xFF94A3B8), fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(Sp.md))

                    // SOS Red Dial Button
                    BodaButton(
                        text = "1-Tap Gulu SOS (0800 112 112)",
                        onClick = {
                            activeCallContact = "Gulu Boda Dispatch"
                            activeCallNumber = "0800 112 112"
                            viewModel.dispatchSOSSMS()
                        },
                        containerColor = Color(0xFFE4002B),
                        contentColor = Color.White,
                        icon = Icons.Default.Call,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(Sp.md))

                    // Gulu Directory Title
                    Text("Local Gulu Emergency Directory", color = Color(0xFFFDB913), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(Sp.sm))

                    val safetyHotlines = listOf(
                        Triple("Gulu Central Police Station", "0471 432022", "Main division desk"),
                        Triple("Lacor Hospital Emergency Unit", "0471 432494", "24/7 Trauma ward"),
                        Triple("Pece Boda-Watch Patrol", "0772 401402", "Community riders watch")
                    )

                    safetyHotlines.forEach { (name, num, desc) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { 
                                    activeCallContact = name
                                    activeCallNumber = num
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(desc, color = Color(0xFF64748B), fontSize = 11.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(num, color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.width(Sp.sm))
                                    Icon(Icons.Default.Call, contentDescription = "Dial", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Sp.sm))

                    // Action 2: Share Live Location Link
                    BodaButton(
                        text = "Share Live Trip Tracking Link",
                        onClick = { showShareSuccess = true },
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White,
                        icon = Icons.Default.Share,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(Sp.sm))

                    BodaSecondaryButton(
                        text = "Close Safety Panel",
                        onClick = { onClose() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// --- SCREEN 9: POST-TRIP RATING & RECEIPT SUMMARY ---
@Composable
fun PostTripScreen(viewModel: BodaViewModel) {
    val trip = viewModel.currentSimulationTrip ?: return
    var starRating by remember { mutableStateOf(5) }
    var feedbackComment by remember { mutableStateOf("") }
    var processingPayment by remember { mutableStateOf(false) }
    var paymentConfirmed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(Sp.sm))
        Text(BodaLang.get(viewModel.appLanguage, "trip_completed"), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(Sp.xs))
        Text("Your ride with ${trip.riderName} is finished.", color = Color(0xFF94A3B8), fontSize = 14.sp)

        Spacer(modifier = Modifier.height(Sp.md))

        // Rider Card
        BodaCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF334155)),
                    contentAlignment = Alignment.Center) {
                    Text(trip.riderName.first().uppercase(), color = Color(0xFFFDB913),
                        fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(trip.riderName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(trip.riderPlate, color = Color(0xFF64748B), fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("UGX ${trip.fare.toInt()}",
                        color = Color(0xFFFDB913), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("via ${trip.paymentMethod}", color = Color(0xFF64748B), fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(Sp.md))

        // Receipt Details
        BodaCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Official Boda Receipt", color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(Sp.sm))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Total Charged Amount", color = Color.White, fontSize = 14.sp)
                    Text("UGX ${trip.fare.toInt()}", color = Color(0xFFFDB913), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.height(Sp.sm))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Payment Method", color = Color.White, fontSize = 12.sp)
                    Text(trip.paymentMethod, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(Sp.sm))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Trip ID Reference", color = Color.White, fontSize = 12.sp)
                    Text("BODA-TRIP-${trip.id}", color = Color(0xFF64748B), fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(Sp.sm))
                HorizontalDivider(color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(Sp.sm))
                BodaSecondaryButton(
                    text = "Download PDF Receipt",
                    onClick = { /* Simulate Receipt Download */ },
                    icon = Icons.Default.Download,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(Sp.md))

        // Stars Rating Form
        Text(BodaLang.get(viewModel.appLanguage, "rate_rider"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(Sp.sm))
        Row {
            for (i in 1..5) {
                IconButton(onClick = { starRating = i }) {
                    Icon(
                        imageVector = if (i <= starRating) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = Color(0xFFFDB913),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Sp.sm))

        OutlinedTextField(
            value = feedbackComment,
            onValueChange = { feedbackComment = it },
            placeholder = { Text(BodaLang.get(viewModel.appLanguage, "comment_hint"), color = Color(0xFF475569)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF1E293B),
                unfocusedContainerColor = Color(0xFF1E293B),
                focusedBorderColor = Color(0xFFFDB913),
                unfocusedBorderColor = Color(0xFF334155)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Sp.lg))

        if (!paymentConfirmed) {
            if (processingPayment) {
                CircularProgressIndicator(color = Color(0xFFFDB913))
                Spacer(modifier = Modifier.height(Sp.sm))
                Text("Confirming MTN/Airtel Mobile Money escrow transfer...", color = Color(0xFFFDB913), fontSize = 12.sp)
            } else {
                BodaButton(
                    text = "Release Escrow & Pay",
                    onClick = {
                        processingPayment = true
                        // Simulate escrow check
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(4000)
                            processingPayment = false
                            paymentConfirmed = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pay_and_finish_btn")
                )
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Sp.sm)) {
                BodaSecondaryButton(
                    text = "Skip",
                    onClick = {
                        viewModel.currentSimulationTrip = null
                        viewModel.simulationState = "idle"
                        viewModel.navigateTo(Screen.Home)
                    },
                    modifier = Modifier.weight(1f)
                )
                BodaButton(
                    text = "Done",
                    onClick = { viewModel.submitPostTripRating(starRating, feedbackComment) },
                    modifier = Modifier.weight(2f)
                )
            }
        }
    }
}

// --- SCREEN 10: TRIPS LISTING & DISPUTE SYSTEM ---
@Composable
fun TripsHistoryScreen(viewModel: BodaViewModel, trips: List<Trip>) {
    var displayDisputeDialog by remember { mutableStateOf<Trip?>(null) }
    var disputeReason by remember { mutableStateOf("") }
    var disputeDetails by remember { mutableStateOf("") }

    if (viewModel.isLoadingData) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0F172A)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFFDB913))
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        Text("Your Trips & Deliveries", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(Sp.sm))

        if (trips.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = "No Trips",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(Sp.sm))
                    Text("No trips yet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(Sp.sm))
                    Text("Your past ride and delivery ledger will appear here.", color = Color(0xFF64748B), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(Sp.md))
                    BodaButton(
                        text = "Book your first ride",
                        onClick = { viewModel.navigateTo(Screen.Home) }
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(trips) { trip ->
                    BodaCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (trip.type == "ride") Icons.Default.TwoWheeler else Icons.Default.LocalShipping,
                                        contentDescription = null,
                                        tint = Color(0xFFFDB913)
                                    )
                                    Spacer(modifier = Modifier.width(Sp.sm))
                                    Text(trip.type.replaceFirstChar { it.uppercase() }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Text(
                                    text = "UGX ${trip.fare.toInt()}",
                                    color = Color(0xFFFDB913),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(Sp.sm))
                            Text("From: ${trip.pickupName}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text("To: ${trip.dropoffName}", color = Color(0xFF94A3B8), fontSize = 12.sp)

                            Spacer(modifier = Modifier.height(Sp.sm))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Rider: ${trip.riderName} (${trip.riderPlate})", color = Color.White, fontSize = 11.sp)
                                Text(
                                    text = trip.status.uppercase(),
                                    color = when (trip.status) {
                                        "completed" -> Color(0xFF10B981)
                                        "canceled" -> Color(0xFFEF4444)
                                        else -> Color(0xFFFDB913)
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            if (trip.status == "completed") {
                                Spacer(modifier = Modifier.height(Sp.sm))
                                Row {
                                    BodaButton(
                                        text = "Dispute Fare / Trip",
                                        onClick = { displayDisputeDialog = trip },
                                        containerColor = Color(0xFFE4002B),
                                        contentColor = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else if (trip.status == "disputed") {
                                Spacer(modifier = Modifier.height(Sp.sm))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFE4002B).copy(alpha = 0.2f))
                                        .padding(8.dp)
                                ) {
                                    Text("Disputed Filed: ${trip.disputeReason}. Gulu team is reviewing evidence.", color = Color(0xFFEF4444), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dispute form overlay
        if (displayDisputeDialog != null) {
            val disputedTrip = displayDisputeDialog!!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                BodaCard(modifier = Modifier.fillMaxWidth(0.85f)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Dispute Boda Trip", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(Sp.sm))
                        listOf("Wrong Route taken", "Rider was rude", "Overcharged", "Package damaged").forEach { reason ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { disputeReason = reason }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (disputeReason == reason) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = Color(0xFFFDB913)
                                )
                                Spacer(modifier = Modifier.width(Sp.sm))
                                Text(reason, color = Color.White, fontSize = 14.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(Sp.sm))
                        OutlinedTextField(
                            value = disputeDetails,
                            onValueChange = { disputeDetails = it },
                            placeholder = { Text("Provide details / description of incident", color = Color(0xFF475569)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedBorderColor = Color(0xFFFDB913),
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(Sp.md))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { displayDisputeDialog = null }) {
                                Text("Cancel", color = Color.White)
                            }
                            Spacer(modifier = Modifier.width(Sp.sm))
                            BodaButton(
                                text = "Submit Dispute",
                                onClick = {
                                    viewModel.disputeTrip(disputedTrip.id, disputeReason, disputeDetails)
                                    displayDisputeDialog = null
                                },
                                enabled = disputeReason.isNotEmpty() && disputeDetails.isNotEmpty(),
                                containerColor = Color(0xFFE4002B),
                                contentColor = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- SCREEN 11: WALLET balance & DEPOSITS ---
@Composable
fun WalletScreen(viewModel: BodaViewModel, balance: Double, txns: List<WalletTransaction>) {
    if (viewModel.isLoadingData) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0F172A)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFFDB913))
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Boda Gulu Wallet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(Sp.sm))

        // Balance Card
        BodaCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Available Escrow Balance", color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(Sp.sm))
                Text("UGX ${balance.toInt()}", color = Color(0xFFFDB913), fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                Spacer(modifier = Modifier.height(Sp.sm))
                Text("Secure payments around Gulu without physical cash.", color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(Sp.md))

        // Spending Stats Row
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        val ridesThisMonth = txns.count { it.type == "payment" &&
            System.currentTimeMillis() - it.timestamp < thirtyDaysMs }
        val spentThisMonth = txns.filter { it.type == "payment" &&
            System.currentTimeMillis() - it.timestamp < thirtyDaysMs }
            .sumOf { it.amount }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Sp.sm)) {
            listOf(
                "$ridesThisMonth" to "Trips this month",
                "UGX ${spentThisMonth.toInt()}" to "Spent this month",
                txns.firstOrNull()?.provider ?: "MTN" to "Last used"
            ).forEach { (val_, lbl) ->
                BodaCard(modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(val_, color = Color(0xFFFDB913), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(lbl, color = Color(0xFF64748B), fontSize = 9.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Sp.md))

        // Quick Top-up selector
        Text("MTN / Airtel Quick Deposit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(Sp.sm))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            listOf("2000", "5000", "10000", "20000").forEach { valAmount ->
                val isSelected = viewModel.walletTopupAmountInput == valAmount
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Color(0xFFFDB913) else Color(0xFF1E293B))
                        .clickable { viewModel.walletTopupAmountInput = valAmount }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("+$valAmount", color = if (isSelected) Color.Black else Color(0xFFFDB913), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(Sp.sm))

        BodaTextField(
            value = viewModel.walletTopupPhoneInput,
            onValueChange = { viewModel.walletTopupPhoneInput = it },
            label = "Mobile Money Number",
            placeholder = "e.g. 0772 123456",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            testTag = "wallet_phone_input"
        )

        Spacer(modifier = Modifier.height(Sp.sm))

        if (viewModel.walletTopupStatus == "pending") {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFFFDB913))
                    Spacer(modifier = Modifier.height(Sp.sm))
                    Text("Awaiting Mobile Money OTP and Pin Approval...", color = Color(0xFFFDB913), fontSize = 12.sp)
                }
            }
        } else {
            BodaButton(
                text = "Top Up Wallet Balance (UGX ${viewModel.walletTopupAmountInput})",
                onClick = { viewModel.startWalletTopup() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wallet_topup_btn")
            )
        }

        if (viewModel.walletTopupStatus == "success") {
            Text("Top up successful! Ref: ${viewModel.activeTransactionReference}", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(Sp.md))

        // Historic Ledger list
        Text("Transaction History Ledger", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(Sp.sm))

        if (txns.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = "No Transactions",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(Sp.sm))
                    Text("No transactions yet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(Sp.xs))
                    Text("Your Mobile Money deposits and ride payments will show up here.", color = Color(0xFF64748B), fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            txns.forEach { txn ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val txIcon = when {
                            txn.type == "payment" -> Icons.Default.TwoWheeler
                            txn.reference.startsWith("REF-") -> Icons.Default.CardGiftcard
                            else -> Icons.Default.ArrowUpward
                        }
                        val txColor = if (txn.type == "payment") Color(0xFFF87171) else Color(0xFF10B981)
                        Icon(
                            imageVector = txIcon,
                            contentDescription = null,
                            tint = txColor
                        )
                        Spacer(modifier = Modifier.width(Sp.sm))
                        Column {
                            Text(if (txn.type == "topup") "MoMo Topup Deposit" else "Boda Booking Payment", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Ref: ${txn.reference} â€¢ ${txn.provider}", color = Color(0xFF64748B), fontSize = 11.sp)
                        }
                    }
                    Text(
                        text = "${if (txn.type == "topup") "+" else "-"} UGX ${txn.amount.toInt()}",
                        color = if (txn.type == "topup") Color(0xFF10B981) else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                HorizontalDivider(color = Color(0xFF1E293B))
            }
        }
    }
}

// --- SCREEN 12: PROFILE SETTINGS ENGINE ---
@Composable
fun ProfileSettingsScreen(viewModel: BodaViewModel, user: UserProfile?, contacts: List<EmergencyContact>) {
    val coroutineScope = rememberCoroutineScope()
    val referralEarnings by viewModel.referralEarnings.collectAsState()
    val myReferralCode = user?.referralCode?.ifEmpty { "GULU-BODA-256" } ?: "GULU-BODA-256"
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (viewModel.isLoadingData) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0F172A)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFFDB913))
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFDB913)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Black, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(Sp.sm))
            Column {
                Text(user?.name ?: "Boda Gulu Customer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(user?.phoneNumber ?: "No verified phone", color = Color(0xFF64748B), fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(Sp.md))

        // Community Referral Earnings Code Card
        BodaCard(
            onClick = { viewModel.navigateTo(Screen.Referrals) },
            testTag = "referrals_card",
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Referral Earnings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("UGX ${referralEarnings.toInt()}", color = Color(0xFF10B981), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(Sp.xs))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(Sp.xs))
                Text("Share code $myReferralCode to earn 3,000 UGX for each friend registered in Gulu. Click to view dashboard & refer friends!", color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(Sp.md))

        // Gulu Local Language Selector Card
        Text("App Language Localization", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(Sp.sm))
        BodaCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Select Local Gulu Dialect:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(Sp.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val languages = listOf(
                        Triple("en", "English", "ðŸ‡¬ðŸ‡§"),
                        Triple("ach", "Acholi/Luo", "ðŸ‡ºðŸ‡¬"),
                        Triple("luo", "Lango/Luo", "ðŸ‡ºðŸ‡¬")
                    )
                    languages.forEach { (code, label, flag) ->
                        val isSelected = viewModel.appLanguage == code
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFFDB913) else Color(0xFF0F172A))
                                .clickable {
                                    coroutineScope.launch {
                                        viewModel.updateLanguage(code)
                                        // Update user profile language persistent setting
                                        val updatedProfile = user?.copy(language = code) ?: UserProfile(id = 1, isSetupComplete = true, language = code)
                                        viewModel.saveUserProfile(updatedProfile)
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(flag, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(Sp.xs))
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Sp.md))

        // App Theme Selector Card
        Text("App Theme & Styling Mode", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(Sp.sm))
        BodaCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Select Theme mode for Boda Gulu:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(Sp.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val themes = listOf(
                        Triple("system", "System", Icons.Default.Settings),
                        Triple("dark", "Dark Mode", Icons.Default.NightsStay),
                        Triple("light", "Light Mode", Icons.Default.WbSunny)
                    )
                    themes.forEach { (mode, label, icon) ->
                        val isSelected = viewModel.appThemeSetting == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFFDB913) else Color(0xFF0F172A))
                                .clickable {
                                    viewModel.updateAppThemeSetting(mode)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.Black else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(Sp.sm))
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Sp.md))

        // PostgreSQL Real-time WebSocket Sync & Room Cache Control Center
        Text("PostgreSQL Real-time Sync & Cache Monitor", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(Sp.sm))
        BodaCard(
            border = androidx.compose.foundation.BorderStroke(1.dp, if (viewModel.isOnline) Color(0xFF10B981) else Color(0xFF64748B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Connection state row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (viewModel.isOnline) Color(0xFF10B981) else Color(0xFFEF4444))
                        )
                        Spacer(modifier = Modifier.width(Sp.sm))
                        Text(
                            text = if (viewModel.isOnline) "Postgres WebSocket Connected" else "Postgres Offline (Room Active)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    
                    // Switch to toggle online/offline
                    Switch(
                        checked = viewModel.isOnline,
                        onCheckedChange = { viewModel.toggleNetworkConnection() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF10B981),
                            checkedTrackColor = Color(0xFF10B981).copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(Sp.sm))
                
                // Cache stats row
                val cachedTrips = viewModel.trips.collectAsState().value
                val unsyncedCount = viewModel.unsyncedTripsCount.collectAsState().value
                val cachedPlacesCount = viewModel.savedPlaces.collectAsState().value.size
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Cached Trips", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("${cachedTrips.size}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Cached Places", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("$cachedPlacesCount", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Unsynced to PG", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("$unsyncedCount", color = if (unsyncedCount > 0) Color(0xFFFDB913) else Color(0xFF10B981), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                
                Spacer(modifier = Modifier.height(Sp.sm))
                
                // Real-time scrolling CDC WebSocket log terminal!
                Text("PostgreSQL Live CDC Replication Log:", color = Color(0xFFFDB913), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(Sp.xs))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(viewModel.postgresWebSocketLogs) { log ->
                            Text(
                                text = log,
                                color = if (log.contains("SUCCESS") || log.contains("âœ“")) Color(0xFF10B981) else if (log.contains("Warning") || log.contains("offline")) Color(0xFFFDB913) else Color.White,
                                fontSize = 11.sp,
                                style = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(Sp.sm))
                
                // Forced manual replication trigger
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BodaSecondaryButton(
                        text = "Force PG Sync",
                        onClick = {
                            viewModel.connectPostgresWebSocket()
                        },
                        enabled = viewModel.isOnline,
                        modifier = Modifier.weight(1f)
                    )
                    
                    BodaSecondaryButton(
                        text = "Reset Sync",
                        onClick = {
                            coroutineScope.launch {
                                viewModel.connectPostgresWebSocket()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Sp.md))

        // Dual Mode Switch: Driver Mode Toggle
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFFDB913), RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        if (viewModel.isDriverRegistered) {
                            viewModel.isDriverMode = !viewModel.isDriverMode
                            viewModel.navigateTo(Screen.Home)
                        } else {
                            viewModel.startDriverOnboarding()
                        }
                    }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = Color(0xFFFDB913), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(Sp.sm))
                    Column {
                        Text("Gulu Boda Driver Mode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            if (!viewModel.isDriverRegistered) "Tap to Register as Vetted Rider"
                            else if (viewModel.isDriverMode) "Active (Gulu Cockpit)"
                            else "Inactive (Passenger)", 
                            color = if (viewModel.isDriverRegistered) Color(0xFF10B981) else Color(0xFFFDB913), 
                            fontSize = 12.sp
                        )
                    }
                }
                Switch(
                    checked = viewModel.isDriverMode,
                    onCheckedChange = { 
                        if (viewModel.isDriverRegistered) {
                            viewModel.isDriverMode = it
                            viewModel.navigateTo(Screen.Home)
                        } else {
                            viewModel.startDriverOnboarding()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = Color(0xFFFDB913),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color(0xFF1E293B)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(Sp.md))

        // Settings Buttons list
        Text("Preferences & Customization", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(Sp.sm))

        // Option: Emergency Contacts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.navigateTo(Screen.EmergencyContacts) }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Emergency, contentDescription = null, tint = Color(0xFFE4002B))
                Spacer(modifier = Modifier.width(Sp.sm))
                Column {
                    Text("Emergency Contacts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("${contacts.size} of 3 emergency linkages setup", color = Color(0xFF64748B), fontSize = 12.sp)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
        }
        HorizontalDivider(color = Color(0xFF1E293B))

        // Option: Saved Places
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.navigateTo(Screen.SavedPlacesManage) }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color(0xFFFDB913))
                Spacer(modifier = Modifier.width(Sp.sm))
                Text("Manage Saved Places", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
        }
        HorizontalDivider(color = Color(0xFF1E293B))

        // Option: Help & Support
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.navigateTo(Screen.Support) }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HelpCenter, contentDescription = null, tint = Color(0xFFFDB913))
                Spacer(modifier = Modifier.width(Sp.sm))
                Text("Help & Support", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
        }
        HorizontalDivider(color = Color(0xFF1E293B))

        // Option: System Overlay Triggers (For testing and complete 75 screen path coverage)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.showDowntimeNotice = true }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Build, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(Sp.sm))
                Text("Simulate App Maintenance Error", color = Color.White, fontSize = 14.sp)
            }
        }
        HorizontalDivider(color = Color(0xFF1E293B))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.showSuspendedNotice = true }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Gavel, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(Sp.sm))
                Text("Simulate Account Suspension Error", color = Color.White, fontSize = 14.sp)
            }
        }
        HorizontalDivider(color = Color(0xFF1E293B))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.isOffline = true }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(Sp.sm))
                Text("Simulate Offline State Error Banner", color = Color.White, fontSize = 14.sp)
            }
        }
        HorizontalDivider(color = Color(0xFF1E293B))

        // Delete Account button
        Spacer(modifier = Modifier.height(Sp.lg))
        BodaButton(
            text = "Delete Account & Logout",
            onClick = {
                showDeleteConfirmDialog = true
            },
            containerColor = Color(0xFFE4002B),
            contentColor = Color.White,
            modifier = Modifier.fillMaxWidth()
        )

        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Delete Account?", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete your account? This action cannot be undone and you will lose all Gulu wallet balances, trip history, and settings.", color = Color(0xFF94A3B8)) },
                containerColor = Color(0xFF1E293B),
                confirmButton = {
                    BodaButton(
                        text = "Delete",
                        onClick = {
                            showDeleteConfirmDialog = false
                            coroutineScope.launch {
                                val emptyProfile = UserProfile(id = 1, isSetupComplete = false)
                                viewModel.saveUserProfile(emptyProfile)
                                viewModel.resetOnboarding()
                                viewModel.navigateTo(Screen.WelcomeOnboarding)
                            }
                        },
                        containerColor = Color(0xFFE4002B),
                        contentColor = Color.White
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                }
            )
        }
    }
}

// --- SCREEN 13: EMERGENCY CONTACTS (SAFETY PATHS) ---
@Composable
fun EmergencyContactsScreen(viewModel: BodaViewModel, contacts: List<EmergencyContact>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(Sp.sm))
            Text("Emergency Contacts Linkage", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(Sp.sm))
        Text("Link up to 3 trusted contacts. We will SMS them a live Gulu tracking map link whenever you trigger the 1-Tap SOS.", color = Color(0xFF94A3B8), fontSize = 12.sp)

        Spacer(modifier = Modifier.height(Sp.md))

        // Contact fields insertion
        if (contacts.size < 3) {
            BodaCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Add Emergency Contact Link", color = Color(0xFFFDB913), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(Sp.sm))
                    BodaTextField(
                        value = viewModel.newEmergencyName,
                        onValueChange = { viewModel.newEmergencyName = it },
                        label = "Contact Name",
                        placeholder = "e.g. Uncle Benson",
                        testTag = "emergency_contact_name_input"
                    )
                    Spacer(modifier = Modifier.height(Sp.sm))
                    BodaTextField(
                        value = viewModel.newEmergencyPhone,
                        onValueChange = { viewModel.newEmergencyPhone = it },
                        label = "Phone Number",
                        placeholder = "e.g. +256 772 111222",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        testTag = "emergency_contact_phone_input"
                    )
                    Spacer(modifier = Modifier.height(Sp.sm))
                    BodaButton(
                        text = "Add Contact Now",
                        onClick = { viewModel.addEmergencyContact() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Sp.md))

        LazyColumn {
            items(contacts) { contact ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(contact.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(contact.phoneNumber, color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                    IconButton(onClick = { viewModel.removeEmergencyContact(contact) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFE4002B))
                    }
                }
                HorizontalDivider(color = Color(0xFF1E293B))
            }
        }
    }
}

// --- SCREEN 14: SAVED PLACES MANAGEMENT ---
@Composable
fun SavedPlacesManageScreen(viewModel: BodaViewModel, savedPlaces: List<SavedPlace>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(Sp.sm))
            Text("Saved Places", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(Sp.sm))

        BodaCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Add New Location Bookmark", color = Color(0xFFFDB913), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(Sp.sm))
                BodaTextField(
                    value = viewModel.newPlaceLabel,
                    onValueChange = { viewModel.newPlaceLabel = it },
                    label = "Place Label",
                    placeholder = "e.g. Market, Church, Gulu School",
                    testTag = "new_place_label_input"
                )
                Spacer(modifier = Modifier.height(Sp.sm))
                BodaTextField(
                    value = viewModel.newPlaceName,
                    onValueChange = { viewModel.newPlaceName = it },
                    label = "Address / Landmarks",
                    placeholder = "Full Address / Landmarks in Gulu",
                    testTag = "new_place_address_input"
                )
                Spacer(modifier = Modifier.height(Sp.sm))
                BodaButton(
                    text = "Bookmark Place Now",
                    onClick = { viewModel.addSavedPlace() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(Sp.md))

        LazyColumn {
            items(savedPlaces) { place ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFFDB913))
                        Spacer(modifier = Modifier.width(Sp.sm))
                        Column {
                            Text(place.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(place.name, color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                    }
                    IconButton(onClick = { viewModel.removeSavedPlace(place) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFE4002B))
                    }
                }
                HorizontalDivider(color = Color(0xFF1E293B))
            }
        }
    }
}

// --- SCREEN 15: SUPPORT CHAT & FAQ ENGINE ---
@Composable
fun SupportScreen(viewModel: BodaViewModel) {
    var inChatMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Help & Support Gulu", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (inChatMode) Color(0xFFFDB913) else Color(0xFF334155))
                    .clickable { inChatMode = !inChatMode }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(if (inChatMode) "FAQ Center" else "Live Officer Chat", color = if (inChatMode) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(Sp.sm))

        if (inChatMode) {
            // Live agent chat messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                items(viewModel.activeChatMessages) { msg ->
                    val isUser = msg.sender == "user"
                    val isSystem = msg.sender == "system"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = if (isUser) Alignment.CenterEnd else if (isSystem) Alignment.Center else Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isUser) Color(0xFFFDB913)
                                    else if (isSystem) Color(0xFF475569).copy(alpha = 0.5f)
                                    else Color(0xFF334155)
                                )
                                .padding(10.dp)
                                .fillMaxWidth(if (isSystem) 0.9f else 0.75f)
                        ) {
                            Text(
                                msg.message,
                                color = if (isUser) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isSystem) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Sp.sm))

            // Message text input bar
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = viewModel.newChatMessageText,
                    onValueChange = { viewModel.newChatMessageText = it },
                    placeholder = { Text("Ask about fares or lost items...", color = Color(0xFF475569)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B),
                        focusedBorderColor = Color(0xFFFDB913),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(Sp.sm))
                IconButton(onClick = { viewModel.sendSupportChatMessage() }) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color(0xFFFDB913))
                }
            }
        } else {
            // Standard FAQ Knowledge Center
            OutlinedTextField(
                value = viewModel.supportSearchQuery,
                onValueChange = { viewModel.supportSearchQuery = it },
                placeholder = { Text("Search FAQ articles (e.g. lost bag)...", color = Color(0xFF475569)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF1E293B),
                    focusedBorderColor = Color(0xFFFDB913),
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Sp.sm))

            val faqs = listOf(
                "How is my fare calculated in Gulu?" to "Boda Gulu rides start with a base of 2,000 UGX, plus 1,000 UGX per kilometer. Deliveries start at 3,000 UGX.",
                "How do I use MTN Mobile Money to top up?" to "Go to Wallet, type the amount, tap deposit, and enter your MoMo pin on the phone screen.",
                "What is Boda Escrow protection?" to "Your payment is locked when you book. It is only released to the rider after you arrive and share the Security OTP.",
                "Lost item recovery in Gulu?" to "Go to Support Live Officer Chat immediately or call our Gulu hotline at 0800 112 112 to secure your item."
            )

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(faqs.filter { it.first.contains(viewModel.supportSearchQuery, ignoreCase = true) }) { (q, a) ->
                    BodaCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(q, color = Color(0xFFFDB913), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(Sp.xs))
                            Text(a, color = Color(0xFF94A3B8), fontSize = 11.sp, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

// --- APP BOTTOM COMPOSABLE NAVIGATION BAR ---
@Composable
fun BodaBottomNavigation(viewModel: BodaViewModel) {
    NavigationBar(
        containerColor = Color(0xFF0F172A),
        windowInsets = WindowInsets.navigationBars
    ) {
        val curr = viewModel.currentScreen
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Home") },
            selected = curr == Screen.Home || curr == Screen.RoutePreview || curr == Screen.Matching || curr == Screen.RiderEnRoute || curr == Screen.ActiveTrip || curr == Screen.PostTrip,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = Color(0xFFFDB913),
                indicatorColor = Color(0xFFFDB913),
                unselectedIconColor = Color.White,
                unselectedTextColor = Color.White
            ),
            onClick = { viewModel.navigateTo(Screen.Home) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.History, contentDescription = null) },
            label = { Text(BodaLang.get(viewModel.appLanguage, "history")) },
            selected = curr == Screen.TripsHistory,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = Color(0xFFFDB913),
                indicatorColor = Color(0xFFFDB913),
                unselectedIconColor = Color.White,
                unselectedTextColor = Color.White
            ),
            onClick = { viewModel.navigateTo(Screen.TripsHistory) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
            label = { Text("Wallet") },
            selected = curr == Screen.Wallet,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = Color(0xFFFDB913),
                indicatorColor = Color(0xFFFDB913),
                unselectedIconColor = Color.White,
                unselectedTextColor = Color.White
            ),
            onClick = { viewModel.navigateTo(Screen.Wallet) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text("Profile") },
            selected = curr == Screen.ProfileSettings || curr == Screen.EmergencyContacts || curr == Screen.SavedPlacesManage,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = Color(0xFFFDB913),
                indicatorColor = Color(0xFFFDB913),
                unselectedIconColor = Color.White,
                unselectedTextColor = Color.White
            ),
            onClick = { viewModel.navigateTo(Screen.ProfileSettings) }
        )
    }
}

@Composable
fun DriverOnboardingScreen(viewModel: BodaViewModel) {
    val step = viewModel.driverOnboardingStep
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Back arrow
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { 
                if (step > 1) viewModel.driverOnboardingStep-- 
                else viewModel.navigateTo(Screen.WelcomeOnboarding) 
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(Sp.sm))
            Text("Driver Onboarding Portal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(Sp.md))

        // Progress bar indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Step $step of 5", color = ComposeColor(0xFFFDB913), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(
                text = when(step) {
                    1 -> "Personal & NIN Details"
                    2 -> "Motorcycle & Stage"
                    3 -> "Document Security Scan"
                    4 -> "Gulu Safety Code Quiz"
                    else -> "Approved!"
                },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(Sp.sm))
        Row(modifier = Modifier.fillMaxWidth()) {
            for (i in 1..5) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (i <= step) ComposeColor(0xFF10B981) else Color(0xFF334155)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(Sp.lg))

        when (step) {
            1 -> {
                // Step 1: Personal Details
                Text(
                    "Register as Gulu Boda Member",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    "Enter your legal bio-data and National ID details. All drivers must be vetted for safety.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp).align(Alignment.Start)
                )

                BodaTextField(
                    value = viewModel.driverRegName,
                    onValueChange = { viewModel.driverRegName = it },
                    label = "Driver Legal Name",
                    testTag = "driver_reg_name"
                )

                Spacer(modifier = Modifier.height(Sp.sm))

                BodaTextField(
                    value = viewModel.driverRegPhone,
                    onValueChange = { viewModel.driverRegPhone = it },
                    label = "Payout Phone Number (+256...)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    testTag = "driver_reg_phone"
                )

                Spacer(modifier = Modifier.height(Sp.sm))

                BodaTextField(
                    value = viewModel.driverRegNID,
                    onValueChange = { viewModel.driverRegNID = it.uppercase().take(14) },
                    label = "National ID / NIN (e.g. CM8400...)",
                    placeholder = "14 character NIN",
                    testTag = "driver_reg_nid"
                )

                Spacer(modifier = Modifier.height(Sp.xl))

                BodaButton(
                    text = "Continue to Motorcycle Info",
                    onClick = { viewModel.driverOnboardingStep = 2 },
                    enabled = viewModel.driverRegName.isNotEmpty() && viewModel.driverRegPhone.isNotEmpty() && viewModel.driverRegNID.length >= 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            2 -> {
                // Step 2: Motorcycle Details
                Text(
                    "Motorcycle & Stage Setup",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    "Enter your license plate and local Gulu security stage association.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp).align(Alignment.Start)
                )

                BodaTextField(
                    value = viewModel.driverRegPlate,
                    onValueChange = { viewModel.driverRegPlate = it.uppercase() },
                    label = "License Plate Number",
                    placeholder = "e.g. UFL 123X",
                    testTag = "driver_reg_plate"
                )

                Spacer(modifier = Modifier.height(Sp.md))

                Text("Select Assigned Security Stage in Gulu:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(Sp.sm))

                val stages = listOf(
                    "Gulu Main Market Stage",
                    "Gulu University Gate Stage",
                    "Lacor Hospital Road Stage",
                    "Cereleno Roundabout Stage",
                    "Pece Stadium West Stage"
                )
                stages.forEach { s ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (viewModel.driverRegStage == s) Color(0xFF1E293B) else Color.Transparent)
                            .border(1.dp, if (viewModel.driverRegStage == s) ComposeColor(0xFFFDB913) else Color(0xFF334155), RoundedCornerShape(8.dp))
                            .clickable { viewModel.driverRegStage = s }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = viewModel.driverRegStage == s,
                            onClick = { viewModel.driverRegStage = s },
                            colors = RadioButtonDefaults.colors(selectedColor = ComposeColor(0xFFFDB913), unselectedColor = Color.White)
                        )
                        Spacer(modifier = Modifier.width(Sp.sm))
                        Text(s, color = Color.White, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(Sp.md))
                Text("Select Helmet Color Scheme:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(Sp.sm))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("Yellow", "Orange", "Black").forEach { color ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (viewModel.driverRegHelmetColor == color) Color(0xFF1E293B) else Color.Transparent)
                                .border(1.dp, if (viewModel.driverRegHelmetColor == color) ComposeColor(0xFFFDB913) else Color(0xFF334155), RoundedCornerShape(8.dp))
                                .clickable { viewModel.driverRegHelmetColor = color }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(color, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Sp.xl))

                BodaButton(
                    text = "Continue to Documents",
                    onClick = { viewModel.driverOnboardingStep = 3 },
                    enabled = viewModel.driverRegPlate.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            3 -> {
                // Step 3: Document Security Upload
                Text(
                    "Security Vetting & Documents",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    "Upload high-quality scans of required legal documents to verify identification.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp).align(Alignment.Start)
                )

                val docs = listOf(
                    "National ID Card (NIN)",
                    "Driving Permit (Class A)",
                    "Stage Recommendation Letter"
                )

                docs.forEach { doc ->
                    val isUploaded = viewModel.driverDocsUploaded.contains(doc)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUploaded) Color(0xFF1E293B) else Color(0xFF131A2A)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .border(1.dp, if (isUploaded) ComposeColor(0xFF10B981) else Color(0xFF334155), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(doc, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    if (isUploaded) "Document Security Approved" else "Requires High-Res JPEG Scan",
                                    color = if (isUploaded) ComposeColor(0xFF10B981) else Color(0xFF64748B),
                                    fontSize = 11.sp
                                )
                            }
                            
                            BodaButton(
                                text = if (isUploaded) "UPLOADED" else "UPLOAD",
                                onClick = { viewModel.simulateDocUpload(doc) },
                                containerColor = if (isUploaded) Color(0xFF10B981) else Color(0xFFFDB913),
                                contentColor = ComposeColor.Black,
                                modifier = Modifier.height(36.dp).widthIn(max = 120.dp)
                            )
                        }
                    }
                }

                if (viewModel.driverUploadProgress > 0f && viewModel.driverUploadProgress < 1f) {
                    Spacer(modifier = Modifier.height(Sp.md))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Uploading ${viewModel.driverDocumentType}...",
                            color = ComposeColor(0xFFFDB913),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(Sp.sm))
                        LinearProgressIndicator(
                            progress = viewModel.driverUploadProgress,
                            color = ComposeColor(0xFF10B981),
                            trackColor = Color(0xFF334155),
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Sp.xl))

                BodaButton(
                    text = "Continue to Safety Quiz",
                    onClick = { viewModel.driverOnboardingStep = 4 },
                    enabled = viewModel.driverDocsUploaded.containsAll(docs),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            4 -> {
                // Step 4: Safety & Community Quiz
                Text(
                    "Gulu Safety & Code of Conduct",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    "Verify Gulu street safety compliance rules. Select correct answers below.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp).align(Alignment.Start)
                )

                // Question 1
                BodaCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("1. What is the maximum speed limit for Bodas inside Gulu Town center?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(Sp.sm))
                        listOf("30 km/h (Town Core Limit)", "60 km/h (High Speed)", "No speed limit").forEach { choice ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.driverQuizAnswer1 = choice }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = viewModel.driverQuizAnswer1 == choice,
                                    onClick = { viewModel.driverQuizAnswer1 = choice },
                                    colors = RadioButtonDefaults.colors(selectedColor = ComposeColor(0xFFFDB913), unselectedColor = Color.White)
                                )
                                Text(choice, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Question 2
                BodaCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("2. What are you mandatory required to provide to all passengers?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(Sp.sm))
                        listOf("A clean spare Helmet and Reflector Jacket", "Nothing, passenger holds tightly", "A bottle of water").forEach { choice ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.driverQuizAnswer2 = choice }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = viewModel.driverQuizAnswer2 == choice,
                                    onClick = { viewModel.driverQuizAnswer2 = choice },
                                    colors = RadioButtonDefaults.colors(selectedColor = ComposeColor(0xFFFDB913), unselectedColor = Color.White)
                                )
                                Text(choice, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Sp.sm))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.driverTermsAccepted = !viewModel.driverTermsAccepted }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = viewModel.driverTermsAccepted,
                        onCheckedChange = { viewModel.driverTermsAccepted = it },
                        colors = CheckboxDefaults.colors(checkedColor = ComposeColor(0xFFFDB913), uncheckedColor = Color.White)
                    )
                    Spacer(modifier = Modifier.width(Sp.sm))
                    Text(
                        "I pledge to drive safely and respect all traffic codes and passengers in Gulu.",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(Sp.xl))

                BodaButton(
                    text = "Submit Safety Test",
                    onClick = { viewModel.driverOnboardingStep = 5 },
                    enabled = viewModel.driverQuizAnswer1.isNotEmpty() && viewModel.driverQuizAnswer2.isNotEmpty() && viewModel.driverTermsAccepted,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            5 -> {
                // Step 5: Verified & Welcome
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(ComposeColor(0xFF10B981).copy(alpha = 0.2f))
                        .border(2.dp, ComposeColor(0xFF10B981), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Verified", tint = ComposeColor(0xFF10B981), modifier = Modifier.size(50.dp))
                }

                Spacer(modifier = Modifier.height(Sp.md))
                Text(
                    "Congratulations & Welcome!",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.height(Sp.sm))
                Text(
                    "Your Boda-Gulu Driver Account has been dynamically approved & synchronized! You are officially registered in Gulu.",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(Sp.lg))

                BodaCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Driver Name:", color = Color(0xFF64748B), fontSize = 12.sp)
                            Text(viewModel.driverRegName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(Sp.sm))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Plate Number:", color = Color(0xFF64748B), fontSize = 12.sp)
                            Text(viewModel.driverRegPlate, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(Sp.sm))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Assigned Stage:", color = Color(0xFF64748B), fontSize = 12.sp)
                            Text(viewModel.driverRegStage, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(Sp.sm))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Helmet Preference:", color = Color(0xFF64748B), fontSize = 12.sp)
                            Text(viewModel.driverRegHelmetColor, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Sp.xxl))

                BodaButton(
                    text = "LAUNCH DRIVER COCKPIT",
                    onClick = { viewModel.completeDriverOnboarding() },
                    containerColor = Color(0xFF10B981),
                    contentColor = ComposeColor.Black,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// --- SECURE INTERACTIVE OVERLAYS ---

@Composable
fun MoMoPinDialog(viewModel: BodaViewModel) {
    val isMtn = viewModel.momoPromptProvider.contains("MTN", ignoreCase = true)
    val brandColor = if (isMtn) ComposeColor(0xFFFDB913) else Color(0xFFE4002B)
    val brandTextColor = if (isMtn) ComposeColor.Black else ComposeColor.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor.Black.copy(alpha = 0.85f))
            .padding(24.dp)
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        BodaCard(
            border = BorderStroke(2.dp, brandColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Header with Brand Name
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(brandColor)
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = viewModel.momoPromptProvider.uppercase(),
                            color = brandTextColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Secure",
                            tint = brandTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "GULU SECURE ESCROW TRANSACTION",
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(Sp.sm))
                    Text(
                        text = "Authorize payment of UGX ${viewModel.momoPromptAmount.toInt()} to Boda Gulu Wallet?",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(Sp.sm))
                    Text(
                        text = "Target Number: ${viewModel.momoPromptPhone}",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(Sp.md))

                    // PIN Mask Display
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (viewModel.momoPinInput.isEmpty()) "Enter PIN" else "â€¢ ".repeat(viewModel.momoPinInput.length),
                            color = if (viewModel.momoPinInput.isEmpty()) Color(0xFF475569) else brandColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }

                    if (viewModel.momoPinError) {
                        Text(
                            text = "Invalid PIN format. Must be 4-5 digits.",
                            color = Color(0xFFE4002B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(Sp.md))

                    // Custom Numeric Keyboard inside dialog for 100% simulated USSD feel!
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val keys = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("Clear", "0", "Delete")
                        )
                        keys.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { key ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF334155))
                                            .clickable {
                                                when (key) {
                                                    "Clear" -> viewModel.momoPinInput = ""
                                                    "Delete" -> {
                                                        if (viewModel.momoPinInput.isNotEmpty()) {
                                                            viewModel.momoPinInput = viewModel.momoPinInput.dropLast(1)
                                                        }
                                                    }
                                                    else -> {
                                                        if (viewModel.momoPinInput.length < 5) {
                                                            viewModel.momoPinInput += key
                                                            viewModel.momoPinError = false
                                                        }
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key,
                                            color = if (key in listOf("Clear", "Delete")) Color(0xFF94A3B8) else ComposeColor.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Sp.lg))

                    // Dialog Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BodaSecondaryButton(
                            text = "Cancel",
                            onClick = { viewModel.showMoMoPinDialog = false },
                            modifier = Modifier.weight(1f)
                        )

                        BodaButton(
                            text = "Approve",
                            onClick = { viewModel.confirmWalletTopupWithPin() },
                            containerColor = brandColor,
                            contentColor = brandTextColor,
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CallOverlay(viewModel: BodaViewModel) {
    var isMuted by remember { mutableStateOf(false) }
    var isSpeaker by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            Spacer(modifier = Modifier.height(Sp.xxl))

            // Subtitle
            Text(
                text = "SECURE GULU BODA-WATCH CONNECT",
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(Sp.xxl))

            // Animated pulsing circle avatar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                // Outer ring
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(ComposeColor(0xFFFDB913).copy(alpha = 0.05f))
                )
                // Inner ring
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(ComposeColor(0xFFFDB913).copy(alpha = 0.15f))
                )
                // Actual Avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(ComposeColor(0xFFFDB913)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Sp.lg))

            // Name
            Text(
                text = viewModel.callOverlayName,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.height(Sp.sm))

            // Number
            Text(
                text = viewModel.callOverlayNumber,
                color = Color(0xFF64748B),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(Sp.md))

            // Call state / timer
            val displayStatus = when (viewModel.callOverlayState) {
                "dialing" -> "Dialing Gulu Boda channel..."
                "active" -> {
                    val m = viewModel.callDurationSeconds / 60
                    val s = viewModel.callDurationSeconds % 60
                    String.format("Active â€¢ %02d:%02d", m, s)
                }
                "disconnected" -> "Call ended"
                else -> ""
            }
            Text(
                text = displayStatus,
                color = if (viewModel.callOverlayState == "active") ComposeColor(0xFF10B981) else ComposeColor(0xFFFDB913),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            // Call controls grid
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (isMuted) Color.White else Color(0xFF1E293B))
                            .clickable { isMuted = !isMuted },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute",
                            tint = if (isMuted) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(Sp.sm))
                    Text("Mute", color = Color(0xFF64748B), fontSize = 11.sp)
                }

                // Speaker
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (isSpeaker) Color.White else Color(0xFF1E293B))
                            .clickable { isSpeaker = !isSpeaker },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Speaker",
                            tint = if (isSpeaker) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(Sp.sm))
                    Text("Speaker", color = Color(0xFF64748B), fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(Sp.xxl))

            // Giant RED Decline Call Button
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE4002B))
                    .clickable { viewModel.endActiveCall() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(Sp.xxl))
        }
    }
}

@Composable
fun RiderChatOverlay(viewModel: BodaViewModel) {
    val trip = viewModel.currentSimulationTrip ?: return
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor.Black.copy(alpha = 0.5f))
            .clickable { viewModel.showRiderChatOverlay = false },
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .clickable(enabled = false) {} // block click throughs
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ComposeColor(0xFFFDB913)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = ComposeColor.Black, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(Sp.sm))
                            Column {
                                Text(trip.riderName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Vetted Rider â€¢ ${trip.riderPlate}", color = ComposeColor(0xFFFDB913), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        IconButton(onClick = { viewModel.showRiderChatOverlay = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // Scrollable Chat Message List
                val listState = rememberLazyListState()
                LaunchedEffect(viewModel.riderChatMessages.size) {
                    if (viewModel.riderChatMessages.isNotEmpty()) {
                        listState.animateScrollToItem(viewModel.riderChatMessages.size - 1)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(viewModel.riderChatMessages) { msg ->
                        val isUser = msg.sender == "user"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (isUser) 12.dp else 2.dp,
                                            bottomEnd = if (isUser) 2.dp else 12.dp
                                        )
                                    )
                                    .background(if (isUser) ComposeColor(0xFFFDB913) else Color(0xFF1E293B))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .widthIn(max = 260.dp)
                            ) {
                                Text(
                                    text = msg.message,
                                    color = if (isUser) ComposeColor.Black else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = if (isUser) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Quick pre-written localized Gulu chat chips
                val quickChips = listOf(
                    "Atye i main gate",
                    "Atye yo Pece market",
                    "Please hurry up",
                    "Atye i yo dong"
                )
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickChips) { chip ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF131A2A))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                                .clickable {
                                    viewModel.riderChatInputText = chip
                                    viewModel.sendRiderChatMessage()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(chip, color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Typing indicator
                if (viewModel.riderIsTyping) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${trip.riderName} is typing",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontStyle = FontStyle.Italic
                        )
                        Spacer(modifier = Modifier.width(Sp.xs))
                        Text("...", color = Color(0xFFFDB913), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Text Input Footer Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = viewModel.riderChatInputText,
                        onValueChange = { viewModel.onRiderChatInputChanged(it) },
                        placeholder = { Text("Write message...", color = Color(0xFF64748B), fontSize = 14.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B),
                            focusedBorderColor = Color(0xFFFDB913),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(max = 48.dp)
                    )
                    Spacer(modifier = Modifier.width(Sp.sm))
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ComposeColor(0xFFFDB913))
                            .clickable { viewModel.sendRiderChatMessage() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = ComposeColor.Black, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun OfflineSMSBookingOverlay(viewModel: BodaViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor.Black.copy(alpha = 0.85f))
            .clickable { viewModel.showOfflineSMSDialog = false },
        contentAlignment = Alignment.Center
    ) {
        BodaCard(
            border = androidx.compose.foundation.BorderStroke(1.5.dp, ComposeColor(0xFFFDB913)),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Offline Mode",
                    tint = ComposeColor(0xFFFDB913),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(Sp.sm))
                Text(
                    text = "Boda-Safe SMS Fallback Booking",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(Sp.sm))
                Text(
                    text = "You are currently offline. This booking will be dispatched via cell SMS shortcode and safely stored in your local Room SQLite database cache. Remote sync resumes automatically once you regain internet access.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(Sp.md))
                
                // Formatted Message Window
                Text(
                    text = "SMS PAYLOAD PREVIEW:",
                    color = ComposeColor(0xFFFDB913),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(Sp.xs))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("To Shortcode:", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(viewModel.offlineSMSRecipientNumber, color = ComposeColor(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(modifier = Modifier.height(Sp.sm))
                        HorizontalDivider(color = Color(0xFF1E293B))
                        Spacer(modifier = Modifier.height(Sp.sm))
                        Text(
                            text = viewModel.offlineSMSMessageBody,
                            color = Color.White,
                            fontSize = 11.sp,
                            style = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(Sp.md))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BodaSecondaryButton(
                        text = "Cancel",
                        onClick = { viewModel.showOfflineSMSDialog = false },
                        modifier = Modifier.weight(1f)
                    )
                    
                    BodaButton(
                        text = "Send via SMS",
                        onClick = { viewModel.confirmOfflineSMSBooking() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun ReferralsScreen(viewModel: BodaViewModel, referrals: List<Referral>) {
    val user by viewModel.userProfile.collectAsState()
    val myCode = user?.referralCode?.ifEmpty { "GULU-BODA-256" } ?: "GULU-BODA-256"
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }
    
    val totalCount = referrals.size
    val pendingCount = referrals.count { it.status == "pending" }
    val completedCount = referrals.count { it.status == "completed" }
    val totalEarnings = referrals.filter { it.status == "completed" }.sumOf { it.rewardAmount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        // Top Back Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .testTag("referrals_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(Sp.md))
            Text(
                text = "Refer & Earn Gulu",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        if (viewModel.activePromoMessage.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clickable { viewModel.activePromoMessage = "" }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Celebration, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(Sp.sm))
                    Text(
                        text = viewModel.activePromoMessage,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.activePromoMessage = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero card explaining the system
            item {
                BodaCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFDB913).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFFFDB913), modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(Sp.sm))
                            Column {
                                Text("Get UGX 3,000 for every friend!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Both of you get rewarded on their 1st ride", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(Sp.sm))
                        HorizontalDivider(color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(Sp.sm))
                        
                        Text("YOUR REFERRAL CODE", color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(Sp.sm))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = myCode,
                                color = Color(0xFFFDB913),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                letterSpacing = 1.sp,
                                modifier = Modifier.testTag("referral_code_text")
                            )
                            Button(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(myCode))
                                    isCopied = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isCopied) Color(0xFF10B981) else Color(0xFFFDB913)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(32.dp).testTag("copy_code_btn")
                            ) {
                                Text(
                                    text = if (isCopied) "Copied!" else "Copy",
                                    color = if (isCopied) ComposeColor.White else Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Stats grid card
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BodaCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Total Referred", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(Sp.xs))
                            Text(text = "$totalCount", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    BodaCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("In Progress", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(Sp.xs))
                            Text(text = "$pendingCount", color = Color(0xFFFDB913), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    BodaCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Total Earned", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(Sp.xs))
                            Text(text = "UGX ${totalEarnings.toInt()}", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // Referred Friends Header
            item {
                Text(
                    text = "REFERRED FRIENDS",
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // List items or empty placeholder
            if (referrals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF334155), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(Sp.sm))
                            Text("No referrals yet in Gulu.", color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(referrals) { ref ->
                    BodaCard(
                        testTag = "referral_item_${ref.id}",
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF334155)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(Sp.sm))
                                Column {
                                    Text(ref.referredName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(ref.referredPhone, color = Color(0xFF64748B), fontSize = 11.sp)
                                }
                            }
                            
                            // Status Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (ref.status == "completed") Color(0xFF10B981).copy(alpha = 0.15f)
                                        else Color(0xFFFDB913).copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (ref.status == "completed") "UGX +3,000" else "Pending 1st Ride",
                                    color = if (ref.status == "completed") Color(0xFF10B981) else Color(0xFFFDB913),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            
            // Testing / Simulation Controls
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFFFDB913), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(Sp.sm))
                            Text("GULU REFERRAL SIMULATOR", color = Color(0xFFFDB913), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(Sp.xs))
                        Text(
                            "Simulate realistic refer-a-friend operations locally to test the automated payouts and statistics updates.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(Sp.sm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.simulateNewReferralSignUp() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f).testTag("simulate_signup_btn")
                            ) {
                                Text("1. Friend Sign-up", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { viewModel.simulateReferralFirstTripCompletion() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFDB913)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f).testTag("simulate_trip_btn")
                            ) {
                                Text("2. Friend's 1st Ride", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}


