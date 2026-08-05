package com.notification.app.ui.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Sprint 1 — Application Foundation / Design System.
 *
 * Semantic corner-radius shapes for the new UI. These mirror the exact
 * dp values already defined in [com.notification.app.ui.theme.Shapes]
 * (the app's Material 3 Shapes object is NOT modified) — this file just
 * exposes them as ready-to-use [RoundedCornerShape]s so new composables
 * don't need to reference MaterialTheme.shapes manually or hard-code dp.
 */
object AppRadius {
    val extraSmall = RoundedCornerShape(10.dp)
    val small = RoundedCornerShape(14.dp)
    val medium = RoundedCornerShape(20.dp)
    val large = RoundedCornerShape(26.dp)
    val extraLarge = RoundedCornerShape(32.dp)

    // Rafeeq Design Language semantics
    /** Premium cards: 26dp. */
    val card = RoundedCornerShape(26.dp)

    /** Buttons: 22dp. */
    val button = RoundedCornerShape(22.dp)

    /** Bottom sheets: very rounded top corners. */
    val sheet = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

    /** Fully rounded — for pills, chips, and circular buttons. */
    val full = RoundedCornerShape(50)
}
