package com.cinetrack.ui.components.recommendations

import com.cinetrack.R
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
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.buildTmdbImageUrl
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
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
    onActionClick: () -> Unit
) {
    val rotation = if (isTop) swipeOffsetX / 22f else 0f
    val cardHazeState = remember { HazeState() }

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
            .background(Color(0xFF161618))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onActionClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(state = cardHazeState, style = HazeStyles.PremiumDark)
        ) {
            val posterUrl = buildTmdbImageUrl(movie.posterPath, ImageType.POSTER, LocalImageQuality.current)
            if (posterUrl != null) {
                val context = LocalContext.current
                val imageRequest = remember(posterUrl) {
                    ImageRequest.Builder(context)
                        .data(posterUrl)
                        .crossfade(false)
                        .build()
                }
                AsyncImage(
                    model = imageRequest,
                    contentDescription = movie.title ?: movie.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF2E2E32), Color(0xFF151518))
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
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.85f)
                            ),
                            startY = 0f
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

        // Bottom info card
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .hazeGlass(
                        state = cardHazeState,
                        shape = RoundedCornerShape(30.dp),
                        containerColor = Color.Black.copy(alpha = 0.45f),
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .graphicsLayer { }
            ) {
                Text(
                    text = movie.title ?: movie.name ?: stringResource(R.string.recommendations_no_title),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val rating = movie.voteAverage ?: 0.0
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_star),
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f", rating),
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(text = "•", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)

                    val year = movie.releaseYear ?: movie.releaseDate?.take(4)
                        ?: movie.firstAirDate?.take(4) ?: stringResource(R.string.recommendations_na)
                    Text(text = year, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }

                val contextLocale = LocalConfiguration.current.locales[0].language
                val genres = remember(movie.genreIds, movie.genreNamesString, contextLocale) {
                    if (!movie.genreIds.isNullOrEmpty()) {
                        movie.genreIds!!.mapNotNull { id ->
                            val defaultName = com.cinetrack.data.model.GenreConstants.ALL_GENRES.find { it.id == id }?.name ?: ""
                            val localized = com.cinetrack.data.model.GenreConstants.getLocalizedName(id, contextLocale, defaultName)
                            localized.takeIf { it.isNotEmpty() }
                        }.joinToString(", ")
                    } else {
                        movie.genreNamesString ?: movie.genres?.mapNotNull { it.name }?.joinToString(", ") ?: ""
                    }
                }

                if (genres.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = genres.uppercase(),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val overview = movie.overview
                if (!overview.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = overview,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
