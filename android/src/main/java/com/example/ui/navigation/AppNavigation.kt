package com.example.ui

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import com.example.ui.components.*
import com.example.ui.auth.SplashScreen
import com.example.ui.auth.OnboardingScreen
import com.example.ui.home.HomeScreen
import com.example.ui.home.BodaBottomNavigation
import com.example.ui.home.navigateTo
import com.example.ui.home.updateLanguage
import com.example.ui.home.startLocationTracking
import com.example.ui.driver.DriverOnboardingScreen
import com.example.ui.ride.ActiveTripScreen
import com.example.ui.ride.PostTripScreen
import com.example.ui.ride.SearchPlacesScreen
import com.example.ui.ride.RoutePreviewScreen
import com.example.ui.ride.MatchingScreen
import com.example.ui.ride.RiderEnRouteScreen
import com.example.ui.wallet.WalletScreen
import com.example.ui.profile.ProfileSettingsScreen
import com.example.ui.profile.EmergencyContactsScreen
import com.example.ui.profile.SavedPlacesManageScreen
import com.example.ui.profile.SupportScreen
import com.example.ui.referrals.ReferralsScreen
import com.example.ui.offline.OfflineSMSBookingOverlay
import com.example.ui.chat.RiderChatOverlay
import com.example.ui.trips.TripsHistoryScreen
import com.example.ui.theme.NunitoFamily
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalPermissionsApi::class)
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
                } else {
                    viewModel.navigateTo(Screen.WelcomeOnboarding)
                }
            }
        }
    }

    LaunchedEffect(viewModel.locationPermissionGranted) {
        if (viewModel.locationPermissionGranted) {
            viewModel.startLocationTracking()
        }
    }

    val locationPermOnResume = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    LaunchedEffect(locationPermOnResume.status) {
        if (locationPermOnResume.status.isGranted && !viewModel.locationPermissionGranted) {
            viewModel.locationPermissionGranted = true
        }
    }

    val scaffoldBg = MaterialTheme.colorScheme.background

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(scaffoldBg)
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
