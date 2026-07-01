package com.example.ui.auth

import com.example.data.ApiClient
import com.example.data.UserProfile
import com.example.ui.BodaViewModel
import com.example.ui.Screen
import com.example.ui.home.navigateTo
import com.example.ui.home.stopLocationTracking
import com.example.ui.home.syncUserToBackend
import com.example.ui.BodaViewModel.Companion.GOOGLE_WEB_CLIENT_ID
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
import kotlin.random.Random

// Auth functions — defined as extensions on BodaViewModel
// They will be moved to a true separate ViewModel in Phase 9

fun BodaViewModel.startOtpFlow(activity: android.app.Activity? = null) {
    if (phoneInput.length < 9) {
        errorMessage.value = "Phone number must be at least 9 digits."
        return
    }

    val phoneNumber = "+256" + phoneInput.removePrefix("+256").trim()

    isSendingOtp = true
    otpSent = true
    otpInput = ""
    otpResendTimer = 45
    isOtpVerified = false

    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            val code = credential.smsCode
            if (code != null) {
                otpInput = code
                isSendingOtp = false
                verifyOtp()
            }
        }

        override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
            errorMessage.value = "Verification failed: ${e.message}"
            otpSent = false
            isSendingOtp = false
        }

        override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
            verificationId = id
            resendToken = token
            isSendingOtp = false
        }
    }

    val builder = PhoneAuthOptions.newBuilder(auth)
        .setPhoneNumber(phoneNumber)
        .setTimeout(60L, TimeUnit.SECONDS)
        .setCallbacks(callbacks)
    activity?.let { builder.setActivity(it) }
    val options = builder.build()

    PhoneAuthProvider.verifyPhoneNumber(options)

    otpTimerJob?.cancel()
    otpTimerJob = viewModelScope.launch {
        while (otpResendTimer > 0) {
            delay(1000)
            otpResendTimer--
        }
    }
}

fun BodaViewModel.verifyOtp() {
    val vid = verificationId
    if (vid == null) {
        errorMessage.value = "Verification not started. Please request a new code."
        return
    }

    if (otpInput.length != 6) {
        errorMessage.value = "Please enter the 6-digit code."
        return
    }

    isVerifyingOtp = true
    val credential = PhoneAuthProvider.getCredential(vid, otpInput)

    auth.signInWithCredential(credential)
        .addOnSuccessListener {
            isVerifyingOtp = false
            otpTimerJob?.cancel()
            ApiClient.invalidateToken()

            viewModelScope.launch {
                val backendResult = apiRepository.fetchUserProfile()
                backendResult.fold(
                    onSuccess = { backendUser ->
                        val cleanPhone = backendUser.phone
                            ?.takeIf { it.isNotEmpty() && it != "+256770000000" && it != "256770000000" }
                            ?: ""
                        val profile = UserProfile(
                            id = 1,
                            name = backendUser.full_name,
                            phoneNumber = cleanPhone.ifEmpty {
                                "+256 " + phoneInput.removePrefix("+256").trim()
                            },
                            language = backendUser.language.ifEmpty { appLanguage },
                            isSetupComplete = backendUser.full_name.isNotEmpty(),
                            referralCode = backendUser.referral_code ?: ""
                        )
                        repository.saveUserProfile(profile)
                        backendBalance = backendUser.wallet_balance

                        if (profile.isSetupComplete) {
                            fetchBackendData(force = true)
                            navigateTo(Screen.Home)
                        } else {
                            isOtpVerified = true
                        }
                    },
                    onFailure = {
                        isOtpVerified = true
                    }
                )
            }
        }
        .addOnFailureListener { e ->
            errorMessage.value = "Invalid code: ${e.message}"
            isVerifyingOtp = false
        }
}

fun BodaViewModel.completeProfileSetup() {
    viewModelScope.launch {
        val genCode = "GULU-${signupName.filter { it.isLetter() }.take(4).uppercase().ifEmpty { "BODA" }}-${Random.nextInt(100, 999)}"
        val profile = UserProfile(
            name = signupName,
            phoneNumber = if (phoneInput.isNotEmpty()) {
                "+256 " + phoneInput.removePrefix("+256").trim()
            } else {
                auth.currentUser?.phoneNumber ?: ""
            },
            language = appLanguage,
            isSetupComplete = true,
            profileImageResId = selectedAvatarRes,
            referralCode = genCode
        )
        repository.saveUserProfile(profile)
        syncUserToBackend(profile)

        if (referralCodeInput.isNotEmpty()) {
            repository.addReferral(com.example.data.Referral(
                referredName = signupName,
                referredPhone = profile.phoneNumber,
                referralCodeUsed = referralCodeInput.uppercase().trim(),
                status = "pending",
                timestamp = System.currentTimeMillis(),
                rewardAmount = 3000.0
            ))
            apiRepository.addReferralToBackend(
                signupName, profile.phoneNumber, referralCodeInput.uppercase().trim()
            )
        }

        val welcomeBonusShown = prefs.getBoolean("welcome_bonus_shown", false)
        if (!welcomeBonusShown) {
            isNewUserSession = true
            showWelcomeBonus = true
        }

        navigateTo(Screen.Home)
    }
}

