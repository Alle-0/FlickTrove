package com.cinetrack.ui.components.comments

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
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
import androidx.compose.ui.unit.sp
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

private val MEDIA_REGEX = Regex("!\\[.*?\\]\\((.*?)\\)")

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
    onBlockUser: (() -> Unit)? = null,
    onTranslate: (text: String) -> Unit,
    onTriggerGuestAuth: () -> Unit,
    onOpenUrl: ((String) -> Unit)? = null,
    isOwner: Boolean = (currentUserId != null && comment.userId == currentUserId),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val visualDepth = comment.depth.coerceAtMost(3)
    val baseStartPadding = 16.dp
    val indentSpacing = 20.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val startX = (baseStartPadding + 18.dp).toPx() // center of first avatar
                val spacingPx = indentSpacing.toPx()
                val avatarCenterY = (12.dp + 18.dp).toPx() // top padding + half avatar

                if (visualDepth > 0) {
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
            val avatarBgColor = remember(comment.userDisplayName, comment.originSlug) {
                if (comment.userDisplayName.isNotBlank()) {
                    val hue = (comment.userDisplayName.fold(0) { acc, c -> acc * 31 + c.code }.and(0x7FFFFFFF) % 360).toFloat()
                    Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.45f, 0.35f)))
                } else {
                    Color(0xFF2A2A2A)
                }
            }
            var isImageError by remember(comment.userAvatarUrl) { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(avatarBgColor),
                contentAlignment = Alignment.Center
            ) {
                val initial = comment.userDisplayName.trim().firstOrNull()?.uppercaseChar()
                if (initial != null && initial.isLetterOrDigit()) {
                    Text(
                        text = initial.toString(),
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_persona),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                val safeAvatarUrl = comment.userAvatarUrl.trim()
                if (safeAvatarUrl.isNotBlank() && !isImageError) {
                    val modelData: Any = if (safeAvatarUrl.startsWith("data:image", ignoreCase = true) && safeAvatarUrl.contains("base64,")) {
                        try {
                            val base64Data = safeAvatarUrl.substringAfter("base64,")
                            android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                        } catch (e: Exception) {
                            safeAvatarUrl
                        }
                    } else {
                        safeAvatarUrl
                    }

                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(modelData)
                            .crossfade(true)
                            .build(),
                        imageLoader = imageLoader,
                        contentDescription = comment.userDisplayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onError = { errorResult ->
                            android.util.Log.e("CommentAvatar", "Failed loading avatar for ${comment.userDisplayName} ($safeAvatarUrl): ${errorResult.result.throwable.message}")
                            isImageError = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Body
            Column(modifier = Modifier.weight(1f)) {
                val isEffectivelyDeleted = comment.isEffectivelyDeleted

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

                val textWithoutMedia = remember(displayedTextRaw) {
                    val clean = displayedTextRaw.replace(MEDIA_REGEX, "").trim()
                    if (clean == "[image]" || clean == "[gif]") "" else clean
                }

                val mediaUrls = remember(comment.text, comment.attachedMedia) {
                    val inlineMediaUrls = MEDIA_REGEX.findAll(comment.text).map { it.groupValues[1] }.toList()
                    (inlineMediaUrls + comment.attachedMedia).distinct()
                }

                val parentDisplayName = remember(comment.depth, comment.parentId, flatTree) {
                    if (comment.depth >= 3 && comment.parentId != null) {
                        flatTree.find { it.id == comment.parentId }?.userDisplayName
                    } else null
                }

                val annotatedCommentText = remember(mediaUrls, textWithoutMedia, displayedTextRaw, accentColor, parentDisplayName, onOpenUrl) {
                    buildAnnotatedString {
                        if (parentDisplayName != null) {
                            withStyle(style = SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) {
                                append("@$parentDisplayName ")
                            }
                        }
                        val targetText = if (mediaUrls.isNotEmpty()) textWithoutMedia else displayedTextRaw
                        append(parseSimpleMarkdown(targetText, accentColor, onLinkClick = onOpenUrl))
                    }
                }

                val contentToDraw = @Composable { isBlurred: Boolean ->
                    Column {
                        if (textWithoutMedia.isNotEmpty() || mediaUrls.isEmpty()) {
                            Text(
                                text = annotatedCommentText,
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
                                        .crossfade(true)
                                        .apply {
                                            if (mediaUrl.contains("commsuni.tv")) {
                                                addHeader("Authorization", "Bearer ${com.cinetrack.BuildConfig.COMMSUNI_API_KEY}")
                                                addHeader("User-Agent", "FlickTrove-Android/${com.cinetrack.BuildConfig.VERSION_NAME}")
                                            }
                                        }
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
                        if (!isOwner) {
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.ic_flag),
                                contentDescription = stringResource(R.string.comment_report_title),
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(14.dp)
                                    .bounceClick {
                                        if (isUserAnonymous) onTriggerGuestAuth()
                                        else onReport()
                                    }
                            )
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

@Composable
fun TranslationProgressIndicator(
    translationState: CommentsViewModel.TranslationState?,
    accentColor: Color,
    onTranslate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDownloading = translationState is CommentsViewModel.TranslationState.Downloading
    val isTranslating = translationState is CommentsViewModel.TranslationState.Translating
    val isTranslated = translationState is CommentsViewModel.TranslationState.Translated
    val isProgressVisible = isDownloading || isTranslating

    val progress = remember { Animatable(0.08f) }

    val infiniteTransition = rememberInfiniteTransition(label = "downloadingShimmer")
    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerPhase"
    )
    val iconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.50f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconPulse"
    )

    LaunchedEffect(isDownloading) {
        if (isDownloading) {
            progress.snapTo(0.08f)
            // Multi-phase continuous filling curve scaled to realistic ML Kit model downloads:
            // Phase 1: Fast start response
            progress.animateTo(0.35f, tween(2000, easing = FastOutSlowInEasing))
            // Phase 2: Steady mid download
            progress.animateTo(0.65f, tween(4000, easing = LinearEasing))
            // Phase 3: High progress
            progress.animateTo(0.85f, tween(6000, easing = LinearOutSlowInEasing))
            // Phase 4: Long buffer crawl so it never freezes
            progress.animateTo(0.96f, tween(12000, easing = LinearOutSlowInEasing))
        }
    }

    LaunchedEffect(isTranslating) {
        if (isTranslating) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
            )
        }
    }

    // Animate width & spacing to smoothly slide the icon to the left when opening,
    // and slide it back to the right when closing (preserving rounded pill shape at every frame)
    val animatedBarWidth by animateDpAsState(
        targetValue = if (isProgressVisible) 36.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "translationBarWidth"
    )
    val animatedSpacing by animateDpAsState(
        targetValue = if (isProgressVisible) 6.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "translationSpacing"
    )
    val animatedBarAlpha by animateFloatAsState(
        targetValue = if (isProgressVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isProgressVisible) 220 else 180,
            easing = FastOutSlowInEasing
        ),
        label = "translationBarAlpha"
    )

    val targetIconTint = when {
        isDownloading -> accentColor.copy(alpha = iconAlpha)
        isTranslating -> accentColor
        isTranslated -> accentColor
        else -> Color.White.copy(alpha = 0.55f)
    }
    val animatedIconTint by animateColorAsState(
        targetValue = targetIconTint,
        animationSpec = tween(220),
        label = "translationIconTint"
    )

    val canClick = !isDownloading && !isTranslating

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_traduzione),
            contentDescription = stringResource(
                if (isTranslated) R.string.comment_show_original else R.string.comment_translate
            ),
            tint = animatedIconTint,
            modifier = Modifier
                .size(14.dp)
                .bounceClick(enabled = canClick) { onTranslate() }
        )

        if (animatedBarWidth > 0.5.dp && animatedBarAlpha > 0.01f) {
            Spacer(modifier = Modifier.width(animatedSpacing))
            Box(
                modifier = Modifier
                    .width(animatedBarWidth)
                    .height(4.dp)
                    .graphicsLayer { alpha = animatedBarAlpha }
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                val currentProg = progress.value.coerceIn(0.06f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = currentProg)
                        .clip(CircleShape)
                        .background(
                            if (isTranslating) {
                                Brush.linearGradient(listOf(accentColor, accentColor))
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.70f),
                                        Color.White.copy(alpha = 0.90f),
                                        accentColor
                                    ),
                                    startX = -40f + shimmerPhase * 100f,
                                    endX = shimmerPhase * 100f
                                )
                            }
                        )
                )
            }
        }
    }
}
