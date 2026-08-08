// includeFontPadding is flagged deprecated by Compose, but it is exactly
// the switch that stops tall Arabic glyphs from being clipped.
@file:Suppress("DEPRECATION")

package com.notification.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.notification.app.R

/**
 * Rafeeq Design Language — premium typography.
 *
 * Bundled fonts (res/font, no network font provider):
 *  - IBM PLEX SANS ARABIC — the corporate-premium Arabic face (static
 *    Regular/Medium/SemiBold/Bold files, so REAL weights render on every
 *    API level). Carries full Latin coverage too.
 *  - LEXEND — calm, highly readable face when the app language is English.
 *
 * CRITICAL: every style sets includeFontPadding = true and a centered
 * LineHeightStyle. Compose's modern default (no font padding) CLIPS tall
 * Arabic ascenders/descenders in tight containers — buttons, chips and
 * card captions were losing the tops/bottoms of their letters on device.
 */
private val PlexArabic = FontFamily(
    Font(R.font.plex_arabic_regular, FontWeight.Normal),
    Font(R.font.plex_arabic_medium, FontWeight.Medium),
    Font(R.font.plex_arabic_semibold, FontWeight.SemiBold),
    Font(R.font.plex_arabic_bold, FontWeight.Bold)
)

private val Lexend = FontFamily(
    Font(R.font.lexend, FontWeight.Light),
    Font(R.font.lexend, FontWeight.Normal),
    Font(R.font.lexend, FontWeight.Medium),
    Font(R.font.lexend, FontWeight.SemiBold),
    Font(R.font.lexend, FontWeight.Bold)
)

/** One style constructor so EVERY style carries the anti-clipping setup. */
private fun rafeeqStyle(
    font: FontFamily,
    weight: FontWeight,
    size: TextUnit,
    lineHeight: TextUnit
) = TextStyle(
    fontFamily = font,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = 0.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = true),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None
    )
)

/**
 * Builds the Rafeeq type scale for the current language:
 * Arabic → IBM Plex Sans Arabic everywhere; English → Lexend everywhere.
 */
fun rafeeqTypography(isArabic: Boolean): Typography {
    val font = if (isArabic) PlexArabic else Lexend

    return Typography(
        displayLarge = rafeeqStyle(font, FontWeight.Bold, 40.sp, 56.sp),
        displayMedium = rafeeqStyle(font, FontWeight.Bold, 32.sp, 46.sp),
        headlineLarge = rafeeqStyle(font, FontWeight.Bold, 28.sp, 42.sp),
        headlineMedium = rafeeqStyle(font, FontWeight.Bold, 24.sp, 36.sp),
        headlineSmall = rafeeqStyle(font, FontWeight.SemiBold, 20.sp, 32.sp),
        titleLarge = rafeeqStyle(font, FontWeight.Bold, 20.sp, 32.sp),
        titleMedium = rafeeqStyle(font, FontWeight.SemiBold, 17.sp, 28.sp),
        titleSmall = rafeeqStyle(font, FontWeight.Medium, 14.sp, 24.sp),
        bodyLarge = rafeeqStyle(font, FontWeight.Normal, 16.sp, 30.sp),
        bodyMedium = rafeeqStyle(font, FontWeight.Normal, 14.sp, 26.sp),
        bodySmall = rafeeqStyle(font, FontWeight.Normal, 12.sp, 22.sp),
        labelLarge = rafeeqStyle(font, FontWeight.SemiBold, 14.sp, 24.sp),
        labelMedium = rafeeqStyle(font, FontWeight.Medium, 12.sp, 20.sp),
        labelSmall = rafeeqStyle(font, FontWeight.Medium, 11.sp, 18.sp)
    )
}

/** Default (English) scale — kept for callers that don't pass a language. */
val Typography = rafeeqTypography(isArabic = false)
