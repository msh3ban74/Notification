package com.notification.app.ui.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Visual polish sprint — card HIERARCHY.
 *
 * Not every card is equal: the Executive Summary is a Hero, statistics
 * are compact chips, and everything else is a standard card. Each style
 * gets its own surface level (from the theme's container ladder), radius,
 * padding, and elevation — different weights, ONE design language.
 */
enum class PremiumCardStyle { Hero, Standard, Compact }

/**
 * A reusable Material 3 card wrapping the app's EXISTING theme. Use this
 * instead of a raw [Card] so shape, depth, and padding stay consistent.
 *
 * Depth: every card floats on a soft 1dp light border (6% white) plus a
 * gentle elevation — layered, never flat.
 *
 * @param onClick optional — if provided, the whole card becomes tappable
 * with a subtle press-scale.
 * @param style Hero / Standard / Compact — see [PremiumCardStyle].
 * @param contentPadding overrides the style's default inner padding.
 */
@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevated: Boolean = true,
    style: PremiumCardStyle = PremiumCardStyle.Standard,
    contentPadding: Dp? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        animationSpec = tween(durationMillis = AppAnimationDuration.fast),
        label = "premiumCardPressScale"
    )

    val cardModifier = if (onClick != null) {
        modifier
            .scale(pressScale)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
    } else {
        modifier
    }

    val containerColor = when (style) {
        PremiumCardStyle.Hero -> MaterialTheme.colorScheme.surfaceContainerHighest
        PremiumCardStyle.Standard -> MaterialTheme.colorScheme.surfaceContainerHigh
        PremiumCardStyle.Compact -> MaterialTheme.colorScheme.surfaceContainer
    }
    val shape = when (style) {
        PremiumCardStyle.Hero -> RoundedCornerShape(30.dp)
        PremiumCardStyle.Standard -> AppRadius.card
        PremiumCardStyle.Compact -> AppRadius.medium
    }
    val resolvedPadding = contentPadding ?: when (style) {
        PremiumCardStyle.Hero -> 24.dp
        PremiumCardStyle.Standard -> AppPadding.card
        PremiumCardStyle.Compact -> AppPadding.cardCompact
    }
    val restingElevation = when {
        !elevated -> AppElevation.none
        style == PremiumCardStyle.Hero -> AppElevation.high
        style == PremiumCardStyle.Compact -> AppElevation.low
        else -> AppElevation.floatingCard
    }

    Card(
        modifier = cardModifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        elevation = CardDefaults.cardElevation(
            defaultElevation = restingElevation,
            pressedElevation = if (elevated) AppElevation.medium else AppElevation.none
        )
    ) {
        Box(modifier = Modifier.padding(resolvedPadding)) {
            content()
        }
    }
}
