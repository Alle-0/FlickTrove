package com.cinetrack.ui.components.card

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.data.model.NextEpisodeInfo

// ─── Constants & Styles ────────────────────────────────────────────────────────

/** Values for episodeInfoStyle preference */
object EpisodeInfoStyle {
    // 1. Remaining
    const val REMAINING_SEASON = "REMAINING_SEASON"
    const val REMAINING_TOTAL = "REMAINING_TOTAL"
    const val REMAINING_SEASON_AND_TOTAL = "REMAINING_SEASON_AND_TOTAL"
    const val REMAINING = "REMAINING" // default / alias for REMAINING_SEASON_AND_TOTAL

    // 2. Watched / Total
    const val SEASON_ONLY = "SEASON_ONLY"
    const val WATCHED_OVER_TOTAL = "WATCHED_OVER_TOTAL"
    const val SEASON_AND_TOTAL = "SEASON_AND_TOTAL"

    // 3. Percentage
    const val PERCENTAGE_SEASON = "PERCENTAGE_SEASON"
    const val PERCENTAGE_TOTAL = "PERCENTAGE_TOTAL"
    const val PERCENTAGE = "PERCENTAGE" // alias for PERCENTAGE_TOTAL
    const val PERCENTAGE_SEASON_AND_TOTAL = "PERCENTAGE_SEASON_AND_TOTAL"
}

/** Values for progressBarTarget preference */
object ProgressBarTarget {
    const val CIRCULAR_TOTAL = "CIRCULAR_TOTAL"
    const val CIRCULAR_SEASON = "CIRCULAR_SEASON"
    const val LINEAR_TOTAL = "LINEAR_TOTAL"
    const val LINEAR_SEASON = "LINEAR_SEASON"
    const val NONE = "NONE"
}

enum class PreviewCardMode {
    GRID_3_COL,
    GRID_2_COL,
    LIST
}

// ─── Shared Formatters ──────────────────────────────────────────────────────

fun formatEpisodeInfo(
    style: String,
    nextInfo: NextEpisodeInfo,
    movie: Movie,
    seriesProgress: Float
): String {
    val totalInSeason = movie.seasons?.find { it.seasonNumber == nextInfo.seasonNumber }?.episodeCount ?: 0
    val watchedInSeason = movie.watchedEpisodes?.get(nextInfo.seasonNumber.toString())?.size 
        ?: if (totalInSeason > 0) (totalInSeason - nextInfo.remainingInSeason).coerceAtLeast(0) else 0
    val totalWatched = movie.watchedEpisodes?.filter { it.key != "0" }?.values?.sumOf { it.size } ?: 0
    val totalEpisodes = movie.numberOfEpisodes ?: 0
    val seasonProgress = if (totalInSeason > 0) (watchedInSeason.toFloat() / totalInSeason.toFloat()).coerceIn(0f, 1f) else 0f

    return when (style) {
        // Remaining
        EpisodeInfoStyle.REMAINING_SEASON -> {
            if (nextInfo.remainingInSeason > 0) " • ${nextInfo.remainingInSeason}"
            else ""
        }
        EpisodeInfoStyle.REMAINING_TOTAL -> {
            if (nextInfo.remainingTotal > 0) " • ${nextInfo.remainingTotal} tot"
            else ""
        }
        EpisodeInfoStyle.REMAINING, EpisodeInfoStyle.REMAINING_SEASON_AND_TOTAL, "REMAINING_AND_TOTAL" -> {
            if (nextInfo.remainingTotal > nextInfo.remainingInSeason) {
                " • ${nextInfo.remainingInSeason} (${nextInfo.remainingTotal} tot)"
            } else if (nextInfo.remainingInSeason > 0) {
                " • ${nextInfo.remainingInSeason}"
            } else {
                ""
            }
        }

        // Watched / Total
        EpisodeInfoStyle.SEASON_ONLY -> {
            if (totalInSeason > 0) " • $watchedInSeason/$totalInSeason ep"
            else ""
        }
        EpisodeInfoStyle.WATCHED_OVER_TOTAL -> {
            if (totalEpisodes > 0) " • $totalWatched/$totalEpisodes ep"
            else if (totalWatched > 0) " • $totalWatched ep"
            else ""
        }
        EpisodeInfoStyle.SEASON_AND_TOTAL -> {
            if (totalInSeason > 0 && totalEpisodes > 0) " • $watchedInSeason/$totalInSeason • $totalWatched/$totalEpisodes tot"
            else if (totalInSeason > 0) " • $watchedInSeason/$totalInSeason ep"
            else if (totalEpisodes > 0) " • $totalWatched/$totalEpisodes tot"
            else ""
        }

        // Percentage
        EpisodeInfoStyle.PERCENTAGE_SEASON -> {
            " • ${(seasonProgress * 100).toInt()}%"
        }
        EpisodeInfoStyle.PERCENTAGE, EpisodeInfoStyle.PERCENTAGE_TOTAL -> {
            " • ${(seriesProgress * 100).toInt()}%"
        }
        EpisodeInfoStyle.PERCENTAGE_SEASON_AND_TOTAL -> {
            " • ${(seasonProgress * 100).toInt()}% • ${(seriesProgress * 100).toInt()}%"
        }

        else -> {
            if (nextInfo.remainingInSeason > 0) " • ${nextInfo.remainingInSeason}"
            else ""
        }
    }
}

