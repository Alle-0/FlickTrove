package com.cinetrack.ui.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * CinematicHaptics
 * Gestore aptico ad alta precisione per micro-interazioni cinematografiche:
 * - Scatti meccanici a ghiera di precisione (lente, obiettivi fotografici, focus ring).
 * - Sblocco / "Unseal Thud" ad impatto profondo al completamento di un sigillo.
 *
 * Supporta in modo trasparente l'hardware più moderno (Primitive composition su API 31+)
 * con graceful fallback scalare fino alle versioni precedenti di Android, zero crash.
 */
object CinematicHaptics {

    private fun getVibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Esegue un micro-scatto secco e meccanico (ghiera di calibrazione dell'ottica).
     * @param intensity Intensità scalare tra 0.1f e 1.0f.
     */
    fun mechanicalTick(context: Context, intensity: Float = 0.5f) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        val clampedIntensity = intensity.coerceIn(0.1f, 1.0f)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK)
            ) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, clampedIntensity)
                    .compose()
                vibrator.vibrate(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitude = (clampedIntensity * 160).toInt().coerceIn(1, 255)
                vibrator.vibrate(VibrationEffect.createOneShot(10, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(10)
            }
        } catch (_: Throwable) {
            // Silenzioso, zero-crash policy
        }
    }

    /**
     * Esegue uno scatto meccanico leggermente più pronunciato (step intermedio o checkpoint).
     */
    fun mechanicalClick(context: Context, intensity: Float = 0.7f) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        val clampedIntensity = intensity.coerceIn(0.1f, 1.0f)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)
            ) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, clampedIntensity)
                    .compose()
                vibrator.vibrate(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitude = (clampedIntensity * 220).toInt().coerceIn(1, 255)
                vibrator.vibrate(VibrationEffect.createOneShot(18, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(18)
            }
        } catch (_: Throwable) {
            // Silenzioso, zero-crash policy
        }
    }

    /**
     * Colpo profondo e solido di sblocco ("Unseal Thud"):
     * Simula il rilascio elastico del blocco ottico / esplosione cinematografica.
     */
    fun unsealThud(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                vibrator.areAllPrimitivesSupported(
                    VibrationEffect.Composition.PRIMITIVE_CLICK,
                    VibrationEffect.Composition.PRIMITIVE_TICK
                )
            ) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.7f, 25)
                    .compose()
                vibrator.vibrate(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(45)
            }
        } catch (_: Throwable) {
            // Silenzioso, zero-crash policy
        }
    }
}
