package com.example.service

import com.example.data.model.AppLanguage

enum class VoiceActionType {
    SCAN_MEDICINE,
    CAPTURE_PHOTO,
    TORCH_ON,
    TORCH_OFF,
    SWITCH_SIDE,
    GO_HOME,
    GO_HISTORY,
    READ_HISTORY,
    GO_CAREGIVER,
    CALL_EMERGENCY,
    GO_LANGUAGE,
    SET_LANGUAGE,
    GO_PROFILE,
    REPEAT_MEDICINE,
    CHECK_SAFETY,
    CHECK_DOSAGE,
    GENERATE_PDF,
    SCAN_ANOTHER,
    STOP_AUDIO,
    HELP_COMMANDS,
    FOLLOW_UP_QUESTION,
    UNKNOWN
}

data class ParsedVoiceCommand(
    val actionType: VoiceActionType,
    val targetLanguage: AppLanguage? = null,
    val rawText: String,
    val queryText: String = ""
)

object VoiceCommandEngine {

    /**
     * Parses spoken user input across all supported languages (English, Tamil, Hindi,
     * Telugu, Kannada, Malayalam, Bengali, Spanish) and maps to appropriate app action.
     */
    fun parseCommand(input: String, currentScreenName: String = ""): ParsedVoiceCommand {
        val clean = input.trim()
        val lower = clean.lowercase()

        // 1. Audio Stop / Quiet
        if (matchesAny(lower, listOf("stop", "quiet", "be quiet", "shut up", "silence", "pause", "நிறுத்து", "போதும்", "சத்தம் வேண்டாம்", "रुको", "चुप", "शांत", "ఆపు", "ನಿಲ್ಲಿಸು", "നിർത്തുക", "থামো", "alto", "silencio", "callar"))) {
            return ParsedVoiceCommand(VoiceActionType.STOP_AUDIO, rawText = clean)
        }

        // 2. Help / Guide
        if (matchesAny(lower, listOf("help", "what can i say", "commands", "options", "guide", "voice commands", "உதவி", "என்ன பேசலாம்", "வழிகாட்டி", "கட்டளைகள்", "मदद", "क्या बोलूं", "सहायता", "कमांड", "సహాయం", "ఏం మాట్లాడాలి", "ಸಹಾಯ", "സഹായം", "সাহায্য", "ayuda", "instrucciones", "qué puedo decir", "comandos"))) {
            return ParsedVoiceCommand(VoiceActionType.HELP_COMMANDS, rawText = clean)
        }

        // 3. Photo Capture (Shutter)
        if (matchesAny(lower, listOf("capture", "take photo", "take picture", "click photo", "click picture", "shoot", "snap", "take image", "click now", "capture now", "படம் எடு", "போட்டோ எடு", "படம் பிடி", "கிளிக் செய்", "फोटो खींचो", "फोटो लो", "तस्वीर लो", "ఫోటో తీయి", "చిత్రం తీయి", "ಫೋಟೋ ತೆಗೆ", "ഫോട്ടോ എടുക്കുക", "ছবি তুলুন", "tomar foto", "sacar foto", "capturar"))) {
            return ParsedVoiceCommand(VoiceActionType.CAPTURE_PHOTO, rawText = clean)
        }

        // 4. Torch / Flashlight On
        if (matchesAny(lower, listOf("torch on", "flash on", "turn on flash", "turn on torch", "light on", "turn on light", "enable torch", "flashlight on", "டார்ச் போடு", "டார்ச் ஆன்", "லைட் போடு", "வெளிச்சம் போடு", "टॉर्च चालू", "लाइट जलाओ", "टॉर्च ऑन", "ఫ్లాష్ ఆన్", "ಲೈಟ್ ಆನ್", "ലൈറ്റ് ഓൺ", "আলো জ্বালাও", "encender linterna", "luz encendida", "prender linterna"))) {
            return ParsedVoiceCommand(VoiceActionType.TORCH_ON, rawText = clean)
        }

        // 5. Torch / Flashlight Off
        if (matchesAny(lower, listOf("torch off", "flash off", "turn off flash", "turn off torch", "light off", "turn off light", "disable torch", "flashlight off", "டார்ச் ஆப்", "டார்ச் நிறுத்து", "லைட் ஆப்", "टॉर्च बंद", "लाइट बंद", "ఫ్లాష్ ఆఫ్", "ಲೈಟ್ ಆಫ್", "ലൈറ്റ് ഓഫ്", "আলো নিভিয়ে দাও", "apagar linterna", "luz apagada"))) {
            return ParsedVoiceCommand(VoiceActionType.TORCH_OFF, rawText = clean)
        }

        // 6. Flip side (Scan back side / front side)
        if (matchesAny(lower, listOf("back side", "scan back", "flip side", "other side", "turn over", "rear side", "மறுபக்கம்", "பின்பக்கம்", "திருப்பு", "पीछे की तरफ", "दूसरी तरफ", "पलटो", "వెనుక వైపు", "మరో వైపు", "ಹಿಂದಿನ ಬದಿ", "മറുപുറം", "অন্য পাশ", "otro lado", "lado posterior", "voltear"))) {
            return ParsedVoiceCommand(VoiceActionType.SWITCH_SIDE, rawText = clean)
        }

        // 7. Direct Language Switch by name
        val directLang = detectDirectLanguageSwitch(lower)
        if (directLang != null) {
            return ParsedVoiceCommand(VoiceActionType.SET_LANGUAGE, targetLanguage = directLang, rawText = clean)
        }

        // 8. Open Language Screen
        if (matchesAny(lower, listOf("change language", "select language", "language menu", "switch language", "open language", "languages", "மொழி மாற்று", "மொழி", "மொழிகள்", "பாஷை", "भाषा बदलो", "भाषा", "भाषाएं", "భాష మార్చు", "భాష", "ಭಾಷೆ ಬದಲಾಯಿಸಿ", "ಭಾಷೆ", "ഭാഷ മാറ്റുക", "ভাষা পরিবর্তন", "cambiar idioma", "idiomas", "idioma"))) {
            return ParsedVoiceCommand(VoiceActionType.GO_LANGUAGE, rawText = clean)
        }

        // 9. Scan Medicine / Camera
        if (matchesAny(lower, listOf("scan", "scan medicine", "open camera", "camera", "medicine scan", "scan tablet", "scan strip", "take photo of medicine", "check medicine", "read strip", "verify medicine", "ஸ்கேன்", "மருந்து ஸ்கேன்", "கேமரா", "ஸ்கேன் செய்", "மருந்தை பார்", "स्कैन करो", "दवाई स्कैन", "कैमरा खोलो", "स्कैन", "दवाई जांचो", "స్కాన్ చేయి", "మందులు స్కాన్", "కెమెరా", "ಸ್ಕ್ಯಾನ್ ಮಾಡಿ", "ಔಷಧಿ ಸ್ಕ್ಯಾನ್", "ಕ್ಯಾಮೆರಾ", "സ്കാൻ ചെയ്യുക", "മരുന്ന് സ്കാൻ", "ക്യാമറ", "স্ক্যান করুন", "ওষুধ স্ক্যান", "ক্যামেরা", "escanear", "escanear medicina", "abrir cámara", "cámara"))) {
            return ParsedVoiceCommand(VoiceActionType.SCAN_MEDICINE, rawText = clean)
        }

        // 10. Home Screen
        if (matchesAny(lower, listOf("home", "go home", "main screen", "dashboard", "main menu", "home page", "back home", "return home", "start over", "ஹோம்", "முகப்பு", "முதன்மை பக்கம்", "வீடு", "ஹோம்க்கு போ", "होम", "मुख्य पृष्ठ", "घर", "होम पेज", "హోమ్", "ప్రధాన పేజీ", "ಮುಖಪುಟ", "ಹೋಮ್", "ഹോം", "പ്രധാന പേജ്", "হোম", "মূল পাতা", "inicio", "ir al inicio", "pantalla principal", "casa"))) {
            return ParsedVoiceCommand(VoiceActionType.GO_HOME, rawText = clean)
        }

        // 11. Read History Aloud
        if (matchesAny(lower, listOf("read history", "what medicines did i take", "list my medicines", "tell my history", "வரலாற்றை படி", "என் மருந்துகளை சொல்", "इतिहास पढ़ो", "मेरी दवाइयां बताओ", "చరిత్ర చదువు", "leer historial"))) {
            return ParsedVoiceCommand(VoiceActionType.READ_HISTORY, rawText = clean)
        }

        // 12. History Screen
        if (matchesAny(lower, listOf("history", "show history", "my medicines", "past medicines", "saved medicines", "view history", "previous medicines", "medicine records", "medicine log", "yesterday medicine", "வரலாறு", "பழைய மருந்துகள்", "சேமித்த மருந்துகள்", "மருந்து பட்டியல்", "முந்தைய மருந்துகள்", "इतिहास", "पुरानी दवाई", "दवाई की लिस्ट", "मेरी दवाइयां", "दवाई रिकॉर्ड", "చరిత్ర", "పాత మందులు", "మందుల జాబితా", "ಇತಿಹಾಸ", "ಹಳೆಯ ಔಷಧಿಗಳು", "ചരിത്രം", "പഴയ മരുന്നുകൾ", "ইতিহাস", "ওষুধের তালিকা", "historial", "ver historial", "mis medicamentos", "medicamentos guardados"))) {
            return ParsedVoiceCommand(VoiceActionType.GO_HISTORY, rawText = clean)
        }

        // 13. Call Emergency (108 / Doctor)
        if (matchesAny(lower, listOf("call emergency", "call 108", "call ambulance", "call doctor", "dial 108", "phone doctor", "phone family", "அவசர அழைப்பு", "108க்கு கூப்பிடு", "டாக்டருக்கு கூப்பிடு", "ஆம்புலன்ஸ் கூப்பிடு", "इमरजेंसी कॉल", "108 पर कॉल करो", "डॉक्टर को फोन लगाओ", "एम्बुलेंस बुलाओ", "108 కి కాల్ చేయి", "llamar emergencia", "llamar al 108", "llamar ambulancia"))) {
            return ParsedVoiceCommand(VoiceActionType.CALL_EMERGENCY, rawText = clean)
        }

        // 14. Caregiver & Emergency Portal
        if (matchesAny(lower, listOf("caregiver", "emergency", "call family", "help me", "doctor", "ambulance", "safety portal", "sos", "emergency contact", "family contact", "nurse", "பாதுகாப்பாளர்", "அவசரம்", "உதவி", "குடும்பம்", "டாக்டர்", "ஆம்புலன்ஸ்", "துணைவர்", "केयरगिवर", "मदद", "इमरजेंसी", "परिवार", "डॉक्टर", "एम्बुलेंस", "सहायक", "సంరక్షకుడు", "సహాయం", "ఎమర్జెన్సీ", "వైద్యుడు", "ಆರೈಕೆದಾರ", "ತುರ್ತು", "ಸಹಾಯ", "സംരക്ഷകൻ", "അടിയന്തിരം", "സഹായം", "জরুরী", "সাহায্য", "ডাক্তার", "cuidador", "emergencia", "ayuda", "socorro", "contacto de emergencia"))) {
            return ParsedVoiceCommand(VoiceActionType.GO_CAREGIVER, rawText = clean)
        }

        // 15. User Profile / Settings
        if (matchesAny(lower, listOf("profile", "user setup", "my name", "my age", "change name", "my details", "settings", "பெயர் மாற்று", "என் விவரம்", "சுயவிவரம்", "அமைப்புகள்", "नाम बदलो", "मेरी जानकारी", "प्रोफाइल", "సెట్టింగ్స్", "ప్రొఫైల్", "ಸೆಟ್ಟಿಂಗ್ಸ್", "പ്രൊഫൈൽ", "প্রোফাইল", "perfil", "mis datos", "mi nombre"))) {
            return ParsedVoiceCommand(VoiceActionType.GO_PROFILE, rawText = clean)
        }

        // 16. Result Screen: Repeat / Read aloud
        if (matchesAny(lower, listOf("repeat", "read again", "tell me again", "speak again", "read aloud", "what is this medicine", "say again", "tell me details", "மீண்டும் சொல்", "படி", "மறுபடியும் சொல்", "என்ன மருந்து", "தோபாரா போலோ", "फिर से बताओ", "दोबारा बोलो", "यह कौन सी दवाई है", "మళ్ళీ చెప్పు", "మరోసారి చెప్పు", "ಇನ್ನೊಮ್ಮೆ ಹೇಳಿ", "വീണ്ടും പറയുക", "আবার বলুন", "repetir", "leer de nuevo", "decir otra vez", "qué medicina es"))) {
            return ParsedVoiceCommand(VoiceActionType.REPEAT_MEDICINE, rawText = clean)
        }

        // 17. Result Screen: Check Safety / Expiry
        if (matchesAny(lower, listOf("is it safe", "is this safe", "check expiry", "is it expired", "expired or not", "safe or not", "expiry date", "when does it expire", "பாதுகாப்பானதா", "காலாவதி", "காலாவதி தேதி", "பயன்படுத்தலாமா", "सुरक्षित है क्या", "एक्सपायरी क्या है", "क्या यह खराब है", "सुरक्षा", "సురక్షితమేనా", "గడువు తేదీ", "ಸುರಕ್ಷಿತವೇ", "കാലാവധി", "নিরাপদ কি", "es seguro", "está vencido", "fecha de vencimiento"))) {
            return ParsedVoiceCommand(VoiceActionType.CHECK_SAFETY, rawText = clean)
        }

        // 18. Result Screen: Dosage & How to take
        if (matchesAny(lower, listOf("how to take", "dosage", "when to take", "how many tablets", "how many pills", "after food", "before food", "morning or night", "schedule", "instructions", "எப்படி சாப்பிட வேண்டும்", "அளவு", "எப்போது சாப்பிட வேண்டும்", "சாப்பாட்டுக்கு பின்பா", "மாத்திரை அளவு", "कैसा लेना है", "खुराक", "कब् लेना है", "कितनी गोली", "खाने के बाद", "ఎలా తీసుకోవాలి", "మోతాదు", "ఎప్పుడు తీసుకోవాలి", "ಹೇಗೆ ತೆಗೆದುಕೊಳ್ಳಬೇಕು", "എങ്ങനെ കഴിക്കണം", "কীভাবে খেতে হবে", "cómo tomar", "dosis", "cuándo tomar", "después de comer"))) {
            return ParsedVoiceCommand(VoiceActionType.CHECK_DOSAGE, rawText = clean)
        }

        // 19. Result Screen: Generate PDF / Share Report
        if (matchesAny(lower, listOf("pdf", "generate pdf", "download report", "share report", "export report", "medicine report", "medical report", "அறிக்கை", "ரிப்போர்ட்", "பிடிஎப்", "அறிக்கையை பகிர்", "रिपोर्ट", "पीडीएफ", "दवाई रिपोर्ट", "రిపోర్ట్", "ವರದಿ", "റിപ്പോർട്ട്", "রিপোর্ট", "reporte", "descargar reporte", "compartir reporte"))) {
            return ParsedVoiceCommand(VoiceActionType.GENERATE_PDF, rawText = clean)
        }

        // 20. Result Screen: Scan Another
        if (matchesAny(lower, listOf("scan another", "next medicine", "new scan", "scan again", "another one", "next one", "அடுத்த மருந்து", "அடுத்த ஸ்கேன்", "வேறு மருந்து", "अगली दवाई", "दूसरा स्कैन", "नया स्कैन", "మరో మందు", "ಮುಂದಿನ ಔಷಧಿ", "മറ്റൊരു മരുന്ന്", "পরের ওষুধ", "otro medicamento", "siguiente medicina", "nuevo escaneo"))) {
            return ParsedVoiceCommand(VoiceActionType.SCAN_ANOTHER, rawText = clean)
        }

        // If on medicine result screen, interpret as follow-up medical question!
        if (currentScreenName == "MEDICINE_RESULT" || clean.length > 5) {
            return ParsedVoiceCommand(
                VoiceActionType.FOLLOW_UP_QUESTION,
                rawText = clean,
                queryText = clean
            )
        }

        return ParsedVoiceCommand(VoiceActionType.UNKNOWN, rawText = clean)
    }

