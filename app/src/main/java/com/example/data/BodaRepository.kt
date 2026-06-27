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

    suspend fun completePendingPayment(reference: String) {
        dao?.completePendingPayment(reference)
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

    suspend fun updateTripStatus(tripId: Int, status: String, rating: Int? = null, comment: String? = null, disputeReason: String? = null, disputeEvidence: String? = null): Result<Trip> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.updateTripStatus(tripId, TripStatusUpdate(status, rating, comment, dispute_reason = disputeReason, dispute_evidence = disputeEvidence))
                if (response.isSuccessful && response.body()?.success == true) {
                    val trip = response.body()?.trip
                    if (trip != null) Result.success(trip) else Result.failure(Exception("Trip data is null"))
                } else {
                    Result.failure(Exception(response.body()?.error ?: "Failed to update trip"))
                }
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun claimTrip(tripId: Int): Result<Trip> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.claimTrip(tripId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val trip = response.body()?.trip
                    if (trip != null) Result.success(trip) else Result.failure(Exception("Trip data is null"))
                } else {
                    Result.failure(Exception(response.body()?.error ?: "Trip already claimed"))
                }
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun savePlaceToBackend(label: String, name: String, latitude: Double, longitude: Double): Result<SavedPlace> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.savePlace(SavePlaceRequest(label, name, latitude, longitude))
                if (response.isSuccessful && response.body()?.success == true) {
                    val place = response.body()?.savedPlace
                    if (place != null) Result.success(place) else Result.failure(Exception("Place data is null"))
                } else {
                    Result.failure(Exception(response.body()?.error ?: "Failed to save place"))
                }
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun fetchTrips(): Result<List<TripDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getTrips()
                if (response.isSuccessful) Result.success(response.body() ?: emptyList())
                else Result.failure(Exception("Failed to load trips"))
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun fetchUserProfile(): Result<UserMeResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getMe()
                if (response.isSuccessful) {
                    val user = response.body()
                    if (user != null) Result.success(user) else Result.failure(Exception("User not found"))
                } else {
                    Result.failure(Exception("Failed to load profile"))
                }
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun fetchWalletBalance(): Result<Double> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getMe()
                if (response.isSuccessful) {
                    val balance = response.body()?.wallet_balance ?: 0.0
                    Result.success(balance)
                } else {
                    Result.failure(Exception("Failed to load balance"))
                }
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun walletPay(amount: Double, provider: String, tripId: Int? = null): Result<TopupResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.walletPay(WalletTopupRequest(amount, provider))
                if (response.isSuccessful && response.body()?.success == true) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Payment failed"))
                }
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun fetchWalletTransactions(): Result<List<WalletTransactionDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getWalletTransactions()
                if (response.isSuccessful) Result.success(response.body() ?: emptyList())
                else Result.failure(Exception("Failed to load transactions"))
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun walletTopup(amount: Double, provider: String): Result<TopupResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.walletTopup(WalletTopupRequest(amount, provider))
                if (response.isSuccessful && response.body()?.success == true) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Topup failed"))
                }
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun fetchEmergencyContactsFromBackend(): Result<List<EmergencyContactDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getEmergencyContacts()
                if (response.isSuccessful) Result.success(response.body() ?: emptyList())
                else Result.failure(Exception("Failed to load contacts"))
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun addEmergencyContactToBackend(name: String, phone: String): Result<EmergencyContactDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.addEmergencyContact(EmergencyContactRequest(name, phone))
                if (response.isSuccessful && response.body()?.success == true) {
                    val contact = response.body()?.contact
                    if (contact != null) Result.success(contact) else Result.failure(Exception("Contact data is null"))
                } else {
                    Result.failure(Exception("Failed to add contact"))
                }
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun deleteEmergencyContactFromBackend(contactId: Int): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.deleteEmergencyContact(contactId)
                if (response.isSuccessful) Result.success(true) else Result.failure(Exception("Failed to delete contact"))
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun deleteSavedPlaceFromBackend(placeId: Int): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.deleteSavedPlace(placeId)
                if (response.isSuccessful) Result.success(true) else Result.failure(Exception("Failed to delete place"))
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun fetchReferralsFromBackend(): Result<List<ReferralDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getReferrals()
                if (response.isSuccessful) Result.success(response.body() ?: emptyList())
                else Result.failure(Exception("Failed to load referrals"))
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun addReferralToBackend(name: String, phone: String, code: String): Result<ReferralDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.addReferral(ReferralRequest(name, phone, code))
                if (response.isSuccessful && response.body()?.success == true) {
                    val ref = response.body()?.referral
                    if (ref != null) Result.success(ref) else Result.failure(Exception("Referral data is null"))
                } else {
                    Result.failure(Exception("Failed to add referral"))
                }
            } catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun completeReferralOnBackend(referralId: Int): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.completeReferral(referralId)
                if (response.isSuccessful && response.body()?.success == true) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Failed to complete referral"))
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
