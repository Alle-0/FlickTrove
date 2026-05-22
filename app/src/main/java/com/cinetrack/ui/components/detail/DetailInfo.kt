package com.cinetrack.ui.components.detail

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.ui.utils.bounceClick

/**
 * DetailInfo
 * Renders the movie plot/overview with premium typography and expandable toggle.
 */
@Composable
fun DetailInfo(
    overview: String?,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (overview.isNullOrBlank()) return

    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var hasOverflow by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Text(
            text = "TRAMA",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            ),
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Column(
            modifier = Modifier
                .bounceClick(scaleDown = 0.99f) { if (hasOverflow || isExpanded) isExpanded = !isExpanded }
        ) {
            AnimatedContent(
                targetState = isExpanded,
                transitionSpec = {
                    fadeIn(animationSpec = tween(350)) togetherWith
                            fadeOut(animationSpec = tween(350)) using
                            SizeTransform(clip = true) { _, _ ->
                                spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            }
                },
                label = "PlotExpansion"
            ) { targetExpanded ->
                Text(
                    text = overview,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp,
                        letterSpacing = 0.2.sp,
                        fontSize = 15.sp
                    ),
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = if (targetExpanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { result ->
                        if (!targetExpanded && hasOverflow != result.hasVisualOverflow) {
                            hasOverflow = result.hasVisualOverflow
                        }
                    }
                )
            }

            if (hasOverflow || isExpanded) {
                Text(
                    text = if (isExpanded) "Leggi meno" else "Leggi tutto",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}
