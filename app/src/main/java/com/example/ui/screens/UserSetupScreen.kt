package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.ui.MainViewModel
import com.example.ui.components.AccessibleBigButton
import kotlinx.coroutines.delay

enum class SetupVoiceStep {
    IDLE,
    ASKING_NAME,
    LISTENING_NAME,
    ASKING_AGE,
    LISTENING_AGE,
    ASKING_PHONE,
    LISTENING_PHONE,
    SAVING,
    DONE
}

@Composable
fun UserSetupScreen(viewModel: MainViewModel) {
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val isListening by viewModel.voiceAssistant.isListening.collectAsState()
    val isSpeaking by viewModel.voiceAssistant.isSpeaking.collectAsState()
    val recognizedText by viewModel.voiceAssistant.recognizedText.collectAsState()

    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var activeStep by remember { mutableStateOf(SetupVoiceStep.IDLE) }
    var statusMessage by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    val prompts = remember(selectedLanguage) {
        getSetupPromptsForLanguage(selectedLanguage)
    }

    // Function to run conversational voice step machine
    fun runVoiceStep(step: SetupVoiceStep) {
        activeStep = step
        when (step) {
            SetupVoiceStep.ASKING_NAME -> {
                statusMessage = prompts.askName
                viewModel.voiceAssistant.speakThenListen(
                    text = prompts.askName,
                    onResult = { speech ->
                        val parsedName = extractNameFromSpeech(speech)
                        name = parsedName
                        viewModel.hapticManager.successFraming()
                        runVoiceStep(SetupVoiceStep.ASKING_AGE)
                    },
                    onError = {
                        statusMessage = "Could not hear name. Tap mic or type below."
                        activeStep = SetupVoiceStep.IDLE
                    }
                )
            }
            SetupVoiceStep.ASKING_AGE -> {
                val formattedPrompt = String.format(prompts.askAge, name.ifBlank { "User" })
                statusMessage = formattedPrompt
                viewModel.voiceAssistant.speakThenListen(
                    text = formattedPrompt,
                    onResult = { speech ->
                        val parsedAge = extractAgeFromSpeech(speech) ?: 70
                        age = parsedAge.toString()
                        viewModel.hapticManager.successFraming()
                        runVoiceStep(SetupVoiceStep.ASKING_PHONE)
                    },
                    onError = {
                        statusMessage = "Could not hear age. Tap mic or type below."
                        activeStep = SetupVoiceStep.IDLE
                    }
                )
            }
            SetupVoiceStep.ASKING_PHONE -> {
                statusMessage = prompts.askPhone
                viewModel.voiceAssistant.speakThenListen(
                    text = prompts.askPhone,
                    onResult = { speech ->
                        val (parsedPhone, isSkip) = extractPhoneFromSpeech(speech)
                        if (!isSkip && !parsedPhone.isNullOrBlank()) {
                            phone = parsedPhone
                        }
                        viewModel.hapticManager.successFraming()
                        runVoiceStep(SetupVoiceStep.SAVING)
                    },
                    onError = {
                        // Default to device bound profile if no number spoken
                        runVoiceStep(SetupVoiceStep.SAVING)
                    }
                )
            }
            SetupVoiceStep.SAVING -> {
                statusMessage = prompts.saving
                viewModel.voiceAssistant.speak(prompts.saving)
                val ageInt = age.toIntOrNull() ?: 70
                if (phone.isNotBlank()) {
                    viewModel.registerUserWithVoice(name.ifBlank { "User" }, ageInt, phone)
                } else {
                    viewModel.registerWithoutPhoneNumber()
                }
            }
            else -> {}
        }
    }

    // Auto-start conversational voice setup when entering screen
    LaunchedEffect(Unit) {
        delay(600)
        runVoiceStep(SetupVoiceStep.ASKING_NAME)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .verticalScroll(scrollState)
            .testTag("user_setup_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = prompts.screenTitle,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = prompts.screenSubtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Voice Interaction Live Card
            Surface(
                color = if (isListening) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .clickable {
                        if (isListening) {
                            viewModel.voiceAssistant.stopListening()
                            activeStep = SetupVoiceStep.IDLE
                        } else {
                            runVoiceStep(SetupVoiceStep.ASKING_NAME)
                        }
                    }
                    .testTag("voice_assistant_setup_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isListening) MaterialTheme.colorScheme.error
                                    else if (isSpeaking) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.Mic else if (isSpeaking) Icons.Default.VolumeUp else Icons.Default.MicNone,
                                contentDescription = "Voice Assistant",
                                tint = if (isListening || isSpeaking) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    isListening -> "Listening... Please speak your answer"
                                    isSpeaking -> "Speaking question..."
                                    else -> "Tap to Start Voice Setup"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isListening) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = statusMessage.ifBlank { "We ask your name, age & phone aloud" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isListening) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }

                    if (recognizedText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Heard: \"$recognizedText\"",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(prompts.nameLabel, style = MaterialTheme.typography.titleMedium) },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(28.dp))
                },
                trailingIcon = {
                    IconButton(onClick = { runVoiceStep(SetupVoiceStep.ASKING_NAME) }) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Speak Name",
                            tint = if (activeStep == SetupVoiceStep.ASKING_NAME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                textStyle = MaterialTheme.typography.titleLarge,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("name_input_field")
            )

            // Age Field
            OutlinedTextField(
                value = age,
                onValueChange = { age = it },
                label = { Text(prompts.ageLabel, style = MaterialTheme.typography.titleMedium) },
                leadingIcon = {
                    Icon(Icons.Default.Cake, contentDescription = null, modifier = Modifier.size(28.dp))
                },
                trailingIcon = {
                    IconButton(onClick = { runVoiceStep(SetupVoiceStep.ASKING_AGE) }) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Speak Age",
                            tint = if (activeStep == SetupVoiceStep.ASKING_AGE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                textStyle = MaterialTheme.typography.titleLarge,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("age_input_field")
            )

            // Phone Field
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(prompts.phoneLabel, style = MaterialTheme.typography.titleMedium) },
                leadingIcon = {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(28.dp))
                },
                trailingIcon = {
                    IconButton(onClick = { runVoiceStep(SetupVoiceStep.ASKING_PHONE) }) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Speak Mobile Number",
                            tint = if (activeStep == SetupVoiceStep.ASKING_PHONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                textStyle = MaterialTheme.typography.titleLarge,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("phone_input_field")
            )

            // "I don't know my number" Card
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.secondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.registerWithoutPhoneNumber()
                    }
                    .padding(vertical = 6.dp)
                    .testTag("no_phone_number_button")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Device Binding",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "“I don't know my number”",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Tap here to skip. We securely link this phone device automatically.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Save & Continue Button
        AccessibleBigButton(
            text = prompts.continueButton,
            icon = Icons.Default.Check,
            onClick = {
                val ageInt = age.toIntOrNull() ?: 70
                viewModel.registerUserWithVoice(name.ifBlank { "User" }, ageInt, phone.ifBlank { null })
            },
            subtext = "Tap to save and continue",
            testTag = "save_profile_button"
        )
    }
}

