package com.cinetrack.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object VibrationHelper {
    
    fun vibrate(context: Context, durationMs: Long, amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(durationMs, amplitude)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Light tick vibration for regular button clicks
     */
    fun vibrateTick(context: Context) {
        vibrate(context, durationMs = 15L, amplitude = 140)
    }

    /**
     * Standard click vibration (slightly stronger than tick)
     */
    fun vibrateClick(context: Context) {
        vibrate(context, durationMs = 30L, amplitude = VibrationEffect.DEFAULT_AMPLITUDE)
    }

    /**
     * Strong vibration for long press feedback
     */
    fun vibrateLongClick(context: Context) {
        vibrate(context, durationMs = 50L, amplitude = VibrationEffect.DEFAULT_AMPLITUDE)
    }
}
