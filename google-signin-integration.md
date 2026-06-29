# Google Sign-In Integration — Boda Gulu

This guide adds Google Sign-In alongside your existing Firebase Phone OTP flow.
All changes are drop-in: no existing code is deleted, only extended.

**Firebase project:** `boda-app-99092`  
**Web client ID (already in google-services.json):**
`387194086675-2c2v0c3rk9o7v49cm998gu48qgaqp6pn.apps.googleusercontent.com`

---

## 1. Dependencies

### `gradle/libs.versions.toml` — add these entries

```toml
[versions]
# ... existing entries ...
googleid = "1.1.1"
credentialsManager = "1.5.0"

[libraries]
# ... existing entries ...
androidx-credentials = { group = "androidx.credentials", name = "credentials", version.ref = "credentialsManager" }
androidx-credentials-play-services = { group = "androidx.credentials", name = "credentials-play-services-auth", version.ref = "credentialsManager" }
googleid = { group = "com.google.android.libraries.identity.googleid", name = "googleid", version.ref = "googleid" }
```

### `app/build.gradle.kts` — add to `dependencies {}`

```kotlin
implementation(libs.androidx.credentials)
implementation(libs.androidx.credentials.play.services)
implementation(libs.googleid)
```

> **Why Credential Manager instead of the old Google Sign-In SDK?**
> The legacy `com.google.android.gms:play-services-auth` is deprecated as of 2024.
> `androidx.credentials` (Credential Manager) is the current Google-recommended API,
> works on Android 4.4+, and shows the native bottom-sheet account picker automatically.

---

## 2. ViewModel — `BodaViewModel.kt`

Add the following to `BodaViewModel`. Place the new state variables near the
existing OTP state block (around line 1238), and the new functions near `startOtpFlow()`.

### 2a. New state variables

```kotlin
// Google Sign-In state — add near the OTP state block
var isSigningInWithGoogle by mutableStateOf(false)
    private set
var googleSignInError by mutableStateOf<String?>(null)
    private set
```

### 2b. The web client ID constant

```kotlin
companion object {
    // From google-services.json → oauth_client → client_type 3
    const val GOOGLE_WEB_CLIENT_ID =
        "387194086675-2c2v0c3rk9o7v49cm998gu48qgaqp6pn.apps.googleusercontent.com"
}
```

### 2c. `signInWithGoogle()` — the main function

```kotlin
/**
 * Launches the Google account picker via Credential Manager, exchanges the
 * Google ID token for a Firebase credential, signs in, then syncs the
 * user profile to Room and the backend exactly like the OTP path does.
 *
 * @param context  Pass LocalContext.current from the composable.
 */
fun signInWithGoogle(context: android.content.Context) {
    isSigningInWithGoogle = true
    googleSignInError = null

    viewModelScope.launch {
        try {
            // 1. Build the Google ID token request
            val googleIdOption = com.google.android.libraries.identity.googleid
                .GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)   // show all accounts, not just previously used ones
                .setServerClientId(GOOGLE_WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)            // always show the picker — do not auto-pick
                .build()

            val request = androidx.credentials.GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // 2. Launch the system account picker (bottom sheet)
            val credentialManager = androidx.credentials.CredentialManager.create(context)
            val result = credentialManager.getCredential(context, request)

            // 3. Extract the Google ID token from the result
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

            // 4. Exchange Google ID token for a Firebase credential and sign in
            val firebaseCredential = com.google.firebase.auth.GoogleAuthProvider
                .getCredential(idToken, null)
            val authResult = withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.google.android.gms.tasks.Tasks.await(
                    auth.signInWithCredential(firebaseCredential)
                )
            }

            val firebaseUser = authResult.user
                ?: throw Exception("Firebase sign-in succeeded but user is null")

            android.util.Log.d("BODA_GOOGLE", "Signed in: ${firebaseUser.uid} ${firebaseUser.email}")

            // 5. Mark as verified and populate phone input if available
            isOtpVerified = true
            prefs.edit().putBoolean("otp_verified", true).apply()
            phoneInput = firebaseUser.phoneNumber?.removePrefix("+256") ?: ""

            // 6. Try to restore profile from backend first (returning user path)
            val backendResult = apiRepository.fetchUserProfile()

            backendResult.fold(
                onSuccess = { backendUser ->
                    // Returning user — profile already exists on backend
                    val profile = UserProfile(
                        id = 1,
                        name = backendUser.full_name.ifEmpty {
                            // Fall back to Google display name if backend name is blank
                            firebaseUser.displayName ?: ""
                        },
                        phoneNumber = backendUser.phone.ifEmpty {
                            firebaseUser.phoneNumber ?: ""
                        },
                        language = backendUser.language.ifEmpty { appLanguage },
                        isSetupComplete = backendUser.full_name.isNotEmpty(),
                        referralCode = backendUser.referral_code
                    )
                    repository.saveUserProfile(profile)
                    backendBalance = backendUser.wallet_balance

                    if (profile.isSetupComplete) {
                        // Known returning user — go straight to Home
                        fetchBackendData(force = true)
                        navigateTo(Screen.Home)
                    } else {
                        // Account exists but profile setup was never finished
                        // Pre-fill the name field from Google account
                        signupName = firebaseUser.displayName ?: ""
                        navigateTo(Screen.WelcomeOnboarding)
                    }
                },
                onFailure = {
                    // New user — pre-fill name from Google and send them to profile setup
                    // isOtpVerified = true, so OnboardingScreen will show Step 3 (profile setup)
                    signupName = firebaseUser.displayName ?: ""
                    // Note: phone may be empty for Google-only users — that is fine,
                    // completeProfileSetup() handles empty phoneInput gracefully
                    navigateTo(Screen.WelcomeOnboarding)
                }
            )

        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
            // User dismissed the picker — not an error, just reset
            android.util.Log.d("BODA_GOOGLE", "Google sign-in cancelled by user")
        } catch (e: androidx.credentials.exceptions.NoCredentialException) {
            googleSignInError = "No Google account found on this device. Please add a Google account in Settings."
            errorMessage.value = googleSignInError
        } catch (e: Exception) {
            android.util.Log.e("BODA_GOOGLE", "Google sign-in failed: ${e.message}", e)
            googleSignInError = "Google sign-in failed: ${e.message}"
            errorMessage.value = googleSignInError
        } finally {
            isSigningInWithGoogle = false
        }
    }
}
```

