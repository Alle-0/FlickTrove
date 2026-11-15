package com.cinetrack.ui.components.card

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.CardRippleState
import com.cinetrack.util.VibrationHelper
import kotlinx.coroutines.launch

/**
 * Pulsante interattivo ad angolo inferiore per MovieCard:
 * gestisce la progressione degli stati (Watchlist/Occhio -> Visto/Check -> Promemoria/Campana),
 * animazioni di rimbalzo aptiche, oscillazione campana, indicatore circolare di avanzamento episodi.
 */
@Composable
fun MovieCardActionButton(
    movie: Movie,
    cardWidth: Dp,
    cardHeight: Dp,
    isFavorite: Boolean,
    isWatched: Boolean,
    isReminder: Boolean,
    computedProgress: Float,
    showActionHint: Boolean,
    isLarge: Boolean,
    rippleState: CardRippleState,
    onAction: (Movie) -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val hintReminder = stringResource(R.string.card_hint_manage_reminder)
    val hintEpisodes = stringResource(R.string.card_hint_manage_episodes)

    val appAccent = MaterialTheme.colorScheme.primary
    val isReleased = movie.isReleased
    val isTv = movie.mediaType == "tv"

    var optimisticFavorite by remember(isFavorite) { mutableStateOf(isFavorite) }
    var optimisticWatched by remember(isWatched) { mutableStateOf(isWatched) }
    var optimisticReminder by remember(isReminder) { mutableStateOf(isReminder) }

    val effectiveWatched = optimisticWatched
    val effectiveFavorite = optimisticFavorite
    val effectiveReminder = optimisticReminder

    val isPromemoriaAttiva = !isReleased && !effectiveWatched && (effectiveReminder || effectiveFavorite)
    val isOcchio = isReleased && !effectiveWatched && (effectiveReminder || effectiveFavorite)
    val isTick = effectiveWatched
    val isPlus = !effectiveWatched && isReleased && !isOcchio
    val isBellOutlined = !isReleased && !effectiveWatched && !(effectiveReminder || effectiveFavorite)

    val actionButtonScale = remember { Animatable(1f) }
    val actionIconScale = remember { Animatable(1f) }
    val actionScope = rememberCoroutineScope()

    val bellRotation = remember { Animatable(0f) }
    var bellHasInitialized by remember { mutableStateOf(false) }

    fun triggerActionAnimation() {
        actionScope.launch {
            if (isPlus) {
                actionIconScale.snapTo(0.70f)
                actionIconScale.animateTo(
                    1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            } else if (isOcchio) {
                actionIconScale.snapTo(0.65f)
                actionIconScale.animateTo(
                    1.28f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
                actionIconScale.animateTo(
                    1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            } else if (isTick) {
                actionIconScale.snapTo(0.65f)
                actionIconScale.animateTo(
                    1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            } else if (!isReleased) {
                bellRotation.animateTo(-25f, tween(60, easing = LinearEasing))
                bellRotation.animateTo(20f, tween(100, easing = LinearEasing))
                bellRotation.animateTo(-15f, tween(100, easing = LinearEasing))
                bellRotation.animateTo(10f, tween(100, easing = LinearEasing))
                bellRotation.animateTo(0f, tween(100, easing = FastOutSlowInEasing))
            }
        }
    }

    LaunchedEffect(isReminder, isFavorite) {
        if (bellHasInitialized && !isReleased && (isReminder || isFavorite)) {
            bellRotation.animateTo(-25f, tween(60, easing = LinearEasing))
            bellRotation.animateTo(20f, tween(100, easing = LinearEasing))
            bellRotation.animateTo(-15f, tween(100, easing = LinearEasing))
            bellRotation.animateTo(10f, tween(100, easing = LinearEasing))
            bellRotation.animateTo(0f, tween(100, easing = FastOutSlowInEasing))
        }
        bellHasInitialized = true
    }

    Box(
        modifier = modifier
            .padding(end = if (isLarge) 12.dp else 8.dp, bottom = if (isLarge) 12.dp else 8.dp)
            .size(if (isLarge) 34.dp else 24.dp)
            .graphicsLayer {
                scaleX = actionButtonScale.value
                scaleY = actionButtonScale.value
                clip = false
            }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                this.contentDescription = "Action"
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    actionScope.launch {
                        actionButtonScale.animateTo(
                            0.80f,
                            animationSpec = spring(stiffness = 10000f, dampingRatio = Spring.DampingRatioNoBouncy)
                        )
                    }
                    val up = waitForUpOrCancellation()
                    if (up != null) {
                        up.consume()
                        actionScope.launch {
                            actionButtonScale.animateTo(
                                1.15f,
                                animationSpec = spring(stiffness = 12000f, dampingRatio = Spring.DampingRatioNoBouncy)
                            )
                            actionButtonScale.animateTo(
                                1.0f,
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessMediumLow,
                                    dampingRatio = Spring.DampingRatioMediumBouncy
                                )
                            )
                        }

                        val rippleX = with(density) { cardWidth.toPx() - 20.dp.toPx() }
                        val rippleY = with(density) { cardHeight.toPx() - 20.dp.toPx() }
                        rippleState.trigger(Offset(rippleX, rippleY))
                        VibrationHelper.vibrateTick(context)
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)

                        if (!isReleased && !effectiveWatched) {
                            if (!effectiveWatched && !effectiveFavorite && !effectiveReminder) {
                                optimisticReminder = true
                                onAction(movie)
                            } else {
                                onMessage(hintReminder)
                            }
                        } else if (isTv) {
                            if (!effectiveWatched && !effectiveFavorite && !effectiveReminder) {
                                optimisticFavorite = true
                                onAction(movie)
                            } else if (showActionHint) {
                                onMessage(hintEpisodes)
                            }
                        } else {
                            if (!effectiveWatched && !effectiveFavorite && !effectiveReminder) {
                                optimisticFavorite = true
                                optimisticWatched = false
                                optimisticReminder = false
                            } else if (!effectiveWatched && (effectiveFavorite || effectiveReminder)) {
                                optimisticWatched = true
                                optimisticFavorite = false
                                optimisticReminder = false
                            }
                            onAction(movie)
                        }
                        triggerActionAnimation()
                    } else {
                        actionScope.launch {
                            actionButtonScale.animateTo(
                                1.0f,
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessMediumLow,
                                    dampingRatio = Spring.DampingRatioNoBouncy
                                )
                            )
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val targetCircularProgress = computedProgress
        val animatedCircularProgress by animateFloatAsState(
            targetValue = targetCircularProgress,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "movieCardCircularProgress"
        )
        val isHasCircularProgress = isTv && !effectiveWatched && (isOcchio || isPromemoriaAttiva || computedProgress > 0f)

        val actionBg = when {
            effectiveWatched -> appAccent.copy(alpha = 0.1f)
            isPromemoriaAttiva -> appAccent.copy(alpha = 0.1f)
            isOcchio -> HazeStyles.AccentYellow.copy(alpha = 0.1f)
            else -> HazeStyles.GlassColor.copy(alpha = 0.60f)
        }

        val actionBorder = when {
            effectiveWatched -> appAccent
            isPromemoriaAttiva -> appAccent
            isOcchio -> HazeStyles.AccentYellow
            else -> HazeStyles.GlassBorderColor
        }.copy(alpha = 0.3f)

        val actionTint = when {
            effectiveWatched -> appAccent
            isPromemoriaAttiva -> appAccent
            isOcchio -> HazeStyles.AccentYellow
            else -> Color.White
        }

        val iconSizeModifier = Modifier.size(
            if (isLarge) {
                if (isTick) 13.dp else if (isPlus) 16.dp else if (isOcchio) 20.dp else if (isBellOutlined) 20.dp else 21.dp
            } else {
                if (isTick) 10.dp else if (isPlus) 12.dp else if (isOcchio) 15.dp else if (isBellOutlined) 15.5.dp else 17.dp
            }
        )

        val buttonSize = if (isLarge) 30.dp else 22.dp
        val buttonModifier = Modifier
            .size(buttonSize)
            .background(color = actionBg, shape = CircleShape)
            .then(
                if (!isHasCircularProgress) Modifier.border(width = 1.dp, color = actionBorder, shape = CircleShape)
                else Modifier
            )

        Box(
            modifier = buttonModifier,
            contentAlignment = Alignment.Center
        ) {
            if (isHasCircularProgress) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val sw = if (isLarge) 2.dp.toPx() else 1.5.dp.toPx()
                    val strokeOffset = sw / 2
                    val arcSize = Size(size.width - sw, size.height - sw)
                    drawArc(
                        color = Color.White.copy(alpha = 0.15f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(strokeOffset, strokeOffset),
                        size = arcSize,
                        style = Stroke(width = sw)
                    )
                    if (animatedCircularProgress > 0f) {
                        drawArc(
                            color = appAccent,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedCircularProgress.coerceIn(0f, 1f),
                            useCenter = false,
                            topLeft = Offset(strokeOffset, strokeOffset),
                            size = arcSize,
                            style = Stroke(width = sw, cap = StrokeCap.Round)
                        )
                    }
                }
            }
            Icon(
                imageVector = when {
                    isTick -> ImageVector.vectorResource(id = R.drawable.ic_tick_card)
                    !isReleased -> if (isPromemoriaAttiva) ImageVector.vectorResource(id = R.drawable.ic_bell_piena) else ImageVector.vectorResource(id = R.drawable.ic_bell)
                    isOcchio -> ImageVector.vectorResource(id = R.drawable.ic_eye)
                    else -> ImageVector.vectorResource(id = R.drawable.ic_plus)
                },
                contentDescription = "Action",
                tint = actionTint,
                modifier = iconSizeModifier.graphicsLayer {
                    scaleX = actionIconScale.value
                    scaleY = actionIconScale.value
                    rotationZ = if (!isReleased && isPromemoriaAttiva) {
                        bellRotation.value
                    } else {
                        0f
                    }
                    if (!isReleased && isPromemoriaAttiva) {
                        transformOrigin = TransformOrigin(0.5f, 0.2f)
                    }
                }
            )
        }
    }
}
