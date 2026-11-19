package com.cinetrack.ui.components.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.cinetrack.R
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.history.model.WatchTimelineEntry
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.mirrorInRtl
import dev.chrisbanes.haze.HazeState

@Composable
fun HistoryDiaryCard(
    hazeState: HazeState,
    backgroundLuminance: Float = 0f,
    recentEntries: List<WatchTimelineEntry> = emptyList(),
    totalCount: Int = 0,
    onClick: () -> Unit
) {
    val cardOverlay = if (backgroundLuminance > 0.38f) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.05f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .hazeGlass(
                    state = hazeState,
                    shape = RoundedCornerShape(32.dp),
                    containerColor = cardOverlay,
                    borderColor = Color.White.copy(alpha = 0.12f),
                    borderWidth = 1.dp,
                    useOffscreenStrategy = false
                )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick { onClick() }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ticket),
                    contentDescription = stringResource(R.string.history_account_card_title),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.history_account_card_title).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    painter = painterResource(id = R.drawable.ic_right),
                    contentDescription = "Open Diary",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(20.dp)
                        .mirrorInRtl()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.history_account_card_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Overlapping poster thumbnails from the last 3 items
                val previewPosters = remember(recentEntries) {
                    recentEntries.filter { !it.posterPath.isNullOrBlank() }.take(3)
                }

                if (previewPosters.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((-10).dp)
                    ) {
                        previewPosters.forEachIndexed { index, item ->
                            Box(
                                modifier = Modifier
                                    .zIndex((index + 1).toFloat())
                                    .width(28.dp)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E1E24))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            ) {
                                AsyncImage(
                                    model = "https://image.tmdb.org/t/p/w185${item.posterPath}",
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                } else if (totalCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "$totalCount",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
