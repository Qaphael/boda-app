package com.example.data

import retrofit2.Response
import retrofit2.http.*

// API Service interface for PostgreSQL backend
interface BodaApiService {

    // User endpoints
    @POST("api/users/sync")
    suspend fun syncUser(@Body user: UserSyncRequest): Response<ApiResponse<UserProfile>>

    @GET("api/users/me")
    suspend fun getMe(): Response<UserMeResponse>

    @GET("api/saved-places")
    suspend fun getSavedPlaces(): Response<List<SavedPlace>>

    @POST("api/saved-places")
    suspend fun savePlace(@Body place: SavePlaceRequest): Response<ApiResponse<SavedPlace>>

    // Trip endpoints
    @POST("api/trips/book")
    suspend fun bookTrip(@Body trip: TripBookingRequest): Response<ApiResponse<Trip>>

    @GET("api/trips")
    suspend fun getTrips(): Response<List<TripDto>>

    @GET("api/trips/{id}")
    suspend fun getTrip(@Path("id") tripId: Int): Response<Trip>

    @PATCH("api/trips/{id}/status")
    suspend fun updateTripStatus(@Path("id") tripId: Int, @Body body: TripStatusUpdate): Response<ApiResponse<Trip>>

    @PATCH("api/trips/{id}/claim")
    suspend fun claimTrip(@Path("id") tripId: Int): Response<ApiResponse<Trip>>

    @GET("api/trips/{id}/messages")
    suspend fun getChatMessages(@Path("id") tripId: Int): Response<List<ChatMessageDto>>

    // Wallet endpoints
    @GET("api/wallet/transactions")
    suspend fun getWalletTransactions(): Response<List<WalletTransactionDto>>

    @POST("api/wallet/topup")
    suspend fun walletTopup(@Body body: WalletTopupRequest): Response<TopupResponse>

    @POST("api/wallet/pay")
    suspend fun walletPay(@Body body: WalletTopupRequest): Response<TopupResponse>

    // Emergency contacts endpoints
    @GET("api/emergency-contacts")
    suspend fun getEmergencyContacts(): Response<List<EmergencyContactDto>>

    @POST("api/emergency-contacts")
    suspend fun addEmergencyContact(@Body body: EmergencyContactRequest): Response<ApiResponse<EmergencyContactDto>>

    @DELETE("api/emergency-contacts/{id}")
    suspend fun deleteEmergencyContact(@Path("id") contactId: Int): Response<ApiResponse<Any>>

    // Saved places endpoints
    @DELETE("api/saved-places/{id}")
    suspend fun deleteSavedPlace(@Path("id") placeId: Int): Response<ApiResponse<Any>>

    // Referrals endpoints
    @GET("api/referrals")
    suspend fun getReferrals(): Response<List<ReferralDto>>

    @POST("api/referrals")
    suspend fun addReferral(@Body body: ReferralRequest): Response<ApiResponse<ReferralDto>>

    @POST("api/referrals/{id}/complete")
    suspend fun completeReferral(@Path("id") referralId: Int): Response<ApiResponse<Any>>

    // Driver endpoints
    @POST("api/drivers/register")
    suspend fun registerDriver(@Body driver: DriverRegistrationRequest): Response<ApiResponse<DriverProfile>>

    @POST("api/drivers/status")
    suspend fun updateDriverStatus(@Body status: DriverStatusUpdate): Response<ApiResponse<DriverProfile>>

    // Fare calculation
    @POST("api/trips/calculate-fare")
    suspend fun calculateFare(@Body request: FareCalculationRequest): Response<FareCalculationResponse>

    // Promo endpoints
    @POST("api/promos/validate")
    suspend fun validatePromo(@Body promo: PromoValidationRequest): Response<PromoValidationResponse>
}

// Request/Response data classes
data class UserSyncRequest(
    val phone: String,
    val name: String,
    val email: String? = null
)

data class UserMeResponse(
    val uid: String = "",
    val phone: String = "",
    val full_name: String = "",
    val wallet_balance: Double = 0.0,
    val language: String = "en",
    val referral_code: String = ""
)

