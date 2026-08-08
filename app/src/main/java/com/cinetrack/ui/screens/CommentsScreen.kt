package com.cinetrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil.compose.AsyncImage
import com.cinetrack.data.model.AppComment
import com.cinetrack.ui.viewmodel.CommentsViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.res.painterResource
import com.cinetrack.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import com.cinetrack.ui.components.glass.hazeGlass
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.rememberLazyListState

class CommentsScreen(
    private val mediaId: String,
    private val mediaType: String,
    private val accentColorValue: Long,
    private val mediaTitle: String = ""
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = getViewModel<CommentsViewModel>()
        
        LaunchedEffect(mediaId, mediaType) {
            viewModel.init(mediaId, mediaType)
        }

        val comments by viewModel.comments.collectAsStateWithLifecycle()
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
        val accentColor = Color(accentColorValue.toULong())

        var replyingTo by remember { mutableStateOf<AppComment?>(null) }
        var inputText by remember { mutableStateOf("") }
        var isSpoiler by remember { mutableStateOf(false) }
        var commentToReport by remember { mutableStateOf<AppComment?>(null) }
        val listState = rememberLazyListState()
        val localFocusManager = LocalFocusManager.current
        val hazeState = remember { HazeState() }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        localFocusManager.clearFocus()
                    })
                },
            topBar = {
                TopAppBar(
                    modifier = Modifier.hazeChild(state = hazeState, style = dev.chrisbanes.haze.HazeStyle(tint = Color(0xFF121212).copy(alpha = 0.5f), blurRadius = 15.dp)),
                    title = { Text(if (mediaTitle.isNotBlank()) "Discussione: $mediaTitle" else "Discussione", fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(painter = painterResource(id = R.drawable.ic_left), contentDescription = "Indietro", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    },
                    windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color(0xFF121212)
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                val flatTree = remember(comments) { buildFlatTree(comments) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(hazeState),
                    contentPadding = PaddingValues(top = paddingValues.calculateTopPadding() + 16.dp, bottom = 140.dp)
                ) {
                    if (isLoading) {
                        items(5) {
                            SkeletonCommentItem()
                        }
                    } else {
                        itemsIndexed(flatTree, key = { _, it -> it.id }) { index, comment ->
                            val isLiked = viewModel.currentUserId != null && comment.likedBy.contains(viewModel.currentUserId)
                            val visualDepth = minOf(comment.depth, 3)
                            val indentSpacing = 32.dp
                            val baseStartPadding = 16.dp
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                .drawBehind {
                                    val startX = (baseStartPadding + 18.dp).toPx() // center of first avatar
                                    val spacingPx = indentSpacing.toPx()
                                    val avatarCenterY = (12.dp + 18.dp).toPx() // top padding + half avatar
                                    
                                    for (i in 0 until visualDepth) {
                                        val xPos = startX + (i * spacingPx)
                                        
                                        var endY = size.height
                                        if (i == visualDepth - 1) {
                                            // Check if this is the last sibling
                                            // It's the last sibling if there is no subsequent item with the SAME depth, 
                                            // before we encounter an item with a SMALLER depth.
                                            var isLastSibling = true
                                            for (j in (index + 1) until flatTree.size) {
                                                if (flatTree[j].depth < comment.depth) break
                                                if (flatTree[j].depth == comment.depth) {
                                                    isLastSibling = false
                                                    break
                                                }
                                            }
                                            if (isLastSibling) {
                                                endY = avatarCenterY
                                            }
                                            
                                            // Horizontal branch
                                            drawLine(
                                                color = Color.White.copy(alpha = 0.15f),
                                                start = Offset(xPos, avatarCenterY),
                                                end = Offset(xPos + 12.dp.toPx(), avatarCenterY),
                                                strokeWidth = 3f
                                            )
                                        }
                                        
                                        drawLine(
                                            color = Color.White.copy(alpha = 0.15f),
                                            start = Offset(xPos, 0f),
                                            end = Offset(xPos, endY),
                                            strokeWidth = 3f
                                        )
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
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = comment.userDisplayName.ifBlank { "Anonimo" },
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (comment.isSpoiler) {
                                        Text(
                                            text = "SPOILER",
                                            color = Color.Red,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                            modifier = Modifier
                                                .background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
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
                                        append(comment.text)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "Rispondi",
                                        color = accentColor,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.clickable { replyingTo = comment }
                                    )
                                    Row(
                                        modifier = Modifier.clickable { viewModel.toggleLikeComment(comment.id) },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(id = if (isLiked) R.drawable.ic_star_piena else R.drawable.ic_star),
                                            contentDescription = "Like",
                                            tint = if (isLiked) accentColor else Color.White.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${comment.likesCount}",
                                            color = if (isLiked) accentColor else Color.White.copy(alpha = 0.5f),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                    
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_flag),
                                        contentDescription = "Segnala",
                                        tint = Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { commentToReport = comment }
                                    )
                                }
                            }
                        }
                    }
                }
                }

                // Input Area
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding() // Prevent overlap with system gesture bar
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .hazeChild(
                            state = hazeState,
                            shape = RoundedCornerShape(32.dp),
                            style = dev.chrisbanes.haze.HazeStyle(tint = Color(0xFF1E1E1E).copy(alpha = 0.85f), blurRadius = 15.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                    if (replyingTo != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = buildAnnotatedString {
                                        append("Risposta a ")
                                        withStyle(style = SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) {
                                            append(replyingTo!!.userDisplayName)
                                        }
                                    },
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = "\"${replyingTo!!.text}\"",
                                    color = Color.White.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Annulla",
                                color = Color.White.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.clickable { replyingTo = null }
                            )
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Scrivi un commento...", color = Color.White.copy(alpha = 0.4f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp),
                            maxLines = 4
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    val pId = replyingTo?.id
                                    val pUserId = replyingTo?.userId
                                    val newDepth = (replyingTo?.depth ?: -1) + 1
                                    viewModel.addComment(inputText, isSpoiler, pId, pUserId, newDepth)
                                    inputText = ""
                                    replyingTo = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text("Invia", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Report Dialog Overlay
        commentToReport?.let { comment ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .pointerInput(Unit) { detectTapGestures(onTap = { commentToReport = null }) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .hazeGlass(
                            state = hazeState,
                            shape = RoundedCornerShape(24.dp),
                            containerColor = Color(0xFF1E1E1E).copy(alpha = 0.5f)
                        )
                        .padding(24.dp)
                        .pointerInput(Unit) { detectTapGestures {} }
                ) {
                    Column {
                        Text("Segnala", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Cosa desideri segnalare?", color = Color.White.copy(0.8f))
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = {
                                viewModel.reportComment(comment.id, "Segnalazione Utente: ${comment.userId}", comment.text)
                                commentToReport = null
                            }) {
                                Text("Segnala Nome o Avatar Utente", color = Color.Red)
                            }
                            TextButton(onClick = {
                                viewModel.reportComment(comment.id, "Segnalazione Commento", comment.text)
                                commentToReport = null
                            }) {
                                Text("Segnala Commento", color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
        } // End of Scaffold
        } // End of Outer Box
    }

    @Composable
    private fun SkeletonCommentItem() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.height(14.dp).fillMaxWidth(0.3f).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.height(14.dp).fillMaxWidth(0.8f).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.height(14.dp).fillMaxWidth(0.5f).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
            }
        }
    }

    private fun buildFlatTree(comments: List<AppComment>): List<AppComment> {
        val tree = mutableListOf<AppComment>()
        val map = comments.groupBy { it.parentId }

        fun addChildren(parentId: String?) {
            val children = map[parentId]?.sortedBy { it.createdAt } ?: return
            for (child in children) {
                tree.add(child)
                addChildren(child.id)
            }
        }

        addChildren(null)
        return tree
    }
}
