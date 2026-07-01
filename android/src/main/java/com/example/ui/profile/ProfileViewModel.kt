package com.example.ui.profile

import androidx.lifecycle.viewModelScope
import com.example.data.EmergencyContact
import com.example.data.SavedPlace
import com.example.ui.BodaViewModel
import com.example.ui.ChatMessage
import com.example.ui.Screen
import com.example.ui.SupportTicket
import com.example.ui.home.navigateTo
import com.example.ui.home.navigateBack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

fun BodaViewModel.submitSupportTicket() {
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

fun BodaViewModel.sendSupportChatMessage() {
    if (newChatMessageText.isEmpty()) return
    activeChatMessages.add(ChatMessage("user", newChatMessageText))

    val reply = when {
        newChatMessageText.contains("price", ignoreCase = true) || newChatMessageText.contains("fare", ignoreCase = true) ->
            "Fares in Gulu are calculated based on distance. Rides start at 2,000 UGX and deliveries at 3,000 UGX."
        newChatMessageText.contains("payment", ignoreCase = true) || newChatMessageText.contains("money", ignoreCase = true) ->
            "We accept MTN MoMo, Airtel Money, and Boda Wallet balances."
        else -> "Thank you. Our Gulu response officer will reply in 2-3 minutes. Call 0800 112 112 for urgent safety!"
    }

    newChatMessageText = ""

    viewModelScope.launch {
        delay(1500)
        activeChatMessages.add(ChatMessage("agent", reply))
    }
}

fun BodaViewModel.addSavedPlace() {
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

fun BodaViewModel.removeSavedPlace(place: SavedPlace) {
    viewModelScope.launch {
        repository.removeSavedPlace(place)
        apiRepository.deleteSavedPlaceFromBackend(place.id)
    }
}

fun BodaViewModel.addEmergencyContact() {
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

fun BodaViewModel.removeEmergencyContact(contact: EmergencyContact) {
    viewModelScope.launch {
        repository.removeEmergencyContact(contact)
        apiRepository.deleteEmergencyContactFromBackend(contact.id)
    }
}
