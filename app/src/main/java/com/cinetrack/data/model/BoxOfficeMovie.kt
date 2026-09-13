package com.cinetrack.data.model

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class BoxOfficeMovie(
    val movie: Movie,
    val rank: Int,
    val revenue: Long,
    val formattedRevenue: String,
    val weekendRange: String? = null
) {
    companion object {
        fun formatRevenue(revenue: Long): String {
            return when {
                revenue >= 1_000_000_000L -> {
                    val billions = revenue / 1_000_000_000.0
                    String.format(Locale.US, "$%.2fB", billions)
                }
                revenue >= 1_000_000L -> {
                    val millions = revenue / 1_000_000.0
                    String.format(Locale.US, "$%.1fM", millions)
                }
                revenue >= 1_000L -> {
                    val thousands = revenue / 1_000.0
                    String.format(Locale.US, "$%.1fK", thousands)
                }
                revenue > 0L -> "$$revenue"
                else -> "N/A"
            }
        }
    }
}
