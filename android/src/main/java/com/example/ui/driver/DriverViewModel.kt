package com.example.ui.driver

import androidx.lifecycle.viewModelScope
import com.example.data.WalletTransaction
import com.example.ui.BodaViewModel
import com.example.ui.Screen
import com.example.ui.home.navigateTo
import com.example.ui.home.startLocationTracking
import com.example.ui.home.stopLocationTracking
import com.example.ui.home.refreshWalletBalance
import com.example.ui.components.getLatLngForPlace
import com.example.ui.ride.fetchRouteForPoints
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

fun BodaViewModel.registerDriverViaBackend() {
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

fun BodaViewModel.updateDriverStatusViaBackend(isOnline: Boolean) {
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

fun BodaViewModel.startDriverOnboarding() {
    driverOnboardingStep = 1
    driverUploadProgress = 0f
    driverDocsUploaded = emptySet()
    driverTermsAccepted = false
    driverQuizAnswer1 = ""
    driverQuizAnswer2 = ""
    userProfile.value?.let {
        if (driverRegName.isEmpty()) driverRegName = it.name
        if (driverRegPhone.isEmpty()) driverRegPhone = it.phoneNumber
    }
    navigateTo(Screen.DriverOnboarding)
}

fun BodaViewModel.simulateDocUpload(docType: String) {
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

fun BodaViewModel.completeDriverOnboarding() {
    isDriverRegistered = true
    isDriverMode = true
    isDriverOnline = true
    driverTripState = "none"
    navigateTo(Screen.Home)
}

fun BodaViewModel.toggleDriverOnline() {
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

fun BodaViewModel.driverAcceptTrip() {
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
        while (driverSimulationCountdown > 0) {
            delay(1000)
            driverSimulationCountdown--
            driverSimulationProgress = (8 - driverSimulationCountdown) / 8f
        }
        driverTripState = "pickup_arrived"
    }
}

fun BodaViewModel.driverRejectTrip() {
    driverIncomingRequest = null
    driverTripState = "none"
}

fun BodaViewModel.driverArrivePickup() {
    driverTripState = "pickup_arrived"
}

fun BodaViewModel.driverStartTrip() {
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
        while (driverSimulationCountdown > 0) {
            delay(1000)
            driverSimulationCountdown--
            driverSimulationProgress = (10 - driverSimulationCountdown) / 10f
        }
        driverTripState = "completed"
        driverCompleteTrip()
    }
}

fun BodaViewModel.driverCompleteTrip() {
    val trip = driverActiveTrip ?: return
    viewModelScope.launch {
        driverEarnings += trip.fare
        driverCompletedTrips += 1
        apiRepository.updateTripStatus(trip.id, "completed")

        repository.addTrip(trip.copy(id = 0, status = "completed"))

        repository.addTransaction(
            WalletTransaction(
                amount = trip.fare,
                type = "topup",
                status = "completed",
                phoneNumber = userProfile.value?.phoneNumber ?: "+256 772 123456",
                timestamp = System.currentTimeMillis(),
                provider = trip.paymentMethod,
                reference = "BODA-DRV-PAY-" + (1000 + Random.nextInt(9000))
            )
        )

        driverActiveTrip = null
        driverTripState = "none"
        refreshWalletBalance()
    }
}

fun BodaViewModel.updateDriverLocation() {
    updateDriverStatusViaBackend(isDriverOnline)
}
