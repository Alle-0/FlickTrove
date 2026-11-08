package com.cinetrack.ui.components.navigation
import com.cinetrack.R

import androidx.compose.ui.res.vectorResource
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.LocalBackdrop
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import androidx.compose.ui.graphics.Brush
import dev.chrisbanes.haze.HazeState
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.tanh

@Composable
fun GlassyBottomBar(
    hazeState: HazeState,
    selectedRoute: String?,
    onNavigate: (String) -> Unit,
    isDimmed: Boolean = false,
    onDimmedAreaClick: (() -> Unit)? = null,
    currentMediaMode: String? = null,
    onSwitchMediaMode: ((String) -> Unit)? = null
) {
    val backdrop = LocalBackdrop.current
    val accentColor = MaterialTheme.colorScheme.primary
    val animatedDimAlpha by animateFloatAsState(
        targetValue = if (isDimmed) 0.7f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "BottomBarDimAlpha"
    )

    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    // Synchronous physical drag state for 100% zero-latency tracking (fast swipes)
    var dragX by remember { mutableFloatStateOf(0f) }
    var isSpringing by remember { mutableStateOf(false) }
    var springJob by remember { mutableStateOf<Job?>(null) }
    var releaseDirectionIsRight by remember { mutableStateOf<Boolean?>(null) }
    val animDrag = remember { Animatable(0f) }
    var hasFiredHaptic by remember { mutableStateOf(false) }

    val thresholdRawDp = 115f // Commitment threshold: reached only after dragging past center through progressive tension
    val maxDragDp = 10f // Bar is quasi-ferma: heavy progressive damping with 10dp asymptotic ceiling

    val effectiveRaw = if (isSpringing) animDrag.value else dragX
    val isDragging = abs(effectiveRaw) > 3f * density.density
    val isThresholdCrossed = (abs(effectiveRaw) / density.density) >= thresholdRawDp
    val isDraggingRight = if (isSpringing && releaseDirectionIsRight != null) releaseDirectionIsRight == true else (effectiveRaw >= 0f)

    val dragModifier = if (onSwitchMediaMode != null && !isDimmed) {
        Modifier.pointerInput(currentMediaMode) {
            detectHorizontalDragGestures(
                onDragStart = {
                    springJob?.cancel()
                    isSpringing = false
                    dragX = 0f
                    releaseDirectionIsRight = null
                    hasFiredHaptic = false
                },
                onDragEnd = {
                    val finalDragDp = dragX / density.density
                    if (finalDragDp >= thresholdRawDp) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSwitchMediaMode.invoke("tv")
                    } else if (finalDragDp <= -thresholdRawDp) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSwitchMediaMode.invoke("movie")
                    }
                    hasFiredHaptic = false

                    val startPos = dragX
                    releaseDirectionIsRight = if (abs(startPos) > 4f) startPos >= 0f else null
                    springJob = coroutineScope.launch {
                        isSpringing = true
                        animDrag.snapTo(startPos)
                        animDrag.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = 0.58f,
                                stiffness = 500f
                            )
                        )
                        dragX = 0f
                        releaseDirectionIsRight = null
                        isSpringing = false
                    }
                },
                onDragCancel = {
                    hasFiredHaptic = false
                    val startPos = dragX
                    releaseDirectionIsRight = if (abs(startPos) > 4f) startPos >= 0f else null
                    springJob = coroutineScope.launch {
                        isSpringing = true
                        animDrag.snapTo(startPos)
                        animDrag.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = 0.58f,
                                stiffness = 500f
                            )
                        )
                        dragX = 0f
                        releaseDirectionIsRight = null
                        isSpringing = false
                    }
                },
                onHorizontalDrag = { change, dragAmount ->
                    dragX += dragAmount

                    val currentRawDp = dragX / density.density
                    if (!hasFiredHaptic && abs(currentRawDp) >= thresholdRawDp) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        hasFiredHaptic = true
                    } else if (hasFiredHaptic && abs(currentRawDp) < thresholdRawDp) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        hasFiredHaptic = false
                    }

                    if (abs(dragX) > 6f) {
                        change.consume()
                    }
                }
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 20.dp, top = 4.dp)
            .height(60.dp)
            .offset {
                if (onSwitchMediaMode == null) return@offset IntOffset.Zero
                val raw = effectiveRaw
                val rawDp = raw / density.density
                val sign = if (raw >= 0f) 1f else -1f
                val absRawDp = abs(rawDp)
                val barOffsetMagnitude = (maxDragDp * tanh((absRawDp / 110f).toDouble())).toFloat()
                val dampedOffsetDp = sign * barOffsetMagnitude
                IntOffset((dampedOffsetDp * density.density).roundToInt(), 0)
            }
            .graphicsLayer {
                if (onSwitchMediaMode == null) return@graphicsLayer
                val raw = effectiveRaw
                val rawDp = raw / density.density
                val absRawDp = abs(rawDp)
                val barOffsetMagnitude = (maxDragDp * tanh((absRawDp / 110f).toDouble())).toFloat()
                val stretch = (barOffsetMagnitude / maxDragDp).coerceIn(0f, 1f)

                // Elastic deformation (organic silicone glass rebound)
                this.scaleX = 1f + stretch * 0.024f
                this.scaleY = 1f - stretch * 0.015f
                this.transformOrigin = TransformOrigin(if (raw >= 0f) 0.2f else 0.8f, 0.5f)
            }
            .then(dragModifier),
        contentAlignment = Alignment.Center
    ) {
        // BACKGROUND BLUR LAYER WITH OPTICAL REFRACTION (Liquid Glass)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .then(
                    if (backdrop != null) {
                        Modifier.drawPlainBackdrop(
                            backdrop = backdrop,
                            shape = { CircleShape },
                            effects = {
                                vibrancy()
                                blur(10.dp.toPx())
                                lens(
                                    refractionHeight = 18.dp.toPx(),
                                    refractionAmount = 26.dp.toPx(),
                                    depthEffect = false,
                                    chromaticAberration = true
                                )
                            },
                            onDrawSurface = {
                                // Deep dark obsidian liquid glass tint without specular white edge
                                drawRoundRect(
                                    color = Color(0xFF0C0C10).copy(alpha = 0.52f),
                                    cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                                )
                            }
                        )
                    } else {
                        Modifier.hazeGlass(
                            state = hazeState, 
                            shape = CircleShape,
                            useRefraction = false
                        )
                    }
                )
                .drawWithContent {
                    drawContent()
                    if (onSwitchMediaMode != null) {
                        val raw = effectiveRaw
                        val rawDp = raw / density.density
                        val absRawDp = abs(rawDp)
                        // Progressive glow: begins at 75dp (center), smoothly intensifies up to 115dp (soglia)
                        val glowProgress = ((absRawDp - 75f) / 40f).coerceIn(0f, 1f)
                        if (glowProgress > 0f) {
                            val strokeWidth = (1.2.dp * (1f + 0.25f * glowProgress)).toPx()
                            val halfStroke = strokeWidth / 2f
                            val radius = (size.height - strokeWidth) / 2f
                            drawRoundRect(
                                color = accentColor.copy(alpha = 0.75f * glowProgress),
                                topLeft = Offset(halfStroke, halfStroke),
                                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                                style = Stroke(width = strokeWidth),
                                cornerRadius = CornerRadius(radius, radius)
                            )
                        }
                    }
                }
        )

        // 4 NAV ITEMS (ROW) - Smoothly fades out as horizontal drag displacement starts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .graphicsLayer {
                    if (onSwitchMediaMode == null) return@graphicsLayer
                    val rawDp = abs(effectiveRaw) / density.density
                    // Icons remain solid during initial nudge, then fade smoothly between 18dp and 50dp
                    alpha = (1f - ((rawDp - 18f) / 32f)).coerceIn(0f, 1f)
                },
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                icon = ImageVector.vectorResource(id = R.drawable.ic_home),
                animatedIconRes = R.drawable.ic_home_anim,
                label = "Home",
                isSelected = selectedRoute == "feed",
                enabled = !isDimmed && !isDragging,
                onClick = { onNavigate("feed") },
                accentColor = accentColor
            )
            NavItem(
                icon = ImageVector.vectorResource(id = R.drawable.ic_segnalibro),
                animatedIconRes = R.drawable.ic_segnalibro_anim,
                label = stringResource(R.string.bottom_bar_to_watch),
                isSelected = selectedRoute == "index",
                enabled = !isDimmed && !isDragging,
                onClick = { onNavigate("index") },
                accentColor = accentColor
            )
            NavItem(
                icon = ImageVector.vectorResource(id = R.drawable.ic_tick_pieno),
                animatedIconRes = R.drawable.ic_tick_pieno_anim,
                label = stringResource(R.string.bottom_bar_watched),
                isSelected = selectedRoute == "visti",
                enabled = !isDimmed && !isDragging,
                onClick = { onNavigate("visti") },
                accentColor = accentColor
            )
            NavItem(
                icon = ImageVector.vectorResource(id = R.drawable.ic_persona),
                animatedIconRes = R.drawable.ic_persona_anim,
                label = stringResource(R.string.bottom_bar_account),
                isSelected = selectedRoute == "account",
                enabled = !isDimmed && !isDragging,
                onClick = { onNavigate("account") },
                accentColor = accentColor
            )
        }

        // DIRECTIONAL 35MM FILMSTRIP TRACK - Fluid cinema-first presentation
        if (onSwitchMediaMode != null) {
            Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    val raw = effectiveRaw
                    val rawDp = raw / density.density
                    val absRawDp = abs(rawDp)
                    val dirIsRight = if (isSpringing && releaseDirectionIsRight != null) {
                        releaseDirectionIsRight == true
                    } else {
                        raw >= 0f
                    }

                    // Strictly unidirectional during spring rebound: if the spring overshoots past 0
                    // to the opposite side, alpha is immediately 0 to prevent reverse label flashes.
                    // During active user drag, it is fully dynamic in both directions.
                    val isSameSide = if (isSpringing && releaseDirectionIsRight != null) {
                        if (releaseDirectionIsRight == true) raw >= 0f else raw <= 0f
                    } else {
                        true
                    }
                    alpha = if (isSameSide) {
                        ((absRawDp - 20f) / 25f).coerceIn(0f, 1f)
                    } else {
                        0f
                    }

                    // Phase 2: Travels smoothly from the entry edge (36dp) to the exact center (0dp) between 20dp and 75dp
                    val toCenterProgress = ((absRawDp - 20f) / 55f).coerceIn(0f, 1f)
                    val entryTravel = 36f * (1f - toCenterProgress) // 36dp down to 0dp

                    // Phase 3: Past center (75dp+), progressive resistance gently builds up through the 115dp threshold
                    val pastCenter = (absRawDp - 75f).coerceAtLeast(0f)
                    val pastCenterTravel = (28f * tanh((pastCenter / 60f).toDouble())).toFloat()

                    val slideOffsetDp = if (dirIsRight) {
                        // TV: enters from left (-36dp), hits center (0dp) at 75dp, pulls right with gentle progressive resistance
                        -entryTravel + pastCenterTravel
                    } else {
                        // Movies: enters from right (+36dp), hits center (0dp) at 75dp, pulls left with gentle progressive resistance
                        entryTravel - pastCenterTravel
                    }
                    translationX = slideOffsetDp * density.density
                },
            contentAlignment = Alignment.Center
        ) {
            val movieLabel = stringResource(R.string.home_tab_movies).uppercase()
            val tvLabel = stringResource(R.string.home_tab_tv_series).uppercase()
            val contentColor by animateColorAsState(
                targetValue = if (isThresholdCrossed) accentColor else Color.White,
                animationSpec = tween(durationMillis = 150),
                label = "switchContentColor"
            )

            // Micro-nudge and scale bump for the directional arrow when threshold is engaged
            val arrowOffset by animateFloatAsState(
                targetValue = if (isThresholdCrossed) 3.5f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "arrowNudgeOffset"
            )
            val arrowScale by animateFloatAsState(
                targetValue = if (isThresholdCrossed) 1.22f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "arrowScaleBump"
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isDraggingRight) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_left),
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer {
                                translationX = -arrowOffset * density.density
                                scaleX = arrowScale
                                scaleY = arrowScale
                            }
                    )
                }

                Text(
                    text = if (isDraggingRight) tvLabel else movieLabel,
                    color = contentColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Default,
                    letterSpacing = 3.sp
                )

                if (isDraggingRight) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer {
                                translationX = arrowOffset * density.density
                                scaleX = arrowScale
                                scaleY = arrowScale
                            }
                    )
                }
            }
        }

        }

        // DIM OVERLAY
        if (animatedDimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = animatedDimAlpha), CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            onDimmedAreaClick?.invoke()
                        }
                    }
            )
        }
    }
}

