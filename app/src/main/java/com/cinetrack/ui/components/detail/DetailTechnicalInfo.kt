package com.cinetrack.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
            text = "DETTAGLI TECNICI",
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
            // Row 1: Release Date & Budget
            Row(modifier = Modifier.fillMaxWidth()) {
                TechnicalItem(
                    label = "DATA USCITA",
                    value = details.releaseDate ?: details.firstAirDate ?: "—",
                    icon = Icons.Rounded.CalendarMonth,
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
                TechnicalItem(
                    label = "BUDGET",
                    value = if (details.budget != null && details.budget > 0) "${formatter.format(details.budget)} USD" else "—",
                    icon = Icons.Rounded.AttachMoney,
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: Language & Revenue
            Row(modifier = Modifier.fillMaxWidth()) {
                TechnicalItem(
                    label = "LINGUA",
                    value = details.originalLanguage?.uppercase(Locale.getDefault()) ?: "—",
                    icon = Icons.Rounded.Translate,
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
                TechnicalItem(
                    label = "INCASSI",
                    value = if (details.revenue != null && details.revenue > 0) "${formatter.format(details.revenue)} USD" else "—",
                    icon = Icons.Rounded.Public,
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 3: Production & Country
            Row(modifier = Modifier.fillMaxWidth()) {
                TechnicalItem(
                    label = "PRODUZIONE",
                    value = details.productionCompanies?.firstOrNull()?.name ?: "—",
                    icon = Icons.Rounded.Business,
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
                TechnicalItem(
                    label = "PAESE",
                    value = details.productionCountries?.firstOrNull()?.name ?: "—",
                    icon = Icons.Rounded.AccountBalance,
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
        
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = Color.White.copy(alpha = 0.3f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color.White,
                maxLines = 2
            )
        }
    }
}
