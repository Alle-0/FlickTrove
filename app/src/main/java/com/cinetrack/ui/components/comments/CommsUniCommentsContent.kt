package com.cinetrack.ui.components.comments

import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.Navigator
import coil3.compose.AsyncImage
import coil3.imageLoader
import com.cinetrack.R
import com.cinetrack.data.model.AppComment
import com.cinetrack.data.model.CommentSortOption
import com.cinetrack.data.model.CommentSortOrder
import com.cinetrack.ui.components.shared.rememberShimmerBrush
import com.cinetrack.ui.screens.MovieDetailScreen
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.TaggedMediaRef
import com.cinetrack.ui.utils.rememberGuestAuthManager
import com.cinetrack.ui.utils.serializeMediaTagsForSend
import com.cinetrack.ui.viewmodel.CommsUniViewModel
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.ImageType
import com.cinetrack.util.LinkSecurityCheck
import com.cinetrack.util.LinkSecurityUtils
import com.cinetrack.util.buildTmdbImageUrl
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommsUniCommentsContent(
    viewModel: CommsUniViewModel,
    navigator: Navigator,
    tvdbId: Int? = null,
    mediaId: String = "",
    mediaType: String = "movie",
    seasonNumber: Int? = null,
    episodeNumber: Int? = null,
    accentColorValue: Long = 0L,
    mediaTitle: String = "",
    mediaImage: String? = null,
    backdropUrl: String? = null,
    releaseYear: String? = null,
    focusInputOnLaunch: Boolean = false,
    targetCommentId: String? = null,
    imdbId: String? = null,
    initialImageUri: String? = null,
    episodeTitle: String? = null
) {
    val localCtx = LocalContext.current
    var currentContext = localCtx
    while (currentContext is ContextWrapper && currentContext !is ComponentActivity) {
        currentContext = currentContext.baseContext
    }
    val activity = currentContext as? ComponentActivity
    val guestAuthManager = rememberGuestAuthManager()

    var currentSeasonNumber by rememberSaveable { mutableStateOf(seasonNumber) }
    var currentEpisodeNumber by rememberSaveable { mutableStateOf(episodeNumber) }
    var currentEpTitle by rememberSaveable { mutableStateOf(episodeTitle) }
    var currentBackdropUrl by rememberSaveable { mutableStateOf(backdropUrl) }

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
    val mediaTagSuggestions by viewModel.mediaTagSuggestions.collectAsStateWithLifecycle()
    val isSearchingMediaTags by viewModel.isSearchingMediaTags.collectAsStateWithLifecycle()
    val taggedMediaTitles by viewModel.taggedMediaTitles.collectAsStateWithLifecycle()
    val taggedMediaPosters by viewModel.taggedMediaPosters.collectAsStateWithLifecycle()
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
    var inputText by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var isSpoiler by remember { mutableStateOf(false) }
    var postToCommsUni by rememberSaveable { mutableStateOf(true) }

    // Lock destination to match the parent comment's platform
    LaunchedEffect(replyingTo) {
        when {
            replyingTo != null && replyingTo!!.isOnCommsUni -> postToCommsUni = true
            replyingTo != null && !replyingTo!!.isOnCommsUni -> postToCommsUni = false
        }
    }
    var commentToReport by remember { mutableStateOf<AppComment?>(null) }
    var commentToDelete by remember { mutableStateOf<AppComment?>(null) }
    var userToBlock by remember { mutableStateOf<AppComment?>(null) }
    var pendingExternalUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingExternalHost by rememberSaveable { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(CommentSortOption.DATE) }
    var sortOrder by remember { mutableStateOf(CommentSortOrder.DESC) }
    var sourceFilters by remember { mutableStateOf<Set<String>>(emptySet()) }
    var languageFilters by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showCommsUniInfo by remember { mutableStateOf(false) }
    var showWritingTips by remember { mutableStateOf(false) }
    var sortButtonBounds by remember { mutableStateOf<Rect?>(null) }
    var commsUniButtonBounds by remember { mutableStateOf<Rect?>(null) }
    var writingTipsButtonBounds by remember { mutableStateOf<Rect?>(null) }
    var episodesButtonBounds by remember { mutableStateOf<Rect?>(null) }
    var gifButtonBounds by remember { mutableStateOf<Rect?>(null) }
    var selectedMediaTags by rememberSaveable(
        stateSaver = listSaver(
            save = { list ->
                list.map {
                    listOf(
                        it.id.toString(),
                        it.mediaType,
                        it.title,
                        it.posterPath ?: "",
                        it.releaseYear ?: ""
                    )
                }
            },
            restore = { saved ->
                saved.mapNotNull { row ->
                    val id = row.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
                    val type = row.getOrNull(1) ?: return@mapNotNull null
                    val title = row.getOrNull(2) ?: return@mapNotNull null
                    TaggedMediaRef(
                        id = id,
                        mediaType = type,
                        title = title,
                        posterPath = row.getOrNull(3)?.takeIf { it.isNotEmpty() },
                        releaseYear = row.getOrNull(4)?.takeIf { it.isNotEmpty() }
                    )
                }
            }
        )
    ) { mutableStateOf(emptyList<TaggedMediaRef>()) }

    LaunchedEffect(sortOption, sortOrder, sourceFilters, languageFilters) {
        val sortString = if (sortOption == CommentSortOption.DATE) (if (sortOrder == CommentSortOrder.DESC) "most_recent" else "oldest") else "most_liked"
        val singleSource = if (sourceFilters.count() == 1) sourceFilters.first() else null
        val singleLanguage = if (languageFilters.count() == 1) languageFilters.first() else null
        viewModel.setSortAndFilters(sortString, singleSource, singleLanguage)
    }
    val localFocusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val hazeState = remember { HazeState() }
    val globalHazeState = remember { HazeState() }
    val focusRequester = remember { FocusRequester() }
    var isUploadingImage by remember { mutableStateOf(false) }
    var attachedMedia by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var showGifPicker by rememberSaveable { mutableStateOf(false) }
    val imageLoader = context.imageLoader

    val currentAttachedMedia by rememberUpdatedState(attachedMedia)
    DisposableEffect(Unit) {
        onDispose {
            if (activity?.isChangingConfigurations != true) {
                currentAttachedMedia.forEach { url ->
                    viewModel.deleteDraftCommentImage(url)
                }
            }
        }
    }

    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val mediaChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
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
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
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
            val cacheDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
            val photoFile = File(cacheDir, "camera_${System.currentTimeMillis()}.jpg")
            val cameraUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            pendingCameraFile = photoFile
            pendingCameraUri = cameraUri

            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val pickIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }

            val chooserTitle = context.getString(R.string.comment_attach_image_title)
            val chooserIntent = Intent.createChooser(pickIntent, chooserTitle).apply {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(captureIntent))
            }

            mediaChooserLauncher.launch(chooserIntent)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.comment_camera_error), Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (focusInputOnLaunch) {
            delay(300) // Aspetta fine animazione navigazione
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(initialImageUri) {
        if (!initialImageUri.isNullOrBlank()) {
            val uri = Uri.parse(initialImageUri)
            isUploadingImage = true
            viewModel.uploadCommentImage(
                imageUri = uri,
                onSuccess = { url ->
                    isUploadingImage = false
                    attachedMedia = listOf(url.toString())
                },
                onError = { error ->
                    isUploadingImage = false
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

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
                    CommsUniCommentsTopBar(
                        mediaTitle = mediaTitle,
                        isTvSeries = isTvSeries,
                        currentSeasonNumber = currentSeasonNumber,
                        currentEpisodeNumber = currentEpisodeNumber,
                        currentEpisodeTitle = currentEpisodeTitle,
                        hazeState = hazeState,
                        onBackClick = { navigator.pop() },
                        onCommsUniInfoClick = { bounds ->
                            commsUniButtonBounds = bounds
                            showCommsUniInfo = true
                        },
                        onSortClick = { bounds ->
                            sortButtonBounds = bounds
                            showSortMenu = true
                        },
                        onEpisodesClick = { bounds ->
                            episodesButtonBounds = bounds
                            showEpisodePickerSheet = true
                        }
                    )
                },
                containerColor = Color.Transparent
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize()) {
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

                    val flatTree = remember(comments, expandedComments.value, sortOption, sortOrder, sourceFilters, languageFilters) {
                        buildFlatCommentTree(comments, expandedComments.value, sortOption, sortOrder, sourceFilters, languageFilters)
                    }

                    LaunchedEffect(flatTree) {
                        if (targetCommentId != null && !hasScrolledToTarget && flatTree.isNotEmpty()) {
                            val index = flatTree.indexOfFirst { it.id == targetCommentId }
                            if (index != -1) {
                                delay(300)
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
                                        drawRect(Color.Black.copy(alpha = 0.88f))
                                    }
                            )
                        }

                        CommsUniCommentsList(
                            listState = listState,
                            paddingValues = paddingValues,
                            isLoading = isLoading,
                            flatTree = flatTree,
                            comments = comments,
                            expandedComments = expandedComments.value,
                            onToggleExpand = { comment ->
                                val willExpand = !expandedComments.value.contains(comment.id)
                                if (willExpand) {
                                    viewModel.loadRepliesForComment(comment.id)
                                }
                                expandedComments.value = if (expandedComments.value.contains(comment.id)) {
                                    expandedComments.value - comment.id
                                } else {
                                    expandedComments.value + comment.id
                                }
                            },
                            hasMoreComments = hasMoreComments,
                            isLoadingMore = isLoadingMore,
                            onLoadMore = { viewModel.loadMoreComments() },
                            isCommentLiked = { comment -> viewModel.isCommentLikedByMe(comment) },
                            isCommentMine = { comment -> viewModel.isCommentMine(comment) },
                            translationStates = translationStates,
                            currentUserId = viewModel.currentUserId,
                            isUserAnonymous = viewModel.isUserAnonymous,
                            accentColor = accentColor,
                            imageLoader = imageLoader,
                            shimmerBrush = shimmerBrush,
                            onToggleSpoilerAuthor = { comment ->
                                viewModel.toggleSpoilerStatus(comment.id, comment.isSpoiler)
                            },
                            onDeleteComment = { comment ->
                                commentToDelete = comment
                            },
                            onReply = { comment ->
                                replyingTo = comment
                                postToCommsUni = comment.isOnCommsUni
                            },
                            onToggleLike = { comment ->
                                viewModel.toggleLikeComment(comment.id, mediaTitle, mediaImage)
                            },
                            onReport = { comment ->
                                commentToReport = comment
                            },
                            onBlockUser = { comment ->
                                userToBlock = comment
                            },
                            onTranslate = { commentId, text ->
                                viewModel.translateComment(commentId, text)
                            },
                            onTriggerGuestAuth = {
                                guestAuthManager?.triggerGuestAuthDialog()
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
                            },
                            taggedMediaTitles = taggedMediaTitles,
                            taggedMediaPosters = taggedMediaPosters,
                            onMediaTagClick = { mType, mId ->
                                navigator.push(MovieDetailScreen(movieId = mId, mediaType = mType))
                            }
                        )
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
                        onGuestAuthTrigger = { guestAuthManager?.triggerGuestAuthDialog() },
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
                        onPickGif = { bounds ->
                            gifButtonBounds = bounds
                            showGifPicker = true
                        },
                        onSendComment = {
                            if (inputText.text.isNotBlank() || attachedMedia.isNotEmpty()) {
                                val pId = replyingTo?.id
                                val pUserId = replyingTo?.userId
                                val newDepth = (replyingTo?.depth ?: -1) + 1

                                if (pId != null) {
                                    expandedComments.value = expandedComments.value + pId
                                }

                                val cleanMessage = inputText.text.trim()
                                val messageWithTags = serializeMediaTagsForSend(
                                    text = cleanMessage,
                                    selectedTags = selectedMediaTags,
                                    maxLength = 8000
                                )

                                localFocusManager.clearFocus()
                                val effectivePostToCommsUni = when {
                                    isReplyingToCommsUni -> true
                                    isReplyingToFlickTrove -> false
                                    else -> postToCommsUni
                                }
                                viewModel.addComment(
                                    text = messageWithTags,
                                    isSpoiler = isSpoiler,
                                    parentId = pId,
                                    parentUserId = pUserId,
                                    depth = newDepth,
                                    mediaTitle = mediaTitle,
                                    mediaImage = mediaImage,
                                    postToCommsUni = effectivePostToCommsUni,
                                    attachedMedia = attachedMedia
                                )
                                inputText = TextFieldValue("")
                                selectedMediaTags = emptyList()
                                viewModel.clearMediaTagSuggestions()
                                attachedMedia = emptyList()
                                replyingTo = null
                                isSpoiler = false
                            }
                        },
                        accentColor = accentColor,
                        hazeState = hazeState,
                        focusRequester = focusRequester,
                        selectedMediaTags = selectedMediaTags,
                        onSelectedMediaTagsChanged = { selectedMediaTags = it },
                        mediaTagSuggestions = mediaTagSuggestions,
                        isSearchingMediaTags = isSearchingMediaTags,
                        onSearchMediaTags = { viewModel.searchMediaTags(it) },
                        onClearMediaTagSuggestions = { viewModel.clearMediaTagSuggestions() },
                        onOpenWritingTips = { bounds ->
                            writingTipsButtonBounds = bounds
                            showWritingTips = true
                        }
                    )
                }
            } // End of Scaffold
        } // End of Box with globalHazeState

        // Modal Overlays Host
        CommsUniCommentsModalsHost(
            showGifPicker = showGifPicker,
            gifButtonBounds = gifButtonBounds,
            onDismissGifPicker = { showGifPicker = false },
            onGifSelected = { gifUrl ->
                attachedMedia = listOf(gifUrl)
                showGifPicker = false
            },
            showSortMenu = showSortMenu,
            conversationStats = conversationStats,
            comments = comments,
            availableSources = availableSources,
            sortButtonBounds = sortButtonBounds,
            sortOption = sortOption,
            sortOrder = sortOrder,
            sourceFilters = sourceFilters,
            languageFilters = languageFilters,
            onSortChanged = { newSortOption, newSortOrder, newSourceFilters, newLanguageFilters ->
                sortOption = newSortOption
                sortOrder = newSortOrder
                sourceFilters = newSourceFilters
                languageFilters = newLanguageFilters
            },
            onDismissSortMenu = { showSortMenu = false },
            showCommsUniInfo = showCommsUniInfo,
            commsUniButtonBounds = commsUniButtonBounds,
            onDismissCommsUniInfo = { showCommsUniInfo = false },
            showWritingTips = showWritingTips,
            writingTipsButtonBounds = writingTipsButtonBounds,
            onDismissWritingTips = { showWritingTips = false },
            showTranslationPrompt = showTranslationPrompt,
            onDismissTranslationPrompt = { viewModel.dismissTranslationPrompt() },
            onTranslatePromptConfirm = { commentId, text, requireWifi ->
                viewModel.translateComment(commentId, text, requireWifi)
            },
            commentToReport = commentToReport,
            onDismissReport = { commentToReport = null },
            onReportConfirm = { category, detail ->
                commentToReport?.let { c ->
                    viewModel.reportComment(c.id, category, c.text, c.userId, c.userDisplayName, detail = detail)
                }
                commentToReport = null
            },
            onReportBlockUser = { target ->
                commentToReport = null
                userToBlock = target
            },
            userToBlock = userToBlock,
            onDismissBlock = { userToBlock = null },
            onBlockConfirm = {
                userToBlock?.let { c ->
                    viewModel.blockAuthor(c.userId, c.userDisplayName)
                }
                userToBlock = null
            },
            commentToDelete = commentToDelete,
            onDismissDelete = { commentToDelete = null },
            onDeleteConfirm = {
                commentToDelete?.let { c ->
                    viewModel.deleteComment(c.id, c)
                }
                commentToDelete = null
            },
            pendingExternalUrl = pendingExternalUrl,
            pendingExternalHost = pendingExternalHost,
            onDismissExternalLink = { pendingExternalUrl = null },
            onConfirmExternalLink = { linkUrl ->
                pendingExternalUrl = null
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                    viewModel.emitBlockedLink(R.string.external_link_error_no_browser)
                }
            },
            showEpisodePickerSheet = showEpisodePickerSheet,
            showSeasons = showSeasons,
            seasonEpisodes = seasonEpisodes,
            isLoadingSeasonEpisodes = isLoadingSeasonEpisodes,
            currentSeasonNumber = currentSeasonNumber,
            currentEpisodeNumber = currentEpisodeNumber,
            currentBackdropUrl = currentBackdropUrl,
            episodesButtonBounds = episodesButtonBounds,
            onLoadSeasonEpisodes = { sNum -> viewModel.loadSeasonEpisodes(sNum) },
            onSelectEpisode = { sNum, ep ->
                showEpisodePickerSheet = false
                currentSeasonNumber = sNum
                currentEpisodeNumber = ep.episodeNumber
                currentEpTitle = ep.name
                val epStill = ep.stillPath?.let {
                    buildTmdbImageUrl(it, ImageType.BACKDROP, ImageQuality.HIGH)
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
            onDismissEpisodePicker = { showEpisodePickerSheet = false },
            accentColor = accentColor,
            globalHazeState = globalHazeState
        )

        BackHandler(
            enabled = showEpisodePickerSheet || showWritingTips || pendingExternalUrl != null || userToBlock != null || commentToReport != null || commentToDelete != null
        ) {
            when {
                showEpisodePickerSheet -> showEpisodePickerSheet = false
                showWritingTips -> showWritingTips = false
                pendingExternalUrl != null -> pendingExternalUrl = null
                userToBlock != null -> userToBlock = null
                commentToReport != null -> commentToReport = null
                commentToDelete != null -> commentToDelete = null
            }
        }
    } // End of Outer Box
}
