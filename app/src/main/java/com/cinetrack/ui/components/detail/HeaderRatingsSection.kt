package com.cinetrack.ui.components.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.ExternalRatings
import java.util.Locale

@Composable
internal fun HeaderRatingsSection(
    movie: Movie,
    ratings: ExternalRatings,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
        Spacer(modifier = Modifier.height(16.dp))
        
        val uriHandler = LocalUriHandler.current
        val imdbId = movie.imdbId ?: ratings.imdb

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModernRatingItem(
                badge = { Badge("IMDb", Color(0xFFF5C518), Color.Black) },
                value = ratings.imdb ?: "-",
                subValue = ratings.imdbVotes,
                onClick = {
                    imdbId?.let { uriHandler.openUri("https://www.imdb.com/title/$it") }
                }
            )
            VerticalDivider()
            
            val rtScore = ratings.rottenTomatoes?.removeSuffix("%")?.toIntOrNull() ?: 0
            val rtIcon = if (rtScore >= 60 || ratings.rottenTomatoes == null) "🍅" else "🤢"
            ModernRatingItem(
                badge = { Text(rtIcon, fontSize = 14.sp) },
                value = ratings.rottenTomatoes ?: "-",
                subValue = null,
                onClick = {
                    val title = movie.title ?: movie.name
                    title?.let { uriHandler.openUri("https://www.rottentomatoes.com/search?search=${it}") }
                }
            )
            VerticalDivider()
            
            val mcScore = ratings.metacritic?.split("/")?.firstOrNull()?.toIntOrNull() ?: 0
            val mcColor = when {
                mcScore >= 61 -> Color(0xFF66CC33)
                mcScore >= 40 -> Color(0xFFFFCC33)
                else -> Color(0xFFFF0000)
            }
            ModernRatingItem(
                badge = {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(mcColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_metacritic_logo),
                            contentDescription = "Metacritic Logo",
                            modifier = Modifier.size(14.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                },
                value = ratings.metacritic?.replace("/100", "") ?: "-",
                subValue = "Score",
                onClick = {
                    val title = movie.title ?: movie.name
                    title?.let { uriHandler.openUri("https://www.metacritic.com/search/${it}/") }
                }
            )
            VerticalDivider()
            ModernRatingItem(
                badge = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_trakt_logo),
                        contentDescription = "Trakt.tv Logo",
                        modifier = Modifier.size(20.dp),
                        contentScale = ContentScale.Fit
                    )
                },
                value = ratings.trakt?.let { "${(it * 10).toInt()}%" } ?: "-",
                subValue = ratings.traktVotes,
                onClick = {
                    if (imdbId != null) {
                        uriHandler.openUri("https://trakt.tv/search?query=$imdbId")
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        val hasBoxOffice = (movie.revenue ?: 0L) > 0
        ratings.awards?.let { awardText ->
            if (awardText != "N/A" && awardText.isNotEmpty()) {
                AwardBox(
                    awards = awardText,
                    accentColor = accentColor,
                    onClick = {
                        imdbId?.let { uriHandler.openUri("https://www.imdb.com/title/$it/awards") }
                    }
                )
                if (hasBoxOffice) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
        
        movie.revenue?.let { rev ->
            if (rev > 0) {
                val formattedRevenue = java.text.NumberFormat.getCurrencyInstance(Locale.US).apply {
                    maximumFractionDigits = 0
                }.format(rev)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetaBadge(label = "BOX OFFICE", value = formattedRevenue)
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
fun AwardBox(awards: String, accentColor: Color, onClick: () -> Unit = {}) {
    val gold = Color(0xFFFFD700)
    var textWidth by remember { mutableStateOf(0) }
    var containerWidth by remember { mutableStateOf(0) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.97f) { onClick() }
            .background(gold.copy(alpha = 0.05f), CircleShape)
            .border(0.5.dp, gold.copy(alpha = 0.15f), CircleShape)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { containerWidth = it.width }
                .then(
                    if (textWidth > containerWidth && containerWidth > 0) {
                        Modifier
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                val fadeWidthPx = 16.dp.toPx()
                                val fadeFraction = if (size.width > 0f) (fadeWidthPx / size.width).coerceAtMost(0.49f) else 0f
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        0f to Color.Transparent,
                                        fadeFraction to Color.Black,
                                        1f - fadeFraction to Color.Black,
                                        1f to Color.Transparent
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = awards,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = gold.copy(alpha = 0.8f),
                maxLines = 1,
                softWrap = false,
                letterSpacing = 0.5.sp,
                modifier = Modifier
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        repeatDelayMillis = 1200
                    )
                    .then(
                        if (textWidth > containerWidth && containerWidth > 0) {
                            Modifier.padding(horizontal = 16.dp)
                        } else Modifier
                    )
                    .onSizeChanged { textWidth = it.width }
            )
        }
    }
}

@Composable
fun ModernRatingItem(
    badge: @Composable () -> Unit,
    value: String,
    subValue: String? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.bounceClick(scaleDown = 0.9f) { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        badge()
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            if (subValue != null && subValue != "N/A" && subValue != "Score") {
                Text(
                    text = subValue.uppercase(),
                    fontSize = 7.sp,
                    lineHeight = 7.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.3f),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun Badge(text: String, bgColor: Color, textColor: Color, isSquare: Boolean = false) {
    Surface(
        color = bgColor,
        shape = if (isSquare) RoundedCornerShape(4.dp) else CircleShape,
        modifier = (if (isSquare) Modifier.size(20.dp) else Modifier.width(34.dp).height(14.dp))
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = if (isSquare) 12.sp else 8.sp,
                fontWeight = FontWeight.Black,
                color = textColor,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    }
}

@Composable
fun VerticalDivider() {
    Box(modifier = Modifier.width(0.5.dp).height(24.dp).background(Color.White.copy(alpha = 0.12f)))
}

@Composable
fun MetaBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White,
    showValueBox: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .let { if (onClick != null) it.bounceClick(scaleDown = 0.95f) { onClick() } else it }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f), CircleShape)
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), CircleShape)
            .padding(
                start = 10.dp,
                end = if (showValueBox) 3.dp else 10.dp,
                top = 3.dp,
                bottom = 3.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.padding(end = 6.dp)
        )
        if (showValueBox) {
            Box(
                modifier = Modifier
                    .background(valueColor.copy(alpha = 0.12f), CircleShape)
                    .border(0.5.dp, valueColor.copy(alpha = 0.25f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = valueColor
                )
            }
        } else {
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = valueColor
            )
        }
    }
}
