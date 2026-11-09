package com.cinetrack.ui.components.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cafe.adriel.voyager.navigator.tab.Tab
import com.cinetrack.R
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.screens.SettingsTab
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

@Composable
fun BoxScope.MainSearchFab(
    currentTab: Tab,
    contentHazeState: HazeState,
    isDimmed: Boolean = false,
    onDimmedAreaClick: (() -> Unit)? = null,
    onSearchClick: (Offset) -> Unit
) {
    if (isPrimaryMainTab(currentTab) && currentTab !is SettingsTab) {
        val scrollToTopState = com.cinetrack.ui.LocalScrollToTopState.current.value
        val isScrollToTopVisible = scrollToTopState.isVisible

        val animProgress = remember { Animatable(if (isScrollToTopVisible) 1f else 0f) }
        val searchBounceY = remember { Animatable(0f) }
        var hasTriggeredRecoil by remember { androidx.compose.runtime.mutableStateOf(false) }

        val animatedDimAlpha by animateFloatAsState(
            targetValue = if (isDimmed) 0.7f else 0f,
            animationSpec = tween(durationMillis = 300),
            label = "SearchFabDimAlpha"
        )

        LaunchedEffect(isScrollToTopVisible) {
            if (isScrollToTopVisible) {
                hasTriggeredRecoil = false
                animProgress.snapTo(0f)
                animProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                )
            } else {
                hasTriggeredRecoil = false
                animProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 220, easing = FastOutLinearInEasing)
                )
            }
        }

        val p = animProgress.value
        val isDetached = p >= 0.68f

        // Rinculo elastico del Search FAB al momento del distacco della goccia (snap a 3.5dp e rimbalzo a molla)
        LaunchedEffect(isDetached) {
            if (isDetached && !hasTriggeredRecoil) {
                hasTriggeredRecoil = true
                searchBounceY.snapTo(3.5f)
                searchBounceY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            } else if (!isDetached && p < 0.20f) {
                hasTriggeredRecoil = false
            }
        }

        val t = (p / 0.68f).coerceIn(0f, 1f)
        // Durante la trazione il Search FAB si stira organicamente verso l'alto (da 52.dp a 56.5.dp)
        val stretchHeight = if (!isDetached && p > 0.02f) 4.5.dp * t else 0.dp
        val fabShape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50)

        val fabCenter = remember { arrayOf<Offset?>(null) }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 90.dp)
                .offset(y = searchBounceY.value.dp)
                .zIndex(60f)
                .width(52.dp)
                .height(52.dp + stretchHeight)
                .onGloballyPositioned { coords ->
                    val pos = coords.positionInWindow()
                    val size = coords.size
                    fabCenter[0] = Offset(
                        pos.x + size.width / 2f,
                        pos.y + size.height / 2f
                    )
                }
                .bounceClick(scaleDown = 0.90f, enabled = !isDimmed) {
                    val center = fabCenter[0]
                    val startX = center?.x ?: 540f
                    val startY = center?.y ?: 1140f
                    onSearchClick(Offset(startX, startY))
                }
                .hazeGlass(
                    state = contentHazeState,
                    shape = fabShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_lente),
                contentDescription = stringResource(R.string.main_cd_search),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )

            // Scrim di oscuramento sincronizzato con modali e dialog
            if (animatedDimAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = animatedDimAlpha), fabShape)
                        .pointerInput(Unit) {
                            detectTapGestures {
                                onDimmedAreaClick?.invoke()
                            }
                        }
                )
            }
        }
    }
}

