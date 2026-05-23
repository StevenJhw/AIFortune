package com.fortune.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    secondary = Color(0xFF03DAC5),
    background = Color(0xFFF5F5FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFE8E8F0),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color(0xFF444444),
    outline = Color(0xFF7070A0),
    primaryContainer = Color(0xFFE8DEF8)
)

@Composable
fun AiFortuneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        content = content
    )
}
