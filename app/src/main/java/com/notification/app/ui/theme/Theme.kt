package com.notification.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Rafeeq Royal theme — black & champagne gold.
 *
 * Dark mode is the flagship look from the brand board: rich black
 * backgrounds, charcoal surfaces, champagne-gold primary, cream text.
 * Light mode is its ivory twin: cream backgrounds with deep gold.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Primary,                 // Champagne Gold
    onPrimary = OnPrimary,             // near-black on gold
    primaryContainer = GoldContainerDark,
    onPrimaryContainer = PrimaryLight, // Soft Gold
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = DarkSlate,
    onSecondaryContainer = PrimaryLight,
    background = RichBlack,
    onBackground = Cream,
    surface = Charcoal,
    onSurface = Cream,
    surfaceVariant = DarkSlate,
    onSurfaceVariant = MutedBronze,
    // Visual polish — REAL surface depth hierarchy: backgrounds, cards,
    // elevated cards, and dialogs each live one level brighter, so the
    // UI reads as layered depth instead of one flat black.
    surfaceContainerLowest = Color(0xFF0E0F13),
    surfaceContainerLow = Charcoal,
    surfaceContainer = Color(0xFF191B21),
    surfaceContainerHigh = Color(0xFF1F2128),
    surfaceContainerHighest = Color(0xFF262932),
    outline = GoldOutlineDark,
    error = Error,
    onError = Color.White,
    errorContainer = Color(0xFF4A1F1F),
    onErrorContainer = Color(0xFFF2C8C8)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryDark,             // deep gold — readable on cream
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3E7C4),
    onPrimaryContainer = Color(0xFF4A3B10),
    secondary = PrimaryDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF7EFDC),
    onSecondaryContainer = Color(0xFF4A3B10),
    background = Background,           // Cream
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Color(0xFFD5CBB4),
    error = Error,
    onError = Color.White,
    errorContainer = Color(0xFFF6DEDE),
    onErrorContainer = Color(0xFF5C1F1F)
)

@Composable
fun NotificationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isArabic: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        // Rafeeq premium typography: Cinzel titles + Lexend body (EN),
        // Cairo everywhere (AR). RTL/LTR handling is unaffected.
        typography = rafeeqTypography(isArabic),
        shapes = Shapes,
        content = content
    )
}
