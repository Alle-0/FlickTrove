package com.cinetrack.util

object CountryUtils {
    /**
     * Converts a 2-letter ISO 3166-1 alpha-2 country code into its corresponding Unicode flag emoji.
     */
    fun getFlagEmoji(countryCode: String): String {
        if (countryCode.length != 2) return "🌍"
        val code = countryCode.uppercase()
        val firstLetter = Character.codePointAt(code, 0) - 0x41 + 0x1F1E6
        val secondLetter = Character.codePointAt(code, 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
    }

    /**
     * Resolves the localized country name for a given ISO country code without deprecation warnings.
     */
    fun getLocalizedCountryName(countryCode: String, targetLocale: java.util.Locale): String {
        return try {
            java.util.Locale.Builder().setRegion(countryCode).build().getDisplayCountry(targetLocale).ifBlank { countryCode }
        } catch (_: Exception) {
            countryCode
        }
    }

    /**
     * Prominent cinema & TV producing countries for filter suggestions.
     */
    val POPULAR_COUNTRIES = listOf(
        "US", "GB", "IT", "FR", "JP", "KR", "ES", "DE",
        "CA", "AU", "IN", "SE", "DK", "NO", "IE", "MX",
        "BR", "CN", "HK", "PL", "NL", "BE", "NZ", "AR"
    )
}
