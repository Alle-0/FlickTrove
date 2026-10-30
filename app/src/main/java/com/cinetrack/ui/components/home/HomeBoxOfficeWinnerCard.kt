package com.cinetrack.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cinetrack.R
import com.cinetrack.data.model.BoxOfficeMovie
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.buildTmdbImageUrl
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.cinetrack.ui.components.glass.hazeGlass

@Composable
fun HomeBoxOfficeWinnerCard(
    item: BoxOfficeMovie,
    onMovieClick: () -> Unit,
    modifier: Modifier = Modifier,
    onResolveLogo: (suspend (Movie) -> String?)? = null
) {
    val movie = item.movie
    val imageQuality = LocalImageQuality.current
    val cardHazeState = remember { HazeState() }

    var localLogoPath by remember(movie.id, movie.logoPath) { mutableStateOf(movie.logoPath) }
    LaunchedEffect(movie.id, movie.logoPath) {
        if (localLogoPath.isNullOrEmpty() && onResolveLogo != null) {
            val fetched = onResolveLogo(movie)
            if (!fetched.isNullOrEmpty()) {
                localLogoPath = fetched
            }
        } else {
            localLogoPath = movie.logoPath
        }
    }

    val backdropUrl = remember(movie.backdropPath, movie.posterPath, imageQuality) {
        val path = movie.backdropPath ?: movie.posterPath
        if (path != null) buildTmdbImageUrl(path, ImageType.BACKDROP, imageQuality) else null
    }

    val cardShape = RoundedCornerShape(26.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(200.dp)
            .bounceClick(scaleDown = 0.96f) {
                onMovieClick()
            }
            .clip(cardShape)
            .border(1.dp, Color.White.copy(alpha = 0.08f), cardShape)
    ) {
        // Backdrop & Scrim as Haze Source
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(cardHazeState)
        ) {
            if (backdropUrl != null) {
                AsyncImage(
                    model = backdropUrl,
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1B1B26))
                )
            }

            // Dark Gradient Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.45f),
                                Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
            )
        }

        // Top Badge: #1 Box Office Pill
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .hazeGlass(
                    state = cardHazeState,
                    shape = CircleShape,
                    containerColor = Color.Black.copy(alpha = 0.35f),
                    blurRadius = 16.dp,
                    borderWidth = 1.dp,
                    borderColor = Color.White.copy(alpha = 0.15f)
                )
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_crown),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "#1 BOX OFFICE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.5.sp
            )
        }

        // Top Right Badge: Revenue Pill
        if (item.formattedRevenue.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .hazeGlass(
                        state = cardHazeState,
                        shape = CircleShape,
                        containerColor = Color.Black.copy(alpha = 0.35f),
                        blurRadius = 16.dp,
                        borderWidth = 1.dp,
                        borderColor = Color(0xFF10B981).copy(alpha = 0.4f)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = item.formattedRevenue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF34D399)
                )
            }
        }

        // Bottom Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            val activeLogo = localLogoPath ?: movie.logoPath
            if (!activeLogo.isNullOrEmpty()) {
                val logoUrl = buildTmdbImageUrl(activeLogo, ImageType.LOGO, imageQuality)
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(logoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = movie.title,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .heightIn(min = 28.dp, max = 42.dp)
                )
            } else {
                Text(
                    text = movie.title ?: "",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val year = movie.releaseDate?.take(4) ?: ""
                if (year.isNotBlank()) {
                    Text(
                        text = year,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }

                val rating = movie.voteAverage ?: 0.0
                if (rating > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_star_piena),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f", rating),
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
