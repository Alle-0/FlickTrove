package com.cinetrack.util

import java.util.Locale

/**
 * Single Source of Truth for resolving the 2-letter ISO 3166-1 alpha-2 TMDB watch/release region
 * from the user's `contentLanguage` preference and device/locale settings.
 */
object RegionResolver {

    /**
     * Reads the device's underlying OS locale (which preserves the real country code even after
     * `Locale.setDefault(...)` is overridden with a language-only tag in `FlickTroveApp`).
     * Falls back safely to `Locale.getDefault()` on JVM unit tests.
     */
    fun getSystemLocale(): Locale {
        return try {
            android.content.res.Resources.getSystem().configuration.locales.get(0) ?: Locale.getDefault()
        } catch (_: Throwable) {
            Locale.getDefault()
        }
    }

    /**
     * Resolves the 2-letter uppercase TMDB region code (e.g. "IT", "US", "GB", "FR", "DE")
     * from the stored `contentLanguage` preference.
     */
    fun resolveRegion(
        contentLanguage: String,
        systemLocale: Locale = getSystemLocale()
    ): String {
        val raw = contentLanguage.trim()
        if (raw.isEmpty() || raw.equals("system", ignoreCase = true)) {
            return resolveFromLocale(systemLocale, systemLocale)
        }

        val normalized = raw.lowercase(Locale.ROOT).replace("_", "-")
        if (normalized.contains("-")) {
            val subtag = normalized.substringAfter("-")
                .removePrefix("r")
                .uppercase(Locale.ROOT)
            if (subtag.length == 2 && subtag.all { it in 'A'..'Z' }) {
                return subtag
            }
        }

        val lang = normalized.substringBefore("-")
        if (lang == "en") {
            val sysCountry = systemLocale.country.trim().uppercase(Locale.ROOT)
            if (systemLocale.language.equals("en", ignoreCase = true) && isIsoCountry(sysCountry)) {
                return sysCountry
            }
            return "US"
        }

        return languageToDefaultRegion(lang)
    }

    /**
     * Resolves the 2-letter uppercase TMDB region code from a [Locale] instance,
     * preserving full language-to-region parity when [Locale.getCountry] is empty.
     */
    fun resolveFromLocale(
        locale: Locale = Locale.getDefault(),
        systemLocale: Locale = getSystemLocale()
    ): String {
        val country = locale.country.trim().uppercase(Locale.ROOT)
        if (isIsoCountry(country)) {
            return country
        }

        val lang = locale.language.lowercase(Locale.ROOT)
        if (lang == "en") {
            val sysCountry = systemLocale.country.trim().uppercase(Locale.ROOT)
            if (systemLocale.language.equals("en", ignoreCase = true) && isIsoCountry(sysCountry)) {
                return sysCountry
            }
            return "US"
        }

        return languageToDefaultRegion(lang)
    }

    private fun isIsoCountry(code: String): Boolean {
        return code.length == 2 && code[0] in 'A'..'Z' && code[1] in 'A'..'Z'
    }

    private fun languageToDefaultRegion(languageCode: String): String {
        return when (languageCode.lowercase(Locale.ROOT)) {
            "it" -> "IT"
            "es" -> "ES"
            "fr" -> "FR"
            "de" -> "DE"
            "pt" -> "BR"
            "ru" -> "RU"
            "hi" -> "IN"
            "ja" -> "JP"
            "ko" -> "KR"
            "zh" -> "CN"
            "id", "in" -> "ID"
            "tr" -> "TR"
            "pl" -> "PL"
            "nl" -> "NL"
            "cs" -> "CZ"
            "el" -> "GR"
            "hu" -> "HU"
            "ro" -> "RO"
            "sv" -> "SE"
            "th" -> "TH"
            "uk" -> "UA"
            "vi" -> "VN"
            "ar" -> "SA"
            "da" -> "DK"
            "fi" -> "FI"
            else -> "US"
        }
    }
}
