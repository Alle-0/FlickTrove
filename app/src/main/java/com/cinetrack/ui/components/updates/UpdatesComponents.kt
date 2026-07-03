package com.cinetrack.ui.components.updates

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cinetrack.R
import com.cinetrack.data.Movie
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.*
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.buildTmdbImageUrl

@Composable
fun RemindersSummaryCard(count: Int, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = if (isPressed) Spring.StiffnessHigh else Spring.StiffnessLow
        ),
        label = "summaryScale"
    )

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .hazeGlass(
                state = null, // Header uses its own state, cards use global or none for now to avoid complexity, but usually null is fine for subtle fallback or we can pass state
                shape = RoundedCornerShape(32.dp),
                blurRadius = HazeStyles.SmallGlassBlurRadius
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onClick()
            }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_bell_piena),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    text = stringResource(R.string.updates_your_reminders),
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.updates_waiting_count, count),
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp
                )
            }

            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun EmptyNotificationsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(60.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_bell),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.05f),
                modifier = Modifier.size(60.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.updates_no_notifications),
            color = Color.White.copy(alpha = 0.2f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SectionHeader(text: String, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp, start = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(),
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
        action?.invoke()
    }
}

@Composable
fun UpdateCard(
    movie: Movie, 
    label: String, 
    iconRes: Int, 
    color: Color, 
    onAction: () -> Unit, 
    onPress: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = if (isPressed) Spring.StiffnessHigh else Spring.StiffnessLow
        ),
        label = "cardScale"
    )

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .hazeGlass(
                state = null,
                shape = RoundedCornerShape(26.dp),
                blurRadius = HazeStyles.SmallGlassBlurRadius
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onPress()
            }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = buildTmdbImageUrl(movie.posterPath, ImageType.POSTER, LocalImageQuality.current),
                contentDescription = null,
                modifier = Modifier.width(44.dp).height(58.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(
                    text = movie.title ?: movie.name ?: "", 
                    color = Color.White, 
                    fontSize = 15.sp, 
                    fontWeight = FontWeight.Bold, 
                    maxLines = 1
                )
                Text(text = label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            val actionInteractionSource = remember { MutableInteractionSource() }
            val isActionPressed by actionInteractionSource.collectIsPressedAsState()
            val actionScale by animateFloatAsState(
                targetValue = if (isActionPressed) 0.8f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = if (isActionPressed) Spring.StiffnessHigh else Spring.StiffnessLow
                ),
                label = "actionScale"
            )

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onAction()
                },
                interactionSource = actionInteractionSource,
                modifier = Modifier.graphicsLayer {
                    scaleX = actionScale
                    scaleY = actionScale
                }
            ) {
                Icon(imageVector = ImageVector.vectorResource(id = iconRes), contentDescription = null, tint = color.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

fun formatReleaseDate(dateStr: String?): String {
    if (dateStr.isNullOrEmpty() || dateStr.length < 10) return dateStr ?: ""
    return try {
        val dateObj = java.time.LocalDate.parse(dateStr.take(10))
        val formatter = java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
        dateObj.format(formatter)
    } catch (e: Exception) {
        dateStr
    }
}
