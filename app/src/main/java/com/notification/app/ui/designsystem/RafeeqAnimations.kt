package com.notification.app.ui.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween

/**
 * Rafeeq Design System - Animation Constants
 * Standard animation durations and easing curves for consistent motion.
 */
object RafeeqAnimations {
    // Durations (in milliseconds)
    const val durationInstant: Int = 0
    const val durationFast: Int = 150
    const val durationNormal: Int = 300
    const val durationSlow: Int = 450
    const val durationVerySlow: Int = 600
    
    // Easing Curves
    val defaultEasing = FastOutSlowInEasing
    
    // Predefined Tween Animations
    fun fastTween() = tween<Int>(durationMillis = durationFast, easing = defaultEasing)
    fun normalTween() = tween<Int>(durationMillis = durationNormal, easing = defaultEasing)
    fun slowTween() = tween<Int>(durationMillis = durationSlow, easing = defaultEasing)
}
