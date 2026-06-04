package com.cinetrack.ui.components.shared

import com.cinetrack.R

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
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 16.dp)
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
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = "CARTELLE",
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
                    .height(260.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
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
                                    text = "Nessuna cartella creata",
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

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(accentColor)
                    .bounceClick(scaleDown = 0.92f, onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "FATTO",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = Color.Black
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) Color.White.copy(alpha = 0.08f)
                else Color.White.copy(alpha = 0.03f)
            )
            .border(
                1.5.dp,
                if (isSelected) folderColor else Color.White.copy(alpha = 0.06f),
                RoundedCornerShape(16.dp)
            )
            .bounceClick(scaleDown = 0.97f, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(folderColor)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = folder.name,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            modifier = Modifier.weight(1f),
            maxLines = 1
        )

        if (isSelected) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_tick),
                contentDescription = null,
                tint = folderColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
