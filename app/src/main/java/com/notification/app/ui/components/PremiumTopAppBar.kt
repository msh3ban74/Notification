package com.notification.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    onProfileClick: () -> Unit,
    // When provided, a search action appears left of the profile avatar and
    // opens the global search screen.
    onSearchClick: (() -> Unit)? = null
) {
    // Visual polish — transparent app bar: the bar shares the screen's
    // background so content and chrome read as ONE calm surface (Linear /
    // Pixel style); the status-bar inset still keeps content clear.
    Surface(
        tonalElevation = AppElevation.none,
        shadowElevation = AppElevation.none,
        color = MaterialTheme.colorScheme.background
    ) {
        // QA fix (device screenshots): the app is edge-to-edge, so without
        // consuming the status-bar inset the bar contents rendered UNDER the
        // system clock/battery. statusBarsPadding keeps the surface color
        // flowing behind the status bar (premium look) while pushing the
        // row's content below it — correct in both RTL and LTR.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(AppDimens.topBarHeightCompact)
                .padding(horizontal = AppPadding.screen),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rafeeq brand mark — the sparkle, matching the launcher icon.
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Rafeeq logo",
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

            if (onSearchClick != null) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "بحث / Search",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(AppDimens.iconSizeMedium)
                    )
                }
            }

            // Stability sprint — Settings must be immediately discoverable:
            // a larger, gold-filled avatar instead of the old faint chip.
            IconButton(onClick = onProfileClick) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "الإعدادات / Settings",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(AppDimens.iconSizeMedium)
                    )
                }
            }
        }
    }
}
