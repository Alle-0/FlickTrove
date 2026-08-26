package com.cinetrack.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.cinetrack.ui.utils.bounceClick
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cinetrack.R
import com.cinetrack.data.local.entities.WatchHistoryEntity
import com.cinetrack.ui.theme.PremiumBackground
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchHistoryBottomSheet(
    movie: com.cinetrack.data.model.Movie,
    history: List<WatchHistoryEntity>,
    accentColor: Color,
    onDismiss: () -> Unit,
    onAddRewatch: () -> Unit,
    onUpdateDate: (WatchHistoryEntity, String) -> Unit,
    onDelete: (WatchHistoryEntity) -> Unit,
    hazeState: HazeState? = null
) {
    var editingHistory by remember { mutableStateOf<WatchHistoryEntity?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker && editingHistory != null) {
        val initialLocalDate = try {
            val zdt = Instant.parse(editingHistory!!.watchedAt).atZone(ZoneId.systemDefault())
            zdt.toLocalDate()
        } catch (e: Exception) {
            LocalDate.now()
        }

        com.cinetrack.ui.components.shared.FlickTroveDatePickerModal(
            initialDate = initialLocalDate,
            onDateSelected = { selectedDate ->
                val newIso = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toString()
                onUpdateDate(editingHistory!!, newIso)
                showDatePicker = false
                editingHistory = null
            },
            onDismissRequest = {
                showDatePicker = false
                editingHistory = null
            },
            hazeState = hazeState,
            accentColor = accentColor
        )
    } else {
        com.cinetrack.ui.components.shared.FlickTroveModal(
            onDismissRequest = onDismiss,
            hazeState = hazeState
        ) {
            Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.watch_history_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    
                    val rewatchCount = if (history.size > 1) history.size - 1 else 0
                    if (rewatchCount > 0) {
                        Text(
                            text = "x$rewatchCount ${stringResource(id = R.string.watch_history_rewatch).lowercase()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .bounceClick(scaleDown = 0.9f, onClick = onAddRewatch)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_plus),
                            contentDescription = "Add Rewatch",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .bounceClick(scaleDown = 0.9f, onClick = onDismiss)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_x),
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (history.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.watch_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 32.dp)
                )
            } else {
            val sortedHistory = history.sortedBy { it.watchedAt }
            val firstViewing = sortedHistory.firstOrNull()
            val rewatches = if (sortedHistory.size > 1) sortedHistory.drop(1).sortedByDescending { it.watchedAt } else emptyList()

            if (firstViewing != null) {
                HistoryItemRow(
                    item = firstViewing,
                    accentColor = accentColor,
                    onEdit = {
                        editingHistory = it
                        showDatePicker = true
                    },
                    onDelete = null,
                    label = stringResource(id = R.string.watch_history_first_watch)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (rewatches.isNotEmpty()) {
                val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                
                val showTopGradient by remember {
                    derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
                }
                
                val showBottomGradient by remember {
                    derivedStateOf {
                        val layoutInfo = listState.layoutInfo
                        val visibleItemsInfo = layoutInfo.visibleItemsInfo
                        if (layoutInfo.totalItemsCount == 0 || visibleItemsInfo.isEmpty()) {
                            false
                        } else {
                            val lastVisibleItem = visibleItemsInfo.last()
                            lastVisibleItem.index < layoutInfo.totalItemsCount - 1 ||
                            lastVisibleItem.offset + lastVisibleItem.size > layoutInfo.viewportEndOffset
                        }
                    }
                }

                val fadeBrush = Brush.verticalGradient(
                    0f to if (showTopGradient) Color.Transparent else Color.Black,
                    0.05f to Color.Black,
                    0.95f to Color.Black,
                    1f to if (showBottomGradient) Color.Transparent else Color.Black
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .heightIn(max = 350.dp)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(brush = fadeBrush, blendMode = BlendMode.DstIn)
                        },
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    itemsIndexed(
                        items = rewatches,
                        key = { _, item -> item.id }
                    ) { index, item ->
                        val rewatchNumber = rewatches.size - index
                        HistoryItemRow(
                            item = item,
                            accentColor = accentColor,
                            onEdit = {
                                editingHistory = it
                                showDatePicker = true
                            },
                            onDelete = { onDelete(it) },
                            label = "${stringResource(id = R.string.watch_history_rewatch)} $rewatchNumber",
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
    }
    }
}

@Composable
private fun HistoryItemRow(
    item: WatchHistoryEntity,
    accentColor: Color,
    onEdit: (WatchHistoryEntity) -> Unit,
    onDelete: ((WatchHistoryEntity) -> Unit)?,
    label: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val dateStr = try {
                val zdt = Instant.parse(item.watchedAt).atZone(ZoneId.systemDefault())
                zdt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
            } catch (e: Exception) {
                item.watchedAt
            }

            Text(
                text = dateStr,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor.copy(alpha = 0.8f)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .bounceClick(scaleDown = 0.9f) { onEdit(item) }
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_pencil),
                    contentDescription = "Edit Date",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
            if (onDelete != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .bounceClick(scaleDown = 0.9f) { onDelete(item) }
                        .clip(CircleShape)
                        .background(Color(0xFFFF3D3D).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_trash),
                        contentDescription = "Delete",
                        tint = Color(0xFFFF3D3D),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun formatDateToLocalDisplay(isoDate: String): String {
    if (isoDate.isBlank()) return ""
    return try {
        val instant = java.time.Instant.parse(isoDate)
        val formatter = java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
            .withLocale(java.util.Locale.getDefault())
            .withZone(java.time.ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        try {
            val date = java.time.LocalDate.parse(isoDate)
            val formatter = java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
                .withLocale(java.util.Locale.getDefault())
            formatter.format(date)
        } catch (e2: Exception) {
            isoDate
        }
    }
}
