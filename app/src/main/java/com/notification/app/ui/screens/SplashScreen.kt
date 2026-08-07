package com.notification.app.ui.screens

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.notification.app.ui.theme.Primary
import com.notification.app.ui.theme.PrimaryDark
import com.notification.app.ui.theme.RafeeqCanvas
import kotlinx.coroutines.delay

/**
 * Rafeeq Vivid — splash.
 *
 * Drawn entirely in code with the live palette (no baked artwork), so the
 * splash always matches the app that follows it: a bright canvas melting
 * into indigo, the sparkle brand-mark settling in, and the promise line.
 */
@Composable
fun SplashScreen(
    isArabic: Boolean,
    onSplashFinished: () -> Unit
) {
    var start by remember { mutableStateOf(false) }

    val markAlpha by animateFloatAsState(
        targetValue = if (start) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = EaseOutCubic),
        label = "SplashAlpha"
    )
    val markScale by animateFloatAsState(
        targetValue = if (start) 1f else 0.82f,
        animationSpec = tween(durationMillis = 1600, easing = EaseOutCubic),
        label = "SplashScale"
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
                    colors = listOf(RafeeqCanvas, Color(0xFFECEEFB), Color(0xFFDDE1F9))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .alpha(markAlpha)
                .scale(markScale)
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .background(
                        Brush.linearGradient(colors = listOf(Primary, PrimaryDark)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }
            Text(
                text = if (isArabic) "رفيق" else "Rafeeq",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryDark
            )
            Text(
                text = if (isArabic) "ذاكرتك الذكية — افتكر كل حاجة في وقتها"
                else "Your smart memory — everything, right on time",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF5D6078),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}
