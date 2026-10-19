package com.cinetrack.ui.components.updates

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.repository.TrophyUpdateNotification
import com.cinetrack.ui.components.badge.FlickTroveBadgeEmblem
import com.cinetrack.ui.components.badge.getBadgeDescriptionRes
import com.cinetrack.ui.components.badge.getBadgeTitleRes
import com.cinetrack.ui.utils.bounceClick

@Composable
fun TrophyNotificationCard(
    notification: TrophyUpdateNotification,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = if (isPressed) Spring.StiffnessHigh else Spring.StiffnessLow
        ),
        label = "trophyCardScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF1C1C1E))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (notification) {
                is TrophyUpdateNotification.BootstrapSummary -> {
                    // Emblema coppa dorata circolare
                    Box(
                        modifier = Modifier.size(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFBBF24).copy(alpha = 0.16f))
                                .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.40f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_trophy),
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Colonna Info
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.trophy_unlock_bootstrap_title, notification.unlockedCount),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PillBadge(
                                text = stringResource(R.string.dashboard_trophies).uppercase(),
                                color = Color(0xFFFBBF24)
                            )
                            Text(
                                text = stringResource(R.string.trophy_unlock_bootstrap_subtitle),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Pulsante Spunta di conferma
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .bounceClick(
                                scaleDown = 0.82f,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onDismiss()
                                }
                            )
                            .clip(CircleShape)
                            .background(Color(0xFFFBBF24).copy(alpha = 0.14f))
                            .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.28f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_tick_card),
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                is TrophyUpdateNotification.SingleTrophy -> {
                    val tierColor = notification.tier.primaryColor
                    val titleRes = getBadgeTitleRes(notification.badgeId)
                    val displayTitle = if (titleRes != 0) stringResource(titleRes) else notification.title
                    val descRes = getBadgeDescriptionRes(notification.badgeId)
                    val displaySubtitle = if (descRes != 0) stringResource(descRes) else notification.subtitle

                    // Emblema circolare del badge (senza sfondo rettangolare)
                    Box(
                        modifier = Modifier.size(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FlickTroveBadgeEmblem(
                            tier = notification.tier,
                            iconKind = notification.iconKind,
                            isUnlocked = true,
                            size = 52.dp,
                            enablePeriodicGleam = false
                        )
                    }

                    // Colonna Info
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = displayTitle,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PillBadge(
                                text = notification.tier.localizedFormatName().uppercase(),
                                color = tierColor
                            )
                            if (displaySubtitle.isNotBlank()) {
                                Text(
                                    text = displaySubtitle,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Pulsante Spunta di conferma
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .bounceClick(
                                scaleDown = 0.82f,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onDismiss()
                                }
                            )
                            .clip(CircleShape)
                            .background(tierColor.copy(alpha = 0.14f))
                            .border(1.dp, tierColor.copy(alpha = 0.28f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_tick_card),
                            contentDescription = null,
                            tint = tierColor,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}
