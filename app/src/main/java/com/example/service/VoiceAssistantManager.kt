package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.data.model.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceAssistantManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var currentLanguage = AppLanguage.ENGLISH
    private var speechRate = 0.92f // Slightly slower for elderly clarity

    private var speechRecognizer: SpeechRecognizer? = null
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private var pendingSpeechOnTtsInit: String? = null
    private var onSpeechDoneCallback: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            setLanguage(currentLanguage)
            tts?.setSpeechRate(speechRate)
            tts?.setPitch(1.0f)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    if (utteranceId?.startsWith("ask_listen_") == true) {
                        val action = onSpeechDoneCallback
                        onSpeechDoneCallback = null
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            action?.invoke()
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    if (utteranceId?.startsWith("ask_listen_") == true) {
                        val action = onSpeechDoneCallback
                        onSpeechDoneCallback = null
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            action?.invoke()
                        }
                    }
                }
            })

            pendingSpeechOnTtsInit?.let {
                speak(it)
                pendingSpeechOnTtsInit = null
            }
        }
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.7f, 1.5f)
        tts?.setSpeechRate(speechRate)
    }

    fun setLanguage(language: AppLanguage) {
        currentLanguage = language
        val locale = when (language) {
            AppLanguage.ENGLISH -> Locale.US
            AppLanguage.HINDI -> Locale("hi", "IN")
            AppLanguage.TAMIL -> Locale("ta", "IN")
            AppLanguage.TELUGU -> Locale("te", "IN")
            AppLanguage.KANNADA -> Locale("kn", "IN")
            AppLanguage.MALAYALAM -> Locale("ml", "IN")
            AppLanguage.BENGALI -> Locale("bn", "IN")
            AppLanguage.SPANISH -> Locale("es", "ES")
        }
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to English if exact Indian regional voice package is missing
            tts?.setLanguage(Locale.US)
        }
    }

    fun speak(text: String, flush: Boolean = true) {
        if (!isTtsInitialized) {
            pendingSpeechOnTtsInit = text
            return
        }
        val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts?.speak(text, queueMode, null, "utterance_${System.currentTimeMillis()}")
    }

    fun speakThenListen(
        text: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        stopListening()
        stopSpeaking()
        onSpeechDoneCallback = {
            startListening(onResult, onError)
        }
        if (!isTtsInitialized) {
            pendingSpeechOnTtsInit = text
            // If TTS is not yet ready, start listening directly after short delay
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val action = onSpeechDoneCallback
                onSpeechDoneCallback = null
                action?.invoke()
            }, 1200)
            return
        }
        _isSpeaking.value = true
        val utteranceId = "ask_listen_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

        // Safety fallback timer: if TTS does not trigger onDone within 5 seconds, trigger listen
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val action = onSpeechDoneCallback
            if (action != null) {
                onSpeechDoneCallback = null
                action.invoke()
            }
        }, 5500)
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition not available on this device")
            return
        }

        stopSpeaking()
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            val langTag = when (currentLanguage) {
                AppLanguage.ENGLISH -> "en-US"
                AppLanguage.HINDI -> "hi-IN"
                AppLanguage.TAMIL -> "ta-IN"
                AppLanguage.TELUGU -> "te-IN"
                AppLanguage.KANNADA -> "kn-IN"
                AppLanguage.MALAYALAM -> "ml-IN"
                AppLanguage.BENGALI -> "bn-IN"
                AppLanguage.SPANISH -> "es-ES"
            }
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
                _recognizedText.value = ""
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                _isListening.value = false
            }

            override fun onError(error: Int) {
                _isListening.value = false
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timed out"
                    else -> "Microphone error code: $error"
                }
                onError(errorMsg)
            }

            override fun onResults(results: Bundle?) {
                _isListening.value = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                _recognizedText.value = text
                if (text.isNotBlank()) {
                    onResult(text)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let {
                    _recognizedText.value = it
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
    }

    /**
     * Spoken Language Identification Pipeline:
     * Evaluates natural utterance without forcing the user to say language names.
     */
    fun detectSpokenLanguage(utterance: String): Pair<AppLanguage?, Float> {
        val text = utterance.lowercase().trim()

        // 1. Unicode script detection (100% precision for native scripts)
        var tamilChars = 0
        var devanagariChars = 0
        var teluguChars = 0
        var kannadaChars = 0
        var malayalamChars = 0
        var bengaliChars = 0
        var latinChars = 0

        for (ch in text) {
            val block = Character.UnicodeBlock.of(ch)
            when (block) {
                Character.UnicodeBlock.TAMIL -> tamilChars++
                Character.UnicodeBlock.DEVANAGARI -> devanagariChars++
                Character.UnicodeBlock.TELUGU -> teluguChars++
                Character.UnicodeBlock.KANNADA -> kannadaChars++
                Character.UnicodeBlock.MALAYALAM -> malayalamChars++
                Character.UnicodeBlock.BENGALI -> bengaliChars++
                Character.UnicodeBlock.BASIC_LATIN, Character.UnicodeBlock.LATIN_1_SUPPLEMENT -> latinChars++
                else -> {}
            }
        }

        if (tamilChars > 2) return Pair(AppLanguage.TAMIL, 0.99f)
        if (devanagariChars > 2) return Pair(AppLanguage.HINDI, 0.99f)
        if (teluguChars > 2) return Pair(AppLanguage.TELUGU, 0.99f)
        if (kannadaChars > 2) return Pair(AppLanguage.KANNADA, 0.99f)
        if (malayalamChars > 2) return Pair(AppLanguage.MALAYALAM, 0.99f)
        if (bengaliChars > 2) return Pair(AppLanguage.BENGALI, 0.99f)

        // 2. Keyword & Phonetic Language Detection
        val tamilKeywords = listOf("tamil", "thamizh", "marunthu", "marundu", "enna", "sollu", "vanakkam", "sollunga", "pesu")
        val hindiKeywords = listOf("hindi", "dawa", "samjhao", "namaste", "batao", "ye kya", "kahiye", "dawai", "samjhiye")
        val teluguKeywords = listOf("telugu", "mandhu", "mandu", "emiti", "cheppandi", "namaskaram")
        val kannadaKeywords = listOf("kannada", "aushadhi", "enu", "heli", "namaskara")
        val malayalamKeywords = listOf("malayalam", "marannu", "enthannu", "parayu", "namaskaram")
        val bengaliKeywords = listOf("bengali", "bangla", "aushudh", "ki", "bolun", "namashkar")
        val spanishKeywords = listOf("spanish", "espanol", "medicina", "pastilla", "que es", "hola", "por favor")
        val englishKeywords = listOf("english", "medicine", "pill", "tablet", "read", "scan", "what is")

        fun countMatches(keywords: List<String>): Int =
            keywords.count { text.contains(it) }

        val matches = mapOf(
            AppLanguage.TAMIL to countMatches(tamilKeywords),
            AppLanguage.HINDI to countMatches(hindiKeywords),
            AppLanguage.TELUGU to countMatches(teluguKeywords),
            AppLanguage.KANNADA to countMatches(kannadaKeywords),
            AppLanguage.MALAYALAM to countMatches(malayalamKeywords),
            AppLanguage.BENGALI to countMatches(bengaliKeywords),
            AppLanguage.SPANISH to countMatches(spanishKeywords),
            AppLanguage.ENGLISH to countMatches(englishKeywords)
        )

        val best = matches.maxByOrNull { it.value }
        return if (best != null && best.value > 0) {
            val confidence = if (best.value >= 2) 0.95f else 0.75f
            Pair(best.key, confidence)
        } else {
            // Default confidence low
            Pair(null, 0.2f)
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}
