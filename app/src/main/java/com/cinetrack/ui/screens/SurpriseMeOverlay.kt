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

    val remainingQuota by viewModel.remainingQuota.collectAsState()
    var isAiMode by remember(remainingQuota) { mutableStateOf(remainingQuota > 0) }

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
                onAiModeChanged = { isAiMode = it },
                onEmitMessage = { viewModel.emitMessage(it) },
                isGhost = contentAlpha == 0f,
                onBack = handleBack,
                onClose = onClose,
                onRandomClick = {
                    scope.launch {
                        step = 5
                        delay(800)
                        val movie = viewModel.getRandomMovie()
                        onMovieFound(movie)
                    }
                },
                onPromptClick = { step = 4 },
                onPromptSubmit = { text, aiMode ->
                    scope.launch {
                        step = 5
                        delay(800)
                        val movie = viewModel.getPromptMovie(text, forceLocal = !aiMode)
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
                0 -> ChoiceStep(onRandomClick = {}, onPromptClick = {}, onEmotionalClick = {})
                1 -> TimeStep(onSelect = {})
                2 -> MoodStep(onSelect = {})
                3 -> CompanyStep(onSelect = {})
                4 -> PromptStep(
                    remainingQuota = remainingQuota,
                    isAiMode = isAiMode,
                    onAiModeChanged = {},
                    onSubmit = { _, _ -> }
                )
                5 -> LoadingStep(isGhost = true)
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
                    0 -> ChoiceStep(
                        onRandomClick = onRandomClick,
                        onPromptClick = onPromptClick,
                        onEmotionalClick = onEmotionalClick
                    )
                    1 -> TimeStep(onSelect = onTimeSelect)
                    2 -> MoodStep(onSelect = onMoodSelect)
                    3 -> CompanyStep(onSelect = onCompanySelect)
                    4 -> PromptStep(
                        remainingQuota = remainingQuota,
                        isAiMode = isAiMode,
                        onAiModeChanged = onAiModeChanged,
                        onEmitMessage = onEmitMessage,
                        onSubmit = onPromptSubmit
                    )
                    5 -> LoadingStep(isGhost = false)
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

@Composable
private fun ChoiceStep(
    onRandomClick: () -> Unit,
    onPromptClick: () -> Unit,
    onEmotionalClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
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

        PromptOptionCard(
            title = stringResource(R.string.surprise_mode_prompt),
            description = stringResource(R.string.surprise_mode_prompt_desc),
            icon = ImageVector.vectorResource(R.drawable.ic_pencil),
            onClick = onPromptClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PromptOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .bounceClick(scaleDown = 0.94f, onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = "AI",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_right),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun PromptStep(
    remainingQuota: Int,
    isAiMode: Boolean,
    onAiModeChanged: (Boolean) -> Unit,
    onEmitMessage: (UiText) -> Unit = {},
    onSubmit: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                text = if (text.isBlank()) spoken else "$text $spoken"
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(150)
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title on its own line: full visibility without truncation
        Text(
            text = stringResource(R.string.surprise_prompt_title),
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Mode Selector (IA • 3 | ALGO • ∞) placed directly beneath the title
        Box(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CategoryTabSelector(
                options = listOf(
                    stringResource(R.string.search_mood_mode_ai_tab, remainingQuota),
                    stringResource(R.string.search_mood_mode_local_tab)
                ),
                selectedIndex = if (isAiMode) 0 else 1,
                onOptionClick = { index ->
                    if (index == 0) {
                        if (remainingQuota > 0) {
                            onAiModeChanged(true)
                        } else {
                            onEmitMessage(UiText.StringResource(R.string.search_mood_exhausted_notice))
                        }
                    } else {
                        onAiModeChanged(false)
                    }
                },
                tabWidth = 72.dp,
                fontSize = 10.sp,
                uppercase = true,
                enableDrag = false
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Text input card (Concentric 16dp radius)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column {
                BasicTextField(
                    value = text,
                    onValueChange = { if (it.length <= 300) text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp, max = 110.dp)
                        .focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        lineHeight = 20.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (text.isNotBlank()) {
                                keyboardController?.hide()
                                onSubmit(text, isAiMode)
                            }
                        }
                    ),
                    decorationBox = { innerTextField ->
                        if (text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.surprise_prompt_placeholder),
                                color = Color.White.copy(alpha = 0.35f),
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 20.sp
                            )
                        }
                        innerTextField()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${text.length}/300",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.35f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (text.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .bounceClick { text = "" }
                                    .background(Color.White.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .bounceClick {
                                    try {
                                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(
                                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                            )
                                        }
                                        speechLauncher.launch(intent)
                                    } catch (_: Exception) {}
                                }
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Mic,
                                contentDescription = "Detta",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action button - Concentric with the box and outer modal (R_modal 32dp - Padding 16dp = 16dp)
        val isEnabled = text.isNotBlank()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .bounceClick(enabled = isEnabled) {
                    keyboardController?.hide()
                    onSubmit(text, isAiMode)
                }
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isEnabled) MaterialTheme.colorScheme.primary
                    else Color.White.copy(alpha = 0.08f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_sparkle),
                    contentDescription = null,
                    tint = if (isEnabled) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.surprise_title),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.3f)
                    )
                )
            }
        }
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
