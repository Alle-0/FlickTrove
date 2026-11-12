package com.cinetrack.ui.components.detail

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import com.cinetrack.R
import com.cinetrack.data.api.CastMember
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.shared.ModalCloseButton
import com.cinetrack.ui.components.shared.SymbiontPagerIndicator
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.rtlSlideDirection
import com.cinetrack.ui.utils.verticalFadingEdges
import com.cinetrack.util.VibrationHelper
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * CheckInModal (formerly PeekABooCheckInDrawer)
 *
 * A clean, centered glassmorphic wizard modal for logging:
 * - Step 0: Rating (fluid draggable stars + large score display)
 * - Step 1: Emotional vibes (up to 3 vibes with live community percentages)
 * - Step 2: Cast MVP selection (character/actor avatar picker)
 *
 * Triggered explicitly from the Personal Zone "Vibe" card, or automatically
 * on marking as watched when `autoOpenVibeOnWatch` is enabled.
 */
@Composable
fun CheckInModal(
    visible: Boolean,
    movie: com.cinetrack.data.model.Movie? = null,
    globalStats: com.cinetrack.data.model.GlobalMovieStats? = null,
    cast: List<CastMember>,
    characterImages: Map<String, String> = emptyMap(),
    accentColor: Color,
    hazeState: HazeState,
    onSave: (rating: Double?, vibes: List<String>, mvp: CastMember?, characterImageUrl: String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current

    var currentPage by remember { mutableIntStateOf(0) }
    var rating by remember(movie?.personalRating) { mutableDoubleStateOf(movie?.personalRating ?: 0.0) }

    // Local selection state initialized from movie if present
    var selectedVibes by remember(movie?.emotionalVibes) {
        mutableStateOf(resolveInitialVibes(movie?.emotionalVibes))
    }
    var selectedMvp by remember(movie?.favoriteActorId, cast) {
        mutableStateOf(resolveInitialMvp(movie, cast))
    }

    val coroutineScope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    val burstProgress = remember { Animatable(0f) }

    val particles = remember(accentColor) {
        val random = Random(1337)
        val colors = listOf(
            Color(0xFF10B981), // Emerald Green
            Color(0xFF34D399), // Light Emerald
            Color(0xFFFBBF24), // Gold
            Color(0xFFF59E0B), // Amber
            Color.White,
            accentColor
        )
        List(26) {
            val angle = random.nextFloat() * (2.0 * Math.PI).toFloat()
            val dist = random.nextFloat() * 46f + 16f
            val rad = random.nextFloat() * 3.5f + 2f
            SaveBurstParticle(angle, dist, rad, colors[random.nextInt(colors.size)])
        }
    }

    // Reset when `visible` transitions to true
    LaunchedEffect(visible) {
        if (visible) {
            rating = movie?.personalRating ?: 0.0
            selectedVibes = resolveInitialVibes(movie?.emotionalVibes)
            selectedMvp = resolveInitialMvp(movie, cast)
            currentPage = 0
            isSaving = false
            burstProgress.snapTo(0f)
        }
    }

    val dismissAll = {
        isSaving = false
        onDismiss()
    }

    androidx.activity.compose.BackHandler(enabled = visible) {
        dismissAll()
    }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (visible) 0.5f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "CheckInScrimAlpha"
    )

    if (visible || scrimAlpha > 0f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .zIndex(100f)
                .background(Color.Black.copy(alpha = scrimAlpha))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { dismissAll() })
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(200)) + scaleIn(
                    initialScale = 0.92f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
                exit = fadeOut(tween(150)) + scaleOut(
                    targetScale = 0.92f,
                    animationSpec = tween(150, easing = FastOutSlowInEasing)
                ),
                modifier = Modifier
                    .align(Alignment.Center)
                    .zIndex(10f)
            ) {
            Box(
                modifier = Modifier
                    .width(340.dp)
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(32.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                // Background Layer
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .hazeGlass(
                            state = hazeState,
                            shape = RoundedCornerShape(32.dp),
                            containerColor = Color(0xFF080B14),
                            useOffscreenStrategy = true,
                            borderColor = accentColor.copy(alpha = 0.5f)
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header and Content
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AnimatedContent(
                                targetState = currentPage,
                                label = "TitleAnimation",
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                                },
                                modifier = Modifier.weight(1f)
                            ) { page ->
                                Text(
                                    text = when (page) {
                                        0 -> stringResource(R.string.dialog_rating_drag)
                                        1 -> stringResource(R.string.checkin_how_did_it_make_you_feel)
                                        else -> stringResource(R.string.checkin_mvp_actor)
                                    },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                    color = Color.White,
                                    lineHeight = 16.sp
                                )
                            }
                            ModalCloseButton(onClick = { dismissAll() })
                        }

                        val slideDir = rtlSlideDirection()
                        AnimatedContent(
                            targetState = currentPage,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    slideInHorizontally { width -> width * slideDir } + fadeIn() togetherWith
                                            slideOutHorizontally { width -> -width * slideDir } + fadeOut()
                                } else {
                                    slideInHorizontally { width -> -width * slideDir } + fadeIn() togetherWith
                                            slideOutHorizontally { width -> width * slideDir } + fadeOut()
                                }.using(SizeTransform(clip = false))
                            },
                            label = "Page Transition",
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) { page ->
                            when (page) {
                                0 -> {
                                    // Page 0: Rating
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 20.dp)
                                    ) {
                                        val numberScale = remember { Animatable(1f) }
                                        val coroutineScope = rememberCoroutineScope()

                                        // Large Rating Display on top (matching QuickRatingModal)
                                        if (rating > 0) {
                                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                                Row(
                                                    verticalAlignment = Alignment.Bottom,
                                                    horizontalArrangement = Arrangement.Center,
                                                    modifier = Modifier
                                                        .padding(bottom = 12.dp)
                                                        .graphicsLayer {
                                                            scaleX = numberScale.value
                                                            scaleY = numberScale.value
                                                        }
                                                ) {
                                                    Text(
                                                        text = String.format(java.util.Locale.US, "%.1f", rating),
                                                        style = MaterialTheme.typography.displayLarge.copy(
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 52.sp,
                                                            letterSpacing = (-2).sp
                                                        ),
                                                        color = accentColor,
                                                        textAlign = TextAlign.Center
                                                    )
                                                    Text(
                                                        text = "/ 10",
                                                        style = MaterialTheme.typography.headlineSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White.copy(alpha = 0.25f)
                                                        ),
                                                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                                                    )
                                                }
                                            }
                                        } else {
                                            Text(
                                                text = stringResource(R.string.dialog_rating_remove),
                                                color = Color.White.copy(0.5f),
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(bottom = 24.dp, top = 8.dp)
                                            )
                                        }

                                        // Star Rating Bar underneath
                                        com.cinetrack.ui.components.shared.FluidRatingBar(
                                            rating = rating,
                                            onRatingChange = { rating = it },
                                            onRatingSettled = { settledVal ->
                                                if (settledVal > 0.0) {
                                                    coroutineScope.launch {
                                                        numberScale.snapTo(1.24f)
                                                        numberScale.animateTo(
                                                            targetValue = 1f,
                                                            animationSpec = spring(
                                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                                stiffness = Spring.StiffnessMediumLow
                                                            )
                                                        )
                                                    }
                                                }
                                            },
                                            accentColor = accentColor,
                                            starSize = 54.dp,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                                1 -> {
                                    // Page 1: Vibe Grid
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        ALL_VIBES.chunked(3).forEach { rowVibes ->
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                rowVibes.forEach { vibe ->
                                                    val isSelected = selectedVibes.contains(vibe)
                                                    val isDisabled = !isSelected && selectedVibes.size >= 3
                                                    val originalVibes = remember(movie?.emotionalVibes) {
                                                        resolveInitialVibes(movie?.emotionalVibes).map { it.code }.toSet()
                                                    }
                                                    val currentVibes = selectedVibes.map { it.code }.toSet()
                                                    val missingOriginalVibes = originalVibes.count { 
                                                        (globalStats?.vibes?.get(it) ?: 0L) == 0L 
                                                    }
                                                    val baseGlobalTotal = maxOf(
                                                        globalStats?.totalVibes ?: 0L,
                                                        globalStats?.vibes?.values?.filter { it > 0 }?.sum() ?: 0L
                                                    )
                                                    val baselineTotal = baseGlobalTotal + missingOriginalVibes
                                                    val added = currentVibes - originalVibes
                                                    val removed = originalVibes - currentVibes
                                                    val projectedTotalVibes = maxOf(0L, baselineTotal + added.size - removed.size)
                                                    var baselineVibeCount = maxOf(0L, globalStats?.vibes?.get(vibe.code) ?: 0L)
                                                    
                                                    if (originalVibes.contains(vibe.code) && baselineVibeCount == 0L) {
                                                        baselineVibeCount = 1L
                                                    }
                                                    
                                                    val totalVibes = projectedTotalVibes
                                                    var vibeCount = baselineVibeCount
                                                    if (added.contains(vibe.code)) vibeCount++
                                                    if (removed.contains(vibe.code)) vibeCount--
                                                    vibeCount = maxOf(0L, vibeCount)
                                                    val realPercentage = if (totalVibes > 0) ((vibeCount.toFloat() / totalVibes) * 100).toInt() else 0

                                                    VibeChip(
                                                        modifier = Modifier.weight(1f),
                                                        vibe = vibe.copy(percentage = realPercentage),
                                                        isSelected = isSelected,
                                                        isDisabled = isDisabled,
                                                        accentColor = accentColor,
                                                        onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            selectedVibes = if (isSelected) {
                                                                selectedVibes - vibe
                                                            } else {
                                                                if (selectedVibes.size < 3) selectedVibes + vibe else selectedVibes
                                                            }
                                                        }
                                                    )
                                                }
                                                if (rowVibes.size < 3) {
                                                    repeat(3 - rowVibes.size) {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    // Page 2: MVP Cast Section
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        if (cast.isNotEmpty()) {
                                            val castGridState = rememberLazyGridState()

                                            LazyVerticalGrid(
                                                state = castGridState,
                                                columns = GridCells.Fixed(4),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 12.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(300.dp)
                                                    .verticalFadingEdges(castGridState, 16.dp, 16.dp)
                                            ) {
                                                items(cast.take(24), key = { it.id }) { actor ->
                                                    val isMvp = selectedMvp?.id == actor.id
                                                    val charName = actor.character?.lowercase()?.trim()
                                                    val actorName = actor.name.lowercase().trim()
                                                    val charImageUrl = charName?.let { characterImages[it] } ?: characterImages[actorName]
                                                    val originalMvpId = movie?.favoriteActorId
                                                    val currentMvpId = selectedMvp?.id
                                                    var baselineMvpCount = globalStats?.mvps?.get(actor.id.toString()) ?: 0L
                                                    var totalMvpsCount = globalStats?.totalMvps ?: 0L
                                                    if (originalMvpId == actor.id && baselineMvpCount == 0L) baselineMvpCount = 1L
                                                    if (originalMvpId != null && totalMvpsCount == 0L) totalMvpsCount = 1L
                                                    
                                                    val addedMvp = if (currentMvpId == actor.id && originalMvpId != actor.id) 1L else 0L
                                                    val removedMvp = if (currentMvpId != actor.id && originalMvpId == actor.id) 1L else 0L
                                                    val globalTotalAdded = if (originalMvpId == null && currentMvpId != null) 1L else 0L
                                                    val globalTotalRemoved = if (originalMvpId != null && currentMvpId == null) 1L else 0L

                                                    val projectedMvpCount = maxOf(0L, baselineMvpCount + addedMvp - removedMvp)
                                                    val projectedTotalMvps = maxOf(0L, totalMvpsCount + globalTotalAdded - globalTotalRemoved)
                                                    
                                                    val realPercentage = if (projectedTotalMvps > 0 && projectedMvpCount > 0) {
                                                        ((projectedMvpCount.toFloat() / projectedTotalMvps) * 100).toInt()
                                                    } else null

                                                    CastMvpChip(
                                                        actor = actor,
                                                        isMvp = isMvp,
                                                        accentColor = accentColor,
                                                        characterImageUrl = charImageUrl,
                                                        mvpPercentage = realPercentage,
                                                        onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            selectedMvp = if (isMvp) null else actor
                                                        }
                                                    )
                                                }
                                            }
                                        } else {
                                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                                                Text(stringResource(R.string.checkin_no_cast), color = Color.White.copy(alpha = 0.5f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom area: Pagination Dots + Buttons
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    ) {
                        SymbiontPagerIndicator(
                            currentPage = currentPage,
                            pageCount = 3,
                            modifier = Modifier.padding(bottom = 14.dp),
                            accentColor = accentColor,
                            persistPreviousDots = true,
                            onDotClick = { index ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                currentPage = index
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AnimatedVisibility(
                                visible = currentPage > 0,
                                enter = fadeIn() + expandHorizontally(),
                                exit = fadeOut() + shrinkHorizontally()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .bounceClick(scaleDown = 0.90f) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            currentPage -= 1
                                        }
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_left),
                                        contentDescription = "Back",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Celebration Particle Burst
                                if (isSaving && burstProgress.value > 0f) {
                                    CheckInBurstCanvas(
                                        burstProgress = burstProgress.value,
                                        particles = particles
                                    )
                                }

                                val saveBrush = if (isSaving) {
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF10B981), Color(0xFF059669))
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        colors = listOf(accentColor, accentColor.copy(alpha = 0.7f))
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .bounceClick(scaleDown = if (isSaving) 1f else 0.96f) {
                                            if (isSaving) return@bounceClick
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (currentPage < 2) {
                                                currentPage += 1
                                            } else {
                                                isSaving = true
                                                VibrationHelper.vibrateClick(context)
                                                coroutineScope.launch {
                                                    burstProgress.animateTo(
                                                        targetValue = 1f,
                                                        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing)
                                                    )
                                                    delay(120)
                                                    val finalCharImageUrl = selectedMvp?.let { mvp ->
                                                        val cName = mvp.character?.lowercase()?.trim()
                                                        val aName = mvp.name.lowercase().trim()
                                                        cName?.let { characterImages[it] }
                                                            ?: characterImages[aName]
                                                            ?: movie?.takeIf { it.favoriteActorId == mvp.id }?.favoriteActorProfilePath
                                                                ?.takeIf { it.isNotBlank() && it != mvp.profilePath }
                                                    }
                                                    val finalRating = if (rating > 0.0) rating else null
                                                    onSave(finalRating, selectedVibes.map { it.code }, selectedMvp, finalCharImageUrl)
                                                    dismissAll()
                                                }
                                            }
                                        }
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(saveBrush),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AnimatedContent(
                                        targetState = isSaving,
                                        transitionSpec = {
                                            (scaleIn(
                                                initialScale = 0.5f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            ) + fadeIn(tween(200)))
                                                .togetherWith(scaleOut(targetScale = 0.5f) + fadeOut(tween(150)))
                                        },
                                        label = "SaveButtonContent"
                                    ) { saving ->
                                        if (saving) {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_tick),
                                                contentDescription = "Saved",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            Text(
                                                text = if (currentPage < 2) stringResource(R.string.checkin_next) else stringResource(R.string.checkin_save_diary),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0B0F19)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

/**
 * Backward compatibility alias for [CheckInModal].
 */
@Deprecated(
    message = "Use CheckInModal directly. Peek-a-boo tab has been phased out.",
    replaceWith = ReplaceWith("CheckInModal(visible = visible, movie = movie, globalStats = globalStats, cast = cast, characterImages = characterImages, accentColor = accentColor, hazeState = hazeState, onSave = onSave, onDismiss = onDismiss, modifier = modifier)")
)
@Composable
fun PeekABooCheckInDrawer(
    visible: Boolean,
    startExpanded: Boolean = true,
    movie: com.cinetrack.data.model.Movie? = null,
    globalStats: com.cinetrack.data.model.GlobalMovieStats? = null,
    cast: List<CastMember>,
    characterImages: Map<String, String> = emptyMap(),
    accentColor: Color,
    hazeState: HazeState,
    onSave: (rating: Double?, vibes: List<String>, mvp: CastMember?, characterImageUrl: String?) -> Unit,
    onDismiss: () -> Unit,
    peekTimeoutMs: Long = 6000L,
    modifier: Modifier = Modifier
) {
    CheckInModal(
        visible = visible,
        movie = movie,
        globalStats = globalStats,
        cast = cast,
        characterImages = characterImages,
        accentColor = accentColor,
        hazeState = hazeState,
        onSave = onSave,
        onDismiss = onDismiss,
        modifier = modifier
    )
}

private fun resolveInitialVibes(emotionalVibes: String?): Set<EmotionalVibe> {
    return emotionalVibes
        ?.split(",")
        ?.mapNotNull { vibeString -> findVibe(vibeString) }
        ?.toSet()
        ?: emptySet()
}

private fun resolveInitialMvp(
    movie: com.cinetrack.data.model.Movie?,
    cast: List<CastMember>
): CastMember? {
    val actorId = movie?.favoriteActorId ?: return null
    return cast.find { it.id == actorId } ?: CastMember(
        id = actorId,
        name = movie.favoriteActorName.orEmpty(),
        character = movie.favoriteActorCharacter,
        profilePath = movie.favoriteActorTmdbPath ?: movie.favoriteActorProfilePath?.takeIf { !it.startsWith("http") }
    )
}
