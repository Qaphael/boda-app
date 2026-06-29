# RydLink — Claude Code Agent Prompts
# Copy and paste ONE prompt at a time. Wait for it to finish and verify before moving to the next.

---
---

## CONTEXT BLOCK — Include this at the start of every session (not every prompt)

```
You are working on RydLink, a ride-hailing and parcel delivery app for Gulu, Uganda.

Tech stack:
- Android: Kotlin / Jetpack Compose / Jetpack Navigation / Room / Retrofit / Socket.io client / Firebase Auth + FCM
- Backend: Node.js / Express / PostgreSQL (pg pool) / Socket.io / Firebase Admin SDK
- Package: com.example (Android), keep this namespace throughout the refactor
- Windows machine, project at D:\Current\remix_-boda-gulu\
- Backend is live on a VPS — do not change any API endpoint paths or response shapes

Cardinal rules for this refactor:
1. No logic changes. Move code exactly as written. Bug fixes are tagged separately.
2. After every file move, update ALL import statements that referenced the moved code.
3. Every prompt ends with the project in a compilable/runnable state.
4. Do not rename functions, classes, or variables during moves.
5. When extracting from BodaScreens.kt, leave the original function in place with a comment // MOVED TO: ui/xxx/Xxx.kt — remove it only in Phase 9.
6. When extracting from BodaViewModel.kt, leave the original function as a delegation stub that calls the feature ViewModel — remove stubs only in Phase 9.
```

---
---

## PROMPT 1.1 — Backend: Create config/ layer

```
Task: Extract rate limiters and environment config from backend/server.js into a config/ folder.

Steps:
1. Create backend/config/ directory
2. Create backend/config/rateLimits.js — move ALL four rateLimit() definitions out of server.js:
   - globalLimiter
   - bookingLimiter
   - walletLimiter
   - syncLimiter
   Export each as a named export.

3. Create backend/config/env.js — install zod if not present (npm install zod), then:
   const { z } = require('zod');
   const envSchema = z.object({
     DATABASE_URL: z.string().min(1),
     JWT_SECRET: z.string().optional(),
     ADMIN_SECRET_KEY: z.string().min(1),
     PORT: z.string().default('3000'),
     FIREBASE_PROJECT_ID: z.string().optional(),
   });
   const env = envSchema.parse(process.env);
   module.exports = { env };

4. In backend/server.js:
   - Remove the four rateLimit() definitions
   - Add at top: const { globalLimiter, bookingLimiter, walletLimiter, syncLimiter } = require('./config/rateLimits');
   - Add: require('./config/env'); (just the require, for early validation)
   - Keep everything else exactly as-is

Verify: Run `node backend/server.js` — it must start without errors. Hit GET /health and confirm 200 response.
```

---

## PROMPT 1.2 — Backend: Extract admin middleware

```
Task: Extract the adminAuth middleware from backend/server.js into backend/middleware/admin.js.

Steps:
1. Create backend/middleware/admin.js:
   const adminAuth = (req, res, next) => {
     const key = req.headers['x-admin-key'];
     if (!key || key !== process.env.ADMIN_SECRET_KEY) {
       return res.status(403).json({ error: 'Forbidden: invalid or missing admin key' });
     }
     next();
   };
   module.exports = { adminAuth };

2. In backend/server.js:
   - Remove the adminAuth function definition
   - Add at top: const { adminAuth } = require('./middleware/admin');
   - All existing usages of adminAuth in routes remain unchanged

Verify: `node backend/server.js` starts. Make a request to GET /api/admin/stats without the x-admin-key header — confirm you get 403.
```

---

## PROMPT 1.3 — Backend: Extract push notification service

```
Task: Extract sendPushToUser and sendPushToDriver from backend/server.js into a service file.

Steps:
1. Create backend/services/push.service.js:
   - Move the ENTIRE sendPushToUser async function
   - Move the ENTIRE sendPushToDriver async function
   - Add the required imports at the top: const db = require('../db'); const { getFirebaseAdmin } = require('../middleware/auth');
   - Export both: module.exports = { sendPushToUser, sendPushToDriver };

2. In backend/server.js:
   - Remove both function definitions
   - Add at top: const { sendPushToUser, sendPushToDriver } = require('./services/push.service');
   - All existing call sites (sendPushToUser(...) and sendPushToDriver(...)) stay exactly as-is

Verify: `node backend/server.js` starts. Book a trip via the Android app and confirm no crash on the push notification attempt.
```

---

## PROMPT 1.4 — Backend: Extract fare service and trip cleanup job

```
Task: Extract fare calculation logic and the auto-cancel setInterval job.

Steps:
1. Create backend/services/fare.service.js:
   - Extract the fare calculation logic that currently lives inside the POST /api/trips/calculate-fare handler
   - Create: async function calculateFare(distance_km, duration_mins) { ... }
     It should: query pricing_settings, compute rawFare, apply surge, return { base_fare, surge_multiplier, surge_reason, original_fare, final_fare }
   - Export: module.exports = { calculateFare };

2. Update the POST /api/trips/calculate-fare route in server.js to call calculateFare() instead of inline logic. Keep the route in server.js for now.

3. Create backend/services/trip.cleanup.js:
   - Move the ENTIRE setInterval block (the one that cancels trips stuck in 'matching' after 90 seconds)
   - Wrap it in: function startTripCleanup(io) { setInterval(async () => { ... }, 30000); }
   - Import: const db = require('../db'); const { sendPushToUser } = require('./push.service');
   - Export: module.exports = { startTripCleanup };

4. In backend/server.js:
   - Remove the setInterval block
   - Add: const { startTripCleanup } = require('./services/trip.cleanup');
   - Near the bottom, after io is defined: startTripCleanup(io);

Verify: `node backend/server.js` starts. Wait 2 minutes and confirm no unhandled errors in the console log. Call POST /api/trips/calculate-fare with { distance_km: 3, duration_mins: 10 } and confirm a valid fare response.
```

---

## PROMPT 1.5 — Backend: Extract Socket.io handler

