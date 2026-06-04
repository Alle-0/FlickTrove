package com.cinetrack.ui.components.detail

import com.cinetrack.R

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.foundation.background
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
    val label: String,
    val description: String,
    val color: Color
)

private val RATING_DATA = listOf(
    RatingInfo("G", "PER TUTTI", "Film adatto a tutte le età. Non contiene scene o linguaggi che possano offendere i genitori o i bambini.", Color(0xFF4CAF50)),
    RatingInfo("PG", "CONSIGLIATA SUPERVISIONE", "Alcune scene potrebbero non essere adatte ai bambini. Si consiglia la presenza di un genitore per spiegare eventuali temi.", Color(0xFFFFC107)),
    RatingInfo("PG-13", "SCONSIGLIATO SOTTO I 13", "Alcuni contenuti potrebbero essere inappropriati per minori di 13 anni. Richiede cautela da parte dei genitori.", Color(0xFFFF9800)),
    RatingInfo("R", "VIETATO AI MINORI (V.M. 17)", "Richiede l'accompagnamento di un adulto. Contiene temi maturi, linguaggio forte, violenza o nudità.", Color(0xFFF44336)),
    RatingInfo("NC-17", "RIGOROSAMENTE ADULTI", "Contenuto esclusivamente per un pubblico adulto. Assolutamente vietato l'ingresso ai minori.", Color(0xFFB71C1C)),
    RatingInfo("TV-14", "SCONSIGLIATO SOTTO I 14", "Questo programma contiene materiale che molti genitori potrebbero trovare inappropriato per bambini sotto i 14 anni.", Color(0xFFFF7043)),
    RatingInfo("TV-MA", "CONTENUTO PER ADULTI", "Programma specificamente progettato per essere visionato da adulti e potrebbe essere inappropriato per minori di 17 anni.", Color(0xFFD32F2F))
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
                    text = "Guida alle Classificazioni",
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
                        text = "I dati provengono dal sistema MPAA (USA), lo standard internazionale per la classificazione dei film.",
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
                            text = "FlickTrove visualizza queste informazioni per aiutarti a scegliere il film giusto per la tua serata.",
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
                text = rating.label,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.3.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = rating.description,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
