package com.notification.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.notification.app.ui.designsystem.AppDimens
import com.notification.app.ui.designsystem.AppElevation
import com.notification.app.ui.designsystem.AppPadding

/**
 * Sprint 2 — UI/UX Polish.
 *
 * Compact premium top bar used across all four primary destinations
 * (Dashboard, AI Assistant, Tasks, Notifications).
 *
 * Contains ONLY the same three elements as before:
 *  - Application logo (notification icon)
 *  - Application title
 *  - Profile / Settings avatar button (the ONLY way to reach Settings)
 *
 * This is a visual-only refinement of the previous CenterAlignedTopAppBar:
 * a fixed compact height, tighter/balanced padding, and vertically
 * centered elements via a plain Row + Surface instead of the taller
 * default Material3 app bar layout. No new colors or typography styles
 * are introduced — everything still comes from the existing MaterialTheme
 * (NotificationTheme) and the Sprint 1 Design System tokens.
 *
 * @param onProfileClick navigates to the Settings screen. Settings is
 * intentionally NOT part of Bottom Navigation — this button is its only
 * entry point.
 */
@Composable
fun PremiumTopAppBar(
    title: String,
    onProfileClick: () -> Unit
) {
    Surface(
        tonalElevation = AppElevation.topBar,
        shadowElevation = AppElevation.topBar,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.topBarHeightCompact)
                .padding(horizontal = AppPadding.screen),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = "App logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(AppDimens.iconSizeMedium)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = AppPadding.cardCompact)
            )

            IconButton(onClick = onProfileClick) {
                Box(
                    modifier = Modifier
                        .size(AppDimens.avatarSizeSmall)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile / Settings",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(AppDimens.iconSizeSmall)
                    )
                }
            }
        }
    }
}
