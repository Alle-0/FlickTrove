package com.cinetrack.ui.components.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.cinetrack.R
import com.cinetrack.ui.components.common.CategoryTabSelector
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState

@Composable
fun HomeFloatingHeader(
    activeTab: String,
    topPadding: Dp,
    stickyHeaderHeight: Dp,
    activeHazeState: HazeState?,
    onTabChanged: (String) -> Unit,
    onSurpriseMeClick: ((Rect?) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topPadding)
            .zIndex(1f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(stickyHeaderHeight)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            val rightControlsInset = 44.dp

            // Category Tab Selector Island
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(end = rightControlsInset),
                contentAlignment = Alignment.Center
            ) {
                Spacer(
                    modifier = Modifier
                        .matchParentSize()
                        .hazeGlass(
                            state = activeHazeState,
                            shape = CircleShape,
                            blurRadius = HazeStyles.SmallGlassBlurRadius,
                            useOffscreenStrategy = false
                        )
                )

                val options = listOf(
                    stringResource(R.string.home_tab_movies),
                    stringResource(R.string.home_tab_tv_series)
                )
                val selectedIndex = if (activeTab == "movie") 0 else 1

                CategoryTabSelector(
                    options = options,
                    selectedIndex = selectedIndex,
                    onOptionClick = { index ->
                        onTabChanged(if (index == 0) "movie" else "tv")
                    }
                )
            }

            // Circular Surprise Me Button (Right Aligned)
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val wandCoords = remember { arrayOf<LayoutCoordinates?>(null) }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .onGloballyPositioned { wandCoords[0] = it }
                        .bounceClick(scaleDown = 0.90f) {
                            val rect = wandCoords[0]?.let {
                                val pos = it.positionInWindow()
                                Rect(
                                    pos.x,
                                    pos.y,
                                    pos.x + it.size.width,
                                    pos.y + it.size.height
                                )
                            }
                            onSurpriseMeClick?.invoke(rect)
                        }
                        .hazeGlass(
                            state = activeHazeState,
                            shape = CircleShape,
                            blurRadius = HazeStyles.SmallGlassBlurRadius,
                            useOffscreenStrategy = false,
                            borderWidth = 1.dp,
                            borderColor = HazeStyles.GlassBorderColor.copy(alpha = HazeStyles.GlassBorderAlphaTop)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_bacchetta),
                        contentDescription = "Surprise Me",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
