package com.cinetrack.ui.components.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.cinetrack.R
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.DarkSurface
import com.cinetrack.ui.utils.bounceClick
import kotlin.math.roundToInt

/**
 * Balloon tooltip fluttuante per mostrare il dettaglio episodi su tocco in Stats.
 */
@Composable
fun StatsEpisodesTooltipOverlay(
    displayBounds: Rect?,
    displayText: String?,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (displayBounds == null || displayText == null) return

    val density = LocalDensity.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    Box(
        contentAlignment = AbsoluteAlignment.TopLeft,
        modifier = modifier
            .fillMaxSize()
            .zIndex(500f)
    ) {
        // Scrim di dismiss al tocco esterno
        if (isVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { onDismiss() }
                    }
            )
        }

        val sideMarginPx = with(density) { 16.dp.toPx() }
        val spacingPx = with(density) { 6.dp.toPx() }
        val screenWidthPx = with(density) { screenWidth.toPx() }
        val minTopPx = with(density) { 60.dp.toPx() }

        var tooltipWidthPx by remember { mutableFloatStateOf(0f) }
        var tooltipHeightPx by remember { mutableFloatStateOf(0f) }

        val targetX = (displayBounds.center.x - tooltipWidthPx / 2f)
            .coerceIn(sideMarginPx, (screenWidthPx - sideMarginPx - tooltipWidthPx).coerceAtLeast(sideMarginPx))
        val targetY = (displayBounds.top - tooltipHeightPx - spacingPx)
            .coerceAtLeast(minTopPx)

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(160)) + scaleIn(
                initialScale = 0.75f,
                transformOrigin = TransformOrigin(0.5f, 1f),
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy)
            ),
            exit = fadeOut(tween(120)) + scaleOut(
                targetScale = 0.80f,
                transformOrigin = TransformOrigin(0.5f, 1f),
                animationSpec = tween(120)
            ),
            modifier = Modifier
                .absoluteOffset { IntOffset(targetX.roundToInt(), targetY.roundToInt()) }
                .alpha(if (tooltipWidthPx > 0f) 1f else 0f)
        ) {
            Box(
                modifier = Modifier
                    .onSizeChanged { size ->
                        tooltipWidthPx = size.width.toFloat()
                        tooltipHeightPx = size.height.toFloat()
                    }
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        spotColor = Color.Black.copy(alpha = 0.6f)
                    )
                    .clip(CircleShape)
                    .background(Color(0xFF1A1D24))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                    .bounceClick(scaleDown = 0.95f) { onDismiss() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_tv),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = displayText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Overlay oscurante e card di caricamento con indicatore circolare durante
 * la generazione e il rendering della card grafica di riepilogo / Wrapped da condividere.
 */
@Composable
fun StatsSharingLoadingOverlay(
    isSharing: Boolean,
    modifier: Modifier = Modifier
) {
    val sharingAlpha by animateFloatAsState(
        targetValue = if (isSharing) 1f else 0f,
        animationSpec = tween(300),
        label = "sharingAlpha"
    )

    if (sharingAlpha <= 0f) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(100f),
        contentAlignment = Alignment.Center
    ) {
        // Scrim oscurante
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f * sharingAlpha))
                .pointerInput(Unit) {
                    detectTapGestures { /* consume clicks */ }
                }
        )

        Card(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth(0.85f)
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .hazeGlass(
                            state = null,
                            shape = RoundedCornerShape(24.dp),
                            containerColor = DarkSurface.copy(alpha = 0.45f),
                            alpha = sharingAlpha,
                            useOffscreenStrategy = true
                        )
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = sharingAlpha
                        }
                        .padding(horizontal = 28.dp, vertical = 32.dp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.stats_generating_card),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.stats_generating_card_desc),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
