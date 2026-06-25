package com.cinetrack.ui.components.detail

import androidx.compose.foundation.background

import androidx.compose.ui.res.stringResource
import com.cinetrack.R

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.ui.components.shared.FlickTroveBottomSheet
import com.cinetrack.ui.theme.NeonTeal

private data class RatingInfo(
    val code: String,
    val labelRes: Int,
    val descriptionRes: Int,
    val color: Color
)

private val RATING_DATA = listOf(
    RatingInfo("G", R.string.rating_g_label, R.string.rating_g_desc, Color(0xFF4CAF50)),
    RatingInfo("PG", R.string.rating_pg_label, R.string.rating_pg_desc, Color(0xFFFFC107)),
    RatingInfo("PG-13", R.string.rating_pg13_label, R.string.rating_pg13_desc, Color(0xFFFF9800)),
    RatingInfo("R", R.string.rating_r_label, R.string.rating_r_desc, Color(0xFFF44336)),
    RatingInfo("NC-17", R.string.rating_nc17_label, R.string.rating_nc17_desc, Color(0xFFB71C1C)),
    RatingInfo("TV-14", R.string.rating_tv14_label, R.string.rating_tv14_desc, Color(0xFFFF7043)),
    RatingInfo("TV-MA", R.string.rating_tvma_label, R.string.rating_tvma_desc, Color(0xFFD32F2F))
)

/**
 * Educational bottom sheet explaining MPAA and TV ratings.
 * Features a scrollable list of classification codes and descriptions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatedExplanationBottomSheet(
    onDismiss: () -> Unit,
    accentColor: Color = NeonTeal
) {
    FlickTroveBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxHeight(0.75f)) {
            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .border(0.5.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_documento),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = stringResource(R.string.rated_guide_title),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_x),
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.4f)
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

            // Scrollable Content
            LazyColumn(
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.rated_guide_source),
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }

                items(RATING_DATA, key = { it.code }, contentType = { "rating" }) { rating ->
                    RatingEntry(rating)
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = stringResource(R.string.rated_guide_help),
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 16.sp,
                            modifier = Modifier.fillMaxWidth(0.9f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingEntry(rating: RatingInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Semantic Code Box
        Box(
            modifier = Modifier
                .width(54.dp)
                .height(32.dp)
                .border(
                    width = 0.5.dp,
                    color = rating.color.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rating.code,
                color = rating.color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
        }

        // Description Column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(rating.labelRes),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.3.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(rating.descriptionRes),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
