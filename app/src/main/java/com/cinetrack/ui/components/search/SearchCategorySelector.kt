package com.cinetrack.ui.components.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.components.common.CategoryTabSelector
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.HazeStyles
import dev.chrisbanes.haze.HazeState

@Composable
fun SearchCategorySelector(
    category: String,
    onCategoryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val selectedIndex = when (category) {
        "movie" -> 0
        "tv" -> 1
        "person" -> 2
        "collection" -> 3
        else -> 0
    }

    val options = listOf(
        stringResource(R.string.folder_detail_tab_movies),
        stringResource(R.string.folder_detail_tab_tv),
        stringResource(R.string.search_tab_persons),
        stringResource(R.string.search_tab_collections)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.wrapContentSize(),
            contentAlignment = Alignment.Center
        ) {
            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .hazeGlass(
                        state = hazeState,
                        shape = CircleShape,
                        blurRadius = HazeStyles.SmallGlassBlurRadius,
                        useOffscreenStrategy = false,
                        borderWidth = 1.dp,
                        borderColor = HazeStyles.GlassBorderColor.copy(alpha = HazeStyles.GlassBorderAlphaTop)
                    )
            )

            CategoryTabSelector(
                options = options,
                selectedIndex = selectedIndex,
                onOptionClick = { index ->
                    val categoryStr = when (index) {
                        0 -> "movie"
                        1 -> "tv"
                        2 -> "person"
                        else -> "collection"
                    }
                    onCategoryChanged(categoryStr)
                },
                tabWidth = 100.dp,
                fontSize = 11.sp
            )
        }
    }
}
