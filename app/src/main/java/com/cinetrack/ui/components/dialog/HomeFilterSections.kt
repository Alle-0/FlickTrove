package com.cinetrack.ui.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.model.GenreConstants
import com.cinetrack.data.model.SortConfig
import com.cinetrack.ui.utils.ProviderConstants
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.horizontalFadingEdges

@Composable
fun FilterSortSection(
    category: String,
    isCollectionFilter: Boolean,
    isCommentsFilter: Boolean,
    isVisti: Boolean,
    isFolder: Boolean,
    sortConfig: SortConfig,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onInfoClick: () -> Unit,
    onSortConfigChanged: (SortConfig) -> Unit
) {
    ExpandableSection(
        title = stringResource(R.string.filter_sort_by),
        isExpanded = isExpanded,
        showChevron = true,
        isClickable = true,
        onToggle = onToggle
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isDiscoverCategory = category.contains("popular") ||
                category.contains("upcoming") ||
                category.contains("now_playing") ||
                category.contains("airing") ||
                category.contains("trending") ||
                category.contains("streaming") ||
                category.contains("on_the_air") ||
                category == "genre"

            val sortOptions = buildList {
                if (isCollectionFilter) {
                    add(FilterOption("chronological", stringResource(R.string.collection_filter_sort_chronological)))
                    add(FilterOption("release_date", stringResource(R.string.filter_sort_release_date)))
                } else if (isCommentsFilter) {
                    add(FilterOption("date", stringResource(R.string.comment_sort_date)))
                    add(FilterOption("likes", stringResource(R.string.comment_sort_likes)))
                } else if (isDiscoverCategory) {
                    if (category.contains("upcoming")) {
                        add(FilterOption("release_date", stringResource(R.string.filter_sort_release_date)))
                        add(FilterOption("created_at", stringResource(R.string.person_popularity).lowercase().replaceFirstChar { it.uppercase() }))
                    } else {
                        add(FilterOption("created_at", stringResource(R.string.person_popularity).lowercase().replaceFirstChar { it.uppercase() }))
                        add(FilterOption("release_date", stringResource(R.string.filter_sort_release_date)))
                        add(FilterOption("personal_rating", stringResource(R.string.detail_rating)))
                        add(FilterOption("title", stringResource(R.string.filter_sort_title)))
                    }
                } else {
                    if (isVisti) {
                        add(FilterOption("last_watched_at", stringResource(R.string.filter_sort_last_watched_at)))
                        add(FilterOption("first_watched_at", stringResource(R.string.filter_sort_first_watched_at)))
                        add(FilterOption("rewatch_count", stringResource(R.string.filter_sort_rewatch_count)))
                    }
                    if (isFolder) {
                        add(FilterOption("manual", stringResource(R.string.filter_sort_manual)))
                    }
                    add(FilterOption("added_at", stringResource(R.string.filter_sort_added_at)))
                    add(FilterOption("release_date", stringResource(R.string.filter_sort_release_date)))
                    add(FilterOption("title", stringResource(R.string.filter_sort_title)))
                    add(FilterOption("personal_rating", stringResource(R.string.filter_sort_personal_rating)))
                    add(FilterOption("runtime", stringResource(R.string.filter_sort_runtime)))
                    if (category != "movie") {
                        add(FilterOption("series_status", stringResource(R.string.filter_sort_series_status)))
                        add(FilterOption("remaining_episodes", stringResource(R.string.filter_sort_remaining_episodes)))
                    }
                }
            }

            sortOptions.forEach { option ->
                SortOptionItem(
                    label = option.label,
                    isSelected = sortConfig.sortType == option.id,
                    onInfoClick = if (option.id == "series_status") onInfoClick else null,
                    onClick = {
                        val newDirection = if (sortConfig.sortType != option.id && (option.id == "series_status" || option.id == "title")) "asc" else sortConfig.sortDirection
                        onSortConfigChanged(sortConfig.copy(sortType = option.id, sortDirection = newDirection))
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DirectionChip(
                    label = stringResource(R.string.filter_dir_desc),
                    isSelected = sortConfig.sortDirection == "desc",
                    icon = ImageVector.vectorResource(id = R.drawable.ic_right),
                    iconRotation = 90f,
                    modifier = Modifier.weight(1f),
                    onClick = { onSortConfigChanged(sortConfig.copy(sortDirection = "desc")) }
                )
                DirectionChip(
                    label = stringResource(R.string.filter_dir_asc),
                    isSelected = sortConfig.sortDirection == "asc",
                    icon = ImageVector.vectorResource(id = R.drawable.ic_right),
                    iconRotation = -90f,
                    modifier = Modifier.weight(1f),
                    onClick = { onSortConfigChanged(sortConfig.copy(sortDirection = "asc")) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterCommentsSourceSection(
    availableSources: List<com.cinetrack.data.api.SourceCatalogRow>,
    sourceCounts: Map<String, Int>,
    sortConfig: SortConfig,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSortConfigChanged: (SortConfig) -> Unit
) {
    val activeSources = remember(sortConfig.selectedSources, sortConfig.selectedSource) {
        (sortConfig.selectedSources + listOfNotNull(sortConfig.selectedSource)).toSet()
    }
    ExpandableSection(
        title = stringResource(R.string.filter_source),
        isExpanded = isExpanded,
        showChevron = true,
        isClickable = true,
        badgeCount = activeSources.size,
        onToggle = onToggle
    ) {
        val sources = remember(availableSources, sourceCounts, activeSources) {
            val rawList = if (availableSources.isNotEmpty()) {
                availableSources
            } else {
                listOf(
                    com.cinetrack.data.api.SourceCatalogRow(slug = "flicktrove", displayName = "FlickTrove"),
                    com.cinetrack.data.api.SourceCatalogRow(slug = "tvtime", displayName = "TV Time Refugees")
                )
            }
            val filteredList = rawList.filter {
                !it.slug.equals("commsuni.tv", ignoreCase = true) &&
                !it.slug.equals("commsuni", ignoreCase = true) &&
                !(it.displayName?.equals("commsuni.tv", ignoreCase = true) == true)
            }
            val flickTroveItem = filteredList.firstOrNull { it.slug.equals("flicktrove", ignoreCase = true) }
                ?: com.cinetrack.data.api.SourceCatalogRow(slug = "flicktrove", displayName = "FlickTrove")
            val otherItems = filteredList
                .filter { !it.slug.equals("flicktrove", ignoreCase = true) }
                .sortedBy { (it.displayName?.ifBlank { it.slug } ?: it.slug).lowercase() }
            val candidateSources = listOf(flickTroveItem) + otherItems

            if (sourceCounts.isNotEmpty()) {
                candidateSources.filter { source ->
                    val count = sourceCounts[source.slug.lowercase().trim()] ?: sourceCounts[source.slug] ?: 0
                    count > 0 || source.slug in activeSources
                }
            } else {
                candidateSources
            }
        }
        FlowRow(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val totalSourceComments = remember(sourceCounts) {
                if (sourceCounts.isNotEmpty()) sourceCounts.values.sum() else null
            }
            FilterChip(
                label = stringResource(R.string.filter_source_all),
                isSelected = activeSources.isEmpty(),
                onClick = { onSortConfigChanged(sortConfig.copy(selectedSources = emptyList(), selectedSource = null)) },
                count = totalSourceComments
            )
            sources.forEach { source ->
                val isSelected = source.slug in activeSources
                val count = sourceCounts[source.slug.lowercase().trim()] ?: sourceCounts[source.slug]
                FilterChip(
                    label = source.displayName?.ifBlank { source.slug } ?: source.slug,
                    isSelected = isSelected,
                    onClick = {
                        val newSet = if (isSelected) activeSources - source.slug else activeSources + source.slug
                        onSortConfigChanged(
                            sortConfig.copy(
                                selectedSources = newSet.toList(),
                                selectedSource = null
                            )
                        )
                    },
                    count = count
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterCommentsLanguageSection(
    availableLanguages: List<String>,
    languageCounts: Map<String, Int>,
    sortConfig: SortConfig,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSortConfigChanged: (SortConfig) -> Unit
) {
    val configuration = LocalConfiguration.current
    val activeLanguages = remember(sortConfig.selectedLanguages, sortConfig.selectedLanguage) {
        (sortConfig.selectedLanguages + listOfNotNull(sortConfig.selectedLanguage)).toSet()
    }
    ExpandableSection(
        title = stringResource(R.string.filter_language),
        isExpanded = isExpanded,
        showChevron = true,
        isClickable = true,
        badgeCount = activeLanguages.size,
        onToggle = onToggle
    ) {
        val currentLocale = if (!configuration.locales.isEmpty) configuration.locales[0] else java.util.Locale.getDefault()
        val visibleLanguageCodes = remember(languageCounts, availableLanguages, activeLanguages) {
            if (languageCounts.isNotEmpty()) {
                val positiveCodes = languageCounts.filter { it.value > 0 }.keys
                (positiveCodes + activeLanguages).map { it.lowercase().trim() }.filter { it.isNotBlank() }.distinct()
            } else {
                availableLanguages.map { it.lowercase().trim() }.filter { it.isNotBlank() }.distinct()
            }
        }
        val allLabel = stringResource(R.string.flow_filter_media_all)
        val languages = remember(currentLocale, visibleLanguageCodes, allLabel) {
            val userLangCode = currentLocale.language.lowercase()
            val mapped = visibleLanguageCodes.map { code ->
                val loc = java.util.Locale.forLanguageTag(code)
                val rawName = loc.getDisplayLanguage(currentLocale)
                val displayName = rawName.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(currentLocale) else it.toString()
                }.ifBlank { code.uppercase() }
                code to displayName
            }
            val userLangPair = mapped.firstOrNull { it.first == userLangCode }
            val otherLangs = mapped
                .filter { it.first != userLangCode }
                .sortedBy { it.second.lowercase(currentLocale) }
            if (userLangPair != null) {
                listOf(userLangPair) + otherLangs
            } else {
                otherLangs
            }
        }
        FlowRow(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val totalLanguageComments = remember(languageCounts) {
                if (languageCounts.isNotEmpty()) languageCounts.values.sum() else null
            }
            FilterChip(
                label = allLabel,
                isSelected = activeLanguages.isEmpty(),
                onClick = { onSortConfigChanged(sortConfig.copy(selectedLanguages = emptyList(), selectedLanguage = null)) },
                count = totalLanguageComments
            )
            languages.forEach { (code, label) ->
                val isSelected = code in activeLanguages
                val count = languageCounts[code]
                FilterChip(
                    label = label,
                    isSelected = isSelected,
                    onClick = {
                        val newSet = if (isSelected) activeLanguages - code else activeLanguages + code
                        onSortConfigChanged(
                            sortConfig.copy(
                                selectedLanguages = newSet.toList(),
                                selectedLanguage = null
                            )
                        )
                    },
                    count = count
                )
            }
        }
    }
}

@Composable
fun FilterMediaTypeSection(
    sortConfig: SortConfig,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSortConfigChanged: (SortConfig) -> Unit
) {
    ExpandableSection(
        title = stringResource(R.string.flow_filter_section_media),
        isExpanded = isExpanded,
        showChevron = true,
        isClickable = true,
        badgeCount = if (sortConfig.selectedMedia != null) 1 else 0,
        onToggle = onToggle
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SortOptionItem(
                label = stringResource(R.string.flow_filter_media_all),
                isSelected = sortConfig.selectedMedia == null,
                onClick = { onSortConfigChanged(sortConfig.copy(selectedMedia = null)) }
            )
            SortOptionItem(
                label = stringResource(R.string.flow_filter_media_movies),
                isSelected = sortConfig.selectedMedia == "movie",
                onClick = { onSortConfigChanged(sortConfig.copy(selectedMedia = "movie")) }
            )
            SortOptionItem(
                label = stringResource(R.string.flow_filter_media_tv),
                isSelected = sortConfig.selectedMedia == "tv",
                onClick = { onSortConfigChanged(sortConfig.copy(selectedMedia = "tv")) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterTvStatusSection(
    isVisti: Boolean,
    sortConfig: SortConfig,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSortConfigChanged: (SortConfig) -> Unit
) {
    ExpandableSection(
        title = stringResource(R.string.filter_status),
        isExpanded = isExpanded,
        badgeCount = sortConfig.selectedStatuses.size,
        onToggle = onToggle
    ) {
        val statuses = if (!isVisti) {
            listOf(
                "watching" to stringResource(R.string.filter_status_watching),
                "not_started" to stringResource(R.string.filter_status_not_started),
                "dropped" to stringResource(R.string.filter_status_dropped)
            )
        } else {
            listOf(
                "up_to_date" to stringResource(R.string.filter_status_up_to_date)
            )
        }

        FlowRow(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            statuses.forEach { (statusId, label) ->
                val isSelected = statusId in sortConfig.selectedStatuses
                FilterChip(
                    label = label.uppercase(),
                    isSelected = isSelected,
                    onClick = {
                        val newList = if (isSelected) sortConfig.selectedStatuses - statusId
                        else sortConfig.selectedStatuses + statusId
                        onSortConfigChanged(sortConfig.copy(selectedStatuses = newList))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterKeywordsSection(
    suggestedFilters: List<com.cinetrack.ui.viewmodel.FilterPill>,
    initialKeywordName: String?,
    sortConfig: SortConfig,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSortConfigChanged: (SortConfig) -> Unit
) {
    ExpandableSection(
        title = stringResource(R.string.filter_active_subgenres),
        isExpanded = isExpanded,
        badgeCount = sortConfig.selectedKeywords.size,
        onToggle = onToggle
    ) {
        FlowRow(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sortConfig.selectedKeywords.forEach { keywordId ->
                val currentLanguage = LocalConfiguration.current.locales[0]?.language ?: "en"
                val dictName = com.cinetrack.data.model.KeywordDictionary.getLocalizedKeywordName(keywordId, currentLanguage)?.uppercase()
                val suggestedName = suggestedFilters.find { it.id == keywordId }?.name?.uppercase()
                val keywordName = suggestedName ?: dictName ?: initialKeywordName?.uppercase() ?: stringResource(R.string.filter_selected_subgenre)

                FilterChip(
                    label = keywordName,
                    isSelected = true,
                    onClick = {
                        onSortConfigChanged(sortConfig.copy(selectedKeywords = sortConfig.selectedKeywords - keywordId))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterGenresSection(
    category: String,
    sortConfig: SortConfig,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSortConfigChanged: (SortConfig) -> Unit,
    isFolder: Boolean = false,
    hasTvSeries: Boolean = false
) {
    var showAllGenres by remember { mutableStateOf(false) }
    val currentLanguage = LocalConfiguration.current.locales[0]?.language ?: "en"

    ExpandableSection(
        title = stringResource(R.string.filter_genres),
        isExpanded = isExpanded,
        badgeCount = sortConfig.selectedGenres.size,
        onToggle = onToggle
    ) {
        FlowRow(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isTvExclusive = category.contains("tv") && !category.contains("movie") && !isFolder
            val isMovieExclusive = category.contains("movie") && !category.contains("tv") && !isFolder && !hasTvSeries
            val baseGenres = when {
                isTvExclusive -> GenreConstants.TV_GENRES
                isMovieExclusive -> GenreConstants.MOVIE_GENRES
                else -> GenreConstants.ALL_GENRES
            }

            val sortedGenres = remember(baseGenres, currentLanguage) {
                baseGenres.sortedBy { genre ->
                    GenreConstants.getLocalizedName(genre.id, currentLanguage, genre.name).lowercase()
                }
            }

            val displayedGenres = if (showAllGenres) {
                sortedGenres
            } else {
                sortedGenres.filterIndexed { index, genre ->
                    index < 12 || genre.id in sortConfig.selectedGenres
                }
            }

            displayedGenres.forEach { genre ->
                val isSelected = genre.id in sortConfig.selectedGenres
                val localizedName = GenreConstants.getLocalizedName(genre.id, currentLanguage, genre.name)
                FilterChip(
                    label = localizedName.uppercase(),
                    isSelected = isSelected,
                    onClick = {
                        val newList = if (isSelected) sortConfig.selectedGenres - genre.id
                        else sortConfig.selectedGenres + genre.id
                        onSortConfigChanged(sortConfig.copy(selectedGenres = newList))
                    }
                )
            }

            if (!showAllGenres && sortedGenres.size > displayedGenres.size) {
                Box(
                    modifier = Modifier
                        .bounceClick { showAllGenres = true }
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(width = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), shape = CircleShape)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_plus),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = stringResource(R.string.filter_show_all).uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            } else if (showAllGenres && sortedGenres.size > 12) {
                Box(
                    modifier = Modifier
                        .bounceClick { showAllGenres = false }
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(width = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), shape = CircleShape)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_drop_arrow_up),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = stringResource(R.string.show_less).uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterPlatformsSection(
    sortConfig: SortConfig,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSortConfigChanged: (SortConfig) -> Unit
) {
    val configuration = LocalConfiguration.current
    ExpandableSection(
        title = stringResource(R.string.filter_platforms),
        isExpanded = isExpanded,
        badgeCount = sortConfig.selectedProviders.size,
        onToggle = onToggle
    ) {
        val currentLang = configuration.locales[0]?.language?.lowercase() ?: java.util.Locale.getDefault().language.lowercase()
        val availableProviders = remember(currentLang) { ProviderConstants.getAvailableProviders(currentLang) }
        val providersListState = androidx.compose.foundation.lazy.rememberLazyListState()
        LazyRow(
            state = providersListState,
            modifier = Modifier.horizontalFadingEdges(providersListState, leftEdgeWidth = 16.dp, rightEdgeWidth = 16.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(availableProviders, key = { it.providerId }, contentType = { "provider" }) { provider ->
                val isSelected = provider.providerId in sortConfig.selectedProviders
                ProviderItem(
                    name = provider.providerName,
                    logoPath = provider.logoPath,
                    isSelected = isSelected,
                    onClick = {
                        val newList = if (isSelected)
                            sortConfig.selectedProviders - provider.providerId
                        else
                            sortConfig.selectedProviders + provider.providerId
                        onSortConfigChanged(sortConfig.copy(selectedProviders = newList))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterPeriodSection(
    category: String,
    sortConfig: SortConfig,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSortConfigChanged: (SortConfig) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isUpcoming = category.contains("upcoming")
    val sectionTitle = if (isUpcoming) {
        stringResource(R.string.filter_release_month)
    } else {
        stringResource(R.string.filter_period)
    }
    ExpandableSection(
        title = sectionTitle,
        isExpanded = isExpanded,
        badgeCount = sortConfig.selectedDecades.size,
        onToggle = onToggle
    ) {
        if (isUpcoming) {
            val currentYearMonth = remember { java.time.YearMonth.now() }
            val currentLocale = configuration.locales[0] ?: java.util.Locale.getDefault()
            val months = remember(currentYearMonth, currentLocale) {
                (0..11).map { offset ->
                    val ym = currentYearMonth.plusMonths(offset.toLong())
                    val key = String.format(java.util.Locale.US, "%04d-%02d", ym.year, ym.monthValue)
                    val rawName = ym.month.getDisplayName(java.time.format.TextStyle.FULL, currentLocale)
                    val capitalized = rawName.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(currentLocale) else it.toString()
                    }
                    val label = if (ym.year == currentYearMonth.year) {
                        capitalized
                    } else {
                        "$capitalized '${ym.year % 100}"
                    }
                    key to label
                }
            }
            val beyondLabel = stringResource(R.string.filter_future_beyond)

            FlowRow(
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                months.forEach { (key, label) ->
                    val isSelected = key in sortConfig.selectedDecades
                    FilterChip(
                        label = label,
                        isSelected = isSelected,
                        onClick = {
                            val newList = if (isSelected) sortConfig.selectedDecades - key
                            else sortConfig.selectedDecades + key
                            onSortConfigChanged(sortConfig.copy(selectedDecades = newList))
                        }
                    )
                }
                val isBeyondSelected = "future" in sortConfig.selectedDecades
                FilterChip(
                    label = beyondLabel,
                    isSelected = isBeyondSelected,
                    onClick = {
                        val newList = if (isBeyondSelected) sortConfig.selectedDecades - "future"
                        else sortConfig.selectedDecades + "future"
                        onSortConfigChanged(sortConfig.copy(selectedDecades = newList))
                    }
                )
            }
        } else {
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            val currentDecade = (currentYear / 10) * 10
            val decades = (currentDecade downTo 1960 step 10).map { it.toString() }
            FlowRow(
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                decades.forEach { decade ->
                    val isSelected = decade in sortConfig.selectedDecades
                    FilterChip(
                        label = "${decade}s",
                        isSelected = isSelected,
                        onClick = {
                            val newList = if (isSelected) sortConfig.selectedDecades - decade
                            else sortConfig.selectedDecades + decade
                            onSortConfigChanged(sortConfig.copy(selectedDecades = newList))
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterCountriesSection(
    sortConfig: SortConfig,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSortConfigChanged: (SortConfig) -> Unit,
    availableCountries: List<String> = emptyList()
) {
    var showAllCountries by remember { mutableStateOf(false) }
    val currentLocale = LocalConfiguration.current.locales[0] ?: java.util.Locale.getDefault()

    ExpandableSection(
        title = stringResource(R.string.filter_countries),
        isExpanded = isExpanded,
        badgeCount = sortConfig.selectedCountries.size,
        onToggle = onToggle
    ) {
        FlowRow(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val allCodes = remember(sortConfig.selectedCountries, availableCountries, currentLocale) {
                val combined = (com.cinetrack.util.CountryUtils.ALL_CINEMA_COUNTRIES + availableCountries + sortConfig.selectedCountries)
                    .map { it.uppercase().trim() }
                    .filter { it.length == 2 }
                    .distinct()
                combined.sortedBy { code ->
                    com.cinetrack.util.CountryUtils.getLocalizedCountryName(code, currentLocale).lowercase(currentLocale)
                }
            }
            val displayedCodes = if (showAllCountries) {
                allCodes
            } else {
                allCodes.filter { code ->
                    code in com.cinetrack.util.CountryUtils.DEFAULT_POPULAR_COUNTRIES || code in sortConfig.selectedCountries
                }
            }

            displayedCodes.forEach { code ->
                val isSelected = code in sortConfig.selectedCountries
                val flag = com.cinetrack.util.CountryUtils.getFlagEmoji(code)
                val countryName = com.cinetrack.util.CountryUtils.getLocalizedCountryName(code, currentLocale)
                FilterChip(
                    label = "$flag ${countryName.uppercase(currentLocale)}",
                    isSelected = isSelected,
                    onClick = {
                        val newList = if (isSelected) sortConfig.selectedCountries - code
                        else sortConfig.selectedCountries + code
                        onSortConfigChanged(sortConfig.copy(selectedCountries = newList))
                    }
                )
            }

            if (!showAllCountries && allCodes.size > displayedCodes.size) {
                Box(
                    modifier = Modifier
                        .bounceClick { showAllCountries = true }
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(width = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), shape = CircleShape)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_plus),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = stringResource(R.string.filter_show_all).uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            } else if (showAllCountries && allCodes.size > 12) {
                Box(
                    modifier = Modifier
                        .bounceClick { showAllCountries = false }
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(width = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), shape = CircleShape)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_drop_arrow_up),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = stringResource(R.string.show_less).uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
