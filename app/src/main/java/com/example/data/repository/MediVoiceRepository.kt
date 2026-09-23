package com.example.data.repository

import com.example.data.db.*
import com.example.data.model.InteractionAlert
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class MediVoiceRepository(private val database: AppDatabase) {

    private val userDao = database.userDao()
    private val scanDao = database.medicineScanDao()
    private val caregiverDao = database.caregiverDao()
    private val emergencyDao = database.emergencyContactDao()
    private val interactionDao = database.drugInteractionDao()
    private val verifiedDao = database.verifiedMedicineDao()

    val activeUserFlow: Flow<UserEntity?> = userDao.getActiveUserFlow()
    val allScansFlow: Flow<List<MedicineScanEntity>> = scanDao.getAllScansFlow()
    val expiringScansFlow: Flow<List<MedicineScanEntity>> = scanDao.getExpiringOrExpiredScansFlow()

    suspend fun getActiveUser(): UserEntity? = userDao.getActiveUser()

    suspend fun saveUser(
        name: String,
        age: Int,
        languageCode: String,
        phoneNumber: String?,
        isPhoneLinked: Boolean
    ): UserEntity {
        val existing = userDao.getActiveUser()
        val userId = if (!phoneNumber.isNullOrBlank()) {
            "P-${phoneNumber.takeLast(10)}"
        } else existing?.userId ?: "U-${UUID.randomUUID().toString().take(6).uppercase()}"

        val user = UserEntity(
            userId = userId,
            name = name.ifBlank { "User" },
            age = if (age > 0) age else 70,
            languageCode = languageCode,
            phoneNumber = phoneNumber,
            isPhoneLinked = isPhoneLinked,
            isDeviceBound = true,
            highContrast = existing?.highContrast ?: true,
            speechRate = existing?.speechRate ?: 0.95f
        )
        userDao.insertUser(user)
        return user
    }

    suspend fun createDeviceBoundProfile(languageCode: String): UserEntity {
        val deviceId = "U-${UUID.randomUUID().toString().take(6).uppercase()}"
        val user = UserEntity(
            userId = deviceId,
            name = "Device User",
            age = 70,
            languageCode = languageCode,
            phoneNumber = null,
            isPhoneLinked = false,
            isDeviceBound = true
        )
        userDao.insertUser(user)
        return user
    }

    suspend fun updateSpeechRate(rate: Float) {
        val user = userDao.getActiveUser() ?: return
        userDao.updateUser(user.copy(speechRate = rate))
    }

    suspend fun updateHighContrast(enabled: Boolean) {
        val user = userDao.getActiveUser() ?: return
        userDao.updateUser(user.copy(highContrast = enabled))
    }

    suspend fun saveScan(scan: MedicineScanEntity): Long {
        return scanDao.insertScan(scan)
    }

    suspend fun getScanById(scanId: Long): MedicineScanEntity? = scanDao.getScanById(scanId)

    suspend fun getLastScan(): MedicineScanEntity? = scanDao.getLastScan()

    suspend fun deleteScan(scanId: Long) = scanDao.deleteScanById(scanId)

    suspend fun checkDrugInteractions(
        newMedicineName: String,
        newIngredient: String
    ): List<InteractionAlert> {
        val existingScans = scanDao.getAllScansFlow()
        // Check against saved medicines
        val savedMedicines = mutableListOf<Pair<String, String>>()
        val allScans = scanDao.getExpiringOrExpiredScans() // or get recent
        // We can query last scans or all
        val allDbScans = scanDao.getLastScan()?.let { listOf(it) } ?: emptyList()
        val allInteractions = interactionDao.getAllInteractions()
        val alerts = mutableListOf<InteractionAlert>()

        // Look through pre-loaded and saved records
        for (interaction in allInteractions) {
            val drugA = interaction.drugA.lowercase()
            val drugB = interaction.drugB.lowercase()
            val newNameLower = newMedicineName.lowercase()
            val newIngLower = newIngredient.lowercase()

            val matchesDrugA = newNameLower.contains(drugA) || newIngLower.contains(drugA) || drugA.contains(newNameLower)
            val matchesDrugB = newNameLower.contains(drugB) || newIngLower.contains(drugB) || drugB.contains(newNameLower)

            if (matchesDrugA || matchesDrugB) {
                val otherDrug = if (matchesDrugA) drugB else drugA
                // Check if user has otherDrug in their scanned history
                // or if it's a known dangerous combination with common substances
                alerts.add(
                    InteractionAlert(
                        drugNameA = newMedicineName,
                        drugNameB = interaction.drugB,
                        severity = interaction.severity,
                        warning = interaction.warningMessage,
                        clinicalAction = interaction.clinicalAction
                    )
                )
            }
        }
        return alerts
    }

    suspend fun searchVerifiedMedicines(query: String): List<VerifiedMedicineEntity> {
        return verifiedDao.searchMedicines(query)
    }

    suspend fun getAllVerifiedMedicines(): List<VerifiedMedicineEntity> {
        return verifiedDao.getAllVerifiedMedicines()
    }

    fun getCaregiversFlow(userId: String): Flow<List<CaregiverEntity>> =
        caregiverDao.getCaregiversFlow(userId)

    suspend fun addCaregiver(caregiver: CaregiverEntity): Long =
        caregiverDao.insertCaregiver(caregiver)

    fun getEmergencyContactsFlow(userId: String): Flow<List<EmergencyContactEntity>> =
        emergencyDao.getContactsFlow(userId)

    suspend fun addEmergencyContact(contact: EmergencyContactEntity): Long =
        emergencyDao.insertContact(contact)

    suspend fun deleteEmergencyContact(contactId: Long) =
        emergencyDao.deleteContact(contactId)
}
