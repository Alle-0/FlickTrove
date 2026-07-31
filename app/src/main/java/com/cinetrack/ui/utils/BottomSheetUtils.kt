package com.cinetrack.ui.utils

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Velocity

/**
 * Creates and remembers a NestedScrollConnection that intercepts all scrolling.
 * This is useful for placing inside a ModalBottomSheet when you want to allow
 * a child LazyColumn/LazyVerticalGrid to scroll without dragging the bottom sheet itself.
 */
@Composable
fun rememberBottomSheetNestedScrollConnection(): NestedScrollConnection {
    return remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                return Velocity(0f, available.y)
            }
        }
    }
}

/**
 * Modifier to block vertical drags from bubbling up to the BottomSheet.
 * Apply this to non-scrollable containers inside the bottom sheet (like headers)
 * if you want to prevent dismissing the sheet by dragging those areas.
 */
@Composable
fun Modifier.blockBottomSheetVerticalDrag(): Modifier {
    val touchSlop = LocalViewConfiguration.current.touchSlop
    return this.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
            var verticalDragged = false
            var totalDy = 0f
            var totalDx = 0f
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (change.isConsumed || !change.pressed) break
                val dy = change.position.y - change.previousPosition.y
                val dx = change.position.x - change.previousPosition.x
                totalDy += dy
                totalDx += dx
                if (!verticalDragged) {
                    if (kotlin.math.abs(totalDy) > touchSlop && kotlin.math.abs(totalDy) > kotlin.math.abs(totalDx)) {
                        verticalDragged = true
                    }
                }
                if (verticalDragged) {
                    change.consume()
                }
            }
        }
    }
}
