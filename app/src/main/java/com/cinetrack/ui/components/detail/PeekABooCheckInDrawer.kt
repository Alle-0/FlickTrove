package com.cinetrack.ui.components.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.annotation.StringRes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import coil.compose.AsyncImage
import com.cinetrack.R
import com.cinetrack.data.api.CastMember
import com.cinetrack.ui.components.glass.glassmorphic
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.shared.ModalCloseButton
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.verticalFadingEdges
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.ImageType
import com.cinetrack.util.buildTmdbImageUrl
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.cinetrack.ui.components.shared.SymbiontPagerIndicator
import com.cinetrack.util.VibrationHelper
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class SaveBurstParticle(
    val angle: Float,
    val distance: Float,
    val radius: Float,
    val color: Color
)

/**
 * Represents one selectable emotional reaction.
 */
data class EmotionalVibe(val code: String, val emoji: String, @StringRes val labelRes: Int, val percentage: Int, val iconRes: Int, val colorHex: Long)

val ALL_VIBES = listOf(
    EmotionalVibe("MASTERPIECE",  "🤩", R.string.checkin_vibe_masterpiece, 34, R.drawable.ic_vibe_masterpiece, 0xFFFFD700),
    EmotionalVibe("MIND_BLOWING", "🤯", R.string.checkin_vibe_mind_blowing, 21, R.drawable.ic_vibe_mind_blowing, 0xFF9C27B0),
    EmotionalVibe("IN_TEARS",     "😭", R.string.checkin_vibe_in_tears, 15, R.drawable.ic_vibe_in_tears, 0xFF2196F3),
    EmotionalVibe("HYPED",        "🔥", R.string.checkin_vibe_action, 7, R.drawable.ic_vibe_hyped, 0xFFFF5722),
    EmotionalVibe("COZY",         "☕", R.string.checkin_vibe_comfort, 8, R.drawable.ic_vibe_cozy, 0xFFFF9800),
    EmotionalVibe("FEELS_GOOD",   "😊", R.string.checkin_vibe_feels_good, 12, R.drawable.ic_vibe_feels_good, 0xFFFFEB3B),
    EmotionalVibe("FUNNY",        "😂", R.string.checkin_vibe_funny, 5, R.drawable.ic_vibe_funny, 0xFFE91E63),
    EmotionalVibe("MEH",          "😐", R.string.checkin_vibe_meh, 3, R.drawable.ic_vibe_meh, 0xFF9E9E9E),
    EmotionalVibe("WEIRD",        "🌀", R.string.checkin_vibe_weird, 2, R.drawable.ic_vibe_weird, 0xFF00BCD4),
    EmotionalVibe("SCARY",        "😱", R.string.checkin_vibe_scary, 1, R.drawable.ic_vibe_scary, 0xFFF44336),
    EmotionalVibe("DISAPPOINTED", "😤", R.string.checkin_vibe_disappointed, 1, R.drawable.ic_vibe_disappointed, 0xFFE64A19),
    EmotionalVibe("BORING",       "😴", R.string.checkin_vibe_boring, 1, R.drawable.ic_vibe_boring, 0xFF7986CB)
)

