package com.cinetrack.ui.components.shared

import androidx.compose.ui.res.stringResource
import com.cinetrack.R
import com.cinetrack.ui.utils.ColorUtils
import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.ui.res.vectorResource

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.ui.utils.bounceClick
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import com.cinetrack.ui.theme.GlassSurface
import kotlin.math.roundToInt

@Composable
fun QuickRatingModal(
    initialRating: Double,
    movieTitle: String,
    accentColor: Color,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var currentRating by remember { mutableStateOf(initialRating) }
    val numberScale = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    FlickTroveModal(
        onDismissRequest = onDismiss,
        hazeState = hazeState,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_star),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Column(modifier = Modifier.padding(start = 14.dp)) {
                    Text(
                        text = movieTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.quick_rating_title),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.4f),
                                letterSpacing = 1.sp
                            )
                        )
                        if (initialRating > 0.0 && currentRating != initialRating) {
                            Text(
                                text = stringResource(R.string.quick_rating_previous, initialRating),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = accentColor.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Large Rating Display
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .graphicsLayer {
                        scaleX = numberScale.value
                        scaleY = numberScale.value
                    }
            ) {
                Text(
                    text = "%.1f".format(currentRating),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 54.sp,
                        letterSpacing = (-2).sp
                    ),
                    color = accentColor,
                    modifier = Modifier.widthIn(min = 100.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "/ 10",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                )
            }

            // Star Rating Bar
            FluidRatingBar(
                rating = currentRating,
                onRatingChange = { currentRating = it },
                onRatingSettled = { settledVal ->
                    if (settledVal > 0.0) {
                        coroutineScope.launch {
                            numberScale.snapTo(1.24f)
                            numberScale.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                    }
                },
                accentColor = accentColor,
                starSize = 48.dp,
                modifier = Modifier
                    .width(280.dp)
                    .height(80.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .bounceClick(scaleDown = 0.92f, onClick = onDismiss)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .bounceClick(scaleDown = 0.92f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSave(currentRating)
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.action_save),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Black
                        ),
                        color = ColorUtils.contentColorForAccent(accentColor)
                    )
                }
            }
        }
    }
}
