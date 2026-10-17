package com.cinetrack.ui.components.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.components.glass.hazeGlass
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay

/**
 * Interactive pill badge for collection / saga progress.
 * Displays the item count (e.g. "70 ITEMS", "4 MOVIES") with an animated [PillProgressBorder].
 * When tapped, triggers haptic feedback and smoothly morphs the text inside to show the watched ratio with percentage (e.g. "47/70 visti (67%)").
 * Tapping again or waiting 4 seconds reverts the text back to the item label.
 */
@Composable
fun CollectionProgressPill(
    watchedCount: Int,
    totalCount: Int,
    badgeText: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    containerColor: Color = Color(0xFF0F0F1A).copy(alpha = 0.82f),
    strokeWidth: Dp = 1.5.dp,
    fontSize: TextUnit = 11.sp,
    horizontalPadding: Dp = 12.dp,
    verticalPadding: Dp = 6.dp
) {
    val haptic = LocalHapticFeedback.current
    var showRatio by remember { mutableStateOf(false) }

    val progress = if (totalCount > 0) (watchedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "pillProgress"
    )

    // Auto-revert back to the label after 4 seconds
    LaunchedEffect(showRatio) {
        if (showRatio) {
            delay(4000)
            showRatio = false
        }
    }

    val shape = CircleShape
    val pillModifier = if (hazeState != null) {
        Modifier
            .clip(shape)
            .hazeGlass(
                state = hazeState,
                shape = shape,
                containerColor = containerColor,
                borderColor = Color.Transparent,
                borderWidth = 0.dp
            )
    } else {
        Modifier
            .clip(shape)
            .background(color = containerColor, shape = shape)
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.35f), shape = shape)
    }

    val percentage = if (totalCount > 0) ((watchedCount.toFloat() / totalCount.toFloat()) * 100f + 0.5f).toInt().coerceIn(0, 100) else 0
    val isAllWatched = totalCount > 0 && watchedCount >= totalCount

    val progressText = if (isAllWatched) {
        stringResource(R.string.collection_progress_popup_completed, watchedCount, totalCount)
    } else {
        stringResource(R.string.collection_progress_popup, watchedCount, totalCount, percentage)
    }

    Box(
        modifier = modifier
            .then(pillModifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showRatio = !showRatio
            },
        contentAlignment = Alignment.Center
    ) {
        PillProgressBorder(
            progress = animatedProgress,
            color = accentColor,
            strokeWidth = strokeWidth,
            modifier = Modifier.matchParentSize()
        )

        AnimatedContent(
            targetState = showRatio,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
            },
            label = "pillTextContent"
        ) { isExpanded ->
            val displayText = if (isExpanded && totalCount > 0) {
                progressText
            } else {
                badgeText
            }

            Text(
                text = displayText,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (isExpanded) (fontSize.value - 0.5f).sp else fontSize,
                letterSpacing = 0.4.sp,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding)
            )
        }
    }
}
