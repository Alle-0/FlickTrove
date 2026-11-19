package com.cinetrack.ui.components.comments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.cinetrack.R
import com.cinetrack.data.model.AppComment
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.stripTmdbMediaLinkForPreview

/**
 * Top header banner displayed in the comment input bar when replying to another user's comment.
 */
@Composable
fun CommentReplyingToHeader(
    replyingTo: AppComment?,
    accentColor: Color,
    onCancelReply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lastReplyingTo = remember { mutableStateOf(replyingTo) }
    if (replyingTo != null) {
        lastReplyingTo.value = replyingTo
    }

    AnimatedVisibility(
        visible = replyingTo != null,
        enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
        modifier = modifier
    ) {
        lastReplyingTo.value?.let { replyTarget ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = buildAnnotatedString {
                            append(stringResource(R.string.comment_replying_to) + " ")
                            withStyle(style = SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) {
                                append(replyTarget.userDisplayName)
                            }
                        },
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    val previewMediaRegex = Regex("!\\[(?:gif|foto)\\]\\((.*?)\\)")
                    val cleanText = stripTmdbMediaLinkForPreview(replyTarget.text.replace(previewMediaRegex, "")).trim()
                    val previewText = if (cleanText.isEmpty() && previewMediaRegex.containsMatchIn(replyTarget.text)) {
                        stringResource(R.string.comment_image_preview)
                    } else {
                        cleanText
                    }
                    Text(
                        text = "\"$previewText\"",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.comment_cancel_btn),
                    color = Color.White.copy(0.7f),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.bounceClick { onCancelReply() }
                )
            }
        }
    }
}