```
Task: Extract the entire io.on('connection', ...) block from backend/server.js into backend/sockets/socket.handler.js.

Steps:
1. Create backend/sockets/ directory
2. Create backend/sockets/socket.handler.js:
   - Import at top: const db = require('../db');
   - Create: function registerSocketHandlers(io) { io.on('connection', (socket) => { ... all socket.on handlers ... }); }
   - Move ALL of the following socket events inside:
     * join_trip_channel
     * update_live_gps
     * chat_message
     * chat_typing
     * disconnect
   - Export: module.exports = { registerSocketHandlers };

3. In backend/server.js:
   - Remove the entire io.on('connection', ...) block
   - Add: const { registerSocketHandlers } = require('./sockets/socket.handler');
   - After io is created: registerSocketHandlers(io);

Verify: `node backend/server.js` starts. Connect a Socket.io client (or use the Android app) and confirm the 'New Web Socket Client Connected' log appears.
```

---

## PROMPT 1.6 — Backend: Extract users routes

```
Task: Extract all /api/users/* and /api/saved-places/* routes from server.js into route files.

Steps:
1. Create backend/routes/users.routes.js:
   - Import: express, db, verifyFirebaseToken, syncLimiter from '../config/rateLimits'
   - Create: const router = express.Router();
   - Move these routes (adjust paths — remove the /api/users prefix since the router will be mounted there):
     * POST /sync  (was /api/users/sync)
     * POST /fcm-token  (was /api/users/fcm-token)
     * GET /me  (was /api/users/me)
     * DELETE /me  (was /api/users/me DELETE)
   - Keep all logic inside each handler exactly as-is
   - Export: module.exports = router;

2. Create backend/routes/places.routes.js:
   - Move: GET /api/saved-places, POST /api/saved-places, DELETE /api/saved-places/:id
   - Mount path will be /api/saved-places so strip that prefix

3. In backend/server.js:
   - Remove all moved route definitions
   - Add: const usersRouter = require('./routes/users.routes'); const placesRouter = require('./routes/places.routes');
   - Add: app.use('/api/users', usersRouter); app.use('/api/saved-places', placesRouter);

Verify: `node backend/server.js` starts. From Android, complete a fresh sync (POST /api/users/sync) and confirm the user row is returned.
```

---

## PROMPT 1.7 — Backend: Extract trips routes

```
Task: Extract all /api/trips/* routes from server.js into backend/routes/trips.routes.js.

Steps:
1. Create backend/routes/trips.routes.js:
   - Imports: express, db, verifyFirebaseToken, bookingLimiter, io (pass io as a parameter — see note below), sendPushToUser, sendPushToDriver, calculateFare
   
   NOTE on io: the router needs the socket.io instance. Use a factory pattern:
     module.exports = function createTripsRouter(io) {
       const router = express.Router();
       // ... all routes ...
       return router;
     };
   
   - Move these routes (strip /api/trips prefix):
     * GET /  (trip history)
     * POST /book
     * PATCH /:id/claim
     * GET /:id
     * GET /:id/messages
     * PATCH /:id/status  ← this one has the earnings update bug, move it exactly as-is for now
     * POST /calculate-fare  (use the calculateFare service)

2. In backend/server.js:
   - Remove all moved route definitions
   - Add: const createTripsRouter = require('./routes/trips.routes');
   - Mount: app.use('/api/trips', createTripsRouter(io));

Verify: `node backend/server.js` starts. Book a ride from Android and confirm the trip appears in the DB.
```

---

## PROMPT 1.8 — Backend: Extract drivers and wallet routes

```
Task: Extract /api/drivers/* and /api/wallet/* routes.

Steps:
1. Create backend/routes/drivers.routes.js:
   - Factory: module.exports = function createDriversRouter(io) { ... }
   - Move: POST /register, POST /status (both need io for the broadcast)
   - Strip /api/drivers prefix

2. Create backend/routes/wallet.routes.js:
   - Imports: express, db, verifyFirebaseToken, walletLimiter
   - Move: GET /transactions, POST /topup, POST /pay
   - Strip /api/wallet prefix
   - Export: module.exports = router; (no io needed)

3. In backend/server.js:
   - Remove moved routes
   - Add: const createDriversRouter = require('./routes/drivers.routes'); const walletRouter = require('./routes/wallet.routes');
   - Mount: app.use('/api/drivers', createDriversRouter(io)); app.use('/api/wallet', walletRouter);

Verify: `node backend/server.js` starts. Toggle a driver online from Android and confirm is_online updates in DB.
```

---

## PROMPT 1.9 — Backend: Extract remaining user-facing routes

```
Task: Extract emergency contacts, referrals, SOS, and promo validation routes.

Steps:
1. Create backend/routes/contacts.routes.js:
   - Move: GET /, POST /, DELETE /:id (all /api/emergency-contacts)
   - Export: module.exports = router;

2. Create backend/routes/referrals.routes.js:
   - Move: GET /, POST /, POST /:id/complete (all /api/referrals)
   - Export: module.exports = router;

3. Create backend/routes/sos.routes.js:
   - Factory (needs io): module.exports = function createSosRouter(io) { ... }
   - Move: POST /api/sos (the user-facing one, NOT the admin resolve endpoint)

4. Create backend/routes/promos.routes.js:
   - Move: POST /api/promos/validate
   - Export: module.exports = router;

5. In backend/server.js:
   - Remove all moved routes
   - Mount all routers at their correct paths

Verify: `node backend/server.js` starts. Add an emergency contact from Android and confirm it persists.
```

---

## PROMPT 1.10 — Backend: Extract admin routes

```
Task: Extract all /api/admin/* routes from server.js into backend/routes/admin.routes.js.

Steps:
1. Create backend/routes/admin.routes.js:
   - Factory (needs io): module.exports = function createAdminRouter(io) { ... }
   - Import: adminAuth from '../middleware/admin'
   - Move ALL admin routes (strip /api/admin prefix):
     * GET /stats
     * GET /active-trips
     * GET /pricing, POST /pricing
     * GET /promos, POST /promos
     * GET /sos, POST /sos/:id/resolve
     * GET /drivers, POST /drivers/:uid/toggle-status, POST /drivers/:uid/settle
     * GET /riders, POST /riders/:uid/credit, POST /riders/:uid/status
     * GET /trips
   - Every route must still have adminAuth as middleware

2. In backend/server.js:
   - Remove all moved admin routes
   - Mount: app.use('/api/admin', createAdminRouter(io));

Verify: Open the admin dashboard in a browser. Confirm stats, drivers, and trips all load correctly.
```

