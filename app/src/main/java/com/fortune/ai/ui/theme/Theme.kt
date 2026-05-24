package com.fortune.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DarkMysticScheme = darkColorScheme(
    primary = MysticGold,
    onPrimary = MysticBlack,
    primaryContainer = MysticMediumPurple,
    onPrimaryContainer = MysticLightGold,
    secondary = GlowPurple,
    onSecondary = Color.White,
    secondaryContainer = MysticPurple,
    onSecondaryContainer = TextPrimary,
    tertiary = MysticLightGold,
    onTertiary = MysticBlack,
    background = MysticBlack,
    onBackground = TextPrimary,
    surface = MysticDarkPurple,
    onSurface = TextPrimary,
    surfaceVariant = MysticPurple,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    outlineVariant = SectionDivider
)

private val MysticTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.5.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.3.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.15.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.3.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun AiFortuneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkMysticScheme,
        typography = MysticTypography,
        content = content
    )
}
