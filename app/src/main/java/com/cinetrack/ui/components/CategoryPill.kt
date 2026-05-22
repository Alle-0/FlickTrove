package com.cinetrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.ui.theme.HazeStyles

@Composable
fun CategoryPill(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val accentColor = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        color = if (isSelected) accentColor else Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, if (isSelected) accentColor else HazeStyles.GlassBorderColor.copy(alpha = HazeStyles.GlassBorderAlphaTop)),
        modifier = Modifier
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.95f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }
}
