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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.ImageType
import com.cinetrack.util.buildTmdbImageUrl
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay

/**
 * Represents one selectable emotional reaction.
 */
data class EmotionalVibe(val code: String, val emoji: String, @StringRes val labelRes: Int, val percentage: Int)

val ALL_VIBES = listOf(
    EmotionalVibe("MASTERPIECE",  "🤩", R.string.checkin_vibe_masterpiece, 34),
    EmotionalVibe("MIND_BLOWING", "🤯", R.string.checkin_vibe_mind_blowing, 21),
    EmotionalVibe("IN_TEARS",     "😭", R.string.checkin_vibe_in_tears, 15),
    EmotionalVibe("HYPED",        "🔥", R.string.checkin_vibe_action, 7),
    EmotionalVibe("COZY",         "☕", R.string.checkin_vibe_comfort, 8),
    EmotionalVibe("FEELS_GOOD",   "😊", R.string.checkin_vibe_feels_good, 12),
    EmotionalVibe("FUNNY",        "😂", R.string.checkin_vibe_funny, 5),
    EmotionalVibe("MEH",          "😐", R.string.checkin_vibe_meh, 3),
    EmotionalVibe("WEIRD",        "🌀", R.string.checkin_vibe_weird, 2),
    EmotionalVibe("SCARY",        "😱", R.string.checkin_vibe_scary, 1),
    EmotionalVibe("DISAPPOINTED", "😤", R.string.checkin_vibe_disappointed, 1),
    EmotionalVibe("BORING",       "😴", R.string.checkin_vibe_boring, 1)
)

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
    onSave: (vibes: List<String>, mvp: CastMember?, characterImageUrl: String?) -> Unit,
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

    // Local selection state initialized from movie if present
    var selectedVibes by remember(movie?.emotionalVibes) { 
        val initial = movie?.emotionalVibes?.split(",")?.mapNotNull { vibeString ->
            val clean = vibeString.trim()
            val emojiPart = clean.split(" ").firstOrNull()
            ALL_VIBES.find { it.code == clean || it.emoji == emojiPart }
        }?.toSet() ?: emptySet()
        mutableStateOf(initial) 
    }
    var selectedMvp by remember(movie?.favoriteActorId) { 
        mutableStateOf(cast.find { it.id == movie?.favoriteActorId })
    }

    // Reset and trigger peek when `visible` transitions to true
    LaunchedEffect(visible, startExpanded) {
        if (visible) {
            // Restore selection to the saved movie state (in case they modified and dismissed previously)
            selectedVibes = movie?.emotionalVibes?.split(",")?.mapNotNull { vibeString ->
                val clean = vibeString.trim()
                val emojiPart = clean.split(" ").firstOrNull()
                ALL_VIBES.find { it.code == clean || it.emoji == emojiPart }
            }?.toSet() ?: emptySet()
            selectedMvp = cast.find { it.id == movie?.favoriteActorId }
            currentPage = 0
            isExpanded = startExpanded
            isPeeking = true
        } else {
            isPeeking = false
            isExpanded = false
            currentPage = 0
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
                    .clip(RoundedCornerShape(20.dp))
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
                            shape = RoundedCornerShape(20.dp),
                            containerColor = Color(0xFF080B14),
                            useOffscreenStrategy = true,
                            borderColor = accentColor.copy(alpha = 0.5f)
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
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
                                    text = if (page == 0) stringResource(R.string.checkin_how_did_it_make_you_feel) else stringResource(R.string.checkin_mvp_actor),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                    color = Color.White,
                                    lineHeight = 16.sp
                                )
                            }
                            // Close button
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { dismissAll() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_x),
                                    contentDescription = "Close",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
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
                                // Page 1: Vibe Grid
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    ALL_VIBES.chunked(3).forEach { rowVibes ->
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            rowVibes.forEach { vibe ->
                                                val isSelected = selectedVibes.contains(vibe)
                                                val scale by animateFloatAsState(
                                                    targetValue = if (isSelected) 1.08f else 1.0f,
                                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                                    label = "vibeScale"
                                                )
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
                                                    scale = scale,
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

                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(4),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(300.dp)
                                                .graphicsLayer(compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen)
                                                .drawWithContent {
                                                    drawContent()
                                                    drawRect(
                                                        brush = Brush.verticalGradient(
                                                            0f to Color.Transparent,
                                                            0.1f to Color.Black,
                                                            0.9f to Color.Black,
                                                            1f to Color.Transparent
                                                        ),
                                                        blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                                                    )
                                                }
                                        ) {
                                            items(cast.take(24), key = { it.id }) { actor ->
                                                val isMvp = selectedMvp?.id == actor.id
                                                val charName = actor.character?.lowercase()?.trim()
                                                val actorName = actor.name.lowercase().trim()
                                                val charImageUrl = charName?.let { characterImages[it] } ?: characterImages[actorName]
                                                CastMvpChip(
                                                    actor = actor,
                                                    isMvp = isMvp,
                                                    accentColor = accentColor,
                                                    characterImageUrl = charImageUrl,
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
                        // Pagination dots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            repeat(2) { index ->
                                val isCurrent = currentPage == index
                                val isDone = currentPage > index
                                val color by animateColorAsState(
                                    targetValue = if (isCurrent || isDone) accentColor else Color.White.copy(alpha = 0.2f),
                                    label = "dotColor"
                                )
                                val width by animateDpAsState(
                                    targetValue = if (isCurrent) 16.dp else 6.dp,
                                    label = "dotWidth"
                                )
                                Box(
                                    modifier = Modifier
                                        .height(6.dp)
                                        .width(width)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            currentPage = index
                                        }
                                )
                            }
                        }

                        // Back + Save / Next buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Back button (only visible on page 2)
                            AnimatedVisibility(
                                visible = currentPage == 1,
                                enter = fadeIn() + expandHorizontally(),
                                exit = fadeOut() + shrinkHorizontally()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .height(40.dp)
                                        .width(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.1f))
                                        .bounceClick(scaleDown = 0.95f) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            currentPage = 0
                                        },
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
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(accentColor, accentColor.copy(alpha = 0.7f))
                                        )
                                    )
                                    .bounceClick(scaleDown = 0.95f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (currentPage == 0) {
                                            currentPage = 1
                                        } else {
                                            val finalCharImageUrl = selectedMvp?.let { mvp ->
                                                val cName = mvp.character?.lowercase()?.trim()
                                                val aName = mvp.name.lowercase().trim()
                                                cName?.let { characterImages[it] } ?: characterImages[aName]
                                            }
                                            onSave(selectedVibes.map { it.code }, selectedMvp, finalCharImageUrl)
                                            dismissAll()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (currentPage == 0) stringResource(R.string.checkin_next) else stringResource(R.string.checkin_save_diary),
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
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
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
    scale: Float,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .scale(scale)
            .alpha(if (isDisabled) 0.3f else 1f)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) accentColor.copy(alpha = 0.18f)
                else Color.White.copy(alpha = 0.06f)
            )
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) accentColor else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
        ) {
            Text(
                text = vibe.emoji, 
                fontSize = 18.sp,
                lineHeight = 18.sp
            )
            Text(
                text = stringResource(vibe.labelRes),
                fontSize = 9.sp,
                lineHeight = 10.sp,
                color = if (isSelected) accentColor else Color.White.copy(alpha = 0.6f),
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
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isMvp) 1.06f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "mvpScale"
    )
    val mvpGold = Color(0xFFFFC800)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
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
