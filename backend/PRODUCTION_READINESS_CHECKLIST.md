# SafeBoda Gulu | Production-Ready Connection Checklist

This checklist details the exact sequential actions required to transition your SafeBoda Gulu system from a **high-fidelity standalone simulation** to a **fully integrated production environment**. 

---

## 🛠️ Phase 1: VPS & Database Setup

- [ ] **1. Deploy PostgreSQL Database Schema**
  * SSH into your Ubuntu VPS.
  * Initialize the database (`boda_gulu`) and secure user credentials as outlined in the `DEPLOYMENT_GUIDE.md`.
  * Import `/backend/schema.sql` to generate the 5 foundational tables (`users`, `drivers`, `saved_places`, `trips`, `transactions`).
  ```bash
  psql -h localhost -U boda_admin -d boda_gulu -f /path/to/backend/schema.sql
  ```

- [ ] **2. Verify Node.js Environment & Run Backend**
  * Navigate to the `/backend` directory on your server.
  * Install dependencies: `npm install --production`.
  * Copy `.env.example` to `.env` and fill in your actual PostgreSQL credentials.
  * Start the service using PM2 to guarantee 24/7 uptime:
  ```bash
  pm2 start server.js --name "boda-gulu-backend"
  ```

- [ ] **3. Setup Firebase Auth & JWT Middleware**
  * Generate a service account private key in your **Firebase Console** -> **Project Settings** -> **Service Accounts**.
  * Securely upload the JSON file to your VPS (e.g., `/var/www/boda-gulu-backend/firebase-key.json`).
  * Ensure the path is correctly set in your `.env` file (`FIREBASE_SERVICE_ACCOUNT_PATH`).

---

## 🌐 Phase 2: Deploying the Admin Dashboard

- [ ] **1. Review `API_BASE` in `app.js`**
  * Currently, the dashboard uses `window.location.origin` for the `API_BASE` variable. This is perfect if Nginx serves both your API and your static HTML on the same domain (e.g. under `api.yourdomain.com` and `api.yourdomain.com/admin/`).
  * If you plan to host the static files on a separate domain (e.g. `admin.yourdomain.com`), open `/admin-dashboard/app.js` and change:
  ```javascript
  const API_BASE = "https://api.yourdomain.com"; // Your live backend URL
  ```

- [ ] **2. Upload Static Assets**
  * Upload the entire `/admin-dashboard` directory contents (HTML, CSS, JS, assets) to your VPS's static serving folder:
  ```bash
  mkdir -p /var/www/boda-gulu-admin/
  cp -r admin-dashboard/* /var/www/boda-gulu-admin/
  ```

- [ ] **3. Configure Nginx Reverse Proxy with SSL**
  * Implement the reverse proxy configuration block provided in the `DEPLOYMENT_GUIDE.md` under `/etc/nginx/sites-available/boda-gulu`.
  * Enable the virtual host and reload Nginx:
  ```bash
  sudo ln -s /etc/nginx/sites-available/boda-gulu /etc/nginx/sites-enabled/
  sudo nginx -t
  sudo systemctl reload nginx
  ```
  * Run **Certbot** to acquire free, auto-renewing Let's Encrypt SSL certificates (highly recommended for `https://` and secure WebSocket `wss://` requests).
  ```bash
  sudo certbot --nginx -d yourdomain.com
  ```

---

## 📱 Phase 3: Android App Integration

- [ ] **1. Firebase Services Configuration**
  * Download the `google-services.json` config file for your app from your Firebase console.
  * Copy the file into your Android module's `app/` folder.

- [ ] **2. Link JWT Auth Token in Network Headers**
  * When your Android app makes an HTTP request to your backend endpoints, you **must** fetch the user's current Firebase token and attach it as a Bearer token in the request header:
  ```kotlin
  // Add an OkHttpClient Interceptor to automatically append the Firebase JWT
  val client = OkHttpClient.Builder()
      .addInterceptor { chain ->
          val token = getCurrentFirebaseUserToken() // Fetch via FirebaseUser.getIdToken()
          val newRequest = chain.request().newBuilder()
              .addHeader("Authorization", "Bearer $token")
              .build()
          chain.proceed(newRequest)
      }
      .build()
  ```

- [ ] **3. Point Retrofit to Production API**
  * In your Android Kotlin code, change the Retrofit client base URL to point to your live Nginx public API subdomain:
  ```kotlin
  const val BACKEND_URL = "https://api.yourdomain.com/"
  ```

- [ ] **4. Configure Socket.io Client for Live Map Tracking**
  * Connect your Android driver app (or simulated GPS broadcaster) to your Socket.io WebSocket server.
  * Join the specific trip channel by emitting `join_trip_channel` with the current `tripId`.
  * Regularly emit GPS updates using `update_live_gps`:
  ```json
  {
    "tripId": 12,
    "latitude": 2.7712,
    "longitude": 32.2985,
    "bearing": 180.0,
    "speed": 12.5
  }
  ```

---

## 🧪 Phase 4: Verification & Live Launch

- [ ] **1. Confirm Endpoint Integrations**
  * Verify you can register a driver in the Admin panel and see it instantly inserted into the database's `drivers` table.
  * Verify toggling status changes `is_online` in PostgreSQL.
  * Verify adding credit updates `wallet_balance` in the `users` table and writes a ledger record into `transactions`.
- [ ] **2. Audit the Console Logs**
  * Monitor active queries on the backend by running:
  ```bash
  pm2 logs boda-gulu-backend
  ```
- [ ] **3. Conduct Live Walk-Through**
  * Book a trip on the Android client.
  * Watch the dispatch record populate instantly inside the **Dispatch Ledger** tab of the Admin Dashboard.
  * Track the driver's real-time movement as they navigate across the live Leaflet map.
