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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.cinetrack.LocalAdvancedVisualEffects
import com.cinetrack.R
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.VibeStat
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Sezione Top Vibes: Donut Chart interattivo come nelle statistiche generali,
 * con alone luminoso organico, fette cliccabili, info al centro ed etichette visibili.
 */
@Composable
private fun VibeLegendItem(
    vibe: VibeStat,
    percentage: Int,
    color: Color,
    isSelected: Boolean,
    isDimmed: Boolean,
    onClick: () -> Unit
) {
    val bgAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.12f else 0.02f,
        label = "vibe_bgAlpha"
    )
    val vibeName = vibe.labelRes?.let { stringResource(it) }
        ?: vibe.vibe.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .bounceClick(onClick = onClick)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = bgAlpha))
            .padding(start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconRes = if (vibe.vibe == "OTHER") null else vibe.iconRes
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(color.copy(alpha = if (isSelected) 0.25f else 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (iconRes != null && iconRes != 0) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = color
                )
            } else if (vibe.emoji.isNotEmpty()) {
                Text(
                    text = vibe.emoji,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = vibeName,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isSelected) color else Color.White.copy(alpha = if (isDimmed) 0.35f else 0.9f)
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.width(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${vibe.count}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = if (isSelected) color else Color.White.copy(alpha = if (isDimmed) 0.35f else 0.8f)
                )
            )
            Spacer(Modifier.width(8.dp))
            val percentageText = if (percentage == 0 && vibe.count > 0) "<1%" else "$percentage%"
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

/**
 * Sezione Top Vibes: Donut Chart con lista verticale identica alla schermata statistiche normali (GenreDistributionSection)
 */
@Composable
fun TopVibesSection(vibes: List<VibeStat>) {
    if (vibes.isEmpty()) return

    val totalCount = remember(vibes) { vibes.sumOf { it.count } }
    val textMeasurer = rememberTextMeasurer()

    val otherVibeLabel = stringResource(R.string.stats_genre_other)
    val processedVibes = remember(vibes, otherVibeLabel) {
        if (vibes.size <= 8) {
            vibes
        } else {
            val mainVibes = vibes.take(7)
            val othersCount = vibes.drop(7).sumOf { it.count }
            mainVibes + VibeStat(
                vibe = "OTHER",
                emoji = "",
                count = othersCount,
                iconRes = null,
                colorHex = 0xFFFF9800,
                labelRes = R.string.stats_genre_other
            )
        }
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val vibeDrawables = remember(processedVibes) {
        processedVibes.associate { vibe ->
            val resId = if (vibe.vibe == "OTHER") null else vibe.iconRes
            vibe.vibe to (resId?.let { ContextCompat.getDrawable(context, it)?.mutate() })
        }
    }

    var selectedVibeCode by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }

    val chartSelectedIndex = remember(selectedVibeCode, processedVibes) {
        if (selectedVibeCode == null) {
            -1
        } else {
            val idx = processedVibes.indexOfFirst { it.vibe == selectedVibeCode }
            if (idx != -1) idx else processedVibes.lastIndex
        }
    }

    val animOffsets = processedVibes.mapIndexed { idx, _ ->
        animateFloatAsState(
            targetValue = if (chartSelectedIndex == idx) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "vibe_offset_$idx"
        )
    }

    var animStarted by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animStarted = true
    }

    val initialAnim by animateFloatAsState(
        targetValue = if (animStarted) 1f else 0f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "vibe_initialAnim"
    )

    var currentTotalCount by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(totalCount) {
        currentTotalCount = totalCount
    }

    val animatedTotalCount by animateIntAsState(
        targetValue = currentTotalCount,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "vibe_total_count"
    )

    val defaultAccentColor = vibes.firstOrNull()?.colorHex?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
    val animatedHaloColor by animateColorAsState(
        targetValue = if (selectedVibeCode != null && chartSelectedIndex != -1) {
            processedVibes[chartSelectedIndex].colorHex?.let { Color(it) } ?: defaultAccentColor
        } else {
            defaultAccentColor
        },
        animationSpec = tween(500),
        label = "vibe_haloColor"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .statsCard(RoundedCornerShape(32.dp))
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Donut Chart Box
            Box(
                modifier = Modifier
                    .size(310.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                val strokeWidthPx = with(density) { 26.dp.toPx() }
                val donutRadius = with(density) { 112.dp.toPx() }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(processedVibes, initialAnim) {
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
                                    for (i in processedVibes.indices) {
                                        val sweep = (processedVibes[i].count.toFloat() / totalCount) * 360f
                                        if (normalizedAngle >= currentAngle && normalizedAngle < currentAngle + sweep) {
                                            foundIndex = i
                                            break
                                        }
                                        currentAngle += sweep
                                    }

                                    if (foundIndex != -1) {
                                        val clickedCode = processedVibes[foundIndex].vibe
                                        selectedVibeCode = if (selectedVibeCode == clickedCode) null else clickedCode
                                    }
                                }
                            }
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val centerX = canvasWidth / 2f
                    val centerY = canvasHeight / 2f

                    var currentStartAngle = -90f
                    var selectedSliceDraw: (() -> Unit)? = null

                    processedVibes.forEachIndexed { i, vibe ->
                        val targetSweep = (vibe.count.toFloat() / totalCount) * 360f
                        val sweepAngle = targetSweep * initialAnim
                        val color = vibe.colorHex?.let { Color(it) } ?: Color.White

                        val isSelected = (chartSelectedIndex == i)
                        val isAnySelected = (chartSelectedIndex != -1)
                        val animOffset = animOffsets[i].value

                        val alpha = if (isAnySelected && !isSelected) 0.35f else 1f
                        val selectionOffset = animOffset * 9.dp.toPx()
                        val currentStrokeWidth = if (isSelected) strokeWidthPx + 5.dp.toPx() else strokeWidthPx

                        val midAngle = currentStartAngle + (sweepAngle / 2f)
                        val midRad = Math.toRadians(midAngle.toDouble())
                        val cosVal = cos(midRad).toFloat()
                        val sinVal = sin(midRad).toFloat()

                        val sliceOffsetX = cosVal * selectionOffset
                        val sliceOffsetY = sinVal * selectionOffset

                        val pathRadius = donutRadius - (strokeWidthPx / 2f)
                        val arcSize = pathRadius * 2f
                        val arcTopLeft = Offset(
                            x = centerX - pathRadius + sliceOffsetX,
                            y = centerY - pathRadius + sliceOffsetY
                        )

                        if (isSelected) {
                            val startAngleCapture = currentStartAngle
                            val sweepAngleCapture = sweepAngle
                            selectedSliceDraw = {
                                drawArc(
                                    color = color.copy(alpha = 0.25f),
                                    startAngle = startAngleCapture,
                                    sweepAngle = sweepAngleCapture,
                                    useCenter = false,
                                    topLeft = arcTopLeft,
                                    size = Size(arcSize, arcSize),
                                    style = Stroke(
                                        width = currentStrokeWidth + 8.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                )
                                drawArc(
                                    color = color.copy(alpha = alpha),
                                    startAngle = startAngleCapture,
                                    sweepAngle = sweepAngleCapture,
                                    useCenter = false,
                                    topLeft = arcTopLeft,
                                    size = Size(arcSize, arcSize),
                                    style = Stroke(
                                        width = currentStrokeWidth,
                                        cap = StrokeCap.Round
                                    )
                                )
                            }
                        } else {
                            drawArc(
                                color = color.copy(alpha = alpha),
                                startAngle = currentStartAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = Size(arcSize, arcSize),
                                style = Stroke(
                                    width = currentStrokeWidth,
                                    cap = StrokeCap.Butt
                                )
                            )
                        }

                        // Disegno etichette esterne
                        val isOther = vibe.vibe == "OTHER"
                        val otherVibeText = context.getString(R.string.stats_genre_other)
                        val vibeName = if (isOther) otherVibeText else vibe.labelRes?.let { context.getString(it) }
                            ?: vibe.vibe.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }
                        val countText = vibe.count.toString()
                        val labelText = if (isOther) "$otherVibeText $countText" else countText

                        if (sweepAngle > 8f && vibe.count > 0) {
                            val textRadius = donutRadius + 11.dp.toPx() + selectionOffset
                            val textX = centerX + cosVal * textRadius
                            val textY = centerY + sinVal * textRadius

                            val drawable = if (isOther) null else vibeDrawables[vibe.vibe]
                            val iconSizePx = with(density) { 15.dp.toPx() }
                            val spacingPx = with(density) { 3.dp.toPx() }

                            val countLayoutResult = textMeasurer.measure(
                                text = labelText,
                                style = TextStyle(
                                    color = color.copy(alpha = alpha),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                                )
                            )

                            val totalWidth = if (drawable != null) {
                                iconSizePx + spacingPx + countLayoutResult.size.width
                            } else {
                                countLayoutResult.size.width.toFloat()
                            }
                            val totalHeight = if (drawable != null) {
                                maxOf(iconSizePx, countLayoutResult.size.height.toFloat())
                            } else {
                                countLayoutResult.size.height.toFloat()
                            }

                            val x = if (cosVal > 0.2f) textX else if (cosVal < -0.2f) textX - totalWidth else textX - totalWidth / 2f
                            val y = if (sinVal > 0.2f) textY else if (sinVal < -0.2f) textY - totalHeight else textY - totalHeight / 2f

                            val lineStartX = centerX + cosVal * (donutRadius + 2.dp.toPx() + selectionOffset)
                            val lineStartY = centerY + sinVal * (donutRadius + 2.dp.toPx() + selectionOffset)
                            val lineEndX = centerX + cosVal * (donutRadius + 7.dp.toPx() + selectionOffset)
                            val lineEndY = centerY + sinVal * (donutRadius + 7.dp.toPx() + selectionOffset)

                            drawLine(
                                color = color.copy(alpha = alpha * 0.4f),
                                start = Offset(lineStartX, lineStartY),
                                end = Offset(lineEndX, lineEndY),
                                strokeWidth = 1.dp.toPx()
                            )

                            if (drawable != null) {
                                val iconLeft = x
                                val iconTop = y + (totalHeight - iconSizePx) / 2f
                                val iconRight = iconLeft + iconSizePx
                                val iconBottom = iconTop + iconSizePx

                                drawIntoCanvas { nativeCanvas ->
                                    drawable.setBounds(iconLeft.toInt(), iconTop.toInt(), iconRight.toInt(), iconBottom.toInt())
                                    drawable.setTint(color.copy(alpha = alpha).toArgb())
                                    drawable.draw(nativeCanvas.nativeCanvas)
                                }

                                val textXPos = iconRight + spacingPx
                                val textYPos = y + (totalHeight - countLayoutResult.size.height) / 2f

                                drawText(
                                    textLayoutResult = countLayoutResult,
                                    topLeft = Offset(textXPos, textYPos)
                                )
                            } else if (isOther) {
                                drawText(
                                    textLayoutResult = countLayoutResult,
                                    topLeft = Offset(x, y)
                                )
                            } else {
                                val fallbackLayoutResult = textMeasurer.measure(
                                    text = if (vibe.emoji.isNotEmpty()) "${vibe.emoji} $countText" else countText,
                                    style = TextStyle(
                                        color = color.copy(alpha = alpha),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                                    )
                                )
                                drawText(
                                    textLayoutResult = fallbackLayoutResult,
                                    topLeft = Offset(x, y)
                                )
                            }
                        }

                        currentStartAngle += sweepAngle
                    }

                    selectedSliceDraw?.invoke()
                }

                // Breathing Halo neon interno
                val advancedEffectsEnabled = LocalAdvancedVisualEffects.current
                val (haloScale, haloAlpha) = if (advancedEffectsEnabled) {
                    val infiniteTransition = rememberInfiniteTransition(label = "vibe_breathing")
                    val breathingScale by infiniteTransition.animateFloat(
                        initialValue = 0.96f,
                        targetValue = 1.04f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "vibe_scale"
                    )
                    val breathingAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.15f,
                        targetValue = 0.35f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "vibe_alpha"
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

                // Centro Frosted Glass
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        val activeCode = selectedVibeCode
                        if (activeCode != null) {
                            val targetVibe = processedVibes.firstOrNull { it.vibe == activeCode }
                            val count = targetVibe?.count ?: 0
                            val percentage = if (totalCount > 0) ((count.toFloat() / totalCount) * 100).roundToInt() else 0
                            val percentageText = if (percentage == 0 && count > 0) "<1%" else "$percentage%"
                            val countColor = targetVibe?.colorHex?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
                            val vibeDisplayName = targetVibe?.labelRes?.let { stringResource(it) }
                                ?: targetVibe?.vibe?.lowercase()?.replace("_", " ")?.replaceFirstChar { it.uppercase() }
                                ?: activeCode

                            val centerIconRes = if (targetVibe?.vibe == "OTHER") null else targetVibe?.iconRes
                            if (centerIconRes != null && centerIconRes != 0) {
                                Icon(
                                    painter = painterResource(id = centerIconRes),
                                    contentDescription = null,
                                    tint = countColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            } else if (targetVibe?.emoji?.isNotEmpty() == true) {
                                Text(
                                    text = targetVibe.emoji,
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }

                            Text(
                                text = vibeDisplayName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.9f)
                                ),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CountingText(
                                    target = count,
                                    color = countColor,
                                    fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "($percentageText)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Normal,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.flow_stats_spectrum_title),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.85f)
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.flow_stats_vibe_count, animatedTotalCount),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = animatedHaloColor
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.stats_tap_slice),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 10.sp
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Lista Verticale
            val scrollState = rememberScrollState()
            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        return super.onPostScroll(consumed, available, source)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                val listModifier = if (expanded) {
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            if (scrollState.value < scrollState.maxValue) {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0f to Color.Black,
                                        1f to Color.Transparent,
                                        startY = size.height * 0.75f,
                                        endY = size.height
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                        }
                        .nestedScroll(nestedScrollConnection)
                        .verticalScroll(scrollState)
                        .padding(vertical = 12.dp)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                }

                Column(
                    modifier = listModifier
                ) {
                    val displayList = if (expanded) vibes else vibes.take(7)

                    displayList.forEachIndexed { index, vibe ->
                        val percentage = if (totalCount > 0) ((vibe.count.toFloat() / totalCount) * 100).roundToInt() else 0
                        val color = vibe.colorHex?.let { Color(it) } ?: Color.White

                        val isItemSelected = (selectedVibeCode == vibe.vibe)
                        val isDimmed = selectedVibeCode != null && !isItemSelected

                        VibeLegendItem(
                            vibe = vibe,
                            percentage = percentage,
                            color = color,
                            isSelected = isItemSelected,
                            isDimmed = isDimmed,
                            onClick = {
                                selectedVibeCode = if (isItemSelected) null else vibe.vibe
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
                    if (scrollState.maxValue > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 10.dp, bottom = 10.dp, end = 4.dp)
                                .height(240.dp)
                                .width(3.dp)
                                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.2f)
                                    .offset(y = (192 * scrollFraction).dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                            )
                        }
                    }
                }
            }

            if (vibes.size > 5) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .bounceClick(onClick = { expanded = !expanded })
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
