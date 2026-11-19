package com.cinetrack.ui.components.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.cinetrack.R
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.theme.PrimaryTeal
import com.cinetrack.ui.utils.UiText
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.AuthState
import kotlinx.coroutines.delay

/**
 * Modal progress overlay during authentication, initial account creation,
 * or cloud library delta sync. Includes timeout fallback to continue anyway.
 */
@Composable
fun AuthLoadingOverlay(
    loadingState: AuthState.Loading?,
    onContinueAnyway: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetProgress = loadingState?.progress
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress ?: 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "AuthSyncProgress"
    )

    val isSyncingLibrary = when ((loadingState?.message as? UiText.StringResource)?.resId) {
        R.string.sync_msg_fetching_favorites,
        R.string.sync_msg_syncing_favorites,
        R.string.sync_msg_saving_favorites,
        R.string.sync_msg_fetching_folders,
        R.string.sync_msg_syncing_folders,
        R.string.sync_msg_saving_folders,
        R.string.sync_msg_syncing_preferences,
        R.string.sync_msg_completed -> true
        else -> false
    }

    var showDismissButton by remember { mutableStateOf(false) }
    LaunchedEffect(loadingState) {
        if (loadingState != null) {
            val delayMillis = if (isSyncingLibrary) 15_000L else 6_000L
            delay(delayMillis)
            showDismissButton = true
        } else {
            showDismissButton = false
        }
    }

    AnimatedVisibility(
        visible = loadingState != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.zIndex(30f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth(0.9f)
                    .background(
                        color = HazeStyles.GlassColor,
                        shape = RoundedCornerShape(28.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = HazeStyles.GlassBorderColor.copy(alpha = HazeStyles.GlassBorderAlphaTop),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(horizontal = 22.dp, vertical = 18.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = loadingState?.message?.asString() ?: stringResource(R.string.auth_syncing),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (targetProgress == null) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = PrimaryTeal,
                            trackColor = Color.White.copy(alpha = 0.12f)
                        )
                    } else {
                        val clamped = animatedProgress.coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { clamped },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = PrimaryTeal,
                            trackColor = Color.White.copy(alpha = 0.12f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "${(clamped * 100).toInt()}%",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    AnimatedVisibility(
                        visible = showDismissButton,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .bounceClick {
                                    onContinueAnyway()
                                }
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                .padding(horizontal = 18.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.auth_sync_continue_anyway),
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
