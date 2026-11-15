package com.cinetrack.ui.components.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.cinetrack.R
import com.cinetrack.data.api.CastMember
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.ImageType
import com.cinetrack.util.buildTmdbImageUrl
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

private data class VibeFloatItem(
    val initialX: Float,
    val targetX: Float,
    val targetY: Float,
    val sizeDp: Float,
    val rotationStart: Float,
    val rotationEnd: Float
)

private val VIBE_FLOAT_ITEMS = listOf(
    VibeFloatItem(initialX = -12f, targetX = -24f, targetY = -46f, sizeDp = 24f, rotationStart = -6f, rotationEnd = -20f),
    VibeFloatItem(initialX = 2f, targetX = 6f, targetY = -54f, sizeDp = 28f, rotationStart = 4f, rotationEnd = -10f),
    VibeFloatItem(initialX = 14f, targetX = 26f, targetY = -42f, sizeDp = 22f, rotationStart = 8f, rotationEnd = 22f)
)

/**
 * Chip representing an emotional vibe with icon, label and global community percentage.
 */
@Composable
fun VibeChip(
    vibe: EmotionalVibe,
    isSelected: Boolean,
    isDisabled: Boolean = false,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chipScale by animateFloatAsState(
        targetValue = if (isSelected) 1.03f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "VibeChipScale"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.25f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "VibeIconScale"
    )

    val iconRotation = remember { Animatable(0f) }

    val animatedBorderWidth by animateDpAsState(
        targetValue = if (isSelected) 1.5.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "VibeBorderWidth"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "VibeBorderColor"
    )

    val animatedBgColor by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f),
        animationSpec = tween(durationMillis = 200),
        label = "VibeBgColor"
    )

    val animatedTextColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else Color.White.copy(alpha = 0.7f),
        animationSpec = tween(durationMillis = 200),
        label = "VibeTextColor"
    )

    // Sparkle reaction animation on selection
    val burstProgress = remember { Animatable(0f) }
    var isInitialComposition by remember { mutableStateOf(true) }

    LaunchedEffect(isSelected) {
        if (isInitialComposition) {
            isInitialComposition = false
            return@LaunchedEffect
        }
        if (isSelected) {
            launch {
                iconRotation.snapTo(-14f)
                iconRotation.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            burstProgress.snapTo(0f)
            burstProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing)
            )
        } else {
            burstProgress.snapTo(0f)
        }
    }

    Box(
        modifier = modifier
            .zIndex(if (isSelected) 10f else 0f)
            .graphicsLayer {
                scaleX = chipScale
                scaleY = chipScale
                clip = false
            }
            .alpha(if (isDisabled) 0.3f else 1f)
            .bounceClick(scaleDown = if (isDisabled) 1f else 0.90f) {
                if (!isDisabled) onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // Inner capsule surface (clipped to 24dp rounded shape)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(24.dp))
                .background(animatedBgColor)
                .border(
                    width = animatedBorderWidth,
                    color = animatedBorderColor,
                    shape = RoundedCornerShape(24.dp)
                )
        )

        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
        ) {
            Icon(
                painter = painterResource(id = vibe.iconRes),
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                        rotationZ = iconRotation.value
                    },
                tint = if (isSelected) accentColor else Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = stringResource(vibe.labelRes),
                fontSize = 9.sp,
                lineHeight = 10.sp,
                color = animatedTextColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Always render the text to maintain consistent chip heights, 
            // but make it transparent if the percentage is 0
            Text(
                text = "${vibe.percentage}%",
                fontSize = 8.sp,
                lineHeight = 9.sp,
                color = if (vibe.percentage > 0) {
                    if (isSelected) accentColor.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.4f)
                } else {
                    Color.Transparent
                },
                fontWeight = FontWeight.Medium
            )
        }

        // Floating reaction emojis rising upward on selection (unclipped, rendered over everything)
        val p = burstProgress.value
        if (p > 0f && p < 1f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(20f)
                    .graphicsLayer { clip = false },
                contentAlignment = Alignment.Center
            ) {
                val travel = 1f - (1f - p).pow(2)
                val alpha = if (p < 0.40f) 1f else ((1f - p) / 0.60f).coerceIn(0f, 1f).pow(1.3f)

                VIBE_FLOAT_ITEMS.forEach { item ->
                    val curX = item.initialX + (item.targetX - item.initialX) * travel
                    val curY = item.targetY * travel
                    val scale = if (p < 0.35f) {
                        0.7f + (1.18f - 0.7f) * (p / 0.35f)
                    } else {
                        1.18f - (1.18f - 0.95f) * ((p - 0.35f) / 0.65f)
                    }
                    val rot = item.rotationStart + (item.rotationEnd - item.rotationStart) * p

                    Icon(
                        painter = painterResource(id = vibe.iconRes),
                        contentDescription = null,
                        tint = accentColor.copy(alpha = alpha),
                        modifier = Modifier
                            .size(item.sizeDp.dp)
                            .graphicsLayer {
                                translationX = curX.dp.toPx()
                                translationY = curY.dp.toPx()
                                scaleX = scale
                                scaleY = scale
                                rotationZ = rot
                                this.alpha = alpha
                                clip = false
                            }
                    )
                }
            }
        }
    }
}