### 2d. Update `completeProfileSetup()` to handle Google users with no phone number

Google Sign-In users may not have a phone number attached to their Firebase account.
Replace the `phoneNumber` construction line in your existing `completeProfileSetup()`:

```kotlin
// BEFORE (line ~1851 in BodaViewModel.kt):
phoneNumber = "+256 " + phoneInput.removePrefix("+256").trim(),

// AFTER:
phoneNumber = if (phoneInput.isNotEmpty()) {
    "+256 " + phoneInput.removePrefix("+256").trim()
} else {
    auth.currentUser?.phoneNumber ?: ""
},
```

---

## 3. UI — `BodaScreens.kt`

In `OnboardingScreen`, add the Google Sign-In button to the phone entry step
(the `if (!viewModel.otpSent)` branch, after the existing "Get Verification Code" button).

### 3a. The Google Sign-In button composable

Add this composable anywhere in the file (e.g. near your other `BodaButton` helpers):

```kotlin
@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF1F1F1F),
            disabledContainerColor = Color.White.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, Color(0xFFDADCE0))
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color(0xFF4285F4),
                strokeWidth = 2.dp
            )
        } else {
            // Google "G" logo drawn with Canvas — no image asset needed
            Canvas(modifier = Modifier.size(20.dp)) {
                val w = size.width
                val h = size.height
                // Blue arc
                drawArc(Color(0xFF4285F4), -10f, 100f, false,
                    topLeft = Offset(0f, 0f), size = Size(w, h), style = Stroke(w * 0.18f))
                // Red arc
                drawArc(Color(0xFFEA4335), 90f, 90f, false,
                    topLeft = Offset(0f, 0f), size = Size(w, h), style = Stroke(w * 0.18f))
                // Yellow arc
                drawArc(Color(0xFFFBBC05), 180f, 90f, false,
                    topLeft = Offset(0f, 0f), size = Size(w, h), style = Stroke(w * 0.18f))
                // Green arc
                drawArc(Color(0xFF34A853), 270f, 90f, false,
                    topLeft = Offset(0f, 0f), size = Size(w, h), style = Stroke(w * 0.18f))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "Continue with Google",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF3C4043)
            )
        }
    }
}
```

### 3b. Add the button to the phone entry step

Find the `if (!viewModel.otpSent)` block in `OnboardingScreen` and add after the
existing "LOGIN / REGISTER AS DRIVER" button:

