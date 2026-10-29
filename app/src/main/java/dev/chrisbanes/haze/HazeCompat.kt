package dev.chrisbanes.haze

import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.blur.HazeBlurStyle

/**
 * Backward compatibility bridge for Haze 2.0 source registration and styles.
 *
 * Placed in package `dev.chrisbanes.haze` to seamlessly satisfy existing
 * `import dev.chrisbanes.haze.haze` and `import dev.chrisbanes.haze.HazeStyle` statements
 * across all screen containers without churn.
 */
typealias HazeStyle = HazeBlurStyle

fun Modifier.haze(state: HazeState, zIndex: Float = 0f): Modifier =
    this.hazeSource(state = state, zIndex = zIndex)

fun Modifier.haze(state: HazeState, style: Any?, zIndex: Float = 0f): Modifier =
    this.hazeSource(state = state, zIndex = zIndex)

