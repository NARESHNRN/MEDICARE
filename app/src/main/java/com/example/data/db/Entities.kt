package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String, // e.g. U-7X9K24 or Phone number
    val name: String,
    val age: Int,
    val languageCode: String,
    val phoneNumber: String?,
    val isPhoneLinked: Boolean,
    val isDeviceBound: Boolean,
    val highContrast: Boolean = true,
    val speechRate: Float = 0.95f,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "medicine_scans")
data class MedicineScanEntity(
    @PrimaryKey(autoGenerate = true) val scanId: Long = 0,
    val userId: String,
    val medicineName: String,
    val activeIngredient: String,
    val strength: String,
    val manufacturer: String,
    val batchNumber: String,
    val expiryDate: String?,
    val expiryStatus: String, // "NOT_EXPIRED", "EXPIRING_SOON", "EXPIRED", "UNVERIFIED_MISSING"
    val isExpiryVerified: Boolean,
    val scanDate: Long = System.currentTimeMillis(),
    val imagePath: String? = null,
    val confidenceStatus: String, // "HIGH", "MEDIUM", "LOW"
    val confidenceScore: Float,
    val packageRawText: String,
    val verifiedReferenceSource: String,
    val generalUsesJson: String,
    val warningsJson: String,
    val ageGuidelines: String,
    val pregnancyGuidelines: String,
    val commonSideEffectsJson: String,
    val seriousSideEffectsJson: String,
    val storageInstructions: String,
    val isBackScanned: Boolean = false
)

@Entity(tableName = "caregivers")
data class CaregiverEntity(
    @PrimaryKey(autoGenerate = true) val caregiverId: Long = 0,
    val userId: String,
    val name: String,
    val phone: String,
    val relationship: String,
    val isAuthorized: Boolean = true,
    val authPin: String = "1234",
    val receiveExpiryAlerts: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "emergency_contacts")
data class EmergencyContactEntity(
    @PrimaryKey(autoGenerate = true) val contactId: Long = 0,
    val userId: String,
    val name: String,
    val relationship: String,
    val phone: String,
    val isPrimary: Boolean = false
)

@Entity(tableName = "drug_interactions")
data class DrugInteractionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val drugA: String,
    val drugB: String,
    val severity: String, // "HIGH", "MODERATE", "LOW"
    val warningMessage: String,
    val clinicalAction: String
)

@Entity(tableName = "verified_medicines")
data class VerifiedMedicineEntity(
    @PrimaryKey val id: String,
    val brandName: String,
    val genericName: String,
    val standardStrength: String,
    val manufacturer: String,
    val commonUses: String,
    val warnings: String,
    val agePrecautions: String,
    val pregnancyStatus: String,
    val commonSideEffects: String,
    val seriousSideEffects: String,
    val storage: String,
    val scheduleCategory: String
)
