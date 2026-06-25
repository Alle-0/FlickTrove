package com.cinetrack.ui.components

import com.cinetrack.R

import androidx.compose.ui.res.vectorResource
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.ui.theme.NeonTeal
import com.cinetrack.ui.components.glass.hazeGlass
import dev.chrisbanes.haze.HazeState
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector

@Composable
fun GlassyBottomBar(
    hazeState: HazeState,
    selectedRoute: String?,
    onNavigate: (String) -> Unit,
    isDimmed: Boolean = false
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val animatedDimAlpha by animateFloatAsState(
        targetValue = if (isDimmed) 0.6f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "BottomBarDimAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 20.dp, top = 4.dp)
            .height(60.dp),
        contentAlignment = Alignment.Center
    ) {
        // BACKGROUND BLUR LAYER
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .hazeGlass(
                    state = hazeState, 
                    shape = CircleShape
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                icon = ImageVector.vectorResource(id = R.drawable.ic_segnalibro),
                animatedIconRes = R.drawable.ic_segnalibro_anim,
                label = stringResource(R.string.bottom_bar_to_watch),
                isSelected = selectedRoute == "index",
                enabled = !isDimmed,
                onClick = { onNavigate("index") },
                accentColor = accentColor
            )
            NavItem(
                icon = ImageVector.vectorResource(id = R.drawable.ic_tick_pieno),
                animatedIconRes = R.drawable.ic_tick_pieno_anim,
                label = stringResource(R.string.bottom_bar_watched),
                isSelected = selectedRoute == "visti",
                enabled = !isDimmed,
                onClick = { onNavigate("visti") },
                accentColor = accentColor
            )
            NavItem(
                icon = ImageVector.vectorResource(id = R.drawable.ic_stat),
                animatedIconRes = R.drawable.ic_stat_anim,
                label = stringResource(R.string.bottom_bar_stats),
                isSelected = selectedRoute == "stats",
                enabled = !isDimmed,
                onClick = { onNavigate("stats") },
                accentColor = accentColor
            )
        }

        // DIM OVERLAY
        if (animatedDimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = animatedDimAlpha), CircleShape)
            )
        }
    }
}

@Composable
private fun RowScope.NavItem(
    icon: ImageVector,
    animatedIconRes: Int? = null,
    label: String,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    accentColor: Color
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Bounce animation when becoming selected
    val iconScale = remember { androidx.compose.animation.core.Animatable(1f) }
    androidx.compose.runtime.LaunchedEffect(isSelected) {
        if (isSelected) {
            iconScale.animateTo(0.85f, tween(100, easing = androidx.compose.animation.core.LinearOutSlowInEasing))
            iconScale.animateTo(1.1f, androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium))
            iconScale.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow))
        }
    }
    
    // Scale animation ONLY if not selected and enabled
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && !isSelected && enabled) 0.92f else 1f, 
        label = "navScale"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isSelected // Disable click if dimmed OR already on this screen
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val baseColor = if (isSelected) accentColor else Color.White.copy(alpha = 0.6f)
        val tintColor by animateColorAsState(
            targetValue = if (isPressed) baseColor.copy(alpha = 0.9f) else baseColor,
            label = "iconTint"
        )

        Box(contentAlignment = Alignment.Center) {
            // Sfondo fisso: quando è selezionato diventa un "binario" semitrasparente
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected && animatedIconRes != null) Color.White.copy(alpha = 0.25f) else tintColor,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = iconScale.value
                        scaleY = iconScale.value
                    }
            )
            
            if (isSelected && animatedIconRes != null) {
                val animatedIcon = AnimatedImageVector.animatedVectorResource(id = animatedIconRes)
                var atEnd by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    atEnd = true
                }
                val painter = rememberAnimatedVectorPainter(animatedIcon, atEnd = atEnd)
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer {
                            scaleX = iconScale.value
                            scaleY = iconScale.value
                        }
                )
            }
        }
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            color = tintColor,
            modifier = Modifier.padding(top = 4.dp),
            letterSpacing = 0.5.sp
        )

    }
}
