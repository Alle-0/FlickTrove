package com.cinetrack.ui.components.shared

import androidx.compose.ui.res.stringResource
import com.cinetrack.R
import com.cinetrack.ui.utils.ColorUtils

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.ui.theme.OnSurfaceMuted
import com.cinetrack.ui.theme.PrimaryTeal
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.verticalFadingEdges
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.util.toComposeColor

@Composable
fun QuickFolderModal(
    movieTitle: String,
    folders: List<FolderEntity>,
    isItemInFolder: (String) -> Boolean,
    onToggleItem: (FolderEntity) -> Unit,
    onClose: () -> Unit,
    accentColor: Color = Color(0xFF6366F1),
    hazeState: dev.chrisbanes.haze.HazeState? = null
) {
    FlickTroveModal(
        onDismissRequest = onClose,
        hazeState = hazeState
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_cartella),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Column(modifier = Modifier.padding(start = 14.dp)) {
                    Text(
                        text = movieTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color.White,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.quick_folders).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.4f),
                            letterSpacing = 1.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val listState = rememberLazyListState()
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 320.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 10.dp)
                        .verticalFadingEdges(listState, 16.dp, 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    if (folders.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.quick_no_folders),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White.copy(alpha = 0.3f)
                                )
                            }
                        }
                    } else {
                        items(folders, key = { it.id }, contentType = { "quick_folder" }) { folder ->
                            val inFolder = isItemInFolder(folder.id)
                            val folderColor = folder.color.toComposeColor(accentColor)

                            QuickFolderItem(
                                folder = folder,
                                isSelected = inFolder,
                                folderColor = folderColor,
                                onClick = { onToggleItem(folder) }
                            )
                        }
                    }
                }

                // Premium Scrollbar logic
                val totalItems = folders.size
                val layoutInfo = listState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo.size
                
                val isScrollable = remember(totalItems, visibleItems) {
                    totalItems > visibleItems && visibleItems > 0
                }
                
                if (isScrollable) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(vertical = 4.dp)
                            .width(4.dp)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        val thumbHeightPercent = remember(totalItems, visibleItems) {
                            (visibleItems.toFloat() / totalItems).coerceIn(0.1f, 0.9f)
                        }
                        
                        val thumbOffsetPercent by remember {
                            derivedStateOf {
                                val firstVisible = listState.firstVisibleItemIndex
                                val firstVisibleOffset = listState.firstVisibleItemScrollOffset.toFloat()
                                val itemSize = layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1
                                
                                val currentScroll = firstVisible + (firstVisibleOffset / itemSize)
                                val maxScroll = (totalItems - visibleItems).coerceAtLeast(1)
                                (currentScroll / maxScroll).coerceIn(0f, 1f)
                            }
                        }
                        
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val trackHeight = maxHeight
                            val thumbHeight = trackHeight * thumbHeightPercent
                            val thumbOffset = (trackHeight - thumbHeight) * thumbOffsetPercent
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(thumbHeight)
                                    .offset(y = thumbOffset)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.7f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .bounceClick(scaleDown = 0.92f, onClick = onClose)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.action_done).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = ColorUtils.contentColorForAccent(accentColor)
                )
            }
        }
    }
}

@Composable
private fun QuickFolderItem(
    folder: FolderEntity,
    isSelected: Boolean,
    folderColor: Color,
    onClick: () -> Unit
) {
    val currentUserId = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid }
    val isOwner = currentUserId != null && folder.ownerUid == currentUserId
    val sharedBadgeText = if (isOwner || folder.ownerName.isNullOrBlank()) {
        stringResource(id = R.string.folder_shared_badge)
    } else {
        folder.ownerName
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.97f, onClick = onClick)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isSelected) Color.White.copy(alpha = 0.08f)
                else Color.White.copy(alpha = 0.03f)
            )
            .border(
                1.5.dp,
                if (isSelected) folderColor else Color.White.copy(alpha = 0.06f),
                RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Folder Icon Badge
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(folderColor.copy(alpha = 0.16f))
                .border(0.5.dp, folderColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_cartella),
                contentDescription = null,
                tint = folderColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Center Content: Title, Shared Badge, Item Count, Description
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (folder.isShared) {
                    Spacer(Modifier.width(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(folderColor.copy(alpha = 0.15f))
                            .border(0.5.dp, folderColor.copy(alpha = 0.3f), CircleShape)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_people),
                            contentDescription = null,
                            tint = folderColor,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = sharedBadgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = folderColor,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(3.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.folder_item_count, folder.itemIds.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.45f)
                )

                if (!folder.description.isNullOrBlank()) {
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.25f)
                    )
                    Text(
                        text = folder.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right Selection Indicator
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isSelected,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.8f),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(targetScale = 0.8f)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(folderColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_tick),
                        contentDescription = null,
                        tint = folderColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            if (!isSelected) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                )
            }
        }
    }
}
