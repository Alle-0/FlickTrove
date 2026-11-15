package com.cinetrack.ui.components.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.cinetrack.R
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.MvpStat
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.ImageType
import com.cinetrack.util.buildTmdbImageUrl

/**
 * Sezione Top MVPs senza box scuri e con badge numerici circolari perfetti
 */
@Composable
fun TopMvpsSection(
    mvps: List<MvpStat>,
    onActorClick: (Long, String?) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current

    Text(
        text = stringResource(R.string.flow_top_mvps),
        color = Color.White.copy(alpha = 0.9f),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    )

    Spacer(modifier = Modifier.height(8.dp))

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(mvps.take(10), key = { _, mvp -> mvp.actorId }) { index, mvp ->
            val imageUrl = buildTmdbImageUrl(
                mvp.profilePath?.takeIf { it.isNotBlank() && it != "null" },
                ImageType.PROFILE,
                ImageQuality.MEDIUM
            )

            val rankColor = when (index) {
                0 -> Color(0xFFFFD700) // Oro
                1 -> Color(0xFFE0E0E0) // Argento
                2 -> Color(0xFFCD7F32) // Bronzo
                else -> Color.White.copy(alpha = 0.3f)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(96.dp)
                    .bounceClick { onActorClick(mvp.actorId, mvp.profilePath) }
            ) {
                Box {
                    if (imageUrl != null) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = mvp.actorName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .border(2.dp, rankColor, CircleShape),
                            error = {
                                InitialsAvatar(name = mvp.actorName)
                            }
                        )
                    } else {
                        InitialsAvatar(name = mvp.actorName)
                    }

                    // Badge Posizione (Cerchio Perfetto 1:1) per la top 3
                    if (index < 3) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = (-2).dp, y = (-2).dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(rankColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Badge Conteggio xN (Cerchio Perfetto 1:1)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (index == 0) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "x${mvp.count}",
                            color = if (index == 0) Color.Black else MaterialTheme.colorScheme.onPrimary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = mvp.actorName,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 14.sp,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                val titlesText = if (mvp.movieTitles.isNotEmpty()) {
                    mvp.movieTitles.take(2).joinToString(", ")
                } else {
                    ""
                }
                if (titlesText.isNotBlank()) {
                    Text(
                        text = titlesText,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InitialsAvatar(name: String) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(Color.DarkGray)
            .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .joinToString(""),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