---

## PROMPT 1.11 — Backend: Slim down server.js to bootstrap only

```
Task: server.js should now contain only app creation, middleware, route mounting, and server.listen. Clean it up.

Steps:
1. Review backend/server.js — remove any remaining inline route definitions (there should be none left)
2. Confirm the file contains only:
   - Requires and imports
   - App creation (express(), http.createServer, new Server)
   - Middleware (globalLimiter, cors, express.json, static admin dashboard)
   - Route mounting (app.use(...))
   - registerSocketHandlers(io)
   - startTripCleanup(io)
   - server.listen(...)
3. The file should be under 60 lines. If it's longer, something wasn't extracted.
4. Add a comment block at the top:
   /**
    * RydLink API — Entry Point
    * All route logic: routes/*.routes.js
    * Socket logic:    sockets/socket.handler.js
    * Background jobs: services/trip.cleanup.js
    */

Verify: `node backend/server.js` starts. Run the full ride booking flow from Android. Confirm trip books, driver can claim it, and status updates work.
```

---

## PROMPT 1.12 — Backend: Add Zod validation to all routes

```
Task: Add request body validation using Zod to every route that accepts a POST or PATCH body.

Steps:
1. Create packages/schemas/ directory at the repo root (or backend/schemas/ if monorepo root not set up yet)
2. Create the following schema files (add only what's needed for backend routes):

   trip.schema.js — bookTripSchema, tripStatusSchema, calculateFareSchema
   user.schema.js — syncUserSchema, fcmTokenSchema
   driver.schema.js — registerDriverSchema, driverStatusSchema
   wallet.schema.js — topupSchema, paySchema
   referral.schema.js — createReferralSchema
   contact.schema.js — createContactSchema
   promo.schema.js — validatePromoSchema, createPromoSchema
   admin.schema.js — pricingUpdateSchema, riderCreditSchema

3. In each route file, import the relevant schema and add validation at the top of each handler:
   const result = schema.safeParse(req.body);
   if (!result.success) {
     return res.status(400).json({ errors: result.error.flatten().fieldErrors });
   }
   // use result.data instead of req.body from here

4. Do NOT change any existing DB queries or response shapes.

Verify: Send a POST /api/trips/book with a missing fare field — confirm 400 with { errors: { fare: ['Required'] } }.
```

---

## PROMPT 1.13 — Fix Bug: Driver earnings not written on trip completion

```
Task: Fix the bug where driver earnings are not updated when a trip is marked completed.

Root cause: In the PATCH /api/trips/:id/status handler, when status = 'completed', the earnings UPDATE runs
against result.rows[0].driver_uid — but if driver_uid was null in the trip row at completion time, the UPDATE
silently skips. The Android app sometimes sends the driver_uid in the body to fix this, but it's not being
used reliably.

Location: backend/routes/trips.routes.js — the PATCH /:id/status handler.

Fix:
1. Before the earnings UPDATE, add a check:
   const completedTrip = result.rows[0];
   const effectiveDriverUid = completedTrip.driver_uid || req.body.driver_uid || req.user.uid;
   
2. Use effectiveDriverUid in the earnings UPDATE:
   UPDATE drivers SET earnings = earnings + $1, completed_trips = completed_trips + 1, updated_at = NOW() WHERE uid = $2
   params: [completedTrip.fare, effectiveDriverUid]

3. Add a console.log before the UPDATE:
   console.log(`[EARNINGS] trip=${req.params.id} driver=${effectiveDriverUid} fare=${completedTrip.fare}`);

4. If effectiveDriverUid is still null after the three checks, log a warning and skip the UPDATE — do not crash.

Verify: Complete a full trip from Android (driver accepts → passenger trips → driver marks complete). Query the database:
  SELECT uid, earnings, completed_trips FROM drivers WHERE uid = '<driver_uid>';
Confirm earnings increased by the trip fare.
```

---

## PROMPT 1.14 — Fix Bug: Referral reward hardcoded / wrong user credited

```
Task: Fix the referral completion endpoint so the correct referrer_uid is always used and the reward uses the DB value.

Location: backend/routes/referrals.routes.js — POST /:id/complete

Root cause: The referral row contains referrer_uid which is the correct user to credit. The endpoint
already reads this from the DB (ref.referrer_uid), but previously the code had a hardcoded placeholder.
Verify the current code uses ref.referrer_uid (not a hardcoded value) and add a guard.

Fix:
1. After fetching the referral row, add:
   if (!ref.referrer_uid) {
     return res.status(422).json({ error: 'Referral record has no referrer — cannot credit reward' });
   }

2. Confirm the wallet UPDATE uses ref.referrer_uid (not req.user.uid or any placeholder):
   UPDATE users SET wallet_balance = wallet_balance + $1 WHERE uid = $2
   params: [rewardAmt, ref.referrer_uid]

3. Confirm rewardAmt = parseFloat(ref.reward_amount) — it should come from the DB row, not be hardcoded to 3000.

4. Add logging:
   console.log(`[REFERRAL] id=${referralId} crediting uid=${ref.referrer_uid} amount=${rewardAmt}`);

Verify: Create a referral in the DB, call POST /api/referrals/:id/complete, check the referrer's wallet_balance increased by the correct reward_amount stored in the referrals table.
```

---
---

## PROMPT 2.1 — Android: Create DesignSystem.kt