private data class MvpSparkleParticle(
    val angleRad: Float,
    val distanceDp: Float,
    val sizeDp: Float,
    val color: Color,
    val isStar: Boolean
)

private val MVP_SPARKLE_PARTICLES = listOf(
    MvpSparkleParticle(angleRad = -0.55f, distanceDp = 22f, sizeDp = 3.2f, color = Color(0xFFFFEE75), isStar = true),
    MvpSparkleParticle(angleRad = -1.25f, distanceDp = 26f, sizeDp = 2.4f, color = Color(0xFFFFD54F), isStar = false),
    MvpSparkleParticle(angleRad = -1.95f, distanceDp = 28f, sizeDp = 3.4f, color = Color(0xFFFFC800), isStar = true),
    MvpSparkleParticle(angleRad = -2.65f, distanceDp = 23f, sizeDp = 2.2f, color = Color(0xFFFFE082), isStar = false),
    MvpSparkleParticle(angleRad = 3.14f, distanceDp = 25f, sizeDp = 3.0f, color = Color(0xFFFFE57F), isStar = true),
    MvpSparkleParticle(angleRad = 2.35f, distanceDp = 21f, sizeDp = 2.5f, color = Color(0xFFFFCA28), isStar = false),
    MvpSparkleParticle(angleRad = 1.25f, distanceDp = 20f, sizeDp = 2.8f, color = Color(0xFFFFD54F), isStar = true),
)

/**
 * Chip representing a cast member for the MVP (Most Valuable Performer) selection.
 */
