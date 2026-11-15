package com.cinetrack.ui.components.comments

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.cinetrack.R
import com.cinetrack.ui.components.shared.shimmerEffect
import com.cinetrack.ui.utils.TaggedMediaRef
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.verticalFadingEdges
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.ImageType
import com.cinetrack.util.buildTmdbImageUrl

data class ActiveHashtagMatch(
    val hashIndex: Int,
    val replaceEndIndex: Int,
    val query: String
)

fun findActiveHashtagQuery(
    text: String,
    cursor: Int,
    selectedTags: List<TaggedMediaRef>
): ActiveHashtagMatch? {
    val safeCursor = cursor.coerceIn(0, text.length)
    val beforeCursor = text.substring(0, safeCursor)
    val lineStart = beforeCursor.lastIndexOf('\n') + 1

    var hashIdx = -1
    for (i in (safeCursor - 1) downTo lineStart) {
        if (beforeCursor[i] == '#') {
            val prevIsBoundary = i == 0 || beforeCursor[i - 1].isWhitespace()
            if (prevIsBoundary) {
                hashIdx = i
                break
            }
        }
    }
    if (hashIdx == -1) return null

    // Ignore '#' if it belongs to an already-confirmed selected media tag
    for (tag in selectedTags) {
        val token = "#${tag.title}"
        if (text.startsWith(token, startIndex = hashIdx)) {
            val tokenEnd = hashIdx + token.length
            if (safeCursor <= tokenEnd || (safeCursor > tokenEnd && text.getOrNull(tokenEnd)?.isWhitespace() == true)) {
                return null
            }
        }
    }

    val rawQuery = text.substring(hashIdx + 1, safeCursor)
    if (rawQuery.startsWith(" ") || rawQuery.length > 50 || rawQuery.contains("  ") || rawQuery.count { it == ' ' } > 5) return null
    val query = rawQuery.trim()

    return ActiveHashtagMatch(
        hashIndex = hashIdx,
        replaceEndIndex = safeCursor,
        query = query
    )
}

@Composable
fun MediaTagSuggestionRow(
    suggestion: TaggedMediaRef,
    accentColor: Color,
    imageLoader: coil3.ImageLoader,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val posterUrl = remember(suggestion.posterPath) {
        buildTmdbImageUrl(suggestion.posterPath, ImageType.POSTER, ImageQuality.LOW)
    }
    val typeLabel = stringResource(
        if (suggestion.mediaType == "tv") R.string.notif_media_tv else R.string.notif_media_movie
    )
    val subtitle = remember(typeLabel, suggestion.releaseYear) {
        if (!suggestion.releaseYear.isNullOrBlank()) "$typeLabel • ${suggestion.releaseYear}" else typeLabel
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onClick() }
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 28.dp, height = 42.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            if (posterUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(posterUrl)
                        .crossfade(true)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    painter = painterResource(
                        id = if (suggestion.mediaType == "tv") R.drawable.ic_tv else R.drawable.ic_film
                    ),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = suggestion.title,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.55f),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = "#",
            color = accentColor.copy(alpha = 0.85f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 6.dp)
        )
    }
}

@Composable
fun CommentHashtagSuggestionsCard(
    visible: Boolean,
    activeMatch: ActiveHashtagMatch?,
    isSearchingMediaTags: Boolean,
    mediaTagSuggestions: List<TaggedMediaRef>,
    accentColor: Color,
    inputText: TextFieldValue,
    onInputTextChanged: (TextFieldValue) -> Unit,
    selectedMediaTags: List<TaggedMediaRef>,
    onSelectedMediaTagsChanged: (List<TaggedMediaRef>) -> Unit,
    onDismissMatch: () -> Unit,
    onClearMediaTagSuggestions: () -> Unit,
    imageLoader: coil3.ImageLoader,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && activeMatch != null,
        enter = fadeIn(tween(150)) + expandVertically(tween(180)),
        exit = fadeOut(tween(120)) + shrinkVertically(tween(150)),
        modifier = modifier
    ) {
        if (activeMatch != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .padding(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_film),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "#${activeMatch.query}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .bounceClick {
                                onDismissMatch()
                                onClearMediaTagSuggestions()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_x),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (activeMatch.query.length < 2) {
                    Text(
                        text = stringResource(R.string.comment_tag_search_hint),
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                } else if (isSearchingMediaTags && mediaTagSuggestions.isEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(2) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .shimmerEffect()
                            ) {}
                        }
                    }
                } else {
                    val suggestionListState = rememberLazyListState()
                    LaunchedEffect(activeMatch.query) {
                        suggestionListState.scrollToItem(0)
                    }
                    LazyColumn(
                        state = suggestionListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 176.dp)
                            .verticalFadingEdges(
                                lazyListState = suggestionListState,
                                topEdgeHeight = 18.dp,
                                bottomEdgeHeight = 18.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = mediaTagSuggestions,
                            key = { "${it.mediaType}_${it.id}" }
                        ) { suggestion ->
                            MediaTagSuggestionRow(
                                suggestion = suggestion,
                                accentColor = accentColor,
                                imageLoader = imageLoader,
                                onClick = {
                                    val currentText = inputText.text
                                    val start = activeMatch.hashIndex.coerceIn(0, currentText.length)
                                    val end = activeMatch.replaceEndIndex.coerceIn(start, currentText.length)
                                    val inserted = "#${suggestion.title} "
                                    val updatedText = currentText.substring(0, start) + inserted + currentText.substring(end)
                                    val newCursor = start + inserted.length
                                    onInputTextChanged(
                                        TextFieldValue(
                                            text = updatedText,
                                            selection = TextRange(newCursor)
                                        )
                                    )
                                    val nextTags = selectedMediaTags.filterNot {
                                        it.id == suggestion.id && it.mediaType == suggestion.mediaType && it.title == suggestion.title
                                    } + suggestion
                                    onSelectedMediaTagsChanged(nextTags)
                                    onDismissMatch()
                                    onClearMediaTagSuggestions()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
