package com.example.ui

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
