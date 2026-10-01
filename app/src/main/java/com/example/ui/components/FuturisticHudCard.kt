package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisCardBorderGlow
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisSurface

@Composable
fun FuturisticHudCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    glowEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    val borderColor = if (glowEnabled) JarvisCyan.copy(alpha = 0.6f) else JarvisCardBorder

    Surface(
        modifier = modifier
            .border(
                BorderStroke(
                    width = if (glowEnabled) 1.5.dp else 1.dp,
                    color = borderColor
                ),
                shape = RoundedCornerShape(cornerRadius)
            ),
        shape = RoundedCornerShape(cornerRadius),
        color = JarvisSurface.copy(alpha = 0.85f),
        content = content
    )
}
