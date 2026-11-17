package com.cinetrack.ui.components.search

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.components.common.CategoryTabSelector
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState

@Composable
fun SearchCategorySelector(
    category: String,
    onCategoryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    isMoodAiMode: Boolean = true,
    moodRemainingQuota: Int = 3,
    onMoodModeChanged: (Boolean) -> Unit = {},
    hazeState: HazeState? = null
) {
    val isMood = category == "mood"
    val selectedIndex = when (category) {
        "movie" -> 0
        "tv" -> 1
        "person" -> 2
        "collection" -> 3
        else -> 0
    }

    var previousCategory by remember { mutableStateOf("movie") }
    LaunchedEffect(category) {
        if (category != "mood") {
            previousCategory = category
        }
    }

    val options = listOf(
        stringResource(R.string.folder_detail_tab_movies),
        stringResource(R.string.folder_detail_tab_tv),
        stringResource(R.string.search_tab_persons),
        stringResource(R.string.search_tab_collections)
    )

    val haptic = LocalHapticFeedback.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val shockwaveProgress = remember { Animatable(0f) }

    LaunchedEffect(isMood) {
        if (isMood) {
            shockwaveProgress.snapTo(0f)
            shockwaveProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
            )
        }
    }

    val buttonBgColor by animateColorAsState(
        targetValue = if (isMood) {
            primaryColor.copy(alpha = 0.22f)
        } else {
            Color.White.copy(alpha = 0.05f)
        },
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "mood_btn_bg"
    )

    val buttonBorderColor by animateColorAsState(
        targetValue = if (isMood) {
            primaryColor
        } else {
            Color.White.copy(alpha = 0.14f)
        },
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "mood_btn_border"
    )

    val buttonBorderWidth by animateDpAsState(
        targetValue = if (isMood) 1.5.dp else 1.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "mood_btn_border_width"
    )

    val iconTint by animateColorAsState(
        targetValue = if (isMood) {
            primaryColor
        } else {
            Color.White.copy(alpha = 0.50f)
        },
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "mood_icon_tint"
    )

    val iconRotation by animateFloatAsState(
        targetValue = if (isMood) 360f else 0f,
        animationSpec = spring(
            dampingRatio = 0.54f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "mood_sparkle_rotation"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isMood) 1.25f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.46f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "mood_sparkle_scale"
    )

    val capsuleBorderColor by animateColorAsState(
        targetValue = if (isMood) {
            primaryColor.copy(alpha = 0.32f)
        } else {
            Color.White.copy(alpha = 0.08f)
        },
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "capsule_border_color"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Capsula Morphing: Si trasforma animata da [Movies, TV, People, Collections] a [AI • Quota, IRIS • ∞]
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp),
                contentAlignment = Alignment.Center
            ) {
                Spacer(
                    modifier = Modifier
                        .matchParentSize()
                        .then(
                            if (hazeState != null) {
                                Modifier.hazeGlass(
                                    state = hazeState,
                                    shape = CircleShape
                                )
                            } else {
                                Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape)
                            }
                        )
                        .border(1.dp, capsuleBorderColor, CircleShape)
                )

                AnimatedContent(
                    targetState = isMood,
                    transitionSpec = {
                        if (targetState) {
                            // Entrata in Mood: le categorie standard scivolano a sinistra, le tab Mood arrivano da destra con rimbalzo elastico
                            (slideInHorizontally(
                                animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
                                initialOffsetX = { fullWidth -> (fullWidth * 0.40f).toInt() }
                            ) + fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                                    scaleIn(initialScale = 0.90f, animationSpec = tween(240, easing = FastOutSlowInEasing)))
                                .togetherWith(
                                    slideOutHorizontally(
                                        animationSpec = tween(180, easing = FastOutSlowInEasing),
                                        targetOffsetX = { fullWidth -> (-fullWidth * 0.35f).toInt() }
                                    ) + fadeOut(animationSpec = tween(160, easing = FastOutSlowInEasing)) +
                                            scaleOut(targetScale = 0.92f, animationSpec = tween(160))
                                )
                        } else {
                            // Uscita da Mood: le tab Mood scivolano a destra, le categorie standard tornano da sinistra
                            (slideInHorizontally(
                                animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
                                initialOffsetX = { fullWidth -> (-fullWidth * 0.40f).toInt() }
                            ) + fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                                    scaleIn(initialScale = 0.90f, animationSpec = tween(240, easing = FastOutSlowInEasing)))
                                .togetherWith(
                                    slideOutHorizontally(
                                        animationSpec = tween(180, easing = FastOutSlowInEasing),
                                        targetOffsetX = { fullWidth -> (fullWidth * 0.35f).toInt() }
                                    ) + fadeOut(animationSpec = tween(160, easing = FastOutSlowInEasing)) +
                                            scaleOut(targetScale = 0.92f, animationSpec = tween(160))
                                )
                        }
                    },
                    label = "category_morphing_capsule"
                ) { moodActive ->
                    if (moodActive) {
                        val effectiveAiMode = isMoodAiMode && moodRemainingQuota > 0
                        CategoryTabSelector(
                            options = listOf(
                                stringResource(R.string.search_mood_mode_ai_tab, moodRemainingQuota),
                                stringResource(R.string.search_mood_mode_local_tab)
                            ),
                            selectedIndex = if (effectiveAiMode) 0 else 1,
                            onOptionClick = { index ->
                                onMoodModeChanged(index == 0)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            fillWidth = true,
                            tabWidth = 80.dp,
                            fontSize = 10.5.sp,
                            uppercase = true,
                            enableDrag = true
                        )
                    } else {
                        CategoryTabSelector(
                            options = options,
                            selectedIndex = selectedIndex,
                            onOptionClick = { index ->
                                val categoryStr = when (index) {
                                    0 -> "movie"
                                    1 -> "tv"
                                    2 -> "person"
                                    else -> "collection"
                                }
                                onCategoryChanged(categoryStr)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            fillWidth = true,
                            tabWidth = 80.dp,
                            fontSize = 10.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 2. Icona Mood a Lato: Switch toggle cinematografico con shockwave e rotazione elastica
            Box(
                modifier = Modifier
                    .drawWithContent {
                        drawContent()
                        val progress = shockwaveProgress.value
                        if (progress in 0.001f..0.999f) {
                            val baseRadius = size.minDimension / 2f
                            val radius = baseRadius + (progress * 18.dp.toPx())
                            val alpha = (1f - progress).coerceIn(0f, 1f) * 0.70f
                            drawCircle(
                                color = primaryColor.copy(alpha = alpha),
                                radius = radius,
                                center = center,
                                style = Stroke(width = (2.dp * (1f - progress * 0.4f)).toPx())
                            )
                        }
                    }
                    .bounceClick(scaleDown = 0.90f) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (isMood) {
                            onCategoryChanged(previousCategory)
                        } else {
                            previousCategory = category
                            onCategoryChanged("mood")
                        }
                    }
                    .size(34.dp)
                    .then(
                        if (hazeState != null) {
                            Modifier.hazeGlass(state = hazeState, shape = CircleShape)
                        } else {
                            Modifier
                        }
                    )
                    .background(buttonBgColor, CircleShape)
                    .border(buttonBorderWidth, buttonBorderColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_sparkle),
                    contentDescription = stringResource(R.string.search_tab_mood),
                    tint = iconTint,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer {
                            rotationZ = iconRotation
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                )
            }
        }
    }
}
