package com.cinetrack.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import coil3.imageLoader
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.parseSimpleMarkdown
import com.cinetrack.ui.utils.MarkdownVisualTransformation
import com.cinetrack.ui.components.shared.shimmerEffect
import com.cinetrack.ui.components.shared.rememberShimmerBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.rounded.Info
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
import coil3.compose.AsyncImage
import com.cinetrack.data.model.AppComment
import com.cinetrack.data.model.filterDeletedWithoutReplies
import com.cinetrack.ui.viewmodel.CommentsViewModel
import com.cinetrack.ui.components.detail.DetailTranslationPromptModal
import com.cinetrack.ui.components.comments.CommentInputBar
import com.cinetrack.ui.components.comments.CommentReportModal
import com.cinetrack.ui.components.comments.CommentBlockModal
import com.cinetrack.ui.components.comments.CommentDeleteModal
import com.cinetrack.ui.components.comments.ExternalLinkWarningModal
import com.cinetrack.util.LinkSecurityUtils
import com.cinetrack.util.LinkSecurityCheck
import com.cinetrack.ui.components.comments.CommentCardItem
import com.cinetrack.ui.components.comments.CommentSkeletonItem
import com.cinetrack.ui.components.comments.CommsUniFilterModal
import com.cinetrack.ui.components.comments.buildFlatCommentTree
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
import com.cinetrack.ui.theme.HazeStyles
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
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


