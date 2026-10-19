package com.cinetrack.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.api.MovieDetailResponse
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.utils.ColorUtils
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@Composable
internal fun HeaderTechnicalSection(
    details: MovieDetailResponse?,
    movie: Movie,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
        Spacer(modifier = Modifier.height(14.dp))

        val symbols = DecimalFormatSymbols(Locale.ITALIAN).apply {
            groupingSeparator = '.'
        }
        val formatter = DecimalFormat("#,###", symbols)

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val originalTitle = details?.originalTitle ?: details?.originalName ?: movie.displayName
            val rawStatus = details?.status ?: "—"
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

            // Row 1: Original Title & Status
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                HeaderTechnicalItem(
                    label = stringResource(R.string.detail_tech_original_title),
                    value = originalTitle,
                    icon = ImageVector.vectorResource(id = R.drawable.ic_documento),
                    accentColor = accentColor,
                    modifier = Modifier.weight(1.15f)
                )
                HeaderTechnicalItem(
                    label = stringResource(R.string.detail_tech_status),
                    value = status,
                    icon = Icons.Rounded.Info,
                    accentColor = accentColor,
                    modifier = Modifier.weight(0.85f)
                )
            }

            // Row 2: Release Date & Budget
            val releaseDate = details?.releaseDate ?: details?.firstAirDate ?: movie.releaseDate ?: movie.firstAirDate ?: "—"
            val budget = details?.budget
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                HeaderTechnicalItem(
                    label = stringResource(R.string.detail_tech_release_date),
                    value = releaseDate,
                    icon = ImageVector.vectorResource(id = R.drawable.ic_calendario),
                    accentColor = accentColor,
                    modifier = Modifier.weight(1.15f)
                )
                HeaderTechnicalItem(
                    label = stringResource(R.string.detail_tech_budget),
                    value = if (budget != null && budget > 0) "${formatter.format(budget)} USD" else "—",
                    icon = ImageVector.vectorResource(id = R.drawable.ic_dollar),
                    accentColor = accentColor,
                    modifier = Modifier.weight(0.85f)
                )
            }

            // Row 3: Language & Revenue
            val origLang = details?.originalLanguage
            val revenue = details?.revenue
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                HeaderTechnicalItem(
                    label = stringResource(R.string.detail_tech_language),
                    value = origLang?.uppercase(Locale.getDefault()) ?: "—",
                    icon = ImageVector.vectorResource(id = R.drawable.ic_traduzione),
                    accentColor = accentColor,
                    modifier = Modifier.weight(1.15f)
                )
                HeaderTechnicalItem(
                    label = stringResource(R.string.detail_tech_revenue),
                    value = if (revenue != null && revenue > 0) "${formatter.format(revenue)} USD" else "—",
                    icon = ImageVector.vectorResource(id = R.drawable.ic_world),
                    accentColor = accentColor,
                    modifier = Modifier.weight(0.85f)
                )
            }

            // Row 4: Production & Country
            val production = details?.productionCompanies?.firstOrNull()?.name ?: "—"
            val country = details?.productionCountries?.firstOrNull()?.name ?: "—"
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                HeaderTechnicalItem(
                    label = stringResource(R.string.detail_tech_production),
                    value = production,
                    icon = ImageVector.vectorResource(id = R.drawable.ic_edifici),
                    accentColor = accentColor,
                    modifier = Modifier.weight(1.15f)
                )
                HeaderTechnicalItem(
                    label = stringResource(R.string.detail_tech_country),
                    value = country,
                    icon = ImageVector.vectorResource(id = R.drawable.ic_partenone),
                    accentColor = accentColor,
                    modifier = Modifier.weight(0.85f)
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
private fun HeaderTechnicalItem(
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
        val iconShape = RoundedCornerShape(10.dp)
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(accentColor.copy(alpha = 0.15f), iconShape)
                .border(0.5.dp, ColorUtils.lightenForText(accentColor, 1.25f).copy(alpha = 0.4f), iconShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ColorUtils.lightenForText(accentColor, 1.35f),
                modifier = Modifier.size(15.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                ),
                color = Color.White.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
