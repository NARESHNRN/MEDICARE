package com.example.service

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.FollowUpAnswer
import com.example.data.model.MedicineAnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiMedicineService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Multimodal OCR Extraction from medicine strip bitmap using Gemini 3.5 Flash.
     */
    suspend fun analyzeMedicineImageWithGemini(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext null
        }

        try {
            val base64Image = bitmapToBase64(bitmap)
            val prompt = """
                You are an accessibility medicine reader assistant.
                Carefully read the text printed on this medicine strip/box image.
                Transcribe all visible printed text verbatim, including:
                - Medicine Brand Name
                - Generic / Active Ingredients
                - Strength / Dosage (e.g. 500mg, 650mg, 10mg)
                - Manufacturer Name
                - Batch Number (B.No / Lot)
                - Manufacturing Date (MFG)
                - Expiry Date (EXP / Use Before)
                - Regulatory Schedule (Rx / Schedule H)
                - Any explicit printed warnings
                Output strictly the transcribed text. Do NOT guess or hallucinate any missing expiry date or missing text.
            """.trimIndent()

            val partsArray = JSONArray().apply {
                put(JSONObject().put("text", prompt))
                put(
                    JSONObject().put(
                        "inlineData",
                        JSONObject().apply {
                            put("mimeType", "image/jpeg")
                            put("data", base64Image)
                        }
                    )
                )
            }

            val contentObject = JSONObject().apply {
                put("parts", partsArray)
            }

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().put(contentObject))
            }

            val requestBody = requestJson.toString().toRequestBody(jsonMediaType)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext null

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates") ?: return@withContext null
            val firstCandidate = candidates.optJSONObject(0) ?: return@withContext null
            val content = firstCandidate.optJSONObject("content") ?: return@withContext null
            val parts = content.optJSONArray("parts") ?: return@withContext null
            val text = parts.optJSONObject(0)?.optString("text")

            text
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Answers follow-up voice questions using Gemini 3.5 Flash or local fallback.
     * Enforces clinical safety boundaries.
     */
    suspend fun answerFollowUpQuestion(
        medicineResult: MedicineAnalysisResult,
        userQuestion: String,
        targetLanguageCode: String
    ): FollowUpAnswer = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Local verified fallback
            return@withContext getLocalClinicalAnswer(medicineResult, userQuestion)
        }

        try {
            val systemInstruction = """
                You are an accessibility medicine assistant for elderly and low-literacy patients.
                You are answering questions about the verified medicine: ${medicineResult.medicineName} (${medicineResult.activeIngredient}, ${medicineResult.strength}).
                
                STRICT HEALTHCARE SAFETY BOUNDARIES:
                1. Speak in simple, reassuring language suitable for an elderly listener.
                2. Respond in the user's requested language code: $targetLanguageCode.
                3. DO NOT diagnose the user's symptoms or illness.
                4. DO NOT prescribe new medications or recommend adjusting doses.
                5. DO NOT independently recommend stopping a prescribed medication.
                6. Explicitly state verified medical reference precautions.
                7. Keep the answer clear and concise (2-4 sentences max so TTS can read it comfortably).
            """.trimIndent()

            val contentsArray = JSONArray().apply {
                put(
                    JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().put("text", userQuestion)))
                    }
                )
            }

            val requestJson = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
                })
            }

            val requestBody = requestJson.toString().toRequestBody(jsonMediaType)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (responseBody != null) {
                val json = JSONObject(responseBody)
                val candidates = json.optJSONArray("candidates")
                val text = candidates?.optJSONObject(0)?.optJSONObject("content")
                    ?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")

                if (!text.isNullOrBlank()) {
                    return@withContext FollowUpAnswer(
                        question = userQuestion,
                        answer = text.trim(),
                        isSafetyCritical = false,
                        source = "Gemini Clinical Assistant & Verified Reference"
                    )
                }
            }
            getLocalClinicalAnswer(medicineResult, userQuestion)
        } catch (e: Exception) {
            e.printStackTrace()
            getLocalClinicalAnswer(medicineResult, userQuestion)
        }
    }

    private fun getLocalClinicalAnswer(
        medicine: MedicineAnalysisResult,
        question: String
    ): FollowUpAnswer {
        val qLower = question.lowercase()
        val answer = when {
            qLower.contains("use") || qLower.contains("what is this for") || qLower.contains("why") || qLower.contains("used") -> {
                "${medicine.medicineName} contains ${medicine.activeIngredient}. It is commonly used for: ${medicine.generalUses.joinToString(", ")}. Always take as directed by your physician."
            }
            qLower.contains("side effect") || qLower.contains("effects") -> {
                "Common side effects of ${medicine.medicineName} include ${medicine.commonSideEffects.joinToString(", ")}. If you experience severe reactions like ${medicine.seriousSideEffects.joinToString(", ")}, seek immediate medical attention."
            }
            qLower.contains("when") || qLower.contains("how to take") || qLower.contains("time") -> {
                "Please follow the specific dosing schedule prescribed by your doctor. Common storage and administration: ${medicine.storageInstructions}. Never skip or double your dose."
            }
            qLower.contains("pregnant") || qLower.contains("pregnancy") || qLower.contains("breastfeed") -> {
                "Pregnancy safety for ${medicine.medicineName}: ${medicine.pregnancyBreastfeeding}. Please consult your obstetrician before taking this medicine."
            }
            qLower.contains("elderly") || qLower.contains("age") || qLower.contains("old") || qLower.contains("child") -> {
                "Age precautions: ${medicine.ageGuidelines}. Older adults must monitor their hydration and kidney profile with their doctor."
            }
            qLower.contains("expiry") || qLower.contains("expired") -> {
                medicine.expiryMessage
            }
            else -> {
                "${medicine.medicineName} is manufactured by ${medicine.manufacturer}. Warnings: ${medicine.importantWarnings.firstOrNull() ?: "Consult your physician for personalized medical advice."}"
            }
        }

        return FollowUpAnswer(
            question = question,
            answer = answer,
            isSafetyCritical = false,
            source = "MediVoice Local Clinical Pharmacopeia"
        )
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Compress to reasonable resolution for high quality multimodal parsing
        val scaled = if (bitmap.width > 1280 || bitmap.height > 1280) {
            val scale = 1280.0f / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else bitmap

        scaled.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
