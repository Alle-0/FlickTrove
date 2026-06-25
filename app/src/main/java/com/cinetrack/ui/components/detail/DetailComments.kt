package com.cinetrack.ui.components.detail

import androidx.compose.ui.res.stringResource
import com.cinetrack.R

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cinetrack.ui.utils.bounceClick
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.geometry.Offset
import com.cinetrack.data.api.TraktComment

@Composable
fun DetailComments(
    comments: List<TraktComment>,
    accentColor: Color,
    translationStates: Map<Long, com.cinetrack.ui.viewmodel.MovieDetailViewModel.TranslationState> = emptyMap(),
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    onTranslateClick: (Long, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    if (comments.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.detail_top_comments),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            ),
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
        )

        val sortedComments = remember(comments) {
            comments.sortedByDescending { it.likes }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(sortedComments, key = { it.id }, contentType = { "comment" }) { comment ->
                CommentCard(
                    comment = comment, 
                    accentColor = accentColor,
                    translationState = translationStates[comment.id],
                    onTranslateRequest = { onTranslateClick(comment.id, comment.comment ?: "") }
                )
            }
        }
    }
}

@Composable
private fun CommentCard(
    comment: TraktComment,
    accentColor: Color,
    translationState: com.cinetrack.ui.viewmodel.MovieDetailViewModel.TranslationState?,
    onTranslateRequest: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showOriginal by remember { mutableStateOf(false) }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val expandedWidth = (configuration.screenWidthDp * 0.85f).dp
    val targetWidth = if (isExpanded) expandedWidth else 280.dp

    Box(
        modifier = Modifier
            .width(targetWidth)
            .animateContentSize(animationSpec = tween(400, easing = FastOutSlowInEasing))
            .heightIn(min = 140.dp, max = 340.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
            .bounceClick(scaleDown = 0.98f) { isExpanded = !isExpanded }
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val username = comment.user?.name?.takeIf { it.isNotBlank() } ?: comment.user?.username ?: "Utente Anonimo"
                Text(
                    text = username,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (translationState?.isTranslating == true) {
                        CircularProgressIndicator(
                            color = accentColor,
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        val hasTranslation = translationState?.translatedText != null
                        val currentLanguage = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]?.language ?: "en"
                        if (currentLanguage != "en") {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_traduzione),
                                contentDescription = "Traduci",
                                tint = if (hasTranslation && !showOriginal) accentColor else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(16.dp)
                                .bounceClick(scaleDown = 0.9f) {
                                    if (hasTranslation) {
                                        showOriginal = !showOriginal
                                    } else {
                                        onTranslateRequest()
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_star),
                        contentDescription = "Likes",
                        tint = accentColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${comment.likes}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val scrollState = rememberScrollState()
            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        // Consuma tutto lo scroll rimanente in modo che non passi al parent
                        return available
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        val topColor = if (scrollState.canScrollBackward) Color.Transparent else Color.Black
                        val bottomColor = if (scrollState.canScrollForward) Color.Transparent else Color.Black
                        
                        if (scrollState.canScrollBackward || scrollState.canScrollForward) {
                            drawRect(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    0f to topColor,
                                    0.1f to Color.Black,
                                    0.9f to Color.Black,
                                    1f to bottomColor
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                    }
            ) {
                val displayedText = if (translationState?.translatedText != null && !showOriginal) {
                    translationState.translatedText
                } else {
                    comment.comment ?: ""
                }
                
                Column(
                    modifier = Modifier
                        .nestedScroll(nestedScrollConnection)
                        .verticalScroll(scrollState)
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = displayedText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 18.sp,
                            fontSize = 13.sp
                        ),
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // La selezione tra testo originale e tradotto ora è gestita tramite l'icona in alto a destra
                }
            }
        }
    }
}
