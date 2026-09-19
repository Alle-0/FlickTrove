package com.cinetrack.ui.components.stats

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowDown
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.utils.bounceClick
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// ════════════════════════════════════════════════════════════════════
// Genre Distribution – Treemap / Donut
// ════════════════════════════════════════════════════════════════════

@Composable
private fun LegendItem(
    genre: String,
    count: Int,
    percentage: Int,
    color: Color,
    isSelected: Boolean,
    isDimmed: Boolean,
    onClick: () -> Unit
) {
    val bgAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.12f else 0.02f,
        label = "bgAlpha"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = bgAlpha))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        
        Spacer(Modifier.width(14.dp))
        
        Text(
            text = genre,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isSelected) color else Color.White.copy(alpha = if (isDimmed) 0.35f else 0.9f)
            ),
            modifier = Modifier.weight(1f)
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = if (isSelected) color else Color.White.copy(alpha = if (isDimmed) 0.35f else 0.8f)
                )
            )
            Spacer(Modifier.width(8.dp))
            val percentageText = if (percentage == 0 && count > 0) "<1%" else "$percentage%"
            Text(
                text = percentageText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = if (isSelected) 0.5f else 0.25f)
                )
            )
        }
    }
}

@Composable
fun GenreDistributionSection(
    genreCounts: List<Pair<String, Int>>, 
    expanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    if (genreCounts.isEmpty()) return
    
    val textMeasurer = rememberTextMeasurer()
    
    val totalCount = genreCounts.sumOf { it.second }
    
    val otherGenreLabel = stringResource(R.string.stats_genre_other)
    val processedGenres = remember(genreCounts, otherGenreLabel) {
        if (genreCounts.size <= 8) {
            genreCounts
        } else {
            val mainGenres = genreCounts.take(7)
            val othersCount = genreCounts.drop(7).sumOf { it.second }
            mainGenres + Pair(otherGenreLabel, othersCount)
        }
    }
    
    var selectedGenreName by remember { mutableStateOf<String?>(null) }
    
    val genreColors = listOf(
        Color(0xFF00F2FE), // Cyan Neon
        Color(0xFF4FACFE), // Blue Neon
        Color(0xFFF355DA), // Magenta Neon
        Color(0xFFFFB300), // Gold/Amber Neon
        Color(0xFF00FF87), // Green Neon
        Color(0xFFFF4F4F), // Red Neon
        Color(0xFF9D50BB), // Purple
        Color(0xFFFF8C00), // Orange
        Color(0xFF00E676), // Bright Green
        Color(0xFFD500F9), // Deep Purple
        Color(0xFFE040FB), // Light Purple
        Color(0xFF18FFFF)  // Cyan 2
    )
    
    val chartSelectedIndex = remember(selectedGenreName, processedGenres) {
        if (selectedGenreName == null) {
            -1
        } else {
            val idx = processedGenres.indexOfFirst { it.first == selectedGenreName }
            if (idx != -1) {
                idx
            } else {
                processedGenres.lastIndex
            }
        }
    }
    
    val animOffsets = processedGenres.mapIndexed { idx, _ ->
        animateFloatAsState(
            targetValue = if (chartSelectedIndex == idx) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "offset_$idx"
        )
    }
    
    val initialAnim by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "initialAnim"
    )
    
    val animatedHaloColor by animateColorAsState(
        targetValue = if (selectedGenreName != null && chartSelectedIndex != -1) {
            genreColors[chartSelectedIndex % genreColors.size]
        } else {
            MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(500),
        label = "haloColor"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statsCard(RoundedCornerShape(32.dp))
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(310.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                val density = androidx.compose.ui.platform.LocalDensity.current
                val strokeWidthPx = with(density) { 28.dp.toPx() }
                val donutRadius = with(density) { 118.dp.toPx() }
                
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(processedGenres, initialAnim) {
                            val canvasSize = size
                            detectTapGestures { offset ->
                                val canvasWidth = canvasSize.width
                                val canvasHeight = canvasSize.height
                                val centerX = canvasWidth / 2f
                                val centerY = canvasHeight / 2f
                                val dx = offset.x - centerX
                                val dy = offset.y - centerY
                                val distance = sqrt(dx * dx + dy * dy)
                                
                                val outerRadius = donutRadius
                                val innerRadius = outerRadius - strokeWidthPx
                                
                                if (distance in innerRadius..outerRadius) {
                                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    if (angle < 0) angle += 360f
                                    val normalizedAngle = (angle + 90f) % 360f
                                    
                                    var currentAngle = 0f
                                    var foundIndex = -1
                                    for (i in processedGenres.indices) {
                                        val sweep = (processedGenres[i].second.toFloat() / totalCount) * 360f
                                        if (normalizedAngle >= currentAngle && normalizedAngle < currentAngle + sweep) {
                                            foundIndex = i
                                            break
                                        }
                                        currentAngle += sweep
                                    }
                                    
                                    if (foundIndex != -1) {
                                        val clickedGenre = processedGenres[foundIndex].first
                                        selectedGenreName = if (selectedGenreName == clickedGenre) null else clickedGenre
                                    }
                                } else {
                                    selectedGenreName = null
                                }
                            }
                        }
                ) {
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    var currentStartAngle = -90f
                    
                    processedGenres.forEachIndexed { index, (genre, count) ->
                        val sweepAngle = (count.toFloat() / totalCount) * 360f * initialAnim
                        val isSelected = (chartSelectedIndex == index)
                        val selectionOffset = animOffsets[index].value * 8.dp.toPx()
                        
                        val midAngle = currentStartAngle + sweepAngle / 2f
                        val rad = Math.toRadians(midAngle.toDouble())
                        val cosVal = cos(rad).toFloat()
                        val sinVal = sin(rad).toFloat()
                        val offsetX = cosVal * selectionOffset
                        val offsetY = sinVal * selectionOffset
                        
                        val currentStrokeWidth = if (isSelected) strokeWidthPx + 4.dp.toPx() else strokeWidthPx
                        val color = genreColors[index % genreColors.size]
                        val alpha = if (chartSelectedIndex == -1 || isSelected) 1f else 0.25f
                        
                        val pathRadius = donutRadius - strokeWidthPx / 2f
                        val arcSize = androidx.compose.ui.geometry.Size(pathRadius * 2f, pathRadius * 2f)
                        val arcTopLeft = Offset(centerX - pathRadius + offsetX, centerY - pathRadius + offsetY)
                        
                        if (isSelected) {
                            drawArc(
                                color = color.copy(alpha = 0.25f),
                                startAngle = currentStartAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcSize,
                                style = Stroke(width = currentStrokeWidth + 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        
                        drawArc(
                            color = color.copy(alpha = alpha),
                            startAngle = currentStartAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(width = currentStrokeWidth, cap = if (isSelected) StrokeCap.Round else StrokeCap.Butt)
                        )
                        
                        // Draw genre text label directly next to donut slice
                        val percentage = if (totalCount > 0) (count.toFloat() / totalCount * 100).roundToInt() else 0
                        val percentageText = if (percentage == 0 && count > 0) "<1%" else "$percentage%"
                        if (sweepAngle > 8f && count > 0) {
                            val textRadius = donutRadius + 12.dp.toPx() + selectionOffset
                            val textX = centerX + cosVal * textRadius
                            val textY = centerY + sinVal * textRadius
                            
                            val titleCasedGenre = genre.lowercase().replaceFirstChar { it.uppercase() }
                            val labelText = "$titleCasedGenre $percentageText"
                            
                            val textLayoutResult = textMeasurer.measure(
                                text = labelText,
                                style = androidx.compose.ui.text.TextStyle(
                                    color = color.copy(alpha = alpha),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            val textWidth = textLayoutResult.size.width
                            val textHeight = textLayoutResult.size.height
                            
                            val x = if (cosVal > 0.2f) {
                                textX
                            } else if (cosVal < -0.2f) {
                                textX - textWidth
                            } else {
                                textX - textWidth / 2f
                            }
                            
                            val y = if (sinVal > 0.2f) {
                                textY
                            } else if (sinVal < -0.2f) {
                                textY - textHeight
                            } else {
                                textY - textHeight / 2f
                            }
                            
                            // Leader line connecting the arc outer boundary to the label
                            val lineStartX = centerX + cosVal * (donutRadius + 2.dp.toPx() + selectionOffset)
                            val lineStartY = centerY + sinVal * (donutRadius + 2.dp.toPx() + selectionOffset)
                            val lineEndX = centerX + cosVal * (donutRadius + 8.dp.toPx() + selectionOffset)
                            val lineEndY = centerY + sinVal * (donutRadius + 8.dp.toPx() + selectionOffset)
                            
                            drawLine(
                                color = color.copy(alpha = alpha * 0.4f),
                                start = Offset(lineStartX, lineStartY),
                                end = Offset(lineEndX, lineEndY),
                                strokeWidth = 1.dp.toPx()
                            )
                            
                            drawText(
                                textLayoutResult = textLayoutResult,
                                topLeft = Offset(x, y)
                            )
                        }
                        
                        currentStartAngle += sweepAngle
                    }
                }
                
                // 1. Organic Breathing Inner Neon Halo
                val advancedEffectsEnabled = com.cinetrack.LocalAdvancedVisualEffects.current
                val (haloScale, haloAlpha) = if (advancedEffectsEnabled) {
                    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
                    val breathingScale by infiniteTransition.animateFloat(
                        initialValue = 0.96f,
                        targetValue = 1.04f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )
                    val breathingAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.15f,
                        targetValue = 0.35f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )
                    breathingScale to breathingAlpha
                } else {
                    1f to 0.25f
                }

                Box(
                    modifier = Modifier
                        .size(165.dp)
                        .graphicsLayer {
                            scaleX = haloScale
                            scaleY = haloScale
                            alpha = haloAlpha
                        }
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    animatedHaloColor,
                                    animatedHaloColor.copy(alpha = 0f)
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // 2. Glowing Frosted Glass Circle Card
                Box(
                    modifier = Modifier
                        .size(158.dp)
                        .background(Color(0xFF0F0F1A).copy(alpha = 0.75f), CircleShape)
                        .border(
                            width = 1.5.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    animatedHaloColor.copy(alpha = 0.5f),
                                    Color.White.copy(alpha = 0.05f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        val genreName = selectedGenreName
                        if (genreName != null) {
                            val count = if (genreName == stringResource(R.string.stats_genre_other)) {
                                processedGenres.firstOrNull { it.first == stringResource(R.string.stats_genre_other) }?.second ?: 0
                            } else {
                                genreCounts.firstOrNull { it.first == genreName }?.second ?: 0
                            }
                            val percentage = ((count.toFloat() / totalCount) * 100).roundToInt()
                            val percentageText = if (percentage == 0 && count > 0) "<1%" else "$percentage%"
                            val countColor = if (chartSelectedIndex != -1) {
                                genreColors[chartSelectedIndex % genreColors.size]
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                            
                            Text(
                                text = genreName.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White.copy(alpha = 0.5f),
                                    letterSpacing = 1.5.sp,
                                    fontSize = 9.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            CountingText(
                                target = count,
                                color = countColor,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (count == 1) stringResource(R.string.stats_title_singular) else stringResource(R.string.stats_title_plural),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = countColor.copy(alpha = 0.7f),
                                    fontSize = 9.sp,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.stats_of_total, percentageText),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 9.sp
                                )
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.stats_genres),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White.copy(alpha = 0.5f),
                                    letterSpacing = 1.5.sp,
                                    fontSize = 10.sp
                                )
                            )
                            Spacer(Modifier.height(2.dp))
                            CountingText(
                                target = genreCounts.size,
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.stats_watched),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 9.sp,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.stats_tap_slice),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    fontSize = 8.sp,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = if (expanded) 320.dp else 200.dp)
            ) {
                val scrollState = rememberScrollState()
                val nestedScrollConnection = remember {
                    object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
                        override fun onPostScroll(
                            consumed: androidx.compose.ui.geometry.Offset,
                            available: androidx.compose.ui.geometry.Offset,
                            source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
                        ): androidx.compose.ui.geometry.Offset {
                            return available
                        }
                    }
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .graphicsLayer(compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen)
                        .drawWithContent {
                            drawContent()
                            if (scrollState.value > 0) {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        1f to Color.Black,
                                        startY = 0f,
                                        endY = size.height * 0.15f
                                    ),
                                    blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                                )
                            }
                            if (scrollState.value < scrollState.maxValue) {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0f to Color.Black,
                                        1f to Color.Transparent,
                                        startY = size.height * 0.75f,
                                        endY = size.height
                                    ),
                                    blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                                )
                            }
                        }
                        .nestedScroll(nestedScrollConnection)
                        .verticalScroll(scrollState)
                        .padding(vertical = 12.dp)
                ) {
                    val displayList = if (expanded) genreCounts else genreCounts.take(7)
                    
                    displayList.forEachIndexed { index, (genre, count) ->
                        val percentage = ((count.toFloat() / totalCount) * 100).roundToInt()
                        
                        val color = if (index < 7) genreColors[index] else genreColors[7]
                        
                        val isItemSelected = (selectedGenreName == genre)
                        val isDimmed = selectedGenreName != null && !isItemSelected
                        
                        LegendItem(
                            genre = genre,
                            count = count,
                            percentage = percentage,
                            color = color,
                            isSelected = isItemSelected,
                            isDimmed = isDimmed,
                            onClick = {
                                selectedGenreName = if (isItemSelected) null else genre
                            }
                        )
                        if (index < displayList.lastIndex) {
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
                
                if (expanded) {
                    val scrollFraction by remember {
                        derivedStateOf { if (scrollState.maxValue > 0) scrollState.value.toFloat() / scrollState.maxValue else 0f }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, bottom = 10.dp, end = 4.dp)
                            .fillMaxHeight()
                            .width(3.dp)
                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.2f)
                                .offset(y = (240 * scrollFraction).dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                        )
                    }
                }
            }
            
            if (genreCounts.size > 5) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .bounceClick(onClick = { onToggleExpanded() })
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (expanded) stringResource(R.string.stats_see_less) else stringResource(R.string.stats_see_all),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Rounded.KeyboardDoubleArrowUp else Icons.Rounded.KeyboardDoubleArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
