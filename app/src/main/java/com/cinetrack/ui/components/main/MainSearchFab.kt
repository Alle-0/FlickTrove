package com.cinetrack.ui.components.main

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
