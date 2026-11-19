package com.cinetrack.util

import java.net.URLEncoder

/**
 * Utility for resolving direct deep-link search queries for streaming and VOD providers.
 */
object WatchProviderDeepLinkUtils {

    /**
     * Resolves a deep-link search URL for a given streaming/buy/rent provider.
     * Falls back to [fallbackUrl] (e.g. JustWatch/TMDB watch provider link) if no specific provider app link matches.
     */
    fun resolveProviderSearchUrl(
        providerName: String,
        mediaTitle: String,
        region: String?,
        fallbackUrl: String?
    ): String? {
        val title = try {
            URLEncoder.encode(mediaTitle, "UTF-8")
        } catch (_: Exception) {
            mediaTitle
        }
        val name = providerName.lowercase().trim()

        return when {
            name.contains("netflix") -> "https://www.netflix.com/search?q=$title"
            name.contains("prime video") || name.contains("amazon prime") || name.contains("amazon video") -> "https://app.primevideo.com/search?searchTerm=$title"
            name.contains("disney") -> "https://www.disneyplus.com/search?q=$title"
            name.contains("apple") -> "https://tv.apple.com/search?term=$title"
            name.contains("crunchyroll") -> "https://www.crunchyroll.com/search?q=$title"
            name.contains("now") || name.contains("sky") -> when (region) {
                "IT" -> "https://www.nowtv.it/ricerca?q=$title"
                "GB", "IE" -> "https://www.nowtv.com/watch/search?q=$title"
                else -> fallbackUrl
            }
            name.contains("paramount") -> "https://www.paramountplus.com/search?q=$title"
            name.contains("youtube") -> "https://www.youtube.com/results?search_query=$title"
            name.contains("google play") -> "https://play.google.com/store/search?q=$title&c=movies"
            name.contains("rakuten") -> {
                val rakutenPath = when (region) {
                    "GB" -> "uk"
                    "IT", "ES", "FR", "DE", "NL", "PL", "PT", "AT", "CH", "BE", "SE", "NO", "DK", "FI", "IE", "CZ", "HU", "RO", "GR" -> region.lowercase()
                    else -> null
                }
                if (rakutenPath != null) "https://rakuten.tv/$rakutenPath/search?q=$title" else fallbackUrl
            }
            name.contains("timvision") -> if (region == "IT") "https://www.timvision.it/search?q=$title" else fallbackUrl
            name.contains("infinity") || name.contains("mediaset") -> if (region == "IT") "https://mediasetinfinity.mediaset.it/ricerca/$title" else fallbackUrl
            name.contains("raiplay") -> if (region == "IT") "https://www.raiplay.it/ricerca.html?q=$title" else fallbackUrl
            name.contains("discovery") -> {
                val discRegions = setOf("IT", "GB", "US", "DE", "NL", "SE", "NO", "DK", "FI", "ES", "PL", "BR", "IN")
                if (region != null && region in discRegions) "https://www.discoveryplus.com/${region.lowercase()}/search?query=$title" else fallbackUrl
            }
            name.contains("max") || name.contains("hbo") -> "https://play.max.com/search?q=$title"
            name.contains("hulu") -> "https://www.hulu.com/search?q=$title"
            name.contains("peacock") -> "https://www.peacocktv.com/watch/search?q=$title"
            name.contains("chili") -> when (region) {
                "IT", "DE", "PL", "AT" -> "https://${region.lowercase()}.chili.com/search?q=$title"
                "GB" -> "https://uk.chili.com/search?q=$title"
                else -> fallbackUrl
            }
            name.contains("mgm") -> "https://www.mgmplus.com/search?q=$title"
            else -> fallbackUrl
        }
    }
}