data class ApiResponse<T>(
    val success: Boolean,
    val user: T? = null,
    val trip: T? = null,
    val driver: T? = null,
    val savedPlace: T? = null,
    val contact: T? = null,
    val referral: T? = null,
    val error: String? = null
)

data class SavePlaceRequest(
    val label: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
)

data class TripBookingRequest(
    val pickup_name: String,
    val pickup_lat: Double,
    val pickup_lon: Double,
    val dropoff_name: String,
    val dropoff_lat: Double,
    val dropoff_lon: Double,
    val distance_km: Double,
    val duration_mins: Int,
    val fare: Double,
    val promo_applied: String? = null,
    val payment_method: String
)

data class DriverRegistrationRequest(
    val uid: String,
    val full_name: String,
    val phone: String,
    val plate_number: String,
    val helmet_number: String
)

data class DriverStatusUpdate(
    val uid: String,
    val is_online: Boolean,
    val latitude: Double,
    val longitude: Double
)

data class DriverProfile(
    val uid: String = "",
    val full_name: String = "",
    val phone: String = "",
    val plate_number: String = "",
    val helmet_number: String = "",
    val is_online: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val rating: Double = 5.0,
    val completed_trips: Int = 0,
    val earnings: Double = 0.0
)

data class FareCalculationRequest(
    val distance_km: Double,
    val duration_mins: Int
)

data class FareCalculationResponse(
    val base_fare: Int,
    val distance_km: Double,
    val duration_mins: Int,
    val surge_multiplier: Double,
    val surge_reason: String,
    val original_fare: Int,
    val final_fare: Int
)

data class PromoValidationRequest(
    val code: String,
    val original_fare: Double
)

data class PromoValidationResponse(
    val valid: Boolean,
    val code: String? = null,
    val discount_amount: Int = 0,
    val final_fare: Double = 0.0,
    val message: String? = null
)

data class ChatMessageDto(
    val id: Int = 0,
    val trip_id: Int = 0,
    val sender_uid: String = "",
    val sender_name: String = "",
    val sender_role: String = "",
    val message: String = "",
    val created_at: String = ""
)

data class TripStatusUpdate(
    val status: String,
    val rating: Int? = null,
    val comment: String? = null,
    val driver_uid: String? = null,
    val dispute_reason: String? = null,
    val dispute_evidence: String? = null
)

data class TripDto(
    val id: Int = 0,
    val trip_code: String = "",
    val passenger_uid: String = "",
    val driver_uid: String? = null,
    val driver_name: String? = null,
    val plate_number: String? = null,
    val pickup_name: String = "",
    val pickup_lat: Double = 0.0,
    val pickup_lon: Double = 0.0,
    val dropoff_name: String = "",
    val dropoff_lat: Double = 0.0,
    val dropoff_lon: Double = 0.0,
    val distance_km: Double = 0.0,
    val duration_mins: Int = 0,
    val fare: Double = 0.0,
    val payment_method: String = "",
    val status: String = "",
    val rating: Int = 0,
    val comment: String? = null,
    val created_at: String = "",
    val completed_at: String? = null
)

data class WalletTransactionDto(
    val id: Int = 0,
    val user_uid: String = "",
    val transaction_ref: String = "",
    val type: String = "",
    val amount: Double = 0.0,
    val payment_provider: String = "",
    val status: String = "",
    val created_at: String = ""
)

data class WalletTopupRequest(
    val amount: Double,
    val payment_provider: String = "MTN"
)

data class TopupResponse(
    val success: Boolean = false,
    val reference: String = ""
)

data class EmergencyContactDto(
    val id: Int = 0,
    val user_uid: String = "",
    val name: String = "",
    val phone_number: String = "",
    val created_at: String = ""
)

data class EmergencyContactRequest(
    val name: String,
    val phone_number: String
)

data class ReferralDto(
    val id: Int = 0,
    val referrer_uid: String = "",
    val referred_name: String = "",
    val referred_phone: String = "",
    val referral_code: String = "",
    val status: String = "",
    val reward_amount: Double = 0.0,
    val created_at: String = ""
)

data class ReferralRequest(
    val referred_name: String,
    val referred_phone: String,
    val referral_code: String
)