fun normalizeVibeCode(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val clean = raw.trim().uppercase()
    return when (clean) {
        "MIND_BLOWING", "MINDBLOWING", "MIND-BLOWING", "SHOCKED", "SHOCK", "SURPRISED", "🤯" -> "MIND_BLOWING"
        "HYPED", "HYPE", "THRILLED", "THRILL", "EXCITED", "ACTION", "🔥" -> "HYPED"
        "FUNNY", "AMUSED", "AMUSING", "HILARIOUS", "LAUGH", "😂" -> "FUNNY"
        "IN_TEARS", "TEARS", "SAD", "CRYING", "HEARTBREAKING", "😭" -> "IN_TEARS"
        "FEELS_GOOD", "FEELSGOOD", "FEELS GOOD", "TOUCHED", "WHOLESOME", "WARM", "LOVED", "HEARTWARMING", "😊" -> "FEELS_GOOD"
        "COZY", "UNDERSTOOD", "COMFORT", "PEACEFUL", "RELAXING", "CHILL", "☕" -> "COZY"
        "DISAPPOINTED", "FRUSTRATED", "ANGRY", "MAD", "UPSET", "😤" -> "DISAPPOINTED"
        "WEIRD", "CONFUSED", "STRANGE", "WTF", "SURREAL", "ODD", "🌀" -> "WEIRD"
        "SCARY", "SCARED", "TENSE", "SPOOKY", "TERRIFIED", "HORROR", "😱" -> "SCARY"
        "BORING", "BORED", "SLEEPY", "TIRED", "DULL", "😴" -> "BORING"
        "MASTERPIECE", "PERFECT", "GOAT", "AMAZING", "BRILLIANT", "CAPOLAVORO", "🤩" -> "MASTERPIECE"
        "MEH", "AVERAGE", "OK", "NEUTRAL", "SO-SO", "😐" -> "MEH"
        else -> clean
    }
}

fun findVibe(codeOrEmoji: String?): EmotionalVibe? {
    if (codeOrEmoji.isNullOrBlank()) return null
    val normalized = normalizeVibeCode(codeOrEmoji)
    val emojiPart = codeOrEmoji.trim().split(" ").firstOrNull()
    return ALL_VIBES.find { it.code == normalized || it.emoji == codeOrEmoji.trim() || (emojiPart != null && it.emoji == emojiPart) }
}

/**
 * PeekABooCheckInDrawer
 *
 * A non-invasive "peek-a-boo" panel anchored to the right edge of the screen.
 * - State 0 (HIDDEN): fully off-screen, no UI shown.
 * - State 1 (PEEKING): a small vertical tab slides in from the right edge as a subtle handle.
 *   Auto-dismisses after [peekTimeoutMs] ms if never tapped.
 * - State 2 (EXPANDED): tapping the tab springs open a full glassmorph panel with
 *   Vibe emoji selection and Cast MVP picker. The user can save or dismiss.
 *
 * The primary action ("Mark as Watched") is never blocked. This drawer is purely additive.
 */
