package com.cinetrack.ui.components.share

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.request.ImageRequest
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.common.FlickTroveSwitch
import com.cinetrack.util.ImageType
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.ui.components.common.SegmentedControl
import com.cinetrack.ui.components.common.SegmentedOption
import com.cinetrack.ui.components.shared.FlickTroveBottomSheet
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.VibrationHelper
import com.google.firebase.auth.FirebaseAuth
import dev.chrisbanes.haze.HazeState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import coil.compose.SubcomposeAsyncImage
import com.cinetrack.data.api.ImagesResponse
import com.cinetrack.data.api.ImageItem
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

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
                        .padding(bottom = 8.dp)
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
                    text = "Story Card",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            // SegmentedControl centrato sopra lo show off (Poster vs Backdrop)
            SegmentedControl(
                options = listOf(
                    SegmentedOption(value = ShareCoverStyle.POSTER_FLOATING, title = "Poster"),
                    SegmentedOption(value = ShareCoverStyle.BACKDROP_FULL, title = "Backdrop")
                ),
                selected = config.coverStyle,
                accentColor = accentColor,
                vibrationEnabled = true,
                context = context,
                modifier = Modifier.width(190.dp),
                height = 30.dp,
                onSelect = { newStyle ->
                    config = config.copy(coverStyle = newStyle)
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Scaled Live Preview Card (Pixel-perfect Full HD layout, scaled to preview dimensions)
            val currentRatio = config.aspectRatio
            val screenDensity = LocalDensity.current.density
            val previewScale = (currentRatio.previewWidthDp.value * screenDensity) / currentRatio.targetWidthPx.toFloat()
            val exportDensity = currentRatio.targetWidthPx.toFloat() / currentRatio.baseWidthDp.value

            Box(
                modifier = Modifier
                    .width(currentRatio.previewWidthDp)
                    .aspectRatio(currentRatio.ratio)
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
                                scale(scale = previewScale, pivot = center) {
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

            Spacer(modifier = Modifier.height(6.dp))

            // Aspect Ratio Selector (4 formati con nome e destinazione chiaramente visibili: Storia, Post, ecc.)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ShareAspectRatio.values().forEach { ratio ->
                    val isSelected = config.aspectRatio == ratio
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .bounceClick(scaleDown = 0.94f) {
                                config = config.copy(aspectRatio = ratio)
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) accentColor.copy(alpha = 0.22f)
                                else Color.White.copy(alpha = 0.05f)
                            )
                            .border(
                                1.dp,
                                if (isSelected) accentColor.copy(alpha = 0.70f)
                                else Color.White.copy(alpha = 0.10f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = ratio.ratioName,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) ColorUtils.lightenForText(accentColor, 1.35f) else Color.White,
                            lineHeight = 12.sp
                        )
                        Text(
                            text = ratio.subtitle,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) ColorUtils.lightenForText(accentColor, 1.2f).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.60f),
                            maxLines = 1,
                            lineHeight = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

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
                        label = "Vibe",
                        checked = config.showVibe,
                        icon = ImageVector.vectorResource(id = R.drawable.ic_sparkle),
                        accentColor = accentColor,
                        enabled = hasVibes,
                        modifier = Modifier.weight(1f),
                        onToggle = { config = config.copy(showVibe = it) }
                    )

                    // Toggle MVP
                    ShareGridToggleCard(
                        label = "MVP",
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
                        label = "Profilo",
                        checked = config.showProfile,
                        icon = ImageVector.vectorResource(id = R.drawable.ic_persona),
                        accentColor = accentColor,
                        enabled = hasProfile,
                        modifier = Modifier.weight(1f),
                        onToggle = { config = config.copy(showProfile = it) }
                    )

                    // Pulsante Scegli / Cambia cover
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .bounceClick(scaleDown = 0.94f) {
                                onLoadAllImages()
                                coverTab = if (config.coverStyle == ShareCoverStyle.BACKDROP_FULL) 1 else 0
                                isSelectingCover = true
                            }
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (hasCustomImage) accentColor.copy(alpha = 0.20f)
                                else Color.White.copy(alpha = 0.06f)
                            )
                            .border(
                                1.dp,
                                if (hasCustomImage) accentColor.copy(alpha = 0.65f)
                                else Color.White.copy(alpha = 0.12f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp),
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
                                text = if (hasCustomImage) "Modifica" else "Scegli cover",
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
                                    contentDescription = "Ripristina originale",
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                                    Toast.makeText(context, "Errore nella preparazione dell'immagine", Toast.LENGTH_SHORT).show()
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
                    text = "Condividi nei commenti",
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
                        text = "Condividi",
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
                                        Toast.makeText(context, "Card salvata nella galleria!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Errore durante il salvataggio", Toast.LENGTH_SHORT).show()
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
                        text = "Salva",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
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
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = if (enabled) 0.06f else 0.02f))
            .border(
                1.dp,
                Color.White.copy(alpha = if (enabled) 0.12f else 0.05f),
                RoundedCornerShape(10.dp)
            )
            .padding(start = 10.dp, end = 8.dp),
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

        // Toggle Switch ufficiale FlickTrove in formato compatto
        FlickTroveSwitch(
            checked = checked,
            onCheckedChange = null,
            accentColor = accentColor,
            enabled = enabled,
            trackWidth = 36.dp,
            trackHeight = 20.dp,
            baseThumbSize = 16.dp,
            padding = 2.dp
        )
    }
}