private data class SetupLanguagePrompts(
    val screenTitle: String,
    val screenSubtitle: String,
    val askName: String,
    val askAge: String,
    val askPhone: String,
    val saving: String,
    val nameLabel: String,
    val ageLabel: String,
    val phoneLabel: String,
    val continueButton: String
)

private fun getSetupPromptsForLanguage(lang: AppLanguage): SetupLanguagePrompts {
    return when (lang) {
        AppLanguage.HINDI -> SetupLanguagePrompts(
            screenTitle = "स्वागत है",
            screenSubtitle = "अपना नाम और उम्र बोलकर या लिखकर बताइए",
            askName = "आपका नाम क्या है? कृपया अपना नाम बताइए।",
            askAge = "धन्यवाद %s। आपकी उम्र कितनी है?",
            askPhone = "आपका मोबाइल नंबर क्या है? आप बिना नंबर भी कह सकते हैं।",
            saving = "विवरण सुरक्षित हो रहे हैं। अगला पेज खुल रहा है।",
            nameLabel = "आपका नाम",
            ageLabel = "आपकी उम्र",
            phoneLabel = "मोबाइल नंबर (वैकल्पिक)",
            continueButton = "आगे बढ़ें"
        )
        AppLanguage.TAMIL -> SetupLanguagePrompts(
            screenTitle = "நல்வரவு",
            screenSubtitle = "உங்கள் பெயர் மற்றும் வயதை வாய்மொழியாகக் கூறுங்கள்",
            askName = "உங்கள் பெயர் என்ன? தயவுசெய்து உங்கள் பெயரைச் சொல்லுங்கள்.",
            askAge = "நன்றி %s. உங்கள் வயது என்ன?",
            askPhone = "உங்கள் மொபைல் எண் என்ன? அல்லது எண் இல்லை என்று கூறலாம்.",
            saving = "உங்கள் விவரங்கள் சேமிக்கப்படுகின்றன. அடுத்த பக்கம் திறக்கிறது.",
            nameLabel = "உங்கள் பெயர்",
            ageLabel = "உங்கள் வயது",
            phoneLabel = "மொபைல் எண் (விருப்பத்தேர்வு)",
            continueButton = "தொடரவும்"
        )
        AppLanguage.TELUGU -> SetupLanguagePrompts(
            screenTitle = "స్వాగతం",
            screenSubtitle = "మీ పేరు మరియు వయస్సును మాట్లాడి నమోదు చేయండి",
            askName = "మీ పేరు ఏమిటి? దయచేసి మీ పేరు చెప్పండి.",
            askAge = "ధన్యవాదాలు %s. మీ వయస్సు ఎంత?",
            askPhone = "మీ మొబైల్ నంబర్ ఏమిటి? లేదా నంబర్ లేదు అని చెప్పవచ్చు.",
            saving = "వివరాలు భద్రపరచబడుతున్నాయి. తదుపరి పేజీ తెరవబడుతుంది.",
            nameLabel = "మీ పేరు",
            ageLabel = "మీ వయస్సు",
            phoneLabel = "మొబైల్ నంబర్",
            continueButton = "కొనసాగించండి"
        )
        AppLanguage.KANNADA -> SetupLanguagePrompts(
            screenTitle = "ಸ್ವಾಗತ",
            screenSubtitle = "ನಿಮ್ಮ ಹೆಸರು ಮತ್ತು ವಯಸ್ಸನ್ನು ಧ್ವನಿಯಲ್ಲಿ ತಿಳಿಸಿ",
            askName = "ನಿಮ್ಮ ಹೆಸರು ಏನು? ದಯವಿಟ್ಟು ನಿಮ್ಮ ಹೆಸರು ಹೇಳಿ.",
            askAge = "ಧನ್ಯವಾದಗಳು %s. ನಿಮ್ಮ ವಯಸ್ಸು ಎಷ್ಟು?",
            askPhone = "ನಿಮ್ಮ ಮೊಬೈಲ್ ಸಂಖ್ಯೆ ಏನು? ಅಥವಾ ಸಂಖ್ಯೆ ಇಲ್ಲ ಎಂದು ಹೇಳಬಹುದು.",
            saving = "ವಿವರಗಳನ್ನು ಉಳಿಸಲಾಗುತ್ತಿದೆ. ಮುಂದಿನ ಪುಟ ತೆರೆಯಲಾಗುತ್ತಿದೆ.",
            nameLabel = "ನಿಮ್ಮ ಹೆಸರು",
            ageLabel = "ನಿಮ್ಮ ವಯಸ್ಸು",
            phoneLabel = "ಮೊಬೈಲ್ ಸಂಖ್ಯೆ",
            continueButton = "ಮುಂದುವರಿಸಿ"
        )
        AppLanguage.MALAYALAM -> SetupLanguagePrompts(
            screenTitle = "സ്വാഗതം",
            screenSubtitle = "നിങ്ങളുടെ പേരും പ്രായവും സംസാരിച്ച് നൽകുക",
            askName = "നിങ്ങളുടെ പേരെന്താണ്? ദയവായി നിങ്ങളുടെ പേര് പറയുക.",
            askAge = "നന്ദി %s. നിങ്ങളുടെ പ്രായം എത്രയാണ്?",
            askPhone = "നിങ്ങളുടെ മൊബൈൽ നമ്പർ എന്താണ്? അല്ലെങ്കിൽ നമ്പർ ഇല്ല എന്ന് പറയാം.",
            saving = "വിവരങ്ങൾ സംരക്ഷിക്കുന്നു. അടുത്ത പേജ് തുറക്കുന്നു.",
            nameLabel = "നിങ്ങളുടെ പേര്",
            ageLabel = "നിങ്ങളുടെ പ്രായം",
            phoneLabel = "മൊബൈൽ നമ്പർ",
            continueButton = "തുടരുക"
        )
        AppLanguage.BENGALI -> SetupLanguagePrompts(
            screenTitle = "স্বাগতম",
            screenSubtitle = "আপনার নাম এবং বয়স মুখে বলুন",
            askName = "আপনার নাম কি? দয়া করে আপনার নাম বলুন।",
            askAge = "ধন্যবাদ %s। আপনার বয়স কত?",
            askPhone = "আপনার মোবাইল নম্বর কি? অথবা নম্বর নেই বলতে পারেন।",
            saving = "বিবরণ সংরক্ষণ করা হচ্ছে। পরবর্তী পৃষ্ঠা খুলছে।",
            nameLabel = "আপনার নাম",
            ageLabel = "আপনার বয়স",
            phoneLabel = "মোবাইল নম্বর",
            continueButton = "এগিয়ে যান"
        )
        AppLanguage.SPANISH -> SetupLanguagePrompts(
            screenTitle = "Bienvenido",
            screenSubtitle = "Diga su nombre y edad en voz alta",
            askName = "¿Cuál es su nombre? Por favor diga su nombre.",
            askAge = "Gracias %s. ¿Cuál es su edad?",
            askPhone = "¿Cuál es su número de teléfono? También puede decir sin número.",
            saving = "¡Detalles guardados! Pasando a la siguiente página.",
            nameLabel = "Su Nombre",
            ageLabel = "Su Edad",
            phoneLabel = "Número de Celular",
            continueButton = "Continuar"
        )
        AppLanguage.ENGLISH -> SetupLanguagePrompts(
            screenTitle = "Welcome",
            screenSubtitle = "Tell us your name, age, and phone number by voice",
            askName = "What is your name? Please speak your name.",
            askAge = "Thank you %s. What is your age?",
            askPhone = "What is your mobile number? You can also say no number to skip.",
            saving = "Saving your details and opening next page.",
            nameLabel = "Your Name",
            ageLabel = "Your Age",
            phoneLabel = "Mobile Number (Optional)",
            continueButton = "Save & Continue"
        )
    }
}