@Composable
fun CastMvpChip(
    actor: CastMember,
    isMvp: Boolean,
    accentColor: Color,
    characterImageUrl: String? = null,
    mvpPercentage: Int? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mvpGold = Color(0xFFFFC800)

    val cardScale by animateFloatAsState(
        targetValue = if (isMvp) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "MvpCardScale"
    )

    val animatedBorderWidth by animateDpAsState(
        targetValue = if (isMvp) 2.dp else 1.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "MvpBorderWidth"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isMvp) mvpGold else Color.White.copy(alpha = 0.2f),
        animationSpec = tween(durationMillis = 250),
        label = "MvpBorderColor"
    )

    val badgeRotation by animateFloatAsState(
        targetValue = if (isMvp) 0f else -75f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "MvpBadgeRotation"
    )

    val animatedTextColor by animateColorAsState(
        targetValue = if (isMvp) mvpGold else Color.White.copy(alpha = 0.75f),
        animationSpec = tween(durationMillis = 250),
        label = "MvpTextColor"
    )

    // Celebration burst animation on selection
    val burstProgress = remember { Animatable(0f) }
    var isInitialComposition by remember { mutableStateOf(true) }

    LaunchedEffect(isMvp) {
        if (isInitialComposition) {
            isInitialComposition = false
            return@LaunchedEffect
        }
        if (isMvp) {
            burstProgress.snapTo(0f)
            burstProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing)
            )
        } else {
            burstProgress.snapTo(0f)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(64.dp)
            .bounceClick(scaleDown = 0.92f) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .zIndex(if (isMvp) 1f else 0f)
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(64.dp)
                    .height(96.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(
                        width = animatedBorderWidth,
                        color = animatedBorderColor,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                val initials = remember(actor.name) {
                    actor.name.split(" ")
                        .filter { it.isNotBlank() }
                        .mapNotNull { it.firstOrNull()?.toString() }
                        .take(2)
                        .joinToString("")
                        .uppercase()
                }
                if (initials.isNotEmpty()) {
                    Text(
                        text = initials,
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_persona),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (!characterImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = characterImageUrl,
                        contentDescription = actor.character,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                    )
                } else if (!actor.profilePath.isNullOrBlank()) {
                    AsyncImage(
                        model = buildTmdbImageUrl(actor.profilePath, ImageType.PROFILE, ImageQuality.LOW),
                        contentDescription = actor.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isMvp,
                enter = scaleIn(
                    initialScale = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(tween(150)),
                exit = scaleOut(
                    targetScale = 0f,
                    animationSpec = tween(150)
                ) + fadeOut(tween(150)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            rotationZ = badgeRotation
                        }
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFEA75),
                                    Color(0xFFFFC800),
                                    Color(0xFFE59400)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFFFFF9E6).copy(alpha = 0.8f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_star_piena),
                        contentDescription = null,
                        tint = Color(0xFF1A1A1A),
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            // Particle Burst & Shockwave Canvas
            val currentBurst = burstProgress.value
            if (currentBurst > 0f && currentBurst < 1f) {
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { clip = false }
                ) {
                    val p = currentBurst
                    val badgeCenterX = size.width - 4.dp.toPx() - 10.dp.toPx()
                    val badgeCenterY = size.height - 4.dp.toPx() - 10.dp.toPx()

                    // Expanding Shockwave Ring
                    if (p in 0.05f..0.85f) {
                        val ringProgress = ((p - 0.05f) / 0.8f).coerceIn(0f, 1f)
                        val ringRadius = (8.dp.toPx() + 18.dp.toPx() * ringProgress)
                        val ringAlpha = (1f - ringProgress).pow(1.5f) * 0.75f
                        val strokeWidth = (2.dp * (1f - ringProgress)).toPx()
                        drawCircle(
                            color = mvpGold.copy(alpha = ringAlpha),
                            radius = ringRadius,
                            center = Offset(badgeCenterX, badgeCenterY),
                            style = Stroke(width = strokeWidth)
                        )
                    }

                    // Burst Particles
                    val burstDecel = 1f - (1f - p).pow(2)
                    val alpha = (1f - p).coerceIn(0f, 1f).pow(1.5f)

                    MVP_SPARKLE_PARTICLES.forEach { part ->
                        val currentDist = part.distanceDp.dp.toPx() * burstDecel
                        val pX = badgeCenterX + cos(part.angleRad) * currentDist
                        val pY = badgeCenterY + sin(part.angleRad) * currentDist

                        if (part.isStar) {
                            val pSize = part.sizeDp.dp.toPx() * (1f - p * 0.4f)
                            drawLine(
                                color = part.color.copy(alpha = alpha),
                                start = Offset(pX - pSize, pY),
                                end = Offset(pX + pSize, pY),
                                strokeWidth = 1.3f.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = part.color.copy(alpha = alpha),
                                start = Offset(pX, pY - pSize),
                                end = Offset(pX, pY + pSize),
                                strokeWidth = 1.3f.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = alpha),
                                radius = pSize * 0.35f,
                                center = Offset(pX, pY)
                            )
                        } else {
                            drawCircle(
                                color = part.color.copy(alpha = alpha),
                                radius = (part.sizeDp * 0.6f).dp.toPx() * (1f - p * 0.5f),
                                center = Offset(pX, pY)
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = actor.character ?: actor.name,
            fontSize = 9.sp,
            color = animatedTextColor,
            fontWeight = if (isMvp) FontWeight.Bold else FontWeight.Normal,
            maxLines = 2,
            lineHeight = 10.sp,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        if (mvpPercentage != null && mvpPercentage > 0) {
            Text(
                text = "$mvpPercentage%",
                color = if (isMvp) mvpGold else accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
            )
        }
        Text(
            text = actor.name,
            fontSize = 8.sp,
            color = Color.Gray,
            maxLines = 2,
            lineHeight = 9.sp,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