@Composable
fun PeekABooCheckInDrawer(
    visible: Boolean,
    startExpanded: Boolean = false,
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
    val haptic = LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current

    // Drawer state machine
    var isPeeking by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(0) }

    var rating by remember(movie?.personalRating) { mutableDoubleStateOf(movie?.personalRating ?: 0.0) }

    // Local selection state initialized from movie if present
    var selectedVibes by remember(movie?.emotionalVibes) { 
        val initial = movie?.emotionalVibes?.split(",")?.mapNotNull { vibeString ->
            findVibe(vibeString)
        }?.toSet() ?: emptySet()
        mutableStateOf(initial) 
    }
    var selectedMvp by remember(movie?.favoriteActorId) { 
        mutableStateOf(cast.find { it.id == movie?.favoriteActorId })
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

    // Reset and trigger peek when `visible` transitions to true
    LaunchedEffect(visible, startExpanded) {
        if (visible) {
            // Restore selection to the saved movie state (in case they modified and dismissed previously)
            rating = movie?.personalRating ?: 0.0
            selectedVibes = movie?.emotionalVibes?.split(",")?.mapNotNull { vibeString ->
                findVibe(vibeString)
            }?.toSet() ?: emptySet()
            selectedMvp = cast.find { it.id == movie?.favoriteActorId }
            currentPage = 0
            isExpanded = startExpanded
            isPeeking = true
            isSaving = false
            burstProgress.snapTo(0f)
        } else {
            isPeeking = false
            isExpanded = false
            currentPage = 0
            isSaving = false
            burstProgress.snapTo(0f)
        }
    }

    // Auto-dismiss the peek tab after timeout (only while just peeking, not expanded)
    LaunchedEffect(isPeeking, isExpanded) {
        if (isPeeking && !isExpanded) {
            delay(peekTimeoutMs)
            if (!isExpanded) {
                isPeeking = false
                onDismiss()
            }
        }
    }

    val dismissAll = {
        isPeeking = false
        isExpanded = false
        isSaving = false
        onDismiss()
    }

    val boxModifier = if (isExpanded) {
        modifier.fillMaxSize()
            .zIndex(100f)
            .background(Color.Black.copy(alpha = 0.5f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { dismissAll() })
            }
    } else {
        modifier.fillMaxSize()
    }

    Box(modifier = boxModifier, contentAlignment = if (isExpanded) Alignment.Center else Alignment.TopStart) {

        // ── EXPANDED MODAL PANEL ──────────────────────────────────────────────
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150)),
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
                        onClick = {} // Consume taps so they don't reach the outer box
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
                            // Close button
                            ModalCloseButton(
                                onClick = { dismissAll() }
                            )
                        }

                        AnimatedContent(
                            targetState = currentPage,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                                            slideOutHorizontally { width -> -width } + fadeOut()
                                } else {
                                    slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                            slideOutHorizontally { width -> width } + fadeOut()
                                }.using(SizeTransform(clip = false))
                            },
                            label = "Page Transition",
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) { page ->
                            if (page == 0) {
                                // Page 0: Rating
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                                ) {
                                    com.cinetrack.ui.components.shared.FluidRatingBar(
                                        rating = rating,
                                        onRatingChange = { rating = it },
                                        accentColor = accentColor,
                                        starSize = 56.dp,
                                        modifier = Modifier.padding(vertical = 16.dp)
                                    )
                                    if (rating > 0) {
                                        Text(
                                            text = rating.toString().removeSuffix(".0"),
                                            color = accentColor,
                                            style = MaterialTheme.typography.displayMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 48.sp
                                            )
                                        )
                                    } else {
                                        Text(
                                            text = stringResource(R.string.dialog_rating_remove),
                                            color = Color.White.copy(0.5f),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            } else if (page == 1) {
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
                                                    movie?.emotionalVibes?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
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
                            } else {
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
                                                val totalAdded = if (currentMvpId != null && currentMvpId != originalMvpId && currentMvpId != actor.id && originalMvpId != actor.id) 1L else 0L
                                                val totalRemoved = if (originalMvpId != null && currentMvpId != originalMvpId && currentMvpId != actor.id && originalMvpId != actor.id) 1L else 0L
                                                
                                                // Actually the total added/removed is just based on whether the current MVP is different from original MVP.
                                                // Wait, if current MVP is different from original MVP, we remove 1 from total (for original) and add 1 to total (for current).
                                                // So totalMvpsCount doesn't change unless we go from no MVP to some MVP (+1) or some MVP to no MVP (-1).
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
                        } // ends AnimatedContent
                    } // ends inner content Column

                    // Bottom area: Pagination Dots + Buttons
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    ) {
                        // Pagination dots (Symbiont / Worm elastico con memoria dei passi precedenti)
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

                        // Back + Save / Next buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Back button (only visible on page 1 and 2)
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

                            // Next / Save button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Celebration Particle Burst
                                if (isSaving && burstProgress.value > 0f) {
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer { clip = false }
                                    ) {
                                        val p = burstProgress.value
                                        val centerX = size.width / 2f
                                        val centerY = size.height / 2f

                                        // Expanding soft radial glow
                                        val glowRadius = (size.width / 2f + 28.dp.toPx()) * p
                                        val glowAlpha = (1f - p) * 0.45f
                                        if (glowAlpha > 0.01f) {
                                            drawCircle(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(
                                                        Color(0xFF10B981).copy(alpha = glowAlpha),
                                                        Color.Transparent
                                                    ),
                                                    center = Offset(centerX, centerY),
                                                    radius = glowRadius
                                                ),
                                                center = Offset(centerX, centerY),
                                                radius = glowRadius
                                            )
                                        }

                                        // Radial flying particles
                                        for (particle in particles) {
                                            val distPx = particle.distance.dp.toPx() * p
                                            val px = centerX + cos(particle.angle) * distPx
                                            val py = centerY + sin(particle.angle) * distPx
                                            val pAlpha = (1f - p).coerceIn(0f, 1f)
                                            val pRadius = particle.radius.dp.toPx() * (1f - p * 0.35f)

                                            drawCircle(
                                                color = particle.color.copy(alpha = pAlpha),
                                                radius = pRadius,
                                                center = Offset(px, py)
                                            )
                                        }
                                    }
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
                                                        cName?.let { characterImages[it] } ?: characterImages[aName]
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

        // ── PEEK TAB ───────────────────────────────────────────────────
        AnimatedVisibility(
            visible = isPeeking && !isExpanded,
            enter = slideInHorizontally(
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                initialOffsetX = { it }
            ) + fadeIn(tween(200)),
            exit = slideOutHorizontally(
                animationSpec = tween(200),
                targetOffsetX = { it }
            ) + fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .zIndex(10f)
        ) {
            // The "handle" tab protruding from the right edge
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(96.dp)
                    .bounceClick(scaleDown = 0.94f) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isExpanded = true
                    },
                contentAlignment = Alignment.Center
            ) {
                // Background Layer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeGlass(
                            state = hazeState,
                            shape = RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp),
                            containerColor = Color(0xFF080B14),
                            useOffscreenStrategy = true,
                            borderColor = accentColor.copy(alpha = 0.6f)
                        )
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_sparkle),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.checkin_vibe_tab).uppercase(),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

