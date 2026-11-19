package com.cinetrack.ui.components.comments

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.mirrorInRtl
import dev.chrisbanes.haze.HazeState
import java.util.Locale

/**
 * Top bar for CommsUni discussion screen, featuring media title,
 * CommsUni project info trigger, sorting/filter trigger, and TV episode navigation pill.
 */
@Composable
fun CommsUniCommentsTopBar(
    mediaTitle: String,
    isTvSeries: Boolean,
    currentSeasonNumber: Int?,
    currentEpisodeNumber: Int?,
    currentEpisodeTitle: String?,
    hazeState: HazeState,
    onBackClick: () -> Unit,
    onCommsUniInfoClick: (Rect?) -> Unit,
    onSortClick: (Rect?) -> Unit,
    onEpisodesClick: (Rect?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .hazeGlass(
                state = hazeState,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                containerColor = Color.Black.copy(alpha = 0.75f),
                blurRadius = 20.dp
            )
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
    ) {
        val sortCoords = remember { arrayOf<LayoutCoordinates?>(null) }
        val commsUniCoords = remember { arrayOf<LayoutCoordinates?>(null) }
        val episodesCoords = remember { arrayOf<LayoutCoordinates?>(null) }

        // Livello 1: Back, Titolo Media & Azioni (CommsUni + Filtri)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .bounceClick(onClick = onBackClick)
                    .padding(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_left),
                    contentDescription = stringResource(R.string.detail_content_desc_back),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = mediaTitle.ifBlank { stringResource(R.string.comments_screen_title) },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 2.dp, end = 6.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { commsUniCoords[0] = it }
                        .bounceClick {
                            val bounds = commsUniCoords[0]?.let {
                                val pos = it.positionInWindow()
                                Rect(pos.x, pos.y, pos.x + it.size.width, pos.y + it.size.height)
                            }
                            onCommsUniInfoClick(bounds)
                        }
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_commsuni_logo),
                            contentDescription = stringResource(R.string.commsuni_info_title),
                            modifier = Modifier.size(15.dp)
                        )
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.70f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .onGloballyPositioned { sortCoords[0] = it }
                        .bounceClick {
                            val bounds = sortCoords[0]?.let {
                                val pos = it.positionInWindow()
                                Rect(pos.x, pos.y, pos.x + it.size.width, pos.y + it.size.height)
                            }
                            onSortClick(bounds)
                        }
                        .padding(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_filtri),
                        contentDescription = stringResource(R.string.comment_sort_by),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Livello 2: Se serie TV, pulsante compatto puntata + titolo episodio esterno
        if (isTvSeries) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp, end = 16.dp, top = 0.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pill compatta: [ 📺 1x02 > ]
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { episodesCoords[0] = it }
                        .bounceClick {
                            val bounds = episodesCoords[0]?.let {
                                val pos = it.positionInWindow()
                                Rect(pos.x, pos.y, pos.x + it.size.width, pos.y + it.size.height)
                            }
                            onEpisodesClick(bounds)
                        }
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                        .padding(horizontal = 9.dp, vertical = 4.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_tv),
                            contentDescription = stringResource(R.string.episodes_discussions_pill),
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(12.dp)
                        )
                        val pillText = if (currentSeasonNumber != null && currentEpisodeNumber != null) {
                            "${currentSeasonNumber}x${String.format(Locale.US, "%02d", currentEpisodeNumber)}"
                        } else {
                            stringResource(R.string.episodes_discussions_pill)
                        }
                        Text(
                            text = pillText,
                            color = Color.White.copy(alpha = 0.90f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_right),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.70f),
                            modifier = Modifier
                                .size(11.dp)
                                .mirrorInRtl()
                        )
                    }
                }

                // Titolo episodio FUORI dal pulsante
                if (!currentEpisodeTitle.isNullOrBlank()) {
                    Text(
                        text = currentEpisodeTitle,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .bounceClick {
                                val bounds = episodesCoords[0]?.let {
                                    val pos = it.positionInWindow()
                                    Rect(pos.x, pos.y, pos.x + it.size.width, pos.y + it.size.height)
                                }
                                onEpisodesClick(bounds)
                            }
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}
