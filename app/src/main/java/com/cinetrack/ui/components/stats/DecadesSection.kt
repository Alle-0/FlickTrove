package com.cinetrack.ui.components.stats

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.cinetrack.R
import kotlin.math.roundToInt

// ════════════════════════════════════════════════════════════════════
// Decades bar chart
// ════════════════════════════════════════════════════════════════════

@Composable
fun DecadesSection(decadeCounts: List<Pair<String, Int>>) {
    val max = decadeCounts.maxOfOrNull { it.second }?.toFloat() ?: 1f
    val totalMovies = remember(decadeCounts) { decadeCounts.sumOf { it.second } }
    var selectedDecadeIdx by remember { mutableStateOf<Int?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statsCard(RoundedCornerShape(32.dp))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom
        ) {
            decadeCounts.forEachIndexed { idx, (decade, count) ->
                val fraction = count.toFloat() / max
                val isMax = count.toFloat() == max
                val animFrac by animateFloatAsState(fraction, tween(1000, idx * 80, FastOutSlowInEasing), label = "dec$idx")
                
                val isSelected = selectedDecadeIdx == idx
                val glowColor = if (isMax) MaterialTheme.colorScheme.primary else Color(0xFF00F2FE)
                
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "barScale_$idx"
                )
                
                val percentage = if (totalMovies > 0) (count.toFloat() / totalMovies * 100).roundToInt() else 0
                val percentageText = if (percentage == 0 && count > 0) "<1%" else "$percentage%"
                
                Column(
                    Modifier
                        .zIndex(if (isSelected) 1f else 0f)
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            selectedDecadeIdx = if (isSelected) null else idx
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn() + scaleIn(transformOrigin = TransformOrigin(0.5f, 1f)),
                            exit = fadeOut() + scaleOut(transformOrigin = TransformOrigin(0.5f, 1f)),
                            modifier = Modifier.zIndex(10f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .wrapContentSize(unbounded = true)
                                    .offset(y = (-4).dp)
                                    .background(
                                        Color(0xFF0F0F1A).copy(alpha = 0.9f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        1.dp,
                                        glowColor.copy(alpha = 0.4f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = percentageText,
                                        color = glowColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = if (count == 1) stringResource(R.string.stats_1_movie) else stringResource(R.string.stats_n_movies, count),
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                        
                        androidx.compose.animation.AnimatedVisibility(
                            visible = !isSelected,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            CountingText(
                                target = count,
                                color = if (isMax) MaterialTheme.colorScheme.primary else Color.White.copy(0.7f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(3.dp))
                    
                    Box(
                        Modifier
                            .fillMaxWidth(0.6f)
                            .height(90.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                transformOrigin = TransformOrigin(0.5f, 1.0f)
                            },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        val minHeight = if (count > 0) 0.06f else 0.0f
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(animFrac.coerceAtLeast(minHeight))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            if (isSelected) glowColor else if (isMax) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(0.45f),
                                            if (isSelected) glowColor.copy(0.3f) else if (isMax) MaterialTheme.colorScheme.primary.copy(0.3f) else MaterialTheme.colorScheme.primary.copy(0.1f)
                                        )
                                    ),
                                    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) glowColor else Color.Transparent,
                                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                                )
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        decade,
                        color = if (isSelected) glowColor else Color.White.copy(0.55f),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
