package com.cinetrack.ui.components.share

import java.util.Locale

internal fun getFlagEmoji(languageCode: String?): String {
    if (languageCode.isNullOrBlank()) return "🌐"
    return when (languageCode.lowercase().trim()) {
        "it" -> "🇮🇹"
        "en" -> "🇬🇧"
        "us" -> "🇺🇸"
        "fr" -> "🇫🇷"
        "es" -> "🇪🇸"
        "de" -> "🇩🇪"
        "ja" -> "🇯🇵"
        "ko" -> "🇰🇷"
        "zh", "cn" -> "🇨🇳"
        "pt" -> "🇵🇹"
        "ru" -> "🇷🇺"
        "ar" -> "🇸🇦"
        "hi" -> "🇮🇳"
        "el" -> "🇬🇷"
        "tr" -> "🇹🇷"
        "nl" -> "🇳🇱"
        "pl" -> "🇵🇱"
        "sv" -> "🇸🇪"
        "da" -> "🇩🇰"
        "no" -> "🇳🇴"
        "fi" -> "🇫🇮"
        "cs" -> "🇨🇿"
        "hu" -> "🇭🇺"
        "ro" -> "🇷🇴"
        "th" -> "🇹🇭"
        "id" -> "🇮🇩"
        "vi" -> "🇻🇳"
        "uk" -> "🇺🇦"
        "he", "iw" -> "🇮🇱"
        "fa" -> "🇮🇷"
        "bn" -> "🇧🇩"
        "ur" -> "🇵🇰"
        "ta" -> "🇮🇳"
        "te" -> "🇮🇳"
        "ml" -> "🇮🇳"
        "mr" -> "🇮🇳"
        "gu" -> "🇮🇳"
        "kn" -> "🇮🇳"
        "tl" -> "🇵🇭"
        "ms" -> "🇲🇾"
        "sk" -> "🇸🇰"
        "bg" -> "🇧🇬"
        "hr" -> "🇭🇷"
        "sr" -> "🇷🇸"
        "sl" -> "🇸🇮"
        "et" -> "🇪🇪"
        "lv" -> "🇱🇻"
        "lt" -> "🇱🇹"
        "is" -> "🇮🇸"
        "ga" -> "🇮🇪"
        "eu" -> "🇪🇸"
        "ca" -> "🇪🇸"
        "gl" -> "🇪🇸"
        else -> "🌐"
    }
}

internal fun getLanguageDisplayName(code: String): String {
    val flag = getFlagEmoji(code)
    val loc = Locale.forLanguageTag(code)
    val localizedName = loc.getDisplayLanguage(Locale.getDefault()).replaceFirstChar { it.uppercase() }
    val display = if (localizedName.isNotBlank() && localizedName != code) localizedName else code.uppercase()
    return "$flag $display"
}
