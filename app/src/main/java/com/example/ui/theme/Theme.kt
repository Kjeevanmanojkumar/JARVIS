package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = Color(0xFF001E2B),
    primaryContainer = Color(0xFF00364F),
    onPrimaryContainer = JarvisCyanBright,
    secondary = JarvisBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0D47A1),
    onSecondaryContainer = Color(0xFFBBDEFB),
    tertiary = JarvisAmber,
    onTertiary = Color(0xFF261900),
    tertiaryContainer = Color(0xFF473100),
    onTertiaryContainer = Color(0xFFFFD54F),
    background = JarvisBackground,
    onBackground = JarvisTextPrimary,
    surface = JarvisSurface,
    onSurface = JarvisTextPrimary,
    surfaceVariant = JarvisSurfaceVariant,
    onSurfaceVariant = JarvisTextSecondary,
    outline = JarvisCardBorder,
    outlineVariant = Color(0xFF132238),
    error = JarvisRed,
    onError = Color.White
)

@Composable
fun JarvisTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}
