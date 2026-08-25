package com.cinetrack.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var editingHistory by remember { mutableStateOf<WatchHistoryEntity?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    com.cinetrack.ui.components.shared.FlickTroveBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        hazeState = hazeState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Watch History", // TODO: localize
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                IconButton(
                    onClick = onAddRewatch,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = accentColor.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add Rewatch",
                        tint = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (history.isEmpty()) {
                Text(
                    text = "No history available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 32.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(history.sortedByDescending { it.watchedAt }) { item ->
                        HistoryItemRow(
                            item = item,
                            accentColor = accentColor,
                            onEdit = {
                                editingHistory = it
                                showDatePicker = true
                            },
                            onDelete = { onDelete(it) }
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker && editingHistory != null) {
        val initialMillis = try {
            Instant.parse(editingHistory!!.watchedAt).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }

        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
                editingHistory = null
            },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // Convert back to ISO string
                        val newIso = Instant.ofEpochMilli(millis).toString()
                        onUpdateDate(editingHistory!!, newIso)
                    }
                    showDatePicker = false
                    editingHistory = null
                }) {
                    Text("Save", color = accentColor)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    editingHistory = null
                }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = PremiumBackground,
                titleContentColor = Color.White,
                headlineContentColor = Color.White,
                weekdayContentColor = Color.White.copy(alpha = 0.7f),
                subheadContentColor = Color.White.copy(alpha = 0.7f),
                yearContentColor = Color.White,
                currentYearContentColor = accentColor,
                selectedYearContentColor = Color.White,
                selectedYearContainerColor = accentColor,
                dayContentColor = Color.White,
                disabledDayContentColor = Color.White.copy(alpha = 0.3f),
                selectedDayContentColor = Color.White,
                selectedDayContainerColor = accentColor,
                todayContentColor = accentColor,
                todayDateBorderColor = accentColor
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    titleContentColor = Color.White,
                    headlineContentColor = Color.White,
                    weekdayContentColor = Color.White.copy(alpha = 0.7f),
                    subheadContentColor = Color.White.copy(alpha = 0.7f),
                    yearContentColor = Color.White,
                    currentYearContentColor = accentColor,
                    selectedYearContentColor = Color.White,
                    selectedYearContainerColor = accentColor,
                    dayContentColor = Color.White,
                    disabledDayContentColor = Color.White.copy(alpha = 0.3f),
                    selectedDayContentColor = Color.White,
                    selectedDayContainerColor = accentColor,
                    todayContentColor = accentColor,
                    todayDateBorderColor = accentColor
                )
            )
        }
    }
}

@Composable
private fun HistoryItemRow(
    item: WatchHistoryEntity,
    accentColor: Color,
    onEdit: (WatchHistoryEntity) -> Unit,
    onDelete: (WatchHistoryEntity) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(16.dp),
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
            if (item.isRewatch) {
                Text(
                    text = "Rewatch", // TODO: localize
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor.copy(alpha = 0.8f)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = { onEdit(item) },
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit Date",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = { onDelete(item) },
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFFF3D3D).copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFFF3D3D),
                    modifier = Modifier.size(20.dp)
                )
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