/**
 * Saves the story card bitmap into MediaStore (Pictures/FlickTrove)
 */
private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String): Boolean {
    return try {
        val cleanTitle = title.replace("[^a-zA-Z0-9]".toRegex(), "_").take(30)
        val filename = "FlickTrove_${cleanTitle}_${System.currentTimeMillis()}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FlickTrove")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return false

        resolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

/**
 * Saves bitmap into cacheDir/images and opens standard Android ACTION_SEND intent
 */
private fun shareBitmapExternal(context: Context, bitmap: Bitmap, title: String) {
    try {
        val cachePath = File(context.cacheDir, "images")
        if (!cachePath.exists()) cachePath.mkdirs()

        val cleanTitle = title.replace("[^a-zA-Z0-9]".toRegex(), "_").take(30)
        val imageFile = File(cachePath, "story_${cleanTitle}_${System.currentTimeMillis()}.png")
        FileOutputStream(imageFile).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
        }

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "Guarda la mia scheda per $title su FlickTrove! 🎬")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Condividi Story Card")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Errore nella condivisione", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Saves bitmap into cacheDir/images and returns its FileProvider content Uri
 */
private fun saveBitmapToCacheUri(context: Context, bitmap: Bitmap, title: String): Uri? {
    return try {
        val cachePath = File(context.cacheDir, "images")
        if (!cachePath.exists()) cachePath.mkdirs()

        val cleanTitle = title.replace("[^a-zA-Z0-9]".toRegex(), "_").take(30)
        val imageFile = File(cachePath, "comment_card_${cleanTitle}_${System.currentTimeMillis()}.png")
        FileOutputStream(imageFile).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
        }

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun getFlagEmoji(languageCode: String?): String {
    if (languageCode.isNullOrBlank()) return "🌐"
    return when (languageCode.lowercase().trim()) {
        "it" -> "🇮🇹"
        "en" -> "🇬🇧"
        "us" -> "🇺🇸"
        "fr" -> "🇫🇷"
        "es" -> "🇪🇸"
        "de" -> "🇩🇪"
        "ja" -> "🇯🇵"
        "ko" -> "🇰🇷"
        "zh", "cn" -> "🇨🇳"
        "pt" -> "🇵🇹"
        "ru" -> "🇷🇺"
        "ar" -> "🇸🇦"
        "hi" -> "🇮🇳"
        "el" -> "🇬🇷"
        "tr" -> "🇹🇷"
        "nl" -> "🇳🇱"
        "pl" -> "🇵🇱"
        "sv" -> "🇸🇪"
        "da" -> "🇩🇰"
        "no" -> "🇳🇴"
        "fi" -> "🇫🇮"
        "cs" -> "🇨🇿"
        "hu" -> "🇭🇺"
        "ro" -> "🇷🇴"
        "th" -> "🇹🇭"
        "id" -> "🇮🇩"
        "vi" -> "🇻🇳"
        "uk" -> "🇺🇦"
        "he", "iw" -> "🇮🇱"
        "fa" -> "🇮🇷"
        "bn" -> "🇧🇩"
        "ur" -> "🇵🇰"
        "ta" -> "🇮🇳"
        "te" -> "🇮🇳"
        "ml" -> "🇮🇳"
        "mr" -> "🇮🇳"
        "gu" -> "🇮🇳"
        "kn" -> "🇮🇳"
        "tl" -> "🇵🇭"
        "ms" -> "🇲🇾"
        "sk" -> "🇸🇰"
        "bg" -> "🇧🇬"
        "hr" -> "🇭🇷"
        "sr" -> "🇷🇸"
        "sl" -> "🇸🇮"
        "et" -> "🇪🇪"
        "lv" -> "🇱🇻"
        "lt" -> "🇱🇹"
        "is" -> "🇮🇸"
        "ga" -> "🇮🇪"
        "eu" -> "🇪🇸"
        "ca" -> "🇪🇸"
        "gl" -> "🇪🇸"
        else -> "🌐"
    }
}

private fun getLanguageDisplayName(code: String): String {
    val flag = getFlagEmoji(code)
    val loc = Locale.forLanguageTag(code)
    val localizedName = loc.getDisplayLanguage(Locale.ITALIAN).replaceFirstChar { it.uppercase() }
    val display = if (localizedName.isNotBlank() && localizedName != code) localizedName else code.uppercase()
    return "$flag $display"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoverSelectionView(
    movie: Movie,
    accentColor: Color,
    initialImages: ImagesResponse?,
    allMediaImages: ImagesResponse?,
    config: ShareStoryCardConfig,
    coverTab: Int,
    onCoverTabChange: (Int) -> Unit,
    onConfigChange: (ShareStoryCardConfig) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val userLang = remember { Locale.getDefault().language.lowercase() }

    val activeImages = allMediaImages ?: initialImages
    val rawPosters = remember(activeImages?.posters, movie.posterPath) {
        val list = activeImages?.posters ?: emptyList()
        val defaultPath = movie.posterPath
        if (defaultPath != null && list.none { it.filePath == defaultPath }) {
            listOf(ImageItem(filePath = defaultPath, iso6391 = null)) + list
        } else list
    }
    val rawBackdrops = remember(activeImages?.backdrops, movie.backdropPath, movie.customBackdropPath) {
        val list = activeImages?.backdrops ?: emptyList()
        val defaultPath = movie.customBackdropPath ?: movie.backdropPath
        if (defaultPath != null && list.none { it.filePath == defaultPath }) {
            listOf(ImageItem(filePath = defaultPath, iso6391 = null)) + list
        } else list
    }

    // "togli il senza testo": filtra solo le locandine con lingua definita
    val validPosters = remember(rawPosters) {
        rawPosters.filter { !it.iso6391.isNullOrBlank() }
    }

    // Lingue disponibili per le locandine (ordinate: lingua utente prima, poi inglese, poi alfabeticamente)
    val availableLanguages = remember(validPosters, userLang) {
        val codes = validPosters.mapNotNull { it.iso6391?.lowercase() }.filter { it.isNotBlank() }.distinct()
        codes.sortedWith { a, b ->
            when {
                a == b -> 0
                a == userLang -> -1
                b == userLang -> 1
                a == "en" -> -1
                b == "en" -> 1
                else -> a.compareTo(b)
            }
        }
    }

    // Per default seleziona la lingua dell'utente ("fai apparire quelle della tua lingua")
    var selectedLanguageCode by remember(validPosters, userLang) {
        mutableStateOf(
            when {
                validPosters.any { it.iso6391.equals(userLang, ignoreCase = true) } -> userLang
                validPosters.any { it.iso6391.equals("en", ignoreCase = true) } -> "en"
                availableLanguages.isNotEmpty() -> availableLanguages.first()
                else -> "all"
            }
        )
    }

    val filteredPosters = remember(validPosters, selectedLanguageCode) {
        if (selectedLanguageCode == "all") {
            validPosters
        } else {
            validPosters.filter { it.iso6391.equals(selectedLanguageCode, ignoreCase = true) }
        }
    }

    // Per i backdrop: nessuna lingua, tutti i backdrop ("lascia stare la storia delle lingue")
    val currentDisplayList = if (coverTab == 0) filteredPosters else rawBackdrops

    val hasCustomForCurrentTab = if (coverTab == 0) config.customPosterPath != null else config.customBackdropPath != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .bounceClick(scaleDown = 0.90f) { onBack() }
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_left),
                        contentDescription = "Indietro",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Text(
                        text = "Personalizza Immagine",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = if (coverTab == 0) "Tocca una locandina per la card" else "Tocca uno sfondo per la card",
                        fontSize = 11.5.sp,
                        color = Color.White.copy(alpha = 0.60f)
                    )
                }
            }

            if (hasCustomForCurrentTab) {
                Text(
                    text = "Ripristina",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorUtils.lightenForText(accentColor, 1.35f),
                    modifier = Modifier
                        .bounceClick(scaleDown = 0.92f) {
                            if (coverTab == 0) {
                                onConfigChange(config.copy(customPosterPath = null))
                            } else {
                                onConfigChange(config.copy(customBackdropPath = null))
                            }
                            onBack()
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Tab Selector (Posters vs Backdrops)
        SegmentedControl(
            options = listOf(
                SegmentedOption(value = 0, title = "Locandine (${validPosters.size})"),
                SegmentedOption(value = 1, title = "Sfondi (${rawBackdrops.size})")
            ),
            selected = coverTab,
            accentColor = accentColor,
            vibrationEnabled = true,
            context = context,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            height = 36.dp,
            onSelect = { newTab ->
                onCoverTabChange(newTab)
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Sezione Filtro Lingua: SOLO per le Locandine (coverTab == 0) con pulsante a tendina e icona standard ic_right
        if (coverTab == 0 && availableLanguages.isNotEmpty()) {
            var languageMenuExpanded by remember { mutableStateOf(false) }
            val chevronRotation by animateFloatAsState(
                targetValue = if (languageMenuExpanded) 270f else 90f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "langChevron"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                val currentCount = if (selectedLanguageCode == "all") {
                    validPosters.size
                } else {
                    validPosters.count { it.iso6391.equals(selectedLanguageCode, ignoreCase = true) }
                }
                val currentLabel = if (selectedLanguageCode == "all") {
                    "🌐 Tutte le lingue"
                } else {
                    getLanguageDisplayName(selectedLanguageCode)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .bounceClick(scaleDown = 0.97f) { languageMenuExpanded = true }
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = currentLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "($currentCount)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.60f)
                        )
                    }

                    Icon(
                        painter = painterResource(id = R.drawable.ic_right),
                        contentDescription = "Seleziona lingua",
                        tint = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier
                            .size(15.dp)
                            .rotate(chevronRotation)
                    )
                }

                DropdownMenu(
                    expanded = languageMenuExpanded,
                    onDismissRequest = { languageMenuExpanded = false },
                    modifier = Modifier
                        .background(Color(0xFF1E1E24))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                        .heightIn(max = 280.dp)
                ) {
                    // Opzione "Tutte le lingue"
                    val totalPosters = validPosters.size
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🌐 Tutte le lingue",
                                    fontSize = 13.5.sp,
                                    fontWeight = if (selectedLanguageCode == "all") FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedLanguageCode == "all") ColorUtils.lightenForText(accentColor, 1.35f) else Color.White
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "($totalPosters)",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.50f)
                                )
                            }
                        },
                        onClick = {
                            VibrationHelper.vibrateTick(context)
                            selectedLanguageCode = "all"
                            languageMenuExpanded = false
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        thickness = 0.5.dp
                    )

                    availableLanguages.forEach { code ->
                        val count = validPosters.count { it.iso6391.equals(code, ignoreCase = true) }
                        val isCurrent = selectedLanguageCode.equals(code, ignoreCase = true)
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = getLanguageDisplayName(code),
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) ColorUtils.lightenForText(accentColor, 1.35f) else Color.White
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = "($count)",
                                        fontSize = 12.sp,
                                        color = if (isCurrent) ColorUtils.lightenForText(accentColor, 1.35f).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.50f)
                                    )
                                }
                            },
                            onClick = {
                                VibrationHelper.vibrateTick(context)
                                selectedLanguageCode = code
                                languageMenuExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Image Grid con gradiente superiore e inferiore di dissolvenza allo scroll
        // Per i backdrop: combacia la ratio con quella selezionata
        val isPosterTab = coverTab == 0
        val itemAspectRatio = if (isPosterTab) (2f / 3f) else config.aspectRatio.ratio
        val columns = if (isPosterTab || itemAspectRatio < 1f) 3 else 2

        if (currentDisplayList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_image),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Nessuna immagine disponibile",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            // Box con dissolvenza sfumata in cima e in fondo allo scorrimento
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(min = 300.dp, max = 500.dp)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.04f to Color.Black,
                                0.96f to Color.Black,
                                1f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(currentDisplayList, key = { it.filePath }) { item ->
                        val isSelected = if (isPosterTab) {
                            (config.customPosterPath ?: movie.posterPath) == item.filePath
                        } else {
                            (config.customBackdropPath ?: (movie.customBackdropPath ?: movie.backdropPath)) == item.filePath
                        }

                        val imageUrl = remember(item.filePath) {
                            val imageType = if (isPosterTab) ImageType.POSTER else ImageType.BACKDROP
                            buildTmdbImageUrl(item.filePath, imageType, ImageQuality.HIGH)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(itemAspectRatio)
                                .bounceClick(scaleDown = 0.95f) {
                                    VibrationHelper.vibrateTick(context)
                                    if (isPosterTab) {
                                        onConfigChange(config.copy(customPosterPath = item.filePath))
                                    } else {
                                        onConfigChange(config.copy(customBackdropPath = item.filePath))
                                    }
                                    onBack() // Si applica subito alla card e torna indietro
                                }
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E1E24))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) accentColor else Color.White.copy(alpha = 0.10f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                loading = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF18181B)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = accentColor.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            )

                            // Top-right selection badge
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .align(Alignment.TopEnd)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(accentColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_tick),
                                        contentDescription = "Selezionato",
                                        tint = ColorUtils.contentColorForAccent(accentColor),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
