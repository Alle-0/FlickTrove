package com.cinetrack.ui.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.LocalModalHost
import com.cinetrack.ui.components.shared.FlickTroveDatePickerModal
import com.cinetrack.ui.components.shared.FlickTroveModal
import com.cinetrack.ui.components.shared.calendar.MonthYearPickerDialog
import com.cinetrack.ui.history.model.WatchTimelineEntry
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState
import java.time.LocalDate
import java.time.YearMonth

/**
 * Gestore centralizzato dei modali della schermata History.
 * Incapsula l'interazione con [LocalModalHost] per il MonthYearPicker,
 * il DatePicker di modifica data e il modale di conferma cancellazione sessione.
 */
@Composable
fun HistoryModalsHost(
    showMonthPicker: Boolean,
    onDismissMonthPicker: () -> Unit,
    onMonthSelected: (YearMonth) -> Unit,
    selectedCalendarMonth: YearMonth,
    editingEntry: WatchTimelineEntry?,
    showDatePicker: Boolean,
    onDismissDatePicker: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    entryToDelete: WatchTimelineEntry?,
    onDismissDeleteSession: () -> Unit,
    onDeleteSessionEpisodes: (WatchTimelineEntry) -> Unit,
    onDeleteWatchSession: (Long) -> Unit,
    hazeState: HazeState
) {
    val modalHost = LocalModalHost.current

    // Month/Year Picker via Global ModalHost
    LaunchedEffect(showMonthPicker) {
        if (showMonthPicker) {
            modalHost.show {
                MonthYearPickerDialog(
                    showMonthPicker = true,
                    initialMonth = selectedCalendarMonth,
                    onMonthSelected = { month ->
                        onMonthSelected(month)
                        modalHost.dismiss()
                    },
                    onDismiss = {
                        onDismissMonthPicker()
                        modalHost.dismiss()
                    },
                    internalHazeState = hazeState,
                    accentColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    // Edit Watch Date Modal via Global ModalHost
    LaunchedEffect(showDatePicker, editingEntry) {
        val entry = editingEntry
        if (showDatePicker && entry != null) {
            modalHost.show {
                FlickTroveDatePickerModal(
                    initialDate = entry.parsedDate,
                    isVisible = true,
                    onDateSelected = { newDate ->
                        onDateSelected(newDate)
                        modalHost.dismiss()
                    },
                    onDismissRequest = {
                        onDismissDatePicker()
                        modalHost.dismiss()
                    },
                    accentColor = MaterialTheme.colorScheme.primary,
                    hazeState = hazeState
                )
            }
        }
    }

    // Confirm Delete Watch Session Modal via Global ModalHost
    LaunchedEffect(entryToDelete) {
        val target = entryToDelete
        if (target != null) {
            modalHost.show {
                HistoryDeleteSessionModal(
                    target = target,
                    onDismiss = {
                        onDismissDeleteSession()
                        modalHost.dismiss()
                    },
                    onDeleteSessionOnly = {
                        onDeleteSessionEpisodes(it)
                        modalHost.dismiss()
                    },
                    onDeleteEntireRewatch = {
                        onDeleteWatchSession(it)
                        modalHost.dismiss()
                    },
                    hazeState = hazeState
                )
            }
        }
    }
}

/**
 * Modale di conferma per l'eliminazione di una sessione di visione.
 * Supporta la rimozione della singola sessione per serie TV multi-sessione
 * oppure l'eliminazione dell'intero rewatch archiviato.
 */
@Composable
fun HistoryDeleteSessionModal(
    target: WatchTimelineEntry,
    onDismiss: () -> Unit,
    onDeleteSessionOnly: (WatchTimelineEntry) -> Unit,
    onDeleteEntireRewatch: (Long) -> Unit,
    hazeState: HazeState
) {
    FlickTroveModal(
        isVisible = true,
        onDismissRequest = onDismiss,
        hazeState = hazeState
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_trash),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }

            val isMultiSessionTv = target.isTvShow && target.totalSessionsInRewatch > 1

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(id = R.string.history_delete_session_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isMultiSessionTv) {
                        stringResource(
                            id = R.string.history_delete_session_question,
                            target.episodeInfo.orEmpty()
                        )
                    } else {
                        stringResource(id = R.string.history_delete_session_confirm)
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.65f),
                        lineHeight = 19.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }

            if (isMultiSessionTv) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Button 1: Remove Session Only
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick(scaleDown = 0.96f) {
                                onDeleteSessionOnly(target)
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.history_delete_session_only),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    // Button 2: Delete Entire Rewatch
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick(scaleDown = 0.96f) {
                                onDeleteEntireRewatch(target.historyId)
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.history_delete_entire_rewatch),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    // Button 3: Cancel
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick(scaleDown = 0.96f) {
                                onDismiss()
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.settings_cancel),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .weight(1f)
                            .bounceClick(scaleDown = 0.94f) {
                                onDismiss()
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.settings_cancel),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .weight(1f)
                            .bounceClick(scaleDown = 0.94f) {
                                onDeleteEntireRewatch(target.historyId)
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.quick_action_delete),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
