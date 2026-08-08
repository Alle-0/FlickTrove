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
import com.cinetrack.data.model.AppComment
import com.google.firebase.auth.FirebaseAuth

@Composable
fun DetailComments(
    comments: List<AppComment>,
    accentColor: Color,
    onOpenThread: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TOP COMMENTS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp
                ),
                color = Color.White.copy(alpha = 0.5f)
            )
            
            Text(
                text = "Scrivi",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = accentColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenThread() }
                    .background(accentColor.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        val sortedComments = remember(comments) {
            comments.filter { it.depth == 0 }.sortedByDescending { it.likesCount }.take(10)
        }

        if (comments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .clickable { onOpenThread() }
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nessun commento. Sii il primo a scriverne uno!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
            items(sortedComments, key = { it.id }, contentType = { "comment" }) { comment ->
                CommentCard(
                    comment = comment, 
                    accentColor = accentColor
                )
            }
            item {
                Box(
                    modifier = Modifier
                        .height(140.dp)
                        .padding(start = 8.dp)
                        .clickable { onOpenThread() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Vedi discussione completa ->",
                        color = accentColor,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun CommentCard(
    comment: AppComment,
    accentColor: Color
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
                val username = comment.userDisplayName.takeIf { it.isNotBlank() } ?: "Utente Anonimo"
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
                    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid }
                    val isLiked = currentUserId != null && comment.likedBy.contains(currentUserId)

                    Icon(
                        imageVector = ImageVector.vectorResource(id = if (isLiked) R.drawable.ic_star_piena else R.drawable.ic_star),
                        contentDescription = "Likes",
                        tint = if (isLiked) accentColor else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${comment.likesCount}",
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
                val displayedText = comment.text
                
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
