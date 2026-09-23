package com.example.data.model

import java.time.LocalDate

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String, val welcomeVoice: String) {
    ENGLISH("en", "English", "English", "Please tell me which language you normally speak."),
    HINDI("hi", "Hindi", "हिन्दी", "कृपया बताइए आप आमतौर पर कौन सी भाषा बोलते हैं।"),
    TAMIL("ta", "Tamil", "தமிழ்", "தயவுசெய்து நீங்கள் வழக்கமாகப் பேசும் மொழியைச் சொல்லுங்கள்."),
    TELUGU("te", "Telugu", "తెలుగు", "దయచేసి మీరు సాధారణంగా మాట్లాడే భాషను చెప్పండి."),
    KANNADA("kn", "Kannada", "ಕನ್ನಡ", "ದಯವಿಟ್ಟು ನೀವು ಸಾಮಾನ್ಯವಾಗಿ ಮಾತನಾಡುವ ಭಾಷೆಯನ್ನು ತಿಳಿಸಿ."),
    MALAYALAM("ml", "Malayalam", "മലയാളം", "ദയവായി നിങ്ങൾ സാധാരണ സംസാരിക്കുന്ന ഭാഷ പറയുക."),
    BENGALI("bn", "Bengali", "বাংলা", "অনুগ্রহ করে বলুন আপনি সাধারণত কোন ভাষায় কথা বলেন।"),
    SPANISH("es", "Spanish", "Español", "Por favor, dígame qué idioma habla normalmente.")
}

enum class ExpiryStatus {
    NOT_EXPIRED,
    EXPIRING_SOON,
    EXPIRED,
    UNVERIFIED_MISSING
}

enum class ConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW
}

data class ImageQualityResult(
    val isAcceptable: Boolean,
    val blurScore: Double,
    val isBlurry: Boolean,
    val brightnessScore: Double,
    val isTooDark: Boolean,
    val isTooBright: Boolean,
    val hasGlare: Boolean,
    val guidanceMessage: String
)

data class VerificationEvidence(
    val nameScore: Float,
    val strengthMatched: Boolean,
    val ingredientMatched: Boolean,
    val manufacturerMatched: Boolean,
    val packageMarkingsFound: Boolean,
    val databaseMatched: Boolean,
    val physicalShapeScore: Float,
    val overallScore: Float,
    val confidenceLevel: ConfidenceLevel,
    val evidenceDetails: List<String>
)

data class MedicineAnalysisResult(
    val medicineName: String,
    val activeIngredient: String,
    val strength: String,
    val manufacturer: String,
    val batchNumber: String,
    val mfgDate: String?,
    val expiryDateString: String?,
    val parsedExpiryDate: LocalDate?,
    val expiryStatus: ExpiryStatus,
    val expiryMessage: String,
    val generalUses: List<String>,
    val importantWarnings: List<String>,
    val ageGuidelines: String,
    val pregnancyBreastfeeding: String,
    val commonSideEffects: List<String>,
    val seriousSideEffects: List<String>,
    val storageInstructions: String,
    val verificationEvidence: VerificationEvidence,
    val packageRawText: String,
    val verifiedReferenceSource: String,
    val potentialInteractions: List<InteractionAlert> = emptyList(),
    val needsBackScan: Boolean = false,
    val backScanReason: String? = null
)

data class InteractionAlert(
    val drugNameA: String,
    val drugNameB: String,
    val severity: String, // "HIGH", "MODERATE", "LOW"
    val warning: String,
    val clinicalAction: String
)

data class FollowUpAnswer(
    val question: String,
    val answer: String,
    val isSafetyCritical: Boolean,
    val source: String
)
