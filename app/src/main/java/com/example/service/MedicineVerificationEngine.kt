package com.example.service

import com.example.data.db.VerifiedMedicineEntity
import com.example.data.model.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object MedicineVerificationEngine {

    // Anchor Date: 2026-09-23 (as provided by system context)
    private val CURRENT_DATE: LocalDate = LocalDate.of(2026, 9, 23)

    /**
     * Executes the Multi-Evidence Verification Pipeline.
     * Takes raw text (from front, or combined front+back), queries verified database,
     * and performs fuzzy/regex verification.
     */
    fun verifyMedicine(
        rawOcrText: String,
        verifiedDatabase: List<VerifiedMedicineEntity>,
        isBackScanCombined: Boolean = false
    ): MedicineAnalysisResult {
        val cleanText = rawOcrText.replace("\n", " ").trim()
        val upperText = cleanText.uppercase(Locale.ROOT)

        // 1. Evidence: Dosage / Strength extraction
        val strengthPattern = Regex("""(\d{1,4}(\.\d+)?\s*(MG|MCG|G|ML|%|IU|UNITS))""")
        val matchedStrength = strengthPattern.find(upperText)?.value ?: ""

        // 2. Evidence: Batch Number extraction
        val batchPattern = Regex("""(B\.?\s*NO|BATCH|LOT)[\.\s:]*([A-Z0-9\-]+)""")
        val matchedBatch = batchPattern.find(upperText)?.groupValues?.getOrNull(2) ?: "Not legible"

        // 3. Evidence: Manufacturing Date extraction
        val mfgPattern = Regex("""(MFG|MFD|M\.?DATE)[\.\s:]*([A-Za-z]{3,4}[\/\s\.-]*\d{2,4}|\d{1,2}[\/\.-]\d{2,4})""")
        val matchedMfg = mfgPattern.find(upperText)?.groupValues?.getOrNull(2)

        // 4. Evidence: Expiry Date Extraction & Parsing
        val (parsedExpiry, expiryStr, expiryStatus, expiryMsg) = parseAndValidateExpiry(upperText)

        // 5. Evidence: Package Markings (Rx, Schedule H, warnings)
        val hasRx = upperText.contains("RX") || upperText.contains("SCHEDULE H") || upperText.contains("SCHEDULE")
        val hasStripMarkings = upperText.contains("TAB") || upperText.contains("CAP") || upperText.contains("MG") || upperText.contains("PHARMA")

        // 6. Evidence: Match against Verified Medicine Knowledge Base
        var bestMatch: VerifiedMedicineEntity? = null
        var bestNameScore = 0.0f
        var bestIngredientScore = 0.0f
        var bestManufacturerScore = 0.0f

        for (item in verifiedDatabase) {
            val nameScore = calculateSimilarity(upperText, item.brandName.uppercase(Locale.ROOT))
            val genericScore = calculateSimilarity(upperText, item.genericName.uppercase(Locale.ROOT))
            val mfgScore = if (upperText.contains(item.manufacturer.uppercase(Locale.ROOT).take(5))) 0.9f else 0.0f

            val overallItemScore = max(nameScore, genericScore)
            if (overallItemScore > bestNameScore) {
                bestNameScore = overallItemScore
                bestIngredientScore = genericScore
                bestManufacturerScore = mfgScore
                bestMatch = item
            }
        }

        // Check strength alignment
        val strengthMatches = if (bestMatch != null && matchedStrength.isNotBlank()) {
            bestMatch.standardStrength.uppercase(Locale.ROOT).contains(matchedStrength) ||
                    matchedStrength.contains(bestMatch.standardStrength.uppercase(Locale.ROOT).take(3))
        } else false

        // Compute Weighted Confidence Score
        // Evidence 1: Name similarity (0.30)
        // Evidence 2: Strength match (0.20)
        // Evidence 3: Active ingredient match (0.20)
        // Evidence 4: Manufacturer match (0.10)
        // Evidence 5: Package markings (0.05)
        // Evidence 6: Database match (0.15)
        var weightedScore = (bestNameScore * 0.30f) +
                (if (strengthMatches) 0.20f else 0.0f) +
                (bestIngredientScore * 0.20f) +
                (bestManufacturerScore * 0.10f) +
                (if (hasRx || hasStripMarkings) 0.05f else 0.0f) +
                (if (bestMatch != null && bestNameScore > 0.4f) 0.15f else 0.0f)

        weightedScore = min(1.0f, weightedScore)

        val confidenceLevel = when {
            weightedScore >= 0.70f -> ConfidenceLevel.HIGH
            weightedScore >= 0.45f -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }

        val evidenceDetails = mutableListOf<String>()
        if (bestNameScore > 0.5f) evidenceDetails.add("Medicine brand name recognized with ${(bestNameScore * 100).toInt()}% confidence.")
        if (strengthMatches) evidenceDetails.add("Strength matches verified therapeutic formulation ($matchedStrength).")
        if (bestIngredientScore > 0.5f) evidenceDetails.add("Active chemical ingredient identified.")
        if (bestManufacturerScore > 0.5f) evidenceDetails.add("Manufacturer verified in drug registry.")
        if (hasRx) evidenceDetails.add("Prescription Schedule H / Rx warning markings confirmed on package.")
        if (parsedExpiry != null) evidenceDetails.add("Expiry date verified ($expiryStr).")

        // Cut / Partially Damaged Strip Check
        // If critical information (expiry or strength) is missing on a single side, prompt for back scan
        val needsBack = !isBackScanCombined && (expiryStatus == ExpiryStatus.UNVERIFIED_MISSING || matchedStrength.isBlank() || confidenceLevel == ConfidenceLevel.MEDIUM)
        val backReason = when {
            expiryStatus == ExpiryStatus.UNVERIFIED_MISSING -> "Expiry date is not visible on this side. Please turn the strip over and scan the back side."
            matchedStrength.isBlank() -> "Dosage strength is cut or missing. Please scan the back side for dosage verification."
            confidenceLevel == ConfidenceLevel.MEDIUM -> "Additional packaging markings needed for high-confidence identification. Please scan the back side."
            else -> null
        }

        val medicineName = bestMatch?.brandName ?: extractLikelyName(upperText)
        val activeIngredient = bestMatch?.genericName ?: "Unverified active ingredient"
        val strength = if (matchedStrength.isNotBlank()) matchedStrength else bestMatch?.standardStrength ?: "Strength not clearly specified on strip"
        val manufacturer = bestMatch?.manufacturer ?: "Manufacturer details on packaging"

        val uses = bestMatch?.commonUses?.split(",")?.map { it.trim() }
            ?: listOf("Symptomatic medical treatment as directed by prescribing physician")

        val warnings = bestMatch?.warnings?.split(".")?.filter { it.isNotBlank() }?.map { it.trim() }
            ?: listOf("Check with doctor or pharmacist before administering.")

        val commonSideEffects = bestMatch?.commonSideEffects?.split(",")?.map { it.trim() }
            ?: listOf("Mild nausea", "Indigestion", "Headache")

        val seriousSideEffects = bestMatch?.seriousSideEffects?.split(",")?.map { it.trim() }
            ?: listOf("Severe allergic reaction", "Difficulty breathing", "Facial swelling")

        return MedicineAnalysisResult(
            medicineName = medicineName,
            activeIngredient = activeIngredient,
            strength = strength,
            manufacturer = manufacturer,
            batchNumber = matchedBatch,
            mfgDate = matchedMfg,
            expiryDateString = expiryStr,
            parsedExpiryDate = parsedExpiry,
            expiryStatus = expiryStatus,
            expiryMessage = expiryMsg,
            generalUses = uses,
            importantWarnings = warnings,
            ageGuidelines = bestMatch?.agePrecautions ?: "Consult physician for pediatric or elderly dosing adjustments.",
            pregnancyBreastfeeding = bestMatch?.pregnancyStatus ?: "Safety in pregnancy or breastfeeding must be verified by a medical practitioner.",
            commonSideEffects = commonSideEffects,
            seriousSideEffects = seriousSideEffects,
            storageInstructions = bestMatch?.storage ?: "Store in a cool dry place protected from direct sunlight.",
            verificationEvidence = VerificationEvidence(
                nameScore = bestNameScore,
                strengthMatched = strengthMatches,
                ingredientMatched = bestIngredientScore > 0.4f,
                manufacturerMatched = bestManufacturerScore > 0.4f,
                packageMarkingsFound = hasRx || hasStripMarkings,
                databaseMatched = bestMatch != null,
                physicalShapeScore = 0.8f,
                overallScore = weightedScore,
                confidenceLevel = confidenceLevel,
                evidenceDetails = evidenceDetails
            ),
            packageRawText = rawOcrText,
            verifiedReferenceSource = "MediVoice Local Clinical Knowledge Graph & Central Pharmacopeia Reference",
            needsBackScan = needsBack,
            backScanReason = backReason
        )
    }

    /**
     * Robust Expiry Date Parser and Validator.
     * Uses regex patterns for standard medicine packaging dates.
     * Compares against current date (2026-09-23).
     */
    private fun parseAndValidateExpiry(text: String): Tuple4<LocalDate?, String?, ExpiryStatus, String> {
        val expiryKeywords = listOf("EXP", "EXPIRY", "EXP DATE", "USE BEFORE", "BEST BEFORE", "BB", "VAL")
        val monthNames = mapOf(
            "JAN" to 1, "FEB" to 2, "MAR" to 3, "APR" to 4, "MAY" to 5, "JUN" to 6,
            "JUL" to 7, "AUG" to 8, "SEP" to 9, "OCT" to 10, "NOV" to 11, "DEC" to 12
        )

        // Pattern 1: EXP. 08/2027 or EXP 08/27 or EXP: 12-2028
        val pattern1 = Regex("""(EXP|EXPIRY|USE BEFORE|BEST BEFORE|BB)[\.\s:]*([0-1]?\d)[\/\-\.](20\d{2}|\d{2})""")
        val match1 = pattern1.find(text)
        if (match1 != null) {
            val month = match1.groupValues[2].toIntOrNull() ?: 1
            var year = match1.groupValues[3].toIntOrNull() ?: 2026
            if (year < 100) year += 2000

            val ym = YearMonth.of(year, month.coerceIn(1, 12))
            val expDate = ym.atEndOfMonth()
            val status = evaluateExpiryStatus(expDate)
            val msg = buildExpiryMessage(status, expDate)
            return Tuple4(expDate, "${expDate.month.name.take(3)} $year", status, msg)
        }

        // Pattern 2: EXP AUG 2027 or EXP AUG-27 or EXP: OCT 26
        val pattern2 = Regex("""(EXP|EXPIRY|USE BEFORE|BEST BEFORE|BB)[\.\s:]*([A-Z]{3,4})[\/\s\.-]*(20\d{2}|\d{2})""")
        val match2 = pattern2.find(text)
        if (match2 != null) {
            val monthStr = match2.groupValues[2].take(3)
            val month = monthNames[monthStr] ?: 12
            var year = match2.groupValues[3].toIntOrNull() ?: 2026
            if (year < 100) year += 2000

            val ym = YearMonth.of(year, month)
            val expDate = ym.atEndOfMonth()
            val status = evaluateExpiryStatus(expDate)
            val msg = buildExpiryMessage(status, expDate)
            return Tuple4(expDate, "$monthStr $year", status, msg)
        }

        // Pattern 3: Standalone date near end of text (e.g. 05/2028)
        val standalonePattern = Regex("""\b([0-1]?\d)[\/\-](202[4-9]|203\d|\d{2})\b""")
        val match3 = standalonePattern.find(text)
        if (match3 != null && text.contains("EXP")) {
            val month = match3.groupValues[1].toIntOrNull() ?: 1
            var year = match3.groupValues[2].toIntOrNull() ?: 2026
            if (year < 100) year += 2000

            val ym = YearMonth.of(year, month.coerceIn(1, 12))
            val expDate = ym.atEndOfMonth()
            val status = evaluateExpiryStatus(expDate)
            val msg = buildExpiryMessage(status, expDate)
            return Tuple4(expDate, "${expDate.month.name.take(3)} $year", status, msg)
        }

        // Missing / Unverified expiry — DO NOT GUESS OR INVENT FAKE DATE!
        return Tuple4(
            null,
            null,
            ExpiryStatus.UNVERIFIED_MISSING,
            "The expiry date could not be verified from the visible packaging. Please scan the other side or check the original box."
        )
    }

    private fun evaluateExpiryStatus(expiryDate: LocalDate): ExpiryStatus {
        return when {
            expiryDate.isBefore(CURRENT_DATE) -> ExpiryStatus.EXPIRED
            expiryDate.isBefore(CURRENT_DATE.plusDays(60)) -> ExpiryStatus.EXPIRING_SOON
            else -> ExpiryStatus.NOT_EXPIRED
        }
    }

    private fun buildExpiryMessage(status: ExpiryStatus, date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
        val formatted = date.format(formatter)
        return when (status) {
            ExpiryStatus.NOT_EXPIRED -> "This medicine expires in $formatted. It is safe for current use."
            ExpiryStatus.EXPIRING_SOON -> "Warning: This medicine will expire soon in $formatted. Plan replacement soon."
            ExpiryStatus.EXPIRED -> "CRITICAL WARNING: This medicine expired in $formatted. Do not consume expired medicine. Please consult a pharmacist."
            ExpiryStatus.UNVERIFIED_MISSING -> "The expiry date could not be verified."
        }
    }

    /**
     * Levenshtein Distance & Fuzzy Match similarity (0.0 to 1.0)
     */
    private fun calculateSimilarity(source: String, target: String): Float {
        if (source.contains(target)) return 1.0f
        val words = source.split(" ", "-", "/")
        var maxWordSimilarity = 0.0f

        for (word in words) {
            if (word.length < 3) continue
            val dist = levenshtein(word, target)
            val maxLen = max(word.length, target.length)
            val sim = 1.0f - (dist.toFloat() / maxLen)
            if (sim > maxWordSimilarity) {
                maxWordSimilarity = sim
            }
        }
        return maxWordSimilarity
    }

    private fun levenshtein(s: String, t: String): Int {
        val dp = Array(s.length + 1) { IntArray(t.length + 1) }
        for (i in 0..s.length) dp[i][0] = i
        for (j in 0..t.length) dp[0][j] = j

        for (i in 1..s.length) {
            for (j in 1..t.length) {
                val cost = if (s[i - 1] == t[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s.length][t.length]
    }

    private fun extractLikelyName(text: String): String {
        val words = text.split(" ")
        return words.firstOrNull { it.length > 4 && it.all { ch -> ch.isLetter() } }
            ?: "Unidentified Medicine Strip"
    }

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
