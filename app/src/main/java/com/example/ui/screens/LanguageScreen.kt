package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.ui.MainViewModel
import com.example.ui.components.AccessibleBigButton
import com.example.ui.components.AccessibleVoiceBar

@Composable
fun LanguageScreen(viewModel: MainViewModel) {
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val isListening by viewModel.voiceAssistant.isListening.collectAsState()
    val isSpeaking by viewModel.voiceAssistant.isSpeaking.collectAsState()
    val recognizedText by viewModel.voiceAssistant.recognizedText.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .testTag("language_selection_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header Prompt
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Voice prompt",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Speak naturally in your language, or tap below.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Voice Bar
            AccessibleVoiceBar(
                isListening = isListening,
                isSpeaking = isSpeaking,
                transcript = recognizedText,
                promptText = "Tap mic and say your language or speak naturally",
                onMicClick = {
                    if (isListening) {
                        viewModel.voiceAssistant.stopListening()
                    } else {
                        viewModel.voiceAssistant.startListening(
                            onResult = { text ->
                                viewModel.onUserSpokeLanguage(text)
                            }
                        )
                    }
                },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Select Your Language",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Large Touch Target Language Cards
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
            ) {
                items(AppLanguage.entries) { lang ->
                    val isSelected = lang == selectedLanguage
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = if (isSelected) 8.dp else 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .border(
                                width = if (isSelected) 3.dp else 1.5.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable {
                                viewModel.selectLanguage(lang)
                                viewModel.voiceAssistant.speak(lang.welcomeVoice)
                            }
                            .testTag("lang_button_${lang.code}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = lang.nativeName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = lang.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Major Next Button
        AccessibleBigButton(
            text = "Continue in ${selectedLanguage.nativeName}",
            icon = Icons.Default.ArrowForward,
            onClick = { viewModel.confirmLanguageAndProceed() },
            subtext = "Tap to setup your profile",
            testTag = "continue_language_button"
        )
    }
}
