package com.cinetrack.ui.components.comments

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cinetrack.R
import com.cinetrack.data.model.AppComment
import com.cinetrack.util.DateTimeUtils
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.CommentsViewModel

/**
 * Bottom action bar of a comment card containing reply, like (liquid star),
 * report, smart translation and relative/formatted timestamp.
 */
@Composable
fun CommentCardActionsRow(
    comment: AppComment,
    isLiked: Boolean,
    isOwner: Boolean,
    isEffectivelyDeleted: Boolean,
    isUserAnonymous: Boolean,
    accentColor: Color,
    translationState: CommentsViewModel.TranslationState?,
    onTriggerGuestAuth: () -> Unit,
    onReply: () -> Unit,
    onToggleLike: () -> Unit,
    onReport: () -> Unit,
    onTranslate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        if (!isEffectivelyDeleted) {
            Text(
                text = stringResource(R.string.comment_reply_btn),
                color = accentColor,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .bounceClick {
                        if (isUserAnonymous) onTriggerGuestAuth()
                        else onReply()
                    }
                    .padding(vertical = 6.dp, horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Row(
                modifier = Modifier
                    .bounceClick(scaleDown = 0.90f) {
                        if (isUserAnonymous) onTriggerGuestAuth()
                        else onToggleLike()
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidStarIcon(
                    isLiked = isLiked,
                    accentColor = accentColor,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "${comment.likesCount}",
                    color = if (isLiked) accentColor else Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            if (!isOwner) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .bounceClick {
                            if (isUserAnonymous) onTriggerGuestAuth()
                            else onReport()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_flag),
                        contentDescription = stringResource(R.string.comment_report_title),
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Translate button with progressive filling download bar
            TranslationProgressIndicator(
                translationState = translationState,
                accentColor = accentColor,
                onTranslate = { onTranslate(comment.text) }
            )
            Spacer(modifier = Modifier.width(16.dp))
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        if (comment.createdAt != null) {
            val timeStr = remember(comment.createdAt, context) {
                val date = comment.createdAt.toDate()
                val diff = System.currentTimeMillis() - date.time
                val hours = diff / (1000 * 60 * 60)
                val minutes = diff / (1000 * 60)
                when {
                    minutes < 1 -> context.getString(R.string.comment_time_now)
                    minutes < 60 -> context.getString(R.string.comment_time_mins_ago, minutes.toInt())
                    hours < 24 -> context.getString(R.string.comment_time_hours_ago, hours.toInt())
                    else -> DateTimeUtils.formatShortDate(date)
                }
            }
            Text(
                text = timeStr,
                color = Color.White.copy(alpha = 0.3f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
