package com.cinetrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.DarkSurface
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.components.shared.SymbiontPagerIndicator
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.SurpriseCompany
import com.cinetrack.ui.viewmodel.SurpriseMeViewModel
import com.cinetrack.ui.viewmodel.SurpriseMood
import com.cinetrack.ui.viewmodel.SurpriseTime
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SurpriseMeOverlay(
    isVisible: Boolean,
    triggerBounds: Rect? = null,
    viewModel: SurpriseMeViewModel,
    globalHazeState: HazeState,
    onMovieFound: (Movie?) -> Unit,
    onClose: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    
    // Selections
    var selectedTime by remember { mutableStateOf<SurpriseTime?>(null) }
    var selectedMood by remember { mutableStateOf<SurpriseMood?>(null) }
    var selectedCompany by remember { mutableStateOf<SurpriseCompany?>(null) }

    val scope = rememberCoroutineScope()

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    val targetWidth = (screenWidth * 0.90f).coerceAtMost(with(density) { 380.dp.toPx() })
    
    var contentHeightPx by remember { mutableStateOf(0f) }
    val maxModalHeightPx = with(density) { 520.dp.toPx() }
    val maxAllowedHeight = minOf(screenHeight * 0.78f, maxModalHeightPx)
    val targetHeightPx by animateFloatAsState(
        targetValue = if (contentHeightPx > 0) contentHeightPx.coerceAtMost(maxAllowedHeight) 
                      else with(density) { 240.dp.toPx() },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "dynamicHeight"
    )

    val targetRect = Rect(
        left = (screenWidth - targetWidth) / 2f,
        top = (screenHeight - targetHeightPx) / 2f,
        right = (screenWidth + targetWidth) / 2f,
        bottom = (screenHeight + targetHeightPx) / 2f
    )

    val transition = updateTransition(targetState = isVisible, label = "SurpriseMeTransition")

    BackHandler(enabled = isVisible) {
        if (step in 1..3) {
            step--
        } else {
            onClose()
        }
    }

    LaunchedEffect(transition.currentState, isVisible) {
        if (!isVisible && !transition.currentState) {
            step = 0
            selectedTime = null
            selectedMood = null
            selectedCompany = null
        }
    }

    val progress by transition.animateFloat(
        transitionSpec = {
            if (initialState == false && targetState == true) {
                spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy)
            } else {
                spring(stiffness = Spring.StiffnessMedium)
            }
        },
        label = "expansionProgress"
    ) { state -> if (state) 1f else 0f }

    val alpha by transition.animateFloat(label = "scrimAlpha") { state -> if (state) 1f else 0f }

    if (transition.currentState || transition.targetState) {
        val effectiveScrimAlpha = if (triggerBounds != null) {
            val scrimProgress = ((progress - 0.08f) / 0.92f).coerceIn(0f, 1f)
            0.6f * FastOutSlowInEasing.transform(scrimProgress)
        } else {
            0.6f * alpha
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(90000f)
                .background(Color.Black.copy(alpha = effectiveScrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                )
        ) {
            // --- GHOST MEASUREMENT LAYER ---
            Box(
                modifier = Modifier
                    .width(with(density) { targetWidth.toDp() })
                    .alpha(0f)
                    .onSizeChanged { size ->
                        if (size.height > 0) contentHeightPx = size.height.toFloat()
                    }
                    .align(Alignment.Center)
            ) {
                SurpriseMeContent(
                    step = step,
                    selectedTime = selectedTime,
                    selectedMood = selectedMood,
                    selectedCompany = selectedCompany,
                    isGhost = true,
                    onBack = {},
                    onClose = {},
                    onRandomClick = {},
                    onEmotionalClick = {},
                    onTimeSelect = {},
                    onMoodSelect = {},
                    onCompanySelect = {}
                )
            }

            val startRect = triggerBounds ?: targetRect.copy(
                left = targetRect.center.x - 20f,
                top = targetRect.center.y - 20f,
                right = targetRect.center.x + 20f,
                bottom = targetRect.center.y + 20f
            )

            // Center moves smoothly towards screen center
            val currentCenterX = lerp(startRect.center.x, targetRect.center.x, progress)
            val currentCenterY = lerp(startRect.center.y, targetRect.center.y, progress)

            // Delayed size expansion: stays compact/circular during liftoff (0..0.18), then blossoms into full size
            val sizeProgress = if (triggerBounds != null) {
                val delay = 0.18f
                if (progress <= delay) {
                    (progress / delay) * 0.05f
                } else {
                    val raw = (progress - delay) / (1f - delay)
                    0.05f + 0.95f * FastOutSlowInEasing.transform(raw.coerceIn(0f, 1f))
                }
            } else {
                progress
            }

            // Corner radius stays circular during liftoff, then morphs into target rounded corners
            val cornerRadiusProgress = if (triggerBounds != null) {
                val delay = 0.18f
                if (progress <= delay) 0f
                else FastOutSlowInEasing.transform(((progress - delay) / (1f - delay)).coerceIn(0f, 1f))
            } else {
                progress
            }

            val currentWidth = lerp(startRect.width, targetRect.width, sizeProgress)
            val currentHeight = lerp(startRect.height, targetRect.height, sizeProgress)

            val currentRect = Rect(
                left = currentCenterX - currentWidth / 2f,
                top = currentCenterY - currentHeight / 2f,
                right = currentCenterX + currentWidth / 2f,
                bottom = currentCenterY + currentHeight / 2f
            )

            val startRadius = if (triggerBounds != null) startRect.width / 2f else with(density) { 32.dp.toPx() }
            val endRadius = with(density) { 32.dp.toPx() }
            val currentCornerRadius = lerp(startRadius, endRadius, cornerRadiusProgress)
            val currentShape = RoundedCornerShape(with(density) { currentCornerRadius.toDp() })

            val currentTintAlpha = if (triggerBounds != null) {
                lerp(0.48f, HazeStyles.glassmorphicDialog.tint.alpha, sizeProgress)
            } else {
                HazeStyles.glassmorphicDialog.tint.alpha
            }
            val currentBlur = if (triggerBounds != null) {
                androidx.compose.ui.unit.lerp(16.dp, HazeStyles.glassmorphicDialog.blurRadius, sizeProgress)
            } else {
                HazeStyles.glassmorphicDialog.blurRadius
            }
            val animatedStyle = remember(currentTintAlpha, currentBlur) {
                HazeStyles.glassmorphicDialog.copy(
                    tint = HazeStyles.glassmorphicDialog.tint.copy(alpha = currentTintAlpha),
                    blurRadius = currentBlur
                )
            }
            val borderAlpha = if (triggerBounds != null) {
                lerp(HazeStyles.ModalBorderAlphaStart, HazeStyles.ModalBorderAlpha, sizeProgress)
            } else {
                HazeStyles.ModalBorderAlpha
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(currentRect.left.roundToInt(), currentRect.top.roundToInt()) }
                    .size(
                        width = with(density) { currentRect.width.toDp() },
                        height = with(density) { currentRect.height.toDp() }
                    )
                    .clip(currentShape)
                    .bounceClick(scaleDown = 1f) { /* Prevent dismissal */ }
            ) {
                // Background Layer (Blurred glass)
                Spacer(
                    modifier = Modifier
                        .matchParentSize()
                        .hazeGlass(
                            state = globalHazeState,
                            shape = currentShape,
                            style = animatedStyle,
                            useOffscreenStrategy = false
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = borderAlpha),
                            shape = currentShape
                        )
                )

                // Foreground Content
                if (progress > 0.38f) {
                    val contentAlpha = ((progress - 0.38f) / 0.62f).coerceIn(0f, 1f)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(1f)
                            .graphicsLayer(
                                alpha = contentAlpha,
                                compositingStrategy = CompositingStrategy.Offscreen
                            )
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SurpriseMeContent(
                            step = step,
                            selectedTime = selectedTime,
                            selectedMood = selectedMood,
                            selectedCompany = selectedCompany,
                            isGhost = false,
                            onBack = { if (step in 1..3) step-- else onClose() },
                            onClose = onClose,
                            onRandomClick = {
                                scope.launch {
                                    step = 4
                                    delay(800)
                                    val movie = viewModel.getRandomMovie()
                                    onMovieFound(movie)
                                }
                            },
                            onEmotionalClick = { step = 1 },
                            onTimeSelect = {
                                selectedTime = it
                                step = 2
                            },
                            onMoodSelect = {
                                selectedMood = it
                                step = 3
                            },
                            onCompanySelect = {
                                selectedCompany = it
                                scope.launch {
                                    step = 4
                                    delay(800)
                                    val movie = viewModel.getEmotionalMovie(selectedTime!!, selectedMood!!, selectedCompany!!)
                                    onMovieFound(movie)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun SurpriseMeContent(
    step: Int,
    selectedTime: SurpriseTime?,
    selectedMood: SurpriseMood?,
    selectedCompany: SurpriseCompany?,
    isGhost: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onRandomClick: () -> Unit,
    onEmotionalClick: () -> Unit,
    onTimeSelect: (SurpriseTime) -> Unit,
    onMoodSelect: (SurpriseMood) -> Unit,
    onCompanySelect: (SurpriseCompany) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step in 1..3) {
                com.cinetrack.ui.components.shared.ModalBackButton(
                    onClick = { if (!isGhost) onBack() }
                )
            } else {
                Spacer(modifier = Modifier.size(32.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.surprise_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )
            }

            com.cinetrack.ui.components.shared.ModalCloseButton(
                onClose = { if (!isGhost) onClose() }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isGhost) {
            when (step) {
                0 -> ChoiceStep(onRandomClick = {}, onEmotionalClick = {})
                1 -> TimeStep(onSelect = {})
                2 -> MoodStep(onSelect = {})
                3 -> CompanyStep(onSelect = {})
                4 -> LoadingStep(isGhost = true)
            }
        } else {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val duration = 400
                    if (targetState > initialState) {
                        (slideInHorizontally(animationSpec = tween(duration, easing = FastOutSlowInEasing)) { width -> width / 2 } + 
                         fadeIn(animationSpec = tween(duration)) + 
                         scaleIn(initialScale = 0.85f, animationSpec = tween(duration, easing = FastOutSlowInEasing))) togetherWith 
                        (slideOutHorizontally(animationSpec = tween(duration, easing = FastOutSlowInEasing)) { width -> -width / 2 } + 
                         fadeOut(animationSpec = tween(duration)) + 
                         scaleOut(targetScale = 0.85f, animationSpec = tween(duration, easing = FastOutSlowInEasing)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(duration, easing = FastOutSlowInEasing)) { width -> -width / 2 } + 
                         fadeIn(animationSpec = tween(duration)) + 
                         scaleIn(initialScale = 0.85f, animationSpec = tween(duration, easing = FastOutSlowInEasing))) togetherWith 
                        (slideOutHorizontally(animationSpec = tween(duration, easing = FastOutSlowInEasing)) { width -> width / 2 } + 
                         fadeOut(animationSpec = tween(duration)) + 
                         scaleOut(targetScale = 0.85f, animationSpec = tween(duration, easing = FastOutSlowInEasing)))
                    }.using(SizeTransform(clip = false))
                },
                label = "wizard"
            ) { currentStep ->
                when (currentStep) {
                    0 -> ChoiceStep(
                        onRandomClick = onRandomClick,
                        onEmotionalClick = onEmotionalClick
                    )
                    1 -> TimeStep(onSelect = onTimeSelect)
                    2 -> MoodStep(onSelect = onMoodSelect)
                    3 -> CompanyStep(onSelect = onCompanySelect)
                    4 -> LoadingStep(isGhost = false)
                }
            }
        }

        // Progress bar (Symbiont Elastic Indicator)
        if (step in 1..3) {
            Spacer(modifier = Modifier.height(20.dp))
            SymbiontPagerIndicator(
                currentPage = step - 1,
                pageCount = 3,
                persistPreviousDots = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ChoiceStep(onRandomClick: () -> Unit, onEmotionalClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OptionGridCard(
            title = stringResource(R.string.surprise_mode_random),
            description = stringResource(R.string.surprise_mode_random_desc),
            icon = ImageVector.vectorResource(R.drawable.ic_dice),
            onClick = onRandomClick,
            modifier = Modifier.weight(1f)
        )
        OptionGridCard(
            title = stringResource(R.string.surprise_mode_emotional),
            description = stringResource(R.string.surprise_mode_emotional_desc),
            icon = ImageVector.vectorResource(R.drawable.ic_sparkle),
            onClick = onEmotionalClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TimeStep(onSelect: (SurpriseTime) -> Unit) {
    WizardStep(
        title = stringResource(R.string.surprise_q_time),
        options = listOf(
            WizardOption(stringResource(R.string.surprise_time_short), ImageVector.vectorResource(R.drawable.ic_crono), { onSelect(SurpriseTime.SHORT) }),
            WizardOption(stringResource(R.string.surprise_time_medium), ImageVector.vectorResource(R.drawable.ic_clock), { onSelect(SurpriseTime.MEDIUM) }),
            WizardOption(stringResource(R.string.surprise_time_long), ImageVector.vectorResource(R.drawable.ic_clessidra), { onSelect(SurpriseTime.LONG) }),
            WizardOption(stringResource(R.string.surprise_any), ImageVector.vectorResource(R.drawable.ic_sparkle), { onSelect(SurpriseTime.ANY) })
        )
    )
}

@Composable
private fun MoodStep(onSelect: (SurpriseMood) -> Unit) {
    WizardStep(
        title = stringResource(R.string.surprise_q_mood),
        options = listOf(
            WizardOption(stringResource(R.string.surprise_mood_laugh), ImageVector.vectorResource(R.drawable.ic_laugh), { onSelect(SurpriseMood.LAUGH) }),
            WizardOption(stringResource(R.string.surprise_mood_tension), ImageVector.vectorResource(R.drawable.ic_bolt), { onSelect(SurpriseMood.TENSION) }),
            WizardOption(stringResource(R.string.surprise_mood_emotion), ImageVector.vectorResource(R.drawable.ic_goccia), { onSelect(SurpriseMood.EMOTION) }),
            WizardOption(stringResource(R.string.surprise_mood_escape), ImageVector.vectorResource(R.drawable.ic_rocket), { onSelect(SurpriseMood.ESCAPE) }),
            WizardOption(stringResource(R.string.surprise_mood_any), ImageVector.vectorResource(R.drawable.ic_sparkle), { onSelect(SurpriseMood.ANY) })
        )
    )
}

@Composable
private fun CompanyStep(onSelect: (SurpriseCompany) -> Unit) {
    WizardStep(
        title = stringResource(R.string.surprise_q_company),
        options = listOf(
            WizardOption(stringResource(R.string.surprise_company_alone), ImageVector.vectorResource(R.drawable.ic_persona), { onSelect(SurpriseCompany.ALONE) }),
            WizardOption(stringResource(R.string.surprise_company_couple), ImageVector.vectorResource(R.drawable.ic_heart), { onSelect(SurpriseCompany.COUPLE) }),
            WizardOption(stringResource(R.string.surprise_company_friends), ImageVector.vectorResource(R.drawable.ic_people), { onSelect(SurpriseCompany.FRIENDS) }),
            WizardOption(stringResource(R.string.surprise_company_family), ImageVector.vectorResource(R.drawable.ic_home), { onSelect(SurpriseCompany.FAMILY) })
        )
    )
}

@Composable
private fun LoadingStep(isGhost: Boolean = false) {
    if (isGhost) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                stringResource(R.string.surprise_loading_1), 
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "loading_infinite")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_sparkle),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            stringResource(R.string.surprise_loading_1), 
            color = Color.White.copy(alpha = 0.9f), 
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        )
    }
}

private data class WizardOption(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
private fun WizardStep(title: String, options: List<WizardOption>) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
            modifier = Modifier.padding(bottom = 24.dp)
        )
        val chunkedOptions = options.chunked(2)
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            chunkedOptions.forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowOptions.forEach { option ->
                        OptionGridCard(
                            title = option.label,
                            icon = option.icon,
                            isFullWidth = rowOptions.size == 1,
                            onClick = option.onClick,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionGridCard(
    title: String,
    description: String? = null,
    icon: ImageVector,
    isFullWidth: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .then(
                if (isFullWidth) {
                    Modifier.heightIn(min = if (description != null) 90.dp else 72.dp)
                } else {
                    Modifier.heightIn(min = if (description != null) 110.dp else 90.dp)
                }
            )
            .bounceClick(scaleDown = 0.92f, onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(if (description != null) 10.dp else 12.dp))
        Text(
            text = title, 
            color = Color.White, 
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (description != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description, 
                color = Color.White.copy(alpha = 0.6f), 
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}
