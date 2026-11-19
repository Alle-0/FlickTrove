package com.cinetrack.ui.components.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.theme.OnSurfaceMuted

/**
 * Standard attribution item row rendering external data source branding,
 * legal attribution texts, and logos (TMDB, Trakt, TheTVDB, etc.).
 */
@Composable
fun AttributionRow(
    brand: String,
    text: String,
    modifier: Modifier = Modifier
) {
    val logoRes = when (brand) {
        "TMDB" -> R.drawable.ic_tmdb_logo
        "Trakt.tv" -> R.drawable.ic_trakt_logo
        "TheTVDB" -> R.drawable.ic_tvdb_logo
        "KLIPY" -> R.drawable.ic_klipy_logo
        "SIMKL" -> R.drawable.ic_simkl_logo
        "CommsUni", "CommsUni (commsuni.tv)" -> R.drawable.ic_commsuni_logo
        "Rotten Tomatoes" -> R.drawable.ic_rottentomatoes_logo
        "Metacritic" -> R.drawable.ic_metacritic_logo
        else -> null
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.02f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.04f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo Representation with local vector or fallback text
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when (brand) {
                        "TMDB" -> Color(0xFF032541)
                        "OMDb API" -> Color(0xFFF5C518)
                        "TheTVDB" -> Color(0xFF31B057)
                        "Trakt.tv" -> Color.White.copy(alpha = 0.05f)
                        "KLIPY" -> Color(0xFF141418)
                        "SIMKL" -> Color(0xFF1B1B1B)
                        "CommsUni", "CommsUni (commsuni.tv)" -> Color(0xFF2B2D30)
                        "Rotten Tomatoes" -> Color(0xFFFA320A)
                        "Metacritic" -> Color(0xFFFFCC33)
                        else -> Color.White.copy(alpha = 0.05f)
                    }
                )
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (logoRes != null) {
                Image(
                    painter = painterResource(id = logoRes),
                    contentDescription = "$brand Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            when (brand) {
                                "TMDB" -> 6.dp
                                "KLIPY" -> 4.dp
                                else -> 0.dp
                            }
                        ),
                    contentScale = ContentScale.Fit
                )
            } else {
                // Stylized Fallback for OMDb, TheTVDB, or missing logos
                Text(
                    text = when (brand) {
                        "OMDb API" -> "OMDb"
                        "TheTVDB" -> "TVDB"
                        "KLIPY" -> "KLIPY"
                        "SIMKL" -> "SIMKL"
                        "Rotten Tomatoes" -> "RT"
                        "Metacritic" -> "MC"
                        else -> brand.take(1)
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = if (brand == "OMDb API" || brand == "TheTVDB" || brand == "KLIPY" || brand == "SIMKL") 9.sp else 14.sp
                    ),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Visible,
                    color = if (brand == "OMDb API" || brand == "Metacritic") Color.Black else Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        val annotatedString = buildAnnotatedString {
            val parts = text.split(brand)
            parts.forEachIndexed { index, part ->
                append(part)
                if (index < parts.size - 1) {
                    withStyle(
                        style = SpanStyle(fontWeight = FontWeight.ExtraBold, color = Color.White)
                    ) {
                        append(brand)
                    }
                }
            }
        }

        Text(
            text = annotatedString,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = OnSurfaceMuted,
            lineHeight = 15.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
