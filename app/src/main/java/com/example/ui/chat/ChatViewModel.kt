package com.example.ui.chat

import com.example.ui.BodaViewModel
import com.example.ui.ChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun BodaViewModel.initiateCall(name: String, phone: String) {
    callOverlayName = name
    callOverlayNumber = phone
    callOverlayState = "dialing"
    callDurationSeconds = 0
    showCallOverlay = true
    
    callTimerJob?.cancel()
    callTimerJob = viewModelScope.launch {
        delay(3000)
        callOverlayState = "active"
        while (callOverlayState == "active") {
            delay(1000)
            callDurationSeconds++
        }
    }
}

fun BodaViewModel.endActiveCall() {
    callOverlayState = "disconnected"
    callTimerJob?.cancel()
    viewModelScope.launch {
        delay(1000)
        showCallOverlay = false
    }
}

fun BodaViewModel.openRiderChat() {
    showRiderChatOverlay = true
    val trip = currentSimulationTrip ?: return
    currentChatTripId = trip.id

    webSocketClient.joinTripChannel(trip.id.toString())

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

fun BodaViewModel.sendRiderChatMessage() {
    val text = riderChatInputText.trim()
    if (text.isEmpty()) return
    val trip = currentSimulationTrip ?: return
    val uid = auth.currentUser?.uid ?: ""
    val name = userProfile.value?.name ?: "Rider"

    riderChatMessages.add(ChatMessage(sender = "user", message = text))
    riderChatInputText = ""

    webSocketClient.sendChatMessage(
        tripId = trip.id,
        senderUid = uid,
        senderName = name,
        senderRole = "rider",
        message = text
    )

    webSocketClient.sendTypingIndicator(trip.id, uid, name, false)
}

fun BodaViewModel.onRiderChatInputChanged(text: String) {
    riderChatInputText = text
    val trip = currentSimulationTrip ?: return
    val uid = auth.currentUser?.uid ?: ""
    val name = userProfile.value?.name ?: "Rider"
    webSocketClient.sendTypingIndicator(trip.id, uid, name, text.isNotEmpty())
}
