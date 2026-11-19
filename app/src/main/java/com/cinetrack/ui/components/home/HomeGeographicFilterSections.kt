package com.cinetrack.ui.components.home

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.cinetrack.data.model.SortConfig
import com.cinetrack.ui.components.dialog.ExpandableSection
import com.cinetrack.ui.components.dialog.FilterChip
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.CountryUtils
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale

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
            val currentYearMonth = remember { YearMonth.now() }
            val currentLocale = configuration.locales[0] ?: Locale.getDefault()
            val months = remember(currentYearMonth, currentLocale) {
                (0..11).map { offset ->
                    val ym = currentYearMonth.plusMonths(offset.toLong())
                    val key = String.format(Locale.US, "%04d-%02d", ym.year, ym.monthValue)
                    val rawName = ym.month.getDisplayName(TextStyle.FULL, currentLocale)
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
            val decades = remember {
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                val currentDecade = (currentYear / 10) * 10
                (currentDecade downTo 1960 step 10).map { it.toString() }
            }
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
    val currentLocale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()

    ExpandableSection(
        title = stringResource(R.string.filter_countries),
        isExpanded = isExpanded,
        badgeCount = sortConfig.selectedCountries.size,
        onToggle = onToggle
    ) {
        val allCodes = remember(sortConfig.selectedCountries, availableCountries, currentLocale) {
            val combined = (CountryUtils.ALL_CINEMA_COUNTRIES + availableCountries + sortConfig.selectedCountries)
                .map { it.uppercase().trim() }
                .filter { it.length == 2 }
                .distinct()
            combined.sortedBy { code ->
                CountryUtils.getLocalizedCountryName(code, currentLocale).lowercase(currentLocale)
            }
        }

        AnimatedContent(
            targetState = showAllCountries,
            transitionSpec = {
                (fadeIn(animationSpec = tween(280)) togetherWith
                        fadeOut(animationSpec = tween(180)))
                    .using(
                        SizeTransform(clip = true) { _, _ ->
                            spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        }
                    )
            },
            label = "CountriesExpansion"
        ) { isAllShown ->
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val displayedCodes = if (isAllShown) {
                    allCodes
                } else {
                    allCodes.filter { code ->
                        code in CountryUtils.DEFAULT_POPULAR_COUNTRIES || code in sortConfig.selectedCountries
                    }
                }

                displayedCodes.forEach { code ->
                    val isSelected = code in sortConfig.selectedCountries
                    val flag = CountryUtils.getFlagEmoji(code)
                    val countryName = CountryUtils.getLocalizedCountryName(code, currentLocale)
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

                if (!isAllShown && allCodes.size > displayedCodes.size) {
                    ShowMoreLessToggleChip(
                        isExpanded = false,
                        onClick = { showAllCountries = true }
                    )
                } else if (isAllShown && allCodes.size > 12) {
                    ShowMoreLessToggleChip(
                        isExpanded = true,
                        onClick = { showAllCountries = false }
                    )
                }
            }
        }
    }
}

@Composable
internal fun ShowMoreLessToggleChip(
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .bounceClick(onClick = onClick)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                shape = CircleShape
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(
                    id = if (isExpanded) R.drawable.ic_drop_arrow_up else R.drawable.ic_plus
                ),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = (if (isExpanded) stringResource(R.string.show_less) else stringResource(R.string.filter_show_all)).uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}
