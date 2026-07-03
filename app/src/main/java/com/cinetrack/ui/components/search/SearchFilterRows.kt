package com.cinetrack.ui.components.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                        .bounceClick { onSearchClick(search) },
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onToggleExpanded() }
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
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) stringResource(R.string.search_collapse) else stringResource(R.string.search_expand),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
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
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                            .bounceClick { onFilterClick(filter) },
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
