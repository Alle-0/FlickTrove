package com.cinetrack.ui.components.comments

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.cinetrack.R
import com.cinetrack.ui.utils.LocalVibrationEnabled
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.parseSimpleMarkdownWithMediaTags
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.ImageType
import com.cinetrack.util.VibrationHelper
import com.cinetrack.util.buildTmdbImageUrl

/**
 * Shared comment markdown renderer supporting inline `#Film`/`#Serie` TMDB tags (poster thumbnail or
 * cinema fallback icon + clean localized title), shielded URL links, and optional `@reply` prefix.
 */
@Composable
fun CommentRichMarkdownText(
    text: String,
    accentColor: Color,
    textStyle: TextStyle,
    textColor: Color,
    modifier: Modifier = Modifier,
    prefixText: String? = null,
    localizedMediaTitles: Map<String, String> = emptyMap(),
    localizedMediaPosters: Map<String, String> = emptyMap(),
    imageLoader: ImageLoader = LocalContext.current.imageLoader,
    onOpenUrl: ((String) -> Unit)? = null,
    onMediaTagClick: ((mediaType: String, mediaId: Long) -> Unit)? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    isBlurred: Boolean = false,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    val context = LocalContext.current
    val vibrationEnabled = LocalVibrationEnabled.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val hapticsOpenUrl = remember(onOpenUrl, context, vibrationEnabled) {
        onOpenUrl?.let { callback ->
            { url: String ->
                if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                callback(url)
            }
        }
    }

    val hapticsMediaTagClick = remember(onMediaTagClick, context, vibrationEnabled) {
        onMediaTagClick?.let { callback ->
            { mediaType: String, mediaId: Long ->
                if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                callback(mediaType, mediaId)
            }
        }
    }

    val parsedMarkdown = remember(
        text,
        accentColor,
        hapticsOpenUrl,
        localizedMediaTitles,
        hapticsMediaTagClick
    ) {
        parseSimpleMarkdownWithMediaTags(
            text = text,
            accentColor = accentColor,
            onLinkClick = hapticsOpenUrl,
            localizedMediaTitles = localizedMediaTitles,
            onMediaTagClick = hapticsMediaTagClick
        )
    }

    val annotatedCommentText = remember(prefixText, accentColor, parsedMarkdown) {
        if (prefixText.isNullOrEmpty()) {
            parsedMarkdown.text
        } else {
            buildAnnotatedString {
                withStyle(style = SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) {
                    append(prefixText)
                }
                append(parsedMarkdown.text)
            }
        }
    }

    val blurModifier = remember(isBlurred) {
        if (isBlurred) {
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .blur(16.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        } else {
            Modifier
        }
    }

    if (parsedMarkdown.mediaTags.isEmpty()) {
        Text(
            text = annotatedCommentText,
            style = textStyle,
            color = textColor,
            maxLines = maxLines,
            overflow = overflow,
            onTextLayout = onTextLayout,
            modifier = modifier.then(blurModifier)
        )
    } else {
        BoxWithConstraints(modifier = modifier) {
            val pillHPadDp = 5.dp
            val pillVPadDp = 1.dp
            val posterWidthDp = 12.dp
            val posterHeightDp = 18.dp
            val iconSizeDp = 13.dp
            val posterGapDp = 4.dp
            val maxBubbleWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val inlineContent = remember(
                parsedMarkdown.mediaTags,
                localizedMediaPosters,
                imageLoader,
                accentColor,
                textStyle,
                maxBubbleWidthPx,
                isBlurred,
                hapticsMediaTagClick,
                hapticsOpenUrl
            ) {
                val tagStyle = textStyle.copy(
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
                val hPadPx = with(density) { (pillHPadDp * 2).toPx() }
                val vPadPx = with(density) { (pillVPadDp * 2).toPx() }
                val posterExtraPx = with(density) { (posterWidthDp + posterGapDp).toPx() }
                val iconExtraPx = with(density) { (iconSizeDp + posterGapDp).toPx() }
                val posterHeightPx = with(density) { posterHeightDp.toPx() }
                val iconHeightPx = with(density) { iconSizeDp.toPx() }
                parsedMarkdown.mediaTags.associate { token ->
                    val tagKey = "${token.mediaType}:${token.mediaId}"
                    val posterUrl = buildTmdbImageUrl(
                        path = localizedMediaPosters[tagKey],
                        type = ImageType.POSTER,
                        quality = ImageQuality.LOW
                    )
                    val cleanTitle = token.displayText
                        .removePrefix("#")
                        .trimStart()
                        .ifEmpty { token.displayText }
                    val measured = textMeasurer.measure(
                        text = cleanTitle,
                        style = tagStyle,
                        maxLines = 1
                    )
                    val leadingExtraPx = if (posterUrl != null) posterExtraPx else iconExtraPx
                    val leadingHeightPx = if (posterUrl != null) posterHeightPx else iconHeightPx
                    val totalWidthPx = (
                        measured.size.width +
                            leadingExtraPx +
                            hPadPx +
                            4f
                        ).coerceAtMost(maxBubbleWidthPx)
                    val totalHeightPx = maxOf(measured.size.height.toFloat(), leadingHeightPx) + vPadPx
                    val fallbackIconRes = if (token.mediaType == "tv") R.drawable.ic_tv else R.drawable.ic_film
                    token.placeholderId to InlineTextContent(
                        placeholder = Placeholder(
                            width = with(density) { totalWidthPx.toSp() },
                            height = with(density) { totalHeightPx.toSp() },
                            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                        )
                    ) {
                        val tagInteractionSource = remember { MutableInteractionSource() }
                        val isTagPressed by tagInteractionSource.collectIsPressedAsState()
                        val pillBgColor by animateColorAsState(
                            targetValue = if (isTagPressed && !isBlurred) accentColor.copy(alpha = 0.24f) else Color.Transparent,
                            animationSpec = tween(durationMillis = 110),
                            label = "mediaTagPillBg"
                        )
                        var posterLoadFailed by remember(posterUrl) { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .bounceClick(
                                    scaleDown = 0.92f,
                                    enabled = !isBlurred,
                                    interactionSource = tagInteractionSource,
                                    onClick = {
                                        if (hapticsMediaTagClick != null) {
                                            hapticsMediaTagClick(token.mediaType, token.mediaId)
                                        } else {
                                            hapticsOpenUrl?.invoke(token.fullUrl)
                                        }
                                    }
                                )
                                .clip(CircleShape)
                                .background(pillBgColor)
                                .padding(horizontal = pillHPadDp, vertical = pillVPadDp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (posterUrl != null && !posterLoadFailed) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(posterUrl)
                                            .crossfade(true)
                                            .build(),
                                        imageLoader = imageLoader,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        onError = { posterLoadFailed = true },
                                        modifier = Modifier
                                            .width(posterWidthDp)
                                            .height(posterHeightDp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = fallbackIconRes),
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(iconSizeDp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(posterGapDp))
                                Text(
                                    text = cleanTitle,
                                    style = tagStyle,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                        }
                    }
                }
            }
            Text(
                text = annotatedCommentText,
                inlineContent = inlineContent,
                style = textStyle,
                color = textColor,
                maxLines = maxLines,
                overflow = overflow,
                onTextLayout = onTextLayout,
                modifier = blurModifier
            )
        }
    }
}
