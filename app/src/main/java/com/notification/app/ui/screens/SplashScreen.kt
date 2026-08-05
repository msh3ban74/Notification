package com.notification.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notification.app.ui.theme.ChampagneGoldDeep
import com.notification.app.ui.theme.ChampagneGoldLight
import com.notification.app.ui.theme.GradientBlueEnd
import com.notification.app.ui.theme.MaroonPrimary
import com.notification.app.ui.theme.MaroonPrimaryDark
import com.notification.app.ui.theme.PlatinumTint
import kotlinx.coroutines.delay

/**
 * Rafeeq — premium splash screen.
 *
 * The brand moment of the app: the "Rafeeq spark" (the same guiding-star
 * mark as the launcher icon) drawn natively in Compose, breathing over a
 * deep diagonal brand gradient, with the bilingual wordmark below it.
 *
 * Everything uses the EXISTING theme palette (ui/theme/Color.kt) — no new
 * theme colors are introduced. Navigation behavior (onSplashFinished
 * after a short delay) is unchanged from the previous splash.
 */
@Composable
fun SplashScreen(
    isArabic: Boolean,
    onSplashFinished: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    // Entrance: the spark scales in with a gentle spring.
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.35f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "SplashScale"
    )
    val fadeAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "SplashFade"
    )

    // Living details: halo breath + companion-star twinkle + loading dots.
    val infinite = rememberInfiniteTransition(label = "SplashInfinite")
    val haloPulse by infinite.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "HaloPulse"
    )
    val twinkle by infinite.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "Twinkle"
    )
    val dotPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(900)),
        label = "DotPhase"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1700)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaroonPrimaryDark,
                        MaroonPrimary,
                        GradientBlueEnd
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // The Rafeeq spark — same mark as the launcher icon.
            Canvas(
                modifier = Modifier
                    .size(150.dp)
                    .scale(scaleAnim)
            ) {
                drawRafeeqSpark(haloPulse = haloPulse, twinkle = twinkle)
            }

            Spacer(modifier = Modifier.height(28.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(fadeAnim)
            ) {
                Text(
                    text = if (isArabic) "رفيق" else "Rafeeq",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = 40.sp,
                    letterSpacing = if (isArabic) 0.sp else 2.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isArabic) "Rafeeq" else "رفيق",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.75f),
                    letterSpacing = if (isArabic) 4.sp else 0.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isArabic) "رفيقك الذكي في كل تفاصيل حياتك" else "Your intelligent life companion",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            Spacer(modifier = Modifier.height(44.dp))

            // Three breathing dots instead of a spinner — quieter, premium.
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.alpha(fadeAnim)
            ) {
                repeat(3) { index ->
                    val distance = kotlin.math.abs(dotPhase - index)
                    val emphasis = (1f - minOf(distance, 1f)) // 1 when active
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.35f + 0.6f * emphasis),
                        modifier = Modifier.size((6 + 3 * emphasis).dp)
                    ) {}
                }
            }
        }
    }
}

/**
 * Draws the "Rafeeq spark": pulsing halo rings, the platinum four-point
 * guiding star, and the golden companion spark — the exact geometry of
 * the launcher icon (viewport 108 scaled to the canvas).
 */
private fun DrawScope.drawRafeeqSpark(haloPulse: Float, twinkle: Float) {
    val u = size.minDimension / 108f

    fun sparklePath(cx: Float, cy: Float, reach: Float, waist: Float): Path =
        Path().apply {
            moveTo(cx, cy - reach)
            quadraticBezierTo(cx + waist, cy - waist, cx + reach, cy)
            quadraticBezierTo(cx + waist, cy + waist, cx, cy + reach)
            quadraticBezierTo(cx - waist, cy + waist, cx - reach, cy)
            quadraticBezierTo(cx - waist, cy - waist, cx, cy - reach)
            close()
        }

    // Halo rings (breathing)
    drawCircle(
        color = Color.White.copy(alpha = 0.10f * haloPulse),
        radius = 42f * u,
        center = Offset(54f * u, 54f * u)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.08f * haloPulse),
        radius = 32f * u,
        center = Offset(54f * u, 54f * u)
    )

    // Main guiding star — platinum gradient
    drawPath(
        path = sparklePath(54f * u, 54f * u, 27f * u, 6.2f * u),
        brush = Brush.linearGradient(
            colors = listOf(Color.White, PlatinumTint),
            start = Offset(42f * u, 36f * u),
            end = Offset(68f * u, 74f * u)
        )
    )

    // Companion spark — champagne gold, twinkling
    drawPath(
        path = sparklePath(76f * u, 33f * u, 8f * u * twinkle, 1.8f * u * twinkle),
        brush = Brush.linearGradient(
            colors = listOf(ChampagneGoldLight, ChampagneGoldDeep),
            start = Offset(71f * u, 27f * u),
            end = Offset(81f * u, 39f * u)
        )
    )
}
