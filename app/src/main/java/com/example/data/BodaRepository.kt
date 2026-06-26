package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class BodaRepository(private val dao: BodaDao? = null) {
    private val api = ApiClient.api

    // ===================== Room / DAO Operations =====================

    val userProfile: Flow<UserProfile?> get() = dao?.getUserProfile() ?: throw IllegalStateException("No DAO")
    val savedPlaces: Flow<List<SavedPlace>> get() = dao?.getSavedPlaces() ?: throw IllegalStateException("No DAO")
    val trips: Flow<List<Trip>> get() = dao?.getTrips() ?: throw IllegalStateException("No DAO")
    val transactions: Flow<List<WalletTransaction>> get() = dao?.getTransactions() ?: throw IllegalStateException("No DAO")
    val emergencyContacts: Flow<List<EmergencyContact>> get() = dao?.getEmergencyContacts() ?: throw IllegalStateException("No DAO")
    val referrals: Flow<List<Referral>> get() = dao?.getReferrals() ?: throw IllegalStateException("No DAO")

    suspend fun initializeDefaultData() {
        dao ?: return
        if (dao.getUserProfile().firstOrNull() == null) {
            dao.insertUserProfile(UserProfile(name = "Gulu Rider"))
        }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        dao?.insertUserProfile(profile)
    }

    suspend fun addTrip(trip: Trip): Long {
        return dao?.insertTrip(trip) ?: 0L
    }

    suspend fun updateTrip(trip: Trip) {
        dao?.updateTrip(trip)
    }

    suspend fun addTransaction(txn: WalletTransaction) {
        dao?.insertTransaction(txn)
    }

    suspend fun addSavedPlace(place: SavedPlace) {
        dao?.insertSavedPlace(place)
    }

    suspend fun removeSavedPlace(place: SavedPlace) {
        dao?.deleteSavedPlace(place)
    }

    suspend fun addEmergencyContact(contact: EmergencyContact) {
        dao?.insertEmergencyContact(contact)
    }

    suspend fun removeEmergencyContact(contact: EmergencyContact) {
        dao?.deleteEmergencyContact(contact)
    }

    suspend fun addReferral(referral: Referral): Long {
        return dao?.insertReferral(referral) ?: 0L
    }

    suspend fun getReferralByPhone(phone: String): Referral? {
        return dao?.getReferralByPhone(phone)
    }

    // ===================== Backend API Operations =====================

    suspend fun syncUser(phone: String, name: String, email: String? = null): Result<UserProfile> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.syncUser(UserSyncRequest(phone, name, email))
                if (response.isSuccessful && response.body()?.success == true) {
                    val user = response.body()?.user
                    if (user != null) Result.success(user) else Result.failure(Exception("User data is null"))
                } else {
                    Result.failure(Exception(response.body()?.error ?: "Failed to sync user"))
                }
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun fetchChatHistory(tripId: Int): Result<List<ChatMessageDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getChatMessages(tripId)
                if (response.isSuccessful) Result.success(response.body() ?: emptyList())
                else Result.failure(Exception("Failed to load chat history"))
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun bookTrip(
        pickupName: String, pickupLat: Double, pickupLon: Double,
        dropoffName: String, dropoffLat: Double, dropoffLon: Double,
        distanceKm: Double, durationMins: Int, fare: Double,
        promoApplied: String? = null, paymentMethod: String
    ): Result<Trip> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.bookTrip(TripBookingRequest(
                    pickupName, pickupLat, pickupLon,
                    dropoffName, dropoffLat, dropoffLon,
                    distanceKm, durationMins, fare, promoApplied, paymentMethod
                ))
                if (response.isSuccessful && response.body()?.success == true) {
                    val trip = response.body()?.trip
                    if (trip != null) Result.success(trip) else Result.failure(Exception("Trip data is null"))
                } else {
                    Result.failure(Exception(response.body()?.error ?: "Failed to book trip"))
                }
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun getTrip(tripId: Int): Result<Trip> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getTrip(tripId)
                if (response.isSuccessful) {
                    val trip = response.body()
                    if (trip != null) Result.success(trip) else Result.failure(Exception("Trip not found"))
                } else {
                    Result.failure(Exception("Failed to load trip"))
                }
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun registerDriver(uid: String, fullName: String, phone: String, plateNumber: String, helmetNumber: String): Result<DriverProfile> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.registerDriver(DriverRegistrationRequest(uid, fullName, phone, plateNumber, helmetNumber))
                if (response.isSuccessful && response.body()?.success == true) {
                    val driver = response.body()?.driver
                    if (driver != null) Result.success(driver) else Result.failure(Exception("Driver data is null"))
                } else {
                    Result.failure(Exception(response.body()?.error ?: "Failed to register driver"))
                }
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun updateDriverStatus(uid: String, isOnline: Boolean, latitude: Double, longitude: Double): Result<DriverProfile> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.updateDriverStatus(DriverStatusUpdate(uid, isOnline, latitude, longitude))
                if (response.isSuccessful && response.body()?.success == true) {
                    val driver = response.body()?.driver
                    if (driver != null) Result.success(driver) else Result.failure(Exception("Driver data is null"))
                } else {
                    Result.failure(Exception(response.body()?.error ?: "Failed to update driver status"))
                }
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun calculateFare(distanceKm: Double, durationMins: Int): Result<FareCalculationResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.calculateFare(FareCalculationRequest(distanceKm, durationMins))
                if (response.isSuccessful) {
                    val fare = response.body()
                    if (fare != null) Result.success(fare) else Result.failure(Exception("Fare calculation failed"))
                } else {
                    Result.failure(Exception("Failed to calculate fare"))
                }
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun validatePromo(code: String, originalFare: Double): Result<PromoValidationResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.validatePromo(PromoValidationRequest(code, originalFare))
                if (response.isSuccessful) {
                    val promo = response.body()
                    if (promo != null) Result.success(promo) else Result.failure(Exception("Promo validation failed"))
                } else {
                    Result.failure(Exception("Failed to validate promo"))
                }
            } catch (e: Exception) { Result.failure(e) }
        }
    }
}