```
Task: Extract the three utility objects from BodaScreens.kt into a new file.

Location: app/src/main/java/com/example/ui/components/DesignSystem.kt

Steps:
1. Create the directory ui/components/ if it doesn't exist
2. Create DesignSystem.kt with package com.example.ui.components
3. Move these three objects exactly as written:
   - object Color { ... }  (the custom color delegator, ~lines 89–128)
   - object Sp { ... }     (if present)
   - object BodaLang { ... } (the string map object)
4. Add the necessary imports at the top of DesignSystem.kt (copy them from BodaScreens.kt)
5. In BodaScreens.kt: add import com.example.ui.components.Color (or whichever are needed)
   and add a comment above each object: // MOVED TO: ui/components/DesignSystem.kt
   Leave the original objects in place for now — DO NOT delete them yet.
   
IMPORTANT: Because both BodaScreens.kt and DesignSystem.kt will define an object named Color,
you will get a conflict. Resolve it by:
- Removing the object from BodaScreens.kt immediately (just this one case — it's a pure data object with no Composable dependencies)
- Import it from DesignSystem.kt in BodaScreens.kt

Verify: Android project compiles with ./gradlew assembleDebug from D:\Current\remix_-boda-gulu\
```

---

## PROMPT 2.2 — Android: Create BodaButton.kt

```
Task: Extract all button composables from BodaScreens.kt into ui/components/BodaButton.kt.

Location: app/src/main/java/com/example/ui/components/BodaButton.kt

Steps:
1. Create BodaButton.kt with package com.example.ui.components
2. Move these four composables exactly as written:
   - fun BodaButton(...)
   - fun BodaSecondaryButton(...)
   - fun BodaOutlinedButton(...)
   - fun BodaTextButton(...)
3. Copy all required imports from BodaScreens.kt to BodaButton.kt
4. In BodaScreens.kt:
   - Add import com.example.ui.components.BodaButton (and the others)
   - Add comment above each function: // MOVED TO: ui/components/BodaButton.kt
   - Do NOT delete the functions yet

Verify: ./gradlew assembleDebug passes. Run app on device and confirm all buttons still render (home screen has BodaButton instances).
```

---

## PROMPT 2.3 — Android: Create BodaCard.kt and BodaTextField.kt

```
Task: Extract BodaCard and BodaTextField into separate component files.

Steps:
1. Create app/src/main/java/com/example/ui/components/BodaCard.kt:
   - Package: com.example.ui.components
   - Move: fun BodaCard(...) exactly as written
   - Add required imports

2. Create app/src/main/java/com/example/ui/components/BodaTextField.kt:
   - Package: com.example.ui.components
   - Move: fun BodaTextField(...) exactly as written
   - Add required imports

3. In BodaScreens.kt:
   - Add imports for both
   - Add // MOVED TO: comments above both
   - Do NOT delete yet

Verify: ./gradlew assembleDebug passes.
```

---

## PROMPT 2.4 — Android: Create Dialogs.kt

```
Task: Extract all dialog composables from BodaScreens.kt.

Location: app/src/main/java/com/example/ui/components/Dialogs.kt

Steps:
1. Create Dialogs.kt with package com.example.ui.components
2. Move these composables:
   - fun WelcomeBonusDialog(...)
   - fun SystemOverlayDialog(...)
   - fun MoMoPinDialog(...)  ← this one is large (~170 lines), move it completely
3. Add all required imports — MoMoPinDialog uses BodaTextField, BodaButton, so import from their new locations
4. In BodaScreens.kt: add imports + // MOVED TO: comments

Verify: ./gradlew assembleDebug passes. Run app and confirm the MoMo PIN dialog still appears when initiating a wallet topup.
```

---

## PROMPT 2.5 — Android: Create Overlays.kt

```
Task: Extract overlay composables from BodaScreens.kt.

Location: app/src/main/java/com/example/ui/components/Overlays.kt

Steps:
1. Create Overlays.kt with package com.example.ui.components
2. Move these composables:
   - fun OfflineBanner(...)
   - fun NotificationPermissionNudge(...)
   - fun CallOverlay(...)  ← this is large (~163 lines), move completely
3. Add all required imports — CallOverlay uses BodaButton, BodaTextField
4. In BodaScreens.kt: add imports + // MOVED TO: comments

Verify: ./gradlew assembleDebug passes.
```

---

## PROMPT 2.6 — Android: Create MapViews.kt

```
Task: Extract all map-related composables and helper functions from BodaScreens.kt.

Location: app/src/main/java/com/example/ui/components/MapViews.kt

Steps:
1. Create MapViews.kt with package com.example.ui.components
2. Move ALL of the following exactly as written:
   - fun getLatLngForPlace(...): LatLng
   - fun generateDetailedRoute(...): List<LatLng>
   - fun getLatLngOnPath(...): LatLng
   - fun GoogleMapViewWrapper(...)  — this is ~100 lines
   - fun GuluMapView(...)           — this is ~100 lines
   - fun GuluCanvasMapView(...)     — this is ~218 lines, the canvas-drawn offline map
3. Add ALL required imports — these functions use Google Maps, Canvas APIs, math functions
4. In BodaScreens.kt: add imports + // MOVED TO: comments

Verify: ./gradlew assembleDebug passes. Run app, navigate to the home screen, confirm the map renders.
```

---

## PROMPT 3.1 — Android: Create Screen.kt (navigation sealed class)

```
Task: Move the sealed class Screen from BodaViewModel.kt into its own file.

Location: app/src/main/java/com/example/ui/navigation/Screen.kt

Steps:
1. Create ui/navigation/ directory
2. Create Screen.kt:
   - Package: com.example.ui
   - Move sealed class Screen { ... } exactly as written (it's around line 36 of BodaViewModel.kt)
   - No imports needed beyond kotlin basics
3. In BodaViewModel.kt:
   - Add: import com.example.ui.Screen (or just leave it in same package — check the package declaration)
   - Add comment: // MOVED TO: ui/navigation/Screen.kt
   - Delete the sealed class from BodaViewModel.kt (sealed classes are safe to move since they have no runtime state)
4. In BodaScreens.kt, confirm Screen is still accessible (it should be since same package)

Verify: ./gradlew assembleDebug passes. Navigate between screens in the app.
```

---

## PROMPT 3.2 — Android: Create AppNavigation.kt

