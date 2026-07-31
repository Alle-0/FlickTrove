package com.cinetrack.ui.components.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import com.cinetrack.ui.components.glass.glassmorphic
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.HazeStyles
import dev.chrisbanes.haze.HazeState

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
    content: @Composable ColumnScope.() -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val config = androidx.compose.ui.platform.LocalConfiguration.current

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
                        .padding(top = 12.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.24f))
                    )
                }

                content()
            }
            }
        }
    }
}
