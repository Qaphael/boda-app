# Onboarding Improvements — Boda Gulu

Covers four improvements in one document:

1. **Referral deep links** — Firebase Dynamic Links pre-fill the referral code on install
2. **Phone pre-fill from SIM** — Android Hint API auto-fills the phone field
3. **Deferred permissions** — move location + notification grants out of registration
4. **Progress indicator** — "Step 1 of 3" bar during signup
5. **Welcome bonus** — surface the UGX 3,000 reward the moment the user lands on Home

Each section lists the exact files and lines to change.

---

## 1. Referral Deep Links

### How it works

A sharing user taps "Share Code" in `ReferralsScreen`. Instead of copying plain text,
the app generates a Firebase Dynamic Link like:

```
https://bodagulu.page.link/ref?code=GULU-OKENY-472
```

When a new user taps this link:
- If the app is installed → opens directly into the onboarding screen with the
  referral code field pre-filled
- If the app is not installed → takes the user to the Play Store, installs the app,
  then on first launch the Dynamic Link is still delivered and the code is pre-filled

### 1a. Dependencies

Add to `gradle/libs.versions.toml`:

```toml
[versions]
firebaseDynamicLinks = "22.1.0"

[libraries]
firebase-dynamic-links = { group = "com.google.firebase", name = "firebase-dynamic-links-ktx", version.ref = "firebaseDynamicLinks" }
```

Add to `app/build.gradle.kts`:

```kotlin
implementation(libs.firebase.dynamic.links)
```

### 1b. Firebase Console setup (one-time)

1. Firebase Console → **Dynamic Links** → Get Started
2. Set your URL prefix: `https://bodagulu.page.link`
3. Add your Android app package name: `com.qaphael.bodaapp`
4. No iOS needed

### 1c. `AndroidManifest.xml` — intent filter

Add inside the `<activity>` tag for `MainActivity`:

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW"/>
    <category android:name="android.intent.category.DEFAULT"/>
    <category android:name="android.intent.category.BROWSABLE"/>
    <data android:scheme="https"
          android:host="bodagulu.page.link"/>
</intent-filter>
```

### 1d. `BodaViewModel.kt` — two new functions

**Function 1: handle the incoming deep link on app launch**

Call this from `init{}`, after the existing Firebase session check:

```kotlin
fun handleDeepLink(intent: android.content.Intent?) {
    intent ?: return
    com.google.firebase.dynamiclinks.ktx.dynamicLinks
        .getDynamicLink(intent)
        .addOnSuccessListener { pendingDynamicLinkData ->
            val deepLink = pendingDynamicLinkData?.link ?: return@addOnSuccessListener
            val code = deepLink.getQueryParameter("code") ?: return@addOnSuccessListener
            if (code.isNotEmpty()) {
                referralCodeInput = code.uppercase().trim()
                android.util.Log.d("BODA_DEEPLINK", "Referral code from deep link: $code")
                // The referral code field in OnboardingScreen is already bound to
                // referralCodeInput, so it will be pre-filled automatically
            }
        }
        .addOnFailureListener { e ->
            android.util.Log.e("BODA_DEEPLINK", "Deep link handling failed: ${e.message}")
        }
}
```

**Function 2: generate and share the Dynamic Link**

Add near your existing referral functions:

```kotlin
fun shareReferralLink(context: android.content.Context) {
    val myCode = userProfile.value?.referralCode ?: return
    viewModelScope.launch {
        try {
            val dynamicLink = com.google.firebase.dynamiclinks.ktx.dynamicLinks
                .shortLinkAsync {
                    link = android.net.Uri.parse(
                        "https://bodagulu.page.link/ref?code=$myCode"
                    )
                    domainUriPrefix = "https://bodagulu.page.link"
                    androidParameters("com.qaphael.bodaapp") {}
                    socialMetaTagParameters {
                        title = "Join me on Boda Gulu!"
                        description = "Use my code $myCode and we both get UGX 3,000 on your first ride."
                    }
                }
                .await()

            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    android.content.Intent.EXTRA_TEXT,
                    "Join Boda Gulu — affordable boda rides in Gulu!\n" +
                    "Use my referral code $myCode and we both get UGX 3,000 on your first ride.\n" +
                    dynamicLink.shortLink.toString()
                )
            }
            context.startActivity(
                android.content.Intent.createChooser(shareIntent, "Share via")
            )
        } catch (e: Exception) {
            errorMessage.value = "Could not generate share link: ${e.message}"
        }
    }
}
```

### 1e. `MainActivity.kt` — pass the Intent to the ViewModel

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: BodaViewModel = viewModel()

            // Pass the launch Intent so the ViewModel can read the deep link
            LaunchedEffect(Unit) {
                viewModel.handleDeepLink(intent)
            }

            BodaApp(viewModel)
        }
    }

    // Also handle the case where the app was already running
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // The ViewModel is a singleton within the process — access it via the store
        // The simplest approach is to re-run handleDeepLink from here
        // If you're using a shared ViewModel, call it directly:
        // viewModel.handleDeepLink(intent)
        // Otherwise, store the intent for the next composition cycle:
        setIntent(intent)
    }
}
```