```
Task: Move BodaAppContent (the NavHost) from BodaScreens.kt to a navigation file.

Location: app/src/main/java/com/example/ui/navigation/AppNavigation.kt

Steps:
1. Create AppNavigation.kt:
   - Package: com.example.ui
   - Move: fun BodaAppContent(viewModel: BodaViewModel) { ... } — this is a large function (~140 lines starting at line 733) containing the NavHost
   - Add all required imports
2. In BodaScreens.kt:
   - Add import for BodaAppContent
   - Add // MOVED TO: comment
   - Do NOT delete yet
3. In MainActivity.kt:
   - Confirm the import for BodaAppContent still works (it should be same package)

Verify: ./gradlew assembleDebug passes. Launch app — confirm full navigation works: splash → onboarding → home → booking flow.
```

---

## PROMPT 4.1 — Android: Create SplashScreen.kt

```
Task: Extract SplashScreen from BodaScreens.kt.

Location: app/src/main/java/com/example/ui/auth/SplashScreen.kt

Steps:
1. Create ui/auth/ directory
2. Create SplashScreen.kt:
   - Package: com.example.ui.auth
   - Move: fun SplashScreen(viewModel: BodaViewModel) { ... }
   - Add all required imports (uses BodaViewModel, Compose, theme components)
3. In BodaScreens.kt and AppNavigation.kt: add import + // MOVED TO: comment

Verify: ./gradlew assembleDebug passes. Launch fresh app — splash screen appears.
```

---

## PROMPT 4.2 — Android: Create OnboardingScreen.kt

```
Task: Extract onboarding composables from BodaScreens.kt.

Location: app/src/main/java/com/example/ui/auth/OnboardingScreen.kt

Steps:
1. Create OnboardingScreen.kt:
   - Package: com.example.ui.auth
   - Move:
     * fun OnboardingScreen(viewModel: BodaViewModel) { ... }  — ~567 lines, the full carousel
     * fun OnboardingProgressBar(...)
     * fun GoogleSignInButton(...)
     * fun WelcomeBonusDialog(...)  — if it was NOT moved to Dialogs.kt already
2. Add all required imports — onboarding uses painterResource, images from res/drawable
3. In BodaScreens.kt and AppNavigation.kt: add imports + // MOVED TO: comments

Verify: ./gradlew assembleDebug passes. Fresh install simulation — onboarding carousel appears, progress dots work.
```

---

## PROMPT 4.3 — Android: Create AuthViewModel.kt

```
Task: Extract authentication-related functions from BodaViewModel.kt into a dedicated AuthViewModel.

Location: app/src/main/java/com/example/ui/auth/AuthViewModel.kt

Steps:
1. Create AuthViewModel.kt:
   - Package: com.example.ui.auth
   - Class: class AuthViewModel(application: Application) : AndroidViewModel(application)
   - Move these functions from BodaViewModel exactly as written:
     * fun startOtpFlow(...)
     * fun verifyOtp()
     * fun completeProfileSetup()
     * fun signOut()
     * fun deleteAccount()
     * fun signInWithGoogle(...)
   - These functions reference BodaViewModel state — for now, leave them referencing it via:
     Pass BodaViewModel as a constructor parameter: class AuthViewModel(val vm: BodaViewModel, application: Application)
     And call vm.stateFlow etc. where needed.
   
   ALTERNATIVE (simpler): Do NOT create a separate class yet. Instead, just create the file as an
   extension file:
   // AuthViewModel.kt — auth functions are defined as extensions on BodaViewModel
   // They will be moved to a true separate ViewModel in Phase 9
   This is acceptable for this phase.

2. In BodaViewModel.kt:
   - Add // MOVED TO: ui/auth/AuthViewModel.kt comment above each moved function
   - Keep the original functions in BodaViewModel as delegation stubs for now

Verify: ./gradlew assembleDebug passes.
```

---

## PROMPT 4.4 — Fix Bug: Session restore loop (OTP screen on relaunch)

```
Task: Fix the bug where users are sent back to the OTP screen every time the app is relaunched,
even when they are already authenticated.

Root cause: The Firebase Auth state listener result is not being awaited before the initial navigation
decision is made. The splash screen navigates to onboarding/OTP before FirebaseAuth.getInstance().currentUser
is populated.

Location: app/src/main/java/com/example/ui/auth/SplashScreen.kt and BodaViewModel.kt (the auth state init section).

Steps:
1. In BodaViewModel.kt, find where the initial screen is decided (look for navigateTo(Screen.Onboarding)
   or navigateTo(Screen.Home) — it's likely in an init block or in a LaunchedEffect in SplashScreen)

2. The fix: use FirebaseAuth.getInstance().addAuthStateListener { firebaseUser -> ... } and only
   call navigateTo() from INSIDE the listener callback, not before it.
   
   Pattern:
   init {
     FirebaseAuth.getInstance().addAuthStateListener { auth ->
       val user = auth.currentUser
       if (user != null) {
         // user is logged in — go to home (after profile sync)
         viewModelScope.launch { syncUserToBackend() }
         navigateTo(Screen.Home)
       } else {
         // not logged in
         navigateTo(Screen.Onboarding)
       }
     }
   }

3. Remove any other navigateTo() calls in the splash screen LaunchedEffect that fire before auth state is known.

4. In SplashScreen.kt: the composable should show a loading indicator while auth state is being determined.
   Add a loading state: var authChecked by remember { mutableStateOf(false) } — only render the splash
   logo while !authChecked.

Verify: 
   - Log in with OTP, close the app completely (remove from recents), reopen — confirm home screen loads, NOT OTP.
   - Sign out, close app, reopen — confirm onboarding/OTP screen loads.
```

---

## PROMPT 5.1 — Android: Create HomeScreen.kt

```
Task: Extract passenger home screens from BodaScreens.kt.

Location: app/src/main/java/com/example/ui/home/HomeScreen.kt

Steps:
1. Create ui/home/ directory
2. Create HomeScreen.kt:
   - Package: com.example.ui.home
   - Move:
     * fun HomeScreen(viewModel: BodaViewModel, savedPlaces: List<SavedPlace>) — the router screen
     * fun PassengerHomeScreen(viewModel: BodaViewModel, savedPlaces: List<SavedPlace>) — the main passenger home (~187 lines)
   - Add all required imports (uses BodaButton, BodaCard, DesignSystem, SavedPlace entity, etc.)
3. Also move: fun BodaBottomNavigation(viewModel: BodaViewModel) — the bottom nav bar
4. In BodaScreens.kt and AppNavigation.kt: add imports + // MOVED TO: comments

Verify: ./gradlew assembleDebug. Open app, go to home screen — map, bottom nav, and saved places load correctly.
```

