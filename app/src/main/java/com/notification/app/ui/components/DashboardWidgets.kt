package com.notification.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notification.app.ui.designsystem.AppDimens
import com.notification.app.ui.designsystem.PremiumCard
import com.notification.app.ui.theme.Spacing

/**
 * Sprint 6 — Executive Dashboard building blocks.
 *
 * [SmartWidget] is the ONE reusable shell for every contextual dashboard
 * card (medicine today, next prayer, gam3iya payout, water goal, work
 * notes, upcoming meeting...). Widgets are SMART: the caller passes
 * `visible` and the card animates in only when it is relevant and
 * gracefully collapses away when it is not — no empty placeholder cards.
 */
@Composable
fun SmartWidget(
    visible: Boolean,
    icon: ImageVector,
    title: String,
    primaryLine: String,
    modifier: Modifier = Modifier,
    secondaryLine: String? = null,
    // Rafeeq Vivid — each widget carries its own accent (teal prayer,
    // sky water, amber habits…) so the dashboard feels alive while every
    // accent shares the same saturation family.
    accent: androidx.compose.ui.graphics.Color? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        PremiumCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.fillMaxWidth()
            ) {
                val tint = accent ?: MaterialTheme.colorScheme.primary
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(tint.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(AppDimens.iconSizeMedium)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = primaryLine,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (secondaryLine != null) {
                        Text(
                            text = secondaryLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                trailing?.invoke()
            }
        }
    }
}
