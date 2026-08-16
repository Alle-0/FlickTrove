package com.cinetrack.ui.components.updates

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.HazeStyles
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cinetrack.R
import com.cinetrack.data.model.SocialNotification
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun SocialNotificationCard(
    notification: SocialNotification,
    onClick: () -> Unit,
    onMarkRead: () -> Unit
) {
    val alpha = if (notification.isRead) 0.5f else 1f
    
    val dateText = notification.createdAt?.toDate()?.toInstant()
        ?.atZone(ZoneId.systemDefault())
        ?.toLocalDateTime()
        ?.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(Locale.getDefault()))
        ?: ""

    val actionText = if (notification.type == "like") {
        stringResource(R.string.updates_social_liked)
    } else {
        stringResource(R.string.updates_social_replied)
    }
    
    val iconRes = if (notification.type == "like") {
        R.drawable.ic_heart
    } else {
        R.drawable.ic_comment
    }
    
    val iconColor = if (notification.type == "like") {
        Color.Red
    } else {
        MaterialTheme.colorScheme.primary
    }

    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = if (isPressed) androidx.compose.animation.core.Spring.StiffnessHigh else androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "socialCardScale"
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
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF1C1C1E))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(26.dp))
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
            // Immagine media (con iconcina in basso a destra)
            Box(
                modifier = Modifier.width(44.dp).height(58.dp)
            ) {
                AsyncImage(
                    model = notification.mediaImage,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.DarkGray)
                )
                
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 6.dp, y = 6.dp)
                        .size(20.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = iconRes),
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                val onText = stringResource(R.string.updates_social_on)
                Text(
                    text = "${notification.senderName} $actionText $onText ${notification.mediaTitle}",
                    color = Color.White.copy(alpha = alpha),
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateText,
                    color = Color.White.copy(alpha = alpha * 0.6f),
                    fontSize = 11.sp
                )
            }
            
            if (!notification.isRead) {
                val actionInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                val isActionPressed by actionInteractionSource.collectIsPressedAsState()
                val actionScale by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (isActionPressed) 0.8f else 1f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = if (isActionPressed) androidx.compose.animation.core.Spring.StiffnessHigh else androidx.compose.animation.core.Spring.StiffnessLow
                    ),
                    label = "socialActionScale"
                )

                androidx.compose.material3.IconButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onMarkRead()
                    },
                    interactionSource = actionInteractionSource,
                    modifier = Modifier.graphicsLayer {
                        scaleX = actionScale
                        scaleY = actionScale
                    }
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_tick_card),
                        contentDescription = "Mark as read",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
