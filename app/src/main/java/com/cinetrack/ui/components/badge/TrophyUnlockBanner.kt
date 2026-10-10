package com.cinetrack.ui.components.badge

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.repository.TrophyUnlockBannerEvent
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.TrophyRoomViewModel
import com.cinetrack.util.VibrationHelper
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay

/**
 * Banner galleggiante celebrativo per lo sblocco di trofei o avanzamenti di formato.
 * Ispirato alle notifiche di sistema PlayStation / Xbox / Steam, integrato con la fisica
 * fluida e l'estetica glassmorphic di FlickTrove.
 *
 * Supporta:
 * 1. [TrophyUnlockBannerEvent.SingleBadge]: Notifica singola di sblocco/upgrade con miniatura ed effetti.
 * 2. [TrophyUnlockBannerEvent.InitialBootstrapSummary]: Notifica aggregata al primo avvio per utenti storici
 *    ("X trofei sbloccati! Controlla la Sala dei Trofei"), prevenendo spam di decine di notifiche a cascata.
 */
@Composable
fun TrophyUnlockBanner(
    trophyRoomViewModel: TrophyRoomViewModel,
    hazeState: HazeState? = null,
    onNavigateToTrophyRoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val eventQueue = remember { mutableStateListOf<TrophyUnlockBannerEvent>() }
    var activeEvent by remember { mutableStateOf<TrophyUnlockBannerEvent?>(null) }
    var isVisible by remember { mutableStateOf(false) }

    // Raccoglie gli eventi emessi dal repository
    LaunchedEffect(trophyRoomViewModel) {
        trophyRoomViewModel.unlockedTierEvents.collect { event ->
            eventQueue.add(event)
        }
    }

    // Gestione della coda eventi (FIFO)
    LaunchedEffect(eventQueue.size, isVisible) {
        if (!isVisible && eventQueue.isNotEmpty()) {
            activeEvent = eventQueue.removeAt(0)
            isVisible = true
            try {
                VibrationHelper.vibrateTick(context)
            } catch (_: Exception) {}
        }
    }

    // Timer auto-dismiss a 4500ms
    LaunchedEffect(activeEvent, isVisible) {
        if (isVisible && activeEvent != null) {
            delay(4500L)
            isVisible = false
            delay(320L)
            activeEvent = null
        }
    }

    AnimatedVisibility(
        visible = isVisible && activeEvent != null,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy)
        ) + fadeIn(tween(240)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(260, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(180)),
        modifier = modifier
    ) {
        val current = activeEvent ?: return@AnimatedVisibility

        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 440.dp)
                .bounceClick {
                    isVisible = false
                    onNavigateToTrophyRoom()
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount < -15f) {
                            isVisible = false
                        }
                    }
                }
                .clip(RoundedCornerShape(32.dp))
                .hazeGlass(
                    state = hazeState,
                    shape = RoundedCornerShape(32.dp),
                    style = HazeStyles.PremiumDark
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color(0xFF0F172A).copy(alpha = 0.75f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            when (current) {
                is TrophyUnlockBannerEvent.InitialBootstrapSummary -> {
                    InitialBootstrapBannerContent(
                        unlockedCount = current.unlockedCount
                    )
                }
                is TrophyUnlockBannerEvent.SingleBadge -> {
                    SingleBadgeBannerContent(
                        event = current
                    )
                }
            }
        }
    }
}

@Composable
private fun InitialBootstrapBannerContent(
    unlockedCount: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Icona Trofeo Dorata con bagliore
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFBBF24).copy(alpha = 0.35f),
                            Color(0xFFB45309).copy(alpha = 0.15f)
                        )
                    )
                )
                .border(1.5.dp, Color(0xFFFBBF24).copy(alpha = 0.70f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_trophy),
                contentDescription = null,
                tint = Color(0xFFFBBF24),
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFFBBF24).copy(alpha = 0.18f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_trophies).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.1.sp
                        ),
                        color = Color(0xFFFBBF24)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = stringResource(R.string.trophy_unlock_bootstrap_title, unlockedCount),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = stringResource(R.string.trophy_unlock_bootstrap_subtitle),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp
                ),
                color = Color.White.copy(alpha = 0.70f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.45f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun SingleBadgeBannerContent(
    event: TrophyUnlockBannerEvent.SingleBadge
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Miniatura emblema trofeo con bagliore cromatico
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            FlickTroveBadgeEmblem(
                tier = event.tier,
                iconKind = event.iconKind,
                isUnlocked = true,
                size = 44.dp,
                enablePeriodicGleam = false
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val headerText = if (event.isFirstUnlock) {
                    stringResource(R.string.trophy_unlock_banner_header)
                } else {
                    stringResource(R.string.trophy_unlock_banner_format_upgraded)
                }

                Text(
                    text = headerText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.1.sp
                    ),
                    color = Color.White.copy(alpha = 0.55f)
                )

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(event.tier.primaryColor.copy(alpha = 0.16f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = event.tier.localizedFormatName().uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = event.tier.primaryColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = event.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = event.milestonePhrase,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp
                ),
                color = Color.White.copy(alpha = 0.70f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.45f),
            modifier = Modifier.size(16.dp)
        )
    }
}
