# RydLink — Architecture & Refactor Reference

**Project:** RydLink (ride-hailing + parcel delivery, Gulu, Uganda)  
**Stack:** Kotlin/Jetpack Compose (Android) · Node.js/Express (backend) · PostgreSQL · Firebase Auth · Socket.io  
**Current codebase root:** `D:\Current\remix_-boda-gulu\` (Windows, username Kiro)  
**Backend VPS:** Live on VPS, nginx proxied  

---

## 1. Target Monorepo Structure

```
rydlink/
├── apps/
│   ├── android/                      ← Kotlin app (currently: app/)
│   ├── backend/                      ← Node.js API (currently: backend/)
│   └── admin/                        ← Web dashboard (currently: admin-dashboard/)
├── packages/
│   ├── schemas/                      ← Zod validation schemas (shared)
│   └── constants/                    ← Shared string constants
├── docs/
├── .env.example
├── .gitignore
├── README.md
└── AGENTS.md
```

---

## 2. Android App — Final File Layout

**Package:** `com.example` (keep during refactor, rename to `com.rydlink` as a separate task)  
**Source root:** `app/src/main/java/com/example/`

### 2.1 `core/` — Infrastructure (no UI dependencies)
```
core/
├── AppDatabase.kt          ← from: data/AppDatabase.kt       (move, no changes)
├── ApiClient.kt            ← from: data/ApiClient.kt         (move, no changes)
└── WebSocketClient.kt      ← from: data/WebSocketClient.kt   (move, no changes)
```

### 2.2 `data/` — Data layer (entities, DAO, repository, API service)
```
data/
├── Entities.kt             ← keep as-is
├── Dao.kt                  ← keep as-is
├── BodaApiService.kt       ← keep as-is
├── BodaMessagingService.kt ← keep as-is
└── BodaRepository.kt       ← keep as-is
```

### 2.3 `ui/theme/` — Design system (no changes)
```
ui/theme/
├── Color.kt
├── Theme.kt
└── Type.kt
```

### 2.4 `ui/components/` — Reusable design system components
**Source:** Extracted from `BodaScreens.kt`

| New file | Extracts from BodaScreens.kt |
|---|---|
| `DesignSystem.kt` | `object Color`, `object Sp`, `object BodaLang` (lines ~89–276) |
| `BodaButton.kt` | `BodaButton`, `BodaSecondaryButton`, `BodaOutlinedButton`, `BodaTextButton` |
| `BodaCard.kt` | `BodaCard` |
| `BodaTextField.kt` | `BodaTextField` |
| `Dialogs.kt` | `WelcomeBonusDialog`, `SystemOverlayDialog`, `MoMoPinDialog` |
| `Overlays.kt` | `OfflineBanner`, `NotificationPermissionNudge`, `CallOverlay` |
| `MapViews.kt` | `GoogleMapViewWrapper`, `GuluMapView`, `GuluCanvasMapView`, `getLatLngForPlace`, `generateDetailedRoute`, `getLatLngOnPath` |

### 2.5 `ui/navigation/` — App navigation host
**Source:** Extracted from `BodaScreens.kt`

| New file | Extracts |
|---|---|
| `AppNavigation.kt` | `BodaAppContent` (line 733) — the NavHost with all route definitions |
| `Screen.kt` | `sealed class Screen` — from `BodaViewModel.kt` (line 36) |

### 2.6 `ui/auth/` — Authentication flow
**Source:** Extracted from `BodaScreens.kt`

| New file | Extracts |
|---|---|
| `SplashScreen.kt` | `SplashScreen` |
| `OnboardingScreen.kt` | `OnboardingScreen`, `OnboardingProgressBar`, `GoogleSignInButton` |
| `OtpScreen.kt` | OTP/phone login screen (inside BodaAppContent nav routes) |
| `AuthViewModel.kt` | `startOtpFlow`, `verifyOtp`, `completeProfileSetup`, `signOut`, `deleteAccount`, `signInWithGoogle` from `BodaViewModel` |

### 2.7 `ui/home/` — Passenger home
**Source:** Extracted from `BodaScreens.kt`

| New file | Extracts |
|---|---|
| `HomeScreen.kt` | `HomeScreen`, `PassengerHomeScreen` |
| `HomeViewModel.kt` | `refreshWalletBalance`, `handleDeepLink`, `shareReferralLink`, `navigateTo`, `navigateBack`, `updateLanguage`, `connectToBackend`, `syncUserToBackend`, `startLocationTracking`, `stopLocationTracking`, `updateAppThemeSetting`, `connectPostgresWebSocket`, `disconnectPostgresWebSocket`, `toggleNetworkConnection` |

### 2.8 `ui/ride/` — Ride booking, tracking, active trip
**Source:** Extracted from `BodaScreens.kt`

| New file | Extracts |
|---|---|
| `SearchPlacesScreen.kt` | `SearchPlacesScreen` |
| `RoutePreviewScreen.kt` | `RoutePreviewScreen` |
| `MatchingScreen.kt` | `MatchingScreen` |
| `RiderEnRouteScreen.kt` | `RiderEnRouteScreen` |
| `ActiveTripScreen.kt` | `ActiveTripScreen`, `SafetyActionsOverlay`, `getSimulatedSpeed`, `getActiveNavigationStep` |
| `PostTripScreen.kt` | `PostTripScreen` |
| `RideViewModel.kt` | `bookTripViaBackend`, `calculateFareViaBackend`, `validatePromoViaBackend`, `createRideRequest`, `cancelRideRequest`, `confirmBooking`, `startActiveTrip`, `cancelActiveTrip`, `submitPostTripRating`, `onPassengerTripCompleted`, `fetchRouteForPoints`, `searchLocations`, `fetchDistanceMatrix`, `triggerOSRMRouteFetch`, `disputeTrip` |

### 2.9 `ui/driver/` — Driver mode
**Source:** Extracted from `BodaScreens.kt`

| New file | Extracts |
|---|---|
| `DriverHomeScreen.kt` | `DriverHomeScreen`, `DriverMiniStat` |
| `DriverOnboardingScreen.kt` | `DriverOnboardingScreen` |
| `DriverViewModel.kt` | `registerDriverViaBackend`, `updateDriverStatusViaBackend`, `startDriverOnboarding`, `simulateDocUpload`, `completeDriverOnboarding`, `toggleDriverOnline`, `driverAcceptTrip`, `driverRejectTrip`, `driverArrivePickup`, `driverStartTrip`, `driverCompleteTrip`, `updateDriverLocation` |

### 2.10 `ui/wallet/` — Wallet & payments
**Source:** Extracted from `BodaScreens.kt`

| New file | Extracts |
|---|---|
| `WalletScreen.kt` | `WalletScreen` |
| `WalletViewModel.kt` | `startWalletTopup`, `confirmWalletTopupWithPin` |

### 2.11 `ui/profile/` — Profile, emergency contacts, saved places, support
**Source:** Extracted from `BodaScreens.kt`

| New file | Extracts |
|---|---|
| `ProfileSettingsScreen.kt` | `ProfileSettingsScreen` |
| `EmergencyContactsScreen.kt` | `EmergencyContactsScreen` |
| `SavedPlacesManageScreen.kt` | `SavedPlacesManageScreen` |
| `SupportScreen.kt` | `SupportScreen` |
| `ProfileViewModel.kt` | `saveUserProfile`, `addSavedPlace`, `removeSavedPlace`, `addEmergencyContact`, `removeEmergencyContact`, `submitSupportTicket`, `sendSupportChatMessage` |

### 2.12 `ui/chat/` — In-trip chat
**Source:** Extracted from `BodaScreens.kt`

| New file | Extracts |
|---|---|
| `RiderChatOverlay.kt` | `RiderChatOverlay` |
| `ChatViewModel.kt` | `openRiderChat`, `sendRiderChatMessage`, `onRiderChatInputChanged`, `initiateCall`, `endActiveCall` |

### 2.13 `ui/referrals/` — Referral program
**Source:** Extracted from `BodaScreens.kt`

| New file | Extracts |
|---|---|
| `ReferralsScreen.kt` | `ReferralsScreen` |
| `ReferralsViewModel.kt` | `simulateNewReferralSignUp`, `simulateReferralFirstTripCompletion`, `dismissWelcomeBonus` |

### 2.14 `ui/offline/` — Offline SMS booking
**Source:** Extracted from `BodaScreens.kt`

| New file | Extracts |
|---|---|
| `OfflineSMSBookingOverlay.kt` | `OfflineSMSBookingOverlay` |
| `OfflineViewModel.kt` | `triggerOfflineSMSBookingFlow`, `confirmOfflineSMSBooking`, `dispatchSOSSMS` |

### 2.15 Shared ViewModel (thin coordinator)
After splitting, `BodaViewModel.kt` becomes a thin coordinator that holds shared `StateFlow`s and delegates to feature ViewModels. The `BodaAppContent` NavHost collects state from here and passes it down.

---

## 3. Backend — Final File Layout

**Source root:** `apps/backend/src/`  
**Entry point:** `server.js` (≤50 lines — app bootstrap only)

### 3.1 `config/`
```
config/
├── env.js          ← Zod-validated process.env (crash early on missing vars)
└── rateLimits.js   ← globalLimiter, bookingLimiter, walletLimiter, syncLimiter
```

### 3.2 `db/`
```
db/
├── db.js           ← pg Pool (keep as-is)
└── schema.sql      ← keep as-is
```

### 3.3 `middleware/`
```
middleware/
├── auth.js         ← verifyFirebaseToken (keep as-is)
└── admin.js        ← adminAuth middleware (extracted from server.js)
```

### 3.4 `services/`
```
services/
├── push.service.js     ← sendPushToUser, sendPushToDriver
├── fare.service.js     ← calculateFare logic (extracted from /api/trips/calculate-fare)
└── trip.cleanup.js     ← setInterval auto-cancel job
```

### 3.5 `sockets/`
```
sockets/
└── socket.handler.js   ← io.on('connection', ...) entire block
```

### 3.6 `routes/`
```
routes/
├── users.routes.js     ← /api/users/*  (sync, fcm-token, me, delete)
├── trips.routes.js     ← /api/trips/*  (book, claim, status, calculate-fare, messages)
├── drivers.routes.js   ← /api/drivers/* (register, status)
├── wallet.routes.js    ← /api/wallet/* (transactions, topup, pay)
├── places.routes.js    ← /api/saved-places/*
├── contacts.routes.js  ← /api/emergency-contacts/*
├── referrals.routes.js ← /api/referrals/*
├── sos.routes.js       ← /api/sos
├── promos.routes.js    ← /api/promos/validate
└── admin.routes.js     ← /api/admin/* (all admin endpoints)
```

### 3.7 Final `server.js` skeleton
```js
require('dotenv').config();
const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
const { globalLimiter } = require('./config/rateLimits');
const { registerSocketHandlers } = require('./sockets/socket.handler');
const { startTripCleanup } = require('./services/trip.cleanup');

// Route imports
const usersRouter = require('./routes/users.routes');
const tripsRouter = require('./routes/trips.routes');
// ... etc

const app = express();
app.set('trust proxy', 1);
const server = http.createServer(app);
const io = new Server(server, { cors: { origin: '*', methods: ['GET', 'POST'] } });

app.use(globalLimiter);
app.use(cors());
app.use(express.json());

app.use('/api/users', usersRouter);
app.use('/api/trips', tripsRouter);
// ... etc

registerSocketHandlers(io);
startTripCleanup(io);

server.listen(process.env.PORT || 3000, '0.0.0.0', () => {
  console.log(`🚀 RydLink API running on port ${process.env.PORT || 3000}`);
});
```

---

## 4. Shared Packages

### 4.1 `packages/schemas/` — Zod validation
Used by: backend routes, admin dashboard

```
schemas/
├── user.schema.js
├── trip.schema.js
├── driver.schema.js
├── payment.schema.js
└── referral.schema.js
```

Example — `trip.schema.js`:
```js
const { z } = require('zod');

const bookTripSchema = z.object({
  pickup_name:   z.string().min(2),
  pickup_lat:    z.number().min(-90).max(90),
  pickup_lon:    z.number().min(-180).max(180),
  dropoff_name:  z.string().min(2),
  dropoff_lat:   z.number().min(-90).max(90),
  dropoff_lon:   z.number().min(-180).max(180),
  distance_km:   z.number().positive(),
  duration_mins: z.number().int().positive(),
  fare:          z.number().positive(),
  payment_method: z.enum(['MTN', 'Airtel', 'Wallet']),
  promo_applied: z.string().optional(),
});

const tripStatusSchema = z.object({
  status: z.enum(['pending','matching','en_route','active','completed','cancelled','disputed']),
  driver_uid:      z.string().optional(),
  rating:          z.number().int().min(1).max(5).optional(),
  comment:         z.string().optional(),
  dispute_reason:  z.string().optional(),
  dispute_evidence: z.string().optional(),
});

module.exports = { bookTripSchema, tripStatusSchema };
```

### 4.2 `packages/constants/`
```js
// tripStatus.js
const TRIP_STATUS = {
  PENDING:   'pending',
  MATCHING:  'matching',
  EN_ROUTE:  'en_route',
  ACTIVE:    'active',
  COMPLETED: 'completed',
  CANCELLED: 'cancelled',
  DISPUTED:  'disputed',
};

// paymentMethods.js
const PAYMENT_METHOD = {
  MTN:    'MTN',
  AIRTEL: 'Airtel',
  WALLET: 'Wallet',
};
```

---

## 5. Known Bugs to Fix During Refactor

Track these — each one has a clear home in the new structure:

| Bug | Location after refactor | Root cause |
|---|---|---|
| Session restore loop (OTP screen on relaunch) | `ui/auth/AuthViewModel.kt` | Firebase Auth state listener not awaited before navigation |
| User not created in PostgreSQL on first sync | `routes/users.routes.js` | Race condition between OTP verification and `/api/users/sync` call |
| Driver earnings not written on trip completion | `routes/trips.routes.js` — status PATCH handler | `driver_uid` null at completion time; earnings UPDATE skipped |
| Referral reward hardcoded placeholder | `routes/referrals.routes.js` | `referrer_uid` not pulled from authenticated user |

---

## 6. Rules for the Refactor

1. **One compilable step at a time.** Every task prompt ends with the Android project building successfully OR the backend starting without errors. Never leave it broken overnight.
2. **No logic changes during moves.** Copy functions exactly as they are. Bug fixes happen in a separate pass after the structure is correct.
3. **Imports update immediately.** When a function moves, update every import that referenced it in the same task.
4. **Shared state stays in BodaViewModel** until the split is complete. Feature ViewModels call into it or duplicate the StateFlow they need — decide per feature.
5. **Backend routes get Zod validation added when the route is extracted**, not before and not after.
6. **Test after every task prompt** by running the app on device (Android) or `node src/server.js` (backend).