### 1f. `BodaScreens.kt` — replace the "Share Code" button in `ReferralsScreen`

Find the share/copy button around line 6483 and replace:

```kotlin
// BEFORE — copies plain text to clipboard:
// val clipboardManager = LocalClipboardManager.current
// onClick = { clipboardManager.setText(AnnotatedString(myCode)) }

// AFTER — shares a Dynamic Link:
val context = LocalContext.current
BodaButton(
    text = "Share My Referral Link",
    onClick = { viewModel.shareReferralLink(context) },
    modifier = Modifier.fillMaxWidth()
)
```

---

## 2. Phone Number Pre-fill from SIM Card

The Android **Hint API** (`GetPhoneNumberHintIntentRequest`) asks the OS to suggest
the phone number associated with the device's SIM. The user sees a bottom sheet
with their number and taps to confirm — one tap instead of typing 9 digits.

This requires no special permission and works on Android 5.0+.

### 2a. Dependency — already present

`play-services-location` is already in your dependencies. The Hint API comes from
`play-services-auth` which is bundled with the Google Play Services base on device.
No additional dependency needed.

### 2b. `BodaViewModel.kt` — expose a trigger state

```kotlin
// Add near the OTP state block:
var triggerPhoneHint by mutableStateOf(false)
    // set to true to fire the hint launcher from the composable
```

### 2c. `BodaScreens.kt` — wire up the Hint API in the phone entry step

Add this block at the top of the `if (!viewModel.otpSent)` branch in `OnboardingScreen`:

```kotlin
if (!viewModel.otpSent) {

    // ── Phone Hint API ─────────────────────────────────────────────
    val activity = LocalContext.current as androidx.activity.ComponentActivity

    val phoneHintLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts
            .StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val phoneNumber = com.google.android.gms.auth.api.identity.Identity
                .getSignInClient(activity)
                .getPhoneNumberFromIntent(result.data)
            // Strip the +256 prefix and any spaces; keep only digits
            val digits = phoneNumber
                ?.replace(Regex("[^0-9]"), "")
                ?.removePrefix("256")
                ?.take(9)
                ?: ""
            if (digits.isNotEmpty()) {
                viewModel.phoneInput = digits
            }
        }
    }

    // Trigger the hint sheet once when this step is first shown
    LaunchedEffect(Unit) {
        try {
            val request = com.google.android.gms.auth.api.identity
                .GetPhoneNumberHintIntentRequest.builder().build()
            val result = com.google.android.gms.auth.api.identity.Identity
                .getSignInClient(activity)
                .getPhoneNumberHintIntent(request)
                .await()
            phoneHintLauncher.launch(
                androidx.activity.result.IntentSenderRequest.Builder(result).build()
            )
        } catch (e: Exception) {
            // Device has no SIM or hint unavailable — fail silently, user types manually
            android.util.Log.d("BODA_HINT", "Phone hint unavailable: ${e.message}")
        }
    }
    // ── End Phone Hint API ─────────────────────────────────────────

    // ... rest of the existing phone input UI (Column, OutlinedTextField, etc.) ...
}
```

> **Why `LaunchedEffect(Unit)` and not a button?**  
> The hint sheet is a system UI prompt, not an in-app modal. Showing it automatically
> on screen entry matches the pattern used by Uber, Bolt, and WhatsApp — the user
> gets the suggestion immediately and can dismiss it if they prefer to type.
> If they dismiss, `RESULT_CANCELED` is returned and `phoneInput` stays empty.

---

## 3. Deferred Permissions

Currently, location and notification grants block the "Register & Open App" button.
A user who denies either permission cannot complete registration — this is the
single largest source of registration abandonment on Android.

**The fix:** remove both permission requests from the profile setup step entirely.
Ask for location the first time the user tries to book a ride. Ask for notifications
once the user is on the Home screen, with a clear value proposition.

### 3a. `BodaScreens.kt` — strip permissions from `OnboardingScreen`

In the `else` branch (profile setup, Step 3), **remove** the following blocks:

```kotlin
// REMOVE THIS ENTIRE BLOCK (location permission card):
val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
BodaCard {
    Column(modifier = Modifier.padding(16.dp)) {
        // ... location icon, text, Grant button ...
    }
}

// REMOVE THIS ENTIRE BLOCK (notification permission card):
val notifPermissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
BodaCard {
    Column(modifier = Modifier.padding(16.dp)) {
        // ... notification icon, text, Enable button ...
    }
}

// REMOVE THIS LaunchedEffect:
LaunchedEffect(locationGranted, notifGranted) { ... }
```

**Replace** the `BodaButton` at the bottom of Step 3 with one that no longer
requires permissions to be granted:

```kotlin
// BEFORE:
BodaButton(
    text = "Register & Open App",
    onClick = { viewModel.completeProfileSetup() },
    enabled = viewModel.signupName.isNotEmpty() && locationGranted && notifGranted,
    modifier = Modifier.fillMaxWidth(),
    testTag = "complete_setup_btn"
)

// AFTER:
BodaButton(
    text = "Register & Open App",
    onClick = { viewModel.completeProfileSetup() },
    enabled = viewModel.signupName.isNotEmpty(),
    modifier = Modifier.fillMaxWidth(),
    testTag = "complete_setup_btn"
)
```

### 3b. `BodaScreens.kt` — ask for location at first booking attempt

In `HomeScreen` (or `RoutePreviewScreen`), wrap the "Book Ride" / "Confirm" button
with a permission check:

```kotlin
@Composable
fun BookingPermissionGate(
    onPermissionGranted: () -> Unit
) {
    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    if (locationPermissionState.status.isGranted) {
        onPermissionGranted()
    } else {
        LaunchedEffect(Unit) {
            locationPermissionState.launchPermissionRequest()
        }
    }
}
```

Then in the place where the user taps to confirm a booking (in `RoutePreviewScreen`):

```kotlin
var requestingLocation by remember { mutableStateOf(false) }
val locationState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

if (requestingLocation && !locationState.status.isGranted) {
    LaunchedEffect(Unit) { locationState.launchPermissionRequest() }
}

LaunchedEffect(locationState.status.isGranted) {
    if (locationState.status.isGranted && requestingLocation) {
        viewModel.locationPermissionGranted = true
        viewModel.startLocationTracking()
        viewModel.confirmBooking()
        requestingLocation = false
    }
}

BodaButton(
    text = "Confirm Booking",
    onClick = {
        if (locationState.status.isGranted) {
            viewModel.locationPermissionGranted = true
            viewModel.confirmBooking()
        } else {
            requestingLocation = true
        }
    },
    modifier = Modifier.fillMaxWidth()
)
```

### 3c. `BodaScreens.kt` — ask for notifications on the Home screen (first visit only)

Add this to `HomeScreen`, guarded by a one-time SharedPreferences flag so it only
shows once:

```kotlin
@Composable
fun NotificationPermissionNudge(viewModel: BodaViewModel) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("boda_prefs", android.content.Context.MODE_PRIVATE)
    }
    val alreadyAsked = remember {
        prefs.getBoolean("notif_permission_asked", false)
    }

    if (alreadyAsked) return

    val notifState = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else return  // Permission not needed below Android 13

    var showNudge by remember { mutableStateOf(!notifState.status.isGranted) }

    if (showNudge) {
        AlertDialog(
            onDismissRequest = {
                showNudge = false
                prefs.edit().putBoolean("notif_permission_asked", true).apply()
            },
            containerColor = Color(0xFF1E293B),
            title = {
                Text("Know when your driver arrives", color = Color.White,
                    fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Get notified the moment your boda is 2 minutes away — " +
                    "so you're ready at the pickup point.",
                    color = Color(0xFF94A3B8), fontSize = 14.sp
                )
            },
            confirmButton = {
                BodaButton(
                    text = "Allow Notifications",
                    onClick = {
                        notifState.launchPermissionRequest()
                        showNudge = false
                        prefs.edit().putBoolean("notif_permission_asked", true).apply()
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = {
                    showNudge = false
                    prefs.edit().putBoolean("notif_permission_asked", true).apply()
                }) {
                    Text("Maybe later", color = Color(0xFF64748B))
                }
            }
        )
    }
}
```

Call it at the top of `HomeScreen`:

```kotlin
@Composable
fun HomeScreen(viewModel: BodaViewModel, savedPlaces: List<SavedPlace>) {
    NotificationPermissionNudge(viewModel)
    // ... rest of HomeScreen ...
}
```

---

## 4. Progress Indicator ("Step 1 of 3")