```kotlin
if (!viewModel.otpSent) {
    // ... existing phone input and "Get Verification Code" button ...
    // ... existing "LOGIN / REGISTER AS DRIVER" button ...

    // ── ADD FROM HERE ──────────────────────────────────────────────

    Spacer(modifier = Modifier.height(Sp.md))

    // Divider with "or" label
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color(0xFF334155)
        )
        Text(
            "  or  ",
            color = Color(0xFF64748B),
            fontSize = 12.sp
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color(0xFF334155)
        )
    }

    Spacer(modifier = Modifier.height(Sp.md))

    val context = LocalContext.current
    GoogleSignInButton(
        onClick = { viewModel.signInWithGoogle(context) },
        isLoading = viewModel.isSigningInWithGoogle,
        modifier = Modifier.fillMaxWidth()
    )

    // ── TO HERE ───────────────────────────────────────────────────
}
```

### 3c. Pre-fill name in profile setup step

In the profile setup step (`else` branch after `isOtpVerified`), the name field
already binds to `viewModel.signupName`. Because `signInWithGoogle()` sets
`signupName = firebaseUser.displayName ?: ""` before navigating, the field will
be pre-filled automatically for Google users. No change needed here.

---

## 4. Backend — `server.js`

Google users may not have a phone number. The `/api/users/sync` endpoint
currently falls back to `'+256770000000'` when phone is missing, which is wrong.

```javascript
// BEFORE (server.js line 85):
const phoneNum = phone || req.user.phone_number || '+256770000000';

// AFTER:
const phoneNum = phone || req.user.phone_number || null;

// And update the INSERT to allow null phone:
const query = `
  INSERT INTO users (uid, phone, full_name, email, wallet_balance, language, referral_code)
  VALUES ($1, $2, $3, $4, 0.00, $5, $6)
  ON CONFLICT (uid) DO UPDATE
  SET full_name = $3,
      phone = COALESCE(EXCLUDED.phone, users.phone),
      language = $5,
      referral_code = $6,
      updated_at = NOW()
  RETURNING *;
`;
```

The `COALESCE` on phone ensures that if a user registers with Google (no phone),
then later links their phone number via OTP, the phone number is preserved on
re-sync rather than overwritten with null.

---

## 5. How the two auth paths now coexist

```
App Launch
    │
    ├── auth.currentUser != null ──► restoreSessionFromBackend() ──► Home
    │
    └── auth.currentUser == null ──► WelcomeOnboarding
                                          │
                              ┌───────────┴───────────┐
                              │                       │
                    [ Phone + OTP path ]    [ Google Sign-In path ]
                              │                       │
                    OTP verified                Google ID token
                    isOtpVerified = true        exchanged for Firebase cred
                              │                       │
                              └───────────┬───────────┘
                                          │
                                   Firebase user exists
                                          │
                              ┌───────────┴───────────┐
                              │                       │
                      Returning user            New user
                      (backend has profile)     (no backend profile)
                              │                       │
                         Go to Home           Profile setup screen
                                              (name pre-filled from Google)
                                                      │
                                              completeProfileSetup()
                                              syncUserToBackend()
                                                      │
                                                 Go to Home
```

---

## 6. Firebase Console setup (one-time)

1. Go to **Firebase Console → Authentication → Sign-in method**
2. Enable **Google** as a provider
3. Set the **Project support email** (required)
4. Under **Authorized domains**, confirm `ryd-api.ocaya.space` is listed
   (needed if you ever use Firebase Hosting redirects)
5. The web client ID in step 2c above (`387194086675-2c2v0c3...`) is already
   present in your `google-services.json` — no manual copy-paste needed

---

## 7. Testing checklist

| Scenario | Expected result |
|---|---|
| New user taps Google, picks account | Account picker shows → profile setup screen with name pre-filled |
| Returning Google user opens app | Goes straight to Home (restoreSessionFromBackend succeeds) |
| User taps Google then dismisses picker | Nothing happens, phone input remains visible |
| No Google account on device | Snackbar: "No Google account found on this device" |
| Google user completes profile setup | Room saved, backend synced, Home screen |
| Google user has no phone number | Profile saved with empty phone; sync sends null phone to backend |
| OTP user and Google user sign in alternately | Each gets their own Room profile (after Issue #1 fix) |
| Google sign-in while offline | `NoCredentialException` caught, error shown in Snackbar |
