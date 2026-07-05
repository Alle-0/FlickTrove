package com.cinetrack.ui.components.shared

import androidx.compose.ui.res.stringResource
import com.cinetrack.R
import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.ui.res.vectorResource

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
    val haptic = LocalHapticFeedback.current

    FlickTroveModal(
        onDismissRequest = onDismiss,
        hazeState = hazeState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp)),
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
                modifier = Modifier.padding(bottom = 16.dp)
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
                accentColor = accentColor,
                starSize = 48.dp,
                modifier = Modifier
                    .width(280.dp)
                    .height(80.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .bounceClick(scaleDown = 0.92f, onClick = onDismiss)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
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
                        .height(56.dp)
                        .bounceClick(scaleDown = 0.92f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSave(currentRating)
                        }
                        .clip(RoundedCornerShape(24.dp))
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.action_save),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Black
                        ),
                        color = Color.Black
                    )
                }
            }
        }
    }
}
