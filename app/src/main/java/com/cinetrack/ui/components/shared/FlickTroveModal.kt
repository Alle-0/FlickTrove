package com.cinetrack.ui.components.shared

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import com.cinetrack.ui.components.glass.hazeGlass
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.HazeState
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import com.cinetrack.ui.utils.bounceClick

/**
 * A premium centered modal with real-time blur (hazeChild).
 * Replaces the legacy Dialog implementation to support glassmorphism without artifacts.
 */
@Composable
fun FlickTroveModal(
    isVisible: Boolean = true,
    onDismissRequest: () -> Unit,
    hazeState: HazeState? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardShape = RoundedCornerShape(32.dp)

    var isClosing by remember { mutableStateOf(false) }

    // Handle back button for dismissal
    BackHandler(enabled = isVisible && !isClosing, onBack = { isClosing = true })

    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animateIn = true }

    val targetAlpha = if (isVisible && animateIn && !isClosing) 1f else 0f

    val modalAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 300),
        finishedListener = {
            if (targetAlpha == 0f && isClosing) {
                onDismissRequest()
            }
        },
        label = "alpha"
    )

    // Sync isClosing with isVisible externally
    LaunchedEffect(isVisible) {
        isClosing = !isVisible
    }

    if (modalAlpha > 0f) {
        Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(100f)
                    .background(Color.Black.copy(alpha = 0.7f * modalAlpha))
                    .bounceClick(scaleDown = 1f) { isClosing = true }
                    .graphicsLayer { alpha = modalAlpha },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .widthIn(max = 400.dp)
                        .fillMaxWidth(0.88f)
                        .padding(horizontal = 16.dp)
                        .bounceClick(scaleDown = 1f) { /* Consume clicks */ },
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Sibling 1: The blurred glass background card (fills parent exactly)
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .hazeGlass(state = hazeState, shape = cardShape)
                        )

                        // Sibling 2: The crisp content card (completely untouched by blur!)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            content()
                        }
                    }
                }
            }
        }
    }
