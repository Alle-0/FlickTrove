package com.cinetrack.ui.components.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cinetrack.R
import com.cinetrack.data.api.SourceCatalogRow
import com.cinetrack.data.model.SortConfig
import com.cinetrack.ui.components.dialog.ExpandableSection
import com.cinetrack.ui.components.dialog.FilterChip
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterCommentsSourceSection(
    availableSources: List<SourceCatalogRow>,
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
                    SourceCatalogRow(slug = "flicktrove", displayName = "FlickTrove"),
                    SourceCatalogRow(slug = "tvtime", displayName = "TV Time Refugees")
                )
            }
            val filteredList = rawList.filter {
                !it.slug.equals("commsuni.tv", ignoreCase = true) &&
                !it.slug.equals("commsuni", ignoreCase = true) &&
                !(it.displayName?.equals("commsuni.tv", ignoreCase = true) == true)
            }
            val flickTroveItem = filteredList.firstOrNull { it.slug.equals("flicktrove", ignoreCase = true) }
                ?: SourceCatalogRow(slug = "flicktrove", displayName = "FlickTrove")
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
        val currentLocale = if (!configuration.locales.isEmpty) configuration.locales[0] else Locale.getDefault()
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
                val loc = Locale.forLanguageTag(code)
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
