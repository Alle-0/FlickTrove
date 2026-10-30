package com.cinetrack.ui.components.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.cinetrack.BuildConfig
import com.cinetrack.R
import com.cinetrack.ui.components.glass.glassmorphic
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.VibrationHelper
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Premium full-screen interactive image viewer dialog supporting:
 * - Continuous pinch-to-zoom (up to 5x)
 * - Free-form pan/drag when zoomed with bounding-box collision clamping
 * - Smooth double-tap to zoom in (2.5x) or reset to 1x
 * - Dismiss on single background tap or close button
 * - Clean zoom percentage indicator pill
 */
@Composable
fun ZoomableImageViewerDialog(
    visible: Boolean,
    imageUrl: String?,
    onDismiss: () -> Unit
) {
    if (!visible || imageUrl.isNullOrBlank()) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val hazeState = remember { HazeState() }

        var containerSize by remember { mutableStateOf(IntSize.Zero) }

        val scaleAnim = remember { Animatable(1f) }
        val offsetAnim = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .onSizeChanged { containerSize = it }
                .pointerInput(Unit) {
                    detectTransformGestures(panZoomLock = false) { _, pan, zoom, _ ->
                        val currentScale = scaleAnim.value
                        val targetScale = (currentScale * zoom).coerceIn(1f, 5f)

                        val currentOffset = offsetAnim.value
                        if (targetScale > 1f && containerSize.width > 0 && containerSize.height > 0) {
                            val maxOffsetX = (containerSize.width * (targetScale - 1f)) / 2f
                            val maxOffsetY = (containerSize.height * (targetScale - 1f)) / 2f

                            val newX = (currentOffset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                            val newY = (currentOffset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)

                            coroutineScope.launch {
                                scaleAnim.snapTo(targetScale)
                                offsetAnim.snapTo(Offset(newX, newY))
                            }
                        } else {
                            coroutineScope.launch {
                                scaleAnim.snapTo(targetScale)
                                offsetAnim.snapTo(Offset.Zero)
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tapPoint ->
                            VibrationHelper.vibrateTick(context)
                            coroutineScope.launch {
                                if (scaleAnim.value > 1.15f) {
                                    launch {
                                        scaleAnim.animateTo(
                                            1f,
                                            animationSpec = tween(280, easing = FastOutSlowInEasing)
                                        )
                                    }
                                    launch {
                                        offsetAnim.animateTo(
                                            Offset.Zero,
                                            animationSpec = tween(280, easing = FastOutSlowInEasing)
                                        )
                                    }
                                } else {
                                    val targetScale = 2.5f
                                    val center = Offset(
                                        containerSize.width / 2f,
                                        containerSize.height / 2f
                                    )
                                    val maxOffsetX = (containerSize.width * (targetScale - 1f)) / 2f
                                    val maxOffsetY = (containerSize.height * (targetScale - 1f)) / 2f

                                    // Zoom towards the tapped area
                                    val targetX = ((center.x - tapPoint.x) * (targetScale - 1f))
                                        .coerceIn(-maxOffsetX, maxOffsetX)
                                    val targetY = ((center.y - tapPoint.y) * (targetScale - 1f))
                                        .coerceIn(-maxOffsetY, maxOffsetY)

                                    launch {
                                        scaleAnim.animateTo(
                                            targetScale,
                                            animationSpec = tween(280, easing = FastOutSlowInEasing)
                                        )
                                    }
                                    launch {
                                        offsetAnim.animateTo(
                                            Offset(targetX, targetY),
                                            animationSpec = tween(280, easing = FastOutSlowInEasing)
                                        )
                                    }
                                }
                            }
                        },
                        onTap = {
                            if (scaleAnim.value <= 1.05f) {
                                onDismiss()
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Main image container (avvolto in Box a tutto schermo per cattura globale hazeState)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(state = hazeState, style = HazeStyles.PremiumDark)
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .apply {
                            if (imageUrl.contains("commsuni.tv")) {
                                addHeader("Authorization", "Bearer ${BuildConfig.COMMSUNI_API_KEY}")
                                addHeader("User-Agent", "FlickTrove-Android/${BuildConfig.VERSION_NAME}")
                            }
                        }
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 20.dp)
                        .graphicsLayer {
                            val s = scaleAnim.value
                            scaleX = s
                            scaleY = s
                            translationX = offsetAnim.value.x
                            translationY = offsetAnim.value.y
                        }
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    when (painter.state) {
                        is AsyncImagePainter.State.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        is AsyncImagePainter.State.Error -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = stringResource(R.string.detail_error_unknown),
                                    color = Color.White.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        else -> {
                            SubcomposeAsyncImageContent()
                        }
                    }
                }
            }

            // Top action bar with Close button and Zoom ratio
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Zoom ratio indicator badge (visible when zoomed)
                AnimatedVisibility(
                    visible = scaleAnim.value > 1.08f,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .hazeGlass(
                                state = hazeState,
                                shape = CircleShape,
                                blurRadius = 20.dp,
                                containerColor = Color.Black.copy(alpha = 0.28f),
                                borderWidth = 1.dp,
                                borderColor = Color.White.copy(alpha = 0.20f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .bounceClick(scaleDown = 0.90f) {
                                    coroutineScope.launch {
                                        launch { scaleAnim.animateTo(1f, tween(250)) }
                                        launch { offsetAnim.animateTo(Offset.Zero, tween(250)) }
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${(scaleAnim.value * 100).roundToInt()}%",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (scaleAnim.value <= 1.08f) {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Close button con blur autentico (il vetro hazeGlass resta fisso, rimbalza solo l'icona interna)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .hazeGlass(
                            state = hazeState,
                            shape = CircleShape,
                            blurRadius = 20.dp,
                            containerColor = Color.Black.copy(alpha = 0.28f),
                            borderWidth = 1.dp,
                            borderColor = Color.White.copy(alpha = 0.20f),
                            useRefraction = true
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .bounceClick(scaleDown = 0.86f) { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_x),
                            contentDescription = stringResource(R.string.settings_close),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
