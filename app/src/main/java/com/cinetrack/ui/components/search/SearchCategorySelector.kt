package com.cinetrack.ui.components.search

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
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
    isMoodAiMode: Boolean = true,
    moodRemainingQuota: Int = 3,
    onMoodModeChanged: (Boolean) -> Unit = {},
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

    var previousCategory by remember { mutableStateOf("movie") }
    LaunchedEffect(category) {
        if (category != "mood") {
            previousCategory = category
        }
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
            // 1. Capsula Morphing: Si trasforma animata da [Movies, TV, People, Collections] a [AI • Quota, ALGO • ∞]
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

                AnimatedContent(
                    targetState = isMood,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                scaleIn(initialScale = 0.95f, animationSpec = tween(220)))
                            .togetherWith(
                                fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                                        scaleOut(targetScale = 0.95f, animationSpec = tween(180))
                            )
                    },
                    label = "category_morphing_capsule"
                ) { moodActive ->
                    if (moodActive) {
                        val effectiveAiMode = isMoodAiMode && moodRemainingQuota > 0
                        CategoryTabSelector(
                            options = listOf(
                                stringResource(R.string.search_mood_mode_ai_tab, moodRemainingQuota),
                                stringResource(R.string.search_mood_mode_local_tab)
                            ),
                            selectedIndex = if (effectiveAiMode) 0 else 1,
                            onOptionClick = { index ->
                                onMoodModeChanged(index == 0)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            fillWidth = true,
                            tabWidth = 80.dp,
                            fontSize = 10.5.sp,
                            uppercase = true,
                            enableDrag = moodRemainingQuota > 0
                        )
                    } else {
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
                            fontSize = 10.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 2. Icona Mood a Lato: Vero e proprio switch toggle
            Box(
                modifier = Modifier
                    .bounceClick(scaleDown = 0.90f) {
                        if (isMood) {
                            onCategoryChanged(previousCategory)
                        } else {
                            previousCategory = category
                            onCategoryChanged("mood")
                        }
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
