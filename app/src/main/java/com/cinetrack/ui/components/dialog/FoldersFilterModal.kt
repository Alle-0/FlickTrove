package com.cinetrack.ui.components.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.cinetrack.R
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.data.model.CommentSortOrder
import com.cinetrack.ui.screens.FolderSortOption
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FoldersFilterModal(
    isVisible: Boolean,
    hazeState: HazeState?,
    currentSortOption: FolderSortOption,
    currentSortOrder: CommentSortOrder,
    onSortChange: (FolderSortOption, CommentSortOrder) -> Unit,
    onDismissRequest: () -> Unit
) {
    var localSortOption by remember(isVisible) { mutableStateOf(currentSortOption) }
    var localSortOrder by remember(isVisible) { mutableStateOf(currentSortOrder) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            localSortOption = currentSortOption
            localSortOrder = currentSortOrder
        }
    }

    com.cinetrack.ui.components.shared.FlickTroveModal(
        isVisible = isVisible,
        onDismissRequest = onDismissRequest,
        hazeState = hazeState
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.filter_title),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_x),
                contentDescription = "Close",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(20.dp)
                    .bounceClick { onDismissRequest() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sort By Section
        ExpandableSection(
            title = stringResource(id = R.string.filter_sort_by),
            isExpanded = true,
            showChevron = false,
            isClickable = false,
            onToggle = { }
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val sortOptions = listOf(
                    FolderSortOption.DATE to stringResource(id = R.string.comment_sort_date),
                    FolderSortOption.NAME to stringResource(id = R.string.folder_sort_name),
                    FolderSortOption.ITEMS to stringResource(id = R.string.folder_sort_items_count)
                )

                sortOptions.forEach { (option, label) ->
                    SortOptionItem(
                        label = label,
                        isSelected = localSortOption == option,
                        onClick = { localSortOption = option }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DirectionChip(
                        label = stringResource(id = R.string.filter_dir_desc),
                        isSelected = localSortOrder == CommentSortOrder.DESC,
                        icon = ImageVector.vectorResource(id = R.drawable.ic_right),
                        iconRotation = 90f,
                        modifier = Modifier.weight(1f),
                        onClick = { localSortOrder = CommentSortOrder.DESC }
                    )
                    DirectionChip(
                        label = stringResource(id = R.string.filter_dir_asc),
                        isSelected = localSortOrder == CommentSortOrder.ASC,
                        icon = ImageVector.vectorResource(id = R.drawable.ic_right),
                        iconRotation = -90f,
                        modifier = Modifier.weight(1f),
                        onClick = { localSortOrder = CommentSortOrder.ASC }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Apply Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .bounceClick {
                    onSortChange(localSortOption, localSortOrder)
                    onDismissRequest()
                }
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.filter_apply),
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
