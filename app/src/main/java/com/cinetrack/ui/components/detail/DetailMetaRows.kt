package com.cinetrack.ui.components.detail

import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.ImageType
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.LocalImageQuality
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cinetrack.data.Genre
import com.cinetrack.data.api.Provider
import com.cinetrack.ui.utils.bounceClick

/**
 * DetailMetaRows
 * Renders Genre pills and Watch Providers (Streaming, Buy, Rent).
 * Features "Zero-Friction" interactions and thin glassmorphic borders.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailMetaRows(
    genres: List<Genre>,
    keywords: List<com.cinetrack.data.api.Keyword> = emptyList(),
    streaming: List<Provider>,
    buyAndRent: List<Provider>,
    accentColor: Color,
    onGenreClick: (Genre, Offset) -> Unit,
    onKeywordClick: (Long, String, Offset) -> Unit,
    onProviderClick: (Provider) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        var showKeywords by remember { mutableStateOf(false) }

        // Genres Flow
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
                .animateContentSize(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (genre in genres) {
                GenrePill(
                    genre = genre,
                    accentColor = accentColor,
                    onGenreClick = { offset -> onGenreClick(genre, offset) }
                )
            }
            if (keywords.isNotEmpty()) {
                AnimatedVisibility(
                    visible = !showKeywords,
                    enter = scaleIn(initialScale = 0.8f) + fadeIn(),
                    exit = scaleOut(targetScale = 0.8f) + fadeOut()
                ) {
                    KeywordExpandPill(
                        accentColor = accentColor,
                        onClick = { showKeywords = true }
                    )
                }
                
                keywords.forEachIndexed { index, keyword ->
                    val reverseIndex = keywords.size - 1 - index
                    AnimatedVisibility(
                        visible = showKeywords,
                        enter = scaleIn(
                            initialScale = 0.8f, 
                            animationSpec = tween(durationMillis = 350, delayMillis = index * 25, easing = FastOutSlowInEasing)
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 300, delayMillis = index * 25, easing = LinearOutSlowInEasing)
                        ),
                        exit = scaleOut(
                            targetScale = 0.8f, 
                            animationSpec = tween(durationMillis = 200, delayMillis = reverseIndex * 20, easing = FastOutSlowInEasing)
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 200, delayMillis = reverseIndex * 20, easing = LinearOutSlowInEasing)
                        )
                    ) {
                        KeywordPill(
                            keyword = keyword.name, 
                            accentColor = accentColor,
                            onKeywordClick = { offset -> onKeywordClick(keyword.id, keyword.name, offset) }
                        )
                    }
                }
                
                AnimatedVisibility(
                    visible = showKeywords,
                    enter = scaleIn(
                        initialScale = 0.8f, 
                        animationSpec = tween(durationMillis = 350, delayMillis = keywords.size * 25, easing = FastOutSlowInEasing)
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 300, delayMillis = keywords.size * 25, easing = LinearOutSlowInEasing)
                    ),
                    exit = scaleOut(
                        targetScale = 0.8f, 
                        animationSpec = tween(durationMillis = 200, delayMillis = 0, easing = FastOutSlowInEasing)
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 200, delayMillis = 0, easing = LinearOutSlowInEasing)
                    )
                ) {
                    KeywordCollapsePill(
                        accentColor = accentColor,
                        onClick = { showKeywords = false }
                    )
                }
            }
        }

        // Watch Providers
        if (streaming.isNotEmpty() || buyAndRent.isNotEmpty()) {
            Text(
                text = "DOVE GUARDARE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp
                ),
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (streaming.isNotEmpty()) {
                ProviderRow(label = "IN STREAMING", providers = streaming, accentColor = accentColor, onProviderClick = onProviderClick)
            }

            if (buyAndRent.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                ProviderRow(label = "NOLEGGIO O ACQUISTO", providers = buyAndRent, accentColor = accentColor, onProviderClick = onProviderClick)
            }
        }
    }
}

@Composable
fun GenrePill(genre: Genre, accentColor: Color, onGenreClick: (Offset) -> Unit) {
    val pillCenter = remember { arrayOf(Offset.Zero) }
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .onGloballyPositioned { layoutCoordinates ->
                val position = layoutCoordinates.positionInWindow()
                pillCenter[0] = Offset(
                    position.x + layoutCoordinates.size.width / 2f,
                    position.y + layoutCoordinates.size.height / 2f
                )
            }
            .bounceClick { onGenreClick(pillCenter[0]) },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .background(Color.White.copy(alpha = 0.05f))
                .border(0.5.dp, Color.White.copy(alpha = 0.1f), shape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = genre.name ?: "",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun ProviderRow(label: String, providers: List<Provider>, accentColor: Color, onProviderClick: (Provider) -> Unit) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            ),
            color = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 24.dp), // To allow scroll past edge
            modifier = Modifier.fillMaxWidth()
        ) {
            items(providers, key = { it.providerId }, contentType = { "provider" }) { provider ->
                ProviderLogo(provider = provider, accentColor = accentColor, onClick = onProviderClick)
            }
        }
    }
}

@Composable
fun ProviderLogo(provider: Provider, accentColor: Color, onClick: (Provider) -> Unit) {
    // logoBaseUrl is no longer needed
    val shape = RoundedCornerShape(12.dp)
    
    Box(
        modifier = Modifier
            .bounceClick { onClick(provider) }
            .size(38.dp)
            .border(
                width = 0.5.dp,
                color = accentColor.copy(alpha = 0.3f),
                shape = shape
            )
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.3f))
    ) {
        AsyncImage(
            model = buildTmdbImageUrl(provider.logoPath, ImageType.LOGO, LocalImageQuality.current),
            contentDescription = provider.providerName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun KeywordExpandPill(accentColor: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .bounceClick { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .background(accentColor.copy(alpha = 0.15f))
                .border(0.5.dp, accentColor.copy(alpha = 0.5f), shape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+ Sottogeneri",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = accentColor,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun KeywordCollapsePill(accentColor: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .bounceClick { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .background(accentColor.copy(alpha = 0.15f))
                .border(0.5.dp, accentColor.copy(alpha = 0.5f), shape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "- Nascondi",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = accentColor,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun KeywordPill(keyword: String, accentColor: Color, onKeywordClick: (Offset) -> Unit) {
    val pillCenter = remember { arrayOf(Offset.Zero) }
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .onGloballyPositioned { layoutCoordinates ->
                val position = layoutCoordinates.positionInWindow()
                pillCenter[0] = Offset(
                    position.x + layoutCoordinates.size.width / 2f,
                    position.y + layoutCoordinates.size.height / 2f
                )
            }
            .bounceClick { onKeywordClick(pillCenter[0]) },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .background(Color.White.copy(alpha = 0.02f))
                .border(0.5.dp, Color.White.copy(alpha = 0.05f), shape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = keyword.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 1.sp
                ),
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}
