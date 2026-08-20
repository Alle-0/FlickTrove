package com.cinetrack.ui.components.detail

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.ui.res.vectorResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
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
import com.cinetrack.R
import com.cinetrack.data.model.AppComment
import com.cinetrack.ui.utils.parseMarkdown
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
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface DetailCommentsEntryPoint {
    fun translationManager(): com.cinetrack.util.TranslationManager
    fun preferenceRepository(): com.cinetrack.data.repository.PreferenceRepository
    fun actionFeedbackManager(): com.cinetrack.ui.utils.ActionFeedbackManager
}

@Composable
fun DetailComments(
    comments: List<AppComment>,
    accentColor: Color,
    isOffline: Boolean = false,
    onOpenThread: (Boolean) -> Unit,
    onLikeClick: (String) -> Unit,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.bounceClick { onOpenThread(false) }
            ) {
                Text(
                    text = stringResource(R.string.detail_top_comments),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp
                    ),
                    color = Color.White.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_right),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(12.dp)
                )
            }
            
            Text(
                text = stringResource(R.string.comment_write_btn),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = accentColor,
                modifier = Modifier
                    .bounceClick { onOpenThread(true) }
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        val sortedComments = remember(comments) {
            comments.filter { 
                val isEffectivelyDeleted = it.isDeleted || it.userId.isBlank() || (it.text.isBlank() && it.userDisplayName.isBlank())
                it.depth == 0 && !isEffectivelyDeleted 
            }.sortedByDescending { it.likesCount }.take(5)
        }

        if (sortedComments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .bounceClick { onOpenThread(true) }
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isOffline) stringResource(R.string.comment_no_connection) else stringResource(R.string.comment_empty_state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                    accentColor = accentColor,
                    onLikeClick = onLikeClick
                )
            }
            item {
                Box(
                    modifier = Modifier
                        .height(140.dp)
                        .padding(start = 8.dp)
                        .clickable { onOpenThread(false) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.comment_view_full_thread),
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
    accentColor: Color,
    onLikeClick: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isSpoilerRevealed by remember { mutableStateOf(false) }
    var tapOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val revealRadius = remember { androidx.compose.animation.core.Animatable(0f) }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val expandedWidth = (configuration.screenWidthDp * 0.85f).dp
    val targetWidth = if (isExpanded) expandedWidth else 280.dp

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val entryPoint = remember(context) {
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext,
            DetailCommentsEntryPoint::class.java
        )
    }
    var translationState by remember { mutableStateOf<com.cinetrack.ui.viewmodel.CommentsViewModel.TranslationState>(com.cinetrack.ui.viewmodel.CommentsViewModel.TranslationState.Idle) }
    var showPrompt by remember { mutableStateOf<Pair<String, String>?>(null) } // <commentId, cleanText>

    fun confirmTranslation(requireWifi: Boolean, cleanText: String, effectiveSourceLang: String, targetMlKit: String) {
        coroutineScope.launch {
            val translationManager = entryPoint.translationManager()
            val actionFeedbackManager = entryPoint.actionFeedbackManager()
            translationState = com.cinetrack.ui.viewmodel.CommentsViewModel.TranslationState.Downloading
            val downloaded = translationManager.downloadModels(effectiveSourceLang, targetMlKit, requireWifi)
            if (!downloaded) {
                translationState = com.cinetrack.ui.viewmodel.CommentsViewModel.TranslationState.Error
                actionFeedbackManager.emit(com.cinetrack.ui.utils.UiText.StringResource(R.string.msg_error_lang_model))
                return@launch
            }
            translationState = com.cinetrack.ui.viewmodel.CommentsViewModel.TranslationState.Translating
            val translated = translationManager.translateFrom(cleanText, effectiveSourceLang, targetMlKit)
            if (translated != null && translated.trim().lowercase() != cleanText.trim().lowercase()) {
                translationState = com.cinetrack.ui.viewmodel.CommentsViewModel.TranslationState.Translated(translated)
            } else {
                actionFeedbackManager.emit(com.cinetrack.ui.utils.UiText.StringResource(R.string.comment_already_in_language))
                translationState = com.cinetrack.ui.viewmodel.CommentsViewModel.TranslationState.Idle
            }
        }
    }

    fun handleTranslate() {
        if (translationState is com.cinetrack.ui.viewmodel.CommentsViewModel.TranslationState.Translated) {
            translationState = com.cinetrack.ui.viewmodel.CommentsViewModel.TranslationState.Idle
            return
        }
        coroutineScope.launch {
            val mediaRegex = Regex("!\\[(?:gif|foto)\\]\\((.*?)\\)")
            val cleanText = comment.text.replace(mediaRegex, "").trim()
            if (cleanText.isBlank()) return@launch

            val translationManager = entryPoint.translationManager()
            val preferenceRepository = entryPoint.preferenceRepository()
            val actionFeedbackManager = entryPoint.actionFeedbackManager()

            val prefs = preferenceRepository.userPreferencesFlow.first()
            val systemLang = java.util.Locale.getDefault().language
            translationManager.setTargetLanguage(prefs.contentLanguage, systemLang)

            val targetMlKit = translationManager.getCurrentTargetLanguage()
            val targetBcp47 = translationManager.mapMlKitToBcp47(targetMlKit)

            val detectedLang = translationManager.identifyLanguage(cleanText)
            if (detectedLang != null && (detectedLang == targetBcp47 || detectedLang == targetMlKit)) {
                actionFeedbackManager.emit(com.cinetrack.ui.utils.UiText.StringResource(R.string.comment_already_in_language))
                return@launch
            }

            val effectiveSourceLang = detectedLang ?: if (targetMlKit != com.google.mlkit.nl.translate.TranslateLanguage.ENGLISH) {
                com.google.mlkit.nl.translate.TranslateLanguage.ENGLISH
            } else {
                com.google.mlkit.nl.translate.TranslateLanguage.ITALIAN
            }

            val modelReady = translationManager.isModelDownloaded(effectiveSourceLang, targetMlKit)
            if (!modelReady) {
                showPrompt = Pair(comment.id, cleanText)
                return@launch
            }

            confirmTranslation(false, cleanText, effectiveSourceLang, targetMlKit)
        }
    }

    DetailTranslationPromptModal(
        showTranslationPrompt = showPrompt,
        onDismiss = { showPrompt = null },
        onTranslate = { _, text, requireWifi ->
            showPrompt = null
            coroutineScope.launch {
                val translationManager = entryPoint.translationManager()
                val preferenceRepository = entryPoint.preferenceRepository()
                val prefs = preferenceRepository.userPreferencesFlow.first()
                val systemLang = java.util.Locale.getDefault().language
                translationManager.setTargetLanguage(prefs.contentLanguage, systemLang)
                val targetMlKit = translationManager.getCurrentTargetLanguage()
                val detectedLang = translationManager.identifyLanguage(text)
                val effectiveSourceLang = detectedLang ?: if (targetMlKit != com.google.mlkit.nl.translate.TranslateLanguage.ENGLISH) {
                    com.google.mlkit.nl.translate.TranslateLanguage.ENGLISH
                } else {
                    com.google.mlkit.nl.translate.TranslateLanguage.ITALIAN
                }
                confirmTranslation(requireWifi, text, effectiveSourceLang, targetMlKit)
            }
        },
        accentColor = accentColor
    )

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
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.bounceClick(scaleDown = 0.8f) { onLikeClick(comment.id) }
                ) {
                    val currentUserId = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid }
                    val isLiked = currentUserId != null && comment.likedBy.contains(currentUserId)

                    com.cinetrack.ui.screens.LiquidStarIcon(
                        isLiked = isLiked,
                        accentColor = accentColor,
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
                    
                    Spacer(modifier = Modifier.width(10.dp))
                    
                    // Translate button in top right
                    when (translationState) {
                        is com.cinetrack.ui.viewmodel.CommentsViewModel.TranslationState.Downloading,
                        is com.cinetrack.ui.viewmodel.CommentsViewModel.TranslationState.Translating -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = accentColor
                            )
                        }
                        is com.cinetrack.ui.viewmodel.CommentsViewModel.TranslationState.Translated -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_traduzione),
                                contentDescription = stringResource(R.string.comment_show_original),
                                tint = accentColor,
                                modifier = Modifier
                                    .size(14.dp)
                                    .bounceClick { handleTranslate() }
                            )
                        }
                        else -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_traduzione),
                                contentDescription = stringResource(R.string.comment_translate),
                                tint = Color.White.copy(alpha = 0.55f),
                                modifier = Modifier
                                    .size(14.dp)
                                    .bounceClick { handleTranslate() }
                            )
                        }
                    }
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
                    },
                contentAlignment = Alignment.Center
            ) {
                val currentTranslation = translationState
                val displayedTextRaw = when (currentTranslation) {
                    is com.cinetrack.ui.viewmodel.CommentsViewModel.TranslationState.Translated -> currentTranslation.text
                    else -> comment.text
                }
                val mediaRegex = Regex("!\\[(?:gif|foto)\\]\\((.*?)\\)")
                val textWithoutMedia = displayedTextRaw.replace(mediaRegex, "").trim()
                val mediaUrls = mediaRegex.findAll(displayedTextRaw).map { it.groupValues[1] }.toList()
                val contentToDraw = @Composable { isBlurred: Boolean ->
                    Column(
                        modifier = Modifier
                            .nestedScroll(nestedScrollConnection)
                            .verticalScroll(scrollState)
                            .padding(vertical = 4.dp)
                    ) {
                        if (textWithoutMedia.isNotEmpty() || mediaUrls.isEmpty()) {
                            val textToDisplay = if (mediaUrls.isNotEmpty()) textWithoutMedia else displayedTextRaw
                            Text(
                                text = textToDisplay.parseMarkdown(),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    lineHeight = 18.sp,
                                    fontSize = 13.sp
                                ),
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.then(
                                    if (isBlurred) Modifier.clip(RoundedCornerShape(8.dp)).blur(16.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded) else Modifier
                                )
                            )
                        }

                        if (mediaUrls.isNotEmpty()) {
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
                                        animationSpec = androidx.compose.animation.core.tween(600, easing = androidx.compose.animation.core.FastOutSlowInEasing)
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
                            enter = androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(200)),
                            modifier = Modifier.matchParentSize()
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(Color(0xFF141414).copy(alpha = 0.88f), RoundedCornerShape(8.dp))
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_eye),
                                        contentDescription = "Rivela",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    val showText = textWithoutMedia.length >= 20 || mediaUrls.isNotEmpty()
                                    if (showText) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.comment_tap_to_reveal),
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        contentToDraw(false)
                    }
                }
            }
        }
    }
}