---

## PROMPT 5.2 — Android: Create HomeViewModel.kt

```
Task: Extract home/session management functions from BodaViewModel into HomeViewModel.

Location: app/src/main/java/com/example/ui/home/HomeViewModel.kt

Follow the same pattern as AuthViewModel.kt (extension file or thin wrapper).

Move comments for:
- refreshWalletBalance
- handleDeepLink
- shareReferralLink
- navigateTo / navigateBack
- updateLanguage
- connectToBackend / syncUserToBackend
- startLocationTracking / stopLocationTracking
- updateAppThemeSetting
- connectPostgresWebSocket / disconnectPostgresWebSocket / toggleNetworkConnection

Add // MOVED TO: ui/home/HomeViewModel.kt comments in BodaViewModel.kt above each.

Verify: ./gradlew assembleDebug passes.
```

---

## PROMPT 6.1 — Android: Create SearchPlacesScreen.kt

```
Task: Extract the location search screen from BodaScreens.kt.

Location: app/src/main/java/com/example/ui/ride/SearchPlacesScreen.kt

Steps:
1. Create ui/ride/ directory
2. Create SearchPlacesScreen.kt:
   - Package: com.example.ui.ride
   - Move: fun SearchPlacesScreen(viewModel: BodaViewModel, savedPlaces: List<SavedPlace>) — ~234 lines
   - Add all required imports
3. In BodaScreens.kt and AppNavigation.kt: add imports + // MOVED TO: comments

Verify: ./gradlew assembleDebug. Tap the "Where to?" field on home screen — search screen opens, results appear.
```

---

## PROMPT 6.2 — Android: Create RoutePreviewScreen.kt

```
Task: Extract the route preview / fare confirmation screen.

Location: app/src/main/java/com/example/ui/ride/RoutePreviewScreen.kt

Steps:
1. Create RoutePreviewScreen.kt:
   - Package: com.example.ui.ride
   - Move: fun RoutePreviewScreen(viewModel: BodaViewModel, walletBalance: Double) — ~381 lines
   - Uses: MapViews (GuluMapView), BodaButton, promo dialog, fare display
2. In BodaScreens.kt and AppNavigation.kt: add imports + // MOVED TO: comments

Verify: ./gradlew assembleDebug. After selecting a destination, the route preview with fare estimate appears.
```

---

## PROMPT 6.3 — Android: Create MatchingScreen.kt and RiderEnRouteScreen.kt

```
Task: Extract the matching and en-route screens.

Steps:
1. Create app/src/main/java/com/example/ui/ride/MatchingScreen.kt:
   - Move: fun MatchingScreen(viewModel: BodaViewModel) — ~90 lines with the "Finding your rider..." animation

2. Create app/src/main/java/com/example/ui/ride/RiderEnRouteScreen.kt:
   - Move: fun RiderEnRouteScreen(viewModel: BodaViewModel) — ~117 lines

3. In BodaScreens.kt and AppNavigation.kt: add imports + // MOVED TO: comments for both

Verify: ./gradlew assembleDebug. Book a ride — matching screen appears after booking.
```

---

## PROMPT 6.4 — Android: Create ActiveTripScreen.kt and PostTripScreen.kt

```
Task: Extract the active trip experience and post-trip rating screens.

Steps:
1. Create app/src/main/java/com/example/ui/ride/ActiveTripScreen.kt:
   - Move:
     * fun ActiveTripScreen(viewModel: BodaViewModel) — the live tracking screen (~257 lines)
     * fun SafetyActionsOverlay(viewModel: BodaViewModel, onClose: () -> Unit) — ~234 lines
     * fun getSimulatedSpeed(...): Int
     * fun getActiveNavigationStep(...): String

2. Create app/src/main/java/com/example/ui/ride/PostTripScreen.kt:
   - Move: fun PostTripScreen(viewModel: BodaViewModel) — ~146 lines (rating, receipt)

3. In BodaScreens.kt and AppNavigation.kt: add imports + // MOVED TO: comments

Verify: ./gradlew assembleDebug. Accept and start a trip — active trip screen with map animation shows.
```

---

## PROMPT 6.5 — Android: Create RideViewModel.kt

```
Task: Group ride-related functions with MOVED TO comments.

Location: app/src/main/java/com/example/ui/ride/RideViewModel.kt

Follow extension file pattern. Add // MOVED TO: ui/ride/RideViewModel.kt comments in BodaViewModel.kt for:
- bookTripViaBackend
- calculateFareViaBackend
- validatePromoViaBackend
- createRideRequest / cancelRideRequest
- confirmBooking
- startActiveTrip / cancelActiveTrip
- submitPostTripRating
- onPassengerTripCompleted
- fetchRouteForPoints / searchLocations / fetchDistanceMatrix / triggerOSRMRouteFetch
- disputeTrip

Verify: ./gradlew assembleDebug passes.
```

---

## PROMPT 6.6 — Fix Bug: User not created in PostgreSQL on first sync

