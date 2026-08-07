package com.notification.app.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// Rafeeq Vivid — the light, lively, premium palette.
//
// One idea: a clean white canvas, electric-indigo identity, and
// GREEN for everything the user finished (done = growth). A small
// family of accents is distributed across the dashboard cards so
// the app feels alive — every accent shares the same saturation
// and depth so they always sit well together.
// ============================================================

// Identity
val Primary = Color(0xFF4F46E5)        // Rafeeq Indigo (electric, premium)
val PrimaryLight = Color(0xFF818CF8)   // Soft indigo
val PrimaryDark = Color(0xFF3730A3)    // Deep indigo

val Secondary = Color(0xFF0D9488)      // Teal — calm, fresh companion accent
val Accent = Color(0xFF7C3AED)         // Violet — sparse premium highlights

// Canvas — white with a whisper of indigo so cards still float
val Background = Color(0xFFFAFAFF)
val Surface = Color(0xFFFFFFFF)
val SurfaceVariant = Color(0xFFEEF0FA)

// Ink
val OnPrimary = Color(0xFFFFFFFF)
val OnSecondary = Color(0xFFFFFFFF)
val OnBackground = Color(0xFF23205A)   // deep indigo-ink, softer than black
val OnSurface = Color(0xFF23205A)
val OnSurfaceVariant = Color(0xFF5D6078)

// Semantics — DONE is always green
val Success = Color(0xFF16A34A)        // vivid green: completed, paid, streaks
val Warning = Color(0xFFF59E0B)        // amber
val Error = Color(0xFFE11D48)          // vivid rose-red: overdue, you-owe
val Info = Color(0xFF0284C7)           // sky blue

// Accent family — distributed across dashboard cards & category icons.
// Same saturation family as the primary so mixes stay harmonious.
val AccentTeal = Color(0xFF0D9488)     // prayer / appointments
val AccentSky = Color(0xFF0284C7)      // water
val AccentAmber = Color(0xFFF59E0B)    // habits / bills
val AccentViolet = Color(0xFF7C3AED)   // work / notes
val AccentCoral = Color(0xFFF43F5E)    // medicine / health
val AccentPink = Color(0xFFDB2777)     // birthdays / events

// Rafeeq brand-mark colors (splash artwork)
val RafeeqCanvas = Color(0xFFFAFAFF)
val RafeeqIndigo = Primary
val RafeeqIndigoLight = PrimaryLight
val RafeeqIndigoDeep = PrimaryDark

// Legacy aliases — every existing screen that references these gets the
// vivid identity automatically (they date back to the maroon/gold eras).
val MaroonPrimary = Primary
val MaroonPrimaryLight = PrimaryLight
val MaroonPrimaryDark = PrimaryDark
