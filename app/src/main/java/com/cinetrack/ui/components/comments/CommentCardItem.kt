package com.cinetrack.ui.components.comments

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.network.httpHeaders
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.cinetrack.R
import com.cinetrack.data.model.AppComment
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.bounceClickWithOffset
import com.cinetrack.ui.utils.parseSimpleMarkdownWithMediaTags
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.ImageType
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.ui.viewmodel.CommentsViewModel
import com.cinetrack.ui.components.common.ZoomableImageViewerDialog
import com.cinetrack.util.DateTimeUtils
import kotlinx.coroutines.launch
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
    localizedMediaTitles: Map<String, String> = emptyMap(),
    localizedMediaPosters: Map<String, String> = emptyMap(),
    onMediaTagClick: ((mediaType: String, mediaId: Long) -> Unit)? = null,
    isOwner: Boolean = (currentUserId != null && comment.userId == currentUserId),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val visualDepth = comment.depth.coerceAtMost(3)
    val baseStartPadding = 16.dp
    val indentSpacing = 20.dp
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

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
            val effectiveAvatarUrl = remember(comment.userAvatarUrl, isOwner, currentUserId) {
                val candidate = comment.userAvatarUrl.trim()
                if (candidate.isNotBlank()) {
                    if (candidate.startsWith("/")) "https://api.commsuni.tv$candidate" else candidate
                } else if (isOwner) {
                    val uid = currentUserId ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null) {
                        val commsPrefs = context.getSharedPreferences("commsuni_prefs", android.content.Context.MODE_PRIVATE)
                        val namePrefs = context.getSharedPreferences("user_name_changes", android.content.Context.MODE_PRIVATE)
                        val localCandidate = commsPrefs.getString("user_avatar_$uid", null)?.trim()?.takeIf { it.isNotBlank() }
                            ?: namePrefs.getString("user_avatar_$uid", null)?.trim()?.takeIf { it.isNotBlank() }
                            ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.photoUrl?.toString()?.trim()?.takeIf { it.isNotBlank() }
                        if (localCandidate?.startsWith("/") == true) "https://api.commsuni.tv$localCandidate" else localCandidate.orEmpty()
                    } else ""
                } else ""
            }
            var isImageError by remember(effectiveAvatarUrl) { mutableStateOf(false) }

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

                val safeAvatarUrl = effectiveAvatarUrl
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
                            .apply {
                                if (safeAvatarUrl.contains("commsuni.tv")) {
                                    httpHeaders(
                                        coil3.network.NetworkHeaders.Builder()
                                            .set("Authorization", "Bearer ${com.cinetrack.BuildConfig.COMMSUNI_API_KEY}")
                                            .set("User-Agent", "FlickTrove-Android/${com.cinetrack.BuildConfig.VERSION_NAME}")
                                            .build()
                                    )
                                }
                            }
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
                CommentAuthorHeader(
                    comment = comment,
                    isOwner = isOwner,
                    isEffectivelyDeleted = isEffectivelyDeleted,
                    accentColor = accentColor,
                    onToggleSpoilerAuthor = onToggleSpoilerAuthor,
                    onDeleteComment = onDeleteComment
                )

                Spacer(modifier = Modifier.height(4.dp))
                var isTextExpanded by remember { mutableStateOf(false) }
                var isTextExpandable by remember { mutableStateOf(false) }

                var isSpoilerRevealed by remember { mutableStateOf(false) }
                var tapOffset by remember { mutableStateOf(Offset.Zero) }
                val revealRadius = remember { androidx.compose.animation.core.Animatable(0f) }
                val coroutineScope = rememberCoroutineScope()

                val revealSpoiler: (Offset) -> Unit = { offset ->
                    tapOffset = offset
                    coroutineScope.launch {
                        revealRadius.animateTo(
                            targetValue = 2000f,
                            animationSpec = tween(600, easing = FastOutSlowInEasing)
                        )
                        isSpoilerRevealed = true
                    }
                }

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
                    val inlineMediaUrls = MEDIA_REGEX.findAll(comment.text).map { it.groupValues[1].trim() }.filter { it.isNotBlank() }.toList()
                    val attached = comment.attachedMedia.map { it.trim() }.filter { it.isNotBlank() }
                    (inlineMediaUrls + attached).map { url ->
                        if (url.startsWith("/")) "https://api.commsuni.tv$url" else url
                    }.distinct()
                }

                val parentDisplayName = remember(comment.depth, comment.parentId, flatTree) {
                    if (comment.depth >= 3 && comment.parentId != null) {
                        flatTree.find { it.id == comment.parentId }?.userDisplayName
                    } else null
                }

                val contentToDraw = @Composable { isBlurred: Boolean ->
                    Column {
                        if (textWithoutMedia.isNotEmpty() || parentDisplayName != null || mediaUrls.isEmpty()) {
                            val targetText = if (mediaUrls.isNotEmpty()) textWithoutMedia else displayedTextRaw
                            CommentRichMarkdownText(
                                text = targetText,
                                accentColor = accentColor,
                                textStyle = MaterialTheme.typography.bodyMedium,
                                textColor = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.fillMaxWidth(),
                                prefixText = parentDisplayName?.let { "@$it " },
                                localizedMediaTitles = localizedMediaTitles,
                                localizedMediaPosters = localizedMediaPosters,
                                imageLoader = imageLoader,
                                onOpenUrl = onOpenUrl,
                                onMediaTagClick = onMediaTagClick,
                                maxLines = if (isTextExpanded) Int.MAX_VALUE else 6,
                                overflow = TextOverflow.Ellipsis,
                                isBlurred = isBlurred,
                                onTextLayout = { textLayoutResult ->
                                    if (!isTextExpanded && textLayoutResult.hasVisualOverflow) {
                                        isTextExpandable = true
                                    }
                                }
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
                                                httpHeaders(
                                                    coil3.network.NetworkHeaders.Builder()
                                                        .set("Authorization", "Bearer ${com.cinetrack.BuildConfig.COMMSUNI_API_KEY}")
                                                        .set("User-Agent", "FlickTrove-Android/${com.cinetrack.BuildConfig.VERSION_NAME}")
                                                        .build()
                                                )

                                            }
                                        }
                                        .build(),
                                    imageLoader = imageLoader,
                                    contentDescription = "Attachment",
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .fillMaxWidth()
                                        .then(
                                            if (isBlurred) {
                                                Modifier.bounceClickWithOffset(scaleDown = 0.98f) { offset ->
                                                    revealSpoiler(offset)
                                                }
                                            } else {
                                                Modifier.bounceClick(
                                                    scaleDown = 0.96f,
                                                    shape = RoundedCornerShape(20.dp)
                                                ) {
                                                    previewImageUrl = mediaUrl
                                                }
                                            }
                                        )
                                        .clip(RoundedCornerShape(20.dp))
                                        .then(
                                            if (isBlurred) Modifier.blur(16.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded).clip(RoundedCornerShape(20.dp)) else Modifier
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
                                    revealSpoiler(offset)
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
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .bounceClickWithOffset(scaleDown = 0.99f) { offset ->
                                        revealSpoiler(offset)
                                    }
                            ) {
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
                CommentCardActionsRow(
                    comment = comment,
                    isLiked = isLiked,
                    isOwner = isOwner,
                    isEffectivelyDeleted = isEffectivelyDeleted,
                    isUserAnonymous = isUserAnonymous,
                    accentColor = accentColor,
                    translationState = translationState,
                    onTriggerGuestAuth = onTriggerGuestAuth,
                    onReply = onReply,
                    onToggleLike = onToggleLike,
                    onReport = onReport,
                    onTranslate = onTranslate
                )

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

    ZoomableImageViewerDialog(
        visible = previewImageUrl != null,
        imageUrl = previewImageUrl,
        onDismiss = { previewImageUrl = null }
    )
}