private fun extractNameFromSpeech(speech: String): String {
    val clean = speech.trim()
    val lower = clean.lowercase()
    val prefixes = listOf(
        "my name is", "i am", "this is", "call me", "name is",
        "mera naam hai", "mera naam", "naam hai",
        "en peyar", "enna peyar", "ennoda peyar",
        "naa peru", "naa peru", "nenu", "nanu",
        "ente peru aanu", "ente peru",
        "amar naam",
        "me llamo", "mi nombre es", "soy"
    )
    var result = clean
    for (prefix in prefixes) {
        if (lower.startsWith(prefix)) {
            result = clean.substring(prefix.length).trim()
            break
        }
    }
    val suffixes = listOf("hai", "aanu", "undu", "aagidhe")
    for (suffix in suffixes) {
        if (result.lowercase().endsWith(" $suffix")) {
            result = result.substring(0, result.length - suffix.length - 1).trim()
        }
    }
    val cleaned = result.filter { it.isLetter() || it.isWhitespace() }.trim()
    return cleaned.split(" ").filter { it.isNotBlank() }
        .joinToString(" ") { it.replaceFirstChar { ch -> ch.uppercase() } }
        .ifBlank { speech.trim().replaceFirstChar { it.uppercase() } }
}

private fun extractAgeFromSpeech(speech: String): Int? {
    val directDigits = Regex("""\b(\d{1,3})\b""").find(speech)?.groupValues?.get(1)?.toIntOrNull()
    if (directDigits != null && directDigits in 1..120) {
        return directDigits
    }
    val words = mapOf(
        "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
        "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14, "fifteen" to 15, "sixteen" to 16, "seventeen" to 17, "eighteen" to 18, "nineteen" to 19,
        "twenty" to 20, "twenty five" to 25, "thirty" to 30, "thirty five" to 35, "forty" to 40, "forty five" to 45,
        "fifty" to 50, "fifty five" to 55, "sixty" to 60, "sixty five" to 65, "seventy" to 70, "seventy two" to 72, "seventy five" to 75,
        "eighty" to 80, "eighty five" to 85, "ninety" to 90,
        "ek" to 1, "do" to 2, "teen" to 3, "char" to 4, "paanch" to 5, "che" to 6, "saat" to 7, "aath" to 8, "nau" to 9, "das" to 10,
        "bees" to 20, "pachees" to 25, "tees" to 30, "pentees" to 35, "chalis" to 40, "pachas" to 50, "saath" to 60, "sattar" to 70, "assi" to 80, "nabbe" to 90
    )
    val lower = speech.lowercase()
    for ((word, num) in words) {
        if (lower.contains(word)) return num
    }
    return null
}

private fun extractPhoneFromSpeech(speech: String): Pair<String?, Boolean> {
    val lower = speech.lowercase()
    val skipKeywords = listOf("no", "none", "skip", "don't know", "dont know", "no number", "illa", "illai", "nahi", "ledu", "beda", "illathe", "sin numero")
    if (skipKeywords.any { lower.contains(it) }) {
        return Pair(null, true)
    }
    val digitMap = mapOf(
        "zero" to "0", "one" to "1", "two" to "2", "three" to "3", "four" to "4",
        "five" to "5", "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9"
    )
    var textWithDigits = lower
    for ((word, digit) in digitMap) {
        textWithDigits = textWithDigits.replace(word, digit)
    }
    val digitsOnly = textWithDigits.filter { it.isDigit() }
    return if (digitsOnly.length >= 6) {
        Pair(digitsOnly, false)
    } else {
        Pair(null, true)
    }
}
