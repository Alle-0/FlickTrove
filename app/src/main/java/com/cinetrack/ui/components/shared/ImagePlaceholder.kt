package com.cinetrack.ui.components.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.ui.theme.DarkGrey
import com.cinetrack.ui.theme.DarkSurface

@Composable
fun ImagePlaceholder(
    title: String? = null,
    mediaType: String = "movie",
    modifier: Modifier = Modifier,
    isBackdrop: Boolean = false
) {
    val icon = when (mediaType) {
        "tv" -> Icons.AutoMirrored.Rounded.List
        "person" -> Icons.Rounded.Person
        else -> Icons.Rounded.PlayArrow
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = listOf(DarkSurface, DarkGrey, Color.Black))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(if (isBackdrop) 48.dp else 32.dp),
                tint = Color.White.copy(alpha = 0.15f)
            )
            if (title != null && !isBackdrop) {
                Text(
                    text = title.uppercase(),
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
