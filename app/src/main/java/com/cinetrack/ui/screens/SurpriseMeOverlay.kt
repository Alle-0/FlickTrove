package com.cinetrack.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.common.CategoryTabSelector
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.mascot.AiSparkleOrb
import com.cinetrack.ui.components.mascot.IrisMascot
import com.cinetrack.ui.components.mascot.IrisMood
import com.cinetrack.ui.theme.DarkSurface
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.components.shared.SymbiontPagerIndicator
import com.cinetrack.ui.utils.rtlSlideDirection
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.UiText
import com.cinetrack.ui.viewmodel.SurpriseCompany
import com.cinetrack.ui.viewmodel.SurpriseMeViewModel
import com.cinetrack.ui.viewmodel.SurpriseMood
import com.cinetrack.ui.viewmodel.SurpriseTime
import com.cinetrack.ui.components.surprise.SurpriseChoiceStep
import com.cinetrack.ui.components.surprise.SurpriseCompanyStep
import com.cinetrack.ui.components.surprise.SurpriseLoadingStep
import com.cinetrack.ui.components.surprise.SurpriseMoodStep
import com.cinetrack.ui.components.surprise.SurprisePromptStep
import com.cinetrack.ui.components.surprise.SurpriseTimeStep
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
    onClose: () -> Unit,
    onGuestAuthTrigger: () -> Unit = {}
) {
    var step by remember { mutableIntStateOf(0) }
    val isGuest = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.isAnonymous != false }
    
    // Selections
    var selectedTime by remember { mutableStateOf<SurpriseTime?>(null) }
    var selectedMood by remember { mutableStateOf<SurpriseMood?>(null) }
    var selectedCompany by remember { mutableStateOf<SurpriseCompany?>(null) }

    val remainingQuota by viewModel.remainingQuota.collectAsState()
    var isAiMode by remember(remainingQuota) { mutableStateOf(remainingQuota > 0) }
    var isSearchingWithAi by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val handleBack: () -> Unit = {
        if (step in 1..3) {
            step--
        } else if (step == 4) {
            step = 0
        } else {
            onClose()
        }
    }

    BackHandler(enabled = isVisible) {
        handleBack()
    }

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            step = 0
            selectedTime = null
            selectedMood = null
            selectedCompany = null
        }
    }

    com.cinetrack.ui.components.shared.MorphGlassModal(
        isVisible = isVisible,
        onDismissRequest = onClose,
        triggerBounds = triggerBounds,
        hazeState = globalHazeState,
        targetMaxWidth = 380.dp,
        targetWidthFraction = 0.90f,
        maxModalHeightFraction = 0.85f,
        minModalHeight = 0.dp,
        defaultModalHeight = 280.dp,
        zIndex = 90000f
    ) { contentAlpha ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            SurpriseMeContent(
                step = step,
                selectedTime = selectedTime,
                selectedMood = selectedMood,
                selectedCompany = selectedCompany,
                remainingQuota = remainingQuota,
                isAiMode = isAiMode,
                isSearchingWithAi = isSearchingWithAi,
                onAiModeChanged = { isAiMode = it },
                onEmitMessage = { viewModel.emitMessage(it) },
                isGhost = contentAlpha == 0f,
                onBack = handleBack,
                onClose = onClose,
                onRandomClick = {
                    isSearchingWithAi = false
                    scope.launch {
                        step = 5
                        delay(800)
                        val movie = viewModel.getRandomMovie()
                        onMovieFound(movie)
                    }
                },
                onPromptClick = {
                    if (isGuest) {
                        onGuestAuthTrigger()
                    } else {
                        step = 4
                    }
                },
                onPromptSubmit = { text, aiMode ->
                    if (isGuest) {
                        onGuestAuthTrigger()
                    } else {
                        isSearchingWithAi = aiMode
                        scope.launch {
                            step = 5
                            delay(800)
                            val movie = viewModel.getPromptMovie(text, forceLocal = !aiMode)
                            onMovieFound(movie)
                        }
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
                    isSearchingWithAi = false
                    scope.launch {
                        step = 5
                        delay(800)
                        val movie = viewModel.getEmotionalMovie(selectedTime!!, selectedMood!!, selectedCompany!!)
                        onMovieFound(movie)
                    }
                }
            )
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
    remainingQuota: Int,
    isAiMode: Boolean,
    isSearchingWithAi: Boolean = false,
    onAiModeChanged: (Boolean) -> Unit,
    onEmitMessage: (UiText) -> Unit = {},
    isGhost: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onRandomClick: () -> Unit,
    onPromptClick: () -> Unit,
    onPromptSubmit: (String, Boolean) -> Unit,
    onEmotionalClick: () -> Unit,
    onTimeSelect: (SurpriseTime) -> Unit,
    onMoodSelect: (SurpriseMood) -> Unit,
    onCompanySelect: (SurpriseCompany) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step in 1..3 || step == 4) {
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

        Spacer(modifier = Modifier.height(16.dp))

        if (isGhost) {
            when (step) {
                0 -> SurpriseChoiceStep(onRandomClick = {}, onPromptClick = {}, onEmotionalClick = {})
                1 -> SurpriseTimeStep(onSelect = {})
                2 -> SurpriseMoodStep(onSelect = {})
                3 -> SurpriseCompanyStep(onSelect = {})
                4 -> SurprisePromptStep(
                    remainingQuota = remainingQuota,
                    isAiMode = isAiMode,
                    onAiModeChanged = {},
                    onSubmit = { _, _ -> }
                )
                5 -> SurpriseLoadingStep(isGhost = true, isSearchingWithAi = isSearchingWithAi)
            }
        } else {
            val slideDir = rtlSlideDirection()
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val duration = 400
                    if (targetState > initialState) {
                        (slideInHorizontally(animationSpec = tween(duration, easing = FastOutSlowInEasing)) { width -> (width / 2) * slideDir } + 
                         fadeIn(animationSpec = tween(duration)) + 
                         scaleIn(initialScale = 0.85f, animationSpec = tween(duration, easing = FastOutSlowInEasing))) togetherWith 
                        (slideOutHorizontally(animationSpec = tween(duration, easing = FastOutSlowInEasing)) { width -> (-width / 2) * slideDir } + 
                         fadeOut(animationSpec = tween(duration)) + 
                         scaleOut(targetScale = 0.85f, animationSpec = tween(duration, easing = FastOutSlowInEasing)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(duration, easing = FastOutSlowInEasing)) { width -> (-width / 2) * slideDir } + 
                         fadeIn(animationSpec = tween(duration)) + 
                         scaleIn(initialScale = 0.85f, animationSpec = tween(duration, easing = FastOutSlowInEasing))) togetherWith 
                        (slideOutHorizontally(animationSpec = tween(duration, easing = FastOutSlowInEasing)) { width -> (width / 2) * slideDir } + 
                         fadeOut(animationSpec = tween(duration)) + 
                         scaleOut(targetScale = 0.85f, animationSpec = tween(duration, easing = FastOutSlowInEasing)))
                    }.using(SizeTransform(clip = false))
                },
                label = "wizard"
            ) { currentStep ->
                when (currentStep) {
                    0 -> SurpriseChoiceStep(
                        onRandomClick = onRandomClick,
                        onPromptClick = onPromptClick,
                        onEmotionalClick = onEmotionalClick
                    )
                    1 -> SurpriseTimeStep(onSelect = onTimeSelect)
                    2 -> SurpriseMoodStep(onSelect = onMoodSelect)
                    3 -> SurpriseCompanyStep(onSelect = onCompanySelect)
                    4 -> SurprisePromptStep(
                        remainingQuota = remainingQuota,
                        isAiMode = isAiMode,
                        onAiModeChanged = onAiModeChanged,
                        onEmitMessage = onEmitMessage,
                        onSubmit = onPromptSubmit
                    )
                    5 -> SurpriseLoadingStep(isGhost = false, isSearchingWithAi = isSearchingWithAi)
                }
            }
        }

        // Progress bar (Symbiont Elastic Indicator)
        if (step in 1..3) {
            Spacer(modifier = Modifier.height(16.dp))
            SymbiontPagerIndicator(
                currentPage = step - 1,
                pageCount = 3,
                persistPreviousDots = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}




