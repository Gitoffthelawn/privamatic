package com.techtrest.privamatic

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Material 3 colour scheme seeded from BritishRacingGreen (#004225).
//
// Every role below was generated with Material Color Utilities
// (com.google.android.material.color.utilities, SchemeTonalSpot, contrast 0.0 -
// the same algorithm Android 12+ uses for dynamic colour), so the secondary,
// tertiary, surface and outline families all derive from the one seed hue.
//
// The single deliberate deviation from the generated output: `primary` is pinned
// to the exact brand greens rather than MCU's tone-40 / tone-80 picks. The top app
// bar and nav-drawer header paint with colorScheme.primary, and the status bar
// icons are forced light on the assumption that surface is a dark green - MCU's
// pale-mint dark primary would break both. `onPrimary` is white in both modes,
// which is what MCU produces for light and the only readable choice on #00854A.
//
// Regenerate by re-running MCU against the seed rather than editing roles by hand.
private val LightColorScheme = lightColorScheme(
    primary = BritishRacingGreen,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFAFF2C4),
    onPrimaryContainer = Color(0xFF002110),
    inversePrimary = Color(0xFF93D5AA),
    secondary = Color(0xFF4E6354),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1E8D5),
    onSecondaryContainer = Color(0xFF0C1F14),
    tertiary = Color(0xFF3B6470),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBFE9F8),
    onTertiaryContainer = Color(0xFF001F27),
    background = Color(0xFFF6FBF4),
    onBackground = Color(0xFF181D19),
    surface = Color(0xFFF6FBF4),
    onSurface = Color(0xFF181D19),
    surfaceVariant = Color(0xFFDCE5DB),
    onSurfaceVariant = Color(0xFF414942),
    surfaceTint = Color(0xFF2A6A47),
    inverseSurface = Color(0xFF2C322D),
    inverseOnSurface = Color(0xFFEDF2EB),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF6E776F),
    outlineVariant = Color(0xFFC0C9C0),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF6FBF4),
    surfaceDim = Color(0xFFD6DBD5),
    surfaceContainer = Color(0xFFEAEFE8),
    surfaceContainerHigh = Color(0xFFE5EAE3),
    surfaceContainerHighest = Color(0xFFDFE4DD),
    surfaceContainerLow = Color(0xFFF0F5EE),
    surfaceContainerLowest = Color(0xFFFFFFFF),
)

private val DarkColorScheme = darkColorScheme(
    primary = BritishRacingGreenDark,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF0B5130),
    onPrimaryContainer = Color(0xFFAFF2C4),
    inversePrimary = Color(0xFF2A6A47),
    secondary = Color(0xFFB5CCBA),
    onSecondary = Color(0xFF213528),
    secondaryContainer = Color(0xFF374B3D),
    onSecondaryContainer = Color(0xFFD1E8D5),
    tertiary = Color(0xFFA3CDDB),
    onTertiary = Color(0xFF033641),
    tertiaryContainer = Color(0xFF224C58),
    onTertiaryContainer = Color(0xFFBFE9F8),
    background = Color(0xFF0F1511),
    onBackground = Color(0xFFDFE4DD),
    surface = Color(0xFF0F1511),
    onSurface = Color(0xFFDFE4DD),
    surfaceVariant = Color(0xFF414942),
    onSurfaceVariant = Color(0xFFC0C9C0),
    surfaceTint = Color(0xFF93D5AA),
    inverseSurface = Color(0xFFDFE4DD),
    inverseOnSurface = Color(0xFF2C322D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8A938B),
    outlineVariant = Color(0xFF414942),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF353B36),
    surfaceDim = Color(0xFF0F1511),
    surfaceContainer = Color(0xFF1C211D),
    surfaceContainerHigh = Color(0xFF262B27),
    surfaceContainerHighest = Color(0xFF313631),
    surfaceContainerLow = Color(0xFF181D19),
    surfaceContainerLowest = Color(0xFF0A0F0C),
)

@Composable
fun PrivacyWidgetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Make status bar transparent for edge-to-edge display
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            // Always use light status bar icons (cream/white) for visibility on green top bar
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
