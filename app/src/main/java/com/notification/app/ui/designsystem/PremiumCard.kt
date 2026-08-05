package com.notification.app.ui.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.Dp

/**
 * Sprint 2 — UI/UX Polish (originally Sprint 1 — Application Foundation).
 *
 * A reusable Material 3 card that wraps the app's EXISTING theme
 * (MaterialTheme.colorScheme / MaterialTheme.shapes come from
 * NotificationTheme — nothing is redefined here). Use this instead of
 * a raw [Card] on new screens so every card in the app shares the same
 * shape, elevation, and internal padding by default.
 *
 * Polish added this sprint: the Rafeeq Design Language card radius via [AppRadius.card],
 * an elevation bump on press, and a subtle press-scale so tappable cards
 * feel responsive. No colors were changed.
 *
 * @param onClick optional — if provided, the whole card becomes tappable.
 * @param elevated if true, uses [AppElevation.floatingCard]; otherwise flat.
 */
@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevated: Boolean = true,
    contentPadding: Dp = AppPadding.card,
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

    Card(
        modifier = cardModifier,
        shape = AppRadius.card,
        colors = CardDefaults.cardColors(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (elevated) AppElevation.floatingCard else AppElevation.none,
            pressedElevation = if (elevated) AppElevation.medium else AppElevation.none
        )
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}
