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
    settingsViewModel: com.cinetrack.ui.viewmodel.SettingsViewModel,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val showMyFolders by settingsViewModel.showMyFolders.collectAsStateWithLifecycle()
    val showYourFlow by settingsViewModel.showYourFlow.collectAsStateWithLifecycle()
    val showGeneralStats by settingsViewModel.showGeneralStats.collectAsStateWithLifecycle()
    val dashboardCardOrder by settingsViewModel.dashboardCardOrder.collectAsStateWithLifecycle()
    
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.settings_ui_layout),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .bounceClick { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_x),
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                var localOrder by remember(dashboardCardOrder) { mutableStateOf(dashboardCardOrder) }
                
                var draggedItemKey by remember { mutableStateOf<String?>(null) }
                var dragOffset by remember { mutableStateOf(0f) }
                var dropTrigger by remember { mutableIntStateOf(0) }
                var itemHeightPx by remember { mutableStateOf(0f) }
                val density = androidx.compose.ui.platform.LocalDensity.current
                val draggedIndex = localOrder.indexOf(draggedItemKey)
                val visualTargetIndex = remember(draggedIndex, dragOffset, itemHeightPx) {
                    if (draggedIndex == -1 || itemHeightPx == 0f) -1
                    else {
                        val offsetSlots = (dragOffset / itemHeightPx).roundToInt()
                        (draggedIndex + offsetSlots).coerceIn(0, localOrder.size - 1)
                    }
                }
                
                val currentDraggedIndex by rememberUpdatedState(draggedIndex)
                val currentVisualTargetIndex by rememberUpdatedState(visualTargetIndex)
                val currentLocalOrder by rememberUpdatedState(localOrder)

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
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

                        val itemInfo = when (itemKey) {
                            "folders" -> DashboardSettingItem(R.drawable.ic_cartella, R.string.settings_show_my_folders, R.string.settings_show_my_folders_desc, showMyFolders) { settingsViewModel.toggleShowMyFolders(it) }
                            "flow" -> DashboardSettingItem(R.drawable.ic_sparkle, R.string.settings_show_your_flow, R.string.settings_show_your_flow_desc, showYourFlow) { settingsViewModel.toggleShowYourFlow(it) }
                            "stats" -> DashboardSettingItem(R.drawable.ic_stat, R.string.settings_show_general_stats, R.string.settings_show_general_stats_desc, showGeneralStats) { settingsViewModel.toggleShowGeneralStats(it) }
                            else -> DashboardSettingItem(R.drawable.ic_stat, R.string.settings_show_general_stats, R.string.settings_show_general_stats_desc, showGeneralStats) { settingsViewModel.toggleShowGeneralStats(it) }
                        }

                         Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    if (itemHeightPx == 0f) {
                                        itemHeightPx = coordinates.size.height.toFloat() + with(density) { 16.dp.toPx() }
                                    }
                                }
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer { translationY = finalTranslation }
                                .background(if (isDragging) Color.White.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DragHandle,
                                contentDescription = "Drag to reorder",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(24.dp)
                                    .pointerInput(itemKey) {
                                        detectDragGestures(
                                            onDragStart = { draggedItemKey = itemKey },
                                            onDragEnd = { 
                                                if (currentDraggedIndex != -1 && currentVisualTargetIndex != -1 && currentDraggedIndex != currentVisualTargetIndex) {
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
                                    }
                            )

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(id = itemInfo.iconRes),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(itemInfo.titleRes), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                Text(stringResource(itemInfo.descRes), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
                            }
                            FlickTroveSwitch(
                                checked = itemInfo.checked,
                                onCheckedChange = itemInfo.onCheckedChange,
                                accentColor = MaterialTheme.colorScheme.primary
                            )
                        }
                        } // End key(itemKey)
            }
        }
    }
}

data class HomeSectionSettingRow(
    val key: String,
    val iconRes: Int,
    val titleRes: Int,
    val canToggle: Boolean,
    val checked: Boolean = true,
    val onCheckedChange: (Boolean) -> Unit = {}
)

