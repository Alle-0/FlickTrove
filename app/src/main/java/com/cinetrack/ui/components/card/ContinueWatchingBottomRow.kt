package com.cinetrack.ui.components.card

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.LocalEpisodeInfoStyle
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.data.model.NextEpisodeInfo
import com.cinetrack.ui.utils.bounceClick

private val UpToDateGreen = Color(0xFF10B981)

@Composable
internal fun ContinueWatchingBottomRow(
    isReleased: Boolean,
    isUpToDate: Boolean,
    formattedNextDate: String?,
    epText: String,
    movie: Movie,
    nextInfo: NextEpisodeInfo,
    animatedSeriesProgress: Float,
    animatedCircularProgress: Float,
    isUpdating: Boolean,
    appAccent: Color,
    isReminder: Boolean,
    isFavorite: Boolean,
    actionButtonModifier: Modifier,
    onAction: (Movie) -> Unit,
    onMessage: (String) -> Unit,
    onQuickMarkWatched: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 36.dp)
            .padding(start = 11.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (!isReleased) {
            // UNRELEASED STATE: Serie non ancora uscita ("In arrivo")
            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = stringResource(R.string.lbl_coming_soon),
                    color = appAccent,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )
                if (!formattedNextDate.isNullOrBlank()) {
                    Text(
                        text = formattedNextDate,
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                }
            }

            // Bell reminder button on the right (matching 24dp size and alignment)
            val isReminderActive = isReminder || isFavorite
            val bellBg = if (isReminderActive) appAccent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f)
            val bellBorder = if (isReminderActive) appAccent.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.15f)
            val bellTint = if (isReminderActive) appAccent else Color.White.copy(alpha = 0.85f)

            val bellRotation = remember { Animatable(0f) }
            var bellHasInitialized by remember { mutableStateOf(false) }

            LaunchedEffect(isReminder, isFavorite) {
                if (bellHasInitialized && isReminderActive) {
                    bellRotation.animateTo(-25f, tween(60, easing = LinearEasing))
                    bellRotation.animateTo(20f, tween(100, easing = LinearEasing))
                    bellRotation.animateTo(-15f, tween(100, easing = LinearEasing))
                    bellRotation.animateTo(10f, tween(100, easing = LinearEasing))
                    bellRotation.animateTo(0f, tween(100, easing = FastOutSlowInEasing))
                }
                bellHasInitialized = true
            }

            val haptic = LocalHapticFeedback.current
            val hintReminder = stringResource(R.string.card_hint_manage_reminder)

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .then(actionButtonModifier)
                    .bounceClick(
                        scaleDown = 0.85f,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (!isReminderActive) {
                                onAction(movie)
                            } else {
                                onMessage(hintReminder)
                            }
                        }
                    )
                    .background(color = bellBg, shape = CircleShape)
                    .border(width = 1.dp, color = bellBorder, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isReminderActive) ImageVector.vectorResource(id = R.drawable.ic_bell_piena) else ImageVector.vectorResource(id = R.drawable.ic_bell),
                    contentDescription = stringResource(R.string.card_hint_manage_reminder),
                    tint = bellTint,
                    modifier = Modifier
                        .size(if (isReminderActive) 14.dp else 13.dp)
                        .graphicsLayer {
                            if (isReminderActive) {
                                rotationZ = bellRotation.value
                                transformOrigin = TransformOrigin(0.5f, 0.2f)
                            }
                        }
                )
            }
        } else if (isUpToDate) {
            // UP TO DATE STATE
            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = stringResource(R.string.lbl_up_to_date),
                    color = UpToDateGreen,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )
                if (!formattedNextDate.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.lbl_until_date, formattedNextDate),
                        color = UpToDateGreen.copy(alpha = 0.80f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                }
            }

            // Completed green check button with 100% progress ring on the border
            SeasonProgressCheckButton(
                progress = 1f,
                accentColor = UpToDateGreen,
                contentDescription = stringResource(R.string.lbl_up_to_date),
                modifier = actionButtonModifier,
                onClick = {
                    val msg = if (!formattedNextDate.isNullOrBlank()) {
                        context.getString(R.string.lbl_up_to_date) + " • " + context.getString(R.string.lbl_until_date, formattedNextDate)
                    } else {
                        context.getString(R.string.lbl_up_to_date)
                    }
                    onMessage(msg)
                }
            )
        } else {
            // IN PROGRESS STATE:
            // Line 1: S01 E04
            // Line 2: ancora 71 (lbl_episodes_remaining)
            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = epText,
                    color = appAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )
                val episodeInfoStyle = LocalEpisodeInfoStyle.current
                val epSubtitle = formatDrawerEpisodeSubtitle(
                    style = episodeInfoStyle,
                    nextInfo = nextInfo,
                    movie = movie,
                    seriesProgress = animatedSeriesProgress
                )
                if (epSubtitle.isNotEmpty()) {
                    Text(
                        text = epSubtitle,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                }
            }

            // 1-tap Checkmark button (✓) with Circular Progress directly on the button border
            SeasonProgressCheckButton(
                progress = animatedCircularProgress,
                showProgress = true,
                accentColor = appAccent,
                isUpdating = isUpdating,
                contentDescription = stringResource(R.string.cd_mark_episode_watched, epText),
                modifier = actionButtonModifier,
                onClick = { onQuickMarkWatched(movie) }
            )
        }
    }
}
