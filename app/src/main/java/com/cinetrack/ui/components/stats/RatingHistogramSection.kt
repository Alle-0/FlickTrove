package com.cinetrack.ui.components.stats

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.roundToInt

@Composable
fun TimeRangePill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f),
        border = if (isSelected) null else BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.animateContentSize()
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (isSelected) Color.Black else Color.White,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
        )
    }
}

// ════════════════════════════════════════════════════════════════════
// Rating Histogram
// Buckets: index 0 = 0.5★, index 1 = 1.0★, ..., index 19 = 10.0★
// Labels below: 0.5, 1, 1.5, 2 ... 10  (every half point)
// Count above each bar
// ════════════════════════════════════════════════════════════════════
@Composable
fun RatingHistogram(distribution: ImmutableList<Int>) {
    val maxCount = distribution.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f
    val totalRatedMovies = remember(distribution) { distribution.sum() }
    var selectedRatingIdx by remember { mutableStateOf<Int?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statsCard(RoundedCornerShape(32.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                for (i in 1..20) {
                    val idx = i - 1
                    val count = if (idx < distribution.size) distribution[idx] else 0
                    val fraction = count / maxCount
                    val isMax = count.toFloat() == maxCount
                    
                    val animFrac by animateFloatAsState(
                        targetValue = fraction,
                        animationSpec = tween(durationMillis = 1000, delayMillis = i * 30),
                        label = "ratingBar_$i"
                    )

                    val isSelected = selectedRatingIdx == idx
                    val glowColor = if (isMax) MaterialTheme.colorScheme.primary else Color(0xFF00F2FE)
                    
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.25f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "ratingScale_$i"
                    )

                    val percentage = if (totalRatedMovies > 0) (count.toFloat() / totalRatedMovies * 100).roundToInt() else 0

                    Column(
                        modifier = Modifier
                            .zIndex(if (isSelected) 1f else 0f)
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                selectedRatingIdx = if (isSelected) null else idx
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // The Bar
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .align(Alignment.BottomCenter)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = 1.0f
                                        transformOrigin = TransformOrigin(0.5f, 1.0f)
                                    },
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                val minHeight = if (count > 0) 0.08f else 0.0f
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(animFrac.coerceAtLeast(minHeight))
                                        .background(
                                            brush = Brush.verticalGradient(
                                                listOf(
                                                    if (isSelected) glowColor else if (isMax) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                                    if (isSelected) glowColor.copy(0.3f) else if (isMax) MaterialTheme.colorScheme.primary.copy(0.3f) else MaterialTheme.colorScheme.primary.copy(0.1f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                        )
                                        .border(
                                            width = if (isSelected) 1.dp else 0.dp,
                                            color = if (isSelected) glowColor else Color.Transparent,
                                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                        )
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                )
                            }

                            // Peak count (when not selected)
                            androidx.compose.animation.AnimatedVisibility(
                                visible = !isSelected,
                                enter = fadeIn(),
                                exit = fadeOut(),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .offset(y = - (100.dp * animFrac.coerceAtLeast(if (count > 0) 0.08f else 0.0f) + 6.dp))
                            ) {
                                if (count > 0 && isMax) {
                                    CountingText(
                                        target = count,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            // Tooltip (when selected)
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isSelected,
                                enter = fadeIn() + scaleIn(transformOrigin = TransformOrigin(0.5f, 1f)),
                                exit = fadeOut() + scaleOut(transformOrigin = TransformOrigin(0.5f, 1f)),
                                modifier = Modifier
                                    .zIndex(10f)
                                    .align(Alignment.BottomCenter)
                                    .offset(y = - (100.dp * animFrac.coerceAtLeast(if (count > 0) 0.08f else 0.0f) + 6.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .wrapContentSize(unbounded = true)
                                        .background(
                                            Color(0xFF0F0F1A).copy(alpha = 0.95f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .border(
                                            1.dp,
                                            glowColor.copy(alpha = 0.4f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        val percentageText = if (percentage == 0 && count > 0) "<1%" else "$percentage%"
                                        Text(
                                            text = percentageText,
                                            color = glowColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = if (count == 1) stringResource(R.string.stats_one_movie) else stringResource(R.string.stats_many_movies, count),
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(10.dp))
            
            Row(Modifier.fillMaxWidth()) {
                for (i in 1..20) {
                    val label = if (i % 2 == 0) {
                        "${i / 2}"
                    } else {
                        ""
                    }
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        color = if (i == 20) MaterialTheme.colorScheme.primary.copy(0.8f) else Color.White.copy(0.45f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
