package com.cinetrack.ui.utils

import com.cinetrack.data.api.Provider

object ProviderConstants {
    val ALL_PROVIDERS = listOf(
        Provider(8L, "Netflix", "/pbpMk2JmcoNnQwx5JGpXngfoWtp.jpg"),
        Provider(337L, "Disney Plus", "/97yvRBw1GzX7fXprcF80er19ot.jpg"),
        Provider(119L, "Prime Video", "/pvske1MyAoymrs5bguRfVqYiM9a.jpg"),
        Provider(350L, "Apple TV Plus", "/mcbz1LgtErU9p4UdbZ0rG6RTWHX.jpg"),
        Provider(531L, "Paramount Plus", "/h5DcR0J2EESLitnhR8xLG1QymTE.jpg"),
        Provider(39L, "NOW", "/g0E9h3JAeIwmdvxlT73jiEuxdNj.jpg"),
        Provider(359L, "Mediaset Infinity", "/2hBbMVUI2G4GAGRAD0RZCZqDMUh.jpg"),
        Provider(35L, "Rakuten TV", "/bZvc9dXrXNly7cA0V4D9pR8yJwm.jpg"),
        Provider(222L, "RaiPlay", "/cmURKKdS72Ckr52615xvc2JPsJm.jpg"),
        Provider(109L, "TIMVISION", "/6FDKQWcR6JfmRKLqezSsvGgRuUY.jpg"),
        Provider(512L, "Discovery+", "/bPW3J8KlLrot95sLzadnpzVe61f.jpg"),
        Provider(283L, "Crunchyroll", "/fzN5Jok5Ig1eJ7gyNGoMhnLSCfh.jpg"),
        Provider(3L, "Google TV", "/8z7rC8uIDaTM91X0ZfkRf04ydj2.jpg"),
        Provider(300L, "Pluto TV", "/dB8G41Q6tSL5NBisrIeqByfepBc.jpg"),
        Provider(192L, "YouTube", "/pTnn5JwWr4p3pG8H6VrpiQo7Vs0.jpg"),
        Provider(40L, "Chili", "/cNqhhi5UYjAvJv2M69t5cwSn0So.jpg"),
        Provider(11L, "MUBI", "/x570VpH2C9EKDf1riP83rYc5dnL.jpg")
    )

    fun getLogoUrl(logoPath: String?): String {
        if (logoPath == null) return ""
        if (logoPath.startsWith("http")) return logoPath
        return "https://image.tmdb.org/t/p/w154$logoPath"
    }

    fun getAvailableProviders(language: String = java.util.Locale.getDefault().language.lowercase()): List<Provider> {
        val italianProviderIds = setOf(359L, 222L, 109L, 40L) // Mediaset Infinity, RaiPlay, TIMVISION, Chili
        val internationalProviders = ALL_PROVIDERS + listOf(
            Provider(15L, "Hulu", "/bxBlRPEPpMVDc4jMhSrTf2339DW.jpg"),
            Provider(1899L, "Max", "/jbe4gVSfRlbPTdESXhEKpornsfu.jpg"),
            Provider(386L, "Peacock", "/2aGrp1xw3qhwCYvNGAJZPdjfeeX.jpg")
        )

        return if (language == "it") {
            ALL_PROVIDERS
        } else {
            internationalProviders.filterNot { it.providerId in italianProviderIds }
        }
    }
}

