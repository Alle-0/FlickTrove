package com.cinetrack.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.utils.bounceClick
import java.util.Locale

@Composable
internal fun HeaderParentsGuideSection(
    certification: String?,
    certificationCountry: String?,
    accentColor: Color,
    onCardClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
        Spacer(modifier = Modifier.height(10.dp))

        val cleanCert = certification?.trim()?.uppercase(Locale.ROOT)?.takeIf { it.isNotBlank() } ?: "NR"
        val ratingInfo = remember(cleanCert) { resolveHeaderRatingInfo(cleanCert) }
        val certColor = ratingInfo.color

        // Line 1: Rated Detail Card for this specific film/series
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .bounceClick(scaleDown = 0.98f) { onCardClick() }
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.04f))
                .border(0.5.dp, certColor.copy(alpha = 0.35f), CircleShape)
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .defaultMinSize(minWidth = 48.dp)
                    .clip(CircleShape)
                    .background(certColor.copy(alpha = 0.15f))
                    .border(0.5.dp, certColor.copy(alpha = 0.5f), CircleShape)
                    .padding(horizontal = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                val displayCode = if (!certificationCountry.isNullOrBlank()) {
                    "$certificationCountry · $cleanCert"
                } else {
                    cleanCert
                }
                Text(
                    text = displayCode,
                    color = certColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(ratingInfo.labelRes),
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.3.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(ratingInfo.descriptionRes),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Lines below: Parents Guide category meter rows
        val parentsGuideData = remember(certification, certificationCountry) {
            ParentsGuideResolver.resolve(certification, certificationCountry)
        }

        if (parentsGuideData != null) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                parentsGuideData.categories.forEach { category ->
                    CategoryMeterRow(category = category, accentColor = accentColor)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.detail_rated_all_ages_note),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                ),
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
