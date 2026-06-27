# Boda Gulu — Authentication & Session Audit Report

**Audit Date:** 2026-06-27  
**Auditor Role:** Senior Mobile App Security & Authentication Architect  
**Scope:** Firebase Phone Auth → Profile Setup → Session Persistence → Logout → Backend Sync  
**Files Reviewed:** BodaViewModel.kt, BodaScreens.kt, BodaRepository.kt, BodaApiService.kt, ApiClient.kt, backend/server.js, backend/middleware/auth.js, Entities.kt, Dao.kt

---

## Executive Summary

The authentication flow has **4 Critical**, **5 High**, **4 Medium**, and **2 Low** severity issues. The root cause of the reported symptom (user sees profile setup after logout instead of OTP entry) is a combination of **Issue #1** (Room not cleared on logout) and **Issue #2** (initializeDefaultData seeding a profile with `isSetupComplete = false` on every fresh install, which then persists across logout). A secondary driver is **Issue #3** (onboarding carousel/language flags never cleared), which skips the user directly to the phone input step but `isOtpVerified` happens to be `true` for a moment during the `LaunchedEffect(user)` race described in Issue #4.

---

## Issues Found

---

### ISSUE #1 — Room Database Not Cleared on Logout (The Primary Bug)

- **Severity:** Critical  
- **Location:** `BodaViewModel.kt`, `fun signOut()`, line 811  
- **Current behavior:**  
  `signOut()` calls `auth.signOut()`, sets `isOtpVerified = false`, clears `otpSent`/`otpInput`/`verificationId`, and calls `navigateTo(Screen.WelcomeOnboarding)`. It does **not** delete or reset the `UserProfile` row in Room. After logout, `repository.userProfile` (a Flow from Room) still emits the previous user's `UserProfile` with `isSetupComplete = true` and `name = "<previous name>"`.

- **Expected behavior:**  
  Logout must reset Room's `user_profile` table to a blank/uncomplete state (or delete the row entirely). Standard ride-hailing auth pattern: logout = auth session cleared + local persistent state cleared.

