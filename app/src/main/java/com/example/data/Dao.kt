package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BodaDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    @Query("SELECT * FROM saved_places ORDER BY id DESC")
    fun getSavedPlaces(): Flow<List<SavedPlace>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPlace(place: SavedPlace)

    @Delete
    suspend fun deleteSavedPlace(place: SavedPlace)

    @Query("SELECT * FROM trips ORDER BY timestamp DESC")
    fun getTrips(): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE id = :id LIMIT 1")
    suspend fun getTripById(id: Int): Trip?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: Trip): Long

    @Update
    suspend fun updateTrip(trip: Trip)

    @Query("SELECT * FROM wallet_transactions ORDER BY timestamp DESC")
    fun getTransactions(): Flow<List<WalletTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(txn: WalletTransaction)

    @Query("SELECT * FROM emergency_contacts LIMIT 3")
    fun getEmergencyContacts(): Flow<List<EmergencyContact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmergencyContact(contact: EmergencyContact)

    @Delete
    suspend fun deleteEmergencyContact(contact: EmergencyContact)

    @Query("SELECT * FROM referrals ORDER BY timestamp DESC")
    fun getReferrals(): Flow<List<Referral>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReferral(referral: Referral): Long

    @Query("SELECT * FROM referrals WHERE referredPhone = :phone LIMIT 1")
    suspend fun getReferralByPhone(phone: String): Referral?
}
