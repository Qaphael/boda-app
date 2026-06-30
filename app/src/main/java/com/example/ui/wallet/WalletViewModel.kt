package com.example.ui.wallet

import com.example.data.WalletTransaction
import com.example.ui.BodaViewModel
import kotlinx.coroutines.launch

fun BodaViewModel.startWalletTopup() {
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

fun BodaViewModel.confirmWalletTopupWithPin() {
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
