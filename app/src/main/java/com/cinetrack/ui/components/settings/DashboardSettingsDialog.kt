package com.cinetrack.ui.components.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinetrack.R
import com.cinetrack.ui.components.common.FlickTroveSwitch
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.SettingsViewModel
import com.cinetrack.util.VibrationHelper
import dev.chrisbanes.haze.HazeState
import kotlin.math.roundToInt

private data class DashboardSettingItem(
    val iconRes: Int,
    val titleRes: Int,
    val descRes: Int,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit
)

@Composable
fun DashboardSettingsDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val showMyFolders by settingsViewModel.showMyFolders.collectAsStateWithLifecycle()
    val showYourFlow by settingsViewModel.showYourFlow.collectAsStateWithLifecycle()
    val showGeneralStats by settingsViewModel.showGeneralStats.collectAsStateWithLifecycle()
    val dashboardCardOrder by settingsViewModel.dashboardCardOrder.collectAsStateWithLifecycle()
    val vibrationEnabled by settingsViewModel.vibrationEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var localOrder by remember(dashboardCardOrder) { mutableStateOf(dashboardCardOrder) }

    var draggedItemKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var dropTrigger by remember { mutableIntStateOf(0) }
    var itemHeightPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
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
            .padding(top = 22.dp, bottom = 18.dp)
    ) {
        // Header
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
                    text = stringResource(R.string.settings_ui_layout),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.settings_ui_layout_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                            val defaultOrder = listOf("stats", "folders", "flow")
                            localOrder = defaultOrder
                            dropTrigger++
                            settingsViewModel.updateDashboardCardOrder(defaultOrder)
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
                com.cinetrack.ui.components.shared.ModalCloseButton(
                    onClose = onDismiss
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                    val translation = remember(dropTrigger) { Animatable(0f) }

                    LaunchedEffect(translationTarget) {
                        if (!isDragging) {
                            translation.animateTo(
                                targetValue = translationTarget,
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                    }

                    val finalTranslation = if (isDragging) dragOffset else translation.value

                    val itemInfo = when (itemKey) {
                        "folders" -> DashboardSettingItem(
                            R.drawable.ic_cartella,
                            R.string.settings_show_my_folders,
                            R.string.settings_show_my_folders_desc,
                            showMyFolders
                        ) { settingsViewModel.toggleShowMyFolders(it) }
                        "flow" -> DashboardSettingItem(
                            R.drawable.ic_sparkle,
                            R.string.settings_show_your_flow,
                            R.string.settings_show_your_flow_desc,
                            showYourFlow
                        ) { settingsViewModel.toggleShowYourFlow(it) }
                        "stats" -> DashboardSettingItem(
                            R.drawable.ic_stat,
                            R.string.settings_show_general_stats,
                            R.string.settings_show_general_stats_desc,
                            showGeneralStats
                        ) { settingsViewModel.toggleShowGeneralStats(it) }
                        else -> DashboardSettingItem(
                            R.drawable.ic_stat,
                            R.string.settings_show_general_stats,
                            R.string.settings_show_general_stats_desc,
                            showGeneralStats
                        ) { settingsViewModel.toggleShowGeneralStats(it) }
                    }

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
                                .pointerInput(itemKey) {
                                    detectDragGestures(
                                        onDragStart = {
                                            if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                                            draggedItemKey = itemKey
                                        },
                                        onDragEnd = {
                                            if (currentDraggedIndex != -1 && currentVisualTargetIndex != -1 && currentDraggedIndex != currentVisualTargetIndex) {
                                                if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                                                val newList = currentLocalOrder.toMutableList()
                                                val item = newList.removeAt(currentDraggedIndex)
                                                newList.add(currentVisualTargetIndex, item)
                                                localOrder = newList
                                                settingsViewModel.updateDashboardCardOrder(newList)
                                            }
                                            draggedItemKey = null
                                            dragOffset = 0f
                                            dropTrigger++
                                        },
                                        onDragCancel = {
                                            draggedItemKey = null
                                            dragOffset = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffset += dragAmount.y
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
                                imageVector = ImageVector.vectorResource(id = itemInfo.iconRes),
                                contentDescription = null,
                                tint = if (!itemInfo.checked) Color.White.copy(alpha = 0.35f) else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(itemInfo.titleRes),
                                color = if (!itemInfo.checked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(itemInfo.descRes),
                                color = if (!itemInfo.checked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        FlickTroveSwitch(
                            checked = itemInfo.checked,
                            onCheckedChange = {
                                if (vibrationEnabled) VibrationHelper.vibrateTick(context)
                                itemInfo.onCheckedChange(it)
                            },
                            accentColor = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
