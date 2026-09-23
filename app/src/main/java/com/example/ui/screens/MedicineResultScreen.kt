package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ConfidenceLevel
import com.example.ui.MainViewModel
import com.example.ui.ScreenState
import com.example.ui.components.AccessibleBigButton
import com.example.ui.components.AccessibleVoiceBar
import com.example.ui.components.ExpiryStatusBadge

@Composable
fun MedicineResultScreen(viewModel: MainViewModel) {
    val result by viewModel.currentMedicineResult.collectAsState()
    val isListening by viewModel.voiceAssistant.isListening.collectAsState()
    val isSpeaking by viewModel.voiceAssistant.isSpeaking.collectAsState()
    val recognizedText by viewModel.voiceAssistant.recognizedText.collectAsState()
    val followUpAnswers by viewModel.followUpAnswers.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Verified Reference, 1: Package Printed
    val scrollState = rememberScrollState()

    if (result == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val medicine = result!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("medicine_result_screen")
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(ScreenState.HOME) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("result_back_button")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back to home", modifier = Modifier.size(28.dp))
            }

            Text(
                text = "Medicine Analysis",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = {
                    viewModel.voiceAssistant.speak(
                        "Identified: ${medicine.medicineName}. Active ingredient: ${medicine.activeIngredient}. Strength: ${medicine.strength}. ${medicine.expiryMessage}."
                    )
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Read aloud",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Drug Interaction Alert (if any)
            if (medicine.potentialInteractions.isNotEmpty()) {
                for (interaction in medicine.potentialInteractions) {
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(2.5.dp, Color(0xFFB71C1C)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFB71C1C), modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "DRUG INTERACTION WARNING",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFB71C1C)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = interaction.warning,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF491212)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Action: ${interaction.clinicalAction}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB71C1C)
                            )
                        }
                    }
                }
            }

            // Expiry Status Badge (with icon & text, never color alone)
            ExpiryStatusBadge(
                status = medicine.expiryStatus,
                formattedDate = medicine.expiryDateString
            )

            // Primary Medicine Card
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = medicine.medicineName,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = medicine.activeIngredient,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Confidence Indicator
                        Surface(
                            color = when (medicine.verificationEvidence.confidenceLevel) {
                                ConfidenceLevel.HIGH -> Color(0xFFE8F5E9)
                                ConfidenceLevel.MEDIUM -> Color(0xFFFFF3E0)
                                ConfidenceLevel.LOW -> Color(0xFFFFEBEE)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${medicine.verificationEvidence.confidenceLevel} CONFIDENCE",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = when (medicine.verificationEvidence.confidenceLevel) {
                                ConfidenceLevel.HIGH -> Color(0xFF1B5E20)
                                ConfidenceLevel.MEDIUM -> Color(0xFFE65100)
                                ConfidenceLevel.LOW -> Color(0xFFB71C1C)
                            },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Strength", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            Text(medicine.strength, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Batch / Lot", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            Text(medicine.batchNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Manufacturer", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            Text(medicine.manufacturer.take(14), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Tab Selection: Separation of Package-Derived vs Verified Reference
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                    .padding(4.dp)
            ) {
                TabButton(
                    title = "Verified Reference",
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    title = "Printed on Package",
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                )
            }

            if (selectedTab == 0) {
                // VERIFIED REFERENCE INFORMATION
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoCard(
                        title = "General Clinical Uses",
                        icon = Icons.Default.MedicalServices,
                        content = medicine.generalUses.joinToString(" • ")
                    )

                    InfoCard(
                        title = "Important Safety Warnings",
                        icon = Icons.Default.ErrorOutline,
                        content = medicine.importantWarnings.joinToString("\n• "),
                        isAlert = true
                    )

                    InfoCard(
                        title = "Age Precautions",
                        icon = Icons.Default.Elderly,
                        content = medicine.ageGuidelines
                    )

                    InfoCard(
                        title = "Pregnancy & Breastfeeding",
                        icon = Icons.Default.PregnantWoman,
                        content = medicine.pregnancyBreastfeeding
                    )

                    InfoCard(
                        title = "Common Side Effects",
                        icon = Icons.Default.Info,
                        content = "${medicine.commonSideEffects.joinToString(", ")}. Serious reactions: ${medicine.seriousSideEffects.joinToString(", ")}."
                    )

                    InfoCard(
                        title = "Storage Instructions",
                        icon = Icons.Default.Inventory2,
                        content = medicine.storageInstructions
                    )
                }
            } else {
                // PACKAGE-DERIVED INFORMATION
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Literal Printed Text from Packaging:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = medicine.packageRawText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Source: High-resolution camera scan & OCR verification",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Natural Follow-Up Voice Q&A Bar
            Text(
                text = "Ask a Follow-Up Question",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Quick Question Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val suggestedQuestions = listOf(
                    "What is this medicine used for?",
                    "When should I take it?",
                    "What are the side effects?",
                    "Is it safe for older adults?",
                    "Can pregnant women take this?"
                )
                items(suggestedQuestions) { question ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.clickable {
                            viewModel.askFollowUpQuestion(question)
                        }
                    ) {
                        Text(
                            text = question,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Voice Bar for Q&A and Voice Commands
            AccessibleVoiceBar(
                isListening = isListening,
                isSpeaking = isSpeaking,
                transcript = recognizedText,
                promptText = "Tap mic to ask questions or give voice commands",
                onMicClick = {
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
            )

            // Display Previous Follow-Up Q&A
            for (qa in followUpAnswers) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Q: ${qa.question}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = qa.answer,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // PDF Medical Report Button
            AccessibleBigButton(
                text = "Generate Medicine PDF Report",
                icon = Icons.Default.PictureAsPdf,
                onClick = {
                    val file = viewModel.generatePdfReport()
                    if (file != null) {
                        viewModel.sharePdfReport(file)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                subtext = "Export accessibility & clinical report",
                testTag = "generate_pdf_button"
            )

            // Scan Another Medicine Button
            OutlinedButton(
                onClick = { viewModel.navigateTo(ScreenState.SCAN) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .testTag("scan_another_button")
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Another Medicine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            // Legal Healthcare Safety Disclaimer (Mandatory)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "Medical Disclaimer: This application provides medicine information and accessibility assistance. It does not replace advice from a doctor or pharmacist. Never change or stop prescribed medication without consulting a healthcare professional.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun TabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun InfoCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: String,
    isAlert: Boolean = false
) {
    Surface(
        color = if (isAlert) Color(0xFFFFF8E1) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isAlert) Color(0xFFFFB300) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isAlert) Color(0xFFE65100) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isAlert) Color(0xFFE65100) else MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