fun calculateSeasonProgress(nextInfo: NextEpisodeInfo, movie: Movie): Float {
    val totalInSeason = movie.seasons?.find { it.seasonNumber == nextInfo.seasonNumber }?.episodeCount ?: 0
    if (totalInSeason <= 0) return 0f
    val watchedInSeason = movie.watchedEpisodes?.get(nextInfo.seasonNumber.toString())?.size 
        ?: (totalInSeason - nextInfo.remainingInSeason).coerceAtLeast(0)
    return (watchedInSeason.toFloat() / totalInSeason.toFloat()).coerceIn(0f, 1f)
}

@Composable
fun formatDrawerEpisodeSubtitle(
    style: String,
    nextInfo: NextEpisodeInfo,
    movie: Movie,
    seriesProgress: Float
): String {
    val totalInSeason = movie.seasons?.find { it.seasonNumber == nextInfo.seasonNumber }?.episodeCount ?: 0
    val watchedInSeason = movie.watchedEpisodes?.get(nextInfo.seasonNumber.toString())?.size 
        ?: if (totalInSeason > 0) (totalInSeason - nextInfo.remainingInSeason).coerceAtLeast(0) else 0
    val totalWatched = movie.watchedEpisodes?.filter { it.key != "0" }?.values?.sumOf { it.size } ?: 0
    val totalEpisodes = movie.numberOfEpisodes ?: 0
    val seasonProgress = if (totalInSeason > 0) (watchedInSeason.toFloat() / totalInSeason.toFloat()).coerceIn(0f, 1f) else 0f

    return when (style) {
        // Remaining
        EpisodeInfoStyle.REMAINING_SEASON -> {
            val remainingSeason = nextInfo.remainingInSeason
            if (remainingSeason > 0) stringResource(R.string.lbl_episodes_remaining, remainingSeason)
            else ""
        }
        EpisodeInfoStyle.REMAINING_TOTAL -> {
            val remainingTotal = nextInfo.remainingTotal
            if (remainingTotal > 0) "$remainingTotal tot"
            else ""
        }
        EpisodeInfoStyle.REMAINING, EpisodeInfoStyle.REMAINING_SEASON_AND_TOTAL, "REMAINING_AND_TOTAL" -> {
            val remainingSeason = nextInfo.remainingInSeason
            val remainingTotal = nextInfo.remainingTotal
            if (remainingTotal > remainingSeason) {
                "${stringResource(R.string.lbl_episodes_remaining, remainingSeason)} ($remainingTotal tot)"
            } else if (remainingSeason > 0) {
                stringResource(R.string.lbl_episodes_remaining, remainingSeason)
            } else {
                ""
            }
        }

        // Watched / Total
        EpisodeInfoStyle.SEASON_ONLY -> {
            if (totalInSeason > 0) "$watchedInSeason / $totalInSeason ep"
            else ""
        }
        EpisodeInfoStyle.WATCHED_OVER_TOTAL -> {
            if (totalEpisodes > 0) "$totalWatched / $totalEpisodes ep"
            else if (totalWatched > 0) "$totalWatched ep"
            else ""
        }
        EpisodeInfoStyle.SEASON_AND_TOTAL -> {
            if (totalInSeason > 0 && totalEpisodes > 0) "$watchedInSeason/$totalInSeason • $totalWatched/$totalEpisodes tot"
            else if (totalInSeason > 0) "$watchedInSeason / $totalInSeason ep"
            else if (totalEpisodes > 0) "$totalWatched / $totalEpisodes tot"
            else ""
        }

        // Percentage
        EpisodeInfoStyle.PERCENTAGE_SEASON -> {
            "${(seasonProgress * 100).toInt()}%"
        }
        EpisodeInfoStyle.PERCENTAGE, EpisodeInfoStyle.PERCENTAGE_TOTAL -> {
            "${(seriesProgress * 100).toInt()}%"
        }
        EpisodeInfoStyle.PERCENTAGE_SEASON_AND_TOTAL -> {
            "${(seasonProgress * 100).toInt()}% • ${(seriesProgress * 100).toInt()}%"
        }

        else -> {
            val remainingSeason = nextInfo.remainingInSeason
            if (remainingSeason > 0) stringResource(R.string.lbl_episodes_remaining, remainingSeason)
            else ""
        }
    }
}
