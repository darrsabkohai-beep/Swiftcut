package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CapCutColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = ObsidianBlack,
    primaryContainer = DarkCardElevated,
    onPrimaryContainer = NeonCyan,
    secondary = ElectricMint,
    onSecondary = ObsidianBlack,
    secondaryContainer = DarkCard,
    onSecondaryContainer = ElectricMint,
    tertiary = NeonPurple,
    onTertiary = PureWhite,
    background = ObsidianBlack,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = RadiantRed,
    onError = PureWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // CapCut is inherently a sleek dark professional video studio
    dynamicColor: Boolean = false, // Keep the signature CapCut neon cyan & obsidian look
    content: @Composable () -> Unit
) {
    val colorScheme = CapCutColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = ObsidianBlack.toArgb()
                window.navigationBarColor = ObsidianBlack.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
