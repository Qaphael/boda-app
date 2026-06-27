# Boda Gulu — Agent Instructions

## Project Overview

Android Jetpack Compose ride-hailing app for Gulu, Uganda with Node.js backend, PostgreSQL database, and admin dashboard.

- **Package**: `com.example` | **App ID**: `com.qaphael.bodaapp`
- **Min SDK**: 24 | **Target SDK**: 36 | **Java**: 11
- **Language**: Kotlin | **UI**: Jetpack Compose Material3
- **Room DB version**: 3 (`fallbackToDestructiveMigration()` — no explicit migrations)

## Build & Run

```bash
# Android app (requires JAVA_HOME set, or use Android Studio)
./gradlew.bat assembleDebug
./gradlew.bat installDebug
./gradlew.bat lint

# Backend (local)
cd backend && npm install && npm start

# Backend (Docker on VPS)
cd /opt/boda-gulu && docker compose up -d --build
# Force rebuild (Docker cache often uses stale COPY):
docker build --no-cache -t boda-gulu-api . && docker stop boda-gulu-api; docker rm boda-gulu-api; docker compose up -d
```

No test suite currently configured. Build verification = compile + lint only.

## Architecture

### Android App (single-module)

| File | Role |
|------|------|
| `MainActivity.kt` | Entry point |
| `ui/BodaScreens.kt` | **All UI composables (~6700 lines, single file — intentional)** |
| `ui/BodaViewModel.kt` | ViewModel + Screen sealed class + navigation + all business logic |
| `data/BodaRepository.kt` | Room + API operations, `clearAllUserData()`, `UserNotFoundException` |
| `data/BodaApiService.kt` | Retrofit interface + all DTOs (`UserSyncResponse`, `SyncApiResponse`, etc.) |
| `data/ApiClient.kt` | Retrofit setup, cached Firebase token, OkHttp `Authenticator` for 401/403 retry |
| `data/Entities.kt` | Room entities: `UserProfile(id=1)`, `Trip`, `WalletTransaction`, etc. |
| `data/Dao.kt` | Room DAO with delete methods for all 6 tables |
| `data/AppDatabase.kt` | Room DB v3, `boda_gulu_database`, singleton pattern |
| `data/WebSocketClient.kt` | Socket.IO client for real-time trip/chat events |
| `data/BodaMessagingService.kt` | FCM token registration + push handling |
| `ui/theme/` | Theme, colors, typography |

### Backend

| Path | Role |
|------|------|
| `backend/server.js` | Express API + Socket.io server (also at VPS root `/opt/boda-gulu/server.js`) |
| `backend/db.js` | PostgreSQL connection pool |
| `backend/schema.sql` | Database schema (users, drivers, trips, transactions) |
| `backend/middleware/auth.js` | Firebase Admin SDK token verification + dev-mode allowlist |
| `admin-dashboard/` | Static HTML/JS admin panel (served from Express at `/admin`) |

### Deployment

| Resource | URL | Port |
|----------|-----|------|
| Backend API | `https://ryd-api.ocaya.space` | 3002 (mapped from 3000 inside container) |
| Admin Dashboard | `https://rydlink-admin.ocaya.space` | Same server, proxied via Nginx |
| PostgreSQL | `boda-postgres` container | 5432 (internal), `boda_gulu` database |
| Redis | `boda-redis` container | 6379 |

**VPS**: `212.47.72.186` (root SSH with key auth)

## Auth Flow

1. **Startup**: `init{}` checks `auth.currentUser` → if present, `isOtpVerified = true` + `restoreSessionFromBackend()` (fetches `GET /api/users/me`, falls back to sync on 404)
2. **Login**: Phone OTP → `signInWithCredential()` → `isOtpVerified = true` → profile setup → sync to backend
3. **API calls**: `ApiClient` uses cached Firebase token in interceptor; `Authenticator` auto-refreshes on 401/403
4. **Logout**: `signOut()` → clear Room (6 tables) → clear SharedPreferences → invalidate cached token → reset state → clear backStack
5. **Delete account**: `deleteAccount()` → backend `DELETE /api/users/me` → Firebase `auth.currentUser.delete()` → full cleanup

**Key DTOs**: `UserSyncResponse` (snake_case matching backend), `SyncApiResponse`, `UserMeResponse` — separate from Room entity `UserProfile`.

## Color System

Custom `Color` object (NOT `androidx.compose.ui.graphics.Color`) provides theme-aware delegation:

```kotlin
import androidx.compose.ui.graphics.Color as ComposeColor
import com.example.ui.Color  // defined at top of BodaScreens.kt
```

- `Color(0xFF...)` → backgrounds, borders, text that adapt to light/dark mode
- `ComposeColor(0xFF...)` → ONLY brand colors (MTN Yellow `0xFFFDB913`, Green `0xFF10B981`, Orange `0xFFF97316`)
- `Color.White`/`Color.Black` → text that inverts with theme
- `ComposeColor.White`/`ComposeColor.Black` → ONLY inside hardcoded dark-bg screens
- Red `0xFFE4002B`/`0xFFEF4444` → maps to Orange `0xFFF97316` in light mode via Color object
- `0xFF131A2A` → maps to light grey in light mode — always use `Color(0xFF131A2A)`

## Typography

Font: **Nunito** (5 weights in `res/font/`), applied globally via `ProvideTextStyle`.

| Level | Size | Weight | Use |
|-------|------|--------|-----|
| Display | 24sp | ExtraBold | Screen titles |
| Title | 18sp | Bold | Card headings |
| Body | 14sp | Normal | Primary text |
| Label | 12sp | Bold | Secondary labels |
| Caption | 11sp | Medium | Subtitles, hints |

## Components & Spacing

