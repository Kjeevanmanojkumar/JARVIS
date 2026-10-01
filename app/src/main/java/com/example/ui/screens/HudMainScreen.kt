package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.JarvisTab
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
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HudMainScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val assistantState by viewModel.assistantState.collectAsStateWithLifecycle()
    val audioLevel by viewModel.audioLevel.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val liveTranscript by viewModel.liveTranscript.collectAsStateWithLifecycle()
    val liveConnection by viewModel.liveConnectionStatus.collectAsStateWithLifecycle()
    val personality by viewModel.personality.collectAsStateWithLifecycle()

    var inputPrompt by remember { mutableStateOf("") }

    val quickCommands = listOf(
        "Open YouTube",
        "Read Screen",
        "Open Settings",
        "Create Note",
        "Adjust Volume Up",
        "Navigate Home"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Futuristic Header Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "J.A.R.V.I.S.",
                        color = JarvisCyan,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 4.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "TACTICAL AI INTERFACE // ${personality.displayName.uppercase()}",
                        color = JarvisTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                StatusBadge(status = assistantState)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 2. Centerpiece: Glowing Arc Reactor Visualizer
        item {
            FuturisticHudCard(
                modifier = Modifier.fillMaxWidth(),
                glowEnabled = assistantState == AssistantVisualState.LISTENING || assistantState == AssistantVisualState.SPEAKING
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ArcReactorVisualizer(
                        visualState = assistantState,
                        audioLevel = audioLevel,
                        size = 210.dp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = statusMessage.uppercase(),
                        color = when (assistantState) {
                            AssistantVisualState.LISTENING -> JarvisCyanBright
                            AssistantVisualState.THINKING -> JarvisAmber
                            AssistantVisualState.SPEAKING -> JarvisCyan
                            AssistantVisualState.INTERRUPTED, AssistantVisualState.ERROR -> JarvisRed
                            else -> JarvisTextSecondary
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    if (liveTranscript.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\"$liveTranscript\"",
                            color = JarvisTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Initiate Continuous Calling Mode Button
                    Button(
                        onClick = { viewModel.setTab(JarvisTab.CALLING) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("initiate_calling_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JarvisCyan,
                            contentColor = Color(0xFF001E2B)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Calling Mode",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "START CONTINUOUS CALLING MODE",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // 3. Quick Command Protocol Chips
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "DIRECT PROTOCOLS",
                    color = JarvisTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickCommands.forEach { cmd ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(JarvisSurface)
                                .clickable {
                                    viewModel.runQuickAction(cmd)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("quick_cmd_${cmd.lowercase().replace(' ', '_')}")
                        ) {
                            Text(
                                text = cmd,
                                color = JarvisCyanBright,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // 4. Input Command Bar
        item {
            FuturisticHudCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputPrompt,
                        onValueChange = { inputPrompt = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hud_prompt_input"),
                        placeholder = {
                            Text(
                                "Command JARVIS...",
                                color = JarvisTextMuted,
                                fontSize = 14.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary,
                            cursorColor = JarvisCyan
                        ),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            if (inputPrompt.isNotBlank()) {
                                viewModel.executeUserPrompt(inputPrompt)
                                inputPrompt = ""
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(JarvisCyan, CircleShape)
                            .testTag("hud_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Command",
                            tint = Color(0xFF001E2B),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            viewModel.handleStopCommand()
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(JarvisRed.copy(alpha = 0.2f), CircleShape)
                            .testTag("hud_stop_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop JARVIS",
                            tint = JarvisRed,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
