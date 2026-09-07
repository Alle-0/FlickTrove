package com.cinetrack.ui.components.detail

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.cinetrack.R
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.premiumScrollbar
import com.cinetrack.ui.utils.verticalFadingEdges
import dev.chrisbanes.haze.HazeState

@Composable
fun DetailRatingInfoDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    hazeState: HazeState,
    countryCode: String? = null,
    isTv: Boolean = false,
    onViewGuideClick: (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = Modifier.zIndex(100f)
    ) {
        val blurAlpha by transition.animateFloat(
            transitionSpec = { tween(200) },
            label = "blurAlpha"
        ) { if (it == androidx.compose.animation.EnterExitState.Visible) 0.85f else 0f }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = HazeStyles.ModalScrimAlpha * (blurAlpha / 0.85f)))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth(0.85f)
                    .hazeGlass(
                        state = hazeState,
                        alpha = blurAlpha,
                        shape = RoundedCornerShape(32.dp)
                    )
                    .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {}
            ) {
                val dialogScrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .heightIn(max = 500.dp)
                ) {
                    // Title (Fixed)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                    ) {
                        Icon(
                            ImageVector.vectorResource(id = R.drawable.ic_documento),
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.detail_rating_legend_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    // Scrollable Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .premiumScrollbar(dialogScrollState)
                            .padding(end = 12.dp)
                            .verticalFadingEdges(dialogScrollState, 16.dp, 16.dp)
                            .verticalScroll(dialogScrollState)
                    ) {
                        when (countryCode?.uppercase()) {
                            "IT" -> {
                                Text(
                                    stringResource(R.string.detail_rating_country_it),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    RatingLegendItem("T", Color(0xFF4CAF50), stringResource(R.string.detail_rating_it_t))
                                    RatingLegendItem("6+", Color(0xFF8BC34A), stringResource(R.string.detail_rating_it_6))
                                    RatingLegendItem("VM14", Color(0xFFFF9800), stringResource(R.string.detail_rating_it_14))
                                    RatingLegendItem("VM18", Color(0xFFF44336), stringResource(R.string.detail_rating_it_18))
                                }
                            }
                            "DE" -> {
                                Text(
                                    stringResource(R.string.detail_rating_country_de),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    RatingLegendItem("FSK 0", Color(0xFF4CAF50), stringResource(R.string.detail_rating_de_0))
                                    RatingLegendItem("FSK 6", Color(0xFFFFC107), stringResource(R.string.detail_rating_de_6))
                                    RatingLegendItem("FSK 12", Color(0xFF8BC34A), stringResource(R.string.detail_rating_de_12))
                                    RatingLegendItem("FSK 16", Color(0xFFFF9800), stringResource(R.string.detail_rating_de_16))
                                    RatingLegendItem("FSK 18", Color(0xFFF44336), stringResource(R.string.detail_rating_de_18))
                                }
                            }
                            "FR" -> {
                                Text(
                                    stringResource(R.string.detail_rating_country_fr),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    RatingLegendItem("U", Color(0xFF4CAF50), stringResource(R.string.detail_rating_fr_u))
                                    RatingLegendItem("10", Color(0xFFFFC107), stringResource(R.string.detail_rating_fr_10))
                                    RatingLegendItem("12", Color(0xFFFF9800), stringResource(R.string.detail_rating_fr_12))
                                    RatingLegendItem("16", Color(0xFFFF5722), stringResource(R.string.detail_rating_fr_16))
                                    RatingLegendItem("18", Color(0xFFF44336), stringResource(R.string.detail_rating_fr_18))
                                }
                            }
                            "ES" -> {
                                Text(
                                    stringResource(R.string.detail_rating_country_es),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    RatingLegendItem("APTA", Color(0xFF4CAF50), stringResource(R.string.detail_rating_es_apta))
                                    RatingLegendItem("7", Color(0xFFFFC107), stringResource(R.string.detail_rating_es_7))
                                    RatingLegendItem("12", Color(0xFFFF9800), stringResource(R.string.detail_rating_es_12))
                                    RatingLegendItem("16", Color(0xFFFF5722), stringResource(R.string.detail_rating_es_16))
                                    RatingLegendItem("18", Color(0xFFF44336), stringResource(R.string.detail_rating_es_18))
                                }
                            }
                            "BR" -> {
                                Text(
                                    stringResource(R.string.detail_rating_country_br),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    RatingLegendItem("L", Color(0xFF4CAF50), stringResource(R.string.detail_rating_br_l))
                                    RatingLegendItem("10", Color(0xFF8BC34A), stringResource(R.string.detail_rating_br_10))
                                    RatingLegendItem("12", Color(0xFFFFC107), stringResource(R.string.detail_rating_br_12))
                                    RatingLegendItem("14", Color(0xFFFF9800), stringResource(R.string.detail_rating_br_14))
                                    RatingLegendItem("16", Color(0xFFFF5722), stringResource(R.string.detail_rating_br_16))
                                    RatingLegendItem("18", Color(0xFFF44336), stringResource(R.string.detail_rating_br_18))
                                }
                            }
                            "RU" -> {
                                Text(
                                    stringResource(R.string.detail_rating_country_ru),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    RatingLegendItem("0+", Color(0xFF4CAF50), stringResource(R.string.detail_rating_ru_0))
                                    RatingLegendItem("6+", Color(0xFF8BC34A), stringResource(R.string.detail_rating_ru_6))
                                    RatingLegendItem("12+", Color(0xFFFF9800), stringResource(R.string.detail_rating_ru_12))
                                    RatingLegendItem("16+", Color(0xFFFF5722), stringResource(R.string.detail_rating_ru_16))
                                    RatingLegendItem("18+", Color(0xFFF44336), stringResource(R.string.detail_rating_ru_18))
                                }
                            }
                            "IN" -> {
                                Text(
                                    stringResource(R.string.detail_rating_country_in),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    RatingLegendItem("U", Color(0xFF4CAF50), stringResource(R.string.detail_rating_in_u))
                                    RatingLegendItem("UA", Color(0xFFFF9800), stringResource(R.string.detail_rating_in_ua))
                                    RatingLegendItem("A", Color(0xFFF44336), stringResource(R.string.detail_rating_in_a))
                                    RatingLegendItem("S", Color(0xFF9E9E9E), stringResource(R.string.detail_rating_in_s))
                                }
                            }
                            "GB" -> {
                                Text(
                                    stringResource(R.string.detail_rating_country_gb),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    RatingLegendItem("U", Color(0xFF4CAF50), stringResource(R.string.detail_rating_gb_u))
                                    RatingLegendItem("PG", Color(0xFF8BC34A), stringResource(R.string.detail_rating_gb_pg))
                                    RatingLegendItem("12A", Color(0xFFFF9800), stringResource(R.string.detail_rating_gb_12))
                                    RatingLegendItem("15", Color(0xFFFF5722), stringResource(R.string.detail_rating_gb_15))
                                    RatingLegendItem("18", Color(0xFFF44336), stringResource(R.string.detail_rating_gb_18))
                                }
                            }
                            else -> {
                                // Default / US Fallback
                                if (isTv) {
                                    Text(
                                        stringResource(R.string.detail_rating_tv_usa),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        RatingLegendItem("TV-Y", Color(0xFF4CAF50), stringResource(R.string.detail_rating_tvy))
                                        RatingLegendItem("TV-Y7", Color(0xFF8BC34A), stringResource(R.string.detail_rating_tvy7))
                                        RatingLegendItem("TV-G", Color(0xFF66BB6A), stringResource(R.string.detail_rating_tvg))
                                        RatingLegendItem("TV-PG", Color(0xFFFF9800), stringResource(R.string.detail_rating_tvpg))
                                        RatingLegendItem("TV-14", Color(0xFFF44336), stringResource(R.string.detail_rating_tv14))
                                        RatingLegendItem("TV-MA", Color(0xFFD32F2F), stringResource(R.string.detail_rating_tvma))
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Text(
                                        stringResource(R.string.detail_rating_movies_usa),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        RatingLegendItem("G", Color(0xFF4CAF50), stringResource(R.string.detail_rating_g))
                                        RatingLegendItem("PG", Color(0xFF8BC34A), stringResource(R.string.detail_rating_pg))
                                        RatingLegendItem("PG-13", Color(0xFFFF9800), stringResource(R.string.detail_rating_pg13))
                                        RatingLegendItem("R", Color(0xFFF44336), stringResource(R.string.detail_rating_r))
                                        RatingLegendItem("NC-17", Color(0xFFD32F2F), stringResource(R.string.detail_rating_nc17))
                                        RatingLegendItem("NR", Color(0xFF9E9E9E), stringResource(R.string.detail_rating_nr))
                                    }
                                } else {
                                    Text(
                                        stringResource(R.string.detail_rating_movies_usa),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        RatingLegendItem("G", Color(0xFF4CAF50), stringResource(R.string.detail_rating_g))
                                        RatingLegendItem("PG", Color(0xFF8BC34A), stringResource(R.string.detail_rating_pg))
                                        RatingLegendItem("PG-13", Color(0xFFFF9800), stringResource(R.string.detail_rating_pg13))
                                        RatingLegendItem("R", Color(0xFFF44336), stringResource(R.string.detail_rating_r))
                                        RatingLegendItem("NC-17", Color(0xFFD32F2F), stringResource(R.string.detail_rating_nc17))
                                        RatingLegendItem("NR", Color(0xFF9E9E9E), stringResource(R.string.detail_rating_nr))
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Text(
                                        stringResource(R.string.detail_rating_tv_usa),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        RatingLegendItem("TV-Y", Color(0xFF4CAF50), stringResource(R.string.detail_rating_tvy))
                                        RatingLegendItem("TV-Y7", Color(0xFF8BC34A), stringResource(R.string.detail_rating_tvy7))
                                        RatingLegendItem("TV-G", Color(0xFF66BB6A), stringResource(R.string.detail_rating_tvg))
                                        RatingLegendItem("TV-PG", Color(0xFFFF9800), stringResource(R.string.detail_rating_tvpg))
                                        RatingLegendItem("TV-14", Color(0xFFF44336), stringResource(R.string.detail_rating_tv14))
                                        RatingLegendItem("TV-MA", Color(0xFFD32F2F), stringResource(R.string.detail_rating_tvma))
                                    }
                                }
                            }
                        }
                    }

                    // Action buttons (Fixed)
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    if (onViewGuideClick != null) {
                        val accent = MaterialTheme.colorScheme.primary
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .bounceClick(onClick = {
                                        onDismiss()
                                        onViewGuideClick()
                                    })
                                    .background(accent.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                                    .border(0.5.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.detail_rating_view_guide),
                                    color = accent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick(onClick = onDismiss)
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.settings_close), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingLegendItem(cert: String, color: Color, desc: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(55.dp)
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = cert,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f)
        )
    }
}
