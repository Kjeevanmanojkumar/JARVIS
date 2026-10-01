package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisRed
import kotlin.math.cos
import kotlin.math.sin

enum class AssistantVisualState {
    STANDBY,
    LISTENING,
    THINKING,
    SPEAKING,
    INTERRUPTED,
    ERROR
}

@Composable
fun ArcReactorVisualizer(
    modifier: Modifier = Modifier,
    visualState: AssistantVisualState = AssistantVisualState.STANDBY,
    audioLevel: Float = 0f,
    size: Dp = 220.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "reactor_anim")

    // Continuous rotation for outer tech ring
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rot"
    )

    // Reverse rotation for inner ring
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rot"
    )

    // Pulsing core animation
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    val primaryColor = when (visualState) {
        AssistantVisualState.STANDBY -> JarvisCyan.copy(alpha = 0.65f)
        AssistantVisualState.LISTENING -> JarvisCyanBright
        AssistantVisualState.THINKING -> JarvisAmber
        AssistantVisualState.SPEAKING -> JarvisCyan
        AssistantVisualState.INTERRUPTED -> JarvisRed
        AssistantVisualState.ERROR -> JarvisRed
    }

    val secondaryColor = when (visualState) {
        AssistantVisualState.THINKING -> Color(0xFFFF9100)
        AssistantVisualState.INTERRUPTED -> Color(0xFFFF5252)
        AssistantVisualState.ERROR -> Color(0xFFFF5252)
        else -> Color(0xFF00B0FF)
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 2f - 12.dp.toPx()

            // Dynamic expansion based on audio level
            val dynamicBoost = audioLevel * 14.dp.toPx()
            val effectiveRadius = baseRadius + dynamicBoost

            // 1. Ambient Glow Field
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.25f + audioLevel * 0.35f),
                        primaryColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = effectiveRadius * 1.15f
                ),
                radius = effectiveRadius * 1.15f,
                center = center
            )

            // 2. Outer Rotating Tech Ring with Ticks
            rotate(outerRotation, pivot = center) {
                drawCircle(
                    color = primaryColor.copy(alpha = 0.35f),
                    radius = effectiveRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Outer tick segments (12 segments)
                for (i in 0 until 12) {
                    val angle = Math.toRadians((i * 30).toDouble())
                    val startR = effectiveRadius - 6.dp.toPx()
                    val endR = effectiveRadius + 4.dp.toPx()
                    val startX = (center.x + startR * cos(angle)).toFloat()
                    val startY = (center.y + startR * sin(angle)).toFloat()
                    val endX = (center.x + endR * cos(angle)).toFloat()
                    val endY = (center.y + endR * sin(angle)).toFloat()

                    drawLine(
                        color = secondaryColor.copy(alpha = 0.7f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 3. Middle Counter-Rotating Ring with Audio Waves
            rotate(innerRotation, pivot = center) {
                val midRadius = effectiveRadius * 0.75f
                drawCircle(
                    color = secondaryColor.copy(alpha = 0.45f),
                    radius = midRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // 8 Arc segments
                for (i in 0 until 8) {
                    val startAngle = i * 45f + 5f
                    val sweepAngle = 35f
                    drawArc(
                        color = primaryColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - midRadius, center.y - midRadius),
                        size = androidx.compose.ui.geometry.Size(midRadius * 2, midRadius * 2),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // 4. Inner Reactor Chamber
            val innerRadius = effectiveRadius * 0.48f * pulseScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        primaryColor,
                        primaryColor.copy(alpha = 0.15f)
                    ),
                    center = center,
                    radius = innerRadius
                ),
                radius = innerRadius,
                center = center
            )

            drawCircle(
                color = primaryColor,
                radius = innerRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // 5. Center Arc Triangle Crest
            val triSize = innerRadius * 0.65f
            val triPath = Path().apply {
                moveTo(center.x, center.y - triSize)
                lineTo(center.x + triSize * 0.866f, center.y + triSize * 0.5f)
                lineTo(center.x - triSize * 0.866f, center.y + triSize * 0.5f)
                close()
            }
            drawPath(
                path = triPath,
                color = Color.White.copy(alpha = 0.95f)
            )

            // Dynamic Audio Reactive Ring Pulse
            if (audioLevel > 0.05f) {
                val pulseRadius = effectiveRadius * (1f + audioLevel * 0.25f)
                drawCircle(
                    color = primaryColor.copy(alpha = (1f - audioLevel).coerceIn(0.1f, 0.6f)),
                    radius = pulseRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
    }
}
