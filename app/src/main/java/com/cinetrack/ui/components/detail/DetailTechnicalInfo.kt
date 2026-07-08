package com.cinetrack.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border

import androidx.compose.ui.res.stringResource

import com.cinetrack.R

import androidx.compose.ui.res.vectorResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.data.api.MovieDetailResponse
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

@Composable
fun DetailTechnicalInfo(
    details: MovieDetailResponse,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = stringResource(R.string.detail_technical_info),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            ),
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        val symbols = DecimalFormatSymbols(Locale.ITALIAN).apply {
            groupingSeparator = '.'
        }
        val formatter = DecimalFormat("#,###", symbols)

        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (!details.originalTitle.isNullOrBlank() || !details.originalName.isNullOrBlank() || !details.status.isNullOrBlank()) {
                val originalTitle = details.originalTitle ?: details.originalName ?: "—"
                val rawStatus = details.status ?: "—"
                val status = when (rawStatus) {
                    "Returning Series" -> stringResource(R.string.status_returning_series)
                    "Planned" -> stringResource(R.string.status_planned)
                    "In Production" -> stringResource(R.string.status_in_production)
                    "Ended" -> stringResource(R.string.status_ended)
                    "Canceled", "Cancelled" -> stringResource(R.string.status_canceled)
                    "Pilot" -> stringResource(R.string.status_pilot)
                    "Rumored" -> stringResource(R.string.status_rumored)
                    "Post Production" -> stringResource(R.string.status_post_production)
                    "Released" -> stringResource(R.string.status_released)
                    else -> rawStatus
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TechnicalItem(
                        label = stringResource(R.string.detail_tech_original_title),
                        value = originalTitle,
                        icon = ImageVector.vectorResource(id = R.drawable.ic_documento),
                        accentColor = accentColor,
                        modifier = Modifier.weight(1f)
                    )
                    TechnicalItem(
                        label = stringResource(R.string.detail_tech_status),
                        value = status,
                        icon = Icons.Rounded.Info,
                        accentColor = accentColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Row 1: Release Date & Budget
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TechnicalItem(
                    label = stringResource(R.string.detail_tech_release_date),
                    value = details.releaseDate ?: details.firstAirDate ?: "—",
                    icon = ImageVector.vectorResource(id = R.drawable.ic_calendario),
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
                TechnicalItem(
                    label = stringResource(R.string.detail_tech_budget),
                    value = if (details.budget != null && details.budget > 0) "${formatter.format(details.budget)} USD" else "—",
                    icon = ImageVector.vectorResource(id = R.drawable.ic_dollar),
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: Language & Revenue
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TechnicalItem(
                    label = stringResource(R.string.detail_tech_language),
                    value = details.originalLanguage?.uppercase(Locale.getDefault()) ?: "—",
                    icon = ImageVector.vectorResource(id = R.drawable.ic_traduzione),
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
                TechnicalItem(
                    label = stringResource(R.string.detail_tech_revenue),
                    value = if (details.revenue != null && details.revenue > 0) "${formatter.format(details.revenue)} USD" else "—",
                    icon = ImageVector.vectorResource(id = R.drawable.ic_world),
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 3: Production & Country
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TechnicalItem(
                    label = stringResource(R.string.detail_tech_production),
                    value = details.productionCompanies?.firstOrNull()?.name ?: "—",
                    icon = ImageVector.vectorResource(id = R.drawable.ic_edifici),
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
                TechnicalItem(
                    label = stringResource(R.string.detail_tech_country),
                    value = details.productionCountries?.firstOrNull()?.name ?: "—",
                    icon = ImageVector.vectorResource(id = R.drawable.ic_partenone),
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TechnicalItem(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconShape = RoundedCornerShape(12.dp)
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(accentColor.copy(alpha = 0.1f), iconShape)
                .border(0.5.dp, accentColor.copy(alpha = 0.15f), iconShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = Color.White.copy(alpha = 0.3f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
