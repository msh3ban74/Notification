package com.notification.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.notification.app.R

/**
 * Rafeeq Design Language — premium typography.
 *
 * Bundled variable fonts (res/font, no Gradle changes, no network
 * font provider):
 *  - CINZEL  — regal Roman capitals for English titles/headlines.
 *  - LEXEND  — calm, highly readable body text for English.
 *  - CAIRO   — elegant Arabic (with full Latin coverage) used for BOTH
 *              titles and body when the app is in Arabic, so Arabic
 *              users get real premium Arabic letterforms instead of a
 *              system fallback.
 *
 * The variable fonts instantiate their real weight axis on API 26+
 * (Compose derives wght from the FontWeight passed to Font()); on the
 * few API 24-25 devices they gracefully fall back to the default
 * instance. RTL/LTR behavior is untouched — fonts never affect layout
 * direction.
 */
private val Cinzel = FontFamily(
    Font(R.font.cinzel, FontWeight.Normal),
    Font(R.font.cinzel, FontWeight.Medium),
    Font(R.font.cinzel, FontWeight.SemiBold),
    Font(R.font.cinzel, FontWeight.Bold)
)

private val Lexend = FontFamily(
    Font(R.font.lexend, FontWeight.Light),
    Font(R.font.lexend, FontWeight.Normal),
    Font(R.font.lexend, FontWeight.Medium),
    Font(R.font.lexend, FontWeight.SemiBold),
    Font(R.font.lexend, FontWeight.Bold)
)

private val Cairo = FontFamily(
    Font(R.font.cairo, FontWeight.Light),
    Font(R.font.cairo, FontWeight.Normal),
    Font(R.font.cairo, FontWeight.Medium),
    Font(R.font.cairo, FontWeight.SemiBold),
    Font(R.font.cairo, FontWeight.Bold)
)

/**
 * Builds the Rafeeq type scale for the current language.
 * English: Cinzel titles + Lexend body. Arabic: Cairo everywhere
 * (it carries both scripts beautifully).
 */
fun rafeeqTypography(isArabic: Boolean): Typography {
    val headlineFont = if (isArabic) Cairo else Cinzel
    val bodyFont = if (isArabic) Cairo else Lexend

    return Typography(
        displayLarge = TextStyle(
            fontFamily = headlineFont,
            fontWeight = FontWeight.Bold,
            fontSize = 40.sp,
            lineHeight = 48.sp,
            letterSpacing = 0.sp
        ),
        displayMedium = TextStyle(
            fontFamily = headlineFont,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = headlineFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = headlineFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = headlineFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.1.sp
        ),
        titleSmall = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 26.sp,
            letterSpacing = 0.2.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.2.sp
        ),
        bodySmall = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.2.sp
        ),
        labelLarge = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.3.sp
        ),
        labelSmall = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            letterSpacing = 0.3.sp
        )
    )
}

/** Default (English) scale — kept for callers that don't pass a language. */
val Typography = rafeeqTypography(isArabic = false)