class CommsUniCommentsScreen(
    private val tvdbId: Int? = null,
    private val mediaId: String = "",
    private val mediaType: String = "movie",
    private val seasonNumber: Int? = null,
    private val episodeNumber: Int? = null,
    private val accentColorValue: Long = 0L,
    private val mediaTitle: String = "",
    private val mediaImage: String? = null,
    private val backdropUrl: String? = null,
    private val releaseYear: String? = null,
    private val focusInputOnLaunch: Boolean = false,
    private val targetCommentId: String? = null,
    private val imdbId: String? = null,
    private val initialImageUri: String? = null,
    private val episodeTitle: String? = null
) : Screen {

    override val key: String = "CommentsScreen_${mediaType}_${mediaId}_${java.util.UUID.randomUUID()}"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = getViewModel<com.cinetrack.ui.viewmodel.CommsUniViewModel>()
        
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
        
        var currentSeasonNumber by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(seasonNumber) }
        var currentEpisodeNumber by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(episodeNumber) }
        var currentEpTitle by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(episodeTitle) }
        var currentBackdropUrl by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(backdropUrl) }

        val currentEffectiveMediaType = remember(currentSeasonNumber, currentEpisodeNumber, mediaType) {
            if (currentSeasonNumber != null && currentEpisodeNumber != null) "episode"
            else if (mediaType == "episode") "tv"
            else mediaType
        }

        LaunchedEffect(tvdbId, currentEffectiveMediaType, mediaTitle, releaseYear, mediaId, imdbId, currentSeasonNumber, currentEpisodeNumber) {
            viewModel.init(
                tvdbId = tvdbId,
                entityType = currentEffectiveMediaType,
                seasonNumber = currentSeasonNumber,
                episodeNumber = currentEpisodeNumber,
                title = mediaTitle,
                year = releaseYear,
                rawMediaId = mediaId,
                imdbId = imdbId
            )
        }

        val comments by viewModel.comments.collectAsStateWithLifecycle()
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
        val hasMoreComments by viewModel.hasMoreComments.collectAsStateWithLifecycle()
        val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()
        val translationStates by viewModel.translationStates.collectAsStateWithLifecycle()
        val showTranslationPrompt by viewModel.showTranslationPrompt.collectAsStateWithLifecycle()
        // Temporary shadow properties
        val isUserBanned = false 
        val banExpiration: String? = null
        val availableSources by viewModel.availableSources.collectAsStateWithLifecycle()
        val conversationStats by viewModel.conversationStats.collectAsStateWithLifecycle()
        val showSeasons by viewModel.showSeasons.collectAsStateWithLifecycle()
        val seasonEpisodes by viewModel.seasonEpisodes.collectAsStateWithLifecycle()
        val isLoadingSeasonEpisodes by viewModel.isLoadingSeasonEpisodes.collectAsStateWithLifecycle()
        val currentEpisodeTitle = remember(currentEpTitle, seasonEpisodes, currentSeasonNumber, currentEpisodeNumber) {
            currentEpTitle?.takeIf { it.isNotBlank() }
                ?: if (currentSeasonNumber != null && currentEpisodeNumber != null) {
                    seasonEpisodes[currentSeasonNumber]?.find { it.episodeNumber == currentEpisodeNumber }?.name?.takeIf { it.isNotBlank() }
                } else null
        }
        var showEpisodePickerSheet by remember { mutableStateOf(false) }
        val isTvSeries = remember(mediaType, currentSeasonNumber, showSeasons) {
            mediaType.lowercase() in listOf("tv", "series", "show", "episode") || currentSeasonNumber != null || showSeasons.isNotEmpty()
        }
        val accentColor = Color(accentColorValue.toULong())

        var replyingTo by remember { mutableStateOf<AppComment?>(null) }
        // True if the parent comment lives on CommsUni
        val isReplyingToCommsUni = replyingTo?.isOnCommsUni == true
        // True if the parent comment lives only on local FlickTrove (Firestore)
        val isReplyingToFlickTrove = replyingTo != null && !replyingTo!!.isOnCommsUni
        var inputText by androidx.compose.runtime.saveable.rememberSaveable(stateSaver = androidx.compose.ui.text.input.TextFieldValue.Saver) { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
        var isSpoiler by remember { mutableStateOf(false) }
        var postToCommsUni by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }

        // Lock destination to match the parent comment's platform
        LaunchedEffect(replyingTo) {
            when {
                replyingTo != null && replyingTo!!.isOnCommsUni -> postToCommsUni = true
                replyingTo != null && !replyingTo!!.isOnCommsUni -> postToCommsUni = false
            }
        }
        var isInputExpanded by remember { mutableStateOf(false) }
        var isMarkdownMenuExpanded by remember { mutableStateOf(false) }
        var commentToReport by remember { mutableStateOf<AppComment?>(null) }
        var commentToDelete by remember { mutableStateOf<AppComment?>(null) }
        var userToBlock by remember { mutableStateOf<AppComment?>(null) }
        var pendingExternalUrl by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
        var pendingExternalHost by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
        var sortOption by remember { mutableStateOf(CommentSortOption.DATE) }
        var sortOrder by remember { mutableStateOf(CommentSortOrder.DESC) }
        var sourceFilters by remember { mutableStateOf<Set<String>>(emptySet()) }
        var languageFilters by remember { mutableStateOf<Set<String>>(emptySet()) }
        var showSortMenu by remember { mutableStateOf(false) }
        var showCommsUniInfo by remember { mutableStateOf(false) }
        var sortButtonBounds by remember { mutableStateOf<Rect?>(null) }
        var commsUniButtonBounds by remember { mutableStateOf<Rect?>(null) }
        var episodesButtonBounds by remember { mutableStateOf<Rect?>(null) }

        LaunchedEffect(sortOption, sortOrder, sourceFilters, languageFilters) {
            val sortString = if (sortOption == CommentSortOption.DATE) (if (sortOrder == CommentSortOrder.DESC) "most_recent" else "oldest") else "most_liked"
            val singleSource = if (sourceFilters.count() == 1) sourceFilters.first() else null
            val singleLanguage = if (languageFilters.count() == 1) languageFilters.first() else null
            viewModel.setSortAndFilters(sortString, singleSource, singleLanguage)
        }
        val localFocusManager = LocalFocusManager.current
        val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
        val context = androidx.compose.ui.platform.LocalContext.current
        val hazeState = remember { HazeState() }
        val globalHazeState = remember { HazeState() }
        val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
        var isUploadingImage by remember { mutableStateOf(false) }
        var attachedMedia by remember { mutableStateOf(emptyList<String>()) }
        var showGiphyPicker by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
        val imageLoader = context.imageLoader
        
        var pendingCameraFile by remember { mutableStateOf<java.io.File?>(null) }
        var pendingCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

        val mediaChooserLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            onResult = { result ->
                if (result.resultCode == android.app.Activity.RESULT_OK) {
                    val galleryUri = result.data?.data
                    val photoFile = pendingCameraFile
                    val finalUri = if (galleryUri != null) {
                        photoFile?.delete()
                        galleryUri
                    } else if (photoFile != null && photoFile.exists() && photoFile.length() > 0) {
                        pendingCameraUri
                    } else {
                        null
                    }

                    if (finalUri != null) {
                        isUploadingImage = true
                        viewModel.uploadCommentImage(
                            imageUri = finalUri,
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
                } else {
                    pendingCameraFile?.let { if (it.exists() && it.length() == 0L) it.delete() }
                }
            }
        )

        val launchMediaChooser: () -> Unit = {
            try {
                val cacheDir = java.io.File(context.cacheDir, "camera_photos").apply { mkdirs() }
                val photoFile = java.io.File(cacheDir, "camera_${System.currentTimeMillis()}.jpg")
                val cameraUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                pendingCameraFile = photoFile
                pendingCameraUri = cameraUri

                val captureIntent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(android.provider.MediaStore.EXTRA_OUTPUT, cameraUri)
                    addFlags(android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val pickIntent = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                    addCategory(android.content.Intent.CATEGORY_OPENABLE)
                }

                val chooserTitle = context.getString(R.string.comment_attach_image_title)
                val chooserIntent = android.content.Intent.createChooser(pickIntent, chooserTitle).apply {
                    putExtra(android.content.Intent.EXTRA_INITIAL_INTENTS, arrayOf(captureIntent))
                }

                mediaChooserLauncher.launch(chooserIntent)
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, context.getString(R.string.comment_camera_error), android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        LaunchedEffect(Unit) {
            if (focusInputOnLaunch) {
                kotlinx.coroutines.delay(300) // Aspetta fine animazione navigazione
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }

        LaunchedEffect(initialImageUri) {
            if (!initialImageUri.isNullOrBlank()) {
                val uri = android.net.Uri.parse(initialImageUri)
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

        val movieColor = if (accentColorValue != 0L) Color(accentColorValue) else Color.Transparent
        val effectiveBackdrop = currentBackdropUrl ?: mediaImage

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(globalHazeState)
            ) {
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .hazeGlass(
                                    state = hazeState,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                                    containerColor = Color.Black.copy(alpha = 0.75f),
                                    blurRadius = 20.dp
                                )
                                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                        ) {
                            val sortCoords = remember { arrayOf<LayoutCoordinates?>(null) }
                            val commsUniCoords = remember { arrayOf<LayoutCoordinates?>(null) }
                            val episodesCoords = remember { arrayOf<LayoutCoordinates?>(null) }

                            // Livello 1: Back, Titolo Media & Azioni (CommsUni + Filtri)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 0.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .bounceClick { navigator.pop() }
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_left),
                                        contentDescription = stringResource(R.string.detail_content_desc_back),
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Text(
                                    text = mediaTitle.ifBlank { stringResource(R.string.comments_screen_title) },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 2.dp, end = 6.dp)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .onGloballyPositioned { commsUniCoords[0] = it }
                                            .bounceClick {
                                                commsUniButtonBounds = commsUniCoords[0]?.let {
                                                    val pos = it.positionInWindow()
                                                    Rect(pos.x, pos.y, pos.x + it.size.width, pos.y + it.size.height)
                                                }
                                                showCommsUniInfo = true
                                            }
                                            .clip(CircleShape)
                                            .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                                            .padding(horizontal = 8.dp, vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.ic_commsuni_logo),
                                                contentDescription = stringResource(R.string.commsuni_info_title),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Icon(
                                                imageVector = Icons.Rounded.Info,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.70f),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 2.dp)
                                            .onGloballyPositioned { sortCoords[0] = it }
                                            .bounceClick { 
                                                sortButtonBounds = sortCoords[0]?.let {
                                                    val pos = it.positionInWindow()
                                                    Rect(pos.x, pos.y, pos.x + it.size.width, pos.y + it.size.height)
                                                }
                                                showSortMenu = true 
                                            }
                                            .padding(8.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_filtri),
                                            contentDescription = stringResource(R.string.comment_sort_by),
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // Livello 2: Se serie TV, pulsante compatto puntata + titolo episodio esterno
                            if (isTvSeries) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 44.dp, end = 16.dp, top = 0.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Pill compatta: [ 📺 1x02 > ]
                                    Box(
                                        modifier = Modifier
                                            .onGloballyPositioned { episodesCoords[0] = it }
                                            .bounceClick {
                                                episodesButtonBounds = episodesCoords[0]?.let {
                                                    val pos = it.positionInWindow()
                                                    Rect(pos.x, pos.y, pos.x + it.size.width, pos.y + it.size.height)
                                                }
                                                showEpisodePickerSheet = true
                                            }
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                                            .padding(horizontal = 9.dp, vertical = 4.5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_tv),
                                                contentDescription = stringResource(R.string.episodes_discussions_pill),
                                                tint = Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            val pillText = if (currentSeasonNumber != null && currentEpisodeNumber != null) {
                                                "${currentSeasonNumber}x${String.format(java.util.Locale.US, "%02d", currentEpisodeNumber)}"
                                            } else {
                                                stringResource(R.string.episodes_discussions_pill)
                                            }
                                            Text(
                                                text = pillText,
                                                color = Color.White.copy(alpha = 0.90f),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_right),
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.70f),
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }

                                    // Titolo episodio FUORI dal pulsante
                                    if (!currentEpisodeTitle.isNullOrBlank()) {
                                        Text(
                                            text = currentEpisodeTitle,
                                            color = Color.White.copy(alpha = 0.75f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .weight(1f, fill = false)
                                                .bounceClick {
                                                    episodesButtonBounds = episodesCoords[0]?.let {
                                                        val pos = it.positionInWindow()
                                                        Rect(pos.x, pos.y, pos.x + it.size.width, pos.y + it.size.height)
                                                    }
                                                    showEpisodePickerSheet = true
                                                }
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
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
                            val parentId = current.parentId
                            toExpand.add(parentId)
                            current = comments.find { it.id == parentId }
                        }
                        if (toExpand.isNotEmpty()) {
                            expandedComments.value = expandedComments.value + toExpand
                        }
                    }
                }

                val flatTree =
                    remember(comments, expandedComments.value, sortOption, sortOrder, sourceFilters, languageFilters) {
                    buildFlatCommentTree(comments, expandedComments.value, sortOption, sortOrder, sourceFilters, languageFilters) 
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

                val shimmerBrush = rememberShimmerBrush()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(state = hazeState, style = HazeStyles.PremiumDark)
                ) {
                    if (!effectiveBackdrop.isNullOrBlank()) {
                        AsyncImage(
                            model = effectiveBackdrop,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clipToBounds()
                                .scale(1.05f)
                                .blur(8.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                                .drawWithContent {
                                    drawContent()
                                    // Scurito uniformemente su tutto lo schermo (senza gradiente) per garantire massimo contrasto
                                    drawRect(Color.Black.copy(alpha = 0.88f))
                                }
                        )
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
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
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        itemsIndexed(flatTree, key = { _, it -> it.id }) { index, comment ->
                            val isLiked = viewModel.isCommentLikedByMe(comment)
                            val isExpanded = expandedComments.value.contains(comment.id)
                            CommentCardItem(
                                modifier = Modifier.animateItem(),
                                comment = comment,
                                index = index,
                                flatTree = flatTree,
                                allComments = comments,
                                isLiked = isLiked,
                                translationState = translationStates[comment.id],
                                currentUserId = viewModel.currentUserId,
                                isOwner = viewModel.isCommentMine(comment),
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
                                    postToCommsUni = comment.isOnCommsUni
                                },
                                onToggleLike = {
                                    viewModel.toggleLikeComment(comment.id, mediaTitle, mediaImage)
                                },
                                onReport = {
                                    commentToReport = comment
                                },
                                onBlockUser = {
                                    userToBlock = comment
                                },
                                onTranslate = { text ->
                                    viewModel.translateComment(comment.id, text)
                                },
                                onTriggerGuestAuth = {
                                    settingsViewModel.triggerGuestAuthDialog()
                                },
                                onOpenUrl = { rawUrl ->
                                    when (val check = LinkSecurityUtils.verifyUrl(rawUrl)) {
                                        is LinkSecurityCheck.Safe -> {
                                            pendingExternalUrl = check.cleanUrl
                                            pendingExternalHost = check.host
                                        }
                                        is LinkSecurityCheck.Blocked -> {
                                            viewModel.emitBlockedLink(check.reasonResId)
                                        }
                                    }
                                }
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
                    postToCommsUni = postToCommsUni,
                    onPostToCommsUniChanged = { postToCommsUni = it },
                    destinationToggleEnabled = replyingTo == null,
                    attachedMedia = attachedMedia,
                    onAttachedMediaChanged = { attachedMedia = it },
                    onRemoveMedia = { removedUrl -> viewModel.deleteDraftCommentImage(removedUrl) },
                    isUploadingImage = isUploadingImage,
                    onPickImage = launchMediaChooser,
                    onPickGif = { showGiphyPicker = true },
                    onSendComment = {
                        if (inputText.text.isNotBlank() || attachedMedia.isNotEmpty()) {
                            val pId = replyingTo?.id
                            val pUserId = replyingTo?.userId
                            val newDepth = (replyingTo?.depth ?: -1) + 1

                            // Auto-expand the parent comment so the new reply is visible immediately
                            if (pId != null) {
                                expandedComments.value = expandedComments.value + pId
                            }

                            val cleanMessage = inputText.text.trim()

                            localFocusManager.clearFocus()
                            val effectivePostToCommsUni = when {
                                isReplyingToCommsUni -> true
                                isReplyingToFlickTrove -> false
                                else -> postToCommsUni
                            }
                            viewModel.addComment(
                                text = cleanMessage,
                                isSpoiler = isSpoiler,
                                parentId = pId,
                                parentUserId = pUserId,
                                depth = newDepth,
                                mediaTitle = mediaTitle,
                                mediaImage = mediaImage,
                                postToCommsUni = effectivePostToCommsUni,
                                attachedMedia = attachedMedia
                            )
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
    } // End of Box with globalHazeState
            
            // GIF Picker Modal Overlay (Root level for full screen blur & safe insets)
            com.cinetrack.ui.components.comments.GiphyPickerModal(
                isVisible = showGiphyPicker,
                onDismiss = { showGiphyPicker = false },
                hazeState = globalHazeState,
                accentColor = accentColor,
                onGifSelected = { gifUrl ->
                    attachedMedia = listOf(gifUrl)
                    showGiphyPicker = false
                }
            )

            // Sort Dialog Overlay
            CommsUniFilterModal(
                isVisible = showSortMenu,
                conversationStats = conversationStats,
                comments = comments,
                availableSources = availableSources,
                sortButtonBounds = sortButtonBounds,
                sortOption = sortOption,
                sortOrder = sortOrder,
                sourceFilters = sourceFilters,
                languageFilters = languageFilters,
                hazeState = globalHazeState,
                onSortChanged = { newSortOption, newSortOrder, newSourceFilters, newLanguageFilters ->
                    sortOption = newSortOption
                    sortOrder = newSortOrder
                    sourceFilters = newSourceFilters
                    languageFilters = newLanguageFilters
                },
                onDismissRequest = { showSortMenu = false }
            )

            // CommsUni Project Info Modal
            com.cinetrack.ui.components.comments.CommsUniInfoModal(
                isVisible = showCommsUniInfo,
                onDismiss = { showCommsUniInfo = false },
                triggerBounds = commsUniButtonBounds,
                hazeState = globalHazeState
            )

            // Translation Prompt Dialog
            DetailTranslationPromptModal(
                showTranslationPrompt = showTranslationPrompt,
                onDismiss = { viewModel.dismissTranslationPrompt() },
                onTranslate = { commentId, text, requireWifi ->
                    viewModel.translateComment(commentId, text, requireWifi)
                },
                hazeState = globalHazeState,
                accentColor = accentColor
            )

            // Report Dialog Overlay
            CommentReportModal(
                comment = commentToReport,
                onDismiss = { commentToReport = null },
                onReport = { category, detail ->
                    commentToReport?.let { c ->
                        viewModel.reportComment(c.id, category, c.text, c.userId, c.userDisplayName, detail = detail)
                    }
                    commentToReport = null
                },
                onBlockUser = { target ->
                    commentToReport = null
                    userToBlock = target
                },
                hazeState = globalHazeState
            )

            // Block User Dialog Overlay
            CommentBlockModal(
                comment = userToBlock,
                onDismiss = { userToBlock = null },
                onConfirm = {
                    userToBlock?.let { c ->
                        viewModel.blockAuthor(c.userId, c.userDisplayName)
                    }
                    userToBlock = null
                },
                hazeState = globalHazeState
            )

            // Delete Dialog Overlay
            CommentDeleteModal(
                comment = commentToDelete,
                onDismiss = { commentToDelete = null },
                onConfirm = {
                    commentToDelete?.let { c ->
                        viewModel.deleteComment(c.id, c)
                    }
                    commentToDelete = null
                },
                hazeState = globalHazeState
            )

            // External Link Warning Dialog Overlay
            ExternalLinkWarningModal(
                url = pendingExternalUrl,
                host = pendingExternalHost,
                onDismiss = { pendingExternalUrl = null },
                onConfirm = { linkUrl ->
                    pendingExternalUrl = null
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(linkUrl)).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        viewModel.emitBlockedLink(R.string.external_link_error_no_browser)
                    }
                },
                hazeState = globalHazeState
            )

            val baseShowId = remember(mediaId) {
                if (mediaId.contains("_s")) mediaId.substringBefore("_s") else mediaId
            }
            com.cinetrack.ui.components.comments.EpisodeDiscussionPickerModal(
                isVisible = showEpisodePickerSheet,
                showSeasons = showSeasons,
                seasonEpisodes = seasonEpisodes,
                isLoadingEpisodes = isLoadingSeasonEpisodes,
                currentSeason = currentSeasonNumber,
                currentEpisode = currentEpisodeNumber,
                fallbackBackdropPath = currentBackdropUrl,
                onLoadSeasonEpisodes = { sNum -> viewModel.loadSeasonEpisodes(sNum) },
                onSelectEpisode = { sNum, ep ->
                    showEpisodePickerSheet = false
                    currentSeasonNumber = sNum
                    currentEpisodeNumber = ep.episodeNumber
                    currentEpTitle = ep.name
                    val epStill = ep.stillPath?.let {
                        com.cinetrack.util.buildTmdbImageUrl(it, com.cinetrack.util.ImageType.BACKDROP, com.cinetrack.util.ImageQuality.HIGH)
                    }
                    if (epStill != null) {
                        currentBackdropUrl = epStill
                    }
                    replyingTo = null
                },
                onSelectMainDiscussion = {
                    showEpisodePickerSheet = false
                    currentSeasonNumber = null
                    currentEpisodeNumber = null
                    currentEpTitle = null
                    currentBackdropUrl = backdropUrl
                    replyingTo = null
                },
                onDismiss = { showEpisodePickerSheet = false },
                accentColor = accentColor,
                hazeState = globalHazeState,
                triggerBounds = episodesButtonBounds
            )

            androidx.activity.compose.BackHandler(
                enabled = showEpisodePickerSheet || pendingExternalUrl != null || userToBlock != null || commentToReport != null || commentToDelete != null
            ) {
                when {
                    showEpisodePickerSheet -> showEpisodePickerSheet = false
                    pendingExternalUrl != null -> pendingExternalUrl = null
                    userToBlock != null -> userToBlock = null
                    commentToReport != null -> commentToReport = null
                    commentToDelete != null -> commentToDelete = null
                }
            }
        } // End of Outer Box
    }
}


