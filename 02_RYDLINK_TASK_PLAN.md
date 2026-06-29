# RydLink — Refactor Task Execution Plan

**Read this before starting any prompt.**  
Tasks are ordered so each one compiles/runs before the next begins. Do not skip steps. Do not combine steps.

---

## Phase 0 — Preparation (manual, no code)

**Before running any prompt:**

1. Make sure you are in `D:\Current\remix_-boda-gulu\` on the Windows machine (username Kiro)
2. Commit everything to git: `git add -A && git commit -m "chore: snapshot before monorepo refactor"`
3. Create a branch: `git checkout -b refactor/monorepo-structure`
4. Confirm the Android app builds on device before touching anything: run it once, make sure it opens

---

## Phase 1 — Backend Restructure
*Work in: `backend/`*  
*Goal: break `server.js` (1,078 lines) into clean layers without changing any logic*

| # | Task | Prompt Doc Section | Verify |
|---|---|---|---|
| 1.1 | Create `config/` folder with rate limiters and env | Prompt 1.1 | `node backend/src/server.js` starts |
| 1.2 | Extract middleware: `admin.js` | Prompt 1.2 | server starts, `/health` returns 200 |
| 1.3 | Extract `services/push.service.js` | Prompt 1.3 | server starts |
| 1.4 | Extract `services/fare.service.js` and `services/trip.cleanup.js` | Prompt 1.4 | server starts |
| 1.5 | Extract `sockets/socket.handler.js` | Prompt 1.5 | server starts, socket connects |
| 1.6 | Extract `routes/users.routes.js` | Prompt 1.6 | POST /api/users/sync works |
| 1.7 | Extract `routes/trips.routes.js` | Prompt 1.7 | POST /api/trips/book works |
| 1.8 | Extract `routes/drivers.routes.js` + `routes/wallet.routes.js` | Prompt 1.8 | server starts |
| 1.9 | Extract remaining routes (places, contacts, referrals, sos, promos) | Prompt 1.9 | server starts |
| 1.10 | Extract `routes/admin.routes.js` | Prompt 1.10 | GET /api/admin/stats returns data |
| 1.11 | Slim down `server.js` to bootstrap only | Prompt 1.11 | full end-to-end test from Android |
| 1.12 | Add `packages/schemas/` Zod validation to all routes | Prompt 1.12 | bad request returns 400 with field errors |
| 1.13 | Fix known bug: driver earnings not written on completion | Prompt 1.13 | complete a trip, check drivers.earnings in DB |
| 1.14 | Fix known bug: referral reward hardcoded placeholder | Prompt 1.14 | referral completion credits correct user |

---

## Phase 2 — Android: Shared Components
*Work in: `app/src/main/java/com/example/ui/`*  
*Goal: extract reusable pieces from BodaScreens.kt first — these have no dependencies on other screens*

| # | Task | Prompt Doc Section | Verify |
|---|---|---|---|
| 2.1 | Create `ui/components/DesignSystem.kt` — extract Color, Sp, BodaLang objects | Prompt 2.1 | app compiles |
| 2.2 | Create `ui/components/BodaButton.kt` — extract all 4 button variants | Prompt 2.2 | app compiles, buttons render |
| 2.3 | Create `ui/components/BodaCard.kt` and `BodaTextField.kt` | Prompt 2.3 | app compiles |
| 2.4 | Create `ui/components/Dialogs.kt` — WelcomeBonusDialog, SystemOverlayDialog, MoMoPinDialog | Prompt 2.4 | app compiles |
| 2.5 | Create `ui/components/Overlays.kt` — OfflineBanner, NotificationPermissionNudge, CallOverlay | Prompt 2.5 | app compiles |
| 2.6 | Create `ui/components/MapViews.kt` — all map composables + lat/lng helpers | Prompt 2.6 | app compiles, map renders on home screen |

---

## Phase 3 — Android: Navigation
*Goal: isolate the NavHost so BodaAppContent is no longer in BodaScreens.kt*

| # | Task | Prompt Doc Section | Verify |
|---|---|---|---|
| 3.1 | Create `ui/navigation/Screen.kt` — move sealed class Screen from BodaViewModel | Prompt 3.1 | app compiles |
| 3.2 | Create `ui/navigation/AppNavigation.kt` — move BodaAppContent NavHost | Prompt 3.2 | app navigates between screens |

---

## Phase 4 — Android: Auth Feature
*Goal: auth screens and auth-related ViewModel functions in their own folder*

| # | Task | Prompt Doc Section | Verify |
|---|---|---|---|
| 4.1 | Create `ui/auth/SplashScreen.kt` | Prompt 4.1 | app compiles, splash shows on launch |
| 4.2 | Create `ui/auth/OnboardingScreen.kt` + helpers | Prompt 4.2 | onboarding flow works |
| 4.3 | Create `ui/auth/AuthViewModel.kt` — extract auth functions from BodaViewModel | Prompt 4.3 | app compiles |
| 4.4 | Fix known bug: session restore loop (OTP on relaunch) | Prompt 4.4 | relaunch app while logged in — goes to home, not OTP |

---

## Phase 5 — Android: Home Feature

| # | Task | Prompt Doc Section | Verify |
|---|---|---|---|
| 5.1 | Create `ui/home/HomeScreen.kt` + `PassengerHomeScreen.kt` | Prompt 5.1 | home screen loads |
| 5.2 | Create `ui/home/HomeViewModel.kt` | Prompt 5.2 | app compiles |

---

## Phase 6 — Android: Ride Feature

| # | Task | Prompt Doc Section | Verify |
|---|---|---|---|
| 6.1 | Create `ui/ride/SearchPlacesScreen.kt` | Prompt 6.1 | search screen opens |
| 6.2 | Create `ui/ride/RoutePreviewScreen.kt` | Prompt 6.2 | route preview shows |
| 6.3 | Create `ui/ride/MatchingScreen.kt` + `RiderEnRouteScreen.kt` | Prompt 6.3 | matching flow works |
| 6.4 | Create `ui/ride/ActiveTripScreen.kt` + `PostTripScreen.kt` | Prompt 6.4 | active trip and rating work |
| 6.5 | Create `ui/ride/RideViewModel.kt` | Prompt 6.5 | app compiles |
| 6.6 | Fix known bug: user not created in PostgreSQL on first sync | Prompt 6.6 | fresh install → user row appears in DB |

---

## Phase 7 — Android: Driver Feature

| # | Task | Prompt Doc Section | Verify |
|---|---|---|---|
| 7.1 | Create `ui/driver/DriverHomeScreen.kt` + `DriverMiniStat` | Prompt 7.1 | driver home loads |
| 7.2 | Create `ui/driver/DriverOnboardingScreen.kt` | Prompt 7.2 | driver onboarding flow works |
| 7.3 | Create `ui/driver/DriverViewModel.kt` | Prompt 7.3 | app compiles |

---

## Phase 8 — Android: Remaining Features

| # | Task | Prompt Doc Section | Verify |
|---|---|---|---|
| 8.1 | Create `ui/wallet/WalletScreen.kt` + `WalletViewModel.kt` | Prompt 8.1 | wallet screen loads, topup works |
| 8.2 | Create `ui/profile/` — all 4 screens + ProfileViewModel | Prompt 8.2 | profile settings and contacts work |
| 8.3 | Create `ui/chat/RiderChatOverlay.kt` + `ChatViewModel.kt` | Prompt 8.3 | in-trip chat sends messages |
| 8.4 | Create `ui/referrals/ReferralsScreen.kt` + `ReferralsViewModel.kt` | Prompt 8.4 | referral list loads |
| 8.5 | Create `ui/offline/OfflineSMSBookingOverlay.kt` + `OfflineViewModel.kt` | Prompt 8.5 | offline SMS flow triggers |

---

## Phase 9 — Cleanup

| # | Task | Prompt Doc Section | Verify |
|---|---|---|---|
| 9.1 | Delete extracted sections from BodaScreens.kt (should be near-empty) | Prompt 9.1 | app compiles |
| 9.2 | Delete extracted functions from BodaViewModel.kt (should be thin coordinator) | Prompt 9.2 | app compiles, all features work |
| 9.3 | Move `admin-dashboard/` into `apps/admin/`, restructure into pages/ + components/ | Prompt 9.3 | admin dashboard loads |
| 9.4 | Move `backend/` into `apps/backend/`, move `app/` into `apps/android/` | Prompt 9.4 | Android builds, backend starts |
| 9.5 | Final smoke test: full ride booking flow end-to-end | Prompt 9.5 | passenger books → driver accepts → trip completes → earnings written |

---

## Summary: 40 total task prompts
- Phase 1 (backend): 14 prompts
- Phase 2 (Android components): 6 prompts  
- Phase 3 (navigation): 2 prompts
- Phase 4 (auth): 4 prompts
- Phase 5 (home): 2 prompts
- Phase 6 (ride): 6 prompts
- Phase 7 (driver): 3 prompts
- Phase 8 (remaining features): 5 prompts
- Phase 9 (cleanup): 5 prompts → leaves a clean, fully structured monorepo

**Estimated time:** 2–3 focused coding sessions, backend one day, Android spread across two.
