package com.cinetrack.ui.screens

import androidx.compose.ui.res.stringResource
import com.cinetrack.R

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.data.Movie
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.SurpriseCompany
import com.cinetrack.ui.viewmodel.SurpriseMeViewModel
import com.cinetrack.ui.viewmodel.SurpriseMood
import com.cinetrack.ui.viewmodel.SurpriseTime
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SurpriseMeOverlay(
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

    BackHandler {
        if (step in 1..3) {
            step--
        } else {
            onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)) // Scrim
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Consume clicks inside modal
                )
                .hazeGlass(state = globalHazeState, shape = RoundedCornerShape(32.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step in 1..3) {
                    IconButton(
                        onClick = { step-- },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_left), contentDescription = stringResource(R.string.detail_content_desc_back), tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                } else {
                    Spacer(modifier = Modifier.size(36.dp))
                }

                Text(
                    text = stringResource(R.string.surprise_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(ImageVector.vectorResource(R.drawable.ic_x), contentDescription = stringResource(R.string.surprise_close), tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                        onRandomClick = { 
                            onMovieFound(viewModel.getRandomMovie()) 
                        },
                        onEmotionalClick = { step = 1 }
                    )
                    1 -> TimeStep(
                        onSelect = { 
                            selectedTime = it
                            step = 2
                        }
                    )
                    2 -> MoodStep(
                        onSelect = {
                            selectedMood = it
                            step = 3
                        }
                    )
                    3 -> CompanyStep(
                        onSelect = {
                            selectedCompany = it
                            // Calcola
                            scope.launch {
                                step = 4 // Loading
                                delay(800) // Fake loading per creare hype
                                val movie = viewModel.getEmotionalMovie(selectedTime!!, selectedMood!!, selectedCompany!!)
                                onMovieFound(movie)
                            }
                        }
                    )
                    4 -> LoadingStep()
                }
            }

            // Progress bar
            if (step in 1..3) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..3) {
                        val isCurrent = step == i
                        val isPast = step > i
                        
                        val width by animateDpAsState(
                            targetValue = if (isCurrent) 24.dp else 8.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                            label = "dotWidth"
                        )
                        
                        val color by animateColorAsState(
                            targetValue = if (isCurrent || isPast) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                            animationSpec = tween(300),
                            label = "dotColor"
                        )
                        
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(4.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
            }
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
private fun LoadingStep() {
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowOptions.forEach { option ->
                        OptionGridCard(
                            title = option.label,
                            icon = option.icon,
                            isFullWidth = rowOptions.size == 1,
                            onClick = option.onClick,
                            modifier = Modifier.weight(1f)
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
                    Modifier.height(if (description != null) 110.dp else 90.dp)
                } else {
                    Modifier.aspectRatio(if (description != null) 0.9f else 1.35f)
                }
            )
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .bounceClick(scaleDown = 0.92f, onClick = onClick)
            .padding(12.dp),
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
            lineHeight = 16.sp
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
