package com.cinetrack.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import coil.imageLoader
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.parseSimpleMarkdown
import com.cinetrack.ui.utils.MarkdownVisualTransformation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import com.cinetrack.ui.components.shared.FlickTroveModal
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
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
import com.cinetrack.ui.components.detail.DetailTranslationPromptModal
import com.cinetrack.ui.components.comments.CommentInputBar
import com.cinetrack.ui.components.comments.CommentReportModal
import com.cinetrack.ui.components.comments.CommentDeleteModal
import com.cinetrack.ui.components.comments.CommentCardItem
import com.cinetrack.ui.components.comments.LiquidStarIcon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.util.lerp
import com.cinetrack.ui.utils.premiumScrollbar
import com.cinetrack.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.cinetrack.ui.components.glass.hazeGlass
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.drawscope.clipPath
import com.cinetrack.data.model.CommentSortOption
import com.cinetrack.data.model.CommentSortOrder

typealias CommentSortOption = com.cinetrack.data.model.CommentSortOption
typealias CommentSortOrder = com.cinetrack.data.model.CommentSortOrder

class CommentsScreen(
    private val mediaId: String,
    private val mediaType: String,
    private val accentColorValue: Long,
    private val mediaTitle: String = "",
    private val mediaImage: String? = null,
    private val focusInputOnLaunch: Boolean = false,
    private val targetCommentId: String? = null
) : Screen {

    override val key: String = "CommentsScreen_${mediaType}_${mediaId}_${java.util.UUID.randomUUID()}"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = getViewModel<CommentsViewModel>()
        
        val localCtx = androidx.compose.ui.platform.LocalContext.current
        var currentContext = localCtx
        while (currentContext is android.content.ContextWrapper && currentContext !is androidx.activity.ComponentActivity) {
            currentContext = currentContext.baseContext
        }
        val activity = currentContext as? androidx.activity.ComponentActivity
        val settingsViewModel = if (activity != null) {
            androidx.hilt.navigation.compose.hiltViewModel<com.cinetrack.ui.viewmodel.SettingsViewModel>(activity)
        } else {
            getViewModel<com.cinetrack.ui.viewmodel.SettingsViewModel>()
        }
        
        LaunchedEffect(mediaId, mediaType) {
            viewModel.init(mediaId, mediaType)
        }

        val comments by viewModel.comments.collectAsStateWithLifecycle()
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
        val hasMoreComments by viewModel.hasMoreComments.collectAsStateWithLifecycle()
        val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()
        val translationStates by viewModel.translationStates.collectAsStateWithLifecycle()
        val showTranslationPrompt by viewModel.showTranslationPrompt.collectAsStateWithLifecycle()
        val isUserBanned by viewModel.isUserBanned.collectAsStateWithLifecycle()
        val banExpiration by viewModel.banExpiration.collectAsStateWithLifecycle()
        val accentColor = Color(accentColorValue.toULong())

        var replyingTo by remember { mutableStateOf<AppComment?>(null) }
        var inputText by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
        var isSpoiler by remember { mutableStateOf(false) }
        var isInputExpanded by remember { mutableStateOf(false) }
        var isMarkdownMenuExpanded by remember { mutableStateOf(false) }
        var commentToReport by remember { mutableStateOf<AppComment?>(null) }
        var commentToDelete by remember { mutableStateOf<AppComment?>(null) }
        var sortOption by remember { mutableStateOf(CommentSortOption.DATE) }
        var sortOrder by remember { mutableStateOf(CommentSortOrder.DESC) }
        var showSortMenu by remember { mutableStateOf(false) }
        var sortButtonBounds by remember { mutableStateOf<Rect?>(null) }

        LaunchedEffect(sortOption, sortOrder) {
            viewModel.setSort(sortOption, sortOrder)
        }
        val localFocusManager = LocalFocusManager.current
        val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
        val context = androidx.compose.ui.platform.LocalContext.current
        val hazeState = remember { HazeState() }
        val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
        var isUploadingImage by remember { mutableStateOf(false) }
        var attachedMedia by remember { mutableStateOf(emptyList<String>()) }
        val imageLoader = context.imageLoader
        
        val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
            onResult = { uri ->
                if (uri != null) {
                    isUploadingImage = true
                    viewModel.uploadCommentImage(
                        imageUri = uri,
                        onSuccess = { url ->
                            isUploadingImage = false
                            attachedMedia = listOf(url.toString())
                        },
                        onError = { error ->
                            isUploadingImage = false
                            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        )

        LaunchedEffect(Unit) {
            if (focusInputOnLaunch) {
                kotlinx.coroutines.delay(300) // Aspetta fine animazione navigazione
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }

        val movieColor = if (accentColorValue != 0L) Color(accentColorValue) else Color.Transparent

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
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
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                        .hazeChild(state = hazeState, shape = androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp), style = dev.chrisbanes.haze.HazeStyle(tint = Color.Black.copy(alpha = 0.7f), blurRadius = 20.dp)),
                    title = { Text(if (mediaTitle.isNotBlank()) stringResource(R.string.comments_screen_title_with_media, mediaTitle) else stringResource(R.string.comments_screen_title), fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1) },
                    navigationIcon = {
                        Box(
                            modifier = Modifier
                                .padding(12.dp)
                                .bounceClick { navigator.pop() }
                        ) {
                            Icon(painter = painterResource(id = R.drawable.ic_left), contentDescription = stringResource(R.string.detail_content_desc_back), tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    },
                    windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    actions = {
                        val sortCoords = remember { arrayOf<LayoutCoordinates?>(null) }
                        Box {
                            Box(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .onGloballyPositioned { sortCoords[0] = it }
                                    .bounceClick { 
                                        sortButtonBounds = sortCoords[0]?.let {
                                            val pos = it.positionInWindow()
                                            Rect(pos.x, pos.y, pos.x + it.size.width, pos.y + it.size.height)
                                        }
                                        showSortMenu = true 
                                    }
                            ) {
                                Icon(painterResource(id = R.drawable.ic_filtri), contentDescription = stringResource(R.string.comment_sort_by), tint = Color.White)
                            }
                        }
                    }
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                val expandedComments = remember { mutableStateOf(setOf<String>()) }
                var hasScrolledToTarget by remember { mutableStateOf(false) }
                val listState = rememberLazyListState()

                LaunchedEffect(comments) {
                    if (targetCommentId != null && !hasScrolledToTarget && comments.isNotEmpty()) {
                        var current = comments.find { it.id == targetCommentId }
                        val toExpand = mutableSetOf<String>()
                        while (current?.parentId != null) {
                            toExpand.add(current.parentId!!)
                            current = comments.find { it.id == current!!.parentId }
                        }
                        if (toExpand.isNotEmpty()) {
                            expandedComments.value = expandedComments.value + toExpand
                        }
                    }
                }

                val flatTree = remember(comments, expandedComments.value, sortOption, sortOrder) { 
                    buildFlatTree(comments, expandedComments.value, sortOption, sortOrder) 
                }

                LaunchedEffect(flatTree) {
                    if (targetCommentId != null && !hasScrolledToTarget && flatTree.isNotEmpty()) {
                        val index = flatTree.indexOfFirst { it.id == targetCommentId }
                        if (index != -1) {
                            kotlinx.coroutines.delay(300)
                            listState.animateScrollToItem(index)
                            hasScrolledToTarget = true
                        }
                    }
                }

                LazyColumn(
                    state = listState,
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
                            val isExpanded = expandedComments.value.contains(comment.id)
                            CommentCardItem(
                                comment = comment,
                                index = index,
                                flatTree = flatTree,
                                allComments = comments,
                                isLiked = isLiked,
                                translationState = translationStates[comment.id],
                                currentUserId = viewModel.currentUserId,
                                isUserAnonymous = viewModel.isUserAnonymous,
                                accentColor = accentColor,
                                isExpanded = isExpanded,
                                onToggleExpand = {
                                    val willExpand = !isExpanded
                                    if (willExpand) {
                                        viewModel.loadRepliesForComment(comment.id)
                                    }
                                    expandedComments.value = if (isExpanded) {
                                        expandedComments.value - comment.id
                                    } else {
                                        expandedComments.value + comment.id
                                    }
                                },
                                imageLoader = imageLoader,
                                onToggleSpoilerAuthor = {
                                    viewModel.toggleSpoilerStatus(comment.id, comment.isSpoiler)
                                },
                                onDeleteComment = {
                                    commentToDelete = comment
                                },
                                onReply = {
                                    replyingTo = comment
                                },
                                onToggleLike = {
                                    viewModel.toggleLikeComment(comment.id, mediaTitle, mediaImage)
                                },
                                onReport = {
                                    commentToReport = comment
                                },
                                onTranslate = { text ->
                                    viewModel.translateComment(comment.id, text)
                                },
                                onTriggerGuestAuth = {
                                    settingsViewModel.triggerGuestAuthDialog()
                                }
                            )
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
                                        androidx.compose.material3.CircularProgressIndicator(color = accentColor, modifier = Modifier.size(24.dp))
                                    } else {
                                        TextButton(onClick = { viewModel.loadMoreComments() }) {
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

                // Input Area
                CommentInputBar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    inputText = inputText,
                    onInputTextChanged = { inputText = it },
                    replyingTo = replyingTo,
                    onCancelReply = { replyingTo = null },
                    isUserBanned = isUserBanned,
                    banExpiration = banExpiration,
                    isUserAnonymous = viewModel.isUserAnonymous,
                    onGuestAuthTrigger = { settingsViewModel.triggerGuestAuthDialog() },
                    isSpoiler = isSpoiler,
                    onSpoilerChanged = { isSpoiler = it },
                    attachedMedia = attachedMedia,
                    onAttachedMediaChanged = { attachedMedia = it },
                    isUploadingImage = isUploadingImage,
                    onPickImage = {
                        photoPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onSendComment = {
                        if (inputText.text.isNotBlank() || attachedMedia.isNotEmpty()) {
                            val pId = replyingTo?.id
                            val pUserId = replyingTo?.userId
                            val newDepth = (replyingTo?.depth ?: -1) + 1

                            // Auto-expand the parent comment so the new reply is visible immediately
                            if (pId != null) {
                                expandedComments.value = expandedComments.value + pId
                            }

                            var finalMessage = inputText.text.trim()
                            if (attachedMedia.isNotEmpty()) {
                                if (finalMessage.isNotEmpty()) finalMessage += "\n\n"
                                finalMessage += attachedMedia.joinToString("\n") { url ->
                                    if (url.contains("giphy.com")) "![gif]($url)" else "![foto]($url)"
                                }
                            }

                            localFocusManager.clearFocus()
                            viewModel.addComment(finalMessage, isSpoiler, pId, pUserId, newDepth, mediaTitle, mediaImage)
                            inputText = androidx.compose.ui.text.input.TextFieldValue("")
                            attachedMedia = emptyList()
                            replyingTo = null
                            isSpoiler = false
                        }
                    },
                    accentColor = accentColor,
                    hazeState = hazeState,
                    focusRequester = focusRequester
                )
            }
        } // End of Scaffold
            
            // Sort Dialog Overlay
            // Sort Menu using HomeFilterModal for consistency
            com.cinetrack.ui.components.dialog.HomeFilterModal(
                isVisible = showSortMenu,
                isCommentsFilter = true,
                triggerBounds = sortButtonBounds,
                sortConfig = com.cinetrack.data.model.SortConfig(
                    sortType = if (sortOption == CommentSortOption.DATE) "date" else "likes",
                    sortDirection = if (sortOrder == CommentSortOrder.DESC) "desc" else "asc"
                ),
                hazeState = hazeState,
                onSortConfigChanged = { newConfig ->
                    sortOption = if (newConfig.sortType == "date") CommentSortOption.DATE else CommentSortOption.LIKES
                    sortOrder = if (newConfig.sortDirection == "desc") CommentSortOrder.DESC else CommentSortOrder.ASC
                },
                onDismissRequest = { showSortMenu = false }
            )

            // Translation Prompt Dialog
            DetailTranslationPromptModal(
                showTranslationPrompt = showTranslationPrompt,
                onDismiss = { viewModel.dismissTranslationPrompt() },
                onTranslate = { commentId, text, requireWifi ->
                    viewModel.translateComment(commentId, text, requireWifi)
                },
                hazeState = hazeState,
                accentColor = accentColor
            )

            // Report Dialog Overlay
            CommentReportModal(
                comment = commentToReport,
                onDismiss = { commentToReport = null },
                onReport = { category ->
                    commentToReport?.let { c ->
                        viewModel.reportComment(c.id, category, c.text, c.userId, c.userDisplayName)
                    }
                    commentToReport = null
                },
                hazeState = hazeState
            )

            // Delete Dialog Overlay
            CommentDeleteModal(
                comment = commentToDelete,
                onDismiss = { commentToDelete = null },
                onConfirm = {
                    commentToDelete?.let { c ->
                        viewModel.deleteComment(c.id)
                    }
                    commentToDelete = null
                },
                hazeState = hazeState
            )
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

    private fun buildFlatTree(
        comments: List<AppComment>, 
        expandedComments: Set<String>,
        sortOption: CommentSortOption,
        sortOrder: CommentSortOrder
    ): List<AppComment> {
        val tree = mutableListOf<AppComment>()
        val map = comments.groupBy { it.parentId }

        fun addChildren(parentId: String?) {
            val children = map[parentId]?.let { list ->
                when (sortOption) {
                    CommentSortOption.DATE -> {
                        if (sortOrder == CommentSortOrder.ASC) list.sortedBy { it.createdAt?.seconds ?: 0L }
                        else list.sortedByDescending { it.createdAt?.seconds ?: 0L }
                    }
                    CommentSortOption.LIKES -> {
                        if (sortOrder == CommentSortOrder.ASC) list.sortedWith(compareBy({ it.likesCount }, { it.createdAt?.seconds ?: 0L }))
                        else list.sortedWith(compareByDescending<AppComment> { it.likesCount }.thenByDescending { it.createdAt?.seconds ?: 0L })
                    }
                }
            } ?: return
            
            for (child in children) {
                tree.add(child)
                if (expandedComments.contains(child.id)) {
                    addChildren(child.id)
                }
            }
        }

        addChildren(null)
        return tree
    }
}
