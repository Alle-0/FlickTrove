package com.cinetrack.ui.components.card

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cinetrack.R
import com.cinetrack.data.model.NewsItem
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.theme.FlickTroveTheme
import androidx.compose.ui.platform.LocalContext

@Composable
fun NewsArticleCard(article: NewsItem, context: Context) {
    // Estrai la fonte dall'URL (es. "screenrant.com" -> "ScreenRant")
    val source = remember(article.link) {
        runCatching {
            android.net.Uri.parse(article.link).host
                ?.removePrefix("www.")
                ?.split(".")
                ?.firstOrNull()
                ?.replaceFirstChar { it.uppercase() }
                ?: ""
        }.getOrDefault("")
    }

    Box(
        modifier = Modifier
            .width(220.dp)
            .height(140.dp)
            .bounceClick {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(article.link))
                context.startActivity(intent)
            }
            .clip(RoundedCornerShape(16.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
    ) {
        // Immagine di sfondo
        if (article.imageUrl != null) {
            AsyncImage(
                model = article.imageUrl,
                contentDescription = article.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
            )
        } else {
            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray)
            )
        }

        // Gradient in basso
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f)
                .align(Alignment.BottomStart)
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))
                    )
                )
        )

        // Testo in basso
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 10.dp, end = 10.dp, bottom = 8.dp)
        ) {
            if (source.isNotBlank()) {
                Text(
                    text = source,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
            Text(
                text = article.title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp
                ),
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Badge "External link" in alto a destra
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Link esterno",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_external_link),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun NewsArticleCardPreview() {
    val sampleNews = NewsItem(
        title = "Breaking: New Christopher Nolan Movie Announced",
        link = "https://www.hollywoodreporter.com/movie-news",
        pubDate = "2026-09-03",
        imageUrl = "https://via.placeholder.com/220x140"
    )
    FlickTroveTheme {
        Surface(color = Color(0xFF121212)) {
            NewsArticleCard(article = sampleNews, context = LocalContext.current)
        }
    }
}
