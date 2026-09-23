package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel

@Composable
fun UniversalVoiceCommander(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isListening by viewModel.voiceAssistant.isListening.collectAsState()
    val isSpeaking by viewModel.voiceAssistant.isSpeaking.collectAsState()
    val recognizedText by viewModel.voiceAssistant.recognizedText.collectAsState()
    val isDialogOpen by viewModel.isVoiceCommandDialogOpen.collectAsState()
    val lastFeedback by viewModel.lastVoiceFeedback.collectAsState()
    val selectedLang by viewModel.selectedLanguage.collectAsState()

    // Pulse animation for listening state
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.28f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micPulseScale"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Transient Command Feedback Banner (Top Overlay)
        AnimatedVisibility(
            visible = lastFeedback != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            lastFeedback?.let { feedback ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("voice_feedback_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "“${feedback.rawSpokenText}”",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = feedback.confirmationText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // 2. Persistent Floating Voice Assistant Action Button (Bottom Right)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 28.dp)
        ) {
            // Animated ripple ring when listening
            if (isListening) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                )
            }

            Surface(
                color = when {
                    isListening -> MaterialTheme.colorScheme.error
                    isSpeaking -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                },
                shape = CircleShape,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (isSpeaking) {
                            viewModel.voiceAssistant.stopSpeaking()
                        } else if (isListening) {
                            viewModel.voiceAssistant.stopListening()
                        } else {
                            viewModel.openVoiceCommandDialog()
                        }
                    }
                    .testTag("universal_voice_fab")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when {
                            isListening -> Icons.Default.Mic
                            isSpeaking -> Icons.Default.VolumeOff
                            else -> Icons.Default.MicNone
                        },
                        contentDescription = "MediVoice Universal Voice Commander",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // 3. Interactive Voice Command Modal Overlay
        if (isDialogOpen) {
            Surface(
                color = Color.Black.copy(alpha = 0.72f),
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { viewModel.closeVoiceCommandDialog() }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        shadowElevation = 16.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = false) {}
                            .testTag("voice_command_sheet")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Drag Handle
                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.RecordVoiceOver,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "MediVoice Assistant",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.closeVoiceCommandDialog() },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Voice Assistant")
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Giant Pulsing Microphone
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(100.dp)
                                    .scale(if (isListening) pulseScale else 1f)
                                    .clip(CircleShape)
                                    .background(
                                        if (isListening) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primaryContainer
                                    )
                                    .clickable {
                                        if (isListening) {
                                            viewModel.voiceAssistant.stopListening()
                                        } else {
                                            viewModel.voiceAssistant.startListening(
                                                onResult = { text ->
                                                    viewModel.handleGeneralVoiceCommand(text)
                                                }
                                            )
                                        }
                                    }
                                    .testTag("voice_sheet_mic_button")
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                                    contentDescription = "Toggle microphone",
                                    tint = if (isListening) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(52.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Status Heading
                            Text(
                                text = if (isListening) "Listening... Speak your command" else "Tap microphone to speak",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )

                            // Live recognized transcript display
                            if (recognizedText.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "“$recognizedText”",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Quick Spoken Command Chips
                            Text(
                                text = "Try saying or tap to command:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.align(Alignment.Start)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            val quickCommands = listOf(
                                "Scan Medicine",
                                "Go Home",
                                "Show History",
                                "Emergency Call",
                                "Repeat Details",
                                "Is it safe?",
                                "How to take?",
                                "Flashlight On",
                                "Change Language",
                                "Help Commands"
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(quickCommands) { cmd ->
                                    SuggestionChip(
                                        onClick = {
                                            viewModel.handleGeneralVoiceCommand(cmd)
                                        },
                                        label = {
                                            Text(cmd, fontWeight = FontWeight.SemiBold)
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Manual voice text simulation input (Guarantees voice commands work anywhere even in emulator)
                            var manualInput by remember { mutableStateOf("") }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = manualInput,
                                    onValueChange = { manualInput = it },
                                    placeholder = { Text("Type or say any command...") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (manualInput.isNotBlank()) {
                                            viewModel.handleGeneralVoiceCommand(manualInput)
                                            manualInput = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.height(54.dp)
                                ) {
                                    Text("Send")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
