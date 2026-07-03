package com.cinetrack.ui.components.search

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.cinetrack.R
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun SearchCategorySelector(
    category: String,
    onCategoryChanged: (String) -> Unit
) {
    val selectedIndex = when (category) {
        "movie" -> 0
        "tv" -> 1
        "person" -> 2
        else -> 0
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .height(40.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
            shape = CircleShape
        ))
         
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            val tabWidth = maxWidth / 3
            val tabWidthPx = with(LocalDensity.current) { tabWidth.toPx() }
            
            val offsetAnimatable = remember { Animatable(selectedIndex * tabWidthPx) }
            val coroutineScope = rememberCoroutineScope()
            
            LaunchedEffect(selectedIndex) {
                if (!offsetAnimatable.isRunning) {
                    offsetAnimatable.animateTo(
                        targetValue = selectedIndex * tabWidthPx,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                    )
                }
            }
            // Tab stretch effect
            val velocity = offsetAnimatable.velocity
            val stretchFactor = 1f + (kotlin.math.abs(velocity) / 4000f).coerceAtMost(0.35f)
            val currentOffset = offsetAnimatable.value
            val extraWidth = (tabWidth * stretchFactor) - tabWidth
            val adjustedOffset = currentOffset - with(LocalDensity.current) { (extraWidth / 2).toPx() }
            
            Box(modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                val targetIndex = (offsetAnimatable.value / tabWidthPx).roundToInt().coerceIn(0, 2)
                                val categoryStr = when(targetIndex) {
                                    0 -> "movie"
                                    1 -> "tv"
                                    else -> "person"
                                }
                                onCategoryChanged(categoryStr)
                                
                                offsetAnimatable.animateTo(
                                    targetValue = targetIndex * tabWidthPx,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                                    initialVelocity = offsetAnimatable.velocity
                                )
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetAnimatable.animateTo(
                                    targetValue = selectedIndex * tabWidthPx,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                                )
                            }
                        },
                        onHorizontalDrag = { change: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: Float ->
                            change.consume()
                            coroutineScope.launch {
                                val newVal = (offsetAnimatable.value + dragAmount).coerceIn(0f, tabWidthPx * 2)
                                offsetAnimatable.snapTo(newVal)
                            }
                        }
                    )
                }
            ) {
                Box(modifier = Modifier
                    .offset { IntOffset(adjustedOffset.roundToInt(), 0) }
                    .width(tabWidth * stretchFactor)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                )
                
                Row(modifier = Modifier.fillMaxSize()) {
                    CategoryTab(stringResource(R.string.folder_detail_tab_movies), category == "movie") { onCategoryChanged("movie") }
                    CategoryTab(stringResource(R.string.folder_detail_tab_tv), category == "tv") { onCategoryChanged("tv") }
                    CategoryTab(stringResource(R.string.search_tab_persons), category == "person") { onCategoryChanged("person") }
                }
            }
        }
    }
}
