package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.GeminiConfig
import com.example.ui.JarvisViewModel
import com.example.ui.components.FuturisticHudCard
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun SettingsScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val storedApiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val liveModel by viewModel.liveModel.collectAsStateWithLifecycle()
    val textModel by viewModel.textModel.collectAsStateWithLifecycle()
    val voiceName by viewModel.voiceName.collectAsStateWithLifecycle()
    val wakeWordEnabled by viewModel.wakeWordEnabled.collectAsStateWithLifecycle()
    val wakeWordPhrase by viewModel.wakeWordPhrase.collectAsStateWithLifecycle()
    val wakeWordSensitivity by viewModel.wakeWordSensitivity.collectAsStateWithLifecycle()
    val personality by viewModel.personality.collectAsStateWithLifecycle()
    val memoryEnabled by viewModel.memoryEnabled.collectAsStateWithLifecycle()
    val continuousCalling by viewModel.continuousCallingEnabled.collectAsStateWithLifecycle()
    val bargeInEnabled by viewModel.bargeInEnabled.collectAsStateWithLifecycle()
    val speechSpeed by viewModel.speechSpeed.collectAsStateWithLifecycle()
    val testResult by viewModel.testResult.collectAsStateWithLifecycle()
    val isTestingKey by viewModel.isTestingKey.collectAsStateWithLifecycle()
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.collectAsStateWithLifecycle()

    var keyInput by remember(storedApiKey) { mutableStateOf(storedApiKey) }
    var showKeyText by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        item {
            Text(
                text = "SYSTEM CONFIGURATION",
                color = JarvisCyan,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "CORE AI BRAIN, AUTOMATION & INTERFACES",
                color = JarvisTextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // 1. AI BRAIN & API KEY
        item {
            FuturisticHudCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. GEMINI AI BRAIN CONFIGURATION",
                        color = JarvisCyanBright,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Gemini API Key (Masked & Stored Locally):",
                        color = JarvisTextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input"),
                        placeholder = { Text("Enter Gemini API Key...", color = JarvisTextMuted) },
                        visualTransformation = if (showKeyText) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showKeyText = !showKeyText }) {
                                Icon(
                                    imageVector = if (showKeyText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Key Visibility",
                                    tint = JarvisTextMuted
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisCardBorder,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.saveApiKey(keyInput) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_api_key_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisCyan,
                                contentColor = Color(0xFF001E2B)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("SAVE KEY", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { viewModel.testApiKey() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_api_key_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisSurfaceVariant,
                                contentColor = JarvisCyan
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isTestingKey) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = JarvisCyan, strokeWidth = 2.dp)
                            } else {
                                Text("TEST KEY", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        IconButton(
                            onClick = {
                                keyInput = ""
                                viewModel.deleteApiKey()
                            },
                            modifier = Modifier.testTag("delete_api_key_button")
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Key", tint = JarvisRed)
                        }
                    }

                    if (testResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (testResult!!.isSuccess) JarvisGreen.copy(alpha = 0.15f) else JarvisRed.copy(alpha = 0.15f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = if (testResult!!.isSuccess) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (testResult!!.isSuccess) JarvisGreen else JarvisRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = testResult!!.message,
                                color = if (testResult!!.isSuccess) JarvisGreen else JarvisRed,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Model Configurations
                    Text(text = "Live Voice Model:", color = JarvisTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = liveModel,
                        onValueChange = { viewModel.setLiveModel(it) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisCardBorder,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "Text & Automation Brain Model:", color = JarvisTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = textModel,
                        onValueChange = { viewModel.setTextModel(it) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisCardBorder,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // 2. VOICE & CALLING MODE
        item {
            FuturisticHudCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "2. GEMINI LIVE VOICE & CALLING",
                        color = JarvisCyanBright,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Selected Gemini Live Voice:", color = JarvisTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GeminiConfig.AVAILABLE_VOICES.forEach { v ->
                            val isSelected = voiceName == v
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) JarvisCyan else JarvisSurfaceVariant,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) JarvisCyanBright else JarvisCardBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                TextButton(onClick = { viewModel.setVoiceName(v) }) {
                                    Text(
                                        text = v,
                                        color = if (isSelected) Color(0xFF001E2B) else JarvisTextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Continuous Hands-Free Calling", color = JarvisTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Listen -> Think -> Speak -> Listen continuously", color = JarvisTextMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = continuousCalling,
                            onCheckedChange = { viewModel.setContinuousCallingEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = JarvisCyan, checkedTrackColor = Color(0xFF00364F))
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Barge-in / User Interruption", color = JarvisTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Stop AI immediately when user starts speaking or says Stop", color = JarvisTextMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = bargeInEnabled,
                            onCheckedChange = { viewModel.setBargeInEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = JarvisCyan, checkedTrackColor = Color(0xFF00364F))
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Speech Playback Speed: ${String.format("%.1fx", speechSpeed)}", color = JarvisTextSecondary, fontSize = 12.sp)
                    Slider(
                        value = speechSpeed,
                        onValueChange = { viewModel.setSpeechSpeed(it) },
                        valueRange = 0.75f..1.5f,
                        steps = 5,
                        colors = SliderDefaults.colors(thumbColor = JarvisCyan, activeTrackColor = JarvisCyan)
                    )
                }
            }
        }

        // 3. WAKE WORD
        item {
            FuturisticHudCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "3. WAKE WORD ENGINE",
                        color = JarvisCyanBright,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Wake Word Activation", color = JarvisTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Listen for wake phrase to activate JARVIS", color = JarvisTextMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = wakeWordEnabled,
                            onCheckedChange = { viewModel.setWakeWordEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = JarvisCyan, checkedTrackColor = Color(0xFF00364F))
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Wake Phrase:", color = JarvisTextSecondary, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf("JARVIS", "Hey JARVIS").forEach { phrase ->
                            val isSelected = wakeWordPhrase.equals(phrase, ignoreCase = true)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.setWakeWordPhrase(phrase) },
                                    colors = RadioButtonDefaults.colors(selectedColor = JarvisCyan)
                                )
                                Text(phrase, color = JarvisTextPrimary, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Sensitivity: ${(wakeWordSensitivity * 100).toInt()}%", color = JarvisTextSecondary, fontSize = 12.sp)
                    Slider(
                        value = wakeWordSensitivity,
                        onValueChange = { viewModel.setWakeWordSensitivity(it) },
                        valueRange = 0.3f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = JarvisCyan, activeTrackColor = JarvisCyan)
                    )
                }
            }
        }

        // 4. PERSONALITY MODES
        item {
            FuturisticHudCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "4. PERSONALITY PROTOCOL",
                        color = JarvisCyanBright,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    GeminiConfig.PersonalityMode.values().forEach { mode ->
                        val isSelected = personality == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.setPersonality(mode) },
                                colors = RadioButtonDefaults.colors(selectedColor = JarvisCyan)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = mode.displayName,
                                    color = if (isSelected) JarvisCyan else JarvisTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = when (mode) {
                                        GeminiConfig.PersonalityMode.NORMAL -> "Helpful, respectful, highly intelligent assistant."
                                        GeminiConfig.PersonalityMode.SERIOUS -> "Tactical, direct, zero-fluff mission focus."
                                        GeminiConfig.PersonalityMode.COMPANION -> "Tony Stark's butler: witty, dry humor, sophisticated."
                                    },
                                    color = JarvisTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. MEMORY
        item {
            FuturisticHudCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "5. LONG-TERM MEMORY SYSTEM",
                        color = JarvisCyanBright,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Memory Retention", color = JarvisTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Inject saved facts and preferences into Gemini context", color = JarvisTextMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = memoryEnabled,
                            onCheckedChange = { viewModel.setMemoryEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = JarvisAmber, checkedTrackColor = Color(0xFF473100))
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.clearAllMemories() },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceVariant, contentColor = JarvisRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("CLEAR ALL JARVIS MEMORY", fontSize = 12.sp)
                    }
                }
            }
        }

        // 6. ACCESSIBILITY & AUTOMATION
        item {
            FuturisticHudCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "6. ACCESSIBILITY & PHONE CONTROL",
                        color = JarvisCyanBright,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Accessibility Service Status:", color = JarvisTextSecondary, fontSize = 12.sp)
                            Text(
                                text = if (isAccessibilityEnabled) "ENABLED (ACTIVE)" else "DISABLED",
                                color = if (isAccessibilityEnabled) JarvisGreen else JarvisAmber,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.openAccessibilitySettings()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAccessibilityEnabled) JarvisSurfaceVariant else JarvisCyan,
                                contentColor = if (isAccessibilityEnabled) JarvisCyan else Color(0xFF001E2B)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("OPEN SETTINGS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Required for screen reading ('Read Screen') and screen interaction (tap, scroll, type) on other Android apps.",
                        color = JarvisTextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // 7. SECURITY & CONFIRMATIONS
        item {
            FuturisticHudCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "7. SECURITY & PERMISSIONS",
                            color = JarvisCyanBright,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "- Never hard-code or expose API keys in logs or UI.\n" +
                                "- Audio capture is only active during Calling Mode or Wake Word.\n" +
                                "- Screen control requires user-granted Android Accessibility permissions.\n" +
                                "- Sensitive communication actions (phone calls & SMS) prepare drafts and require confirmation.",
                        color = JarvisTextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
