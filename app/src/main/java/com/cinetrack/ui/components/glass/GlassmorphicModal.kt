package com.cinetrack.ui.components.glass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.cinetrack.ui.theme.HazeStyles
import dev.chrisbanes.haze.HazeState
import com.cinetrack.ui.components.glass.hazeGlass

@Composable
fun GlassmorphicModal(
    visible: Boolean,
    activeHazeState: HazeState,
    dimBackground: Boolean = false,
    dismissOnClickOutside: Boolean = true,
    onDismissRequest: (() -> Unit)? = null,
    content: @Composable BoxScope.(alpha: Float) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing)),
        exit = fadeOut(tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing)),
        modifier = Modifier.zIndex(100f)
    ) {
        val alpha by transition.animateFloat(
            transitionSpec = { tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing) },
            label = "blurAlpha"
        ) { if (it == EnterExitState.Visible) 1f else 0f }

        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (dimBackground) Modifier.background(Color.Black.copy(alpha = 0.5f * alpha))
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (onDismissRequest != null && dismissOnClickOutside) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onDismissRequest()
                        }
                )
            }

            Box(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(32.dp))
                    .hazeGlass(
                        state = activeHazeState,
                        alpha = alpha,
                        shape = RoundedCornerShape(32.dp),
                        style = HazeStyles.glassmorphicDialog
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
                    .pointerInput(Unit) { 
                        detectTapGestures {
                            focusManager.clearFocus()
                        } 
                    }
            ) {
                content(alpha)
            }
        }
    }
}
