package com.cinetrack.ui.components.mascot

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.sqrt

/**
 * State holding smoothed tilt parallax offsets for IRIS.
 */
@Immutable
data class IrisTiltState(
    val smoothTiltX: Float,
    val smoothTiltY: Float
)

/**
 * Manages device accelerometer sensor listeners, shake-to-dizzy detection,
 * lifecycle observer binding, and spring-smoothed 2D tilt parallax.
 */
@Composable
fun rememberIrisTiltMotion(
    enableTiltParallax: Boolean,
    onShakeDetected: () -> Unit
): IrisTiltState {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var rawTiltX by remember { mutableFloatStateOf(0f) }
    var rawTiltY by remember { mutableFloatStateOf(0f) }

    if (enableTiltParallax && !isPreview) {
        DisposableEffect(lifecycleOwner, enableTiltParallax) {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            if (sensorManager == null || sensor == null) {
                onDispose { }
            } else {
                var lastShakeTime = 0L
                var shakeCount = 0

                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        if (event == null) return
                        val gX = event.values[0]
                        val gY = event.values[1]
                        val gZ = event.values[2]

                        // 1. Inclinazione 2D (con filtro a m/s^2)
                        val x = (-gX / 5.5f).coerceIn(-1f, 1f)
                        val y = ((gY - 6.0f) / 5.5f).coerceIn(-1f, 1f)
                        rawTiltX = x
                        rawTiltY = y

                        // 2. Rilevamento scuotimento vigoroso (Shake)
                        val gMagnitude = sqrt(gX * gX + gY * gY + gZ * gZ) / SensorManager.GRAVITY_EARTH
                        if (gMagnitude > 2.2f) {
                            val now = System.currentTimeMillis()
                            if (now - lastShakeTime < 650L) {
                                shakeCount++
                                if (shakeCount >= 2) {
                                    onShakeDetected()
                                    shakeCount = 0
                                }
                            } else {
                                shakeCount = 1
                            }
                            lastShakeTime = now
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }

                var isRegistered = false
                fun registerSensor() {
                    if (!isRegistered) {
                        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
                        isRegistered = true
                    }
                }

                fun unregisterSensor() {
                    if (isRegistered) {
                        sensorManager.unregisterListener(listener)
                        isRegistered = false
                        rawTiltX = 0f
                        rawTiltY = 0f
                    }
                }

                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    registerSensor()
                }

                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> registerSensor()
                        Lifecycle.Event.ON_PAUSE -> unregisterSensor()
                        else -> {}
                    }
                }

                lifecycleOwner.lifecycle.addObserver(observer)

                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    unregisterSensor()
                }
            }
        }
    }

    val smoothTiltX by animateFloatAsState(
        targetValue = if (enableTiltParallax) rawTiltX else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iris_tilt_x"
    )

    val smoothTiltY by animateFloatAsState(
        targetValue = if (enableTiltParallax) rawTiltY else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iris_tilt_y"
    )

    return remember(smoothTiltX, smoothTiltY) {
        IrisTiltState(smoothTiltX, smoothTiltY)
    }
}
