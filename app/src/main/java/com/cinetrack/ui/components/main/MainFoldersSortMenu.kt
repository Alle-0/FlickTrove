package com.cinetrack.ui.components.main

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.ui.input.pointer.pointerInput
import com.cinetrack.R
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.screens.FolderSortOption
import com.cinetrack.ui.screens.CommentSortOrder
import dev.chrisbanes.haze.HazeState

@Composable
fun MainFoldersSortMenu(
    visible: Boolean = true,
    offset: Offset,
    hazeState: HazeState,
    currentSortOption: FolderSortOption,
    currentSortOrder: CommentSortOrder,
    onDismiss: () -> Unit,
    onSortOptionSelect: (FolderSortOption) -> Unit,
    onSortOrderToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(2000f)
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
    ) {
        val density = LocalDensity.current
        val offsetX = with(density) { offset.x.toDp() }
        val offsetY = with(density) { offset.y.toDp() }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically(
                initialOffsetY = { -it / 4 },
                animationSpec = tween(250, easing = androidx.compose.animation.core.EaseOutCirc)
            ),
            exit = fadeOut() + slideOutVertically(
                targetOffsetY = { -it / 4 },
                animationSpec = tween(200, easing = androidx.compose.animation.core.EaseInCirc)
            ),
            modifier = Modifier.absoluteOffset(x = offsetX - 200.dp + 32.dp, y = offsetY + 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(200.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .then(if (visible) Modifier.hazeGlass(state = hazeState, shape = RoundedCornerShape(24.dp)) else Modifier)
            ) {
                SortOptionItem(
                    text = "Data",
                    isSelected = currentSortOption == FolderSortOption.DATE,
                    onClick = { onSortOptionSelect(FolderSortOption.DATE) }
                )
                SortOptionItem(
                    text = "Nome",
                    isSelected = currentSortOption == FolderSortOption.NAME,
                    onClick = { onSortOptionSelect(FolderSortOption.NAME) }
                )
                SortOptionItem(
                    text = "Numero elementi",
                    isSelected = currentSortOption == FolderSortOption.ITEMS,
                    onClick = { onSortOptionSelect(FolderSortOption.ITEMS) }
                )
                
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(Color.White.copy(alpha = 0.1f)))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick { onSortOrderToggle() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (currentSortOrder == CommentSortOrder.DESC) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (currentSortOrder == CommentSortOrder.DESC) "Decrescente" else "Crescente", 
                        color = Color.White, 
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }
        }
    }
}

@Composable
private fun SortOptionItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = text, color = Color.White, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
