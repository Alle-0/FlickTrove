package com.cinetrack.ui.components.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.FilterPill

@Composable
fun SearchRecentSearchesRow(
    recentSearches: List<String>,
    query: String,
    onSearchClick: (String) -> Unit,
    onClearAll: () -> Unit,
    onDeleteSearch: (String) -> Unit
) {
    if (recentSearches.isNotEmpty() && query.isEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.search_recent), 
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), 
                fontSize = 10.sp, 
                fontWeight = FontWeight.Bold, 
                letterSpacing = 1.sp
            )
            Text(
                text = stringResource(R.string.search_clear_all),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.bounceClick { onClearAll() }.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp), modifier = Modifier.fillMaxWidth()) {
            items(recentSearches, key = { it }, contentType = { "recent_search" }) { search ->
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .bounceClick { onSearchClick(search) }
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(search, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.width(6.dp))
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides androidx.compose.ui.unit.Dp.Unspecified) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_x),
                                contentDescription = stringResource(R.string.search_remove_recent),
                                modifier = Modifier
                                    .bounceClick(scaleDown = 0.8f) { onDeleteSearch(search) }
                                    .size(18.dp)
                                    .padding(4.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SearchSuggestedFiltersRow(
    suggestedFilters: List<FilterPill>,
    query: String,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onFilterClick: (FilterPill) -> Unit
) {
    if (suggestedFilters.isNotEmpty() && query.isNotEmpty()) {
        val rotationAngle by animateFloatAsState(
            targetValue = if (isExpanded) 90f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "suggested_filters_chevron"
        )

        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .bounceClick { onToggleExpanded() }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.search_suggested), 
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), 
                fontSize = 10.sp, 
                fontWeight = FontWeight.Bold, 
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                contentDescription = if (isExpanded) stringResource(R.string.search_collapse) else stringResource(R.string.search_expand),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(12.dp)
                    .rotate(rotationAngle)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                items(suggestedFilters, key = { "${it.id}_${it.isKeyword}_${it.name}" }, contentType = { "suggested_filter" }) { filter ->
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .bounceClick { onFilterClick(filter) }
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.name, 
                            modifier = Modifier.padding(horizontal = 14.dp), 
                            color = MaterialTheme.colorScheme.primary, 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
