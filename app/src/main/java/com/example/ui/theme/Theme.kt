package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrandGreenPrimary,
    onPrimary = Color(0xFF071120),
    primaryContainer = Color(0xFF0F3A24),
    onPrimaryContainer = BrandGreenPrimary,
    secondary = BrandBlueSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF132D5C),
    onSecondaryContainer = Color(0xFFD6E4FF),
    tertiary = BrandTealTertiary,
    onTertiary = Color(0xFF00201A),
    background = BrandNavyBg,
    onBackground = BrandTextPrimary,
    surface = BrandNavySurface,
    onSurface = BrandTextPrimary,
    surfaceVariant = BrandNavySurfaceLight,
    onSurfaceVariant = BrandTextSecondary,
    outline = BrandBorder,
    outlineVariant = BrandBorderSubtle,
    error = StatusDanger,
    onError = Color.White,
    errorContainer = Color(0xFF480F0F),
    onErrorContainer = Color(0xFFFFB4B4)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent Enterprise Dark theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
