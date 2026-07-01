package com.example.ui.offline

import androidx.lifecycle.viewModelScope
import com.example.data.Trip
import com.example.ui.BodaViewModel
import com.example.ui.Screen
import com.example.ui.home.navigateTo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

fun BodaViewModel.triggerOfflineSMSBookingFlow() {
    if (pickupPlace == null || dropoffPlace == null) return

    offlineSMSRecipientNumber = "8080"
    val modeText = if (serviceType == "ride") "RIDE" else "DELIVERY"
    offlineSMSMessageBody = "BODA-GULU REQUEST $modeText: FROM [${pickupPlace!!.label}] TO [${dropoffPlace!!.label}] ON BODA-SAFE ESCROW. FARE UGX ${calculatedFare.toInt()}. PAY BY $selectedPaymentMethod."
    showOfflineSMSDialog = true
}

fun BodaViewModel.confirmOfflineSMSBooking() {
    if (pickupPlace == null || dropoffPlace == null) return
    
    showOfflineSMSDialog = false
    
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
        status = "offline_pending",
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
        
        simulationState = "enroute"
        simulationCountdown = 8
        navigateTo(Screen.RiderEnRoute)
        
        while (simulationCountdown > 0) {
            delay(1000)
            simulationCountdown--
        }
        simulationState = "trip_started"
        navigateTo(Screen.ActiveTrip)
    }
}

fun BodaViewModel.dispatchSOSSMS() {
    val message = "EMERGENCY ALERT: I am on a Boda ride in Gulu and triggered SOS. Track me here: https://boda-gulu.ug/track/GULU-SECURE-SOS"

    emergencySMSDispatchLogs.clear()
    viewModelScope.launch {
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

        emergencyContacts.value.forEach { contact ->
            emergencySMSDispatchLogs.add("SMS dispatched to ${contact.name} (${contact.phoneNumber}): \"$message\"")
            delay(400)
        }
    }
}
