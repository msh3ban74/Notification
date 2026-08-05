package com.notification.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.notification.app.R
import com.notification.app.ui.theme.RafeeqRichBlack
import kotlinx.coroutines.delay

/**
 * Rafeeq — splash screen.
 *
 * The full brand artwork (golden "R" monogram over the sunrise valley,
 * with the wordmark and the "نظم حياتك ودع رفيق يساعدك" tagline baked
 * into the art itself) revealed with a smooth cinematic entrance:
 * a slow fade-in combined with a gentle settle from a slight zoom.
 *
 * Navigation behavior (onSplashFinished after a short delay) is
 * unchanged from the previous splash.
 */
@Composable
fun SplashScreen(
    isArabic: Boolean,
    onSplashFinished: () -> Unit
) {
    var start by remember { mutableStateOf(false) }

    // Smooth reveal: fade in while the artwork settles from a soft zoom.
    val imageAlpha by animateFloatAsState(
        targetValue = if (start) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label = "SplashAlpha"
    )
    val imageScale by animateFloatAsState(
        targetValue = if (start) 1f else 1.08f,
        animationSpec = tween(durationMillis = 2200, easing = EaseOutCubic),
        label = "SplashScale"
    )

    LaunchedEffect(Unit) {
        start = true
        delay(2400)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RafeeqRichBlack),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_rafeeq),
            contentDescription = if (isArabic) "رفيق — نظم حياتك ودع رفيق يساعدك" else "Rafeeq — organize your life and let Rafeeq help you",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(imageAlpha)
                .scale(imageScale)
        )
    }
}
