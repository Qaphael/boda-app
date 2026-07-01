package com.example.ui.referrals

import androidx.lifecycle.viewModelScope
import com.example.data.Referral
import com.example.data.WalletTransaction
import com.example.ui.BodaViewModel
import kotlinx.coroutines.launch
import kotlin.random.Random

fun BodaViewModel.simulateNewReferralSignUp() {
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

fun BodaViewModel.simulateReferralFirstTripCompletion() {
    viewModelScope.launch {
        val list = referrals.value
        val pending = list.firstOrNull { it.status == "pending" }
        if (pending != null) {
            val completed = pending.copy(status = "completed", timestamp = System.currentTimeMillis())
            repository.addReferral(completed)
            
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
