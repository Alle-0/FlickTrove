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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cinetrack.R
import com.cinetrack.data.model.GroupedSocialNotification
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun SocialNotificationCard(
    notification: GroupedSocialNotification,
    onClick: () -> Unit,
    onMarkRead: () -> Unit
) {
    val context = LocalContext.current
    val alpha = if (notification.isRead) 0.55f else 1f
    
    val dateText = notification.latestTimestamp?.toDate()?.toInstant()
        ?.atZone(ZoneId.systemDefault())
        ?.toLocalDateTime()
        ?.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(Locale.getDefault()))
        ?: ""

    val iconRes = if (notification.type == "like") {
        R.drawable.ic_heart
    } else {
        R.drawable.ic_comment
    }
    
    val iconColor = if (notification.type == "like") {
        Color(0xFFFF375F)
    } else {
        MaterialTheme.colorScheme.primary
    }

    val notificationText = remember(notification, context) {
        buildAnnotatedString {
            val senders = notification.senders
            val primarySender = senders.firstOrNull() ?: "Qualcuno"
            val totalCount = notification.count
            val distinctSendersCount = senders.size

            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = alpha))) {
                append(primarySender)
            }

            if (distinctSendersCount > 1) {
                val othersCount = distinctSendersCount - 1
                append(" e ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = alpha))) {
                    if (othersCount == 1 && totalCount == 2 && senders.size > 1) {
                        append(senders[1])
                    } else if (othersCount == 1) {
                        append(context.getString(R.string.updates_social_and_other))
                    } else {
                        append(context.getString(R.string.updates_social_and_others, othersCount))
                    }
                }
                append(" ")
                if (notification.type == "like") {
                    append(context.getString(R.string.updates_social_liked_plural))
                } else {
                    append(context.getString(R.string.updates_social_replied_plural))
                }
            } else if (totalCount > 1) {
                // Same sender, multiple actions on the same movie
                append(" ")
                if (notification.type == "like") {
                    append(context.getString(R.string.updates_social_liked_multiple, totalCount))
                } else {
                    append(context.getString(R.string.updates_social_replied_multiple, totalCount))
                }
            } else {
                // Single interaction
                append(" ")
                if (notification.type == "like") {
                    append(context.getString(R.string.updates_social_liked))
                } else {
                    append(context.getString(R.string.updates_social_replied))
                }
            }

            append(" ")
            append(context.getString(R.string.updates_social_on))
            append(" ")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = alpha))) {
                append(notification.mediaTitle)
            }
        }
    }

    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
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
            .wrapContentHeight()
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
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Immagine media con icona di azione ed eventuale contatore cumulativo
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

                // Action badge (cuore o fumetto)
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

                // Cumulative counter badge (+N) if multiple
                if (notification.count > 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-4).dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+${notification.count}",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = notificationText,
                    color = Color.White.copy(alpha = alpha * 0.85f),
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                // Comment Snippet if present
                if (!notification.snippet.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "«${notification.snippet}»",
                        color = Color.White.copy(alpha = alpha * 0.65f),
                        fontSize = 11.5.sp,
                        fontStyle = FontStyle.Italic,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = dateText,
                    color = Color.White.copy(alpha = alpha * 0.45f),
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
