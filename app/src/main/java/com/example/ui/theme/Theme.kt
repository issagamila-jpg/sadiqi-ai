package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SadiqiColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color.Black,
    primaryContainer = DarkAccentCyan,
    onPrimaryContainer = CyberCyan,
    secondary = CyberCyanDark,
    onSecondary = Color.Black,
    tertiary = WarnAmber,
    onTertiary = Color.Black,
    background = SlateDarkBackground,
    onBackground = IceWhite,
    surface = SlateCardSurface,
    onSurface = IceWhite,
    error = DeepCoralRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to gorgeous dark mode
    dynamicColor: Boolean = false, // Enforce our custom beautiful dark palette
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SadiqiColorScheme,
        typography = Typography,
        content = content
    )
}
