package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpiryStatus

@Composable
fun AccessibleVoiceBar(
    isListening: Boolean,
    isSpeaking: Boolean,
    transcript: String,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier,
    promptText: String = "Tap to speak or listen"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
            .padding(8.dp)
            .testTag("accessible_voice_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = when {
                        isListening -> "Listening to you... Speak now"
                        isSpeaking -> "Speaking to you aloud..."
                        transcript.isNotBlank() -> "\"$transcript\""
                        else -> promptText
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (transcript.isNotBlank() && !isListening) {
                    Text(
                        text = "Voice command recognized",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(68.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        if (isListening) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primaryContainer
                    )
                    .clickable { onMicClick() }
                    .testTag("voice_mic_button")
            ) {
                Icon(
                    imageVector = when {
                        isListening -> Icons.Default.Mic
                        isSpeaking -> Icons.Default.VolumeUp
                        else -> Icons.Default.MicNone
                    },
                    contentDescription = if (isListening) "Listening active" else "Start voice assistant",
                    tint = if (isListening) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
fun ExpiryStatusBadge(
    status: ExpiryStatus,
    formattedDate: String?,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon, label) = when (status) {
        ExpiryStatus.NOT_EXPIRED -> Quad(
            Color(0xFFE8F5E9),
            Color(0xFF1B5E20),
            Icons.Default.CheckCircle,
            "NOT EXPIRED - SAFE"
        )
        ExpiryStatus.EXPIRING_SOON -> Quad(
            Color(0xFFFFF3E0),
            Color(0xFFE65100),
            Icons.Default.HourglassBottom,
            "EXPIRING SOON - ALERT"
        )
        ExpiryStatus.EXPIRED -> Quad(
            Color(0xFFFFEBEE),
            Color(0xFFB71C1C),
            Icons.Default.Warning,
            "EXPIRED - DO NOT USE"
        )
        ExpiryStatus.UNVERIFIED_MISSING -> Quad(
            Color(0xFFEDE7F6),
            Color(0xFF4A148C),
            Icons.Default.HelpOutline,
            "EXPIRY NOT VERIFIED"
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, textColor),
        modifier = modifier
            .fillMaxWidth()
            .testTag("expiry_badge_${status.name}")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = textColor,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                if (!formattedDate.isNullOrBlank()) {
                    Text(
                        text = "Expiry Date: $formattedDate",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                } else {
                    Text(
                        text = "Check other side of blister strip",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
fun AccessibleBigButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    subtext: String? = null,
    testTag: String = "big_accessible_button"
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 18.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
                if (!subtext.isNullOrBlank()) {
                    Text(
                        text = subtext,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.85f),
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
