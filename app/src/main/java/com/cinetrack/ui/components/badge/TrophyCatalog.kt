package com.cinetrack.ui.components.badge

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.cinetrack.R
import com.cinetrack.util.DateTimeUtils
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Modello di un trofeo nella vetrina della Trophy Room.
 */
data class TrophyRoomItemUi(
    val badge: PreviewBadgeItem,
    val currentTier: PreviewBadgeTier,
    val isUnlocked: Boolean,
    val unlockedDate: String? = null
)

/**
 * Criteri di ordinamento per la Sala dei Trofei.
 */
enum class TrophySortOption(@StringRes val titleRes: Int) {
    TIER(R.string.trophy_sort_tier),
    PROGRESS(R.string.trophy_sort_progress),
    RARITY(R.string.trophy_sort_rarity),
    RECENT(R.string.trophy_sort_recent),
    ALPHABETICAL(R.string.trophy_sort_alphabetical);

    @Composable
    fun localizedDisplayName(): String = stringResource(titleRes)
}

/**
 * Categorie di filtro per la Sala dei Trofei.
 */
enum class TrophyFilterCategory(val id: String, @StringRes val titleRes: Int) {
    ALL("ALL", R.string.trophy_cat_all),
    PROGRESSIVE("PROGRESSIVE", R.string.trophy_cat_progressive),
    GENRES_ERAS("GENRES_ERAS", R.string.trophy_cat_genres_eras),
    HONORS("HONORS", R.string.trophy_cat_honors),
    LOST_REEL("LOST_REEL", R.string.trophy_cat_lost_reel)
}

/**
 * Stato di sblocco per i filtri.
 */
enum class TrophyStatusFilter(val id: String, @StringRes val titleRes: Int) {
    ALL("ALL", R.string.trophy_status_all),
    UNLOCKED("UNLOCKED", R.string.trophy_status_unlocked),
    LOCKED("LOCKED", R.string.trophy_status_locked)
}

/**
 * Stato filtri nativo della Trophy Room di FlickTrove.
 */
data class TrophyFilterConfig(
    val sortBy: TrophySortOption = TrophySortOption.TIER,
    val sortDirection: String = "desc",
    val categories: Set<TrophyFilterCategory> = emptySet(),
    val status: TrophyStatusFilter = TrophyStatusFilter.ALL,
    val tiers: Set<PreviewBadgeTier> = emptySet()
) {
    val category: TrophyFilterCategory get() = categories.firstOrNull() ?: TrophyFilterCategory.ALL
    val tier: PreviewBadgeTier? get() = tiers.firstOrNull()

    val hasActiveFilters: Boolean
        get() = sortBy != TrophySortOption.TIER || sortDirection != "desc" || categories.isNotEmpty() || status != TrophyStatusFilter.ALL || tiers.isNotEmpty()
}

fun formatLocalizedTrophyDate(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return ""
    return try {
        val parsed = DateTimeUtils.parseDateOnly(dateStr)
            ?: DateTimeUtils.parseIsoDate(dateStr)
            ?: run {
                try {
                    SimpleDateFormat("dd MMM yyyy", Locale.ITALIAN).parse(dateStr)
                } catch (_: Exception) {
                    try {
                        SimpleDateFormat("dd MMM yyyy", Locale.US).parse(dateStr)
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        if (parsed != null) {
            DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(parsed)
        } else dateStr
    } catch (_: Exception) {
        dateStr
    }
}

internal fun getTierRank(tier: PreviewBadgeTier): Int = when (tier) {
    PreviewBadgeTier.LOST_REEL -> 7
    PreviewBadgeTier.THE_FINAL_CUT -> 6
    PreviewBadgeTier.MM_70 -> 5
    PreviewBadgeTier.MM_35 -> 4
    PreviewBadgeTier.MM_16 -> 3
    PreviewBadgeTier.SUPER_8 -> 2
}

internal fun TrophyRoomItemUi.completionFraction(): Float {
    if (!isUnlocked) return 0f
    val isMastered = currentTier == PreviewBadgeTier.THE_FINAL_CUT ||
        (badge.fixedTier != null && isUnlocked)
    val nextTier = when (currentTier) {
        PreviewBadgeTier.SUPER_8 -> PreviewBadgeTier.MM_16
        PreviewBadgeTier.MM_16 -> PreviewBadgeTier.MM_35
        PreviewBadgeTier.MM_35 -> PreviewBadgeTier.MM_70
        PreviewBadgeTier.MM_70 -> PreviewBadgeTier.THE_FINAL_CUT
        else -> null
    }
    val targetThreshold = if (isUnlocked && nextTier != null) {
        badge.progressiveSteps[nextTier]?.targetThreshold ?: badge.progressTarget
    } else {
        badge.progressiveSteps[currentTier]?.targetThreshold ?: badge.progressTarget
    }
    return when {
        !isUnlocked -> 0f
        isMastered -> 1f
        targetThreshold > 0 -> (badge.progressCurrent.toFloat() / targetThreshold).coerceIn(0f, 1f)
        else -> 1f
    }
}
