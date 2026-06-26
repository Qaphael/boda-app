package com.example.data

import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject

class WebSocketClient(private val serverUrl: String) {
    private var socket: Socket? = null
    private var tripId: String? = null
    
    // Callbacks
    var onNewTripRequest: ((Trip) -> Unit)? = null
    var onDriverLocationUpdate: ((DriverLocation) -> Unit)? = null
    var onLiveGpsBroadcast: ((GpsUpdate) -> Unit)? = null
    var onPricingUpdate: ((PricingSettings) -> Unit)? = null
    var onSosAlert: ((String) -> Unit)? = null
    var onConnectionChange: ((Boolean) -> Unit)? = null
    var onChatMessage: ((ChatWsMessage) -> Unit)? = null
    var onChatTyping: ((ChatTypingEvent) -> Unit)? = null
    
    fun connect() {
        try {
            val options = IO.Options.builder()
                .setReconnection(true)
                .setReconnectionAttempts(10)
                .setReconnectionDelay(2000)
                .build()
            
            socket = IO.socket(serverUrl, options)
            
            socket?.on(Socket.EVENT_CONNECT) {
                onConnectionChange?.invoke(true)
            }
            
            socket?.on(Socket.EVENT_DISCONNECT) {
                onConnectionChange?.invoke(false)
            }
            
            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                onConnectionChange?.invoke(false)
            }
            
            socket?.on("new_trip_request") { args ->
                try {
                    val data = args[0] as JSONObject
                    val trip = Trip(
                        id = data.optInt("id", 0),
                        type = data.optString("service_type", "ride"),
                        pickupName = data.optString("pickup_name", ""),
                        dropoffName = data.optString("dropoff_name", ""),
                        fare = data.optDouble("fare", 0.0),
                        paymentMethod = data.optString("payment_method", "MTN"),
                        status = data.optString("status", "matching"),
                        riderName = data.optString("passenger_name", "Passenger"),
                        riderPlate = ""
                    )
                    onNewTripRequest?.invoke(trip)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            socket?.on("driver_location_update") { args ->
                try {
                    val data = args[0] as JSONObject
                    val location = DriverLocation(
                        driverId = data.optString("uid", ""),
                        latitude = data.optDouble("latitude", 0.0),
                        longitude = data.optDouble("longitude", 0.0),
                        isOnline = data.optBoolean("is_online", false)
                    )
                    onDriverLocationUpdate?.invoke(location)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            socket?.on("live_gps_broadcast") { args ->
                try {
                    val data = args[0] as JSONObject
                    val update = GpsUpdate(
                        tripId = data.optString("tripId", ""),
                        latitude = data.optDouble("latitude", 0.0),
                        longitude = data.optDouble("longitude", 0.0),
                        bearing = data.optDouble("bearing", 0.0).toFloat(),
                        speed = data.optDouble("speed", 0.0).toFloat()
                    )
                    onLiveGpsBroadcast?.invoke(update)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            socket?.on("pricing_rules_updated") { args ->
                try {
                    val data = args[0] as JSONObject
                    val pricing = PricingSettings(
                        baseFare = data.optInt("base_fare", 1500),
                        ratePerKm = data.optInt("rate_per_km", 1000),
                        ratePerMin = data.optInt("rate_per_min", 150),
                        surgeMultiplier = data.optDouble("surge_multiplier", 1.0),
                        surgeReason = data.optString("surge_reason", "Normal")
                    )
                    onPricingUpdate?.invoke(pricing)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            socket?.on("sos_alert_resolved") { args ->
                val alertId = args[0] as? String
                if (alertId != null) {
                    onSosAlert?.invoke(alertId)
                }
            }

            socket?.on("chat_message") { args ->
                try {
                    val data = args[0] as JSONObject
                    val msg = ChatWsMessage(
                        id = data.optInt("id", 0),
                        tripId = data.optInt("tripId", data.optInt("trip_id", 0)),
                        senderUid = data.optString("senderUid", data.optString("sender_uid", "")),
                        senderName = data.optString("senderName", data.optString("sender_name", "")),
                        senderRole = data.optString("senderRole", data.optString("sender_role", "")),
                        message = data.optString("message", ""),
                        createdAt = data.optString("createdAt", data.optString("created_at", ""))
                    )
                    onChatMessage?.invoke(msg)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            socket?.on("chat_typing") { args ->
                try {
                    val data = args[0] as JSONObject
                    val typing = ChatTypingEvent(
                        tripId = data.optInt("tripId", 0),
                        senderUid = data.optString("senderUid", ""),
                        senderName = data.optString("senderName", ""),
                        isTyping = data.optBoolean("isTyping", false)
                    )
                    onChatTyping?.invoke(typing)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            socket?.connect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun joinTripChannel(tripId: String) {
        this.tripId = tripId
        socket?.emit("join_trip_channel", tripId)
    }
    
    fun updateLiveGps(latitude: Double, longitude: Double, bearing: Float, speed: Float) {
        val data = JSONObject().apply {
            put("tripId", tripId)
            put("latitude", latitude)
            put("longitude", longitude)
            put("bearing", bearing.toDouble())
            put("speed", speed.toDouble())
        }
        socket?.emit("update_live_gps", data)
    }

    fun sendChatMessage(tripId: Int, senderUid: String, senderName: String, senderRole: String, message: String) {
        val data = JSONObject().apply {
            put("tripId", tripId)
            put("senderUid", senderUid)
            put("senderName", senderName)
            put("senderRole", senderRole)
            put("message", message)
        }
        socket?.emit("chat_message", data)
    }

    fun sendTypingIndicator(tripId: Int, senderUid: String, senderName: String, isTyping: Boolean) {
        val data = JSONObject().apply {
            put("tripId", tripId)
            put("senderUid", senderUid)
            put("senderName", senderName)
            put("isTyping", isTyping)
        }
        socket?.emit("chat_typing", data)
    }
    
    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
    }
    
    fun isConnected(): Boolean = socket?.connected() == true
}

// Data classes for WebSocket events
data class GpsUpdate(
    val tripId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val bearing: Float = 0f,
    val speed: Float = 0f
)

data class PricingSettings(
    val baseFare: Int = 1500,
    val ratePerKm: Int = 1000,
    val ratePerMin: Int = 150,
    val surgeMultiplier: Double = 1.0,
    val surgeReason: String = "Normal"
)

data class ChatWsMessage(
    val id: Int = 0,
    val tripId: Int = 0,
    val senderUid: String = "",
    val senderName: String = "",
    val senderRole: String = "",
    val message: String = "",
    val createdAt: String = ""
)

data class ChatTypingEvent(
    val tripId: Int = 0,
    val senderUid: String = "",
    val senderName: String = "",
    val isTyping: Boolean = false
)
