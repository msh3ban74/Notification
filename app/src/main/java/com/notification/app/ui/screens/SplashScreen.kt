package com.notification.app.ui.screens

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notification.app.R
import com.notification.app.ui.theme.AccentViolet
import com.notification.app.ui.theme.Primary
import com.notification.app.ui.theme.PrimaryDark
import kotlinx.coroutines.delay

/**
 * Rafeeq splash — the brand moment.
 *
 * A deep indigo→violet canvas, the R monogram resting in a frosted
 * glass tile, the wordmark in the premium type, and one confident
 * tagline. Everything is drawn from the live palette + bundled assets,
 * so it always matches the launcher icon pixel-for-pixel.
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
        targetValue = if (start) 1f else 0.86f,
        animationSpec = tween(durationMillis = 1500, easing = EaseOutCubic),
        label = "SplashScale"
    )

    LaunchedEffect(Unit) {
        start = true
        delay(2000)
        onSplashFinished()
    }

    // The splash canvas is dark — flip to light status-bar icons for its
    // two seconds, then hand back the app's dark-icons-on-white default.
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        val controller = window?.let {
            androidx.core.view.WindowCompat.getInsetsController(it, view)
        }
        controller?.isAppearanceLightStatusBars = false
        onDispose { controller?.isAppearanceLightStatusBars = true }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E1B4B), PrimaryDark, Primary, AccentViolet)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .alpha(markAlpha)
                .scale(markScale)
        ) {
            // Frosted glass tile holding the R monogram — same mark as
            // the launcher icon, so the hand-off feels seamless.
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(32.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_r),
                    contentDescription = null,
                    modifier = Modifier.size(84.dp)
                )
            }
            Text(
                text = if (isArabic) "رفيق" else "Rafeeq",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = if (isArabic) "مساعدك الشخصي الذكي"
                else "Your intelligent personal assistant",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Text(
                text = "R A F E E Q",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 6.sp),
                color = Color.White.copy(alpha = 0.45f)
            )
        }
    }
}