```
Task: Fix the bug where a new user's first sync after OTP verification does not create a row in the users table.

Root cause: The Android app calls /api/users/sync immediately after Firebase OTP verification. At that moment,
the Firebase ID token is freshly minted but the phone number may not yet be in the token claims.
The backend receives uid but phone = null, which can violate the NOT NULL constraint on users.phone.

Location: backend/routes/users.routes.js — POST /sync handler. Also: Android BodaViewModel.kt — syncUserToBackend().

Backend fix:
1. In the sync route, make phone nullable in the INSERT:
   The current schema has phone VARCHAR(20) UNIQUE NOT NULL — this is the constraint that fails.
   
   Add a fallback: if phoneNum is null, use uid + '@placeholder.phone' temporarily, or skip phone in INSERT
   and update it on next sync.
   
   Better fix: Alter the schema to allow null phone on initial sync:
   In schema.sql, change: phone VARCHAR(20) UNIQUE NOT NULL  →  phone VARCHAR(20) UNIQUE
   Then run: ALTER TABLE users ALTER COLUMN phone DROP NOT NULL;
   on your VPS PostgreSQL instance.

2. In the sync route handler, if phone is null:
   - Do the INSERT without phone (let it be null)
   - Log: console.log(`[SYNC] uid=${uid} — phone is null on first sync, will update on next call`);
   - Return success anyway

Android fix:
3. In BodaViewModel.kt, find syncUserToBackend():
   - After verifyOtp() succeeds, wait for FirebaseAuth.getInstance().currentUser?.phoneNumber to be populated
   - If phoneNumber is null after OTP, delay the sync by 1 second and retry once:
     delay(1000)
     val phone = FirebaseAuth.getInstance().currentUser?.phoneNumber
   - Pass this phone value to the sync call

Verify: 
   Fresh install → OTP → check PostgreSQL: SELECT uid, phone FROM users WHERE uid = '<new_uid>';
   Confirm the row exists (phone may be null on first sync but row must exist).
   Reopen app → confirm phone is populated on second sync.
```

---

## PROMPT 7.1 — Android: Create DriverHomeScreen.kt

```
Task: Extract the driver home screen from BodaScreens.kt.

Location: app/src/main/java/com/example/ui/driver/DriverHomeScreen.kt

Steps:
1. Create ui/driver/ directory
2. Create DriverHomeScreen.kt:
   - Package: com.example.ui.driver
   - Move:
     * fun DriverHomeScreen(viewModel: BodaViewModel) — ~379 lines
     * fun DriverMiniStat(title: String, value: String, icon: ImageVector) — ~15 lines
3. In BodaScreens.kt and AppNavigation.kt: add imports + // MOVED TO: comments

Verify: ./gradlew assembleDebug. Switch to driver mode — driver home with online/offline toggle appears.
```

---

## PROMPT 7.2 — Android: Create DriverOnboardingScreen.kt

```
Task: Extract driver onboarding (registration + doc upload flow) from BodaScreens.kt.

Location: app/src/main/java/com/example/ui/driver/DriverOnboardingScreen.kt

Steps:
1. Create DriverOnboardingScreen.kt:
   - Package: com.example.ui.driver
   - Move: fun DriverOnboardingScreen(viewModel: BodaViewModel) — ~433 lines
2. In BodaScreens.kt and AppNavigation.kt: add imports + // MOVED TO: comments

Verify: ./gradlew assembleDebug. Trigger driver registration flow — steps appear correctly.
```

---

## PROMPT 7.3 — Android: Create DriverViewModel.kt

```
Task: Group driver-related functions with MOVED TO comments.

Location: app/src/main/java/com/example/ui/driver/DriverViewModel.kt

Follow extension file pattern. Add // MOVED TO: comments in BodaViewModel.kt for:
- registerDriverViaBackend
- updateDriverStatusViaBackend
- startDriverOnboarding / simulateDocUpload / completeDriverOnboarding
- toggleDriverOnline
- driverAcceptTrip / driverRejectTrip / driverArrivePickup / driverStartTrip / driverCompleteTrip
- updateDriverLocation

Verify: ./gradlew assembleDebug passes.
```

---

## PROMPT 8.1 — Android: Create WalletScreen.kt and WalletViewModel.kt

```
Task: Extract wallet screen and ViewModel.

Steps:
1. Create app/src/main/java/com/example/ui/wallet/WalletScreen.kt:
   - Move: fun WalletScreen(viewModel: BodaViewModel, balance: Double, txns: List<WalletTransaction>) — ~163 lines
   
2. Create app/src/main/java/com/example/ui/wallet/WalletViewModel.kt:
   - Extension file pattern
   - // MOVED TO: comments for startWalletTopup, confirmWalletTopupWithPin

3. In BodaScreens.kt and AppNavigation.kt: add imports + // MOVED TO: comments

Verify: ./gradlew assembleDebug. Navigate to wallet — balance and transaction history appear.
```

---

## PROMPT 8.2 — Android: Create profile/ screens and ProfileViewModel.kt

```
Task: Extract all profile-related screens.

Steps:
1. Create ui/profile/ directory
2. Create the following files (one composable each):
   - ProfileSettingsScreen.kt — fun ProfileSettingsScreen(viewModel, user, contacts) — ~507 lines
   - EmergencyContactsScreen.kt — fun EmergencyContactsScreen(viewModel, contacts) — ~71 lines
   - SavedPlacesManageScreen.kt — fun SavedPlacesManageScreen(viewModel, savedPlaces) — ~70 lines
   - SupportScreen.kt — fun SupportScreen(viewModel) — ~126 lines

3. Create ProfileViewModel.kt with // MOVED TO: comments for:
   saveUserProfile, addSavedPlace, removeSavedPlace, addEmergencyContact, removeEmergencyContact,
   submitSupportTicket, sendSupportChatMessage

4. In BodaScreens.kt and AppNavigation.kt: add imports + comments for all

Verify: ./gradlew assembleDebug. Navigate to Profile → confirm settings, contacts, and saved places all load.
```

---

## PROMPT 8.3 — Android: Create chat/ files

```
Task: Extract in-trip chat overlay.

Steps:
1. Create ui/chat/ directory
2. Create app/src/main/java/com/example/ui/chat/RiderChatOverlay.kt:
   - Move: fun RiderChatOverlay(viewModel: BodaViewModel) — ~183 lines
3. Create ChatViewModel.kt with // MOVED TO: comments for:
   openRiderChat, sendRiderChatMessage, onRiderChatInputChanged, initiateCall, endActiveCall

4. In BodaScreens.kt and AppNavigation.kt: add imports + comments

Verify: ./gradlew assembleDebug. During an active trip, open chat — messages send and appear.
```

---

## PROMPT 8.4 — Android: Create referrals/ files

