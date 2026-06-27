# Boda Gulu — Agent Instructions

## Project Overview

Android Jetpack Compose ride-hailing app for Gulu, Uganda with Node.js backend, PostgreSQL database, and admin dashboard.

- **Package**: `com.example`
- **Min SDK**: 24 | **Target SDK**: 36
- **Language**: Kotlin | **UI**: Jetpack Compose Material3

## Build & Run

```bash
# Android app
./gradlew.bat assembleDebug
./gradlew.bat installDebug
./gradlew.bat lint

# Backend (local)
cd backend && npm install && npm start

# Backend (Docker)
cd backend && docker compose up -d --build
```

Requires Android Studio with JDK 11+. No test suite currently configured.

## Architecture

### Android App

| File | Role |
|------|------|
| `app/src/main/java/com/example/MainActivity.kt` | Entry point |
| `app/src/main/java/com/example/ui/BodaScreens.kt` | All UI composables (~6500 lines, single file) |
| `app/src/main/java/com/example/ui/BodaViewModel.kt` | ViewModel + Screen sealed class + navigation |
| `app/src/main/java/com/example/data/` | Room DB, DAO, entities, API service, WebSocket client, FCM messaging |
| `app/src/main/java/com/example/ui/theme/` | Theme, colors, typography |

**Critical**: Almost all UI lives in `BodaScreens.kt`. This is intentional — keep it that way.

### Backend

| Path | Role |
|------|------|
| `backend/server.js` | Express API + Socket.io server |
| `backend/db.js` | PostgreSQL connection pool |
| `backend/schema.sql` | Database schema (users, drivers, trips, transactions) |
| `backend/middleware/auth.js` | Firebase Auth token verification |
| `admin-dashboard/` | Static HTML/JS admin panel (served from Express at `/admin`) |

### Deployment

| Resource | URL | Port |
|----------|-----|------|
| Backend API | `https://ryd-api.ocaya.space` | 3002 (mapped from 3000 inside container) |
| Admin Dashboard | `https://rydlink-admin.ocaya.space` | Same server, proxied via Nginx |
| PostgreSQL | `boda-postgres` container | 5432 (internal), boda_gulu database |
| Redis | `boda-redis` container | 6379 |

**VPS**: `212.47.72.186` (root SSH with key auth)

## Color System

Custom `Color` object (NOT `androidx.compose.ui.graphics.Color`) provides theme-aware delegation:

```kotlin
import androidx.compose.ui.graphics.Color as ComposeColor
import com.example.ui.Color  // (defined at top of BodaScreens.kt)
```

**Rules**:
- Use `Color(0xFF...)` for backgrounds, borders, text that must adapt to light/dark mode
- Use `ComposeColor(0xFF...)` ONLY for brand colors (MTN Yellow `0xFFFDB913`, Green `0xFF10B981`, Orange `0xFFF97316`)
- Use `Color.White` / `Color.Black` for text that must invert with theme
- Use `ComposeColor.White` / `ComposeColor.Black` ONLY inside screens with hardcoded dark backgrounds (DriverOnboardingScreen, CallOverlay)
- Red (`0xFFE4002B`, `0xFFEF4444`) maps to Vivid Orange `0xFFF97316` in light mode via Color object — do NOT use raw red
- `0xFF131A2A` maps to light grey in light mode — always use `Color(0xFF131A2A)`, never raw

## Typography

Font: **Nunito** (5 weights: Regular, Medium, SemiBold, Bold, ExtraBold) in `res/font/`.

Applied globally via `ProvideTextStyle(TextStyle(fontFamily = NunitoFamily))` wrapping the Scaffold.

**Type scale** (5 levels only):

| Level | Size | Weight | Use |
|-------|------|--------|-----|
| Display | 24sp | ExtraBold | Screen titles, hero text |
| Title | 18sp | Bold | Card headings, section headers |
| Body | 14sp | Normal | Primary text, descriptions |
| Label | 12sp | Bold | Secondary labels, metadata |
| Caption | 11sp | Medium | Subtitles, hints, badges |

## Spacing

Token system in `object Sp` at top of BodaScreens.kt:

```kotlin
object Sp {
    val xs  = 4.dp    val sm  = 8.dp    val md  = 16.dp
    val lg  = 24.dp   val xl  = 32.dp   val xxl = 48.dp
}
```

Use `Sp.xs/sm/md/lg/xl/xxl` for all Spacer heights and widths. No raw dp values.

## Components

- **BodaButton / BodaSecondaryButton** — prefer over raw `Button()`. Shape: 12dp radius, height 52dp.
- **BodaCard** — no `.fillMaxWidth()` inside; callers pass their own modifier.
- **BodaTextField** — standard input. Always use instead of raw `OutlinedTextField`.
- **SystemOverlayDialog** — has internal padding, do NOT add outer padding.

