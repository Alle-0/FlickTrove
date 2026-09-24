package com.cinetrack.ui.components.shared

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cinetrack.ui.utils.bounceClick
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Indicatore paginazione elastico e fluido (Symbiont / Worm) per [PagerState].
 * Traccia a 60fps lo scorrimento continuo, modula l'inerzia e la deformazione d'atterraggio
 * in base alla velocità effettiva e supporta la persistenza opzionale del colore sui passi precedenti.
 */
@Composable
fun SymbiontPagerIndicator(
    pagerState: PagerState,
    pageCount: Int,
    modifier: Modifier = Modifier,
    indicatorHeight: Dp = 20.dp,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    dotSize: Dp = 6.dp,
    spacing: Dp = 8.dp,
    persistPreviousDots: Boolean = false,
    isInfinite: Boolean = false,
    onDotClick: ((Int) -> Unit)? = null
) {
    if (pageCount <= 0) return

    val distance = dotSize + spacing
    val canvasWidth = dotSize + (distance * (pageCount - 1).coerceAtLeast(0))

    // Tracciamento velocità di scorrimento (distingue swipe lenti da swipe veloci o auto-scroll)
    var currentSpeedPagesPerSec by remember { mutableFloatStateOf(1.5f) }
    var lastPos by remember { mutableFloatStateOf(0f) }
    var lastTimeNanos by remember { mutableLongStateOf(0L) }

    LaunchedEffect(pagerState) {
        snapshotFlow {
            pagerState.currentPage to pagerState.currentPageOffsetFraction
        }.collect { (page, offset) ->
            val now = System.nanoTime()
            val currentPos = page + offset
            if (lastTimeNanos > 0L) {
                val dtSec = (now - lastTimeNanos) / 1_000_000_000f
                if (dtSec in 0.001f..0.2f) {
                    val instantSpeed = abs(currentPos - lastPos) / dtSec
                    currentSpeedPagesPerSec = currentSpeedPagesPerSec * 0.5f + instantSpeed * 0.5f
                }
            }
            lastPos = currentPos
            lastTimeNanos = now
        }
    }

    Box(
        modifier = modifier.height(indicatorHeight),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .width(canvasWidth)
                .height(dotSize)
        ) {
            val canvasHeightPx = size.height
            val centerY = canvasHeightPx / 2f
            val dotRadius = dotSize.toPx() / 2f

            // 1. Pallini base (inattivi o passati)
            val currentSettled = if (isInfinite) {
                ((pagerState.currentPage % pageCount) + pageCount) % pageCount
            } else {
                pagerState.currentPage
            }

            for (i in 0 until pageCount) {
                val cx = (i * distance.toPx()) + dotRadius
                val isPast = persistPreviousDots && i < currentSettled
                val dotColor = if (isPast) accentColor else Color.White.copy(alpha = if (persistPreviousDots) 0.2f else 0.35f)
                drawCircle(
                    color = dotColor,
                    radius = dotRadius,
                    center = Offset(cx, centerY)
                )
            }

            // 2. Symbiont worm attivo
            val virtualScrollPosition = (pagerState.currentPage + pagerState.currentPageOffsetFraction).toFloat()
            val scrollPosition = if (isInfinite) {
                val wrapped = ((virtualScrollPosition % pageCount) + pageCount) % pageCount
                if (wrapped >= pageCount) 0f else wrapped
            } else {
                virtualScrollPosition.coerceIn(0f, (pageCount - 1).toFloat())
            }

            val floorPos = floor(scrollPosition.toDouble()).toFloat()
            val fraction = scrollPosition - floorPos

            val bounceFactor = ((currentSpeedPagesPerSec - 0.5f) / 1.5f).coerceIn(0f, 1f)

            val headBase = FastOutSlowInEasing.transform((fraction * 1.35f).coerceAtMost(1f))
            val tailBase = FastOutSlowInEasing.transform(((fraction - 0.25f) / 0.75f).coerceIn(0f, 1f))

            val arrivalPhaseForward = ((fraction - 0.70f) / 0.30f).coerceIn(0f, 1f)
            val elongationForward = sin(arrivalPhaseForward * Math.PI.toFloat()) * bounceFactor * 0.18f

            val arrivalPhaseBackward = ((0.30f - fraction) / 0.30f).coerceIn(0f, 1f)
            val elongationBackward = sin(arrivalPhaseBackward * Math.PI.toFloat()) * bounceFactor * 0.18f

            val rightNode = floorPos + headBase + elongationForward
            val leftNode = floorPos + tailBase - elongationBackward

            val wormHeight = dotSize.toPx()
            val wormTop = centerY - (wormHeight / 2f)

            val leftX = leftNode * distance.toPx()
            val rightX = (rightNode * distance.toPx()) + dotSize.toPx()

            if (rightX > leftX) {
                drawRoundRect(
                    color = accentColor,
                    topLeft = Offset(leftX, wormTop),
                    size = Size(rightX - leftX, wormHeight),
                    cornerRadius = CornerRadius(dotRadius, dotRadius)
                )
            }
        }

        // 3. Touch targets invisibili
        if (onDotClick != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(canvasWidth + 16.dp)
            ) {
                val itemWidth = (canvasWidth + 16.dp) / pageCount
                repeat(pageCount) { index ->
                    Box(
                        modifier = Modifier
                            .width(itemWidth)
                            .height(20.dp)
                            .bounceClick(scaleDown = 0.85f) {
                                onDotClick(index)
                            }
                    )
                }
            }
        }
    }
}

