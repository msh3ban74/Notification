package com.notification.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Rafeeq Design Language — centralized corner-radius system.
 *
 * Large, calm roundness everywhere (cards 22–28dp, buttons 20–24dp,
 * sheets very rounded) so every screen feels like the same premium
 * product designed by one hand.
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),   // buttons, inputs, chips
    large = RoundedCornerShape(26.dp),    // cards
    extraLarge = RoundedCornerShape(32.dp) // sheets, dialogs
)

/**
 * Fixed spacing scale — Rafeeq breathes: generous gaps, nothing crowded.
 * Use these instead of one-off dp values on every screen.
 */
object Spacing {
    val xs = 4.dp
    val sm = 10.dp
    val md = 18.dp
    val lg = 28.dp
    val xl = 36.dp
    val xxl = 52.dp
}