## Navigation

`Screen` sealed class in `BodaViewModel.kt`. Navigation via `viewModel.navigateTo(Screen.X)`.

Bottom nav: Home, TripsHistory, Wallet, ProfileSettings. Support accessible from ProfileSettings.

## Hardcoded Backgrounds

Screens with `Color(0xFF0F172A)` background use hardcoded dark mode. Inside these, use `ComposeColor.White` for text.

Affected: DriverOnboardingScreen, CallOverlay, RiderChatOverlay.

## Error Handling

`viewModel.errorMessage` (MutableStateFlow<String?>) observed by SnackbarHostState. Every catch block and validation failure should set `errorMessage.value = "..."`.

## Localization

`BodaLang` object with `get(language, key)`. Languages: `en`, `ach` (Acholi), `luo` (Lango).

## Backend API

Base URL: `https://ryd-api.ocaya.space`

### Key Endpoints

| Endpoint | Method | Auth | Purpose |
|----------|--------|------|---------|
| `/health` | GET | No | Health check |
| `/api/users/sync` | POST | Firebase | Sync user to PostgreSQL |
| `/api/users/fcm-token` | POST | No | Store FCM push token |
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
| `/api/admin/stats` | GET | No | Dashboard KPIs |
| `/api/admin/drivers` | GET | No | List all drivers |
| `/api/admin/riders` | GET | No | List all riders |
| `/api/admin/trips` | GET | No | List all trips |
| `/api/admin/pricing` | GET/POST | No | Surge pricing config |
| `/api/admin/promos` | GET/POST | No | Promo codes |
| `/api/admin/sos` | GET | No | SOS alerts |
| `/api/promos/validate` | POST | No | Validate promo code |

### WebSocket Events

| Event | Direction | Purpose |
|-------|-----------|---------|
| `new_trip_request` | Server→Client | Broadcast new ride request |
| `driver_location_update` | Server→Client | Driver GPS update |
| `update_live_gps` | Client→Server | Driver GPS broadcast |
| `pricing_rules_updated` | Server→Client | Surge pricing change |
| `chat_message` | Client→Server→Client | Rider/driver chat message (persisted to PostgreSQL) |
| `chat_typing` | Client→Server→Client | Typing indicator broadcast |

## Firebase Configuration

- **Auth**: Phone OTP verification
- **Database**: Realtime DB for ride state (fallback)
- **Service Account Key**: `app/boda-app-99092-firebase-adminsdk-fbsvc-7f08dc1e3b.json`

## Deployment

### Backend Docker

```bash
# On VPS
cd /opt/boda-gulu
docker compose up -d --build

# Check status
docker ps | grep boda
curl http://localhost:3002/health
```

### Nginx

- `ryd-api.ocaya.space` → proxy to `localhost:3002`
- `rydlink-admin.ocaya.space` → proxy to `localhost:3002/admin/` + API passthrough

### Database

- PostgreSQL runs in `boda-postgres` container (existing)
- Database: `boda_gulu` (user: `boda`, password: `boda123`)
- Schema at `backend/schema.sql` — auto-applied via Docker init

## Key Gotchas

1. **JAVA_HOME not set** — Gradle commands fail. Use Android Studio for builds.
2. **Single-file UI** — `BodaScreens.kt` is ~6500 lines. This is by design.
3. **No test suite** — no unit tests or instrumented tests exist.
4. **Port conflicts on VPS** — ports 3000, 3001, 3100, 5432, 6379 are taken. API uses 3002.
5. **Firebase key** — `backend/.env` must match existing PostgreSQL credentials (`boda`/`boda123`).
6. **Admin dashboard** — static files served from `/app/admin-dashboard` via volume mount. API calls from dashboard go through same-origin Nginx proxy.
7. **Hardcoded data** — backend `server.js` had demo SOS alerts and promo codes. Clear them for production.
8. **Network isolation** — containers must be on same Docker network (`boda_default`) to communicate.
9. **Firebase RTDB removed** — all ride/driver data flows through PostgreSQL + Socket.IO. No Firebase Realtime Database usage remains.
10. **FCM push notifications** — `BodaMessagingService` handles token registration and incoming pushes. Backend sends via `admin.messaging().send()`. Requires `fcm_token` column in users/drivers tables.
11. **Commit on confirmation** — When the user confirms a change works (tested in Android Studio or on device), immediately commit and push to GitHub.
8. **Network isolation** — containers must be on same Docker network (`boda_default`) to communicate.
9. **Firebase RTDB removed** — all ride/driver data flows through PostgreSQL + Socket.IO. No Firebase Realtime Database usage remains.
10. **FCM push notifications** — `BodaMessagingService` handles token registration and incoming pushes. Backend sends via `admin.messaging().send()`. Requires `fcm_token` column in users/drivers tables.