```
Task: Extract referrals screen.

Steps:
1. Create ui/referrals/ directory
2. Create app/src/main/java/com/example/ui/referrals/ReferralsScreen.kt:
   - Move: fun ReferralsScreen(viewModel: BodaViewModel, referrals: List<Referral>) — ~84 lines
3. Create ReferralsViewModel.kt with // MOVED TO: comments for:
   simulateNewReferralSignUp, simulateReferralFirstTripCompletion, dismissWelcomeBonus

4. In BodaScreens.kt and AppNavigation.kt: add imports + comments

Verify: ./gradlew assembleDebug. Open referrals screen — list and share button appear.
```

---

## PROMPT 8.5 — Android: Create offline/ files

```
Task: Extract offline SMS booking overlay.

Steps:
1. Create ui/offline/ directory
2. Create app/src/main/java/com/example/ui/offline/OfflineSMSBookingOverlay.kt:
   - Move: fun OfflineSMSBookingOverlay(viewModel: BodaViewModel) — ~96 lines
3. Create OfflineViewModel.kt with // MOVED TO: comments for:
   triggerOfflineSMSBookingFlow, confirmOfflineSMSBooking, dispatchSOSSMS

4. In BodaScreens.kt and AppNavigation.kt: add imports + comments

Verify: ./gradlew assembleDebug passes.
```

---

## PROMPT 9.1 — Android: Clean up BodaScreens.kt

```
Task: BodaScreens.kt should now contain ONLY functions that have not been moved yet.
Delete every function that has a // MOVED TO: comment.

Steps:
1. Open BodaScreens.kt
2. Find every block that starts with // MOVED TO: and delete:
   - The comment line
   - The @Composable annotation (if present)
   - The entire function body from fun ... { to its closing }
3. Remove imports that are no longer needed in BodaScreens.kt (Android Studio will flag these)
4. The file should end up with only:
   - The package declaration
   - The import block (trimmed)
   - Any functions that were NOT moved (check the list — should be none at this point)
5. If BodaScreens.kt is now empty (just package + imports), delete the file.

Verify: ./gradlew assembleDebug passes. Run full app — all screens accessible.
```

---

## PROMPT 9.2 — Android: Clean up BodaViewModel.kt

```
Task: BodaViewModel.kt should become a thin coordinator — remove delegation stubs and move remaining StateFlow definitions to be owned by feature ViewModels.

Steps:
1. Open BodaViewModel.kt
2. Find every function with // MOVED TO: comment — delete those stubs
3. What should REMAIN in BodaViewModel.kt:
   - All StateFlow / MutableStateFlow declarations (they are referenced by name across many composables — leave them here until a full state refactor in a future task)
   - The Room database initialization in init {}
   - The BodaMessagingService hook
   - Any function that genuinely coordinates across multiple features
4. Remove imports that are no longer needed
5. The file should be under 400 lines when done

Verify: ./gradlew assembleDebug passes. Run all major flows: book a ride, check wallet, view profile.
```

---

## PROMPT 9.3 — Restructure admin dashboard

```
Task: Move admin-dashboard/ files into apps/admin/ with a page-based structure.

Steps:
1. Create apps/admin/src/pages/ directory
2. The current admin-dashboard/app.js is ~376 lines — split it:
   - Extract dashboard stats section → pages/dashboard.js
   - Extract drivers section → pages/drivers.js
   - Extract riders section → pages/riders.js
   - Extract trips section → pages/trips.js
   - Extract pricing section → pages/pricing.js
   - Extract SOS section → pages/sos.js
3. Create apps/admin/src/components/:
   - sidebar.js — the navigation sidebar
   - statsCard.js — the metrics card component
   - liveMap.js — the driver map component
4. Update apps/admin/index.html to reference new paths
5. Update backend/server.js static serving path:
   app.use('/admin', express.static(path.join(__dirname, '../admin/src')));

Verify: Open the admin dashboard in a browser — stats, driver map, and trip table all load.
```

---

## PROMPT 9.4 — Final monorepo folder move

```
Task: Move the three apps into the apps/ directory to complete the monorepo structure.

Steps:
1. Create apps/ directory at repo root
2. Move backend/ → apps/backend/
   - Update any relative path references inside backend files (../db → ../../db etc.)
   - Update docker-compose.yml context paths
   - Update nginx conf if it references file paths
3. The Android app (app/) stays at its current path — Android projects require the module to be
   in the root or explicitly declared in settings.gradle.kts. Instead of moving it:
   - Rename the directory from app/ to android/ (optional)
   - Update settings.gradle.kts: include(":android") and project(":android").projectDir = File("android")
4. Move admin-dashboard/ (already done in 9.3) → apps/admin/
5. Update .gitignore at root if needed

Verify: 
   ./gradlew assembleDebug from repo root — Android builds.
   node apps/backend/src/server.js — backend starts.
   Admin dashboard loads.
```

---

## PROMPT 9.5 — Final end-to-end smoke test

```
Task: This is not a code change — it is a full integration verification.

Run through this exact sequence and confirm each step works:

PASSENGER FLOW:
1. Fresh install on Android device
2. Splash screen appears → transitions to onboarding
3. Complete phone OTP login
4. Relaunch app → goes directly to home screen (NOT back to OTP)
5. Check PostgreSQL: SELECT * FROM users WHERE uid = '<your_uid>'; → row exists with phone populated
6. Search for a destination in Gulu
7. Route preview shows with fare from backend
8. Apply a promo code → discount applied
9. Book ride → matching screen appears
10. From admin dashboard → confirm trip appears in active trips

DRIVER FLOW:
11. Go online as driver
12. New trip request appears on driver home
13. Accept trip → trip status in DB changes to en_route
14. Start trip → status = active
15. Complete trip → check DB: SELECT earnings, completed_trips FROM drivers WHERE uid = '<driver_uid>';
    → earnings increased by fare amount ✓

REFERRAL FLOW:
16. Create a referral
17. Call POST /api/referrals/:id/complete
18. Check referrer wallet_balance in DB → increased by reward_amount ✓

ADMIN:
19. Admin dashboard stats update
20. Driver map shows driver location
21. Resolve an SOS alert

If all 21 steps pass → refactor is complete. Commit: git commit -m "feat: complete monorepo restructure and bug fixes"
```

---
# END OF PROMPTS
# Total: 40 prompts across 9 phases
# Read the context block at the top of every new Claude Code session.
