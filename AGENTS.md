# RydLink — Agent Instructions

## Project Overview

RydLink: ride-hailing + parcel delivery app for Gulu, Uganda.

- **Android**: Kotlin / Jetpack Compose / Room / Retrofit / Socket.io / Firebase Auth + FCM
- **Backend**: Node.js / Express / PostgreSQL / Socket.io / Firebase Admin SDK
- **Package**: `com.example` | **App ID**: `com.qaphael.bodaapp`
- **Min SDK**: 24 | **Target SDK**: 36 | **Java**: 11

## Build & Run

```bash
# Android (requires JAVA_HOME or Android Studio)
./gradlew.bat assembleDebug
./gradlew.bat lint

# Backend (local)
cd backend && npm install && npm start

# Backend (Docker on VPS)
cd /opt/boda-gulu && docker compose up -d --build
# Force rebuild:
docker build --no-cache -t boda-gulu-api . && docker stop boda-gulu-api; docker rm boda-gulu-api; docker compose up -d
```

No test suite. Build verification = compile + lint only.

## Architecture

### Android (feature modules)

```
app/src/main/java/com/example/
├── ui/
│   ├── components/     DesignSystem, BodaButton, BodaCard, BodaTextField, Dialogs, Overlays, MapViews
│   ├── navigation/     Screen (sealed class), AppNavigation (NavHost)
│   ├── auth/           SplashScreen, OnboardingScreen, AuthViewModel
│   ├── home/           HomeScreen, HomeViewModel
│   ├── ride/           SearchPlaces, RoutePreview, Matching, RiderEnRoute, ActiveTrip, PostTrip, RideViewModel
│   ├── driver/         DriverOnboarding, DriverViewModel
│   ├── wallet/         WalletScreen, WalletViewModel
│   ├── profile/        ProfileSettings, EmergencyContacts, SavedPlaces, Support, ProfileViewModel
│   ├── chat/           RiderChatOverlay, ChatViewModel
│   ├── referrals/      ReferralsScreen, ReferralsViewModel
│   ├── offline/        OfflineSMSBookingOverlay, OfflineViewModel
│   ├── trips/          TripsHistoryScreen
│   └── theme/          Theme, colors, typography
├── ui/BodaViewModel.kt  State coordinator (557 lines) — owns StateFlows, init, internal helpers
└── data/               Entities, Dao, Repository, ApiClient, WebSocketClient, BodaMessagingService
```

### Backend (extracted routes)

```
backend/
├── server.js           Bootstrap only (60 lines) — app, middleware, route mounting
├── config/             rateLimits.js, env.js (Zod validation)
├── middleware/          auth.js (Firebase), admin.js
├── services/           push.service.js, fare.service.js, trip.cleanup.js
├── sockets/            socket.handler.js
├── routes/             users, places, trips, drivers, wallet, contacts, referrals, sos, promos, admin
├── schemas/            Zod validation schemas for all POST/PATCH endpoints
└── db.js               PostgreSQL pool
```

### Deployment

| Resource | URL | Port |
|----------|-----|------|
| Backend API | `https://ryd-api.ocaya.space` | 3002 |
| Admin Dashboard | `https://rydlink-admin.ocaya.space` | Same server |
| PostgreSQL | `boda-postgres` container | 5432, `boda_gulu` DB |

**VPS**: `212.47.72.186` (root SSH)

## Key Patterns

### ViewModel Extension Functions

Feature ViewModels use Kotlin extension functions on `BodaViewModel`:

```kotlin
// In ui/ride/RideViewModel.kt
fun BodaViewModel.bookTripViaBackend() { ... }

// Callers import the extension
import com.example.ui.ride.bookTripViaBackend
```

ViewModel members are `internal` (not `private`) so extension functions in other packages can access them.

### Color System

Custom `Color` object (`ui/components/DesignSystem.kt`) — NOT `androidx.compose.ui.graphics.Color`:

```kotlin
import androidx.compose.ui.graphics.Color as ComposeColor
import com.example.ui.components.Color  // theme-aware delegator
```

- `Color(0xFF...)` → adapts to light/dark mode
- `ComposeColor(0xFF...)` → ONLY brand colors (MTN Yellow `0xFFFDB913`, Green, Orange)
- `Color.White`/`Color.Black` → text that inverts with theme

### Components

- `BodaButton` / `BodaSecondaryButton` — prefer over raw `Button()`
- `BodaCard` — callers pass modifier (no `.fillMaxWidth()` inside)
- `BodaTextField` — always use instead of raw `OutlinedTextField`
- `object Sp` tokens: `xs=4dp`, `sm=8dp`, `md=16dp`, `lg=24dp`, `xl=32dp`, `xxl=48dp`

### Navigation

`Screen` sealed class in `ui/navigation/Screen.kt`. `viewModel.navigateTo(Screen.X)`. Manual backstack.

### Hardcoded Backgrounds

Screens with `Color(0xFF0F172A)` use hardcoded dark mode — use `ComposeColor.White` for text inside them.

### Error Handling

`viewModel.errorMessage` (MutableStateFlow<String?>) → SnackbarHostState.

### Localization

`BodaLang.get(language, key)`. Languages: `en`, `ach` (Acholi), `luo` (Lango).

## Backend API

Base: `https://ryd-api.ocaya.space`

All routes have Zod validation. Key endpoints: `/health`, `/api/users/*`, `/api/trips/*`, `/api/wallet/*`, `/api/drivers/*`, `/api/admin/*`.

WebSocket events: `new_trip_request`, `driver_location_update`, `update_live_gps`, `chat_message`, `chat_typing`, `pricing_rules_updated`.

## Firebase

- **Auth**: Phone OTP (`.setActivity()` required)
- **Service Account**: `app/boda-app-99092-firebase-adminsdk-fbsvc-7f08dc1e3b.json`
- **FCM**: `BodaMessagingService` + backend `admin.messaging().send()`

## Gotchas

1. **JAVA_HOME not set** — use Android Studio for builds
2. **No test suite** — compile + lint only
3. **Port conflicts on VPS** — API uses 3002
4. **Docker cache** — use `--no-cache` to force rebuild
5. **express-rate-limit v7** — use `validate: false` for Nginx proxy
6. **Firebase RTDB removed** — all data through PostgreSQL + Socket.IO
7. **Docker root vs backend/** — Docker COPY uses root `server.js`, not `backend/server.js`
