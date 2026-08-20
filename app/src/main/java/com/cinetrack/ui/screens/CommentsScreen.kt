package com.cinetrack.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.cinetrack.ui.utils.bounceClick
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
        val imageLoader = remember {
            coil.ImageLoader.Builder(context)
                .components {
                    if (android.os.Build.VERSION.SDK_INT >= 28) {
                        add(coil.decode.ImageDecoderDecoder.Factory())
                    } else {
                        add(coil.decode.GifDecoder.Factory())
                    }
                }
                .build()
        }
        
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
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
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
                            val visualDepth = minOf(comment.depth, 3)
                            val indentSpacing = 32.dp
                            val baseStartPadding = 16.dp
                            
                            val parentExpanded = if (comment.parentId != null) expandedComments.value.contains(comment.parentId) else true
                            var hasAppeared by androidx.compose.runtime.saveable.rememberSaveable(parentExpanded) { mutableStateOf(false) }
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
                                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                                )
                                                // Curved horizontal branch
                                                val cornerRadius = 12.dp.toPx()
                                                val path = androidx.compose.ui.graphics.Path().apply {
                                                    moveTo(xPos, avatarCenterY - cornerRadius)
                                                    quadraticTo(xPos, avatarCenterY, xPos + cornerRadius, avatarCenterY)
                                                    lineTo(xPos + 12.dp.toPx(), avatarCenterY)
                                                }
                                                drawPath(
                                                    path = path,
                                                    color = Color.White.copy(alpha = 0.15f),
                                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                                                )
                                            } else {
                                                // L-Shape
                                                val cornerRadius = 16.dp.toPx()
                                                val path = androidx.compose.ui.graphics.Path().apply {
                                                    moveTo(xPos, 0f)
                                                    lineTo(xPos, avatarCenterY - cornerRadius)
                                                    quadraticTo(xPos, avatarCenterY, xPos + cornerRadius, avatarCenterY)
                                                    lineTo(xPos + 12.dp.toPx(), avatarCenterY)
                                                }
                                                drawPath(
                                                    path = path,
                                                    color = Color.White.copy(alpha = 0.15f),
                                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
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
                                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
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
                                val isEffectivelyDeleted = comment.isDeleted || comment.userId.isBlank() || (comment.text.isBlank() && comment.userDisplayName.isBlank())
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (isEffectivelyDeleted) stringResource(R.string.comment_deleted) else comment.userDisplayName.ifBlank { "Anonimo" },
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    if (comment.userId == viewModel.currentUserId && !isEffectivelyDeleted && comment.createdAt != null) {
                                        val timeSinceCreated = System.currentTimeMillis() - comment.createdAt.toDate().time
                                        if (timeSinceCreated <= 12 * 60 * 60 * 1000) {
                                            Icon(
                                                painter = painterResource(id = if (comment.isSpoiler) R.drawable.ic_eye_off else R.drawable.ic_eye),
                                                contentDescription = "Toggle Spoiler",
                                                tint = if (comment.isSpoiler) accentColor else Color.White.copy(alpha = 0.5f),
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .bounceClick { viewModel.toggleSpoilerStatus(comment.id, comment.isSpoiler) }
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                        }
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_trash),
                                            contentDescription = "Elimina",
                                            tint = Color.White.copy(alpha = 0.5f),
                                            modifier = Modifier
                                                .size(14.dp)
                                                .bounceClick { commentToDelete = comment }
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

                                val currentTranslationState = translationStates[comment.id]
                                val displayedTextRaw = when {
                                    isEffectivelyDeleted -> stringResource(R.string.comment_deleted)
                                    currentTranslationState is com.cinetrack.ui.viewmodel.CommentsViewModel.TranslationState.Translated -> currentTranslationState.text
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
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
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
                                                coil.compose.AsyncImage(
                                                    model = coil.request.ImageRequest.Builder(context)
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
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                            }
                                        }
                                    }
                                }

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
                                                .clip(androidx.compose.foundation.shape.GenericShape { size, _ ->
                                                    addOval(androidx.compose.ui.geometry.Rect(
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
                                                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
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
                                                if (viewModel.isUserAnonymous) settingsViewModel.triggerGuestAuthDialog()
                                                else replyingTo = comment 
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Row(
                                            modifier = Modifier.bounceClick {
                                                if (viewModel.isUserAnonymous) settingsViewModel.triggerGuestAuthDialog()
                                                else viewModel.toggleLikeComment(comment.id, mediaTitle, mediaImage) 
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
                                        if (comment.userId != viewModel.currentUserId) {
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_flag),
                                                contentDescription = stringResource(R.string.comment_report_title),
                                                tint = Color.Red,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .bounceClick { 
                                                        if (viewModel.isUserAnonymous) settingsViewModel.triggerGuestAuthDialog()
                                                        else commentToReport = comment 
                                                    }
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.weight(1f))
                                        
                                        // Translate button
                                        val currentTranslationStateAction = translationStates[comment.id]
                                        when (currentTranslationStateAction) {
                                            is com.cinetrack.ui.viewmodel.CommentsViewModel.TranslationState.Downloading,
                                            is com.cinetrack.ui.viewmodel.CommentsViewModel.TranslationState.Translating -> {
                                                androidx.compose.material3.CircularProgressIndicator(
                                                    modifier = Modifier.size(12.dp),
                                                    strokeWidth = 1.5.dp,
                                                    color = accentColor
                                                )
                                                Spacer(modifier = Modifier.width(16.dp))
                                            }
                                            is com.cinetrack.ui.viewmodel.CommentsViewModel.TranslationState.Translated -> {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_traduzione),
                                                    contentDescription = stringResource(R.string.comment_show_original),
                                                    tint = accentColor,
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .bounceClick { viewModel.translateComment(comment.id, comment.text) }
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
                                                        .bounceClick { viewModel.translateComment(comment.id, comment.text) }
                                                )
                                                Spacer(modifier = Modifier.width(16.dp))
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                    
                                    if (comment.createdAt != null) {
                                        val context = LocalContext.current
                                        val timeStr = remember(comment.createdAt, context) {
                                            val date = comment.createdAt.toDate()
                                            val diff = System.currentTimeMillis() - date.time
                                            val hours = diff / (1000 * 60 * 60)
                                            val minutes = diff / (1000 * 60)
                                            when {
                                                minutes < 1 -> context.getString(R.string.comment_time_now)
                                                minutes < 60 -> context.getString(R.string.comment_time_mins_ago, minutes.toInt())
                                                hours < 24 -> context.getString(R.string.comment_time_hours_ago, hours.toInt())
                                                else -> java.text.SimpleDateFormat("dd MMM yy", java.util.Locale.getDefault()).format(date)
                                            }
                                        }
                                        Text(
                                            text = timeStr,
                                            color = Color.White.copy(alpha = 0.3f),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                
                                val childrenCount = maxOf(comment.repliesCount, comments.count { it.parentId == comment.id })
                                if (childrenCount > 0) {
                                    val isExpanded = expandedComments.value.contains(comment.id)
                                    Row(
                                        modifier = Modifier
                                            .padding(top = 8.dp)
                                            .bounceClick {
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
                            } // Close Row
                            } // Close AnimatedVisibility
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
                val topCornerRadius by animateDpAsState(
                    targetValue = if (replyingTo != null) 24.dp else 50.dp,
                    label = "topCornerRadius"
                )
                val boxShape = RoundedCornerShape(
                    topStart = topCornerRadius,
                    topEnd = topCornerRadius,
                    bottomStart = 50.dp,
                    bottomEnd = 50.dp
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding() // Prevent overlap with system gesture bar
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clip(boxShape)
                        .hazeChild(
                            state = hazeState,
                            shape = boxShape,
                            style = dev.chrisbanes.haze.HazeStyle(tint = Color(0xFF1E1E1E).copy(alpha = 0.85f), blurRadius = 15.dp)
                        )
                        .animateContentSize(alignment = Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                    val lastReplyingTo = remember { mutableStateOf(replyingTo) }
                    if (replyingTo != null) {
                        lastReplyingTo.value = replyingTo
                    }

                    AnimatedVisibility(
                        visible = replyingTo != null,
                        enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                        exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
                    ) {
                        lastReplyingTo.value?.let { replyTarget ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = buildAnnotatedString {
                                            append(stringResource(R.string.comment_replying_to) + " ")
                                            withStyle(style = SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) {
                                                append(replyTarget.userDisplayName)
                                            }
                                        },
                                        color = Color.White.copy(alpha = 0.8f),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    val previewMediaRegex = Regex("!\\[(?:gif|foto)\\]\\((.*?)\\)")
                                    val cleanText = replyTarget.text.replace(previewMediaRegex, "").trim()
                                    val previewText = if (cleanText.isEmpty() && previewMediaRegex.containsMatchIn(replyTarget.text)) {
                                        stringResource(R.string.comment_image_preview)
                                    } else {
                                        cleanText
                                    }
                                    Text(
                                        text = "\"$previewText\"",
                                        color = Color.White.copy(alpha = 0.6f),
                                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.comment_cancel_btn),
                                    color = Color.White.copy(0.7f),
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.bounceClick { replyingTo = null }
                                )
                            }
                        }
                    }
                    
                    if (isUserBanned) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                                .background(Color(0xFF330000), RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = banExpiration?.let { stringResource(R.string.comment_banned_temporary, it) } ?: stringResource(R.string.comment_banned_permanent),
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val maxChar = 4000
                            val inputScrollState = rememberScrollState()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(max = 120.dp)
                                    .border(1.dp, if (isInputExpanded) accentColor else Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                                    .premiumScrollbar(inputScrollState, width = 3f, paddingEnd = 6f)
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null
                                    ) { focusRequester.requestFocus() }
                                    .padding(horizontal = 16.dp, vertical = 15.dp),
                                contentAlignment = Alignment.TopStart
                            ) {
                                androidx.compose.foundation.text.BasicTextField(
                                    value = inputText,
                                    enabled = !viewModel.isUserAnonymous,
                                    onValueChange = { 
                                        if (it.text.length <= maxChar) {
                                            inputText = it 
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                        .onFocusChanged { focusState -> isInputExpanded = focusState.isFocused }
                                        .verticalScroll(inputScrollState),
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                                    cursorBrush = androidx.compose.ui.graphics.SolidColor(accentColor),
                                    visualTransformation = remember { MarkdownVisualTransformation() }
                                )
                                if (inputText.text.isEmpty()) {
                                    Text(stringResource(R.string.comment_input_hint), color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .bounceClick {
                                        if (viewModel.isUserAnonymous) {
                                            settingsViewModel.triggerGuestAuthDialog()
                                        } else if (inputText.text.isNotBlank() || attachedMedia.isNotEmpty()) {
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
                                    }
                                    .background(accentColor, RoundedCornerShape(50.dp))
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(stringResource(R.string.comment_send_btn), color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (inputText.text.isNotEmpty()) {
                            val maxChar = 4000
                            Text(
                                text = "${inputText.text.length}/$maxChar",
                                color = if (inputText.text.length == maxChar) Color.Red.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.3f),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, end = 100.dp), // Aggiunto end padding per allinearlo sotto l'input field e non sotto il pulsante
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        }
                        val isKeyboardOpen = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
                        AnimatedVisibility(visible = isInputExpanded || attachedMedia.isNotEmpty() || inputText.text.isNotEmpty() || isKeyboardOpen) {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                if (attachedMedia.isNotEmpty()) {
                                    androidx.compose.foundation.lazy.LazyRow(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(attachedMedia.size) { index ->
                                            val url = attachedMedia[index]
                                            Box(modifier = Modifier.size(64.dp)) {
                                                coil.compose.AsyncImage(
                                                    model = coil.request.ImageRequest.Builder(context).data(url).build(),
                                                    imageLoader = imageLoader,
                                                    contentDescription = "Attachment",
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(4.dp)
                                                        .size(20.dp)
                                                        .background(Color.Black.copy(alpha = 0.6f), androidx.compose.foundation.shape.CircleShape)
                                                        .bounceClick { attachedMedia = attachedMedia.filter { it != url } },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_x), 
                                                        contentDescription = "Remove", 
                                                        tint = Color.White, 
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Spoiler switch
                                    Text(stringResource(R.string.comment_spoiler_toggle), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    androidx.compose.material3.Switch(
                                        checked = isSpoiler,
                                        onCheckedChange = { isSpoiler = it },
                                        colors = androidx.compose.material3.SwitchDefaults.colors(
                                            checkedThumbColor = accentColor,
                                            checkedTrackColor = accentColor.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier.scale(0.8f)
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = { 
                                            val fragmentManager = context.findFragmentActivity()?.supportFragmentManager
                                            if (fragmentManager != null) {
                                                val settings = com.giphy.sdk.ui.GPHSettings(
                                                    theme = com.giphy.sdk.ui.themes.GPHTheme.Dark,
                                                    mediaTypeConfig = arrayOf(com.giphy.sdk.ui.GPHContentType.gif)
                                                )
                                                val dialog = com.giphy.sdk.ui.views.GiphyDialogFragment.newInstance(settings)
                                                dialog.gifSelectionListener = object : com.giphy.sdk.ui.views.GiphyDialogFragment.GifSelectionListener {
                                                    override fun didSearchTerm(term: String) {}
                                                    override fun onDismissed(selectedContentType: com.giphy.sdk.ui.GPHContentType) {}
                                                    override fun onGifSelected(media: com.giphy.sdk.core.models.Media, searchTerm: String?, selectedContentType: com.giphy.sdk.ui.GPHContentType) {
                                                        val gifUrl = media.images.fixedHeight?.gifUrl ?: media.images.original?.gifUrl ?: ""
                                                        if (gifUrl.isNotEmpty()) {
                                                            attachedMedia = listOf(gifUrl)
                                                        }
                                                        dialog.dismiss()
                                                    }
                                                }
                                                dialog.show(fragmentManager, "giphy_dialog")
                                            } else {
                                                android.widget.Toast.makeText(context, "Fragment manager not found", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        enabled = attachedMedia.isEmpty()
                                    ) {
                                        val gifAlpha = if (attachedMedia.isEmpty()) 0.8f else 0.3f
                                        Icon(painterResource(id = R.drawable.ic_gif), contentDescription = "GIF", tint = Color.White.copy(alpha = gifAlpha), modifier = Modifier.size(20.dp))
                                    }
                                    // Foto
                                    IconButton(
                                        onClick = { 
                                            if (isUploadingImage) return@IconButton
                                            photoPickerLauncher.launch(
                                                androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        enabled = attachedMedia.isEmpty() && !isUploadingImage
                                    ) {
                                        if (isUploadingImage) {
                                            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(20.dp), color = accentColor, strokeWidth = 2.dp)
                                        } else {
                                            val fotoAlpha = if (attachedMedia.isEmpty()) 0.8f else 0.3f
                                            Icon(painterResource(id = R.drawable.ic_image), contentDescription = "Foto", tint = Color.White.copy(alpha = fotoAlpha), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    // MD Toggle
                                    IconButton(onClick = { isMarkdownMenuExpanded = !isMarkdownMenuExpanded }) {
                                        Icon(painterResource(id = R.drawable.ic_pencil), contentDescription = "Markdown", tint = if (isMarkdownMenuExpanded) accentColor else Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                                    }
                                }
                                AnimatedVisibility(visible = isMarkdownMenuExpanded) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)
                                    ) {
                                        val mdAction = { prefix: String, suffix: String ->
                                            val selection = inputText.selection
                                            val text = inputText.text
                                            if (selection.collapsed) {
                                                val newText = text.substring(0, selection.start) + prefix + suffix + text.substring(selection.end)
                                                inputText = androidx.compose.ui.text.input.TextFieldValue(newText, selection = androidx.compose.ui.text.TextRange(selection.start + prefix.length))
                                            } else {
                                                val newText = text.substring(0, selection.start) + prefix + text.substring(selection.start, selection.end) + suffix + text.substring(selection.end)
                                                inputText = androidx.compose.ui.text.input.TextFieldValue(newText, selection = androidx.compose.ui.text.TextRange(selection.end + prefix.length + suffix.length))
                                            }
                                        }
                                        
                                        @Composable
                                        fun MdBtn(onClick: () -> Unit, content: @Composable () -> Unit) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .bounceClick { onClick() }
                                                    .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                content()
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                        }

                                        androidx.compose.foundation.lazy.LazyRow {
                                            item {
                                                MdBtn(onClick = { mdAction("**", "**") }) { Text("B", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.titleMedium) }
                                                MdBtn(onClick = { mdAction("*", "*") }) { Text("I", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = Color.White, style = MaterialTheme.typography.titleMedium) }
                                                MdBtn(onClick = { mdAction("~~", "~~") }) { Text("S", textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough, color = Color.White, style = MaterialTheme.typography.titleMedium) }
                                                MdBtn(onClick = { mdAction("> ", "") }) { Text("\"\"", color = Color.White, style = MaterialTheme.typography.titleMedium) }
                                                MdBtn(onClick = { mdAction("- ", "") }) { Text("•", color = Color.White, style = MaterialTheme.typography.titleMedium) }
                                                MdBtn(onClick = { mdAction("1. ", "") }) { Text("1.", color = Color.White, style = MaterialTheme.typography.titleMedium) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }
        } // End of Scaffold
            
            // Sort Dialog Overlay
            AnimatedVisibility(
                visible = showSortMenu,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .pointerInput(Unit) { detectTapGestures(onTap = { showSortMenu = false }) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .fillMaxWidth()
                            .animateEnterExit(
                                enter = scaleIn(transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)) + fadeIn(),
                                exit = scaleOut(transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)) + fadeOut()
                            )
                            .hazeGlass(
                                state = hazeState,
                                shape = RoundedCornerShape(24.dp),
                                containerColor = Color(0xFF1E1E1E).copy(alpha = 0.7f)
                            )
                            .padding(24.dp)
                            .pointerInput(Unit) { detectTapGestures {} }
                    ) {
                        Column {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.comment_filters_title), color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge, letterSpacing = 2.sp)
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_x),
                                    contentDescription = "Chiudi",
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .bounceClick { showSortMenu = false }
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Sort By Section
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Text(stringResource(R.string.comment_sort_by), color = Color.White.copy(0.8f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Option: Date
                                    val isDate = sortOption == CommentSortOption.DATE
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .bounceClick { sortOption = CommentSortOption.DATE }
                                            .then(
                                                if (isDate) Modifier.border(1.dp, accentColor, RoundedCornerShape(12.dp)).background(accentColor.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                                else Modifier.background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                            )
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(stringResource(R.string.comment_sort_date), color = if(isDate) Color.White else Color.White.copy(alpha = 0.6f), fontWeight = if(isDate) FontWeight.Bold else FontWeight.Normal)
                                        if (isDate) {
                                            Icon(painter = painterResource(id = R.drawable.ic_tick), contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Option: Likes
                                    val isLikes = sortOption == CommentSortOption.LIKES
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .bounceClick { sortOption = CommentSortOption.LIKES }
                                            .then(
                                                if (isLikes) Modifier.border(1.dp, accentColor, RoundedCornerShape(12.dp)).background(accentColor.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                                else Modifier.background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                            )
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(stringResource(R.string.comment_sort_likes), color = if(isLikes) Color.White else Color.White.copy(alpha = 0.6f), fontWeight = if(isLikes) FontWeight.Bold else FontWeight.Normal)
                                        if (isLikes) {
                                            Icon(painter = painterResource(id = R.drawable.ic_tick), contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Order Section
                            Row(modifier = Modifier.fillMaxWidth()) {
                                // Descending
                                val isDesc = sortOrder == CommentSortOrder.DESC
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .bounceClick { sortOrder = CommentSortOrder.DESC }
                                        .then(
                                            if (isDesc) Modifier.border(1.dp, accentColor, RoundedCornerShape(50.dp)).background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(50.dp))
                                            else Modifier.background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(50.dp))
                                        )
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(painter = painterResource(id = R.drawable.ic_left), contentDescription = null, tint = if(isDesc) accentColor else Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp).rotate(-90f))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.comment_sort_descending), color = if(isDesc) accentColor else Color.White.copy(alpha = 0.5f), fontWeight = if(isDesc) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.labelLarge)
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                // Ascending
                                val isAsc = sortOrder == CommentSortOrder.ASC
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .bounceClick { sortOrder = CommentSortOrder.ASC }
                                        .then(
                                            if (isAsc) Modifier.border(1.dp, accentColor, RoundedCornerShape(50.dp)).background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(50.dp))
                                            else Modifier.background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(50.dp))
                                        )
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(painter = painterResource(id = R.drawable.ic_right), contentDescription = null, tint = if(isAsc) accentColor else Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp).rotate(-90f))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.comment_sort_ascending), color = if(isAsc) accentColor else Color.White.copy(alpha = 0.5f), fontWeight = if(isAsc) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }

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
            FlickTroveModal(
                isVisible = commentToReport != null,
                onDismissRequest = { commentToReport = null },
                hazeState = hazeState
            ) {
                val comment = commentToReport
                if (comment != null) {
                    Text(stringResource(R.string.comment_report_title), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.comment_report_subtitle), color = Color.White.copy(0.8f))
                    Spacer(modifier = Modifier.height(24.dp))
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val reportButtonModifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(vertical = 14.dp)
                                
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier
                                    .bounceClick {
                                        viewModel.reportComment(comment.id, "SPOILER", comment.text, comment.userId, comment.userDisplayName)
                                        commentToReport = null
                                    }
                                    .then(reportButtonModifier),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(stringResource(R.string.comment_report_spoiler), color = Color.Red, fontWeight = FontWeight.SemiBold)
                                }
                                
                                Box(modifier = Modifier
                                    .bounceClick {
                                        viewModel.reportComment(comment.id, "INAPPROPRIATE_CONTENT", comment.text, comment.userId, comment.userDisplayName)
                                        commentToReport = null
                                    }
                                    .then(reportButtonModifier),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(stringResource(R.string.comment_report_inappropriate_content), color = Color.Red, fontWeight = FontWeight.SemiBold)
                                }
                                
                                Box(modifier = Modifier
                                    .bounceClick {
                                        viewModel.reportComment(comment.id, "INAPPROPRIATE_USER", comment.text, comment.userId, comment.userDisplayName)
                                        commentToReport = null
                                    }
                                    .then(reportButtonModifier),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(stringResource(R.string.comment_report_inappropriate_user), color = Color.Red, fontWeight = FontWeight.SemiBold)
                                }
                                
                                Box(modifier = Modifier
                                    .bounceClick { commentToReport = null }
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(stringResource(R.string.comment_cancel_btn), color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

            // Delete Dialog Overlay
            FlickTroveModal(
                isVisible = commentToDelete != null,
                onDismissRequest = { commentToDelete = null },
                hazeState = hazeState
            ) {
                val comment = commentToDelete
                if (comment != null) {
                    Text(stringResource(R.string.comment_delete_title), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.comment_delete_subtitle), color = Color.White.copy(0.8f))
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { commentToDelete = null },
                                    modifier = Modifier
                                        .weight(1f)
                                        .bounceClick { commentToDelete = null },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.08f),
                                        contentColor = Color.White.copy(alpha = 0.85f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                                ) {
                                    Text(
                                        text = stringResource(R.string.comment_cancel_btn),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Button(
                                    onClick = {
                                        viewModel.deleteComment(comment.id)
                                        commentToDelete = null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .bounceClick {
                                            viewModel.deleteComment(comment.id)
                                            commentToDelete = null
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Red.copy(alpha = 0.15f),
                                        contentColor = Color(0xFFFF5252)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = stringResource(R.string.comment_delete_title),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
            } // End of Outer Box
        }
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

@Composable
fun LiquidStarIcon(isLiked: Boolean, accentColor: Color, modifier: Modifier = Modifier) {
    val fillAnim = remember { androidx.compose.animation.core.Animatable(if (isLiked) 1f else 0f) }
    val splashAnim = remember { androidx.compose.animation.core.Animatable(0f) }
    val waveAnim = remember { androidx.compose.animation.core.Animatable(0f) }
    var isFirstComposition by remember { mutableStateOf(true) }

    LaunchedEffect(isLiked) {
        if (isFirstComposition) {
            isFirstComposition = false
            return@LaunchedEffect
        }
        
        if (isLiked) {
            launch {
                waveAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 500, easing = androidx.compose.animation.core.LinearEasing)
                )
            }
            launch {
                fillAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 400, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                )
            }
            launch {
                delay(300)
                splashAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 300, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
                )
                splashAnim.snapTo(0f)
                waveAnim.snapTo(0f)
            }
        } else {
            fillAnim.snapTo(0f)
            splashAnim.snapTo(0f)
            waveAnim.snapTo(0f)
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Icon(
            painter = painterResource(id = R.drawable.ic_star),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.matchParentSize()
        )
        
        if (fillAnim.value > 0f) {
            Icon(
                painter = painterResource(id = R.drawable.ic_star_piena),
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier
                    .matchParentSize()
                    .drawWithContent {
                        val path = androidx.compose.ui.graphics.Path()
                        val width = this.size.width
                        val height = this.size.height
                        
                        val fillHeight = height * fillAnim.value
                        val waterY = height - fillHeight
                        
                        if (fillAnim.value < 1f) {
                            val waveHeight = 1.5.dp.toPx()
                            val wavePhase = waveAnim.value * Math.PI * 4 
                            
                            path.moveTo(0f, height)
                            path.lineTo(0f, waterY)
                            
                            for (x in 0..width.toInt() step 2) {
                                val yOffset = kotlin.math.sin((x / width) * Math.PI * 2 + wavePhase).toFloat() * waveHeight
                                path.lineTo(x.toFloat(), waterY + yOffset)
                            }
                            
                            path.lineTo(width, waterY)
                            path.lineTo(width, height)
                            path.close()
                            
                            clipPath(path) {
                                this@drawWithContent.drawContent()
                            }
                        } else {
                            this@drawWithContent.drawContent()
                        }
                    }
            )
        }

        if (splashAnim.value > 0f && splashAnim.value < 1f) {
            Canvas(modifier = Modifier.fillMaxSize().scale(2f)) {
                val center = androidx.compose.ui.geometry.Offset(this.size.width / 2, this.size.height / 2)
                val maxRadius = this.size.width / 2
                
                for (i in 0 until 5) {
                    val angle = (i * (360f / 5) - 90f) * (Math.PI / 180f).toFloat()
                    val distance = maxRadius * (0.5f + splashAnim.value * 0.8f)
                    
                    val dropCenter = androidx.compose.ui.geometry.Offset(
                        x = center.x + kotlin.math.cos(angle.toDouble()).toFloat() * distance,
                        y = center.y + kotlin.math.sin(angle.toDouble()).toFloat() * distance
                    )
                    
                    val dropRadius = (2.dp.toPx()) * (1f - splashAnim.value)
                    
                    drawCircle(
                        color = accentColor.copy(alpha = 1f - splashAnim.value),
                        radius = dropRadius,
                        center = dropCenter
                    )
                }
            }
        }
    }
}

fun parseSimpleMarkdown(text: String, accentColor: Color): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        lines.forEachIndexed { index, line ->
            var currentLine = line
            var isQuote = false
            var isList = false
            
            if (currentLine.startsWith("> ")) {
                isQuote = true
                currentLine = currentLine.substring(2)
            } else if (currentLine.startsWith("- ")) {
                isList = true
                currentLine = currentLine.substring(2)
            }
            
            withStyle(
                style = SpanStyle(
                    color = if (isQuote) Color.White.copy(alpha = 0.5f) else Color.Unspecified,
                    fontStyle = if (isQuote) androidx.compose.ui.text.font.FontStyle.Italic else null
                )
            ) {
                if (isList) {
                    append("• ")
                }
                
                var i = 0
                while (i < currentLine.length) {
                    if (currentLine.startsWith("**", i)) {
                        val end = currentLine.indexOf("**", i + 2)
                        if (end != -1) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(currentLine.substring(i + 2, end))
                            }
                            i = end + 2
                            continue
                        }
                    }
                    if (currentLine.startsWith("*", i) && !currentLine.startsWith("**", i)) {
                        val end = currentLine.indexOf("*", i + 1)
                        if (end != -1) {
                            withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                                append(currentLine.substring(i + 1, end))
                            }
                            i = end + 1
                            continue
                        }
                    }
                    if (currentLine.startsWith("~~", i)) {
                        val end = currentLine.indexOf("~~", i + 2)
                        if (end != -1) {
                            withStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)) {
                                append(currentLine.substring(i + 2, end))
                            }
                            i = end + 2
                            continue
                        }
                    }
                    append(currentLine[i])
                    i++
                }
            }
            if (index < lines.size - 1) append("\n")
        }
    }
}

class MarkdownVisualTransformation : androidx.compose.ui.text.input.VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val originalText = text.text
        val symbolColor = androidx.compose.ui.graphics.Color(0xFF666666) // Grigio scuro per i simboli markdown
        
        val annotatedString = androidx.compose.ui.text.buildAnnotatedString {
            append(originalText)
            
            // Bold
            Regex("\\*\\*(.*?)\\*\\*").findAll(originalText).forEach { match ->
                addStyle(androidx.compose.ui.text.SpanStyle(color = symbolColor), match.range.first, match.range.first + 2)
                addStyle(androidx.compose.ui.text.SpanStyle(color = symbolColor), match.range.last - 1, match.range.last + 1)
                addStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), match.range.first + 2, match.range.last - 1)
            }
            
            // Italic
            Regex("(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)").findAll(originalText).forEach { match ->
                addStyle(androidx.compose.ui.text.SpanStyle(color = symbolColor), match.range.first, match.range.first + 1)
                addStyle(androidx.compose.ui.text.SpanStyle(color = symbolColor), match.range.last, match.range.last + 1)
                addStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), match.range.first + 1, match.range.last)
            }
            
            // Strikethrough
            Regex("~~(.*?)~~").findAll(originalText).forEach { match ->
                addStyle(androidx.compose.ui.text.SpanStyle(color = symbolColor), match.range.first, match.range.first + 2)
                addStyle(androidx.compose.ui.text.SpanStyle(color = symbolColor), match.range.last - 1, match.range.last + 1)
                addStyle(androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough), match.range.first + 2, match.range.last - 1)
            }
            
            // Quotes & Lists (line by line)
            val lines = originalText.split("\n")
            var currentIndex = 0
            lines.forEach { line ->
                if (line.startsWith("> ")) {
                    addStyle(androidx.compose.ui.text.SpanStyle(color = symbolColor), currentIndex, currentIndex + 2)
                    addStyle(androidx.compose.ui.text.SpanStyle(color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), currentIndex + 2, currentIndex + line.length)
                } else if (line.startsWith("- ")) {
                    addStyle(androidx.compose.ui.text.SpanStyle(color = symbolColor), currentIndex, currentIndex + 2)
                } else if (line.matches(Regex("^\\d+\\.\\s.*"))) {
                    val dotIndex = line.indexOf(". ") + 2
                    addStyle(androidx.compose.ui.text.SpanStyle(color = symbolColor), currentIndex, currentIndex + dotIndex)
                }
                currentIndex += line.length + 1 // +1 for the newline
            }
        }
        return androidx.compose.ui.text.input.TransformedText(annotatedString, androidx.compose.ui.text.input.OffsetMapping.Identity)
    }
}

tailrec fun android.content.Context.findFragmentActivity(): androidx.fragment.app.FragmentActivity? = when (this) {
    is androidx.fragment.app.FragmentActivity -> this
    is android.content.ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}