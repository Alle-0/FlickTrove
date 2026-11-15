package com.cinetrack.ui.components.card

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState

/**
 * Badge pillola compatto per visualizzare formato (4K, HDR), rating o lingua sui poster.
 */
@Composable
fun MovieCardBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    isLarge: Boolean = false
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(50))
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(50))
            .border(0.5.dp, color.copy(alpha = 0.8f), RoundedCornerShape(50))
            .padding(horizontal = if (isLarge) 8.dp else 6.dp, vertical = if (isLarge) 4.dp else 2.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = if (isLarge) 9.5.sp else 7.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
        )
    }
}
