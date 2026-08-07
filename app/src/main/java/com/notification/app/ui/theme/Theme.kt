package com.notification.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Rafeeq Vivid theme — light ONLY, by design.
 *
 * A white canvas with electric-indigo identity, green for everything
 * completed (tertiary == success), and a soft indigo container ladder so
 * hero cards read one gentle level deeper than the page. There is no dark
 * scheme on purpose: رفيق is bright, lively and the same for everyone.
 */
private val VividColorScheme = lightColorScheme(
    primary = Primary,                     // Rafeeq Indigo
    onPrimary = OnPrimary,
    primaryContainer = Color(0xFFE3E5FF),  // soft indigo wash
    onPrimaryContainer = Color(0xFF272377),
    secondary = Secondary,                 // Teal
    onSecondary = OnSecondary,
    secondaryContainer = Color(0xFFD3F3EF),
    onSecondaryContainer = Color(0xFF0A4A44),
    // Tertiary IS the success family: every "done / paid / streak" state
    // that uses colorScheme.tertiary turns green automatically.
    tertiary = Success,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCFCE7),
    onTertiaryContainer = Color(0xFF14532D),
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    // Surface ladder on white: each level one whisper more indigo, so
    // cards keep visible depth without shadows doing all the work.
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F8FE),
    surfaceContainer = Color(0xFFF2F3FC),
    surfaceContainerHigh = Color(0xFFECEEFB),
    surfaceContainerHighest = Color(0xFFE5E8FA),
    outline = Color(0xFFD7D9EE),
    outlineVariant = Color(0xFFE6E8F6),
    error = Error,
    onError = Color.White,
    errorContainer = Color(0xFFFFE1E7),
    onErrorContainer = Color(0xFF7F1030)
)

@Composable
fun NotificationTheme(
    // Kept for call-site compatibility; Rafeeq renders light regardless.
    darkTheme: Boolean = false,
    isArabic: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VividColorScheme,
        // Rafeeq premium typography: Cinzel titles + Lexend body (EN),
        // Cairo everywhere (AR). RTL/LTR handling is unaffected.
        typography = rafeeqTypography(isArabic),
        shapes = Shapes,
        content = content
    )
}