The current onboarding has three steps inside one `OnboardingScreen` composable
but no visual indication of progress. Users don't know how close they are to
finishing, which increases abandonment.

### 4a. `BodaViewModel.kt` — expose a computed step index

```kotlin
// Add a computed property near the OTP state variables:
val onboardingStep: Int
    get() = when {
        !otpSent -> 1           // Phone number entry
        !isOtpVerified -> 2     // OTP verification
        else -> 3               // Profile setup
    }

val onboardingStepLabel: String
    get() = when (onboardingStep) {
        1 -> "Enter your phone number"
        2 -> "Verify your number"
        3 -> "Set up your profile"
        else -> ""
    }
```

### 4b. `BodaScreens.kt` — the progress bar composable

Add this near your other helper composables:

```kotlin
@Composable
fun OnboardingProgressBar(
    currentStep: Int,
    totalSteps: Int = 3,
    stepLabel: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Step label + fraction
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stepLabel,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Step $currentStep of $totalSteps",
                color = Color(0xFF64748B),
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Segmented bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(totalSteps) { index ->
                val isComplete = index < currentStep
                val isCurrent = index == currentStep - 1
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            when {
                                isComplete -> Color(0xFFFDB913)       // Boda yellow — done
                                isCurrent -> Color(0xFFFDB913).copy(alpha = 0.4f) // current
                                else -> Color(0xFF334155)             // upcoming
                            }
                        )
                )
            }
        }
    }
}
```

### 4c. `BodaScreens.kt` — insert the bar into `OnboardingScreen`

Find the point in `OnboardingScreen` after the image/logo header and before the
`if (!viewModel.otpSent)` branch. Add:

```kotlin
// After the app logo / header image and before the step content:
Spacer(modifier = Modifier.height(Sp.md))

OnboardingProgressBar(
    currentStep = viewModel.onboardingStep,
    stepLabel = viewModel.onboardingStepLabel
)

Spacer(modifier = Modifier.height(Sp.lg))

// ... then the existing if (!viewModel.otpSent) / else if (!isOtpVerified) / else ...
```

The `onboardingStep` property updates reactively because it reads `otpSent` and
`isOtpVerified`, both of which are `mutableStateOf` — no manual state management needed.

---

## 5. Welcome Bonus

Right now, when a new user completes profile setup and lands on `HomeScreen`,
nothing celebrates the moment. The UGX 3,000 referral reward that exists in the
system is buried inside `ReferralsScreen`. New users who didn't use a referral
code see nothing. This section adds a welcome modal that fires once on the
first Home screen visit.

### 5a. `BodaViewModel.kt` — new state

```kotlin
// Add near the top of state declarations:
var showWelcomeBonus by mutableStateOf(false)
    private set
var isNewUserSession by mutableStateOf(false)
    private set
```

### 5b. `BodaViewModel.kt` — set the flag in `completeProfileSetup()`

At the end of `completeProfileSetup()`, just before `navigateTo(Screen.Home)`:

```kotlin
fun completeProfileSetup() {
    viewModelScope.launch {
        // ... existing profile save + sync + referral logic ...

        // NEW — flag this as a fresh signup so HomeScreen shows the bonus modal
        isNewUserSession = true
        showWelcomeBonus = true

        navigateTo(Screen.Home)
    }
}
```

Also add a dismiss function:

```kotlin
fun dismissWelcomeBonus() {
    showWelcomeBonus = false
    isNewUserSession = false
    // Mark as seen in SharedPreferences so it never shows again
    prefs.edit().putBoolean("welcome_bonus_shown", true).apply()
}
```

### 5c. `BodaScreens.kt` — the welcome modal

Add this composable:

```kotlin
@Composable
fun WelcomeBonusDialog(
    userName: String,
    usedReferralCode: Boolean,
    onDismiss: () -> Unit,
    onGoToReferrals: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E293B))
                .padding(28.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // Celebration icon
                Text("🎉", fontSize = 48.sp)

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Welcome to Boda Gulu, ${userName.substringBefore(" ")}!",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (usedReferralCode) {
                    // User signed up with a referral code
                    Text(
                        "Your referral bonus of UGX 3,000 will be added to your wallet " +
                        "automatically after your first completed ride.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                } else {
                    // No referral code used — nudge them to share theirs
                    Text(
                        "Your account is ready. Share your referral code with friends " +
                        "and earn UGX 3,000 for every friend who completes their first ride.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Reward pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFDB913).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFFDB913), RoundedCornerShape(12.dp))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        if (usedReferralCode) "UGX 3,000 bonus incoming 🏍️"
                        else "Earn UGX 3,000 per referral 🏍️",
                        color = Color(0xFFFDB913),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                BodaButton(
                    text = "Book My First Ride",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (!usedReferralCode) {
                    TextButton(onClick = onGoToReferrals) {
                        Text(
                            "Share my referral code",
                            color = Color(0xFFFDB913),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
```

