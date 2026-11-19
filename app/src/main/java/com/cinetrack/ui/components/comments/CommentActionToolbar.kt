package com.cinetrack.ui.components.comments

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.VibrationHelper
import com.cinetrack.ui.utils.horizontalFadingEdges

/**
 * Action toolbar displayed in the comment input bar providing media attachment,
 * GIF picker, markdown formatting toggle, spoiler pill, writing tips and CommsUni/FlickTrove destination toggle.
 */
@Composable
fun CommentActionToolbar(
    modifier: Modifier = Modifier,
    attachedMedia: List<String>,
    isUploadingImage: Boolean,
    onPickImage: () -> Unit,
    onPickGif: (Rect?) -> Unit,
    isMarkdownMenuExpanded: Boolean,
    onToggleMarkdownMenu: () -> Unit,
    isSpoiler: Boolean,
    onSpoilerChanged: (Boolean) -> Unit,
    onOpenWritingTips: ((Rect?) -> Unit)?,
    postToCommsUni: Boolean,
    onPostToCommsUniChanged: ((Boolean) -> Unit)?,
    destinationToggleEnabled: Boolean,
    accentColor: Color
) {
    val context = LocalContext.current
    val bottomToolsScrollState = rememberScrollState()

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalFadingEdges(
                    scrollState = bottomToolsScrollState,
                    leftEdgeWidth = 14.dp,
                    rightEdgeWidth = 14.dp
                )
                .horizontalScroll(bottomToolsScrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Foto (Scatta foto o Galleria)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .bounceClick(enabled = attachedMedia.isEmpty() && !isUploadingImage) {
                        if (!isUploadingImage) onPickImage()
                    }
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                if (isUploadingImage) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = accentColor, strokeWidth = 2.dp)
                } else {
                    val fotoAlpha = if (attachedMedia.isEmpty()) 0.75f else 0.25f
                    Icon(
                        painter = painterResource(id = R.drawable.ic_image),
                        contentDescription = stringResource(R.string.comment_pick_gallery),
                        tint = Color.White.copy(alpha = fotoAlpha),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // GIF
            var gifButtonBounds by remember { mutableStateOf<Rect?>(null) }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .onGloballyPositioned { coords ->
                        gifButtonBounds = coords.boundsInWindow()
                    }
                    .bounceClick(enabled = attachedMedia.isEmpty()) {
                        onPickGif(gifButtonBounds)
                    }
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                val gifAlpha = if (attachedMedia.isEmpty()) 0.75f else 0.25f
                Icon(
                    painter = painterResource(id = R.drawable.ic_gif),
                    contentDescription = "GIF",
                    tint = Color.White.copy(alpha = gifAlpha),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Markdown pencil toggle
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .bounceClick { onToggleMarkdownMenu() }
                    .clip(CircleShape)
                    .background(if (isMarkdownMenuExpanded) accentColor.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f))
                    .border(1.dp, if (isMarkdownMenuExpanded) accentColor.copy(alpha = 0.45f) else Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_pencil),
                    contentDescription = "Markdown",
                    tint = if (isMarkdownMenuExpanded) accentColor else Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Spoiler pill button
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .bounceClick {
                        VibrationHelper.vibrateTick(context)
                        onSpoilerChanged(!isSpoiler)
                    }
                    .clip(CircleShape)
                    .background(
                        if (isSpoiler) accentColor.copy(alpha = 0.16f)
                        else Color.White.copy(alpha = 0.06f)
                    )
                    .border(
                        1.dp,
                        if (isSpoiler) accentColor.copy(alpha = 0.40f)
                        else Color.White.copy(alpha = 0.15f),
                        CircleShape
                    )
                    .padding(horizontal = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = if (isSpoiler) R.drawable.ic_eye_off else R.drawable.ic_eye),
                        contentDescription = stringResource(R.string.comment_spoiler_toggle),
                        tint = if (isSpoiler) accentColor else Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.comment_spoiler_toggle),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            lineHeight = 11.sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            )
                        ),
                        color = if (isSpoiler) accentColor else Color.White.copy(alpha = 0.65f),
                        maxLines = 1
                    )
                }
            }

            if (onOpenWritingTips != null) {
                var tipsButtonBounds by remember { mutableStateOf<Rect?>(null) }
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .onGloballyPositioned { coords ->
                            tipsButtonBounds = coords.boundsInWindow()
                        }
                        .bounceClick { onOpenWritingTips(tipsButtonBounds) }
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_question_mark_pieno),
                        contentDescription = stringResource(R.string.comment_writing_tips_title),
                        tint = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Delivery destination pill (pinned to the far right)
        if (onPostToCommsUniChanged != null) {
            Spacer(modifier = Modifier.width(8.dp))
            val isDestinationActive = destinationToggleEnabled
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .then(
                        if (isDestinationActive) {
                            Modifier.bounceClick {
                                VibrationHelper.vibrateTick(context)
                                onPostToCommsUniChanged(!postToCommsUni)
                            }
                        } else {
                            Modifier
                        }
                    )
                    .clip(CircleShape)
                    .background(
                        if (postToCommsUni) accentColor.copy(alpha = if (isDestinationActive) 0.14f else 0.07f)
                        else Color.White.copy(alpha = if (isDestinationActive) 0.06f else 0.03f)
                    )
                    .border(
                        1.dp,
                        if (postToCommsUni) accentColor.copy(alpha = if (isDestinationActive) 0.35f else 0.15f)
                        else Color.White.copy(alpha = if (isDestinationActive) 0.15f else 0.07f),
                        CircleShape
                    )
                    .padding(horizontal = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = if (postToCommsUni) R.drawable.ic_world else R.drawable.ic_lock),
                        contentDescription = null,
                        tint = if (postToCommsUni) accentColor.copy(alpha = if (isDestinationActive) 1f else 0.45f)
                               else Color.White.copy(alpha = if (isDestinationActive) 0.65f else 0.35f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (postToCommsUni) stringResource(R.string.comment_destination_commsuni)
                               else stringResource(R.string.comment_destination_flicktrove),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            lineHeight = 11.sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            )
                        ),
                        color = if (postToCommsUni) accentColor.copy(alpha = if (isDestinationActive) 1f else 0.45f)
                               else Color.White.copy(alpha = if (isDestinationActive) 0.65f else 0.35f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
