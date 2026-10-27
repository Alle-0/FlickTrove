package com.cinetrack.ui.utils

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import com.cinetrack.util.VibrationHelper

/**
 * Reusable utility for calculating snapping targets and executing fluid collapse animations
 * for sticky/collapsing search bars and headers with haptic feedback.
 */
object CollapsingHeaderUtils {
    const val SNAP_VELOCITY_THRESHOLD = 300f
    const val SNAP_DURATION_MS = 200

    /**
     * Determines whether a collapsible element is in a partially collapsed/expanded state.
     */
    fun isPartial(currentHeight: Float, maxHeight: Float, tolerance: Float = 0.5f): Boolean {
        return maxHeight > 0f && currentHeight > tolerance && currentHeight < maxHeight - tolerance
    }

    /**
     * Calculates the target snap height based on current position and fling velocity.
     */
    fun calculateSnapTarget(currentHeight: Float, maxHeight: Float, velocity: Float): Float {
        if (maxHeight <= 0f) return 0f
        return when {
            velocity < -SNAP_VELOCITY_THRESHOLD -> 0f
            velocity > SNAP_VELOCITY_THRESHOLD -> maxHeight
            currentHeight < maxHeight * 0.5f -> 0f
            else -> maxHeight
        }
    }

    /**
     * Animates a snap transition from start progress (0f) to finished (1f) with easing and haptic feedback.
     */
    suspend fun animateSnap(
        context: Context,
        durationMillis: Int = SNAP_DURATION_MS,
        onUpdate: (progress: Float) -> Unit
    ) {
        val anim = Animatable(0f)
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)
        ) {
            onUpdate(value)
        }
        VibrationHelper.vibrateTick(context)
    }
}
