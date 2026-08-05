package com.notification.app.ui.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Sprint 1 — Application Foundation / Design System.
 *
 * A reusable Material 3 card that wraps the app's EXISTING theme
 * (MaterialTheme.colorScheme / MaterialTheme.shapes come from
 * NotificationTheme — nothing is redefined here). Use this instead of
 * a raw [Card] on new screens so every card in the app shares the same
 * shape, elevation, and internal padding by default.
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
    Card(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        shape = AppRadius.medium,
        colors = CardDefaults.cardColors(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (elevated) AppElevation.floatingCard else AppElevation.none
        )
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}
