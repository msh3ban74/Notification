package com.notification.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Rafeeq Design DNA — the ATMOSPHERE.
 *
 * The signature visual element that makes any Rafeeq screen recognizable
 * from a single screenshot: a quiet vertical color wash crowned with two
 * or three softly-glowing light orbs and thin flowing wave lines. Each
 * major screen carries its own palette (its own "weather") while every
 * screen unmistakably belongs to the same family.
 *
 * The orbs BREATHE — an endless, very slow drift that keeps the surface
 * feeling alive without ever demanding attention.
 */
data class AtmospherePalette(
    val wash: List<Color>,
    val orbA: Color,
    val orbB: Color,
    val orbC: Color? = null
)

object RafeeqWeather {
    // «يومك» — lavender dawn.
    val Day = AtmospherePalette(
        wash = listOf(Color(0xFFEFEDFD), Color(0xFFF7F6FE), Color(0x00FFFFFF)),
        orbA = Color(0xFF8B5CF6),
        orbB = Color(0xFF60A5FA),
        orbC = Color(0xFF34D399)
    )

    // «المساعد الذكي» — indigo dusk.
    val Assistant = AtmospherePalette(
        wash = listOf(Color(0xFFECEBFD), Color(0xFFF5F4FE), Color(0x00FFFFFF)),
        orbA = Color(0xFF4F46E5),
        orbB = Color(0xFF8B5CF6),
        orbC = Color(0xFF38BDF8)
    )

    // «المهام» — mint morning.
    val Tasks = AtmospherePalette(
        wash = listOf(Color(0xFFE9F9F4), Color(0xFFF4FCF9), Color(0x00FFFFFF)),
        orbA = Color(0xFF34D399),
        orbB = Color(0xFF22D3EE),
        orbC = Color(0xFF6366F1)
    )

    // «الإشعارات» — golden hour (the only place warm gold appears).
    val Alerts = AtmospherePalette(
        wash = listOf(Color(0xFFFDF6EA), Color(0xFFFEFBF3), Color(0x00FFFFFF)),
        orbA = Color(0xFFF59E0B),
        orbB = Color(0xFFFB7185),
        orbC = Color(0xFF8B5CF6)
    )
}

@Composable
fun RafeeqAtmosphere(
    palette: AtmospherePalette,
    modifier: Modifier = Modifier
) {
    // One slow breath ≈ 7 seconds; orbs drift a few dp and softly swell.
    val breath by rememberInfiniteTransition(label = "atmosphere")
        .animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(7000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breath"
        )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1 — the vertical wash.
        drawRect(brush = Brush.verticalGradient(palette.wash))

        // 2 — glowing orbs, breathing.
        fun orb(color: Color, cx: Float, cy: Float, r: Float, alpha: Float) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = r
                ),
                center = Offset(cx, cy),
                radius = r
            )
        }
        val drift = 24f * breath
        orb(palette.orbA, w * 0.85f + drift, h * 0.18f, w * 0.42f * (1f + 0.06f * breath), 0.16f)
        orb(palette.orbB, w * 0.12f - drift, h * 0.55f, w * 0.36f * (1f + 0.05f * (1f - breath)), 0.13f)
        palette.orbC?.let { orb(it, w * 0.55f, h * 0.92f, w * 0.30f, 0.09f) }

        // 3 — thin flowing waves.
        for (i in 0..2) {
            val base = h * (0.55f + i * 0.16f)
            val path = Path().apply {
                moveTo(-w * 0.05f, base)
                cubicTo(
                    w * 0.30f, base - h * 0.16f,
                    w * 0.62f, base + h * 0.10f,
                    w * 1.05f, base - h * 0.18f
                )
            }
            drawPath(
                path = path,
                color = palette.orbA.copy(alpha = 0.06f),
                style = Stroke(width = 2f)
            )
        }
    }
}
