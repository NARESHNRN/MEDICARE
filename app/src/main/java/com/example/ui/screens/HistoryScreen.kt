package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.db.MedicineScanEntity
import com.example.data.model.ExpiryStatus
import com.example.ui.MainViewModel
import com.example.ui.ScreenState
import com.example.ui.components.AccessibleVoiceBar
import com.example.ui.components.ExpiryStatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: MainViewModel) {
    val scanHistory by viewModel.scanHistory.collectAsState()
    val isListening by viewModel.voiceAssistant.isListening.collectAsState()
    val isSpeaking by viewModel.voiceAssistant.isSpeaking.collectAsState()
    val recognizedText by viewModel.voiceAssistant.recognizedText.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var filterOnlyExpiring by remember { mutableStateOf(false) }

    val filteredList = scanHistory.filter { scan ->
        val matchesQuery = searchQuery.isBlank() ||
                scan.medicineName.contains(searchQuery, ignoreCase = true) ||
                scan.activeIngredient.contains(searchQuery, ignoreCase = true)
        val matchesFilter = !filterOnlyExpiring ||
                scan.expiryStatus == "EXPIRED" || scan.expiryStatus == "EXPIRING_SOON"
        matchesQuery && matchesFilter
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("history_screen")
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(ScreenState.HOME) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("history_back_button")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back to home", modifier = Modifier.size(28.dp))
            }

            Text(
                text = "Medicine History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = {
                    val count = scanHistory.size
                    viewModel.voiceAssistant.speak("You have $count medicines in your history. You can tap any medicine to hear details.")
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = "Read summary", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            }
        }

        // Voice Search Bar
        AccessibleVoiceBar(
            isListening = isListening,
            isSpeaking = isSpeaking,
            transcript = recognizedText,
            promptText = "Say “Did I scan Dolo?” or medicine name",
            onMicClick = {
                if (isListening) {
                    viewModel.voiceAssistant.stopListening()
                } else {
                    viewModel.voiceAssistant.startListening(
                        onResult = { text ->
                            val parsed = com.example.service.VoiceCommandEngine.parseCommand(text, "HISTORY")
                            if (parsed.actionType != com.example.service.VoiceActionType.UNKNOWN && parsed.actionType != com.example.service.VoiceActionType.FOLLOW_UP_QUESTION) {
                                viewModel.handleGeneralVoiceCommand(text)
                            } else {
                                searchQuery = text
                                val matched = scanHistory.filter {
                                    it.medicineName.contains(text, ignoreCase = true) || it.activeIngredient.contains(text, ignoreCase = true)
                                }
                                if (matched.isNotEmpty()) {
                                    viewModel.voiceAssistant.speak("Found ${matched.size} matching medicine: ${matched.first().medicineName}. Status: ${matched.first().expiryStatus.replace('_', ' ').lowercase()}.")
                                } else {
                                    viewModel.voiceAssistant.speak("No scanned medicine found matching $text.")
                                }
                            }
                        }
                    )
                }
            },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterChip(
                selected = !filterOnlyExpiring,
                onClick = { filterOnlyExpiring = false },
                label = { Text("All Scans (${scanHistory.size})", style = MaterialTheme.typography.titleMedium) }
            )
            FilterChip(
                selected = filterOnlyExpiring,
                onClick = { filterOnlyExpiring = true },
                label = { Text("Expiring / Alerts", style = MaterialTheme.typography.titleMedium) }
            )
        }

        // Medicine History List
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No medicine found matching “$searchQuery”" else "No saved medicine scans yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredList) { scan ->
                    HistoryItemCard(
                        scan = scan,
                        onSpeak = {
                            viewModel.voiceAssistant.speak(
                                "${scan.medicineName}, ${scan.strength}. Expiry status: ${scan.expiryStatus}. Scanned on ${formatDate(scan.scanDate)}."
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(
    scan: MedicineScanEntity,
    onSpeak: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_${scan.scanId}")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = scan.medicineName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${scan.activeIngredient} • ${scan.strength}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Scanned: ${formatDate(scan.scanDate)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                IconButton(
                    onClick = onSpeak,
                    modifier = Modifier
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Speak details",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val expiryEnum = try {
                ExpiryStatus.valueOf(scan.expiryStatus)
            } catch (e: Exception) {
                ExpiryStatus.UNVERIFIED_MISSING
            }

            ExpiryStatusBadge(
                status = expiryEnum,
                formattedDate = scan.expiryDate
            )
        }
    }
}

private fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.US)
    return formatter.format(Date(millis))
}
