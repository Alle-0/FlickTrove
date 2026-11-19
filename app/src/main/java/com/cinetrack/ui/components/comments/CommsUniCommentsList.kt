package com.cinetrack.ui.components.comments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import com.cinetrack.R
import com.cinetrack.data.model.AppComment
import com.cinetrack.ui.viewmodel.CommentsViewModel

@Composable
fun CommsUniCommentsList(
    listState: LazyListState,
    paddingValues: PaddingValues,
    isLoading: Boolean,
    flatTree: List<AppComment>,
    comments: List<AppComment>,
    expandedComments: Set<String>,
    onToggleExpand: (AppComment) -> Unit,
    hasMoreComments: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    isCommentLiked: (AppComment) -> Boolean,
    isCommentMine: (AppComment) -> Boolean,
    translationStates: Map<String, CommentsViewModel.TranslationState>,
    currentUserId: String?,
    isUserAnonymous: Boolean,
    accentColor: Color,
    imageLoader: ImageLoader,
    shimmerBrush: Brush,
    onToggleSpoilerAuthor: (AppComment) -> Unit,
    onDeleteComment: (AppComment) -> Unit,
    onReply: (AppComment) -> Unit,
    onToggleLike: (AppComment) -> Unit,
    onReport: (AppComment) -> Unit,
    onBlockUser: (AppComment) -> Unit,
    onTranslate: (commentId: String, text: String) -> Unit,
    onTriggerGuestAuth: () -> Unit,
    onOpenUrl: (String) -> Unit,
    taggedMediaTitles: Map<String, String>,
    taggedMediaPosters: Map<String, String>,
    onMediaTagClick: (mediaType: String, mediaId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = paddingValues.calculateTopPadding() + 10.dp, bottom = 140.dp)
    ) {
        if (isLoading) {
            items(5) { index ->
                CommentSkeletonItem(index = index, brush = shimmerBrush)
            }
        } else if (flatTree.isEmpty()) {
            item(key = "comments_empty_state") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.comment_empty_state),
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            itemsIndexed(flatTree, key = { _, it -> it.id }) { index, comment ->
                val isLiked = isCommentLiked(comment)
                val isExpanded = expandedComments.contains(comment.id)
                CommentCardItem(
                    modifier = Modifier.animateItem(),
                    comment = comment,
                    index = index,
                    flatTree = flatTree,
                    allComments = comments,
                    isLiked = isLiked,
                    translationState = translationStates[comment.id],
                    currentUserId = currentUserId,
                    isOwner = isCommentMine(comment),
                    isUserAnonymous = isUserAnonymous,
                    accentColor = accentColor,
                    isExpanded = isExpanded,
                    onToggleExpand = { onToggleExpand(comment) },
                    imageLoader = imageLoader,
                    onToggleSpoilerAuthor = { onToggleSpoilerAuthor(comment) },
                    onDeleteComment = { onDeleteComment(comment) },
                    onReply = { onReply(comment) },
                    onToggleLike = { onToggleLike(comment) },
                    onReport = { onReport(comment) },
                    onBlockUser = { onBlockUser(comment) },
                    onTranslate = { text -> onTranslate(comment.id, text) },
                    onTriggerGuestAuth = onTriggerGuestAuth,
                    onOpenUrl = onOpenUrl,
                    localizedMediaTitles = taggedMediaTitles,
                    localizedMediaPosters = taggedMediaPosters,
                    onMediaTagClick = onMediaTagClick
                )
            }
        }

        if (hasMoreComments) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoadingMore) {
                        CircularProgressIndicator(color = accentColor, modifier = Modifier.size(24.dp))
                    } else {
                        TextButton(onClick = onLoadMore) {
                            Text(
                                text = stringResource(R.string.comment_load_more),
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
