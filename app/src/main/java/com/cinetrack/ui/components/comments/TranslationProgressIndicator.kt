package com.cinetrack.ui.components.comments

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cinetrack.R
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.CommentsViewModel

/**
 * Animated translation button with progressive filling and shimmer bar
 * indicating ML Kit / Google Translate download & translating status.
 */
@Composable
fun TranslationProgressIndicator(
    translationState: CommentsViewModel.TranslationState?,
    accentColor: Color,
    onTranslate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDownloading = translationState is CommentsViewModel.TranslationState.Downloading
    val isTranslating = translationState is CommentsViewModel.TranslationState.Translating
    val isTranslated = translationState is CommentsViewModel.TranslationState.Translated
    val isProgressVisible = isDownloading || isTranslating

    val progress = remember { Animatable(0.08f) }

    val infiniteTransition = rememberInfiniteTransition(label = "downloadingShimmer")
    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerPhase"
    )
    val iconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.50f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconPulse"
    )

    LaunchedEffect(isDownloading) {
        if (isDownloading) {
            progress.snapTo(0.08f)
            // Multi-phase continuous filling curve scaled to realistic ML Kit model downloads:
            // Phase 1: Fast start response
            progress.animateTo(0.35f, tween(2000, easing = FastOutSlowInEasing))
            // Phase 2: Steady mid download
            progress.animateTo(0.65f, tween(4000, easing = LinearEasing))
            // Phase 3: High progress
            progress.animateTo(0.85f, tween(6000, easing = LinearOutSlowInEasing))
            // Phase 4: Long buffer crawl so it never freezes
            progress.animateTo(0.96f, tween(12000, easing = LinearOutSlowInEasing))
        }
    }

    LaunchedEffect(isTranslating) {
        if (isTranslating) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
            )
        }
    }

    // Animate width & spacing to smoothly slide the icon to the left when opening,
    // and slide it back to the right when closing (preserving rounded pill shape at every frame)
    val animatedBarWidth by animateDpAsState(
        targetValue = if (isProgressVisible) 36.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "translationBarWidth"
    )
    val animatedSpacing by animateDpAsState(
        targetValue = if (isProgressVisible) 6.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "translationSpacing"
    )
    val animatedBarAlpha by animateFloatAsState(
        targetValue = if (isProgressVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isProgressVisible) 220 else 180,
            easing = FastOutSlowInEasing
        ),
        label = "translationBarAlpha"
    )

    val targetIconTint = when {
        isDownloading -> accentColor.copy(alpha = iconAlpha)
        isTranslating -> accentColor
        isTranslated -> accentColor
        else -> Color.White.copy(alpha = 0.55f)
    }
    val animatedIconTint by animateColorAsState(
        targetValue = targetIconTint,
        animationSpec = tween(220),
        label = "translationIconTint"
    )

    val canClick = !isDownloading && !isTranslating

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_traduzione),
            contentDescription = stringResource(
                if (isTranslated) R.string.comment_show_original else R.string.comment_translate
            ),
            tint = animatedIconTint,
            modifier = Modifier
                .size(14.dp)
                .bounceClick(enabled = canClick) { onTranslate() }
        )

        if (animatedBarWidth > 0.5.dp && animatedBarAlpha > 0.01f) {
            Spacer(modifier = Modifier.width(animatedSpacing))
            Box(
                modifier = Modifier
                    .width(animatedBarWidth)
                    .height(4.dp)
                    .graphicsLayer { alpha = animatedBarAlpha }
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                val currentProg = progress.value.coerceIn(0.06f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = currentProg)
                        .clip(CircleShape)
                        .background(
                            if (isTranslating) {
                                Brush.linearGradient(listOf(accentColor, accentColor))
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.70f),
                                        Color.White.copy(alpha = 0.90f),
                                        accentColor
                                    ),
                                    startX = -40f + shimmerPhase * 100f,
                                    endX = shimmerPhase * 100f
                                )
                            }
                        )
                )
            }
        }
    }
}
