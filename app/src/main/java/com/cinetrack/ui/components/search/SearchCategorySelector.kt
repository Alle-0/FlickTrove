package com.cinetrack.ui.components.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.components.common.CategoryTabSelector
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState

@Composable
fun SearchCategorySelector(
    category: String,
    onCategoryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val isMood = category == "mood"
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
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Capsula 4 Tab TMDB Standard (Movies, TV, People, Collections) che occupa tutto lo spazio rimanente
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp),
                contentAlignment = Alignment.Center
            ) {
                Spacer(
                    modifier = Modifier
                        .matchParentSize()
                        .hazeGlass(
                            state = hazeState,
                            shape = CircleShape
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
                    modifier = Modifier.fillMaxWidth(),
                    fillWidth = true,
                    tabWidth = 80.dp,
                    fontSize = 10.5.sp,
                    indicatorColor = if (isMood) Color.Transparent else null
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 2. Icona Mood a Lato (Opzione 1B: Zero Invasività)
            Box(
                modifier = Modifier
                    .bounceClick(scaleDown = 0.90f) {
                        onCategoryChanged("mood")
                    }
                    .size(34.dp)
                    .then(
                        if (hazeState != null) {
                            Modifier.hazeGlass(state = hazeState, shape = CircleShape)
                        } else {
                            Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape)
                        }
                    )
                    .then(
                        if (isMood) {
                            Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        } else {
                            Modifier.border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_sparkle),
                    contentDescription = stringResource(R.string.search_tab_mood),
                    tint = if (isMood) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.50f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
