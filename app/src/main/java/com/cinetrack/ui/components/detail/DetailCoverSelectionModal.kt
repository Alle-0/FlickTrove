package com.cinetrack.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cinetrack.R
import com.cinetrack.data.Movie
import com.cinetrack.data.api.ImageItem
import com.cinetrack.ui.components.shared.FlickTroveModal
import com.cinetrack.ui.utils.premiumScrollbar
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.ImageType
import com.cinetrack.util.buildTmdbImageUrl
import dev.chrisbanes.haze.HazeState

@Composable
fun DetailCoverSelectionModal(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    movieEntry: Movie?,
    backdrops: List<ImageItem>,
    currentImageQuality: ImageQuality,
    accentColor: Color,
    hazeState: HazeState,
    onSelectCover: (String?) -> Unit
) {
    FlickTroveModal(
        isVisible = isVisible && movieEntry != null,
        onDismissRequest = onDismiss,
        hazeState = hazeState
    ) {
        val configuration = LocalConfiguration.current
        val dynamicCoverRatio = remember(configuration.screenWidthDp) {
            (configuration.screenWidthDp.toFloat() / 480f).coerceIn(0.6f, 1.5f)
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.detail_select_cover),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                textAlign = TextAlign.Center
            )
            
            val defaultImg = movieEntry?.backdropPath ?: movieEntry?.posterPath
            if (backdrops.isEmpty() && defaultImg == null) {
                Text(
                    text = stringResource(R.string.detail_no_alternative_covers), 
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                val gridState = rememberLazyGridState()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).premiumScrollbar(gridState, paddingEnd = 6f)
                ) {
                    if (defaultImg != null) {
                        item {
                            val isDefaultSelected = movieEntry?.customBackdropPath == null
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(dynamicCoverRatio)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .border(
                                        width = if (isDefaultSelected) 2.dp else 0.dp,
                                        color = if (isDefaultSelected) accentColor else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        onSelectCover(null)
                                        onDismiss()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                val imageUrl = buildTmdbImageUrl(defaultImg, ImageType.BACKDROP, currentImageQuality)
                                val context = LocalContext.current
                                val request = remember(imageUrl) {
                                    ImageRequest.Builder(context)
                                        .data(imageUrl)
                                        .crossfade(true)
                                        .crossfade(400)
                                        .build()
                                }
                                AsyncImage(
                                    model = request,
                                    contentDescription = stringResource(R.string.detail_cover_default),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().alpha(0.7f)
                                )
                                Box(
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                                        .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.detail_cover_default),
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                if (isDefaultSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .background(accentColor, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.detail_cover_selected_badge),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                    items(backdrops) { imageItem ->
                        val isSelected = imageItem.filePath == movieEntry?.customBackdropPath
                        val imageUrl = buildTmdbImageUrl(imageItem.filePath, ImageType.BACKDROP, currentImageQuality)
                        val context = LocalContext.current
                        val request = remember(imageUrl) {
                            ImageRequest.Builder(context)
                                .data(imageUrl)
                                .crossfade(true)
                                .crossfade(400)
                                .build()
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(dynamicCoverRatio)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) accentColor else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    onSelectCover(imageItem.filePath)
                                    onDismiss()
                                }
                        ) {
                            AsyncImage(
                                model = request,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .background(accentColor, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.detail_cover_selected_badge),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.15f)
                )
            ) {
                Text(stringResource(R.string.settings_close), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
