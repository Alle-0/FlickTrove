package com.cinetrack.ui.components.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SkeletonGrid(
    paddingTop: Dp = 110.dp,
    paddingBottom: Dp = 120.dp,
    paddingHorizontal: Dp = 20.dp,
    itemSpacing: Dp = 12.dp,
    columns: Int = 3
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0E))
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(
                start = paddingHorizontal,
                end = paddingHorizontal,
                top = paddingTop,
                bottom = paddingBottom
            ),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false // Skeletons usually don't need to scroll themselves
        ) {
            items(count = 15, contentType = { "skeleton" }) {
                MovieCardSkeleton(width = 100.dp) // Width is handled by grid cells
            }
        }
    }
}
