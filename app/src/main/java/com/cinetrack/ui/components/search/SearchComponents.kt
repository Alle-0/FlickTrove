package com.cinetrack.ui.components.search

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RowScope.CategoryTab(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1f, label = "tabScale")

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        val isLongText = text.length > 8
        Text(
            text = text,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = if (isLongText) (-0.3).sp else 0.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TrendingHeader(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
    )
}

@Composable
fun SearchNoResults(queryLength: Int) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.material3.Icon(
            imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(id = com.cinetrack.R.drawable.ic_lente), 
            contentDescription = null, 
            modifier = Modifier.size(64.dp), 
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(com.cinetrack.R.string.search_no_results), 
            color = MaterialTheme.colorScheme.onSurface, 
            fontSize = 20.sp, 
            fontWeight = FontWeight.Bold
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (queryLength in 1..2) androidx.compose.ui.res.stringResource(com.cinetrack.R.string.search_no_results_short) else androidx.compose.ui.res.stringResource(com.cinetrack.R.string.search_no_results_simplify), 
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), 
            fontSize = 14.sp, 
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
