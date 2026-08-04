package com.notification.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MaroonPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = MaroonContainerDark,
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = ChampagneGold,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3D3219),
    onSecondaryContainer = ChampagneGold,
    background = PlatinumDarkBackground,
    onBackground = PlatinumTextLight,
    surface = PlatinumDarkSurface,
    onSurface = PlatinumTextLight,
    surfaceVariant = PlatinumDarkCard,
    onSurfaceVariant = Color(0xFFD4CED4),
    outline = Color(0xFF534D53)
)

private val LightColorScheme = lightColorScheme(
    primary = MaroonPrimary,
    onPrimary = Color.White,
    primaryContainer = MaroonContainerLight,
    onPrimaryContainer = MaroonPrimaryDark,
    secondary = ChampagneGold,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFFFF6DF),
    onSecondaryContainer = Color(0xFF423200),
    background = PlatinumLight,
    onBackground = PlatinumTextDark,
    surface = PlatinumSurfaceLight,
    onSurface = PlatinumTextDark,
    surfaceVariant = PlatinumCardLight,
    onSurfaceVariant = PlatinumTextMuted,
    outline = Color(0xFFC4BEC4)
)

@Composable
fun NotificationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
