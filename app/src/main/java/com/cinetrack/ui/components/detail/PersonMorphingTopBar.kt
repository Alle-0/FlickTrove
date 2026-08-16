package com.cinetrack.ui.components.detail

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.cinetrack.R
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState

@Composable
fun PersonMorphingTopBar(
    title: String,
    localHazeState: HazeState,
    symbioteProgress: Float,
    detailStackDepth: Int,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val density = LocalDensity.current

    // ── Press states for top bar buttons ──────────────────────────────────────
    var isBackPressed by remember { mutableStateOf(false) }
    val backIconScale by animateFloatAsState(
        targetValue = if (isBackPressed) 0.88f else 1f,
        animationSpec = spring(
            stiffness = if (isBackPressed) 10000f else Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "BackIconScale"
    )

    var isShareButtonPressed by remember { mutableStateOf(false) }
    val shareIconScale by animateFloatAsState(
        targetValue = if (isShareButtonPressed) 0.88f else 1f,
        animationSpec = spring(
            stiffness = if (isShareButtonPressed) 10000f else Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "ShareIconScale"
    )

    // ── Morphing transition ────────────────────────────────────────────────────
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val collapsedPillWidth = screenWidth - 40.dp
    
    val currentEffectiveProgress = symbioteProgress.coerceIn(0f, 1f)
    val modalCorner = 22.dp

    // ── Symbiote pill shape ────────────────────────────────────────────────────
    val symbioteShape: Shape = remember(currentEffectiveProgress, density, modalCorner) {
        GenericShape { size, _ ->
            val circleSize = with(density) { 44.dp.toPx() }
            val progress = currentEffectiveProgress
            val pillWidth = size.width
            val pillHeight = size.height
            val radius = with(density) { modalCorner.toPx() }

            if (progress <= 0.01f && pillHeight <= with(density) { 45.dp.toPx() }) return@GenericShape

            val stretchWidth = circleSize + (pillWidth / 2f - circleSize) * progress
            val p4 = progress * progress * progress * progress
            val innerRadius = radius * (1f - p4)

            val pathLeft = androidx.compose.ui.graphics.Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = 0f, top = 0f, right = stretchWidth + 2f, bottom = pillHeight,
                        topLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
                        topRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(innerRadius),
                        bottomRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(innerRadius),
                        bottomLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(radius)
                    )
                )
            }
            val pathRight = androidx.compose.ui.graphics.Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = pillWidth - stretchWidth - 2f, top = 0f, right = pillWidth, bottom = pillHeight,
                        topLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(innerRadius),
                        topRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
                        bottomRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
                        bottomLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(innerRadius)
                    )
                )
            }
            addPath(
                androidx.compose.ui.graphics.Path.combine(
                    PathOperation.Union, pathLeft, pathRight
                )
            )
        }
    }

    // ── Main morphing container ────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(11f)
            .statusBarsPadding()
            .displayCutoutPadding()
            .padding(top = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier.size(width = collapsedPillWidth, height = 44.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // Glass background
            if (currentEffectiveProgress > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            clip = true
                            shape = symbioteShape
                        }
                        .hazeGlass(
                            state = localHazeState,
                            shape = symbioteShape,
                            useOffscreenStrategy = true
                        )
                )
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // ── Pill header row (44dp) ─────────────────────────────────────

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    // Left: Back + Home
                    Row(
                        modifier = Modifier.align(Alignment.CenterStart),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Back button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isBackPressed = true
                                            try { awaitRelease() } finally { isBackPressed = false }
                                        },
                                        onTap = {
                                            onBackClick()
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentEffectiveProgress <= 0.01f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .hazeGlass(
                                            state = localHazeState,
                                            shape = CircleShape,
                                            blurRadius = HazeStyles.SmallGlassBlurRadius,
                                            useOffscreenStrategy = true
                                        )
                                )
                            }
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_left),
                                contentDescription = stringResource(R.string.detail_content_desc_back),
                                tint = Color.White,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer {
                                        scaleX = backIconScale
                                        scaleY = backIconScale
                                    }
                            )
                        }

                        // Home FAB — visible from 3rd detail screen onwards
                        val homeButtonVisible = detailStackDepth >= 3
                        val homeButtonAlpha by animateFloatAsState(
                            targetValue = if (homeButtonVisible) 1f else 0f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "HomeFabAlpha"
                        )
                        val homeButtonScale by animateFloatAsState(
                            targetValue = if (homeButtonVisible) 1f else 0.6f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "HomeFabScale"
                        )
                        if (homeButtonAlpha > 0.01f) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .graphicsLayer {
                                        alpha = homeButtonAlpha
                                        scaleX = homeButtonScale
                                        scaleY = homeButtonScale
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentEffectiveProgress <= 0.01f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .hazeGlass(
                                                state = localHazeState,
                                                shape = CircleShape,
                                                blurRadius = HazeStyles.SmallGlassBlurRadius,
                                                useOffscreenStrategy = true
                                            )
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .bounceClick { onHomeClick() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_home),
                                        contentDescription = stringResource(R.string.detail_content_desc_home),
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Center Title
                    val personTitleAlpha = ((currentEffectiveProgress - 0.85f) / 0.15f).coerceIn(0f, 1f)
                    if (personTitleAlpha > 0.01f) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 64.dp)
                                .graphicsLayer { 
                                    alpha = personTitleAlpha
                                    scaleX = 0.9f + (0.1f * personTitleAlpha)
                                    scaleY = 0.9f + (0.1f * personTitleAlpha)
                                    translationY = 10f * (1f - personTitleAlpha)
                                }
                        )
                    }

                    // Right: Share
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Share
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isShareButtonPressed = true
                                            try { awaitRelease() } finally { isShareButtonPressed = false }
                                        },
                                        onTap = {
                                            onShareClick()
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentEffectiveProgress <= 0.01f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .hazeGlass(
                                            state = localHazeState,
                                            shape = CircleShape,
                                            blurRadius = HazeStyles.SmallGlassBlurRadius,
                                            useOffscreenStrategy = true
                                        )
                                )
                            }
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_share),
                                contentDescription = stringResource(R.string.detail_content_desc_share),
                                tint = Color.White,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer {
                                        scaleX = shareIconScale
                                        scaleY = shareIconScale
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}