### 5d. `BodaScreens.kt` — show it in `HomeScreen`

At the top of `HomeScreen`, after `NotificationPermissionNudge`:

```kotlin
@Composable
fun HomeScreen(viewModel: BodaViewModel, savedPlaces: List<SavedPlace>) {
    NotificationPermissionNudge(viewModel)

    // Welcome bonus — shows once after signup, never again
    if (viewModel.showWelcomeBonus) {
        WelcomeBonusDialog(
            userName = viewModel.userProfile.collectAsState().value?.name ?: "Rider",
            usedReferralCode = viewModel.referralCodeInput.isNotEmpty(),
            onDismiss = { viewModel.dismissWelcomeBonus() },
            onGoToReferrals = {
                viewModel.dismissWelcomeBonus()
                viewModel.navigateTo(Screen.Referrals)
            }
        )
    }

    // ... rest of existing HomeScreen content ...
}
```

### 5e. `backend/server.js` — apply the referral reward after first ride

The `POST /api/referrals/:id/complete` endpoint already adds the bonus to the
referrer's wallet. Add a call to credit the **new user** as well when a referral
code was used during signup:

```javascript
// In the existing POST /api/referrals/:id/complete handler, after crediting referrer:

// Also credit the new user (the one who used the code)
const newUserResult = await db.query(
  'SELECT uid FROM users WHERE phone = $1',
  [refResult.rows[0].referred_phone]
);

if (newUserResult.rows.length > 0) {
  const newUserUid = newUserResult.rows[0].uid;
  const newUserTxRef = `REF-NEWUSER-${referralId}-${Date.now()}`;
  await db.query(
    `INSERT INTO wallet_transactions 
      (uid, tx_ref, type, amount, provider, status)
     VALUES ($1, $2, 'bonus', 3000, 'Wallet', 'completed')`,
    [newUserUid, newUserTxRef]
  );
}

res.json({ success: true });
```

---

## How everything fits together after these changes

```
New user taps referral link
        │
        ▼
App opens → handleDeepLink() reads ?code=GULU-OKENY-472
referralCodeInput = "GULU-OKENY-472"
        │
        ▼
OnboardingScreen
 ┌─ Step 1 of 3 ─────────────────────────────────────────────┐
 │  Phone Hint API fires → number pre-filled from SIM        │
 │  User taps "Get Verification Code"                        │
 └────────────────────────────────────────────────────────────┘
        │
        ▼
 ┌─ Step 2 of 3 ─────────────────────────────────────────────┐
 │  OTP entry → verify                                       │
 └────────────────────────────────────────────────────────────┘
        │
        ▼
 ┌─ Step 3 of 3 ─────────────────────────────────────────────┐
 │  Name field (only required field)                         │
 │  Referral code pre-filled from deep link                  │
 │  No permission gates — just tap "Register & Open App"     │
 └────────────────────────────────────────────────────────────┘
        │
        ▼
completeProfileSetup()
 → save to Room, sync to backend, addReferralToBackend()
 → isNewUserSession = true, showWelcomeBonus = true
        │
        ▼
HomeScreen
 → NotificationPermissionNudge (system dialog, with value prop)
 → WelcomeBonusDialog ("UGX 3,000 bonus incoming 🏍️")
        │
        ▼
User taps "Book My First Ride"
 → RoutePreviewScreen → Confirm Booking
 → location permission requested here (with context of why)
```

---

## Testing checklist

| Scenario | Expected result |
|---|---|
| User taps referral link with app installed | Opens to onboarding, referral code pre-filled |
| User taps referral link with app not installed | Play Store install → first launch reads deferred link |
| Device has SIM with phone number | Phone field pre-filled on Step 1, user can override |
| Device has no SIM / no hint available | Phone field empty, user types manually — no crash |
| User denies phone hint | Field stays empty, no error shown |
| User completes signup without referral code | Welcome modal shows "Share my referral code" variant |
| User completes signup with referral code | Welcome modal shows "UGX 3,000 bonus incoming" variant |
| User opens app second time (returning user) | Welcome modal does not appear (`welcome_bonus_shown = true` in prefs) |
| User tries to book ride without location permission | Location dialog shown at that point, not during signup |
| User denies location at booking | Booking does not proceed, no crash |
| Notification nudge | Shows once on first Home visit, never again |
