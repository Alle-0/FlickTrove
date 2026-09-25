package com.cinetrack.ui.components.recommendations

import com.cinetrack.R
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.toHexString
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.toComposeColor
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.launch
import kotlin.math.abs

fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + fraction * (stop - start)
}

@Composable
fun FlickMovieCard(
    movie: Movie,
    isTop: Boolean,
    scale: Float,
    yOffset: Float,
    alpha: Float,
    swipeOffsetX: Float,
    swipeOffsetY: Float,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onResolveLogo: (suspend (Movie) -> String?)? = null,
    onActionClick: () -> Unit
) {
    val rotation = if (isTop) swipeOffsetX / 22f else 0f

    var localLogoPath by remember(movie.id, movie.logoPath) { mutableStateOf(movie.logoPath) }
    LaunchedEffect(movie.id, movie.logoPath) {
        if (localLogoPath.isNullOrEmpty()) {
            val fetched = onResolveLogo?.invoke(movie)
            if (!fetched.isNullOrEmpty()) {
                localLogoPath = fetched
                movie.logoPath = fetched
            }
        } else {
            localLogoPath = movie.logoPath
        }
    }

    var dominantColor by remember(movie.id) { mutableStateOf<Color?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val fallbackColor = MaterialTheme.colorScheme.primary
    val rawTargetColor = movie.accentColor?.toComposeColor() ?: dominantColor ?: fallbackColor
    val vividAccent = remember(rawTargetColor) {
        ColorUtils.ensureVividAccent(rawTargetColor)
    }
    val animatedAccent by animateColorAsState(
        targetValue = vividAccent,
        animationSpec = tween(500),
        label = "flickCardAccent"
    )

    val baseDarkColor = remember { Color(0xFF09090D) }
    val cardBottomColor = remember(animatedAccent) {
        androidx.compose.ui.graphics.lerp(animatedAccent, baseDarkColor, 0.78f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .graphicsLayer {
                this.scaleX = scale
                this.scaleY = scale
                this.translationY = yOffset.dp.toPx()
                if (isTop) {
                    this.translationX = swipeOffsetX
                    this.translationY = swipeOffsetY
                    this.rotationZ = rotation
                }
                this.alpha = alpha
                this.shadowElevation = if (isTop) 16f else 4f
                this.shape = RoundedCornerShape(48.dp)
                this.clip = true
            }
            .clip(RoundedCornerShape(48.dp))
            .then(
                if (isTop) {
                    Modifier.pointerInput(movie.id) {
                        detectDragGestures(
                            onDragEnd = onDragEnd,
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount)
                            }
                        )
                    }
                } else Modifier
            )
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(48.dp))
            .background(cardBottomColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onActionClick() }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            val backdropUrl = buildTmdbImageUrl(
                movie.backdropPath ?: movie.posterPath,
                ImageType.BACKDROP,
                LocalImageQuality.current
            )
            if (backdropUrl != null) {
                val context = LocalContext.current
                val imageRequest = remember(backdropUrl) {
                    ImageRequest.Builder(context)
                        .data(backdropUrl)
                        .crossfade(true)
                        .allowHardware(false)
                        .build()
                }
                AsyncImage(
                    model = imageRequest,
                    contentDescription = movie.title ?: movie.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onSuccess = { result ->
                        val bitmap = result.result.drawable.toBitmap()
                        coroutineScope.launch {
                            val color = ColorUtils.extractAccentColor(bitmap, useBottomHalf = true)
                            if (color != Color.Unspecified) {
                                dominantColor = color
                                movie.accentColor = color.toHexString()
                            }
                        }
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF2E2E32), cardBottomColor)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_ciack),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(80.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.30f to Color.Transparent,
                                0.50f to cardBottomColor.copy(alpha = 0.45f),
                                0.70f to cardBottomColor.copy(alpha = 0.88f),
                                0.88f to cardBottomColor.copy(alpha = 0.98f),
                                1.0f to cardBottomColor
                            )
                        )
                    )
            )
        }

        if (isTop) {
            val dragProgress = swipeOffsetX / 180f

            if (dragProgress > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 40.dp, start = 32.dp)
                        .graphicsLayer {
                            rotationZ = -15f
                            this.alpha = dragProgress.coerceIn(0f, 1f)
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .border(width = 2.5.dp, color = Color(0xFF22C55E), shape = CircleShape)
                    )
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Text(
                            text = stringResource(R.string.recommendations_add),
                            color = Color(0xFF22C55E),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }

            if (dragProgress < 0f) {
                val absoluteProgress = abs(dragProgress)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 40.dp, end = 32.dp)
                        .graphicsLayer {
                            rotationZ = 15f
                            this.alpha = absoluteProgress.coerceIn(0f, 1f)
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .border(width = 2.5.dp, color = Color(0xFFEF4444), shape = CircleShape)
                    )
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Text(
                            text = stringResource(R.string.recommendations_pass),
                            color = Color(0xFFEF4444),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        }

        // Bottom info content directly integrated on the backdrop gradient
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            // Movie Title or Clear Logo
            val activeLogo = localLogoPath ?: movie.logoPath
            if (!activeLogo.isNullOrEmpty()) {
                val logoUrl = buildTmdbImageUrl(
                    activeLogo,
                    ImageType.LOGO,
                    LocalImageQuality.current
                )
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(logoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = movie.title ?: movie.name,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .heightIn(min = 38.dp, max = 70.dp)
                )
            } else {
                Text(
                    text = movie.title ?: movie.name ?: stringResource(R.string.recommendations_no_title),
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    lineHeight = 30.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata Row: Match Score + Rating
            val rating = movie.voteAverage ?: 0.0
            val year = movie.releaseYear ?: movie.releaseDate?.take(4)
                ?: movie.firstAirDate?.take(4)
            val matchScore = movie.matchScore

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Match Score Pill — sfondo più pieno per leggibilità su qualsiasi backdrop
                if (matchScore != null && matchScore > 0) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(animatedAccent.copy(alpha = 0.30f))
                            .border(0.5.dp, animatedAccent.copy(alpha = 0.75f), CircleShape)
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${matchScore}%",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.match_score).uppercase(),
                                color = Color.White.copy(alpha = 0.90f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // Rating pulito con stella piena (senza pillola per massima armonia con il Match)
                if (rating > 0.0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_star_piena),
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f", rating),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Anno • Genere • Genere (testo plain, senza pillole)
            val contextLocale = LocalConfiguration.current.locales[0].language
            val genreList = remember(movie.genreIds, movie.genreNamesString, movie.genres, contextLocale) {
                if (!movie.genreIds.isNullOrEmpty()) {
                    movie.genreIds!!.mapNotNull { id ->
                        val defaultName = com.cinetrack.data.model.GenreConstants.ALL_GENRES.find { it.id == id }?.name ?: ""
                        val localized = com.cinetrack.data.model.GenreConstants.getLocalizedName(id, contextLocale, defaultName)
                        localized.takeIf { it.isNotBlank() }
                    }
                } else if (!movie.genreNamesString.isNullOrEmpty()) {
                    movie.genreNamesString!!.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                } else {
                    movie.genres?.mapNotNull { it.name?.takeIf { n -> n.isNotBlank() } } ?: emptyList()
                }
            }

            val metaText = remember(year, genreList) {
                buildAnnotatedString {
                    if (!year.isNullOrEmpty()) {
                        withStyle(
                            SpanStyle(
                                color = Color.White.copy(alpha = 0.95f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        ) {
                            append(year)
                        }
                    }
                    if (!year.isNullOrEmpty() && genreList.isNotEmpty()) {
                        withStyle(
                            SpanStyle(
                                color = Color.White.copy(alpha = 0.40f),
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        ) {
                            append("  •  ")
                        }
                    }
                    if (genreList.isNotEmpty()) {
                        val genresFormatted = genreList.take(3).joinToString(", ") { genre ->
                            genre.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        }
                        withStyle(
                            SpanStyle(
                                color = Color.White.copy(alpha = 0.65f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.5.sp
                            )
                        ) {
                            append(genresFormatted)
                        }
                    }
                }
            }

            if (metaText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = metaText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Overview Synopsis
            val overview = movie.overview
            if (!overview.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = overview,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun FlickControls(
    onPass: () -> Unit,
    onInfo: () -> Unit,
    onLike: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color(0xFF1C1C1E).copy(alpha = 0.75f), CircleShape)
                .border(BorderStroke(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.5f)), CircleShape)
                .bounceClick(scaleDown = 0.88f) { onPass() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_x),
                contentDescription = stringResource(R.string.recommendations_desc_pass),
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(26.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFF1C1C1E).copy(alpha = 0.75f), CircleShape)
                .border(BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f)), CircleShape)
                .bounceClick(scaleDown = 0.9f) { onInfo() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_documento),
                contentDescription = stringResource(R.string.recommendations_desc_info),
                tint = Color(0xFF3B82F6),
                modifier = Modifier.size(22.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color(0xFF1C1C1E).copy(alpha = 0.75f), CircleShape)
                .border(BorderStroke(1.5.dp, Color(0xFF22C55E).copy(alpha = 0.5f)), CircleShape)
                .bounceClick(scaleDown = 0.88f) { onLike() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_star),
                contentDescription = stringResource(R.string.recommendations_desc_favorite),
                tint = Color(0xFF22C55E),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun FlickEmptyState(
    onRefresh: () -> Unit,
    localHazeState: HazeState
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .hazeGlass(
                        state = localHazeState,
                        shape = RoundedCornerShape(24.dp),
                        blurRadius = 16.dp,
                        containerColor = Color.Black.copy(alpha = 0.25f),
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
                    .graphicsLayer { },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_ricarica),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.recommendations_completed_title),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.recommendations_completed_desc),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {},
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(48.dp)
                        .bounceClick(scaleDown = 0.94f) { onRefresh() }
                ) {
                    Text(
                        text = stringResource(R.string.recommendations_reload),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


