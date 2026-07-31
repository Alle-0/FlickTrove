package com.cinetrack.ui.components.card

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.detail.ALL_VIBES
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.buildTmdbImageUrl
import dev.chrisbanes.haze.HazeState
import com.cinetrack.ui.components.glass.hazeGlass

@Composable
fun FlowMovieCard(
    movie: Movie,
    cardWidth: Dp,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    staggerIndex: Int = -1,
    hasAnimatedSet: MutableSet<String>? = null,
    onPress: (Movie) -> Unit = {},
    onLongPress: (Movie, androidx.compose.ui.geometry.Offset, androidx.compose.ui.geometry.Offset) -> Unit = { _, _, _ -> }
) {
    Box(
        modifier = modifier
    ) {
        // Base standard MovieCard
        MovieCard(
            movie = movie,
            cardWidth = cardWidth,
            showBadges = false, // We'll render our own vibes top-right
            showAdvancedBadges = false,
            showActionButton = false,
            hazeState = hazeState,
            staggerIndex = staggerIndex,
            hasAnimatedSet = hasAnimatedSet,
            onPress = onPress,
            onLongPress = onLongPress
        )

        // 1. Vibe Pyramid (Top Right)
        val vibeCodes = remember(movie.emotionalVibes) {
            movie.emotionalVibes?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        }
        val vibes = remember(vibeCodes) {
            vibeCodes.mapNotNull { code -> ALL_VIBES.find { it.code == code || it.emoji == code } }
        }

        if (vibes.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                vibes.forEach { vibe ->
                    Text(
                        text = vibe.emoji,
                        fontSize = 18.sp
                    )
                }
            }
        }

        // 2. MVP Mini-Card (Center/Bottom Right, Overlapping)
        if (movie.favoriteActorId != null) {
            val mvpName = movie.favoriteActorName ?: "MVP"
            val mvpProfilePath = movie.favoriteActorProfilePath
            val profileUrl = buildTmdbImageUrl(mvpProfilePath, ImageType.PROFILE, LocalImageQuality.current)
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 18.dp, y = 6.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(4.dp)
                ) {
                    if (profileUrl != null) {
                        AsyncImage(
                            model = profileUrl,
                            contentDescription = mvpName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.7f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.7f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_persona),
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val charName = movie.favoriteActorCharacter?.takeIf { it.isNotBlank() }
                    if (charName != null) {
                        Text(
                            text = charName,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            lineHeight = 12.sp,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = mvpName,
                        color = if (charName != null) Color.White.copy(alpha = 0.5f) else Color.White,
                        fontSize = if (charName != null) 9.sp else 11.sp,
                        fontWeight = if (charName != null) FontWeight.Normal else FontWeight.Medium,
                        maxLines = 2,
                        lineHeight = 12.sp,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
