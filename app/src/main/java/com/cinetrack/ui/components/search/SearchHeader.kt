package com.cinetrack.ui.components.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.cinetrack.R
import com.cinetrack.data.model.KeywordDictionary
import com.cinetrack.data.model.SortConfig
import com.cinetrack.data.model.UserPreferences
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.shared.layoutToggleIcon
import com.cinetrack.ui.components.shared.nextGridColumns
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.FilterPill
import dev.chrisbanes.haze.HazeState
import java.util.Locale

@Composable
fun SearchHeader(
    query: String,
    category: String,
    sortConfig: SortConfig,
    preferences: UserPreferences,
    recentSearches: List<String>,
    suggestedFilters: List<FilterPill>,
    initialGenreName: String?,
    initialKeywordName: String?,
    textFieldValue: TextFieldValue,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    onBackClick: () -> Unit,
    onLayoutToggleClick: (Int) -> Unit,
    onFilterClick: () -> Unit,
    onCategoryChanged: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    onDeleteRecentSearch: (String) -> Unit,
    onToggleSuggestionsExpanded: () -> Unit,
    onSuggestedFilterClick: (FilterPill) -> Unit,
    focusRequester: FocusRequester,
    keyboardController: SoftwareKeyboardController?,
    hazeState: HazeState,
    onHeaderHeightMeasured: (Dp) -> Unit,
    onFilterBoundsMeasured: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .zIndex(10f)
            .onGloballyPositioned { layoutCoordinates ->
                onHeaderHeightMeasured(with(density) { layoutCoordinates.size.height.toDp() })
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .hazeGlass(
                    state = hazeState,
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                    borderWidth = 0.dp
                )
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.displayCutout)
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            // Row 1: Back, Search, Layout Toggle, Filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(44.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .bounceClick { onBackClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_left),
                            contentDescription = stringResource(R.string.detail_content_desc_back),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier.weight(1f).height(44.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_lente),
                            contentDescription = stringResource(R.string.search_content_desc),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        val primaryColor = MaterialTheme.colorScheme.primary
                        CompositionLocalProvider(
                            LocalTextSelectionColors provides TextSelectionColors(
                                handleColor = primaryColor,
                                backgroundColor = primaryColor.copy(alpha = 0.4f)
                            )
                        ) {
                            BasicTextField(
                                value = textFieldValue,
                                onValueChange = {
                                    onTextFieldValueChange(it)
                                    onQueryChanged(it.text)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { onQueryChanged(query) }),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (query.isEmpty()) {
                                            val hasLayoutButton = preferences.showLayoutToggle && category != "person"
                                            val placeholderText = when {
                                                sortConfig.selectedGenres.isNotEmpty() -> {
                                                    val gid = sortConfig.selectedGenres.first()
                                                    val name = suggestedFilters.find { it.id == gid && !it.isKeyword }?.name
                                                        ?: initialGenreName
                                                        ?: stringResource(R.string.search_fallback_genre)
                                                    stringResource(R.string.search_active_genre_format, name)
                                                }
                                                sortConfig.selectedKeywords.isNotEmpty() -> {
                                                    val kid = sortConfig.selectedKeywords.first()
                                                    val dictName = KeywordDictionary.getLocalizedKeywordName(kid, preferences.contentLanguage)
                                                        ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                                                    val name = suggestedFilters.find { it.id == kid }?.name
                                                        ?: dictName
                                                        ?: initialKeywordName
                                                        ?: stringResource(R.string.search_fallback_keyword)
                                                    stringResource(R.string.search_active_keyword_format, name)
                                                }
                                                else -> stringResource(R.string.search_placeholder)
                                            }
                                            val hasActiveFilter = sortConfig.selectedGenres.isNotEmpty() || sortConfig.selectedKeywords.isNotEmpty()
                                            Text(
                                                text = placeholderText,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (hasActiveFilter) 0.6f else 0.3f),
                                                fontSize = if (hasLayoutButton) 12.sp else 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                        if (query.isNotEmpty()) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_x),
                                contentDescription = stringResource(R.string.search_content_desc_clear),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .size(14.dp)
                                    .bounceClick(scaleDown = 0.8f) { onClearQuery() }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                val hasActiveFilters = sortConfig.selectedGenres.isNotEmpty() ||
                        sortConfig.selectedKeywords.isNotEmpty() ||
                        sortConfig.selectedProviders.isNotEmpty() ||
                        sortConfig.selectedDecades.isNotEmpty() ||
                        sortConfig.sortType != "popularity"

                if (preferences.showLayoutToggle && category != "person") {
                    Box(modifier = Modifier.size(44.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .bounceClick(scaleDown = 0.92f) {
                                    val nextColumns = nextGridColumns(preferences.gridColumns)
                                    onLayoutToggleClick(nextColumns)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = layoutToggleIcon(preferences.gridColumns),
                                contentDescription = stringResource(R.string.search_content_desc_layout),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .onGloballyPositioned { layoutCoordinates ->
                            val position = layoutCoordinates.positionInWindow()
                            onFilterBoundsMeasured(
                                Rect(
                                    position.x,
                                    position.y,
                                    position.x + layoutCoordinates.size.width,
                                    position.y + layoutCoordinates.size.height
                                )
                            )
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape)
                            .then(
                                if (hasActiveFilters) Modifier.border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary), CircleShape)
                                else Modifier
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .bounceClick {
                                keyboardController?.hide()
                                onFilterClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_filtri),
                            contentDescription = stringResource(R.string.folder_detail_filters),
                            tint = if (hasActiveFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (sortConfig.selectedKeywords.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.search_keyword_filter_warning),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 24.dp),
                    fontSize = 10.sp,
                    lineHeight = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SearchRecentSearchesRow(
                recentSearches = recentSearches,
                query = query,
                onSearchClick = { onQueryChanged(it) },
                onClearAll = { onClearRecentSearches() },
                onDeleteSearch = { onDeleteRecentSearch(it) }
            )

            SearchSuggestedFiltersRow(
                suggestedFilters = suggestedFilters,
                query = query,
                isExpanded = preferences.isSearchSuggestionsExpanded,
                onToggleExpanded = { onToggleSuggestionsExpanded() },
                onFilterClick = { filter -> onSuggestedFilterClick(filter) }
            )

            SearchCategorySelector(
                category = category,
                onCategoryChanged = { onCategoryChanged(it) }
            )
        }
    }
}
