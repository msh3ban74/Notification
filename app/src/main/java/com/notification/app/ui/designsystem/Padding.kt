package com.notification.app.ui.designsystem

import androidx.compose.ui.unit.dp

/**
 * Sprint 1 — Application Foundation / Design System.
 *
 * Reusable INTERNAL padding values (space inside a container's own edge,
 * e.g. the inset between a Card's border and its content).
 *
 * This complements [com.notification.app.ui.theme.Spacing], which is used
 * for the GAP BETWEEN sibling elements. Keeping the two concepts separate
 * makes screens easier to read: "Spacing" = between things, "Padding" =
 * inside a thing.
 *
 * This file introduces no new colors, typography, or shapes — it only
 * gives semantic names to values so screens stop hard-coding raw dp
 * numbers.
 */
object AppPadding {
    /** Padding used for the whole screen content area. */
    val screen = 20.dp

    /** Padding inside a standard card. */
    val card = 20.dp

    /** Padding inside a compact card (e.g. a stat chip). */
    val cardCompact = 14.dp

    /** Padding inside a list item / row. */
    val listItem = 16.dp

    /** Horizontal padding for a button's content. */
    val buttonHorizontal = 24.dp

    /** Vertical padding for a button's content. */
    val buttonVertical = 12.dp

    /** Padding around the Top App Bar's leading/trailing icons. */
    val topBarIcon = 8.dp
}