@Composable
fun BoxScope.MainScrollToTopFab(
    visible: Boolean,
    scrollProgress: Float = if (visible) 1f else 0f,
    contentHazeState: HazeState,
    isDimmed: Boolean = false,
    onDimmedAreaClick: (() -> Unit)? = null,
    onScrollToTop: () -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    // Animazione fluida a snap governata dalla visibilità (con stacco e rimbalzo elastico)
    val animProgress = remember { Animatable(if (visible) 1f else 0f) }
    var hasSnappedBounce by remember { androidx.compose.runtime.mutableStateOf(false) }
    val bounceOffsetY = remember { Animatable(0f) }
    val bounceScale = remember { Animatable(1f) }

    LaunchedEffect(visible) {
        if (visible) {
            hasSnappedBounce = false
            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 350,
                    easing = FastOutSlowInEasing
                )
            )
        } else {
            hasSnappedBounce = false
            animProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 220,
                    easing = FastOutLinearInEasing
                )
            )
        }
    }

    val p = animProgress.value

    LaunchedEffect(p >= 0.68f) {
        if (p >= 0.68f && !hasSnappedBounce) {
            hasSnappedBounce = true
            launch {
                bounceOffsetY.snapTo(4f)
                bounceOffsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            launch {
                bounceScale.snapTo(1.10f)
                bounceScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        } else if (p < 0.20f) {
            hasSnappedBounce = false
        }
    }

    val arrowOffsetY = remember { Animatable(0f) }
    val arrowAlpha = remember { Animatable(1f) }
    val animatedDimAlpha by animateFloatAsState(
        targetValue = if (isDimmed) 0.7f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "ScrollToTopDimAlpha"
    )
    if (p <= 0.005f && !visible) return

    val triggerScroll: () -> Unit = {
        coroutineScope.launch {
            try {
                arrowOffsetY.animateTo(-12f, tween(110, easing = FastOutLinearInEasing))
                arrowAlpha.snapTo(0f)
                arrowOffsetY.snapTo(12f)
                arrowAlpha.animateTo(1f, tween(80))
                arrowOffsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            } finally {
                arrowOffsetY.snapTo(0f)
                arrowAlpha.snapTo(1f)
            }
        }
        onScrollToTop()
    }

    // GEOMETRIA FISICA:
    // Search FAB: bottom = 90.dp, end = 20.dp, size = 52.dp (Centro Y = 116.dp, apice = 142.dp)
    // ScrollToTop a riposo: bottom = 146.dp, end = 22.dp, size = 48.dp (Centro Y = 170.dp)
    // Nel container (Y=0 a 194.dp, Y=104.dp a 90.dp):
    // - cx = 26.dp (centro comune)
    // - Y_rest = 194 - 170 = 24.dp
    // - Y_fab_top = 194 - 142 = 52.dp
    val isDetached = p >= 0.68f
    val overallAlpha = (p / 0.08f).coerceIn(0f, 1f)
    val iconAlpha = ((p - 0.65f) / 0.25f).coerceIn(0f, 1f) * arrowAlpha.value

    // Ritrazione rapida del filamento residuo verso il Search FAB dopo lo snap (come in SymbiontPullToRefresh)
    val filamentRecoil by animateFloatAsState(
        targetValue = if (isDetached) 0f else 1f,
        animationSpec = tween(durationMillis = 130, easing = FastOutLinearInEasing),
        label = "SymbiontFilamentRecoil"
    )

    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 20.dp, bottom = 90.dp)
            .size(width = 52.dp, height = 104.dp)
            .zIndex(59f)
    ) {
        // QUANDO È ANCORA ATTACCATO (0.02 < p < 0.68): emersione organica secondo le formule esatte di SymbiontPullToRefresh
        if (!isDetached && p > 0.02f) {
            val t = (p / 0.68f).coerceIn(0f, 1f)

            val symbioteShape = remember(t, density) {
                object : Shape {
                    override fun createOutline(
                        size: Size,
                        layoutDirection: LayoutDirection,
                        density: Density
                    ): Outline {
                        val cx = size.width / 2f
                        val cyFab = with(density) { 52.dp.toPx() }
                        // La sfera sale dall'apice del Search FAB (52.dp) alla posizione finale di riposo (24.dp)
                        val dropY = with(density) { (52.dp - 28.dp * t).toPx() }
                        // Il raggio sboccia da un piccolo germoglio (2.dp) fino a 17.dp (esattamente come 2.dp -> 20.dp nel pull-to-refresh)
                        val rDrop = with(density) { (2.dp + 15.dp * t).toPx() }.coerceAtLeast(0.1f)

                        // Base di attacco sul Search FAB
                        val rBase = with(density) { (3.5.dp + 3.dp * (1f - t * 0.50f)).toPx() }
                        val xBase = rBase

                        // Restringimento del punto vita (waist) durante lo stiro del filamento
                        val waistHalfWidth = (xBase.coerceAtMost(rDrop) * (1f - t * 0.72f))
                            .coerceAtLeast(with(density) { 1.2.dp.toPx() })

                        val distY = (cyFab - dropY).coerceAtLeast(1f)
                        val c1Y = cyFab - distY * 0.35f
                        // Pointedness della goccia organica: arrotondata all'inizio, cono affilato durante lo stiro
                        val pointedness = 0.3f + 0.9f * t
                        val c2Y = (dropY + rDrop * pointedness).coerceAtMost(c1Y - distY * 0.1f)
                        val c1X = cx + waistHalfWidth * 1.15f

                        val path = Path().apply {
                            // 1. Base sinistra sul Search FAB
                            moveTo(cx - xBase, cyFab)

                            // 2. Ponte liquido sinistro verso l'equatore della goccia
                            cubicTo(
                                x1 = cx - (c1X - cx), y1 = c1Y,
                                x2 = cx - rDrop, y2 = c2Y,
                                x3 = cx - rDrop, y3 = dropY
                            )

                            // 3. Calotta superiore circolare (arco di 180 gradi)
                            arcTo(
                                rect = Rect(
                                    left = cx - rDrop,
                                    top = dropY - rDrop,
                                    right = cx + rDrop,
                                    bottom = dropY + rDrop
                                ),
                                startAngleDegrees = 180f,
                                sweepAngleDegrees = 180f,
                                forceMoveTo = false
                            )

                            // 4. Ponte liquido destro di ritorno verso il Search FAB
                            cubicTo(
                                x1 = cx + rDrop, y1 = c2Y,
                                x2 = c1X, y2 = c1Y,
                                x3 = cx + xBase, y3 = cyFab
                            )
                            lineTo(cx - xBase, cyFab)
                            close()
                        }
                        return Outline.Generic(path)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = overallAlpha }
                    .hazeGlass(
                        state = contentHazeState,
                        shape = symbioteShape,
                        borderWidth = 0.dp
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cyFab = with(density) { 52.dp.toPx() }
                    val dropY = with(density) { (52.dp - 28.dp * t).toPx() }
                    val rDrop = with(density) { (2.dp + 15.dp * t).toPx() }.coerceAtLeast(0.1f)

                    val rBase = with(density) { (3.5.dp + 3.dp * (1f - t * 0.50f)).toPx() }
                    val xBase = rBase

                    val waistHalfWidth = (xBase.coerceAtMost(rDrop) * (1f - t * 0.72f))
                        .coerceAtLeast(with(density) { 1.2.dp.toPx() })

                    val distY = (cyFab - dropY).coerceAtLeast(1f)
                    val c1Y = cyFab - distY * 0.35f
                    val pointedness = 0.3f + 0.9f * t
                    val c2Y = (dropY + rDrop * pointedness).coerceAtMost(c1Y - distY * 0.1f)
                    val c1X = cx + waistHalfWidth * 1.15f

                    val strokePath = Path().apply {
                        moveTo(cx - xBase, cyFab)
                        cubicTo(
                            x1 = cx - (c1X - cx), y1 = c1Y,
                            x2 = cx - rDrop, y2 = c2Y,
                            x3 = cx - rDrop, y3 = dropY
                        )
                        arcTo(
                            rect = Rect(
                                left = cx - rDrop,
                                top = dropY - rDrop,
                                right = cx + rDrop,
                                bottom = dropY + rDrop
                            ),
                            startAngleDegrees = 180f,
                            sweepAngleDegrees = 180f,
                            forceMoveTo = false
                        )
                        cubicTo(
                            x1 = cx + rDrop, y1 = c2Y,
                            x2 = c1X, y2 = c1Y,
                            x3 = cx + xBase, y3 = cyFab
                        )
                    }

                    drawPath(
                        strokePath,
                        color = HazeStyles.GlassBorderColor.copy(alpha = HazeStyles.GlassBorderAlphaTop * overallAlpha),
                        style = Stroke(width = with(density) { 1.dp.toPx() })
                    )
                }
            }
        }

        // RITRAZIONE RESIDUA DEL FILAMENTO DOPO LO SNAP (come in SymbiontPullToRefresh)
        if (isDetached && filamentRecoil > 0.02f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cyFab = with(density) { 56.dp.toPx() }
                val snapHeight = with(density) { 8.dp.toPx() } * filamentRecoil
                val snapWidth = with(density) { 1.8.dp.toPx() } * filamentRecoil
                val filamentPath = Path().apply {
                    moveTo(cx - snapWidth, cyFab)
                    lineTo(cx + snapWidth, cyFab)
                    lineTo(cx, cyFab - snapHeight)
                    close()
                }
                drawPath(
                    filamentPath,
                    color = HazeStyles.GlassBorderColor.copy(alpha = HazeStyles.GlassBorderAlphaTop * filamentRecoil),
                    style = Stroke(width = with(density) { 1.dp.toPx() })
                )
            }
        }

        // QUANDO È STACCATO (p >= 0.68): la goccia si stabilizza nel pulsante hazeGlass con rimbalzo elastico
        if (isDetached) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(48.dp)
                    .graphicsLayer {
                        alpha = overallAlpha
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .offset(y = bounceOffsetY.value.dp)
                        .scale(bounceScale.value)
                        .size(34.dp)
                        .bounceClick(scaleDown = 0.90f, enabled = visible && !isDimmed) {
                            triggerScroll()
                        }
                        .hazeGlass(
                            state = contentHazeState,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                        contentDescription = stringResource(R.string.main_cd_scroll_to_top),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(20.dp)
                            .offset(y = arrowOffsetY.value.dp)
                            .alpha(iconAlpha)
                            .rotate(-90f)
                    )

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
        }
    }
}



