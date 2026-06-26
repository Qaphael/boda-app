package com.example.data

import retrofit2.Response
import retrofit2.http.*

// API Service interface for PostgreSQL backend
interface BodaApiService {

    // User endpoints
    @POST("api/users/sync")
    suspend fun syncUser(@Body user: UserSyncRequest): Response<ApiResponse<UserProfile>>

    @GET("api/saved-places")
    suspend fun getSavedPlaces(): Response<List<SavedPlace>>

    @POST("api/saved-places")
    suspend fun savePlace(@Body place: SavePlaceRequest): Response<ApiResponse<SavedPlace>>

    // Trip endpoints
    @POST("api/trips/book")
    suspend fun bookTrip(@Body trip: TripBookingRequest): Response<ApiResponse<Trip>>

    @GET("api/trips/{id}")
    suspend fun getTrip(@Path("id") tripId: Int): Response<Trip>

    @GET("api/trips/{id}/messages")
    suspend fun getChatMessages(@Path("id") tripId: Int): Response<List<ChatMessageDto>>

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

data class ApiResponse<T>(
    val success: Boolean,
    val user: T? = null,
    val trip: T? = null,
    val driver: T? = null,
    val savedPlace: T? = null,
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
