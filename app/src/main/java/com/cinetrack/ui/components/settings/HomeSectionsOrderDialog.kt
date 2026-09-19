package com.cinetrack.ui.components.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import com.cinetrack.ui.components.common.FlickTroveSwitch
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import com.cinetrack.util.VibrationHelper
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.shared.ColorWheel
import com.cinetrack.ui.theme.*
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.premiumScrollbar
import com.cinetrack.ui.utils.verticalFadingEdges
import com.cinetrack.util.toComposeColor
import dev.chrisbanes.haze.HazeState
import com.cinetrack.ui.viewmodel.SettingsViewModel

private data class HomeSectionSettingRow(
    val key: String,
    val iconRes: Int,
    val titleRes: Int,
    val canToggle: Boolean,
    val checked: Boolean = true,
    val onCheckedChange: (Boolean) -> Unit = {}
)

@Composable
fun HomeSectionsOrderDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    settingsViewModel: com.cinetrack.ui.viewmodel.SettingsViewModel,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val showHomeContinueWatching by settingsViewModel.showHomeContinueWatching.collectAsStateWithLifecycle()
    val showHomeBoxOffice by settingsViewModel.showHomeBoxOffice.collectAsStateWithLifecycle()
    val showHomeWatchlist by settingsViewModel.showHomeWatchlist.collectAsStateWithLifecycle()
    val showHomeBecauseYouWatched by settingsViewModel.showHomeBecauseYouWatched.collectAsStateWithLifecycle()
    val homeSectionOrder by settingsViewModel.homeSectionOrder.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val vibrationEnabled by settingsViewModel.vibrationEnabled.collectAsStateWithLifecycle()

    var localOrder by remember(homeSectionOrder) { mutableStateOf(homeSectionOrder) }
    var draggedItemKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var dropTrigger by remember { mutableIntStateOf(0) }
    var itemHeightPx by remember { mutableFloatStateOf(0f) }
    var autoScrollSpeed by remember { mutableFloatStateOf(0f) }
    var pointerWindowY by remember { mutableFloatStateOf(0f) }
    var containerTopInWindow by remember { mutableFloatStateOf(0f) }
    var containerBottomInWindow by remember { mutableFloatStateOf(0f) }

    val configuration = LocalConfiguration.current
    val maxDialogHeight = (configuration.screenHeightDp.dp * 0.74f).coerceIn(460.dp, 580.dp)
    val listScrollState = rememberScrollState()

    // Auto-scroll while dragging near top or bottom edge of the dialog
    LaunchedEffect(draggedItemKey, autoScrollSpeed) {
        if (draggedItemKey != null && autoScrollSpeed != 0f) {
            while (isActive && draggedItemKey != null && autoScrollSpeed != 0f) {
                val consumed = listScrollState.scrollBy(autoScrollSpeed)
                if (consumed != 0f) {
                    dragOffset += consumed
                }
                kotlinx.coroutines.delay(16)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxDialogHeight)
            .padding(top = 22.dp, bottom = 18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.settings_home_feed_sections),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    stringResource(R.string.settings_home_feed_sections_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tasto Reset
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .bounceClick {
                            if (vibrationEnabled) {
                                VibrationHelper.vibrateClick(context)
                            }
                            localOrder = com.cinetrack.data.model.HomeFeedSectionConstants.DEFAULT_ORDER
                            dropTrigger++
                            settingsViewModel.resetHomeFeedSectionsToDefault()
                        }
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_ricarica),
                        contentDescription = stringResource(R.string.settings_home_feed_reset),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                // Tasto Chiudi
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .bounceClick { onDismiss() }
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_x),
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        val density = androidx.compose.ui.platform.LocalDensity.current
        val draggedIndex = localOrder.indexOf(draggedItemKey)
        val visualTargetIndex = remember(draggedIndex, dragOffset, itemHeightPx) {
            if (draggedIndex == -1 || itemHeightPx == 0f) -1
            else {
                val offsetSlots = (dragOffset / itemHeightPx).roundToInt()
                (draggedIndex + offsetSlots).coerceIn(0, localOrder.size - 1)
            }
        }

        var lastHapticIndex by remember { mutableIntStateOf(-1) }
        LaunchedEffect(visualTargetIndex, draggedItemKey) {
            if (draggedItemKey != null && visualTargetIndex != -1) {
                if (lastHapticIndex != -1 && lastHapticIndex != visualTargetIndex && vibrationEnabled) {
                    VibrationHelper.vibrateTick(context)
                }
                lastHapticIndex = visualTargetIndex
            } else {
                lastHapticIndex = -1
            }
        }

        val currentDraggedIndex by rememberUpdatedState(draggedIndex)
        val currentVisualTargetIndex by rememberUpdatedState(visualTargetIndex)
        val currentLocalOrder by rememberUpdatedState(localOrder)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .onGloballyPositioned { coordinates ->
                    val pos = coordinates.positionInWindow()
                    containerTopInWindow = pos.y
                    containerBottomInWindow = pos.y + coordinates.size.height.toFloat()
                }
                .verticalFadingEdges(
                    scrollState = listScrollState,
                    topEdgeHeight = 28.dp,
                    bottomEdgeHeight = 28.dp
                )
                .premiumScrollbar(listScrollState, width = 3f, paddingEnd = 6f)
                .verticalScroll(listScrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 4.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            localOrder.forEachIndexed { index, itemKey ->
                androidx.compose.runtime.key(itemKey) {
                    val isDragging = draggedItemKey == itemKey
                    val translationTarget = when {
                        isDragging -> dragOffset
                        draggedIndex != -1 && visualTargetIndex != -1 -> {
                            if (draggedIndex < index && index <= visualTargetIndex) {
                                -itemHeightPx
                            } else if (draggedIndex > index && index >= visualTargetIndex) {
                                itemHeightPx
                            } else {
                                0f
                            }
                        }
                        else -> 0f
                    }
                    val translation = remember(dropTrigger) { androidx.compose.animation.core.Animatable(0f) }

                    androidx.compose.runtime.LaunchedEffect(translationTarget) {
                        if (!isDragging) {
                            translation.animateTo(
                                targetValue = translationTarget,
                                animationSpec = androidx.compose.animation.core.spring(
                                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                                )
                            )
                        }
                    }

                    val finalTranslation = if (isDragging) dragOffset else translation.value

                    val rowInfo = when (itemKey) {
                        com.cinetrack.data.model.HomeFeedSectionConstants.CONTINUE_WATCHING -> HomeSectionSettingRow(
                            key = itemKey,
                            iconRes = R.drawable.ic_clessidra,
                            titleRes = R.string.home_section_continue_watching,
                            canToggle = true,
                            checked = showHomeContinueWatching,
                            onCheckedChange = { settingsViewModel.toggleShowHomeContinueWatching(it) }
                        )
                        com.cinetrack.data.model.HomeFeedSectionConstants.BOX_OFFICE -> HomeSectionSettingRow(
                            key = itemKey,
                            iconRes = R.drawable.ic_crown,
                            titleRes = R.string.box_office_title,
                            canToggle = true,
                            checked = showHomeBoxOffice,
                            onCheckedChange = { settingsViewModel.toggleShowHomeBoxOffice(it) }
                        )
                        com.cinetrack.data.model.HomeFeedSectionConstants.WATCHLIST -> HomeSectionSettingRow(
                            key = itemKey,
                            iconRes = R.drawable.ic_segnalibro,
                            titleRes = R.string.home_section_watchlist,
                            canToggle = true,
                            checked = showHomeWatchlist,
                            onCheckedChange = { settingsViewModel.toggleShowHomeWatchlist(it) }
                        )
                        com.cinetrack.data.model.HomeFeedSectionConstants.TROVE_PICK -> HomeSectionSettingRow(
                            key = itemKey,
                            iconRes = R.drawable.ic_sparkle,
                            titleRes = R.string.home_section_trove_pick,
                            canToggle = false
                        )
                        com.cinetrack.data.model.HomeFeedSectionConstants.BECAUSE_YOU_WATCHED -> HomeSectionSettingRow(
                            key = itemKey,
                            iconRes = R.drawable.ic_eye,
                            titleRes = R.string.home_section_because_you_watched_generic,
                            canToggle = true,
                            checked = showHomeBecauseYouWatched,
                            onCheckedChange = { settingsViewModel.toggleShowHomeBecauseYouWatched(it) }
                        )
                        com.cinetrack.data.model.HomeFeedSectionConstants.TOP_10 -> HomeSectionSettingRow(
                            key = itemKey,
                            iconRes = R.drawable.ic_trophy,
                            titleRes = R.string.home_section_top_10,
                            canToggle = false
                        )
                        com.cinetrack.data.model.HomeFeedSectionConstants.POPULAR -> HomeSectionSettingRow(
                            key = itemKey,
                            iconRes = R.drawable.ic_star,
                            titleRes = R.string.home_section_popular,
                            canToggle = false
                        )
                        com.cinetrack.data.model.HomeFeedSectionConstants.NOW_PLAYING -> HomeSectionSettingRow(
                            key = itemKey,
                            iconRes = R.drawable.ic_cinema,
                            titleRes = R.string.home_section_in_theaters_streaming,
                            canToggle = false
                        )
                        com.cinetrack.data.model.HomeFeedSectionConstants.UPCOMING -> HomeSectionSettingRow(
                            key = itemKey,
                            iconRes = R.drawable.ic_calendario,
                            titleRes = R.string.home_section_upcoming,
                            canToggle = false
                        )
                        com.cinetrack.data.model.HomeFeedSectionConstants.NEWS -> HomeSectionSettingRow(
                            key = itemKey,
                            iconRes = R.drawable.ic_documento,
                            titleRes = R.string.home_section_magazine,
                            canToggle = false
                        )
                        else -> HomeSectionSettingRow(
                            key = itemKey,
                            iconRes = R.drawable.ic_ciak,
                            titleRes = R.string.home_section_popular,
                            canToggle = false
                        )
                    }

                    var handleCoords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                if (itemHeightPx == 0f) {
                                    itemHeightPx = coordinates.size.height.toFloat() + with(density) { 10.dp.toPx() }
                                }
                            }
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer { translationY = finalTranslation }
                            .background(
                                if (isDragging) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f),
                                RoundedCornerShape(14.dp)
                            )
                            .border(
                                1.dp,
                                if (isDragging) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.06f),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(width = 34.dp, height = 44.dp)
                                .onGloballyPositioned { handleCoords = it }
                                .pointerInput(itemKey) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                                            draggedItemKey = itemKey
                                            val startY = (handleCoords?.positionInWindow()?.y ?: 0f) + offset.y
                                            pointerWindowY = startY
                                            autoScrollSpeed = 0f
                                        },
                                        onDragEnd = {
                                            autoScrollSpeed = 0f
                                            if (currentDraggedIndex != -1 && currentVisualTargetIndex != -1 && currentDraggedIndex != currentVisualTargetIndex) {
                                                if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                                                val newList = currentLocalOrder.toMutableList()
                                                val item = newList.removeAt(currentDraggedIndex)
                                                newList.add(currentVisualTargetIndex, item)
                                                localOrder = newList
                                                settingsViewModel.updateHomeSectionOrder(newList)
                                            }
                                            draggedItemKey = null
                                            dragOffset = 0f
                                            dropTrigger++
                                        },
                                        onDragCancel = {
                                            autoScrollSpeed = 0f
                                            draggedItemKey = null
                                            dragOffset = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffset += dragAmount.y
                                            pointerWindowY += dragAmount.y

                                            val thresholdPx = with(density) { 64.dp.toPx() }
                                            if (containerBottomInWindow > 0f && pointerWindowY > containerBottomInWindow - thresholdPx) {
                                                val overflow = (pointerWindowY - (containerBottomInWindow - thresholdPx)).coerceAtLeast(0f)
                                                val ratio = (overflow / thresholdPx).coerceIn(0.25f, 2.5f)
                                                autoScrollSpeed = with(density) { 12.dp.toPx() } * ratio
                                            } else if (containerTopInWindow > 0f && pointerWindowY < containerTopInWindow + thresholdPx) {
                                                val overflow = ((containerTopInWindow + thresholdPx) - pointerWindowY).coerceAtLeast(0f)
                                                val ratio = (overflow / thresholdPx).coerceIn(0.25f, 2.5f)
                                                autoScrollSpeed = -with(density) { 12.dp.toPx() } * ratio
                                            } else {
                                                autoScrollSpeed = 0f
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DragHandle,
                                contentDescription = "Drag to reorder",
                                tint = if (isDragging) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = rowInfo.iconRes),
                                contentDescription = null,
                                tint = if (rowInfo.canToggle && !rowInfo.checked) Color.White.copy(alpha = 0.35f) else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = stringResource(rowInfo.titleRes),
                            color = if (rowInfo.canToggle && !rowInfo.checked) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        if (rowInfo.canToggle) {
                            FlickTroveSwitch(
                                checked = rowInfo.checked,
                                onCheckedChange = {
                                    if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                                    rowInfo.onCheckedChange(it)
                                },
                                accentColor = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            // Badge Sempre attiva con icona lucchetto
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.06f), CircleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_lock),
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = stringResource(R.string.settings_section_always_on),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

