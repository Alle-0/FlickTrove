package com.cinetrack.ui.components.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cafe.adriel.voyager.navigator.tab.Tab
import com.cinetrack.R
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
    onSearchClick: (Offset) -> Unit
) {
    if (isPrimaryMainTab(currentTab) && currentTab !is SettingsTab) {
        val fabCenter = remember { arrayOf<Offset?>(null) }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 90.dp)
                .zIndex(60f)
                .size(52.dp)
                .onGloballyPositioned { coords ->
                    val pos = coords.positionInWindow()
                    val size = coords.size
                    fabCenter[0] = Offset(
                        pos.x + size.width / 2f,
                        pos.y + size.height / 2f
                    )
                }
                .clip(CircleShape)
                .hazeGlass(
                    state = contentHazeState,
                    shape = CircleShape
                )
                .border(
                    1.dp,
                    HazeStyles.GlassBorderColor.copy(alpha = HazeStyles.GlassBorderAlphaTop),
                    CircleShape
                )
                .bounceClick(scaleDown = 0.9f) {
                    val center = fabCenter[0]
                    val startX = center?.x ?: 540f
                    val startY = center?.y ?: 1140f
                    onSearchClick(Offset(startX, startY))
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_lente),
                contentDescription = stringResource(R.string.main_cd_search),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun BoxScope.MainScrollToTopFab(
    visible: Boolean,
    contentHazeState: HazeState,
    onScrollToTop: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val arrowOffsetY = remember { Animatable(0f) }
    val arrowAlpha = remember { Animatable(1f) }

    LaunchedEffect(visible) {
        if (visible) {
            arrowOffsetY.snapTo(0f)
            arrowAlpha.snapTo(1f)
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth * 2 },
            animationSpec = tween(280)
        ) + fadeIn(animationSpec = tween(200)),
        exit = slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth * 2 },
            animationSpec = tween(220)
        ) + fadeOut(animationSpec = tween(200)),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 29.dp, bottom = 152.dp)
            .zIndex(60f)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .hazeGlass(
                    state = contentHazeState,
                    shape = CircleShape
                )
                .border(
                    1.dp,
                    HazeStyles.GlassBorderColor.copy(alpha = HazeStyles.GlassBorderAlphaTop),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                contentDescription = stringResource(R.string.main_cd_scroll_to_top),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .bounceClick(scaleDown = 0.85f) {
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
                    .offset(y = arrowOffsetY.value.dp)
                    .alpha(arrowAlpha.value)
                    .rotate(-90f)
            )
        }
    }
}


