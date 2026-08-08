package com.notification.app.ui.screens

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.notification.app.R
import com.notification.app.ui.theme.Primary
import com.notification.app.ui.theme.PrimaryDark
import kotlinx.coroutines.delay

/**
 * Rafeeq splash — light, airy brand moment (matches the product mock):
 * a near-white canvas with quiet flowing wave lines, the gradient R
 * monogram, the wordmark, one confident tagline, and a slim loading bar.
 */
@Composable
fun SplashScreen(
    isArabic: Boolean,
    onSplashFinished: () -> Unit
) {
    var start by remember { mutableStateOf(false) }

    val markAlpha by animateFloatAsState(
        targetValue = if (start) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label = "SplashAlpha"
    )
    val markScale by animateFloatAsState(
        targetValue = if (start) 1f else 0.9f,
        animationSpec = tween(durationMillis = 1400, easing = EaseOutCubic),
        label = "SplashScale"
    )
    val barProgress by animateFloatAsState(
        targetValue = if (start) 1f else 0f,
        animationSpec = tween(durationMillis = 1900, easing = EaseOutCubic),
        label = "SplashBar"
    )

    LaunchedEffect(Unit) {
        start = true
        delay(2000)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFAFAFF), Color(0xFFF3F2FE), Color(0xFFEDEBFC))
                )
            )
    ) {
        // Quiet flowing wave lines — the mock's signature decoration.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            for (i in 0..6) {
                val base = h * (0.62f + i * 0.05f)
                val path = Path().apply {
                    moveTo(-w * 0.1f, base)
                    cubicTo(
                        w * 0.25f, base - h * (0.10f + i * 0.008f),
                        w * 0.55f, base + h * 0.05f,
                        w * 1.1f, base - h * (0.16f + i * 0.01f)
                    )
                }
                drawPath(
                    path = path,
                    color = Primary.copy(alpha = 0.05f + (6 - i) * 0.008f),
                    style = Stroke(width = 2.2f)
                )
            }
            // faint highlight sweep top-right
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Primary.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(w * 0.85f, h * 0.12f),
                    radius = w * 0.6f
                ),
                center = Offset(w * 0.85f, h * 0.12f),
                radius = w * 0.6f
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .align(Alignment.Center)
                .alpha(markAlpha)
                .scale(markScale)
        ) {
            Image(
                painter = painterResource(R.drawable.logo_r_gradient),
                contentDescription = null,
                modifier = Modifier.size(132.dp)
            )
            Text(
                text = if (isArabic) "رفيق" else "Rafeeq",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1B4B)
            )
            Text(
                text = if (isArabic) "مساعدك الذكي لإدارة يومك"
                else "Your smart assistant for your day",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }

        // Slim loading bar + label near the bottom.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp)
                .alpha(markAlpha)
        ) {
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(4.dp)
                    .background(Primary.copy(alpha = 0.14f), RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(scaleX = barProgress, scaleY = 1f)
                        .background(
                            Brush.horizontalGradient(listOf(Primary, PrimaryDark)),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
            Text(
                text = if (isArabic) "جاري التحميل…" else "Loading…",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF9CA3AF)
            )
        }
    }
}
