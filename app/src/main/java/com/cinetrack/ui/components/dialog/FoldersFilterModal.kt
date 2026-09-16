package com.cinetrack.ui.components.dialog

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.model.CommentSortOrder
import com.cinetrack.ui.components.shared.MorphGlassModal
import com.cinetrack.ui.screens.FolderSortOption
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FoldersFilterModal(
    isVisible: Boolean,
    triggerBounds: Rect? = null,
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

    MorphGlassModal(
        isVisible = isVisible,
        onDismissRequest = onDismissRequest,
        triggerBounds = triggerBounds,
        hazeState = hazeState,
        targetMaxWidth = 420.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 20.dp, top = 20.dp, bottom = 20.dp),
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
                        modifier = Modifier.fillMaxWidth(),
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
                    .padding(horizontal = 16.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .bounceClick {
                        onSortChange(localSortOption, localSortOrder)
                        onDismissRequest()
                    },
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
}
