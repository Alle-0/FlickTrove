package com.cinetrack.ui.components.detail

import com.cinetrack.R
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.cinetrack.ui.components.shared.FolderPickerModalContent
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.DetailEvent
import com.cinetrack.ui.viewmodel.DetailUiState
import com.cinetrack.ui.viewmodel.MovieDetailViewModel
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.ImageType
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.toComposeColor
import dev.chrisbanes.haze.HazeState
import com.cinetrack.ui.components.glass.hazeGlass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult

@Composable
fun DetailMorphingTopBar(
    successState: DetailUiState.Success?,
    viewModel: MovieDetailViewModel,
    localHazeState: HazeState,
    symbioteProgress: Float,
    detailStackDepth: Int,
    currentImageQuality: ImageQuality,
    showFolderPicker: Boolean,
    onFolderPickerChange: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // ── Press states for top bar buttons ──────────────────────────────────────
    var isBackPressed by remember { mutableStateOf(false) }
    val backIconScale by animateFloatAsState(
        targetValue = if (isBackPressed) 0.88f else 1f,
        animationSpec = spring(
            stiffness = if (isBackPressed) 10000f else Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "BackIconScale"
    )

    var isFolderButtonPressed by remember { mutableStateOf(false) }
    val folderIconScale by animateFloatAsState(
        targetValue = if (isFolderButtonPressed) 0.88f else 1f,
        animationSpec = spring(
            stiffness = if (isFolderButtonPressed) 10000f else Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "FolderIconScale"
    )

    var isShareButtonPressed by remember { mutableStateOf(false) }
    val shareIconScale by animateFloatAsState(
        targetValue = if (isShareButtonPressed) 0.88f else 1f,
        animationSpec = spring(
            stiffness = if (isShareButtonPressed) 10000f else Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "ShareIconScale"
    )

    // ── Folder color brush ─────────────────────────────────────────────────────
    val movieInFolders = successState?.folders?.filter { folder ->
        val itemId = "${viewModel.mediaType}_${viewModel.movieId}"
        folder.itemIds.contains(itemId)
    } ?: emptyList()

    val folderColors = movieInFolders.map { it.color.toComposeColor() }

    // ── Morphing transition ────────────────────────────────────────────────────
    val transition = updateTransition(targetState = showFolderPicker, label = "FolderMorph")
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val collapsedPillWidth = screenWidth - 40.dp

    val modalWidth by transition.animateDp(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow) },
        label = "width"
    ) { collapsedPillWidth }

    val modalHeight by transition.animateDp(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow) },
        label = "height"
    ) { if (it) 500.dp else 44.dp }

    val modalCorner by transition.animateDp(label = "corner") { if (it) 28.dp else 22.dp }

    val modalExpansionProgress by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow) },
        label = "modalExpansion"
    ) { if (it) 1f else 0f }

    val contentAlpha by transition.animateFloat(
        transitionSpec = {
            if (targetState) tween(durationMillis = 200, delayMillis = 150)
            else tween(durationMillis = 150)
        },
        label = "contentAlpha"
    ) { if (it) 1f else 0f }

    val scrimAlpha by transition.animateFloat(label = "scrim") { if (it) 1f else 0f }

    // ── Dismiss scrim ──────────────────────────────────────────────────────────
    if (scrimAlpha > 0.01f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f)
                .background(Color.Black.copy(alpha = scrimAlpha * 0.6f))
                .pointerInput(Unit) { detectTapGestures { onFolderPickerChange(false) } }
        )
    }

    val currentEffectiveProgress = (symbioteProgress.coerceAtLeast(modalExpansionProgress)).coerceIn(0f, 1f)

    // ── Symbiote pill shape ────────────────────────────────────────────────────
    val symbioteShape: Shape = remember(currentEffectiveProgress, density, modalCorner) {
        GenericShape { size, _ ->
            val circleSize = with(density) { 44.dp.toPx() }
            val progress = currentEffectiveProgress
            val pillWidth = size.width
            val pillHeight = size.height
            val radius = with(density) { modalCorner.toPx() }

            if (progress <= 0.01f && pillHeight <= with(density) { 45.dp.toPx() }) return@GenericShape

            val stretchWidth = circleSize + (pillWidth / 2f - circleSize) * progress
            val p4 = progress * progress * progress * progress
            val innerRadius = radius * (1f - p4)

            val pathLeft = androidx.compose.ui.graphics.Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = 0f, top = 0f, right = stretchWidth + 2f, bottom = pillHeight,
                        topLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
                        topRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(innerRadius),
                        bottomRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(innerRadius),
                        bottomLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(radius)
                    )
                )
            }
            val pathRight = androidx.compose.ui.graphics.Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = pillWidth - stretchWidth - 2f, top = 0f, right = pillWidth, bottom = pillHeight,
                        topLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(innerRadius),
                        topRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
                        bottomRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
                        bottomLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(innerRadius)
                    )
                )
            }
            addPath(
                androidx.compose.ui.graphics.Path.combine(
                    PathOperation.Union, pathLeft, pathRight
                )
            )
        }
    }

    // ── Main morphing container ────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(11f)
            .statusBarsPadding()
            .displayCutoutPadding()
            .padding(top = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier.size(width = modalWidth, height = modalHeight),
            contentAlignment = Alignment.TopCenter
        ) {
            // Glass background
            if (currentEffectiveProgress > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            clip = true
                            shape = symbioteShape
                        }
                        .hazeGlass(
                            state = localHazeState,
                            shape = symbioteShape,
                            useOffscreenStrategy = true
                        )
                )
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // ── Pill header row (44dp) ─────────────────────────────────────
                val iconBrush = when {
                    folderColors.isEmpty() -> SolidColor(Color.White)
                    folderColors.size == 1 -> SolidColor(folderColors.first())
                    else -> Brush.linearGradient(folderColors)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    // Left: Back + Home
                    Row(
                        modifier = Modifier.align(Alignment.CenterStart),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Back button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isBackPressed = true
                                            try { awaitRelease() } finally { isBackPressed = false }
                                        },
                                        onTap = {
                                            if (showFolderPicker) onFolderPickerChange(false)
                                            else onBackClick()
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentEffectiveProgress <= 0.01f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .hazeGlass(
                                            state = localHazeState,
                                            shape = CircleShape,
                                            blurRadius = HazeStyles.SmallGlassBlurRadius,
                                            useOffscreenStrategy = true
                                        )
                                )
                            }
                            androidx.compose.material3.Icon(
                                imageVector = GraphicsLayerVector(R.drawable.ic_left),
                                contentDescription = stringResource(R.string.detail_content_desc_back),
                                tint = Color.White,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer {
                                        scaleX = backIconScale
                                        scaleY = backIconScale
                                    }
                            )
                        }

                        // Home FAB — visible from 3rd detail screen onwards
                        val homeButtonVisible = detailStackDepth >= 3
                        val homeButtonAlpha by animateFloatAsState(
                            targetValue = if (homeButtonVisible) 1f else 0f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "HomeFabAlpha"
                        )
                        val homeButtonScale by animateFloatAsState(
                            targetValue = if (homeButtonVisible) 1f else 0.6f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "HomeFabScale"
                        )
                        if (homeButtonAlpha > 0.01f) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .graphicsLayer {
                                        alpha = homeButtonAlpha
                                        scaleX = homeButtonScale
                                        scaleY = homeButtonScale
                                    }
                                    .then(
                                        if (currentEffectiveProgress <= 0.01f) {
                                            Modifier.hazeGlass(
                                                state = localHazeState,
                                                shape = CircleShape,
                                                blurRadius = HazeStyles.SmallGlassBlurRadius,
                                                useOffscreenStrategy = true
                                            )
                                        } else Modifier
                                    )
                                    .bounceClick { onHomeClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = GraphicsLayerVector(R.drawable.ic_home),
                                    contentDescription = stringResource(R.string.detail_content_desc_home),
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Center: title area
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxHeight()
                            .padding(horizontal = 90.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val movieTitleAlpha = if (showFolderPicker) {
                            (1f - modalExpansionProgress).coerceIn(0f, 1f)
                        } else {
                            ((symbioteProgress - 0.7f) / 0.3f).coerceIn(0f, 1f)
                        }

                        if (movieTitleAlpha > 0.01f || (showFolderPicker && modalExpansionProgress < 1f)) {
                            Text(
                                text = successState?.movieEntry?.displayName ?: "",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .alpha(movieTitleAlpha)
                                    .graphicsLayer {
                                        scaleX = 0.9f + (0.1f * movieTitleAlpha)
                                        scaleY = 0.9f + (0.1f * movieTitleAlpha)
                                        translationY = 10f * (1f - movieTitleAlpha)
                                    }
                            )
                        }

                        if (modalExpansionProgress > 0.01f) {
                            Text(
                                text = stringResource(R.string.detail_manage_folders),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier
                                    .alpha(modalExpansionProgress)
                                    .graphicsLayer { translationY = 10f * (1f - modalExpansionProgress) }
                            )
                        }
                    }

                    // Right: Share + Folder
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Share button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .alpha(if (successState != null) 1f else 0f)
                                .then(
                                    if (successState != null) {
                                        Modifier.pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = {
                                                    isShareButtonPressed = true
                                                    try { awaitRelease() } finally { isShareButtonPressed = false }
                                                },
                                                onTap = {
                                                    scope.launch(Dispatchers.IO) {
                                                        val imageUrl = buildTmdbImageUrl(
                                                            successState.movieEntry.posterPath ?: successState.movieEntry.backdropPath,
                                                            ImageType.POSTER,
                                                            currentImageQuality
                                                        )
                                                        val fileUri = if (imageUrl != null) {
                                                            val request = coil.request.ImageRequest.Builder(context)
                                                                .data(imageUrl).build()
                                                            val result = context.imageLoader.execute(request)
                                                            if (result is coil.request.SuccessResult) {
                                                                val bitmap = (result.drawable as android.graphics.drawable.BitmapDrawable).bitmap
                                                                val imagesDir = java.io.File(context.cacheDir, "images")
                                                                imagesDir.mkdirs()
                                                                val file = java.io.File(imagesDir, "share_poster.jpg")
                                                                val fos = java.io.FileOutputStream(file)
                                                                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, fos)
                                                                fos.close()
                                                                androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                                            } else null
                                                        } else null

                                                        withContext(Dispatchers.Main) {
                                                            val sendIntent = android.content.Intent().apply {
                                                                action = android.content.Intent.ACTION_SEND
                                                                val link = "flicktrove://detail/${successState.movieEntry.mediaType}/${successState.movieEntry.id}"
                                                                putExtra(android.content.Intent.EXTRA_TEXT, context.getString(R.string.detail_share_text, successState.movieEntry.displayName, link))
                                                                if (fileUri != null) {
                                                                    putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                                                                    type = "image/jpeg"
                                                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                                } else {
                                                                    type = "text/plain"
                                                                }
                                                            }
                                                            context.startActivity(android.content.Intent.createChooser(sendIntent, null))
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentEffectiveProgress <= 0.01f && successState != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .hazeGlass(
                                            state = localHazeState,
                                            shape = CircleShape,
                                            blurRadius = HazeStyles.SmallGlassBlurRadius,
                                            useOffscreenStrategy = true
                                        )
                                )
                            }
                            androidx.compose.material3.Icon(
                                imageVector = GraphicsLayerVector(R.drawable.ic_share),
                                contentDescription = stringResource(R.string.detail_content_desc_share),
                                tint = Color.White,
                                modifier = Modifier
                                    .offset(x = (-0.5).dp)
                                    .size(15.dp)
                                    .graphicsLayer {
                                        scaleX = shareIconScale
                                        scaleY = shareIconScale
                                    }
                            )
                        }

                        // Folder button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .alpha(if (successState != null) 1f else 0f)
                                .then(
                                    if (successState != null) {
                                        Modifier.pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = {
                                                    isFolderButtonPressed = true
                                                    try { awaitRelease() } finally { isFolderButtonPressed = false }
                                                },
                                                onTap = { onFolderPickerChange(!showFolderPicker) }
                                            )
                                        }
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentEffectiveProgress <= 0.01f && successState != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .hazeGlass(
                                            state = localHazeState,
                                            shape = CircleShape,
                                            blurRadius = HazeStyles.SmallGlassBlurRadius,
                                            useOffscreenStrategy = true
                                        )
                                )
                            }
                            androidx.compose.material3.Icon(
                                imageVector = GraphicsLayerVector(R.drawable.ic_cartella_piena),
                                contentDescription = stringResource(R.string.detail_content_desc_folder),
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer(alpha = 0.99f)
                                    .graphicsLayer {
                                        scaleX = folderIconScale
                                        scaleY = folderIconScale
                                    }
                                    .drawWithCache {
                                        onDrawWithContent {
                                            drawContent()
                                            drawRect(iconBrush, blendMode = BlendMode.SrcAtop)
                                        }
                                    },
                                tint = Color.White
                            )
                        }
                    }
                }

                // ── Modal content: folder picker ───────────────────────────────
                if (contentAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .alpha(contentAlpha)
                    ) {
                        if (successState != null) {
                            FolderPickerModalContent(
                                folders = successState.folders,
                                isItemInFolder = { folderId: String ->
                                    val itemId = "${viewModel.mediaType}_${viewModel.movieId}"
                                    successState.folders.find { it.id == folderId }?.itemIds?.contains(itemId) ?: false
                                },
                                onToggleItem = { folder -> viewModel.onEvent(DetailEvent.ToggleFolderMembership(folder)) },
                                onCreateFolder = { name: String, color: String -> viewModel.onEvent(DetailEvent.CreateFolder(name, color)) },
                                onClose = { onFolderPickerChange(false) },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper to create an [ImageVector] from a drawable resource ID inside a composable.
 */
@Composable
private fun GraphicsLayerVector(resId: Int): androidx.compose.ui.graphics.vector.ImageVector =
    androidx.compose.ui.graphics.vector.ImageVector.vectorResource(id = resId)
