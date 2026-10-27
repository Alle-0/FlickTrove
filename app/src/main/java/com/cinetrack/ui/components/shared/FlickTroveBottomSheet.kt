package com.cinetrack.ui.components.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.cinetrack.ui.components.glass.glassmorphic
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.HazeStyles
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

/**
 * Base BottomSheet for FlickTrove that enforces glassmorphism and edge-to-edge behavior.
 * 
 * Performance: Uses the [glassmorphic] modifier which optimizes blur based on API level.
 * Edge-to-Edge: Sets WindowInsets to zero and applies navigationBarsPadding internally to
 * ensure the glass background extends behind the system navigation bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlickTroveBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    hazeState: HazeState? = null,
    dragOnlyFromHandle: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalContext provides context,
            androidx.compose.ui.platform.LocalConfiguration provides config
        ) {
            // The Surface below provides the glassmorphic background
            val shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (hazeState != null) Modifier.hazeGlass(state = hazeState, shape = shape)
                        else Modifier.glassmorphic(shape = shape, blurRadius = HazeStyles.GlassBlurRadius)
                    ),
                color = Color.Transparent
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding() // Safely handle system navigation
                        .padding(bottom = 24.dp) // Bottom spacing for aesthetics
                ) {
                    // Custom Drag Handle inside the surface
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .then(
                                if (dragOnlyFromHandle) {
                                    Modifier.pointerInput(Unit) {
                                        var totalDrag = 0f
                                        var isDismissing = false
                                        detectVerticalDragGestures(
                                            onDragStart = {
                                                totalDrag = 0f
                                            },
                                            onDragEnd = {
                                                if (!isDismissing && totalDrag > 30f) {
                                                    isDismissing = true
                                                    coroutineScope.launch {
                                                        sheetState.hide()
                                                        onDismissRequest()
                                                    }
                                                }
                                            },
                                            onDragCancel = {
                                                totalDrag = 0f
                                            },
                                            onVerticalDrag = { change, dragAmount ->
                                                totalDrag += dragAmount
                                                if (!isDismissing && (totalDrag > 45f || dragAmount > 12f)) {
                                                    isDismissing = true
                                                    change.consume()
                                                    coroutineScope.launch {
                                                        sheetState.hide()
                                                        onDismissRequest()
                                                    }
                                                }
                                            }
                                        )
                                    }
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 36.dp, height = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.24f))
                        )
                    }

                    if (dragOnlyFromHandle) {
                        val blockNestedScroll = remember {
                            object : NestedScrollConnection {
                                override fun onPostScroll(
                                    consumed: Offset,
                                    available: Offset,
                                    source: NestedScrollSource
                                ): Offset {
                                    return if (available.y > 0f) Offset(0f, available.y) else Offset.Zero
                                }
                                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                                    return if (available.y > 0f) Velocity(0f, available.y) else Velocity.Zero
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .nestedScroll(blockNestedScroll)
                        ) {
                            Column {
                                content()
                            }
                        }
                    } else {
                        content()
                    }
                }
            }
        }
    }
}
