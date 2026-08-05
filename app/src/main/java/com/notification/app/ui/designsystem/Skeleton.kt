package com.notification.app.ui.designsystem

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * v1.0 — Skeleton loading (Rafeeq Design Language).
 *
 * A calm breathing placeholder line used instead of spinners while
 * content loads. Respects the system animation scale automatically
 * (Compose animations are driven by the system clock).
 */
@Composable
fun SkeletonLine(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    widthFraction: Float = 1f
) {
    val infinite = rememberInfiniteTransition(label = "Skeleton")
    val pulse by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "SkeletonPulse"
    )
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .alpha(pulse)
            .background(
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                AppRadius.small
            )
    )
}
