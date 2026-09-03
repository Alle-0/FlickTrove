package com.cinetrack.ui.components.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.ui.theme.FlickTroveTheme

@Composable
fun BackdropMovieCard(movie: Movie, onPress: (Movie) -> Unit) {
    val backdropUrl = buildTmdbImageUrl(movie.backdropPath, ImageType.BACKDROP, LocalImageQuality.current)
    Box(
        modifier = Modifier
            .width(280.dp)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.DarkGray)
            .bounceClick { onPress(movie) }
    ) {
        AsyncImage(
            model = backdropUrl,
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                        startY = 100f
                    )
                )
        )
        Text(
            text = movie.title ?: "",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun BackdropMovieCardPreview() {
    val sampleMovie = Movie(
        id = 1,
        title = "Inception",
        backdropPath = "/8ZTVqvKDQ8emSGUEMjsS4yHAwrp.jpg",
        mediaType = "movie"
    )
    FlickTroveTheme {
        Surface(color = Color(0xFF121212)) {
            BackdropMovieCard(movie = sampleMovie, onPress = {})
        }
    }
}
