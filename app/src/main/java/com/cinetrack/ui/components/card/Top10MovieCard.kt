package com.cinetrack.ui.components.card

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.theme.FlickTroveTheme
import dev.chrisbanes.haze.HazeState

@Composable
fun Top10MovieCard(
    movie: Movie, 
    rank: Int, 
    hazeState: HazeState, 
    staggerIndex: Int = 0,
    isFavorite: Boolean = movie.favorite,
    isWatched: Boolean = movie.watched,
    folderColors: List<Color> = emptyList(),
    onPress: (Movie) -> Unit, 
    onLongPress: (Movie, androidx.compose.ui.geometry.Offset, androidx.compose.ui.geometry.Offset) -> Unit = { _, _, _ -> },
    onAction: (Movie) -> Unit = {},
    onMessage: (String) -> Unit = {}
) {
    val accentColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .width(156.dp)
            .height(210.dp)
    ) {
        val bgColor = MaterialTheme.colorScheme.background
        
        // Large outlined rank number — stroke only, accent color, bleeds off left/bottom edge
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(width = 100.dp, height = 110.dp)
                .offset(x = (-8).dp, y = 22.dp)
        ) {
            val outlinePaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 148.sp.toPx()
                typeface = android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT_BOLD,
                    android.graphics.Typeface.BOLD
                )
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 7f // Double thickness because fill covers inner half
                strokeJoin = android.graphics.Paint.Join.ROUND
                strokeCap = android.graphics.Paint.Cap.ROUND
                color = accentColor.copy(alpha = 0.78f).toArgb()
            }
            val fillPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 148.sp.toPx()
                typeface = android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT_BOLD,
                    android.graphics.Typeface.BOLD
                )
                style = android.graphics.Paint.Style.FILL
                color = bgColor.toArgb()
            }
            drawContext.canvas.nativeCanvas.drawText(
                rank.toString(),
                0f,
                size.height * 0.85f,
                outlinePaint
            )
            drawContext.canvas.nativeCanvas.drawText(
                rank.toString(),
                0f,
                size.height * 0.85f,
                fillPaint
            )
        }

        // Poster card shifted right to reveal number on the left
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(start = 30.dp)
                .width(126.dp)
        ) {
            MovieCard(
                movie = movie,
                cardWidth = 126.dp,
                isFavorite = isFavorite,
                isWatched = isWatched,
                folderColors = folderColors,
                hazeState = hazeState,
                staggerIndex = staggerIndex,
                onPress = onPress,
                onLongPress = onLongPress,
                onAction = onAction,
                onMessage = onMessage
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun Top10MovieCardPreview() {
    val sampleMovie = Movie(
        id = 1,
        title = "Inception",
        posterPath = "/9gk7adHYeDvHkCSEqAvQNLV5Uge.jpg",
        mediaType = "movie"
    )
    val hazeState = remember { HazeState() }
    FlickTroveTheme {
        Surface(color = Color(0xFF121212)) {
            Top10MovieCard(
                movie = sampleMovie,
                rank = 1,
                hazeState = hazeState,
                onPress = {}
            )
        }
    }
}
