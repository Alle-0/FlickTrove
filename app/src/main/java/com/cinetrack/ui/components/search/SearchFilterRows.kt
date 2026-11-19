package com.cinetrack.ui.components.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.cinetrack.R
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.mirrorInRtl
import com.cinetrack.ui.utils.rtlSafeLetterSpacing
import com.cinetrack.ui.viewmodel.FilterPill
import com.cinetrack.data.model.MoodHistoryEntry
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class ActiveSearchBubble(
    val query: String,
    val isMood: Boolean,
    val anchorBounds: Rect,
    val onDelete: () -> Unit,
    val onSearch: () -> Unit
)

@Composable
fun SearchRecentSearchesRow(
    recentSearches: List<String>,
    query: String,
    onSearchClick: (String) -> Unit,
    onClearAll: () -> Unit,
    onDeleteSearch: (String) -> Unit,
    hazeState: HazeState? = null,
    onOpenBubble: ((ActiveSearchBubble) -> Unit)? = null
) {
    AnimatedVisibility(
        visible = recentSearches.isNotEmpty() && query.isEmpty(),
        enter = expandVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(
            animationSpec = tween(180, easing = LinearOutSlowInEasing)
        ),
        exit = shrinkVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeOut(
            animationSpec = tween(140, easing = FastOutSlowInEasing)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.search_recent), 
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), 
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.Bold, 
                    letterSpacing = rtlSafeLetterSpacing(1.sp)
                )
                Text(
                    text = stringResource(R.string.search_clear_all),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = rtlSafeLetterSpacing(0.5.sp),
                    modifier = Modifier.bounceClick { onClearAll() }.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp), modifier = Modifier.fillMaxWidth()) {
                items(recentSearches, key = { it }, contentType = { "recent_search" }) { search ->
                    var pillCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .widthIn(max = 220.dp)
                            .onGloballyPositioned { pillCoords = it }
                            .bounceClick(
                                onLongClick = {
                                    val bounds = pillCoords?.boundsInRoot() ?: Rect.Zero
                                    onOpenBubble?.invoke(
                                        ActiveSearchBubble(
                                            query = search,
                                            isMood = false,
                                            anchorBounds = bounds,
                                            onDelete = { onDeleteSearch(search) },
                                            onSearch = { onSearchClick(search) }
                                        )
                                    )
                                },
                                onClick = { onSearchClick(search) }
                            )
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = search,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides androidx.compose.ui.unit.Dp.Unspecified) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_x),
                                    contentDescription = stringResource(R.string.search_remove_recent),
                                    modifier = Modifier
                                        .bounceClick(scaleDown = 0.8f) { onDeleteSearch(search) }
                                        .size(18.dp)
                                        .padding(4.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SearchRecentMoodsRow(
    moodHistory: List<MoodHistoryEntry>,
    query: String,
    onMoodClick: (MoodHistoryEntry) -> Unit,
    onClearAll: () -> Unit,
    onDeleteMood: (String) -> Unit,
    hazeState: HazeState? = null,
    onOpenBubble: ((ActiveSearchBubble) -> Unit)? = null
) {
    AnimatedVisibility(
        visible = moodHistory.isNotEmpty() && query.isEmpty(),
        enter = expandVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(
            animationSpec = tween(180, easing = LinearOutSlowInEasing)
        ),
        exit = shrinkVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeOut(
            animationSpec = tween(140, easing = FastOutSlowInEasing)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_sparkle),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.search_recent).uppercase(),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = rtlSafeLetterSpacing(1.sp)
                    )
                }
                Text(
                    text = stringResource(R.string.search_clear_all),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = rtlSafeLetterSpacing(0.5.sp),
                    modifier = Modifier.bounceClick { onClearAll() }.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(moodHistory, key = { it.query }, contentType = { "recent_mood" }) { item ->
                    var pillCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .widthIn(max = 220.dp)
                            .onGloballyPositioned { pillCoords = it }
                            .bounceClick(
                                onLongClick = {
                                    val bounds = pillCoords?.boundsInRoot() ?: Rect.Zero
                                    onOpenBubble?.invoke(
                                        ActiveSearchBubble(
                                            query = item.query,
                                            isMood = true,
                                            anchorBounds = bounds,
                                            onDelete = { onDeleteMood(item.query) },
                                            onSearch = { onMoodClick(item) }
                                        )
                                    )
                                },
                                onClick = { onMoodClick(item) }
                            )
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_sparkle),
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = item.query,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides androidx.compose.ui.unit.Dp.Unspecified) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_x),
                                    contentDescription = stringResource(R.string.search_remove_recent),
                                    modifier = Modifier
                                        .bounceClick(scaleDown = 0.8f) { onDeleteMood(item.query) }
                                        .size(18.dp)
                                        .padding(4.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SearchSuggestedFiltersRow(
    suggestedFilters: List<FilterPill>,
    query: String,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onFilterClick: (FilterPill) -> Unit
) {
    AnimatedVisibility(
        visible = suggestedFilters.isNotEmpty() && query.isNotEmpty(),
        enter = expandVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(
            animationSpec = tween(180, easing = LinearOutSlowInEasing)
        ),
        exit = shrinkVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeOut(
            animationSpec = tween(140, easing = FastOutSlowInEasing)
        )
    ) {
        val rotationAngle by animateFloatAsState(
            targetValue = if (isExpanded) 90f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "suggested_filters_chevron"
        )

        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .bounceClick { onToggleExpanded() }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.search_suggested), 
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), 
                fontSize = 10.sp, 
                fontWeight = FontWeight.Bold, 
                letterSpacing = rtlSafeLetterSpacing(1.sp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                contentDescription = if (isExpanded) stringResource(R.string.search_collapse) else stringResource(R.string.search_expand),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(12.dp)
                    .mirrorInRtl()
                    .rotate(rotationAngle)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                items(suggestedFilters, key = { "${it.id}_${it.isKeyword}_${it.name}" }, contentType = { "suggested_filter" }) { filter ->
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .bounceClick { onFilterClick(filter) }
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.name, 
                            modifier = Modifier.padding(horizontal = 14.dp), 
                            color = MaterialTheme.colorScheme.primary, 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * Overlay in-window della bolla di anteprima ricerca/mood recente.
 * Renderizzata a livello di schermata (stessa finestra di internalHazeState)
 * per garantire vero sfocamento Haze Glassmorphism in tempo reale.
 *
 * Applica rigorosa concentricità geometrica (R_inner = R_outer - Padding: 24dp - 12dp = 12dp).
 */
@Composable
fun RecentSearchBubbleOverlay(
    bubble: ActiveSearchBubble,
    hazeState: HazeState?,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    var isCopied by remember { mutableStateOf(false) }

    val bubbleScale = remember { Animatable(0f) }
    val bubbleAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        bubbleScale.snapTo(0f)
        bubbleAlpha.snapTo(0f)
        launch {
            bubbleAlpha.animateTo(1f, tween(100, easing = LinearOutSlowInEasing))
        }
        bubbleScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.60f, // Molla scattante e rimbalzante come MovieActionsPopup
                stiffness = 400f
            )
        )
    }

    BackHandler(onBack = onDismiss)

    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
    val sideMarginPx = with(density) { 16.dp.toPx() }
    val bubbleWidthDp = minOf(330.dp, (config.screenWidthDp - 32).dp)
    val bubbleWidthPx = with(density) { bubbleWidthDp.toPx() }

    // Centratura precisa sulla pillola premuta
    val anchor = bubble.anchorBounds
    val anchorCenterX = if (anchor != Rect.Zero && anchor.width > 0f) {
        anchor.left + anchor.width / 2f
    } else {
        screenWidthPx / 2f
    }
    val bubbleX = (anchorCenterX - bubbleWidthPx / 2f).coerceIn(
        sideMarginPx,
        screenWidthPx - bubbleWidthPx - sideMarginPx
    )

    // Posizionamento verticale sopra o sotto la pillola
    val estimatedHeightPx = with(density) { 130.dp.toPx() }
    val verticalGapPx = with(density) { 12.dp.toPx() }
    val minTopSafetyPx = with(density) { 68.dp.toPx() }
    val isAbove = (anchor.top - estimatedHeightPx - verticalGapPx) >= minTopSafetyPx
    val bubbleY = if (isAbove) {
        (anchor.top - estimatedHeightPx - verticalGapPx).coerceAtLeast(minTopSafetyPx)
    } else {
        (anchor.bottom + verticalGapPx).coerceAtMost(screenHeightPx - estimatedHeightPx - with(density) { 48.dp.toPx() })
    }

    val pivotX = ((anchorCenterX - bubbleX) / bubbleWidthPx).coerceIn(0.1f, 0.9f)
    val pivotY = if (isAbove) 1f else 0f

    // Geometria concentrica rigorosa: R_esterno = 24dp, Padding = 12dp -> R_interno = 12dp!
    val outerShape = RoundedCornerShape(24.dp)
    val buttonShape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(9999f)
    ) {
        // Scrim di dismiss a tutto schermo: tocco ovunque fuori chiude istantaneamente
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        // Bolla Haze Glass con vero sfocamento dello sfondo
        Box(
            modifier = Modifier
                .absoluteOffset { IntOffset(bubbleX.roundToInt(), bubbleY.roundToInt()) }
                .width(bubbleWidthDp)
                .graphicsLayer {
                    scaleX = bubbleScale.value
                    scaleY = bubbleScale.value
                    transformOrigin = TransformOrigin(pivotX, pivotY)
                    alpha = bubbleAlpha.value
                }
                .clip(outerShape)
                .hazeGlass(
                    state = hazeState,
                    shape = outerShape,
                    containerColor = Color(0xFF0F141C).copy(alpha = 0.52f),
                    borderColor = Color.White.copy(alpha = 0.14f),
                    borderWidth = 1.dp,
                    blurRadius = 24.dp
                )
                .padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Testo della query completa
                Text(
                    text = bubble.query,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        lineHeight = 18.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Action Bar: 3 pulsanti concentrici con raggio 12dp e padding 12dp dal bordo 24dp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Copia
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .bounceClick(scaleDown = 0.90f) {
                                clipboardManager.setText(AnnotatedString(bubble.query))
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isCopied = true
                            }
                            .clip(buttonShape)
                            .background(if (isCopied) Color(0xFF34C759).copy(alpha = 0.16f) else Color.White.copy(alpha = 0.08f))
                            .border(
                                width = 1.dp,
                                color = if (isCopied) Color(0xFF34C759).copy(alpha = 0.40f) else Color.White.copy(alpha = 0.10f),
                                shape = buttonShape
                            )
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = if (isCopied) R.drawable.ic_tick else R.drawable.ic_copy),
                            contentDescription = stringResource(if (isCopied) R.string.folder_invite_copied else android.R.string.copy),
                            modifier = Modifier.size(13.dp),
                            tint = if (isCopied) Color(0xFF34C759) else Color.White.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isCopied) stringResource(R.string.folder_invite_copied) else stringResource(android.R.string.copy),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCopied) Color(0xFF34C759) else Color.White.copy(alpha = 0.85f),
                            maxLines = 1
                        )
                    }

                    // 2. Elimina
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .bounceClick(scaleDown = 0.90f) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDismiss()
                                bubble.onDelete()
                            }
                            .clip(buttonShape)
                            .background(Color(0xFFE57373).copy(alpha = 0.12f))
                            .border(
                                width = 1.dp,
                                color = Color(0xFFE57373).copy(alpha = 0.28f),
                                shape = buttonShape
                            )
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_trash),
                            contentDescription = stringResource(R.string.quick_action_delete),
                            modifier = Modifier.size(13.dp),
                            tint = Color(0xFFE57373)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.quick_action_delete),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE57373),
                            maxLines = 1
                        )
                    }

                    // 3. Cerca
                    Row(
                        modifier = Modifier
                            .weight(1.05f)
                            .height(38.dp)
                            .bounceClick(scaleDown = 0.92f) {
                                onDismiss()
                                bubble.onSearch()
                            }
                            .clip(buttonShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                                shape = buttonShape
                            )
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = if (bubble.isMood) R.drawable.ic_sparkle else R.drawable.ic_lente),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.shortcut_search_short),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