@Composable
private fun RowScope.NavItem(
    icon: ImageVector,
    animatedIconRes: Int? = null,
    label: String,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    accentColor: Color
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Bounce animation when becoming selected
    val iconScale = remember { androidx.compose.animation.core.Animatable(1f) }
    androidx.compose.runtime.LaunchedEffect(isSelected) {
        if (isSelected) {
            iconScale.animateTo(0.85f, tween(100, easing = androidx.compose.animation.core.LinearOutSlowInEasing))
            iconScale.animateTo(1.1f, androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium))
            iconScale.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow))
        }
    }
    
    // Scale animation ONLY if not selected and enabled
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && !isSelected && enabled) 0.92f else 1f, 
        label = "navScale"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isSelected // Disable click if dimmed OR already on this screen
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val baseColor = if (isSelected) accentColor else Color.White.copy(alpha = 0.6f)
        val tintColor by animateColorAsState(
            targetValue = if (isPressed) baseColor.copy(alpha = 0.9f) else baseColor,
            label = "iconTint"
        )

        Box(contentAlignment = Alignment.Center) {
            // Sfondo fisso: quando è selezionato diventa un "binario" semitrasparente
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected && animatedIconRes != null) Color.White.copy(alpha = 0.25f) else tintColor,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = iconScale.value
                        scaleY = iconScale.value
                    }
            )
            
            if (isSelected && animatedIconRes != null) {
                val animatedIcon = AnimatedImageVector.animatedVectorResource(id = animatedIconRes)
                var atEnd by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    atEnd = true
                }
                val painter = rememberAnimatedVectorPainter(animatedIcon, atEnd = atEnd)
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer {
                            scaleX = iconScale.value
                            scaleY = iconScale.value
                        }
                )
            }
        }
        
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            color = tintColor,
            modifier = Modifier.padding(top = 4.dp),
            letterSpacing = 0.5.sp
        )

    }
}