- **Root cause:**  
  The `LaunchedEffect(user)` in `BodaAppContent` (line 537–556) observes the Room `userProfile` Flow. Because `signOut()` leaves the Room row intact, the Flow immediately re-emits the old profile. Since `currentScreen` is now `WelcomeOnboarding` (not `Splash`), the navigation guard (`if (viewModel.currentScreen == Screen.Splash)`) does not fire — so the screen remains at `WelcomeOnboarding`. But inside `OnboardingScreen`, the display logic is:
  ```
  if (!viewModel.onboardingCarouselCompleted) → carousel
  else if (!viewModel.onboardingLanguageSelected) → language picker
  else → phone input / OTP / profile setup
  ```
  Since carousel and language flags were **not** cleared by `signOut()` either (see Issue #2), it jumps straight to the third branch. In that branch, `isOtpVerified` is now `false` (correctly reset), but `otpSent` is also `false`, so Step 1 (phone input) should show. **However**, because Room still holds a profile with `isSetupComplete = true` and `isOtpVerified` starts as `prefs.getBoolean("otp_verified", false)` which was just set to `false`, the display is correct — phone input shows.

  The actual symptom (profile setup shown) appears on a **re-login** scenario: after logout, the user enters their phone, sends OTP, and Firebase auto-verifies (instant verification path via `onVerificationCompleted`). This sets `isOtpVerified = true`. `LaunchedEffect(user)` then fires again because Room still has the old profile with `isSetupComplete = true` but now fires from `Splash` (cold start after logout), sending the user to `Home` — skipping OTP entirely for a **new phone number user**, or showing them **the old user's name** in the profile step (`signupName` is blank but `userProfile.value.name` still holds the old name).

  The scenario where the **profile setup screen** appears instead of OTP is: user logs out → `isOtpVerified` is `false` → user re-enters app → `init{}` sees `auth.currentUser != null` (Firebase session can persist even after `auth.signOut()` on certain devices briefly) → sets `isOtpVerified = true` again → Room emits old profile with `isSetupComplete = false` (if this is a second account) → user lands on profile name entry screen with an empty name field. The old profile name has been overwritten by `initializeDefaultData` (Issue #6) reinserting "Gulu Rider" on the next cold start if the row was absent.

- **Fix:**
  ```kotlin
  fun signOut() {
      stopLocationTracking()
      auth.signOut()
      
      // 1. Clear all ViewModel in-memory auth state
      isOtpVerified = false
      otpSent = false
      otpInput = ""
      phoneInput = ""
      signupName = ""
      verificationId = null
      resendToken = null
      backendBalance = null
      
      // 2. Clear all SharedPreferences auth flags
      prefs.edit()
          .putBoolean("otp_verified", false)
          .putBoolean("onboarding_carousel_completed", false)
          .putBoolean("onboarding_language_selected", false)
          .apply()
      
      // 3. Reset in-memory onboarding flags
      onboardingCarouselCompleted = false
      onboardingLanguageSelected = false
      onboardingSlideIndex = 0
      
      // 4. Clear Room (wipe all user-specific data)
      viewModelScope.launch {
          repository.clearAllUserData()  // new method — see below
      }
      
      navigateTo(Screen.WelcomeOnboarding)
  }
  ```
  Add to `BodaDao.kt`:
  ```kotlin
  @Query("DELETE FROM user_profile")
  suspend fun deleteAllUserProfiles()
  
  @Query("DELETE FROM trips")
  suspend fun deleteAllTrips()
  
  @Query("DELETE FROM wallet_transactions")
  suspend fun deleteAllTransactions()
  
  @Query("DELETE FROM saved_places")
  suspend fun deleteAllSavedPlaces()
  
  @Query("DELETE FROM emergency_contacts")
  suspend fun deleteAllEmergencyContacts()
  
  @Query("DELETE FROM referrals")
  suspend fun deleteAllReferrals()
  ```
  Add to `BodaRepository.kt`:
  ```kotlin
  suspend fun clearAllUserData() {
      dao?.deleteAllUserProfiles()
      dao?.deleteAllTrips()
      dao?.deleteAllTransactions()
      dao?.deleteAllSavedPlaces()
      dao?.deleteAllEmergencyContacts()
      dao?.deleteAllReferrals()
  }
  ```

---

### ISSUE #2 — `initializeDefaultData` Seeds a Phantom Profile on Every Cold Start

- **Severity:** Critical  
- **Location:** `BodaRepository.kt`, `fun initializeDefaultData()`, lines 20–24; called from `BodaViewModel.init{}`, line 121  
- **Current behavior:**  
  Every time the app starts (even for a logged-out user), `initializeDefaultData()` inserts a `UserProfile(name = "Gulu Rider", isSetupComplete = false)` into Room **if no row exists**. After a proper logout + Room clear, the next cold start immediately creates this phantom profile. The Room Flow emits it, triggering `LaunchedEffect(user)`. Since `user != null` (the phantom), and `currentScreen == Splash`, the code checks `it.isSetupComplete` → `false` → navigates to `WelcomeOnboarding`. So far correct. But the phantom profile also means `userProfile.value` is non-null throughout the onboarding flow, and `phoneNumber` is empty string. If the `restoreSessionFromBackend` failure path runs (Issue #5), it calls `syncUserToBackend(localProfile)` with this phantom profile — syncing "Gulu Rider" to the backend under the authenticated user's UID.

- **Expected behavior:**  
  `initializeDefaultData()` should not create a profile for an unauthenticated user. The profile row should only be created after successful OTP verification and profile setup. On cold start with no Firebase session, `userProfile` should emit `null` and navigation should go to `WelcomeOnboarding` via the `user == null` branch.

- **Root cause:**  
  The seeding function was written defensively to avoid null crashes throughout the app, but it conflates "no profile yet" with "needs a placeholder."

- **Fix:**  
  Remove `initializeDefaultData()` entirely. Guard all `userProfile.value?.` accesses with null checks (they already use `?.` in most places). In the `LaunchedEffect(user)` null branch, simply navigate to `WelcomeOnboarding` when Firebase session is also absent:
  ```kotlin
  // In BodaViewModel.init{}
  // Remove: repository.initializeDefaultData()
  
  val currentUser = auth.currentUser
  if (currentUser != null) {
      isOtpVerified = true
      prefs.edit().putBoolean("otp_verified", true).apply()
      phoneInput = currentUser.phoneNumber?.removePrefix("+256") ?: ""
      restoreSessionFromBackend()
  } else {
      isOtpVerified = false
      isLoadingData = false
  }
  ```
  And in `BodaAppContent`:
  ```kotlin
  LaunchedEffect(user) {
      if (viewModel.currentScreen != Screen.Splash) return@LaunchedEffect
      val firebaseUser = FirebaseAuth.getInstance().currentUser
      if (user != null) {
          viewModel.updateLanguage(user.language)
          viewModel.navigateTo(
              if (user.isSetupComplete) Screen.Home else Screen.WelcomeOnboarding
          )
      } else if (firebaseUser == null) {
          viewModel.navigateTo(Screen.WelcomeOnboarding)
      }
      // else: Firebase session exists, wait for restoreSessionFromBackend to populate Room
  }
  ```

---

### ISSUE #3 — Onboarding Carousel & Language Flags Not Reset on Logout

- **Severity:** Critical  
- **Location:** `BodaViewModel.kt`, `fun signOut()`, lines 811–820  
- **Current behavior:**  
  `signOut()` resets `isOtpVerified`, `otpSent`, and the `otp_verified` SharedPreference, but does **not** reset `onboardingCarouselCompleted`, `onboardingLanguageSelected`, or their SharedPreferences keys (`onboarding_carousel_completed`, `onboarding_language_selected`). After logout, these remain `true`.

  When the user is sent to `Screen.WelcomeOnboarding`, `OnboardingScreen` checks:
  ```
  if (!viewModel.onboardingCarouselCompleted) → carousel
  else if (!viewModel.onboardingLanguageSelected) → language picker  
  else → phone / OTP / profile setup branch
  ```
  Both flags are `true`, so the user skips the carousel and language picker and lands directly at the phone input. This is actually **correct UX** for a returning user — but it means signOut does not reset the onboarding to its true initial state. If a **different user** logs in on the same device, they never see the language selector. The language from the previous session persists.

  More critically: `completeOnboardingLanguage()` calls `updateLanguage()` which calls `syncUserToBackend()`. If the carousel/language flags are not reset, a new user on the same device silently inherits the previous user's language without an opportunity to change it in the onboarding flow.

- **Expected behavior:**  
  Logout should fully reset all onboarding flags so the next user on the device sees the full fresh-start flow. This is the standard behavior on Uber, Bolt, and inDrive.

- **Fix:**  
  Add to `fun signOut()` (part of the comprehensive fix in Issue #1):
  ```kotlin
  onboardingCarouselCompleted = false
  onboardingLanguageSelected = false
  onboardingSlideIndex = 0
  prefs.edit()
      .putBoolean("onboarding_carousel_completed", false)
      .putBoolean("onboarding_language_selected", false)
      .apply()
  ```

---

### ISSUE #4 — `isOtpVerified` Initialized from SharedPreferences, Not Firebase Auth State

- **Severity:** Critical  
- **Location:** `BodaViewModel.kt`, line 1238 (property declaration)  
- **Current behavior:**  
  ```kotlin
  var isOtpVerified by mutableStateOf(prefs.getBoolean("otp_verified", false))
  ```
  `isOtpVerified` is initialized from SharedPreferences at ViewModel creation time. In `init{}`, if `auth.currentUser != null`, it is then overwritten to `true`. But the initial SharedPreferences read happens **before** `init{}` runs. If `prefs.getBoolean("otp_verified", false)` returns `true` (because it was set during the previous session and `signOut()` correctly cleared it to `false`), the initial value is `false` — this is correct after logout. However: if the app process is killed and restarted while logged in (app cleared from recents), SharedPreferences persists the `true` value but `auth.currentUser` may momentarily appear null before Firebase re-hydrates its session from disk. During this window, `isOtpVerified = true` from prefs but `auth.currentUser == null`, leading to a state where the app shows the phone input screen (because `isOtpVerified` correctly reads `true` from prefs) but then Firebase restores the session and `init{}` sets it to `true` again — so this race can cause a brief flash of the phone input screen before correctly navigating to Home.

  More seriously: SharedPreferences is **not a cryptographically secure store**. A rooted device can set `otp_verified = true` in SharedPreferences and bypass OTP verification entirely — the app would show the profile setup step (Step 3) without any Firebase credential, and a user could create an account without a valid phone OTP.

- **Expected behavior:**  
  `isOtpVerified` should be derived solely from `auth.currentUser != null`. SharedPreferences should not be the source of truth for authentication state. Firebase's own persistence is the correct single source of truth.

- **Root cause:**  
  The developer added SharedPreferences as a "cache" for OTP state to survive process death, without realizing Firebase Auth already handles session persistence across process death natively.

- **Fix:**  
  Remove the SharedPreferences-backed initial value:
  ```kotlin
  // BEFORE (line 1238):
  var isOtpVerified by mutableStateOf(prefs.getBoolean("otp_verified", false))
  
  // AFTER:
  var isOtpVerified by mutableStateOf(false)  // derived only from auth.currentUser in init{}
  ```
  Remove all `prefs.edit().putBoolean("otp_verified", ...)` writes. Remove the `otp_verified` SharedPreference entirely. In `init{}`, keep:
  ```kotlin
  isOtpVerified = auth.currentUser != null
  ```

---

### ISSUE #5 — `restoreSessionFromBackend()` Failure Path Syncs Phantom "Gulu Rider" Profile to Backend

- **Severity:** High  
- **Location:** `BodaViewModel.kt`, `fun restoreSessionFromBackend()`, lines 137–153  
- **Current behavior:**  
  When `fetchUserProfile()` fails (backend down, 404, or network error), the failure handler does:
  ```kotlin
  onFailure = { e ->
      val localProfile = repository.userProfile.firstOrNull()
      if (localProfile != null && localProfile.phoneNumber.isNotEmpty()) {
          syncUserToBackend(localProfile)
      }
      fetchBackendData()
  }
  ```
  The problem is that `initializeDefaultData()` (Issue #2) has already populated Room with `UserProfile(name = "Gulu Rider", phoneNumber = "")`. The guard checks `localProfile.phoneNumber.isNotEmpty()`, which is `false` for the phantom profile — so `syncUserToBackend` is NOT called in this specific case. However, if the user has completed profile setup (has a real name and phone) and the backend is temporarily down, this path correctly falls back to local data. The issue is: on a **404 specifically** (user not found on backend — e.g., database was wiped, or user is on a new backend environment), the backend returns 404, which is caught as a failure. The local profile is real (name + phone filled in), so `syncUserToBackend(localProfile)` is called — this re-creates the user on the backend, which is correct behavior. But the log message says "User not found on backend. Syncing from local profile..." which is logged for ALL failures including transient network errors, misleadingly suggesting a re-sync when the backend is just briefly unreachable.

  The real bug here: **`fetchUserProfile()` fails with a non-404 error (e.g., 500, timeout)** → the code treats it the same as a 404 and attempts re-sync. This can cause repeated unnecessary sync calls during backend downtime.

- **Expected behavior:**  
  Distinguish between 404 (user genuinely not found → re-sync) and other errors (backend unavailable → use local data, do not re-sync). Uber/Bolt pattern: graceful degradation with cached data; re-sync only on first-use signals.

- **Fix:**
  ```kotlin
  // In BodaRepository.kt, fetchUserProfile():
  suspend fun fetchUserProfile(): Result<UserMeResponse> {
      return withContext(Dispatchers.IO) {
          try {
              val response = api.getMe()
              when {
                  response.isSuccessful -> {
                      val user = response.body()
                      if (user != null) Result.success(user)
                      else Result.failure(Exception("User not found"))
                  }
                  response.code() == 404 -> Result.failure(UserNotFoundException())
                  else -> Result.failure(Exception("Backend error: ${response.code()}"))
              }
          } catch (e: Exception) { Result.failure(e) }
      }
  }
  
  class UserNotFoundException : Exception("User not found on backend")
  
  // In BodaViewModel.kt restoreSessionFromBackend():
  onFailure = { e ->
      if (e is UserNotFoundException) {
          addPostgresLog("User not found on backend. Syncing from local profile...")
          val localProfile = repository.userProfile.firstOrNull()
          if (localProfile != null && localProfile.phoneNumber.isNotEmpty()) {
              syncUserToBackend(localProfile)
          }
      } else {
          addPostgresLog("Session restore failed (offline?): ${e.message}")
      }
      fetchBackendData()
  }
  ```

---

### ISSUE #6 — `ON CONFLICT` in `/api/users/sync` Overwrites `wallet_balance` to 0 on First Sync

- **Severity:** High  
- **Location:** `backend/server.js`, lines 89–93  
- **Current behavior:**  
  ```sql
  INSERT INTO users (uid, phone, full_name, email, wallet_balance, language, referral_code)
  VALUES ($1, $2, $3, $4, 0.00, $5, $6)
  ON CONFLICT (uid) DO UPDATE
  SET full_name = $3, phone = $2, language = $5, referral_code = $6, updated_at = NOW()
  ```
  The `ON CONFLICT DO UPDATE` clause correctly does **not** update `wallet_balance` — it only updates name, phone, language, referral_code. So on re-sync, wallet balance is preserved. **However**, if the same user is synced on a **fresh backend database** (new deployment, DB wipe, or first install on new device), the `INSERT` branch fires with `wallet_balance = 0.00`, wiping any balance that should have carried over from a previous deployment. There is no mechanism to restore wallet balance from a backup or any source of truth other than the database.

  Additionally, the sync endpoint updates `full_name` unconditionally on conflict. If a user changes their name in-app on one device, syncs, then opens the app on a second device where the old name is cached, the second device's sync will overwrite the new name with the old one. This is a **last-write-wins race condition** with no conflict resolution.

- **Expected behavior:**  
  Wallet balance should never be set to 0 on conflict (it's already handled). Name changes should be timestamped and last-write-wins should use `updated_at` comparison. Industry standard: wallet balance is the canonical backend value; sync never decreases it.

- **Fix (backend):**
  ```javascript
  // server.js - /api/users/sync
  const query = `
    INSERT INTO users (uid, phone, full_name, email, wallet_balance, language, referral_code, updated_at)
    VALUES ($1, $2, $3, $4, 0.00, $5, $6, NOW())
    ON CONFLICT (uid) DO UPDATE
    SET 
      full_name = CASE 
        WHEN users.updated_at < NOW() THEN EXCLUDED.full_name 
        ELSE users.full_name 
      END,
      phone = EXCLUDED.phone,
      language = EXCLUDED.language,
      referral_code = COALESCE(EXCLUDED.referral_code, users.referral_code),
      updated_at = NOW()
    -- wallet_balance intentionally NOT updated here; use /api/wallet/topup and /api/wallet/pay
    RETURNING *;
  `;
  ```

---

### ISSUE #7 — `syncUser` Response Cannot Be Deserialized: `ApiResponse<UserProfile>` Field Name Mismatch

- **Severity:** High  
- **Location:** `BodaApiService.kt`, lines 8–9 and 104–113; `BodaRepository.kt`, lines 76–84  
- **Current behavior:**  
  The sync endpoint is declared as:
  ```kotlin
  @POST("api/users/sync")
  suspend fun syncUser(@Body user: UserSyncRequest): Response<ApiResponse<UserProfile>>
  ```
  The backend returns JSON like:
  ```json
  { "success": true, "user": { "uid": "...", "full_name": "Okeny", "phone": "+256...", "wallet_balance": 100.0, ... } }
  ```
  `ApiResponse<T>` has a `user: T?` field — so Moshi will try to deserialize the nested `user` object as `UserProfile`. But `UserProfile` is a Room entity with Kotlin camelCase fields (`name`, `phoneNumber`, `isSetupComplete`) and **no** `@Json` annotations. Moshi with `KotlinJsonAdapterFactory` expects exact name matches. The JSON has `full_name`, `phone`, `wallet_balance` — none of which match `name`, `phoneNumber`, `isSetupComplete`. The deserialized `UserProfile` will have all defaults: `name = ""`, `phoneNumber = ""`, `isSetupComplete = false`.

  This means `syncUserToBackend()` effectively succeeds silently but the returned `UserProfile` from the repository is useless. The code in `BodaRepository.syncUser()` returns `Result.success(user)` with a blank profile. The caller in `completeProfileSetup()` ignores the return value anyway, so this silent failure goes unnoticed — but it means the app cannot use the synced data for any post-sync operations.

- **Expected behavior:**  
  Sync response should be deserialized into a dedicated DTO that matches the snake_case JSON, or the `UserProfile` entity should have `@Json` annotations.

- **Root cause:**  
  `UserProfile` was designed as a Room entity and inadvertently also used as a REST response DTO without proper field mappings.

- **Fix (Option A — recommended):**  
  Create a separate `UserSyncResponse` DTO in `BodaApiService.kt`:
  ```kotlin
  data class UserSyncResponse(
      val uid: String = "",
      val phone: String = "",
      val full_name: String = "",
      val wallet_balance: Double = 0.0,
      val language: String = "en",
      val referral_code: String = ""
  )
  
  data class SyncApiResponse(
      val success: Boolean,
      val user: UserSyncResponse? = null,
      val error: String? = null
  )
  
  @POST("api/users/sync")
  suspend fun syncUser(@Body user: UserSyncRequest): Response<SyncApiResponse>
  ```

---

### ISSUE #8 — `ApiClient.authInterceptor` Calls `Tasks.await()` on the Network (IO) Thread — Deadlock Risk

- **Severity:** High  
- **Location:** `ApiClient.kt`, lines 28–36  
- **Current behavior:**  
  ```kotlin
  private val authInterceptor = Interceptor { chain ->
      val token = try {
          Tasks.await(
              FirebaseAuth.getInstance().currentUser
                  ?.getIdToken(false)
                  ?: Tasks.forResult(null)
          )?.token
      } catch (e: Exception) { null }
      ...
  }
  ```
  `Tasks.await()` blocks the current thread until the Firebase Task completes. OkHttp interceptors run on OkHttp's internal thread pool. If `getIdToken()` triggers a token refresh (when the cached token is expired), Firebase internally dispatches work and may try to use the main thread's Looper or a specific thread. `Tasks.await()` with no executor can deadlock if called on the main thread (though OkHttp threads are not the main thread). However, the real risk is: if `getIdToken(false)` returns a cached token, the Task completes inline and `Tasks.await()` returns immediately — this is the happy path. But `getIdToken(true)` (force-refresh, used in `syncUserToBackend`) can block for a network round-trip, holding the OkHttp thread hostage. This does not deadlock per se, but it can starve OkHttp's connection pool and cause timeout cascades.

  More critically, `getIdToken(false)` will return an **expired token** without refreshing it. The backend will then reject requests with 403 until the next forced refresh. There is no automatic retry on 401/403.

- **Expected behavior:**  
  Firebase token acquisition should be done asynchronously before making the request, with a proper retry mechanism for 401/403 responses. Industry standard: OkHttp `Authenticator` for token refresh, not an `Interceptor`.

- **Fix:**  
  Replace the auth interceptor with an `Authenticator` for refresh, and use a cached-token approach in the interceptor:
  ```kotlin
  // In ApiClient.kt:
  private val authInterceptor = Interceptor { chain ->
      val token = cachedToken  // see below
      val request = if (token != null) {
          chain.request().newBuilder()
              .addHeader("Authorization", "Bearer $token")
              .build()
      } else chain.request()
      chain.proceed(request)
  }
  
  private val authenticator = okhttp3.Authenticator { _, response ->
      // Called on 401/403 — refresh token
      val freshToken = try {
          Tasks.await(
              FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                  ?: Tasks.forResult(null)
          )?.token
      } catch (e: Exception) { return@Authenticator null }
      
      freshToken?.let { token ->
          cachedToken = token
          response.request.newBuilder()
              .header("Authorization", "Bearer $token")
              .build()
      }
  }
  
  @Volatile private var cachedToken: String? = null
  
  // Expose a suspend function to pre-warm the token:
  suspend fun refreshToken() {
      cachedToken = withContext(Dispatchers.IO) {
          Tasks.await(
              FirebaseAuth.getInstance().currentUser?.getIdToken(false)
                  ?: Tasks.forResult(null)
          )?.token
      }
  }
  ```

---

### ISSUE #9 — `PhoneAuthOptions` Missing `.setActivity()` — Auto-Verification and reCAPTCHA Will Fail

- **Severity:** High  
- **Location:** `BodaViewModel.kt`, `fun startOtpFlow()`, lines 1642–1650  
- **Current behavior:**  
  ```kotlin
  val options = PhoneAuthOptions.newBuilder(auth)
      .setPhoneNumber(phoneNumber)
      .setTimeout(60L, TimeUnit.SECONDS)
      .setCallbacks(callbacks)
      .build()
  ```
  The `PhoneAuthOptions` does not include `.setActivity(activity)`. Without an Activity reference:
  - Firebase Phone Auth cannot display the reCAPTCHA verification fallback (required when silent push notifications fail on emulators or restrictive network environments).
  - Instant (automatic) verification via `onVerificationCompleted` may fail silently on some devices.
  - On Android 9+ with Play Protect enabled, SafetyNet/Play Integrity attestation requires the Activity context.
  
  Firebase's own documentation states `.setActivity()` is required for the SafetyNet/Play Integrity path.

- **Expected behavior:**  
  Pass the Activity reference. Since this is a ViewModel (no direct Activity reference), the standard approach is to pass it as a parameter from the composable.

- **Fix:**  
  In `BodaViewModel.kt`, add an Activity parameter to `startOtpFlow`:
  ```kotlin
  fun startOtpFlow(activity: android.app.Activity) {
      ...
      val options = PhoneAuthOptions.newBuilder(auth)
          .setPhoneNumber(phoneNumber)
          .setTimeout(60L, TimeUnit.SECONDS)
          .setActivity(activity)   // ADD THIS
          .setCallbacks(callbacks)
          .build()
      ...
  }
  ```
  In `BodaScreens.kt` (OnboardingScreen), pass the Activity:
  ```kotlin
  val activity = LocalContext.current as android.app.Activity
  // In onClick handler:
  viewModel.startOtpFlow(activity)
  ```

---

### ISSUE #10 — `navigateTo(Screen.WelcomeOnboarding)` in `signOut()` Adds to BackStack

- **Severity:** Medium  
- **Location:** `BodaViewModel.kt`, `fun signOut()` line 820 and `fun navigateTo()` line 793  
- **Current behavior:**  
  ```kotlin
  fun navigateTo(screen: Screen) {
      if (currentScreen != screen) {
          backStack.add(currentScreen)  // always adds to backStack
          currentScreen = screen
      }
  }
  ```
  When `signOut()` calls `navigateTo(Screen.WelcomeOnboarding)`, the current screen (e.g., `Screen.ProfileSettings`) is pushed onto the backStack. After logout, the user is on `WelcomeOnboarding`. If they press the Android back button, `navigateBack()` pops `ProfileSettings` from the stack — taking them back to a screen that requires authentication. If they press back again from `ProfileSettings`, they get the `Home` screen (or wherever they were), also requiring auth. This creates a post-logout screen leakage.

- **Expected behavior:**  
  Logout should clear the entire backStack and set the current screen to `WelcomeOnboarding` with no history.

- **Fix:**
  ```kotlin
  fun signOut() {
      // ... (auth clearing) ...
      backStack.clear()           // Clear entire navigation history
      currentScreen = Screen.WelcomeOnboarding  // Set directly, not via navigateTo
  }
  ```

---

### ISSUE #11 — `fetchBackendData()` Writes Trips/Transactions Into Room Without Checking for Duplicate User Data

- **Severity:** Medium  
- **Location:** `BodaViewModel.kt`, `fun fetchBackendData()`, lines 167–218  
- **Current behavior:**  
  `fetchBackendData()` fetches trips, transactions, emergency contacts, and referrals from the backend and inserts them into Room with `repository.addTrip(...)`, `repository.addTransaction(...)`, etc. Each uses `@Insert(onConflict = OnConflictStrategy.REPLACE)` in the DAO. After logout (without Room clear), these records from User A persist. When User B logs in, `fetchBackendData()` runs and inserts User B's records — but **User A's records are still in Room** (they were not deleted). User B now sees User A's trips mixed with their own in the `TripsHistoryScreen`.

  Furthermore, `fetchBackendData()` has a 5-minute cache guard:
  ```kotlin
  if (!force && now - lastBackendFetchMs < 5 * 60 * 1000L) { return }
  ```
  If the user logs out and back in within 5 minutes (same ViewModel instance is reused within the process lifetime), backend data is not refreshed.

- **Expected behavior:**  
  Room should be cleared on logout (covered by Issue #1 fix). Additionally, `fetchBackendData()` after login should clear stale local records before inserting fresh backend data.

- **Fix:**  
  After clearing Room on logout (Issue #1 fix), also reset `lastBackendFetchMs = 0L` in `signOut()` to force a fresh backend fetch on next login. The Room clear in Issue #1 handles the cross-user contamination.

---

### ISSUE #12 — Firebase Admin SDK Falls Back to Accepting Any Token in Development Mode — Production Risk

- **Severity:** Medium  
- **Location:** `backend/middleware/auth.js`, lines 34–40  
- **Current behavior:**  
  ```javascript
  if (!firebaseInitialized) {
      console.log(`[DEV MODE] Mock-verifying token: ...`);
      req.user = {
          uid: token === 'test_rider_uid' ? 'test_rider_uid' : 
               (token === 'test_driver_uid' ? 'test_driver_uid' : token),
          ...
      };
      return next();
  }
  ```
  If `FIREBASE_SERVICE_ACCOUNT_PATH` is not set, the middleware accepts **any Bearer token** as valid and uses it as the `uid`. An attacker who knows the backend URL can forge any `uid` by sending any string as the Bearer token, bypassing all authentication. The only protection is that the production environment supposedly has the variable set — but if it is accidentally unset in a deployment (env var misconfiguration, container restart, etc.), the entire auth layer collapses silently. The `console.log` warning is the only signal.

- **Expected behavior:**  
  If Firebase is not initialized, the middleware should return `503 Service Unavailable`, not bypass auth. Dev mode should be gated behind an explicit `NODE_ENV=development` check, and even then should only allow pre-approved test UIDs.

- **Fix:**
  ```javascript
  if (!firebaseInitialized) {
    if (process.env.NODE_ENV !== 'development') {
      return res.status(503).json({ error: 'Authentication service unavailable' });
    }
    // Strict allow-list for development only
    const allowedTestUids = ['test_rider_uid', 'test_driver_uid'];
    if (!allowedTestUids.includes(token)) {
      return res.status(401).json({ error: 'Invalid test token' });
    }
    req.user = { uid: token, email: 'dev@test.com', phone_number: '+256770000000', name: 'Dev User' };
    return next();
  }
  ```

---

### ISSUE #13 — `deleteAccount` Flow Does Not Call `auth.currentUser.delete()` — Firebase Session Survives Account Deletion

- **Severity:** Medium  
- **Location:** `BodaScreens.kt`, lines 4847–4856 (Delete Account dialog `confirmButton`)  
- **Current behavior:**  
  The "Delete Account" confirm button:
  ```kotlin
  val emptyProfile = UserProfile(id = 1, isSetupComplete = false)
  viewModel.saveUserProfile(emptyProfile)
  viewModel.resetOnboarding()
  viewModel.navigateTo(Screen.WelcomeOnboarding)
  ```
  It writes a blank profile to Room and resets onboarding flags, but:
  1. Does **not** call `auth.signOut()` or `auth.currentUser?.delete()`
  2. Does **not** call any backend API to delete the PostgreSQL user row
  3. Does **not** clear Room data beyond the `UserProfile` row (trips, transactions, etc. persist)
  
  The Firebase session remains live. On next app launch, `init{}` sees `auth.currentUser != null`, sets `isOtpVerified = true`, and calls `restoreSessionFromBackend()` — which fetches the (still-existing) backend profile and repopulates Room. The "deleted" account is fully restored.

- **Expected behavior:**  
  Account deletion must: (1) delete the backend PostgreSQL row via an API call, (2) delete the Firebase Auth user via `auth.currentUser.delete()`, (3) clear all Room data, (4) clear all SharedPreferences.

- **Fix:**  
  Add a `deleteAccount()` function to `BodaViewModel`:
  ```kotlin
  fun deleteAccount() {
      viewModelScope.launch {
          // 1. Delete from backend
          apiRepository.deleteAccount()  // add DELETE /api/users/me endpoint
          
          // 2. Delete Firebase Auth user
          withContext(Dispatchers.IO) {
              try { Tasks.await(auth.currentUser?.delete() ?: Tasks.forResult(null)) }
              catch (e: Exception) { /* log */ }
          }
          
          // 3. Clear Room and SharedPreferences (same as signOut())
          repository.clearAllUserData()
          prefs.edit().clear().apply()
          
          // 4. Reset all state
          isOtpVerified = false
          onboardingCarouselCompleted = false
          onboardingLanguageSelected = false
          backStack.clear()
          currentScreen = Screen.WelcomeOnboarding
      }
  }
  ```

---

### ISSUE #14 — `UserProfile.id` Is Always 1 — Multi-Account Not Possible, Wrong Profile Could Be Shown

- **Severity:** Low  
- **Location:** `Entities.kt`, line 8; `Dao.kt`, line 7  
- **Current behavior:**  
  ```kotlin
  @Entity(tableName = "user_profile")
  data class UserProfile(
      @PrimaryKey val id: Int = 1,
      ...
  )
  ```
  Room's `user_profile` table can only hold a single row (PK is always 1). The DAO queries `WHERE id = 1`. This is intentional for a single-account app, but it means:
  - After logout (without Room clear), User B's `saveUserProfile()` call overwrites User A's record in Room.
  - This is safe if Issue #1 (Room clear on logout) is fixed, but fragile by design.
  - If Room clear is ever skipped, User B silently inherits User A's `referralCode`, `language`, and `isSetupComplete` flag.

- **Expected behavior:**  
  Profile should be keyed by Firebase UID, not hardcoded to `id = 1`. This provides natural isolation and supports future multi-profile features.

- **Fix (low priority — safe after Issue #1 is fixed):**  
  Change PK to `uid: String` and update the DAO query. This is a Room migration — increment the database version and provide a migration:
  ```kotlin
  @Entity(tableName = "user_profile")
  data class UserProfile(
      @PrimaryKey val uid: String = "",
      val name: String = "",
      ...
  )
  
  // In Dao.kt:
  @Query("SELECT * FROM user_profile WHERE uid = :uid LIMIT 1")
  fun getUserProfile(uid: String): Flow<UserProfile?>
  ```

---

### ISSUE #15 — `FCM Token Endpoint` Has No Firebase Auth Verification

- **Severity:** Low  
- **Location:** `backend/server.js`, lines 113–125  
- **Current behavior:**  
  ```javascript
  app.post('/api/users/fcm-token', async (req, res) => {
    const { fcm_token } = req.body;
    const uid = req.user?.uid || req.body.uid;  // req.user is undefined — no middleware!
    ...
    await db.query('UPDATE users SET fcm_token = $1 WHERE uid = $2', [fcm_token, uid]);
  ```
  The route does **not** use `verifyFirebaseToken` middleware. `req.user` is `undefined`. The fallback `req.body.uid` means any caller can update any user's FCM token by just providing a `uid` in the body. An attacker can hijack push notifications for any user by pointing their FCM token at a victim's UID.

- **Expected behavior:**  
  All user-data-mutating endpoints must require Firebase auth. FCM token registration should update only the authenticated user's record.

- **Fix (backend):**
  ```javascript
  // Add verifyFirebaseToken middleware:
  app.post('/api/users/fcm-token', verifyFirebaseToken, async (req, res) => {
    const { fcm_token } = req.body;
    const uid = req.user.uid;  // from verified token only
    if (!fcm_token) {
      return res.status(400).json({ error: 'Missing fcm_token' });
    }
    try {
      await db.query('UPDATE users SET fcm_token = $1, updated_at = NOW() WHERE uid = $2', [fcm_token, uid]);
      res.json({ success: true });
    } catch (error) {
      res.status(500).json({ error: error.message });
    }
  });
  ```

---

## Edge Case Analysis

### A. User logs out, logs back in with same phone number
**Current:** Room still has old profile (`isSetupComplete = true`). After logout, `isOtpVerified = false`. On re-login, OTP is verified → `isOtpVerified = true` → `LaunchedEffect(user)` fires → Room emits old profile with `isSetupComplete = true` → user goes to `Home`. **Profile name is the old name** — correct for same user. However, `restoreSessionFromBackend()` runs again and re-fetches from backend, overwriting Room with the authoritative copy. This is the best-case scenario and works, but only because the user is the same person.

**After fix (Issue #1):** Room is cleared on logout. On re-login, OTP is verified → Room is empty → `LaunchedEffect(user)` emits `null` → waits for `restoreSessionFromBackend()` → backend returns profile → Room is populated → user goes to Home. ✅

### B. User clears app from recents (process death)
**Current:** Firebase Auth persists session to disk. On cold start, `auth.currentUser != null` → `isOtpVerified = true` → `restoreSessionFromBackend()` → profile restored from backend. **Works correctly.** The `isOtpVerified` SharedPreference was `true`, so even if Firebase hadn't re-hydrated yet (early in the process), the init path via `auth.currentUser != null` takes precedence.

**After fix (Issue #4):** SharedPreferences OTP flag is removed. `isOtpVerified = auth.currentUser != null` in `init{}`. Firebase's own disk-persisted session handles cold starts. ✅

### C. User installs on new device
**Current:** No Room data, no SharedPreferences. `auth.currentUser` is `null` (Firebase session is device-local and not transferable). `isOtpVerified = false`. User correctly goes through full onboarding. After OTP, `restoreSessionFromBackend()` will return 404 (user not found on backend yet — this is their first sync from this device). The failure path calls `syncUserToBackend(localProfile)` where `localProfile` is the phantom "Gulu Rider" profile (Issue #2), syncing garbage data. Then `fetchBackendData()` will fail because the user is not yet on the backend, and the profile setup screen shows. User sets up name → `completeProfileSetup()` syncs correctly. Mostly works, but the phantom sync (Issue #2) creates an incomplete record on the backend before the real sync.

**After fix (Issues #1, #2, #5):** No phantom profile created. New device goes through clean OTP + profile setup → first sync. ✅

### D. Firebase token expires mid-session
**Current:** `ApiClient.authInterceptor` calls `getIdToken(false)` (no force-refresh). An expired token will be sent. The backend's `verifyFirebaseToken` middleware calls `admin.auth().verifyIdToken(token)` which **does** check expiry — it will return 403. The app receives a 403 and the `BodaRepository` returns `Result.failure(Exception("Backend error"))`. There is no automatic retry or token refresh. The user sees an error message and the action fails silently (except for the Snackbar). Subsequent requests also fail until the app is restarted.

**After fix (Issue #8):** OkHttp `Authenticator` detects 401/403 and re-fetches a fresh token automatically. ✅

### E. Backend is down
**Current:** `restoreSessionFromBackend()` fails → falls back to local Room data → `fetchBackendData()` also fails silently. If Room has data (returning user), the app works fully offline with cached data. The 5-minute cache guard in `fetchBackendData()` prevents hammering a down backend. **Graceful degradation mostly works**, except the failure logging conflates 404 with network errors (Issue #5).

If backend is down during first login (no Room data), the user can still complete OTP → profile setup → `completeProfileSetup()` saves to Room locally → app navigates to Home. `syncUserToBackend()` fails but logs it. On next app start, if backend is up, `restoreSessionFromBackend()` will call the failure path's `syncUserToBackend()` to create the record. **Mostly works, with the phantom sync caveat (Issue #2).**

---

## Priority Remediation Order

| Priority | Issue | Impact |
|---|---|---|
| 1 | Issue #4 — Remove SharedPreferences OTP gate | Eliminates auth bypass on rooted devices |
| 2 | Issue #1 — Clear Room + all state on signOut | Fixes the reported symptom (profile screen shown after logout) |
| 3 | Issue #2 — Remove `initializeDefaultData()` | Eliminates phantom profile seeding |
| 4 | Issue #3 — Reset onboarding flags on signOut | Correct per-user onboarding flow |
| 5 | Issue #15 — Add auth middleware to FCM endpoint | Prevents push notification hijacking |
| 6 | Issue #9 — Add `.setActivity()` to PhoneAuthOptions | Fixes silent OTP failure on some devices |
| 7 | Issue #10 — Clear backStack on signOut | Prevents post-logout screen leakage |
| 8 | Issue #12 — Harden dev-mode auth bypass | Prevents accidental production bypass |
| 9 | Issue #8 — Fix auth interceptor threading / add Authenticator | Fixes expired token mid-session |
| 10 | Issue #7 — Fix `syncUser` response DTO mismatch | Fixes silent Moshi deserialization failure |
| 11 | Issue #5 — Distinguish 404 from other failures in restoreSession | Prevents phantom sync on backend downtime |
| 12 | Issue #6 — Protect wallet_balance in ON CONFLICT | Prevents balance wipe on re-deploy |
| 13 | Issue #13 — Fix deleteAccount to use Firebase delete | Ensures account deletion actually works |
| 14 | Issue #11 — Reset lastBackendFetchMs on signOut | Prevents stale cache on rapid re-login |
| 15 | Issue #14 — Key UserProfile by Firebase UID | Architectural hygiene; safe after Issue #1 fixed |