// ── PRIVATE SUB-COMPOSABLES ─────────────────────────────────────────────────

@Composable
private fun VibeChip(
    vibe: EmotionalVibe,
    isSelected: Boolean,
    isDisabled: Boolean = false,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .alpha(if (isDisabled) 0.3f else 1f)
            .bounceClick(scaleDown = if (isDisabled) 1f else 0.90f) {
                if (!isDisabled) onClick()
            }
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (isSelected) accentColor.copy(alpha = 0.18f)
                else Color.White.copy(alpha = 0.06f)
            )
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) accentColor else Color.Transparent,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = vibe.iconRes),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isSelected) accentColor else Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = stringResource(vibe.labelRes),
                fontSize = 9.sp,
                lineHeight = 10.sp,
                color = if (isSelected) accentColor else Color.White.copy(alpha = 0.7f),
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
    }
}

@Composable
private fun CastMvpChip(
    actor: CastMember,
    isMvp: Boolean,
    accentColor: Color,
    characterImageUrl: String? = null,
    mvpPercentage: Int? = null,
    onClick: () -> Unit
) {
    val mvpGold = Color(0xFFFFC800)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .bounceClick(scaleDown = 0.92f) { onClick() }
    ) {
        Box {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(64.dp)
                .height(96.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(
                    width = if (isMvp) 2.dp else 1.dp,
                    color = if (isMvp) mvpGold else Color.White.copy(alpha = 0.2f),
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
            if (isMvp) {
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(mvpGold)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_star_piena),
                        contentDescription = null,
                        tint = Color(0xFF1A1A1A),
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = actor.character ?: actor.name,
            fontSize = 9.sp,
            color = if (isMvp) mvpGold else Color.White.copy(alpha = 0.75f),
            fontWeight = if (isMvp) FontWeight.Bold else FontWeight.Normal,
            maxLines = 2,
            lineHeight = 10.sp,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        if (mvpPercentage != null && mvpPercentage > 0) {
            Text(
                text = "$mvpPercentage%",
                color = accentColor,
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
