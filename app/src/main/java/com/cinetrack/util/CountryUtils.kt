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
     * Default popular cinema & TV producing countries displayed initially before expanding.
     */
    val DEFAULT_POPULAR_COUNTRIES = listOf(
        "US", "GB", "IT", "FR", "ES", "DE", "JP", "KR", "CN", "CA", "AU", "IN"
    )

    /**
     * Comprehensive list of prominent film and television producing nations worldwide.
     */
    val ALL_CINEMA_COUNTRIES = listOf(
        // North America
        "US", "CA", "MX",
        // Western / Southern Europe
        "IT", "FR", "GB", "ES", "DE", "IE", "PT", "NL", "BE", "CH", "AT", "GR",
        // Nordic Europe
        "SE", "DK", "NO", "FI", "IS",
        // Eastern / Central Europe
        "PL", "CZ", "HU", "RO", "RU", "UA",
        // East & Southeast Asia
        "JP", "KR", "CN", "HK", "TW", "IN", "TH", "ID", "PH", "VN", "MY", "SG",
        // Middle East & Africa
        "TR", "IR", "IL", "EG", "ZA", "NG", "MA",
        // Latin America
        "BR", "AR", "CL", "CO", "PE", "UY",
        // Oceania
        "AU", "NZ"
    ).distinct()

    /**
     * Backwards-compatible alias for existing callers.
     */
    val POPULAR_COUNTRIES = ALL_CINEMA_COUNTRIES
}
