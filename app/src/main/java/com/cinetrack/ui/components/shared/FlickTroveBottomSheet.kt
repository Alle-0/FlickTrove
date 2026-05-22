package com.cinetrack.ui.components.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cinetrack.ui.components.glass.glassmorphic
import com.cinetrack.ui.theme.HazeStyles

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
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.padding(top = 12.dp),
                color = Color.White.copy(alpha = 0.2f)
            )
        },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        // The Surface below provides the glassmorphic background
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic(
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    blurRadius = HazeStyles.GlassBlurRadius
                ),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding() // Safely handle system navigation
                    .padding(bottom = 24.dp) // Bottom spacing for aesthetics
            ) {
                content()
            }
        }
    }
}
