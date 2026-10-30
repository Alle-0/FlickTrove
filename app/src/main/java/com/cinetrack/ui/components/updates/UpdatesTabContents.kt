package com.cinetrack.ui.components.updates

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cinetrack.R
import com.cinetrack.data.model.GroupedSocialNotification
import com.cinetrack.data.model.Movie
import com.cinetrack.data.repository.TrophyUpdateNotification

@Composable
fun NotificationsTabContent(
    listState: LazyListState,
    paddingValues: PaddingValues,
    futureRemindersCount: Int,
    isLoading: Boolean,
    trophyNotifications: List<TrophyUpdateNotification>,
    newEpisodes: List<Movie>,
    releasedRecently: List<Movie>,
    onNavigateToReminders: () -> Unit,
    onClearAllTrophies: () -> Unit,
    onClearTrophy: (TrophyUpdateNotification) -> Unit,
    onNavigateToTrophyRoom: ((String?) -> Unit)?,
    onClearAllNewEpisodes: () -> Unit,
    onClearAllMigrated: () -> Unit,
    onClearUpdate: (Long, String) -> Unit,
    onClearMigrated: (Long, String) -> Unit,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                bottom = paddingValues.calculateBottomPadding() + 80.dp,
                top = 175.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Promemoria Summary Card
            item {
                RemindersSummaryCard(
                    count = futureRemindersCount,
                    onClick = onNavigateToReminders
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else {
                val hasUpdates = newEpisodes.isNotEmpty() || releasedRecently.isNotEmpty() || trophyNotifications.isNotEmpty()

                if (trophyNotifications.isNotEmpty()) {
                    item {
                        SectionHeader(
                            text = stringResource(R.string.dashboard_trophies),
                            action = if (trophyNotifications.size > 1) {
                                { MarkAllButton(onClick = onClearAllTrophies) }
                            } else null
                        )
                    }
                    items(trophyNotifications, key = { it.key }, contentType = { "trophy" }) { notif ->
                        Box(modifier = Modifier.animateItem()) {
                            TrophyNotificationCard(
                                notification = notif,
                                onDismiss = { onClearTrophy(notif) },
                                onClick = {
                                    val targetBadgeId = (notif as? TrophyUpdateNotification.SingleTrophy)?.badgeId
                                    onNavigateToTrophyRoom?.invoke(targetBadgeId)
                                }
                            )
                        }
                    }
                }

                if (newEpisodes.isNotEmpty()) {
                    item {
                        SectionHeader(
                            text = stringResource(R.string.updates_new_episodes),
                            action = { MarkAllButton(onClick = onClearAllNewEpisodes) }
                        )
                    }
                    items(newEpisodes, key = { it.id.toString() + it.mediaType + "_new" }, contentType = { "movie" }) { movie ->
                        Box(modifier = Modifier.animateItem()) {
                            UpdateCard(
                                movie = movie,
                                label = stringResource(R.string.updates_new_episodes_count, movie.newEpisodesFound ?: 0),
                                iconRes = R.drawable.ic_tick_card,
                                color = MaterialTheme.colorScheme.primary,
                                onAction = { onClearUpdate(movie.id, movie.mediaType) },
                                onPress = { onMovieClick(movie) }
                            )
                        }
                    }
                }

                if (releasedRecently.isNotEmpty()) {
                    item {
                        SectionHeader(
                            text = stringResource(R.string.updates_out_today),
                            action = { MarkAllButton(onClick = onClearAllMigrated) }
                        )
                    }
                    items(releasedRecently, key = { it.id.toString() + it.mediaType + "_recent" }, contentType = { "movie" }) { movie ->
                        Box(modifier = Modifier.animateItem()) {
                            UpdateCard(
                                movie = movie,
                                label = stringResource(R.string.updates_available_now),
                                iconRes = R.drawable.ic_tick_card,
                                color = MaterialTheme.colorScheme.primary,
                                onAction = { onClearMigrated(movie.id, movie.mediaType) },
                                onPress = { onMovieClick(movie) }
                            )
                        }
                    }
                }

                if (!hasUpdates) {
                    item {
                        EmptyNotificationsState()
                    }
                }
            }
        }
    }
}

@Composable
fun SocialTabContent(
    listState: LazyListState,
    paddingValues: PaddingValues,
    socialNotifications: List<GroupedSocialNotification>,
    onMarkAllAsRead: () -> Unit,
    onNotificationClick: (Movie, String?, Int?, Int?) -> Unit,
    onMarkAsRead: (GroupedSocialNotification) -> Unit,
    modifier: Modifier = Modifier
) {
    var lastClickTime by remember { mutableLongStateOf(0L) }

    val unreadSocialNotifs = remember(socialNotifications) {
        socialNotifications.filter { !it.isRead }.sortedByDescending { it.latestTimestamp?.seconds ?: 0L }
    }
    val readSocialNotifs = remember(socialNotifications) {
        socialNotifications.filter { it.isRead }.sortedByDescending { it.latestTimestamp?.seconds ?: 0L }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 80.dp,
                top = 175.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (socialNotifications.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.updates_no_notifications),
                            color = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }
            } else {
                if (unreadSocialNotifs.isNotEmpty()) {
                    item {
                        SectionHeader(
                            text = stringResource(R.string.updates_social_recent),
                            action = { MarkAllButton(onClick = onMarkAllAsRead) }
                        )
                    }

                    items(unreadSocialNotifs, key = { it.id }, contentType = { "social_notif" }) { notif ->
                        Box(modifier = Modifier.animateItem()) {
                            SocialNotificationCard(
                                notification = notif,
                                onClick = {
                                    val currentTime = android.os.SystemClock.elapsedRealtime()
                                    if (currentTime - lastClickTime > 500L) {
                                        lastClickTime = currentTime
                                        onMarkAsRead(notif)
                                        val movie = Movie(
                                            id = notif.mediaId.toLongOrNull() ?: 0L,
                                            mediaType = notif.mediaType,
                                            title = notif.mediaTitle,
                                            posterPath = notif.mediaImage,
                                            backdropPath = null
                                        )
                                        onNotificationClick(movie, notif.commentId.ifBlank { null }, notif.seasonNumber, notif.episodeNumber)
                                    }
                                },
                                onMarkRead = {
                                    onMarkAsRead(notif)
                                }
                            )
                        }
                    }
                }

                if (readSocialNotifs.isNotEmpty()) {
                    item {
                        SectionHeader(
                            text = stringResource(R.string.updates_social_read)
                        )
                    }

                    items(readSocialNotifs, key = { it.id }, contentType = { "social_notif" }) { notif ->
                        Box(modifier = Modifier.animateItem()) {
                            SocialNotificationCard(
                                notification = notif,
                                onClick = {
                                    val currentTime = android.os.SystemClock.elapsedRealtime()
                                    if (currentTime - lastClickTime > 500L) {
                                        lastClickTime = currentTime
                                        val movie = Movie(
                                            id = notif.mediaId.toLongOrNull() ?: 0L,
                                            mediaType = notif.mediaType,
                                            title = notif.mediaTitle,
                                            posterPath = notif.mediaImage,
                                            backdropPath = null
                                        )
                                        onNotificationClick(movie, notif.commentId.ifBlank { null }, notif.seasonNumber, notif.episodeNumber)
                                    }
                                },
                                onMarkRead = {}
                            )
                        }
                    }
                }
            }
        }
    }
}
