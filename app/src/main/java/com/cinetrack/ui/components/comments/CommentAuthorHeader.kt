package com.cinetrack.ui.components.comments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.cinetrack.R
import com.cinetrack.data.model.AppComment
import com.cinetrack.ui.utils.bounceClick
import java.util.Locale

/**
 * Top header of a comment card displaying the author's name, CommsUni/FlickTrove origin badge,
 * and author quick actions (spoiler toggle, delete comment within 12h window).
 */
@Composable
fun CommentAuthorHeader(
    comment: AppComment,
    isOwner: Boolean,
    isEffectivelyDeleted: Boolean,
    accentColor: Color,
    onToggleSpoilerAuthor: () -> Unit,
    onDeleteComment: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = if (isEffectivelyDeleted) stringResource(R.string.comment_deleted) else comment.userDisplayName.ifBlank { stringResource(R.string.comment_anonymous_user) },
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        if (comment.originSlug.isNotBlank()) {
            Spacer(modifier = Modifier.width(8.dp))

            val parsedColor = comment.originColor?.let {
                try {
                    Color(android.graphics.Color.parseColor(it))
                } catch (e: Exception) {
                    null
                }
            }
            // If no API color, derive a deterministic vivid hue from the slug
            val originColor = parsedColor ?: run {
                val hue = (comment.originSlug.fold(0) { acc, c -> acc * 31 + c.code }
                    .and(0x7FFFFFFF) % 360).toFloat()
                Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.65f, 0.9f)))
            }

            val originIconUrl = comment.originIcon?.takeIf { it.isNotBlank() }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(originColor.copy(alpha = 0.18f), CircleShape)
                    .border(1.dp, originColor.copy(alpha = 0.4f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                if (comment.originSlug.equals("flicktrove", ignoreCase = true)) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_flicktrove_logo),
                        contentDescription = "FlickTrove",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(width = 16.dp, height = 12.dp)
                    )
                } else if (!originIconUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = originIconUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(14.dp)
                            .clip(RoundedCornerShape(3.5.dp))
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_world),
                        contentDescription = null,
                        tint = originColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = comment.originName.ifBlank {
                        comment.originSlug.replaceFirstChar { char ->
                            if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString()
                        }
                    },
                    color = originColor,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                    maxLines = 1
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        if (isOwner && !isEffectivelyDeleted && comment.createdAt != null) {
            val timeSinceCreated = System.currentTimeMillis() - comment.createdAt.toDate().time
            if (timeSinceCreated <= 12 * 60 * 60 * 1000) {
                Icon(
                    painter = painterResource(id = if (comment.isSpoiler) R.drawable.ic_eye_off else R.drawable.ic_eye),
                    contentDescription = "Toggle Spoiler",
                    tint = if (comment.isSpoiler) accentColor else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(16.dp)
                        .bounceClick { onToggleSpoilerAuthor() }
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_trash),
                contentDescription = "Elimina",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(14.dp)
                    .bounceClick { onDeleteComment() }
            )
        }
    }
}
