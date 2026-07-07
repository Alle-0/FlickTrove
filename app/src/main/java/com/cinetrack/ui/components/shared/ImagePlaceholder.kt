package com.cinetrack.ui.components.shared

import com.cinetrack.R
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
        "tv" -> ImageVector.vectorResource(id = R.drawable.ic_lista)
        "person" -> ImageVector.vectorResource(id = R.drawable.ic_persona)
        else -> ImageVector.vectorResource(id = R.drawable.ic_play)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = listOf(Color(0xFF2E2E48), Color(0xFF1E1E32), Color(0xFF141422)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(if (isBackdrop) 48.dp else 32.dp),
                tint = Color.White.copy(alpha = 0.35f)
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
