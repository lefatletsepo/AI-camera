package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkSoulsColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = ObsidianBlack,
    primaryContainer = CharcoalSurfaceElevated,
    onPrimaryContainer = PureWhite,
    secondary = ProGold,
    onSecondary = ObsidianBlack,
    secondaryContainer = GraphiteBorder,
    onSecondaryContainer = SoftSilver,
    tertiary = AccentGold,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = PureWhite,
    surface = CharcoalSurface,
    onSurface = PureWhite,
    surfaceVariant = CharcoalSurfaceElevated,
    onSurfaceVariant = SoftSilver,
    outline = GraphiteBorder,
    outlineVariant = GraphiteDivider,
    error = RecordingRed,
    onError = PureWhite
)

@Composable
fun DarkSoulsCameraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkSoulsColorScheme,
        typography = Typography,
        content = content
    )
}
