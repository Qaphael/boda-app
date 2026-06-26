package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.launch

class BodaMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val prefs = getSharedPreferences("boda_gulu_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
        sendTokenToBackend(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        createNotificationChannel()

        val title = message.notification?.title ?: message.data["title"] ?: "Boda Gulu"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val tripId = message.data["trip_id"]

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            tripId?.let { putExtra("trip_id", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.boda_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Boda Gulu Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Trip updates, driver arrivals, and ride alerts"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun sendTokenToBackend(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = okhttp3.OkHttpClient()
                val json = org.json.JSONObject().put("fcm_token", token)
                val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val request = okhttp3.Request.Builder()
                    .url("${ApiClient.getBaseUrl()}/api/users/fcm-token")
                    .post(body)
                    .build()
                client.newCall(request).execute()
            } catch (_: Exception) { }
        }
    }

    companion object {
        private const val CHANNEL_ID = "boda_gulu_alerts"
    }
}