    private fun detectDirectLanguageSwitch(text: String): AppLanguage? {
        val lower = text.lowercase()
        return when {
            lower.contains("tamil") || lower.contains("தமிழ்") || lower.contains("tamizh") -> AppLanguage.TAMIL
            lower.contains("hindi") || lower.contains("हिंदी") || lower.contains("hindee") -> AppLanguage.HINDI
            lower.contains("telugu") || lower.contains("తెలుగు") -> AppLanguage.TELUGU
            lower.contains("kannada") || lower.contains("ಕನ್ನಡ") -> AppLanguage.KANNADA
            lower.contains("malayalam") || lower.contains("മലയാളം") -> AppLanguage.MALAYALAM
            lower.contains("bengali") || lower.contains("বাংলা") || lower.contains("bangla") -> AppLanguage.BENGALI
            lower.contains("spanish") || lower.contains("español") || lower.contains("espanol") -> AppLanguage.SPANISH
            lower.contains("english") || lower.contains("अंग्रेजी") || lower.contains("ஆங்கிலம்") || lower.contains("ingles") -> AppLanguage.ENGLISH
            else -> null
        }
    }

    private fun matchesAny(text: String, keywords: List<String>): Boolean {
        for (kw in keywords) {
            if (text.contains(kw)) return true
        }
        return false
    }

