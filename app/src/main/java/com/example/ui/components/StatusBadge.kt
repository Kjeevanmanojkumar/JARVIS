package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisRed

@Composable
fun StatusBadge(
    status: AssistantVisualState,
    modifier: Modifier = Modifier,
    customText: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_dot")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    val (badgeColor, label) = when (status) {
        AssistantVisualState.STANDBY -> JarvisCyan.copy(alpha = 0.7f) to "STANDBY"
        AssistantVisualState.LISTENING -> JarvisCyan to "LISTENING"
        AssistantVisualState.THINKING -> JarvisAmber to "THINKING"
        AssistantVisualState.SPEAKING -> JarvisGreen to "SPEAKING"
        AssistantVisualState.INTERRUPTED -> JarvisRed to "INTERRUPTED"
        AssistantVisualState.ERROR -> JarvisRed to "ERROR"
    }

    val displayText = customText ?: label

    Row(
        modifier = modifier
            .background(
                color = badgeColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = badgeColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(if (status != AssistantVisualState.STANDBY) dotAlpha else 0.8f)
                .background(badgeColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = displayText,
            color = badgeColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
