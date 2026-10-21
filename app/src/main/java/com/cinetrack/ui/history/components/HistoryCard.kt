package com.cinetrack.ui.history.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.ui.utils.parseMarkdown
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cinetrack.R
import com.cinetrack.ui.components.updates.PillBadge
import com.cinetrack.ui.history.model.WatchTimelineEntry
import com.cinetrack.ui.utils.bounceClick
import java.util.Locale

@Composable
fun HistoryCard(
    entry: WatchTimelineEntry,
    onClick: () -> Unit,
    onEditDate: () -> Unit,
    onDeleteSession: () -> Unit,
    modifier: Modifier = Modifier,
    isCalendarDayMode: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = if (isPressed) Spring.StiffnessHigh else Spring.StiffnessLow
        ),
        label = "historyCardScale"
    )

    val posterUrl = remember(entry.posterPath) {
        if (!entry.posterPath.isNullOrBlank()) {
            "https://image.tmdb.org/t/p/w342${entry.posterPath}"
        } else null
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF1C1C1E))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster thumbnail (matching UpdateCard 48.dp x 72.dp)
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF121216))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            ) {
                if (posterUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(posterUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = entry.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = entry.title.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.35f)
                            )
                        )
                    }
                }
            }

            // Info Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = entry.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(5.dp))

                // Badges Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Episode badge (Teal Cyan)
                    if (entry.episodeInfo != null) {
                        PillBadge(
                            text = entry.episodeInfo,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Timing / Date badge (Amber / Gold)
                    val shortDate = remember(entry.parsedDate) {
                        try {
                            entry.parsedDate.format(java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale.getDefault()))
                        } catch (e: Exception) {
                            entry.formattedDisplayDate.take(6)
                        }
                    }
                    val timingText = if (isCalendarDayMode) {
                        entry.formattedDisplayTime ?: shortDate
                    } else {
                        if (entry.formattedDisplayTime != null) {
                            "$shortDate · ${entry.formattedDisplayTime}"
                        } else {
                            shortDate
                        }
                    }
                    PillBadge(
                        text = timingText,
                        color = Color(0xFFD97706)
                    )

                    // Rewatch badge
                    if (entry.isRewatch) {
                        val rewatchText = if (entry.rewatchNumber > 1) {
                            stringResource(id = R.string.history_rewatch_badge, entry.rewatchNumber)
                        } else {
                            stringResource(id = R.string.history_filter_rewatches)
                        }
                        PillBadge(
                            text = rewatchText,
                            color = Color(0xFFF59E0B)
                        )
                    }

                    // Personal Rating badge (with official ic_star_piena)
                    if (entry.personalRating != null && entry.personalRating > 0.0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFFBBF24).copy(alpha = 0.14f))
                                .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.35f), RoundedCornerShape(50))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_star_piena),
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1f", entry.personalRating),
                                    color = Color(0xFFFBBF24),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Note Callout if present (with Markdown formatting & at least 2 lines)
                if (!entry.personalNote.isNullOrBlank()) {
                    val parsedNote = remember(entry.personalNote) {
                        val md = entry.personalNote.parseMarkdown()
                        buildAnnotatedString {
                            append("\"")
                            append(md)
                            append("\"")
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = parsedNote,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Options Context Menu
            Box {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .bounceClick(scaleDown = 0.85f) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showMenu = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Opzioni",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(17.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier
                        .background(Color(0xFF23232B))
                        .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(id = R.string.history_edit_date_title),
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            showMenu = false
                            onEditDate()
                        }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(id = R.string.history_delete_session_title),
                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.error)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDeleteSession()
                        }
                    )
                }
            }
        }
    }
}