- `BodaButton` / `BodaSecondaryButton` — prefer over raw `Button()`. 12dp radius, 52dp height.
- `BodaCard` — no `.fillMaxWidth()` inside; callers pass modifier.
- `BodaTextField` — always use instead of raw `OutlinedTextField`.
- `SystemOverlayDialog` — has internal padding, no outer padding.
- `object Sp` tokens: `xs=4dp`, `sm=8dp`, `md=16dp`, `lg=24dp`, `xl=32dp`, `xxl=48dp`. No raw dp values.

## Navigation

`Screen` sealed class in `BodaViewModel.kt`. `viewModel.navigateTo(Screen.X)`. Manual backstack (no Navigation Compose).

Bottom nav: Home, TripsHistory, Wallet, ProfileSettings. Support accessible from ProfileSettings.

## Hardcoded Backgrounds

Screens with `Color(0xFF0F172A)` background use hardcoded dark mode — use `ComposeColor.White` for text inside them: DriverOnboardingScreen, CallOverlay, RiderChatOverlay.

## Error Handling

`viewModel.errorMessage` (MutableStateFlow<String?>) → SnackbarHostState. Every catch block and validation failure sets `errorMessage.value`.

## Localization

`BodaLang` object: `get(language, key)`. Languages: `en`, `ach` (Acholi), `luo` (Lango).

## Backend API

Base URL: `https://ryd-api.ocaya.space`

### Key Endpoints

| Endpoint | Method | Auth | Purpose |
|----------|--------|------|---------|
| `/health` | GET | No | Health check |
| `/api/users/sync` | POST | Firebase | Sync user to PostgreSQL (returns `SyncApiResponse`) |
| `/api/users/me` | GET | Firebase | Get profile (returns `UserMeResponse`) |
| `/api/users/me` | DELETE | Firebase | Delete account |
| `/api/users/fcm-token` | POST | Firebase | Store FCM push token |
| `/api/trips/book` | POST | Firebase | Book a ride |
| `/api/trips/:id` | GET | Firebase | Get trip status |
| `/api/trips/:id/messages` | GET | Firebase | Chat history for trip |
| `/api/trips/:id/status` | PATCH | Firebase | Update trip status |
| `/api/wallet/transactions` | GET | Firebase | User transaction history |
| `/api/wallet/topup` | POST | Firebase | Wallet deposit via MoMo |
| `/api/wallet/pay` | POST | Firebase | Ride payment via wallet/mobile money |
| `/api/emergency-contacts` | GET/POST | Firebase | List/add emergency contacts |
| `/api/emergency-contacts/:id` | DELETE | Firebase | Remove emergency contact |
| `/api/saved-places/:id` | DELETE | Firebase | Remove saved place |
| `/api/referrals` | GET/POST | Firebase | List/add referrals |
| `/api/drivers/register` | POST | No | Register driver |
| `/api/drivers/status` | POST | No | Update driver GPS/online |
| `/api/trips/calculate-fare` | POST | No | Fare calculation |
| `/api/promos/validate` | POST | No | Validate promo code |
| `/api/admin/*` | GET/POST | No | Admin dashboard endpoints |

### WebSocket Events

| Event | Direction | Purpose |
|-------|-----------|---------|
| `new_trip_request` | Server→Client | Broadcast new ride request |
| `driver_location_update` | Server→Client | Driver GPS update |
| `update_live_gps` | Client→Server | Driver GPS broadcast |
| `pricing_rules_updated` | Server→Client | Surge pricing change |
| `chat_message` | Bidirectional | Rider/driver chat (persisted to PostgreSQL) |
| `chat_typing` | Bidirectional | Typing indicator |

## Firebase Configuration

- **Auth**: Phone OTP verification (`.setActivity()` required for reCAPTCHA/SafetyNet)
- **Service Account Key**: `app/boda-app-99092-firebase-adminsdk-fbsvc-7f08dc1e3b.json`
- **FCM**: `BodaMessagingService` handles token registration + push. Backend sends via `admin.messaging().send()`.

## Deployment (VPS)

```bash
# On VPS at /opt/boda-gulu
docker compose up -d --build
docker ps | grep boda
curl http://localhost:3002/health
```

**Docker critical**: `server.js` at ROOT (`/opt/boda-gulu/server.js`) is what Docker COPY uses, NOT `backend/server.js`. Always update the root copy for VPS deploys.

**Nginx**: `ryd-api.ocaya.space` → `localhost:3002` | `rydlink-admin.ocaya.space` → `localhost:3002/admin/`

**Database**: PostgreSQL in `boda-postgres` container. DB: `boda_gulu` (user: `boda`, password: `boda123`). Schema auto-applied via Docker init.

## Key Gotchas

1. **JAVA_HOME not set** — Gradle CLI fails. Use Android Studio for builds.
2. **Single-file UI** — `BodaScreens.kt` is ~6700 lines. This is intentional — keep it that way.
3. **No test suite** — no unit or instrumented tests exist yet.
4. **Port conflicts on VPS** — ports 3000, 3001, 3100, 5432, 6379 are taken. API uses 3002.
5. **Docker root vs backend/** — `server.js` exists at both `/opt/boda-gulu/server.js` (ROOT, what Docker copies) AND `/opt/boda-gulu/backend/server.js`. Always update the root file.
6. **Docker cache** — `docker compose up -d --build` often uses cached COPY layers. Use `docker build --no-cache` + stop/rm + `docker compose up -d` to force rebuild.
7. **express-rate-limit v7** — `validate: { xForwardedForHeader: false }` doesn't work. Use `validate: false` for Nginx proxy.
8. **Firebase RTDB removed** — all ride/driver data flows through PostgreSQL + Socket.IO.
9. **Commit on confirmation** — When user confirms a change works, commit and push to GitHub.
