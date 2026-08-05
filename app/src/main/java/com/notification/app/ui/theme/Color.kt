package com.notification.app.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// Rafeeq Royal Palette — black & champagne gold
// (from the official brand board: Rich Black #0B0B10, Charcoal
// #14161A, Dark Slate #1C1E24, Champagne Gold #D4AF37, Soft
// Gold #F0C97A, Cream #F7F4EE, Muted Bronze)
// ============================================================
val Primary = Color(0xFFD4AF37)        // Champagne Gold
val PrimaryLight = Color(0xFFF0C97A)   // Soft Gold
val PrimaryDark = Color(0xFFB8912A)    // Deep Gold
val Secondary = Color(0xFFF0C97A)
val Accent = Color(0xFFE5C378)

val Background = Color(0xFFF7F4EE)     // Cream (light mode)
val Surface = Color(0xFFFFFDF8)
val SurfaceVariant = Color(0xFFEFE9DC)

val OnPrimary = Color(0xFF14161A)      // dark on gold
val OnSecondary = Color(0xFF14161A)
val OnBackground = Color(0xFF1C1E24)
val OnSurface = Color(0xFF1C1E24)
val OnSurfaceVariant = Color(0xFF6E6656)

val Success = Color(0xFF3E9B5F)
val Warning = Color(0xFFD4AF37)
val Error = Color(0xFFC94F4F)

// Dark ("royal") side of the palette
val RichBlack = Color(0xFF0B0B10)
val Charcoal = Color(0xFF14161A)
val DarkSlate = Color(0xFF1C1E24)
val Cream = Color(0xFFF7F4EE)
val MutedBronze = Color(0xFFA69B84)
val GoldContainerDark = Color(0xFF2A2410)  // deep gold-tinted container
val GoldOutlineDark = Color(0xFF4A4332)

// Gradient Colors for premium accents
val GradientBlueStart = Color(0xFFD4AF37)  // legacy name — now gold start
val GradientBlueEnd = Color(0xFFF0C97A)    // legacy name — now gold end
val GradientPurpleStart = Color(0xFFB8912A)
val GradientPurpleEnd = Color(0xFFF0C97A)

// Rafeeq brand-mark colors (launcher icon + splash artwork)
val RafeeqRichBlack = Color(0xFF0A0A0C)
val PlatinumTint = Color(0xFFF7F4EE)
val ChampagneGoldLight = Color(0xFFF3DFAE)
val ChampagneGoldDeep = Color(0xFFE5C378)

// Legacy aliases for backward compatibility — every existing screen that
// references these gets the golden identity automatically.
val MaroonPrimary = Primary
val MaroonPrimaryLight = PrimaryLight
val MaroonPrimaryDark = PrimaryDark
val ChampagneGold = Primary
val MaroonContainerDark = GoldContainerDark
val PlatinumDarkBackground = RichBlack