/**
 * Indicatore paginazione elastico e fluido (Symbiont / Worm) per wizard o step discreti ([currentPage]: [Int]).
 * Utilizza molle asincrone doppie per animare testa e coda durante il cambio di step.
 */
@Composable
fun SymbiontPagerIndicator(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier,
    indicatorHeight: Dp = 20.dp,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    dotSize: Dp = 6.dp,
    spacing: Dp = 8.dp,
    persistPreviousDots: Boolean = true,
    onDotClick: ((Int) -> Unit)? = null
) {
    if (pageCount <= 0) return

    val distance = dotSize + spacing
    val canvasWidth = dotSize + (distance * (pageCount - 1).coerceAtLeast(0))

    val headProgress = remember { Animatable(currentPage.toFloat()) }
    val tailProgress = remember { Animatable(currentPage.toFloat()) }

    LaunchedEffect(currentPage) {
        val target = currentPage.toFloat()
        val currentHead = headProgress.value
        if (target >= currentHead) {
            launch {
                headProgress.animateTo(
                    targetValue = target,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            launch {
                tailProgress.animateTo(
                    targetValue = target,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        } else {
            launch {
                tailProgress.animateTo(
                    targetValue = target,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            launch {
                headProgress.animateTo(
                    targetValue = target,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }
    }

    Box(
        modifier = modifier.height(indicatorHeight),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .width(canvasWidth)
                .height(dotSize)
        ) {
            val centerY = size.height / 2f
            val dotRadius = dotSize.toPx() / 2f

            for (i in 0 until pageCount) {
                val cx = (i * distance.toPx()) + dotRadius
                val isPast = persistPreviousDots && i < currentPage
                val dotColor = if (isPast) accentColor else Color.White.copy(alpha = if (persistPreviousDots) 0.2f else 0.35f)
                drawCircle(
                    color = dotColor,
                    radius = dotRadius,
                    center = Offset(cx, centerY)
                )
            }

            val minNode = min(headProgress.value, tailProgress.value)
            val maxNode = max(headProgress.value, tailProgress.value)
            val leftX = minNode * distance.toPx()
            val rightX = (maxNode * distance.toPx()) + dotSize.toPx()
            val wormHeight = dotSize.toPx()
            val wormTop = centerY - (wormHeight / 2f)

            if (rightX > leftX) {
                drawRoundRect(
                    color = accentColor,
                    topLeft = Offset(leftX, wormTop),
                    size = Size(rightX - leftX, wormHeight),
                    cornerRadius = CornerRadius(dotRadius, dotRadius)
                )
            }
        }

        if (onDotClick != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(canvasWidth + 16.dp)
            ) {
                val itemWidth = (canvasWidth + 16.dp) / pageCount
                repeat(pageCount) { index ->
                    Box(
                        modifier = Modifier
                            .width(itemWidth)
                            .height(20.dp)
                            .bounceClick(scaleDown = 0.85f) {
                                onDotClick(index)
                            }
                    )
                }
            }
        }
    }
}
