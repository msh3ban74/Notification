package com.notification.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.notification.app.ui.designsystem.AppDimens
import com.notification.app.ui.designsystem.AppElevation
import com.notification.app.ui.designsystem.AppPadding
import com.notification.app.ui.designsystem.AppRadius
import com.notification.app.ui.theme.MaroonPrimary
import com.notification.app.ui.theme.NotificationTheme
import com.notification.app.ui.theme.Spacing
import kotlinx.coroutines.delay

/**
 * The single, shared "no data yet" state used across every screen in the app
 * (Reminders, Debts, Gam3iyas, Notes, and the Smart Item "Coming Soon"
 * placeholders...). Having one component instead of a hand-rolled Card per
 * screen is what keeps the whole app feeling consistent.
 *
 * Sprint 2 (UI/UX Polish): now built entirely on the Sprint 1 Design System
 * tokens (AppRadius, AppPadding, AppElevation, AppDimens, Spacing) instead
 * of hard-coded dp values, with a touch more elevation for a premium
 * "floating" feel. The content, animation timing, and colors are unchanged.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    /** v1.0 — optional primary call to action ("Add Task", "Add Person"...). */
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    /**
     * Extra bottom inset so the centered card doesn't sit under a
     * floating action button on screens that have the bottom nav + FAB
     * chrome. Screens without that chrome (back-only TopAppBar) pass 0.dp.
     */
    bottomInset: androidx.compose.ui.unit.Dp = AppDimens.bottomNavHeight
) {
    var visible by remember { mutableFloatStateOf(0f) }
    val animatedAlpha by animateFloatAsState(
        targetValue = visible,
        animationSpec = tween(durationMillis = 450),
        label = "emptyStateAlpha"
    )
    val animatedScale by animateFloatAsState(
        targetValue = 0.9f + (visible * 0.1f),
        animationSpec = tween(durationMillis = 450),
        label = "emptyStateScale"
    )

    androidx.compose.runtime.LaunchedEffect(Unit) {
        delay(50)
        visible = 1f
    }

    // No card, no border: a premium empty state floats directly on the
    // page — tonal halo, strong title, quiet subtitle, one CTA. The column
    // is compact AND scrollable so it can never be clipped on short areas
    // (which previously cut the action button's text on device).
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = bottomInset),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .verticalScroll(rememberScrollState())
                .alpha(animatedAlpha)
                .scale(animatedScale)
        ) {
            // Halo behind the icon for depth without any card.
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(MaroonPrimary.copy(alpha = 0.06f), CircleShape)
                )
                Surface(
                    shape = CircleShape,
                    color = MaroonPrimary.copy(alpha = 0.12f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaroonPrimary,
                            modifier = Modifier.size(AppDimens.iconSizeLarge)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = Spacing.sm)
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(Spacing.lg))
                Button(
                    onClick = onAction,
                    shape = AppRadius.button,
                    // Default content padding: a tight custom vertical inset
                    // clipped tall Arabic ascenders to dots on device.
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaroonPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    NotificationTheme {
        EmptyState(
            icon = Icons.Default.EventAvailable,
            title = "لا توجد تذكيرات حالياً",
            subtitle = "اضغط على زر الإضافة لإنشاء تذكير جديد"
        )
    }
}
