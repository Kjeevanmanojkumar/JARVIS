package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.JarvisViewModel
import com.example.ui.components.ArcReactorVisualizer
import com.example.ui.components.AssistantVisualState
import com.example.ui.components.FuturisticHudCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun CallingModeScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val assistantState by viewModel.assistantState.collectAsStateWithLifecycle()
    val audioLevel by viewModel.audioLevel.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val liveTranscript by viewModel.liveTranscript.collectAsStateWithLifecycle()
    val liveConnection by viewModel.liveConnectionStatus.collectAsStateWithLifecycle()
    val isMicMuted by viewModel.isMicMuted.collectAsStateWithLifecycle()

    BackHandler {
        viewModel.endCallingMode()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP: Connection & Telemetry Status Bar
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CALLING PROTOCOL",
                        color = JarvisCyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "GEMINI LIVE AUDIO // HANDS-FREE",
                        color = JarvisTextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                StatusBadge(status = assistantState)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Real-Time Telemetry Bar (Mic, Speaker, Live Conn)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(JarvisSurface)
                    .border(1.dp, JarvisCardBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic Status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mic Status",
                        tint = if (isMicMuted) JarvisRed else JarvisGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isMicMuted) "MIC MUTED" else "MIC ACTIVE",
                        color = if (isMicMuted) JarvisRed else JarvisGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Speaker Status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Speaker Status",
                        tint = if (assistantState == AssistantVisualState.SPEAKING) JarvisCyanBright else JarvisTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (assistantState == AssistantVisualState.SPEAKING) "SPEAKER ON" else "SPEAKER READY",
                        color = if (assistantState == AssistantVisualState.SPEAKING) JarvisCyanBright else JarvisTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Gemini Live Connection
                Text(
                    text = liveConnection.uppercase(),
                    color = if (liveConnection.contains("Connected")) JarvisCyan else JarvisAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // CENTER: Futuristic Arc Reactor & Live Waveform
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            ArcReactorVisualizer(
                visualState = assistantState,
                audioLevel = audioLevel,
                size = 250.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // State Readout
            Text(
                text = when (assistantState) {
                    AssistantVisualState.LISTENING -> "HEARING YOUR VOICE..."
                    AssistantVisualState.THINKING -> "PROCESSING INTENT..."
                    AssistantVisualState.SPEAKING -> "JARVIS IS RESPONDING..."
                    AssistantVisualState.INTERRUPTED -> "INTERRUPTED // LISTENING..."
                    else -> statusMessage.uppercase()
                },
                color = when (assistantState) {
                    AssistantVisualState.LISTENING -> JarvisCyanBright
                    AssistantVisualState.THINKING -> JarvisAmber
                    AssistantVisualState.SPEAKING -> JarvisCyan
                    AssistantVisualState.INTERRUPTED, AssistantVisualState.ERROR -> JarvisRed
                    else -> JarvisTextSecondary
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Live Transcript Card
            FuturisticHudCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CONVERSATION ACTIVITY",
                        color = JarvisTextMuted,
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (liveTranscript.isNotBlank()) "\"$liveTranscript\"" else "Speak naturally. JARVIS will answer and listen automatically.",
                        color = if (liveTranscript.isNotBlank()) JarvisTextPrimary else JarvisTextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // BOTTOM: Hands-Free Calling Controls (Stop, Mute, End Call)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Say \"JARVIS, stop\" or tap STOP at any time to interrupt.",
                color = JarvisTextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic Mute Toggle
                IconButton(
                    onClick = { viewModel.toggleMicMute() },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isMicMuted) JarvisRed.copy(alpha = 0.2f) else JarvisSurface)
                        .border(1.dp, if (isMicMuted) JarvisRed else JarvisCardBorder, CircleShape)
                        .testTag("calling_mute_button")
                ) {
                    Icon(
                        imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Toggle Mute",
                        tint = if (isMicMuted) JarvisRed else JarvisCyan
                    )
                }

                // Emergency Barge-in / STOP Button
                Button(
                    onClick = { viewModel.handleStopCommand() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JarvisAmber,
                        contentColor = Color(0xFF261900)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .height(56.dp)
                        .testTag("calling_stop_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "STOP",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // End Calling Mode Button
                IconButton(
                    onClick = { viewModel.endCallingMode() },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(JarvisRed)
                        .testTag("calling_end_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
