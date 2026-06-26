package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val phoneNumber: String = "",
    val language: String = "en", // "en" (English), "ach" (Acholi), "luo" (Luo)
    val isSetupComplete: Boolean = false,
    val profileImageResId: Int = 1, // 1 to 4 for simple avatar choices
    val referralCode: String = ""
)

@Entity(tableName = "referrals")
data class Referral(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val referredName: String,
    val referredPhone: String,
    val referralCodeUsed: String,
    val status: String, // "pending", "completed"
    val timestamp: Long = System.currentTimeMillis(),
    val rewardAmount: Double = 3000.0
)

@Entity(tableName = "saved_places")
data class SavedPlace(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String, // Home, Work, Market, Church, Custom
    val name: String,
    val latitude: Double,
    val longitude: Double
)

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "ride" or "delivery"
    val pickupName: String,
    val dropoffName: String,
    val fare: Double,
    val paymentMethod: String, // "MTN", "Airtel", "Wallet"
    val status: String, // "searching", "matched", "completed", "canceled", "disputed"
    val timestamp: Long = System.currentTimeMillis(),
    val riderName: String,
    val riderPlate: String,
    val riderPhone: String = "+256 772 123456",
    val riderPhotoResId: Int = 1,
    val packageDetails: String? = null,
    val recipientName: String? = null,
    val recipientPhone: String? = null,
    val rating: Int = 0, // 1 to 5, 0 means unrated
    val comment: String? = null,
    val disputeReason: String? = null,
    val disputeEvidence: String? = null
)

@Entity(tableName = "wallet_transactions")
data class WalletTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String, // "topup", "payment"
    val status: String, // "pending", "completed", "failed"
    val phoneNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val provider: String, // "MTN" or "Airtel"
    val reference: String
)

@Entity(tableName = "emergency_contacts")
data class EmergencyContact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phoneNumber: String
)

// Ride request model (used for in-app state tracking)
data class RideRequest(
    val id: String = "",
    val riderId: String = "",
    val riderName: String = "",
    val riderPhone: String = "",
    val pickupName: String = "",
    val pickupLat: Double = 0.0,
    val pickupLng: Double = 0.0,
    val dropoffName: String = "",
    val dropoffLat: Double = 0.0,
    val dropoffLng: Double = 0.0,
    val serviceType: String = "ride", // "ride" or "delivery"
    val fare: Double = 0.0,
    val paymentMethod: String = "MTN",
    val status: String = "pending", // "pending", "accepted", "arriving", "in_progress", "completed", "canceled"
    val timestamp: Long = System.currentTimeMillis(),
    val driverId: String? = null,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val driverPlate: String? = null,
    val driverLat: Double? = null,
    val driverLng: Double? = null,
    val etaMinutes: Int? = null
)

data class DriverLocation(
    val driverId: String = "",
    val driverName: String = "",
    val driverPhone: String = "",
    val driverPlate: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isOnline: Boolean = false,
    val isOnTrip: Boolean = false,
    val currentRideId: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)
