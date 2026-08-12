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
import com.cinetrack.ui.screens.CommentSortOrder
import com.cinetrack.ui.screens.FolderSortOption
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

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
        Surface(
            color = Color.White.copy(alpha = 0.03f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.filter_sort_by),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
                )

                val sortOptions = listOf(
                    FolderSortOption.DATE to stringResource(id = R.string.comment_sort_date),
                    FolderSortOption.NAME to stringResource(id = R.string.folder_sort_name),
                    FolderSortOption.ITEMS to stringResource(id = R.string.folder_sort_items_count)
                )

                sortOptions.forEachIndexed { index, (option, label) ->
                    val isSelected = localSortOption == option
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick(scaleDown = 0.98f) {
                                localSortOption = option
                            }
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else Color.White.copy(alpha = 0.05f),
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                        
                        if (isSelected) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_tick),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.CenterEnd)
                            )
                        }
                    }
                    
                    if (index < sortOptions.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                
                // Direction Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .bounceClick { localSortOrder = CommentSortOrder.DESC }
                            .background(
                                if (localSortOrder == CommentSortOrder.DESC) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else Color.White.copy(alpha = 0.05f),
                                RoundedCornerShape(100)
                            )
                            .border(
                                1.dp,
                                if (localSortOrder == CommentSortOrder.DESC) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else Color.Transparent,
                                RoundedCornerShape(100)
                            )
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_right),
                                contentDescription = null,
                                tint = if (localSortOrder == CommentSortOrder.DESC) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp).rotate(90f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.filter_dir_desc),
                                color = if (localSortOrder == CommentSortOrder.DESC) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .bounceClick { localSortOrder = CommentSortOrder.ASC }
                            .background(
                                if (localSortOrder == CommentSortOrder.ASC) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else Color.White.copy(alpha = 0.05f),
                                RoundedCornerShape(100)
                            )
                            .border(
                                1.dp,
                                if (localSortOrder == CommentSortOrder.ASC) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else Color.Transparent,
                                RoundedCornerShape(100)
                            )
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_left),
                                contentDescription = null,
                                tint = if (localSortOrder == CommentSortOrder.ASC) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp).rotate(90f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.filter_dir_asc),
                                color = if (localSortOrder == CommentSortOrder.ASC) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
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
