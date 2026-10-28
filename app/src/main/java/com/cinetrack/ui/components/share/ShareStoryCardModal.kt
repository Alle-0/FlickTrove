package com.cinetrack.ui.components.share

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.request.ImageRequest
import com.cinetrack.R
import com.cinetrack.data.api.ImagesResponse
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.common.FlickTroveSwitch
import com.cinetrack.ui.components.common.SegmentedControl
import com.cinetrack.ui.components.common.SegmentedOption
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.shared.FlickTroveBottomSheet
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.ImageType
import com.cinetrack.util.VibrationHelper
import com.cinetrack.util.buildTmdbImageUrl
import com.google.firebase.auth.FirebaseAuth
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareStoryCardModal(
    visible: Boolean,
    movie: Movie,
    accentColor: Color,
    hazeState: HazeState? = null,
    logoPath: String? = null,
    initialImages: ImagesResponse? = null,
    allMediaImages: ImagesResponse? = null,
    onLoadAllImages: () -> Unit = {},
    onDismiss: () -> Unit,
    onShareToComments: (Uri) -> Unit
) {
    if (!visible) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val userDisplayName = remember(currentUser) { currentUser?.displayName?.takeIf { it.isNotBlank() } }
    val userPhotoUrl = remember(currentUser) { currentUser?.photoUrl?.toString() }

    var config by remember(movie) {
        mutableStateOf(
            ShareStoryCardConfig(
                coverStyle = ShareCoverStyle.POSTER_FLOATING,
                aspectRatio = ShareAspectRatio.STORY_9_16,
                showRating = (movie.personalRating ?: 0.0) > 0.0,
                showProfile = userDisplayName != null,
                showVibe = !movie.emotionalVibes.isNullOrBlank() && movie.emotionalVibes != "[]",
                showMvp = movie.favoriteActorId != null || !movie.favoriteActorName.isNullOrBlank(),
                showWatermark = true
            )
        )
    }

    var isProcessing by remember { mutableStateOf(false) }
    var isSelectingCover by remember { mutableStateOf(false) }
    var coverTab by remember { mutableStateOf(0) }

    // Preload high-res poster and backdrop immediately (including customized paths)
    LaunchedEffect(movie, config.customPosterPath, config.customBackdropPath) {
        val posterPath = config.customPosterPath ?: movie.posterPath
        val posterUrl = posterPath?.let { buildTmdbImageUrl(it, ImageType.POSTER, ImageQuality.HIGH) }
        val backdropPath = config.customBackdropPath ?: movie.customBackdropPath ?: movie.backdropPath ?: movie.posterPath
        val backdropUrl = backdropPath?.let {
            buildTmdbImageUrl(it, ImageType.BACKDROP, ImageQuality.HIGH)
        }
        val imageLoader = coil.Coil.imageLoader(context)
        posterUrl?.let { imageLoader.enqueue(ImageRequest.Builder(context).data(it).build()) }
        backdropUrl?.let { imageLoader.enqueue(ImageRequest.Builder(context).data(it).build()) }
    }

    suspend fun captureCardBitmap(): Bitmap? {
        return try {
            delay(150) // Ensure layout and high-res textures are fully rendered
            val imageBitmap = graphicsLayer.toImageBitmap()
            val androidBmp = imageBitmap.asAndroidBitmap()
            val bmpToUse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && androidBmp.config == Bitmap.Config.HARDWARE) {
                androidBmp.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                androidBmp
            }
            val targetW = config.aspectRatio.targetWidthPx
            val targetH = config.aspectRatio.targetHeightPx
            if (bmpToUse.width != targetW || bmpToUse.height != targetH) {
                Bitmap.createScaledBitmap(bmpToUse, targetW, targetH, true)
            } else {
                bmpToUse
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    FlickTroveBottomSheet(
        onDismissRequest = onDismiss,
        hazeState = hazeState,
        dragOnlyFromHandle = true
    ) {
        AnimatedContent(
            targetState = isSelectingCover,
            transitionSpec = {
                if (targetState) {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut()
                    )
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut()
                    )
                }
            },
            label = "ShareModalScreenTransition"
        ) { selectingCover ->
            if (selectingCover) {
                CoverSelectionView(
                    movie = movie,
                    accentColor = accentColor,
                    initialImages = initialImages,
                    allMediaImages = allMediaImages,
                    config = config,
                    coverTab = coverTab,
                    onCoverTabChange = { coverTab = it },
                    onConfigChange = { config = it },
                    onBack = { isSelectingCover = false }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
            // Header con solo titolo a sinistra
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_share),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.share_story_card_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // SegmentedControl centrato sopra lo show off (Poster vs Backdrop a pillola)
            SegmentedControl(
                options = listOf(
                    SegmentedOption(value = ShareCoverStyle.POSTER_FLOATING, title = stringResource(R.string.share_cover_poster)),
                    SegmentedOption(value = ShareCoverStyle.BACKDROP_FULL, title = stringResource(R.string.share_cover_backdrop))
                ),
                selected = config.coverStyle,
                accentColor = accentColor,
                vibrationEnabled = true,
                context = context,
                modifier = Modifier.width(190.dp),
                height = 30.dp,
                isPill = true,
                onSelect = { newStyle ->
                    config = config.copy(coverStyle = newStyle)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Scaled Live Preview Card (Pixel-perfect Full HD layout, scaled to preview dimensions)
            val currentRatio = config.aspectRatio
            val screenDensity = LocalDensity.current.density
            val exportDensity = currentRatio.targetWidthPx.toFloat() / currentRatio.baseWidthDp.value

            val springSpec = spring<androidx.compose.ui.unit.Dp>(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
            val floatSpringSpec = spring<Float>(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )

            val animatedPreviewWidth by animateDpAsState(
                targetValue = currentRatio.previewWidthDp,
                animationSpec = springSpec,
                label = "PreviewWidth"
            )
            val animatedRatio by animateFloatAsState(
                targetValue = currentRatio.ratio,
                animationSpec = floatSpringSpec,
                label = "PreviewRatio"
            )

            // Dynamic scale derived directly from animatedPreviewWidth to keep card width 1:1 with container
            // and eliminate asynchronous scale dips/shrinking during aspect ratio transitions
            val currentPreviewScale = (animatedPreviewWidth.value * screenDensity) / currentRatio.targetWidthPx.toFloat()

            Box(
                modifier = Modifier
                    .width(animatedPreviewWidth)
                    .aspectRatio(animatedRatio)
                    .clip(RoundedCornerShape(24.dp))
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(
                    LocalDensity provides Density(density = exportDensity, fontScale = 1f)
                ) {
                    Box(
                        modifier = Modifier
                            .requiredSize(currentRatio.baseWidthDp, currentRatio.baseHeightDp)
                            .drawWithContent {
                                graphicsLayer.record {
                                    this@drawWithContent.drawContent()
                                }
                                scale(scale = currentPreviewScale, pivot = center) {
                                    drawLayer(graphicsLayer)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        ShareStoryCard(
                            movie = movie,
                            config = config,
                            accentColor = accentColor,
                            userDisplayName = userDisplayName,
                            userPhotoUrl = userPhotoUrl,
                            logoPath = logoPath,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Aspect Ratio Selector come selettore unico a pillola
            SegmentedControl(
                options = listOf(
                    SegmentedOption(value = ShareAspectRatio.STORY_9_16, title = "9:16", subtitle = stringResource(ShareAspectRatio.STORY_9_16.subtitleResId)),
                    SegmentedOption(value = ShareAspectRatio.POST_4_5, title = "4:5", subtitle = stringResource(ShareAspectRatio.POST_4_5.subtitleResId)),
                    SegmentedOption(value = ShareAspectRatio.SQUARE_1_1, title = "1:1", subtitle = stringResource(ShareAspectRatio.SQUARE_1_1.subtitleResId)),
                    SegmentedOption(value = ShareAspectRatio.LANDSCAPE_16_9, title = "16:9", subtitle = stringResource(ShareAspectRatio.LANDSCAPE_16_9.subtitleResId))
                ),
                selected = config.aspectRatio,
                accentColor = accentColor,
                vibrationEnabled = true,
                context = context,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                height = 38.dp,
                isPill = true,
                onSelect = { newRatio ->
                    config = config.copy(aspectRatio = newRatio)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Griglia 2x2 dei controlli:
            // Riga 1: Toggle Vibe + Toggle MVP
            // Riga 2: Toggle Profilo + Pulsante Scegli
            val hasVibes = !movie.emotionalVibes.isNullOrBlank() && movie.emotionalVibes != "[]"
            val hasMvp = movie.favoriteActorId != null || !movie.favoriteActorName.isNullOrBlank()
            val hasProfile = userDisplayName != null
            val hasCustomImage = config.customPosterPath != null || config.customBackdropPath != null

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Riga 1: Toggle Vibe + Toggle MVP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Toggle Vibe
                    ShareGridToggleCard(
                        label = stringResource(R.string.share_toggle_vibe),
                        checked = config.showVibe,
                        icon = ImageVector.vectorResource(id = R.drawable.ic_sparkle),
                        accentColor = accentColor,
                        enabled = hasVibes,
                        modifier = Modifier.weight(1f),
                        onToggle = { config = config.copy(showVibe = it) }
                    )

                    // Toggle MVP
                    ShareGridToggleCard(
                        label = stringResource(R.string.share_toggle_mvp),
                        checked = config.showMvp,
                        icon = ImageVector.vectorResource(id = R.drawable.ic_star_piena),
                        accentColor = accentColor,
                        enabled = hasMvp,
                        modifier = Modifier.weight(1f),
                        onToggle = { config = config.copy(showMvp = it) }
                    )
                }

                // Riga 2: Toggle Profilo + Pulsante Scegli
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Toggle Profilo
                    ShareGridToggleCard(
                        label = stringResource(R.string.share_toggle_profile),
                        checked = config.showProfile,
                        icon = ImageVector.vectorResource(id = R.drawable.ic_persona),
                        accentColor = accentColor,
                        enabled = hasProfile,
                        modifier = Modifier.weight(1f),
                        onToggle = { config = config.copy(showProfile = it) }
                    )

                    // Pulsante Scegli / Cambia cover a pillola
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .bounceClick(scaleDown = 0.94f) {
                                onLoadAllImages()
                                coverTab = if (config.coverStyle == ShareCoverStyle.BACKDROP_FULL) 1 else 0
                                isSelectingCover = true
                            }
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (hasCustomImage) accentColor.copy(alpha = 0.20f)
                                else Color.White.copy(alpha = 0.06f)
                            )
                            .border(
                                1.dp,
                                if (hasCustomImage) accentColor.copy(alpha = 0.65f)
                                else Color.White.copy(alpha = 0.12f),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_image),
                                contentDescription = null,
                                tint = if (hasCustomImage) ColorUtils.lightenForText(accentColor, 1.35f) else Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (hasCustomImage) stringResource(R.string.share_cover_edit) else stringResource(R.string.share_cover_choose),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (hasCustomImage) ColorUtils.lightenForText(accentColor, 1.35f) else Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (hasCustomImage) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .bounceClick(scaleDown = 0.88f) {
                                        config = config.copy(customPosterPath = null, customBackdropPath = null)
                                    }
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_ricarica),
                                    contentDescription = stringResource(R.string.share_cover_reset_original),
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bottom Action Buttons in 2 Rows:
            // Row 1: [ Condividi nei commenti ] (Hero action a tutta larghezza con accento primario)
            // Row 2: [ Condividi ] [ Salva ] (Azioni secondarie affiancate)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Riga 1: Condividi nei commenti (Hero Community Action a tutta larghezza)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .bounceClick(scaleDown = 0.95f) {
                            if (isProcessing) return@bounceClick
                            isProcessing = true
                            coroutineScope.launch {
                                val bitmap = captureCardBitmap()
                                if (bitmap != null) {
                                val uri = withContext(Dispatchers.IO) {
                                    saveBitmapToCacheUri(context, bitmap, movie.displayName)
                                }
                                if (uri != null) {
                                    onDismiss()
                                    onShareToComments(uri)
                                } else {
                                    Toast.makeText(context, context.getString(R.string.share_toast_prep_error), Toast.LENGTH_SHORT).show()
                                }
                            }
                            isProcessing = false
                        }
                    }
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                accentColor,
                                accentColor.copy(alpha = 0.88f)
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(50)),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_send),
                    contentDescription = null,
                    tint = ColorUtils.contentColorForAccent(accentColor),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = stringResource(R.string.share_action_share_comments),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorUtils.contentColorForAccent(accentColor)
                )
            }

            // Riga 2: Condividi & Salva (Affiancati con stile glass frosted)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Condividi esternamente
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .bounceClick(scaleDown = 0.94f) {
                            if (isProcessing) return@bounceClick
                            isProcessing = true
                            coroutineScope.launch {
                                val bitmap = captureCardBitmap()
                                if (bitmap != null) {
                                    withContext(Dispatchers.IO) {
                                        shareBitmapExternal(context, bitmap, movie.displayName)
                                    }
                                }
                                isProcessing = false
                            }
                        }
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(50)),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_share),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.share_action_share),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                // 2. Salva in galleria
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .bounceClick(scaleDown = 0.94f) {
                            if (isProcessing) return@bounceClick
                            isProcessing = true
                            coroutineScope.launch {
                                val bitmap = captureCardBitmap()
                                if (bitmap != null) {
                                    val success = withContext(Dispatchers.IO) {
                                        saveBitmapToGallery(context, bitmap, movie.displayName)
                                    }
                                    if (success) {
                                        Toast.makeText(context, context.getString(R.string.share_toast_saved_success), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.share_toast_saved_error), Toast.LENGTH_SHORT).show()
                                    }
                                }
                                isProcessing = false
                            }
                        }
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(50)),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_scaricare),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.share_action_save),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
}
}

@Composable
private fun ShareGridToggleCard(
    label: String,
    checked: Boolean,
    icon: ImageVector,
    accentColor: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .height(36.dp)
            .bounceClick(scaleDown = 0.94f) {
                if (enabled) {
                    VibrationHelper.vibrateTick(context)
                    onToggle(!checked)
                }
            }
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = if (enabled) 0.06f else 0.02f))
            .border(
                1.dp,
                Color.White.copy(alpha = if (enabled) 0.12f else 0.05f),
                RoundedCornerShape(50)
            )
            .padding(start = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.20f),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.25f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Toggle Switch ufficiale FlickTrove in formato compatto con tocco diretto dell'utente abilitato
        FlickTroveSwitch(
            checked = checked,
            onCheckedChange = { isChecked ->
                if (enabled) {
                    VibrationHelper.vibrateTick(context)
                    onToggle(isChecked)
                }
            },
            accentColor = accentColor,
            enabled = enabled,
            trackWidth = 36.dp,
            trackHeight = 20.dp,
            baseThumbSize = 16.dp,
            padding = 2.dp
        )
    }
}

