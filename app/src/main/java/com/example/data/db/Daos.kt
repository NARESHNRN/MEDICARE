package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users LIMIT 1")
    fun getActiveUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getActiveUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun clearUsers()
}

@Dao
interface MedicineScanDao {
    @Query("SELECT * FROM medicine_scans ORDER BY scanDate DESC")
    fun getAllScansFlow(): Flow<List<MedicineScanEntity>>

    @Query("SELECT * FROM medicine_scans WHERE userId = :userId ORDER BY scanDate DESC")
    fun getScansByUserFlow(userId: String): Flow<List<MedicineScanEntity>>

    @Query("SELECT * FROM medicine_scans WHERE scanId = :scanId")
    suspend fun getScanById(scanId: Long): MedicineScanEntity?

    @Query("SELECT * FROM medicine_scans ORDER BY scanDate DESC LIMIT 1")
    suspend fun getLastScan(): MedicineScanEntity?

    @Query("SELECT * FROM medicine_scans WHERE expiryStatus IN ('EXPIRED', 'EXPIRING_SOON')")
    fun getExpiringOrExpiredScansFlow(): Flow<List<MedicineScanEntity>>

    @Query("SELECT * FROM medicine_scans WHERE expiryStatus IN ('EXPIRED', 'EXPIRING_SOON')")
    suspend fun getExpiringOrExpiredScans(): List<MedicineScanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: MedicineScanEntity): Long

    @Query("DELETE FROM medicine_scans WHERE scanId = :scanId")
    suspend fun deleteScanById(scanId: Long)
}

@Dao
interface CaregiverDao {
    @Query("SELECT * FROM caregivers WHERE userId = :userId")
    fun getCaregiversFlow(userId: String): Flow<List<CaregiverEntity>>

    @Query("SELECT * FROM caregivers WHERE userId = :userId LIMIT 1")
    suspend fun getPrimaryCaregiver(userId: String): CaregiverEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCaregiver(caregiver: CaregiverEntity): Long

    @Update
    suspend fun updateCaregiver(caregiver: CaregiverEntity)

    @Query("DELETE FROM caregivers WHERE caregiverId = :id")
    suspend fun deleteCaregiver(id: Long)
}

@Dao
interface EmergencyContactDao {
    @Query("SELECT * FROM emergency_contacts WHERE userId = :userId")
    fun getContactsFlow(userId: String): Flow<List<EmergencyContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContactEntity): Long

    @Query("DELETE FROM emergency_contacts WHERE contactId = :id")
    suspend fun deleteContact(id: Long)
}

@Dao
interface DrugInteractionDao {
    @Query("SELECT * FROM drug_interactions")
    suspend fun getAllInteractions(): List<DrugInteractionEntity>

    @Query("""
        SELECT * FROM drug_interactions 
        WHERE (LOWER(drugA) = LOWER(:drug1) AND LOWER(drugB) = LOWER(:drug2))
           OR (LOWER(drugA) = LOWER(:drug2) AND LOWER(drugB) = LOWER(:drug1))
    """)
    suspend fun findInteraction(drug1: String, drug2: String): DrugInteractionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(interactions: List<DrugInteractionEntity>)
}

@Dao
interface VerifiedMedicineDao {
    @Query("SELECT * FROM verified_medicines")
    suspend fun getAllVerifiedMedicines(): List<VerifiedMedicineEntity>

    @Query("SELECT * FROM verified_medicines WHERE LOWER(brandName) LIKE '%' || LOWER(:query) || '%' OR LOWER(genericName) LIKE '%' || LOWER(:query) || '%'")
    suspend fun searchMedicines(query: String): List<VerifiedMedicineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(medicines: List<VerifiedMedicineEntity>)
}