    /**
     * Spoken audio confirmations in user's active language for immediate verbal feedback.
     */
    fun getSpokenConfirmation(action: VoiceActionType, language: AppLanguage, detail: String = ""): String {
        return when (action) {
            VoiceActionType.SCAN_MEDICINE, VoiceActionType.SCAN_ANOTHER -> when (language) {
                AppLanguage.TAMIL -> "மருந்து ஸ்கேன் கேமரா திறக்கப்படுகிறது."
                AppLanguage.HINDI -> "दवाई स्कैन कैमरा खुल रहा है।"
                AppLanguage.TELUGU -> "మందుల స్కానింగ్ కెమెరా తెరవబడుతుంది."
                AppLanguage.KANNADA -> "ಔಷಧಿ ಸ್ಕ್ಯಾನ್ ಕ್ಯಾಮೆರಾ ತೆರೆಯಲಾಗುತ್ತಿದೆ."
                AppLanguage.MALAYALAM -> "മരുന്ന് സ്കാൻ ക്യാമറ തുറക്കുന്നു."
                AppLanguage.BENGALI -> "ওষুধ স্ক্যান ক্যামেরা খুলছে।"
                AppLanguage.SPANISH -> "Abriendo la cámara de escaneo."
                AppLanguage.ENGLISH -> "Opening camera scanner."
            }
            VoiceActionType.CAPTURE_PHOTO -> when (language) {
                AppLanguage.TAMIL -> "மருந்து புகைப்படம் எடுக்கப்படுகிறது."
                AppLanguage.HINDI -> "दवाई की फोटो ली जा रही है।"
                AppLanguage.SPANISH -> "Tomando foto del medicamento."
                else -> "Taking medicine photo now."
            }
            VoiceActionType.TORCH_ON -> when (language) {
                AppLanguage.TAMIL -> "டார்ச் லைட் போடப்பட்டது."
                AppLanguage.HINDI -> "टॉर्च चालू कर दी गई है।"
                AppLanguage.SPANISH -> "Linterna encendida."
                else -> "Flashlight turned on."
            }
            VoiceActionType.TORCH_OFF -> when (language) {
                AppLanguage.TAMIL -> "டார்ச் லைட் அணைக்கப்பட்டது."
                AppLanguage.HINDI -> "टॉर्च बंद कर दी गई है।"
                AppLanguage.SPANISH -> "Linterna apagada."
                else -> "Flashlight turned off."
            }
            VoiceActionType.SWITCH_SIDE -> when (language) {
                AppLanguage.TAMIL -> "மறுபக்கம் ஸ்கேன் செய்ய மாற்றப்பட்டது."
                AppLanguage.HINDI -> "दवाई का दूसरा भाग स्कैन करने के लिए बदला गया।"
                AppLanguage.SPANISH -> "Cambiado a escaneo del otro lado."
                else -> "Switched to scanning back side."
            }
            VoiceActionType.GO_HOME -> when (language) {
                AppLanguage.TAMIL -> "முகப்பு பக்கத்திற்கு செல்கிறது."
                AppLanguage.HINDI -> "होम पेज पर वापस जा रहे हैं।"
                AppLanguage.TELUGU -> "హోమ్ పేజీకి తిరిగి వెళుతుంది."
                AppLanguage.SPANISH -> "Volviendo a la pantalla principal."
                else -> "Returning to Home."
            }
            VoiceActionType.GO_HISTORY -> when (language) {
                AppLanguage.TAMIL -> "மருந்து வரலாறு திறக்கப்படுகிறது."
                AppLanguage.HINDI -> "दवाई इतिहास खुल रहा है।"
                AppLanguage.TELUGU -> "మందుల చరిత్ర తెరవబడుతుంది."
                AppLanguage.SPANISH -> "Abriendo el historial de medicamentos."
                else -> "Opening medicine history."
            }
            VoiceActionType.GO_CAREGIVER -> when (language) {
                AppLanguage.TAMIL -> "பாதுகாப்பாளர் மற்றும் அவசர உதவி பக்கம் திறக்கப்படுகிறது."
                AppLanguage.HINDI -> "केयरगिवर और आपातकालीन सुरक्षा पोर्टल खुल रहा है।"
                AppLanguage.SPANISH -> "Abriendo portal de cuidadores y emergencias."
                else -> "Opening caregiver and emergency safety portal."
            }
            VoiceActionType.CALL_EMERGENCY -> when (language) {
                AppLanguage.TAMIL -> "அவசர உதவி 108 அழைக்கப்படுகிறது."
                AppLanguage.HINDI -> "आपातकालीन 108 पर कॉल लगाया जा रहा है।"
                AppLanguage.SPANISH -> "Llamando al número de emergencia."
                else -> "Connecting emergency call."
            }
            VoiceActionType.GO_LANGUAGE -> when (language) {
                AppLanguage.TAMIL -> "மொழி தேர்வு பக்கம் திறக்கப்படுகிறது."
                AppLanguage.HINDI -> "भाषा चयन पेज खुल रहा है।"
                AppLanguage.SPANISH -> "Abriendo selección de idioma."
                else -> "Opening language selection."
            }
            VoiceActionType.SET_LANGUAGE -> when (language) {
                AppLanguage.TAMIL -> "மொழி மாற்றப்பட்டது: $detail"
                AppLanguage.HINDI -> "भाषा बदल दी गई है: $detail"
                AppLanguage.SPANISH -> "Idioma cambiado a: $detail"
                else -> "Language changed to $detail."
            }
            VoiceActionType.GO_PROFILE -> when (language) {
                AppLanguage.TAMIL -> "பயனர் விவரம் பக்கம் திறக்கப்படுகிறது."
                AppLanguage.HINDI -> "यूजर प्रोफाइल पेज खुल रहा है।"
                AppLanguage.SPANISH -> "Abriendo perfil de usuario."
                else -> "Opening user profile settings."
            }
            VoiceActionType.GENERATE_PDF -> when (language) {
                AppLanguage.TAMIL -> "மருந்து அறிக்கை பிடிஎப் உருவாக்கப்படுகிறது."
                AppLanguage.HINDI -> "दवाई की पीडीएफ रिपोर्ट तैयार की जा रही है।"
                AppLanguage.SPANISH -> "Generando reporte médico en PDF."
                else -> "Generating accessibility PDF report."
            }
            VoiceActionType.STOP_AUDIO -> when (language) {
                AppLanguage.TAMIL -> "ஆடியோ நிறுத்தப்பட்டது."
                AppLanguage.HINDI -> "ऑडियो रोक दिया गया है।"
                AppLanguage.SPANISH -> "Audio detenido."
                else -> "Audio stopped."
            }
            VoiceActionType.HELP_COMMANDS -> when (language) {
                AppLanguage.TAMIL -> "நீங்கள் பேசலாம்: மருந்து ஸ்கேன், ஹோம், வரலாறு, அவசரம், மீண்டும் சொல், அல்லது மொழி மாற்று."
                AppLanguage.HINDI -> "आप बोल सकते हैं: दवाई स्कैन, होम, इतिहास, इमरजेंसी, दोबारा बताओ, या भाषा बदलो।"
                AppLanguage.TELUGU -> "మీరు మాట్లాడవచ్చు: మందుల స్కాన్, హోమ్, చరిత్ర, ఎమర్జెన్సీ, లేదా మళ్ళీ చెప్పు."
                AppLanguage.SPANISH -> "Puede decir: Escanear medicina, Ir al inicio, Ver historial, Emergencia, o Repetir."
                else -> "You can say: Scan Medicine, Go Home, Show History, Emergency, or Repeat details."
            }
            VoiceActionType.UNKNOWN -> when (language) {
                AppLanguage.TAMIL -> "நீங்கள் கூறியது: $detail. 'ஸ்கேன்', 'ஹோம்', அல்லது 'வரலாறு' என்று கூறலாம்."
                AppLanguage.HINDI -> "मैंने सुना: $detail. आप कह सकते हैं: स्कैन दवाई, होम, या इतिहास।"
                AppLanguage.SPANISH -> "Escuché: $detail. Puede decir: Escanear medicina, Inicio, o Historial."
                else -> "I heard: $detail. Say Scan Medicine, Go Home, or Show History."
            }
            else -> ""
        }
    }
}
