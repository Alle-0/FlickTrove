package com.cinetrack.ui.components.comments

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cinetrack.R
import com.cinetrack.data.model.AppComment
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.parseSimpleMarkdown
import com.cinetrack.ui.viewmodel.CommentsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CommentCardItem(
    comment: AppComment,
    index: Int,
    flatTree: List<AppComment>,
    allComments: List<AppComment>,
    isLiked: Boolean,
    translationState: CommentsViewModel.TranslationState?,
    currentUserId: String?,
    isUserAnonymous: Boolean,
    accentColor: Color,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    imageLoader: ImageLoader,
    onToggleSpoilerAuthor: () -> Unit,
    onDeleteComment: () -> Unit,
    onReply: () -> Unit,
    onToggleLike: () -> Unit,
    onReport: () -> Unit,
    onTranslate: (text: String) -> Unit,
    onTriggerGuestAuth: () -> Unit
) {
    val context = LocalContext.current
    val visualDepth = comment.depth.coerceAtMost(3)
    val baseStartPadding = 16.dp
    val indentSpacing = 20.dp

    val parentExpanded = remember(comment.parentId, flatTree) {
        if (comment.parentId == null) true
        else flatTree.any { it.id == comment.parentId }
    }

    var hasAppeared by remember { mutableStateOf(false) }
    var isAppearing by remember(parentExpanded) { mutableStateOf(hasAppeared) }
    LaunchedEffect(parentExpanded) {
        if (!hasAppeared) {
            isAppearing = true
            hasAppeared = true
        }
    }

    AnimatedVisibility(
        visible = isAppearing,
        enter = expandVertically() + fadeIn()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val startX = (baseStartPadding + 18.dp).toPx() // center of first avatar
                    val spacingPx = indentSpacing.toPx()
                    val avatarCenterY = (12.dp + 18.dp).toPx() // top padding + half avatar

                    for (i in 0 until visualDepth) {
                        val xPos = startX + (i * spacingPx)
                        val isLastVisualLevel = (i == visualDepth - 1)
                        val targetDepth = if (isLastVisualLevel) comment.depth else i + 1

                        var hasNextChild = false
                        for (j in (index + 1) until flatTree.size) {
                            if (flatTree[j].depth < targetDepth) break
                            if (flatTree[j].depth == targetDepth) {
                                hasNextChild = true
                                break
                            }
                        }

                        if (isLastVisualLevel) {
                            if (hasNextChild) {
                                // T-Junction
                                drawLine(
                                    color = Color.White.copy(alpha = 0.15f),
                                    start = Offset(xPos, 0f),
                                    end = Offset(xPos, size.height),
                                    strokeWidth = 3f,
                                    cap = StrokeCap.Round
                                )
                                val cornerRadius = 12.dp.toPx()
                                val path = Path().apply {
                                    moveTo(xPos, avatarCenterY - cornerRadius)
                                    quadraticTo(xPos, avatarCenterY, xPos + cornerRadius, avatarCenterY)
                                    lineTo(xPos + 12.dp.toPx(), avatarCenterY)
                                }
                                drawPath(
                                    path = path,
                                    color = Color.White.copy(alpha = 0.15f),
                                    style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            } else {
                                // L-Shape
                                val cornerRadius = 16.dp.toPx()
                                val path = Path().apply {
                                    moveTo(xPos, 0f)
                                    lineTo(xPos, avatarCenterY - cornerRadius)
                                    quadraticTo(xPos, avatarCenterY, xPos + cornerRadius, avatarCenterY)
                                    lineTo(xPos + 12.dp.toPx(), avatarCenterY)
                                }
                                drawPath(
                                    path = path,
                                    color = Color.White.copy(alpha = 0.15f),
                                    style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }
                        } else {
                            // Pass-through line for ancestor
                            if (hasNextChild) {
                                drawLine(
                                    color = Color.White.copy(alpha = 0.15f),
                                    start = Offset(xPos, 0f),
                                    end = Offset(xPos, size.height),
                                    strokeWidth = 3f,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }

                    // Draw line down from our own avatar if we have children
                    val hasChildren = index < flatTree.lastIndex && flatTree[index + 1].depth > comment.depth
                    if (hasChildren) {
                        val myX = startX + (visualDepth * spacingPx)
                        drawLine(
                            color = Color.White.copy(alpha = 0.15f),
                            start = Offset(myX, avatarCenterY),
                            end = Offset(myX, size.height),
                            strokeWidth = 3f
                        )
                    }
                }
                .padding(
                    start = baseStartPadding + (indentSpacing * visualDepth),
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 12.dp
                )
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.DarkGray)
            ) {
                if (comment.userAvatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = comment.userAvatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Body
            Column(modifier = Modifier.weight(1f)) {
                val isEffectivelyDeleted = comment.isDeleted || comment.userId.isBlank() || (comment.text.isBlank() && comment.userDisplayName.isBlank())

                // Author Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isEffectivelyDeleted) stringResource(R.string.comment_deleted) else comment.userDisplayName.ifBlank { stringResource(R.string.comment_anonymous_user) },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (comment.userId == currentUserId && !isEffectivelyDeleted && comment.createdAt != null) {
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

                Spacer(modifier = Modifier.height(4.dp))
                var isTextExpanded by remember { mutableStateOf(false) }
                var isTextExpandable by remember { mutableStateOf(false) }

                var isSpoilerRevealed by remember { mutableStateOf(false) }
                var tapOffset by remember { mutableStateOf(Offset.Zero) }
                val revealRadius = remember { androidx.compose.animation.core.Animatable(0f) }
                val coroutineScope = rememberCoroutineScope()

                val displayedTextRaw = when {
                    isEffectivelyDeleted -> stringResource(R.string.comment_deleted)
                    translationState is CommentsViewModel.TranslationState.Translated -> translationState.text
                    else -> comment.text
                }

                val mediaRegex = Regex("!\\[(?:gif|foto)\\]\\((.*?)\\)")
                val textWithoutMedia = displayedTextRaw.replace(mediaRegex, "").trim()
                val mediaUrls = mediaRegex.findAll(comment.text).map { it.groupValues[1] }.toList()

                val contentToDraw = @Composable { isBlurred: Boolean ->
                    Column {
                        if (textWithoutMedia.isNotEmpty() || mediaUrls.isEmpty()) {
                            Text(
                                text = buildAnnotatedString {
                                    if (comment.depth >= 3 && comment.parentId != null) {
                                        val parentComment = flatTree.find { it.id == comment.parentId }
                                        if (parentComment != null) {
                                            withStyle(style = SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) {
                                                append("@${parentComment.userDisplayName} ")
                                            }
                                        }
                                    }
                                    append(parseSimpleMarkdown(if (mediaUrls.isNotEmpty()) textWithoutMedia else displayedTextRaw, accentColor))
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = if (isTextExpanded) Int.MAX_VALUE else 6,
                                overflow = TextOverflow.Ellipsis,
                                onTextLayout = { textLayoutResult ->
                                    if (!isTextExpanded && textLayoutResult.hasVisualOverflow) {
                                        isTextExpandable = true
                                    }
                                },
                                modifier = Modifier
                                    .animateContentSize()
                                    .then(
                                        if (isBlurred) Modifier.clip(RoundedCornerShape(8.dp)).blur(16.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded) else Modifier
                                    )
                            )
                        }
                        if (mediaUrls.isNotEmpty()) {
                            mediaUrls.forEach { mediaUrl ->
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(mediaUrl)
                                        .build(),
                                    imageLoader = imageLoader,
                                    contentDescription = "Attachment",
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .then(
                                            if (isBlurred) Modifier.clip(RoundedCornerShape(12.dp)).blur(16.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded) else Modifier
                                        ),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }

                // Spoiler Reveal Box
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 32.dp)
                        .pointerInput(comment.isSpoiler, isSpoilerRevealed) {
                            if (comment.isSpoiler && !isSpoilerRevealed) {
                                detectTapGestures(onTap = { offset ->
                                    tapOffset = offset
                                    coroutineScope.launch {
                                        revealRadius.animateTo(
                                            targetValue = 2000f,
                                            animationSpec = tween(600, easing = FastOutSlowInEasing)
                                        )
                                        isSpoilerRevealed = true
                                    }
                                })
                            }
                        }
                ) {
                    if (comment.isSpoiler && !isSpoilerRevealed) {
                        contentToDraw(true)

                        if (revealRadius.value > 0f) {
                            Box(modifier = Modifier
                                .matchParentSize()
                                .clip(GenericShape { _, _ ->
                                    addOval(Rect(
                                        center = tapOffset,
                                        radius = revealRadius.value
                                    ))
                                })
                            ) {
                                contentToDraw(false)
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = revealRadius.value == 0f,
                            enter = fadeIn(),
                            exit = fadeOut(animationSpec = tween(200)),
                            modifier = Modifier.matchParentSize()
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                                    Box(modifier = Modifier.matchParentSize().background(Color(0xFF141414).copy(alpha = 0.9f), RoundedCornerShape(8.dp)))
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_eye),
                                        contentDescription = "Rivela",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    val showText = textWithoutMedia.length >= 20 || mediaUrls.isNotEmpty()
                                    if (showText) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.comment_tap_to_reveal),
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        contentToDraw(false)
                    }
                }

                if (isTextExpandable) {
                    Text(
                        text = if (isTextExpanded) stringResource(R.string.comment_collapse) else stringResource(R.string.comment_expand),
                        color = accentColor,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .bounceClick { isTextExpanded = !isTextExpanded }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Actions Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!isEffectivelyDeleted) {
                        Text(
                            text = stringResource(R.string.comment_reply_btn),
                            color = accentColor,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.bounceClick {
                                if (isUserAnonymous) onTriggerGuestAuth()
                                else onReply()
                            }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Row(
                            modifier = Modifier.bounceClick {
                                if (isUserAnonymous) onTriggerGuestAuth()
                                else onToggleLike()
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LiquidStarIcon(
                                isLiked = isLiked,
                                accentColor = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${comment.likesCount}",
                                color = if (isLiked) accentColor else Color.White.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        if (comment.userId != currentUserId) {
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.ic_flag),
                                contentDescription = stringResource(R.string.comment_report_title),
                                tint = Color.Red,
                                modifier = Modifier
                                    .size(14.dp)
                                    .bounceClick {
                                        if (isUserAnonymous) onTriggerGuestAuth()
                                        else onReport()
                                    }
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Translate button
                        when (translationState) {
                            is CommentsViewModel.TranslationState.Downloading,
                            is CommentsViewModel.TranslationState.Translating -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = accentColor
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                            is CommentsViewModel.TranslationState.Translated -> {
                                Icon(
                                    painter = painterResource(R.drawable.ic_traduzione),
                                    contentDescription = stringResource(R.string.comment_show_original),
                                    tint = accentColor,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .bounceClick { onTranslate(comment.text) }
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                            else -> {
                                Icon(
                                    painter = painterResource(R.drawable.ic_traduzione),
                                    contentDescription = stringResource(R.string.comment_translate),
                                    tint = Color.White.copy(alpha = 0.55f),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .bounceClick { onTranslate(comment.text) }
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                        }
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
                                else -> SimpleDateFormat("dd MMM yy", Locale.getDefault()).format(date)
                            }
                        }
                        Text(
                            text = timeStr,
                            color = Color.White.copy(alpha = 0.3f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Replies Toggle
                val childrenCount = maxOf(comment.repliesCount, allComments.count { it.parentId == comment.id })
                if (childrenCount > 0) {
                    Row(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .bounceClick { onToggleExpand() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val toggleColor = Color.White.copy(alpha = 0.6f)
                        Box(modifier = Modifier.width(12.dp).height(1.dp).background(toggleColor.copy(alpha = 0.3f)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isExpanded) stringResource(R.string.comment_hide_replies) else if (childrenCount == 1) stringResource(R.string.comment_view_replies_singular, childrenCount) else stringResource(R.string.comment_view_replies_plural, childrenCount),
                            color = toggleColor,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