fun BodaViewModel.signOut() {
    stopLocationTracking()
    auth.signOut()

    isOtpVerified = false
    otpSent = false
    otpInput = ""
    phoneInput = ""
    signupName = ""
    verificationId = null
    resendToken = null
    backendBalance = null
    lastBackendFetchMs = 0L

    prefs.edit()
        .apply()

    viewModelScope.launch {
        repository.clearAllUserData()
    }

    ApiClient.invalidateToken()

    backStack.clear()
    currentScreen = Screen.WelcomeOnboarding
}

fun BodaViewModel.deleteAccount() {
    viewModelScope.launch {
        apiRepository.deleteAccount().onSuccess {
            addPostgresLog("Account deleted from backend")
        }.onFailure { e ->
            addPostgresLog("Backend delete failed: ${e.message}")
        }

        withContext(Dispatchers.IO) {
            try {
                Tasks.await(auth.currentUser?.delete() ?: Tasks.forResult(null))
            } catch (_: Exception) {}
        }

        auth.signOut()
        ApiClient.invalidateToken()

        repository.clearAllUserData()
        prefs.edit().clear().apply()

        isOtpVerified = false
        otpSent = false
        otpInput = ""
        phoneInput = ""
        signupName = ""
        verificationId = null
        resendToken = null
        backendBalance = null
        lastBackendFetchMs = 0L
        onboardingCarouselCompleted = false
        onboardingLanguageSelected = false
        onboardingSlideIndex = 0

        backStack.clear()
        currentScreen = Screen.WelcomeOnboarding
    }
}

fun BodaViewModel.signInWithGoogle(context: android.content.Context) {
    isSigningInWithGoogle = true
    googleSignInError = null

    viewModelScope.launch {
        try {
            android.util.Log.d("BODA_GOOGLE", "Starting Google Sign-In flow...")

            val googleIdOption = com.google.android.libraries.identity.googleid
                .GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(GOOGLE_WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()

            val request = androidx.credentials.GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            android.util.Log.d("BODA_GOOGLE", "Launching credential picker...")

            val credentialManager = androidx.credentials.CredentialManager.create(context)
            val result = credentialManager.getCredential(context, request)

            android.util.Log.d("BODA_GOOGLE", "Got credential result")

            val credential = result.credential
            if (credential !is androidx.credentials.CustomCredential ||
                credential.type != com.google.android.libraries.identity.googleid
                    .GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                throw Exception("Unexpected credential type: ${credential.type}")
            }

            val googleIdTokenCredential = com.google.android.libraries.identity.googleid
                .GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken

            android.util.Log.d("BODA_GOOGLE", "Got ID token, exchanging for Firebase credential...")

            val firebaseCredential = com.google.firebase.auth.GoogleAuthProvider
                .getCredential(idToken, null)

            android.util.Log.d("BODA_GOOGLE", "Signing in to Firebase...")

            val authResult = withContext(Dispatchers.IO) {
                Tasks.await(auth.signInWithCredential(firebaseCredential))
            }

            val firebaseUser = authResult.user
                ?: throw Exception("Firebase sign-in succeeded but user is null")

            android.util.Log.d("BODA_GOOGLE", "Signed in: ${firebaseUser.uid} ${firebaseUser.email}")

            ApiClient.invalidateToken()

            android.util.Log.d("BODA_GOOGLE", "Fetching user profile from backend...")

            val backendResult = apiRepository.fetchUserProfile()

            backendResult.fold(
                onSuccess = { backendUser ->
                    android.util.Log.d("BODA_GOOGLE", "Backend profile found: ${backendUser.full_name}")
                    val cleanPhone = backendUser.phone
                        ?.takeIf { it.isNotEmpty() && it != "+256770000000" && it != "256770000000" }
                        ?: ""
                    val profile = UserProfile(
                        id = 1,
                        name = backendUser.full_name.ifEmpty { firebaseUser.displayName ?: "Gulu Rider" },
                        phoneNumber = cleanPhone.ifEmpty { firebaseUser.phoneNumber ?: "" },
                        language = backendUser.language.ifEmpty { appLanguage },
                        isSetupComplete = backendUser.full_name.isNotEmpty(),
                        referralCode = backendUser.referral_code ?: ""
                    )
                    repository.saveUserProfile(profile)
                    backendBalance = backendUser.wallet_balance

                    if (profile.isSetupComplete) {
                        fetchBackendData(force = true)
                        navigateTo(Screen.Home)
                    } else {
                        isOtpVerified = true
                        isSigningInWithGoogle = false
                    }
                },
                onFailure = {
                    android.util.Log.d("BODA_GOOGLE", "Backend profile not found — new user")
                    isOtpVerified = true
                    isSigningInWithGoogle = false
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("BODA_GOOGLE", "Google sign-in failed", e)
            errorMessage.value = "Google sign-in failed: ${e.message}"
            googleSignInError = e.message
            isSigningInWithGoogle = false
        }
    }
}
