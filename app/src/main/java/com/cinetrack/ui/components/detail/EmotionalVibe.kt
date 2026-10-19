package com.cinetrack.ui.components.detail

import androidx.annotation.StringRes
import com.cinetrack.R

/**
 * Represents one selectable emotional reaction.
 */
data class EmotionalVibe(
    val code: String,
    val emoji: String,
    @StringRes val labelRes: Int,
    val percentage: Int,
    val iconRes: Int,
    val colorHex: Long
)

val ALL_VIBES = listOf(
    EmotionalVibe("MASTERPIECE",  "🤩", R.string.checkin_vibe_masterpiece, 34, R.drawable.ic_vibe_masterpiece, 0xFFFFD700),
    EmotionalVibe("MIND_BLOWING", "🤯", R.string.checkin_vibe_mind_blowing, 21, R.drawable.ic_vibe_mind_blowing, 0xFF9C27B0),
    EmotionalVibe("IN_TEARS",     "😭", R.string.checkin_vibe_in_tears, 15, R.drawable.ic_vibe_in_tears, 0xFF2196F3),
    EmotionalVibe("HYPED",        "🔥", R.string.checkin_vibe_action, 7, R.drawable.ic_vibe_hyped, 0xFFFF5722),
    EmotionalVibe("COZY",         "☕", R.string.checkin_vibe_comfort, 8, R.drawable.ic_vibe_cozy, 0xFFFF9800),
    EmotionalVibe("FEELS_GOOD",   "😊", R.string.checkin_vibe_feels_good, 12, R.drawable.ic_vibe_feels_good, 0xFFFFEB3B),
    EmotionalVibe("FUNNY",        "😂", R.string.checkin_vibe_funny, 5, R.drawable.ic_vibe_funny, 0xFFE91E63),
    EmotionalVibe("MEH",          "😐", R.string.checkin_vibe_meh, 3, R.drawable.ic_vibe_meh, 0xFF9E9E9E),
    EmotionalVibe("WEIRD",        "🌀", R.string.checkin_vibe_weird, 2, R.drawable.ic_vibe_weird, 0xFF00BCD4),
    EmotionalVibe("SCARY",        "😱", R.string.checkin_vibe_scary, 1, R.drawable.ic_vibe_scary, 0xFFF44336),
    EmotionalVibe("DISAPPOINTED", "😤", R.string.checkin_vibe_disappointed, 1, R.drawable.ic_vibe_disappointed, 0xFFE64A19),
    EmotionalVibe("BORING",       "😴", R.string.checkin_vibe_boring, 1, R.drawable.ic_vibe_boring, 0xFF7986CB)
)

fun normalizeVibeCode(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val clean = raw.trim().uppercase()
    return when (clean) {
        "MIND_BLOWING", "MINDBLOWING", "MIND-BLOWING", "SHOCKED", "SHOCK", "SURPRISED", "🤯" -> "MIND_BLOWING"
        "HYPED", "HYPE", "THRILLED", "THRILL", "EXCITED", "ACTION", "🔥" -> "HYPED"
        "FUNNY", "AMUSED", "AMUSING", "HILARIOUS", "LAUGH", "😂" -> "FUNNY"
        "IN_TEARS", "TEARS", "SAD", "CRYING", "HEARTBREAKING", "😭" -> "IN_TEARS"
        "FEELS_GOOD", "FEELSGOOD", "FEELS GOOD", "TOUCHED", "WHOLESOME", "WARM", "LOVED", "HEARTWARMING", "😊" -> "FEELS_GOOD"
        "COZY", "UNDERSTOOD", "COMFORT", "PEACEFUL", "RELAXING", "CHILL", "☕" -> "COZY"
        "DISAPPOINTED", "FRUSTRATED", "ANGRY", "MAD", "UPSET", "😤" -> "DISAPPOINTED"
        "WEIRD", "CONFUSED", "STRANGE", "WTF", "SURREAL", "ODD", "🌀" -> "WEIRD"
        "SCARY", "SCARED", "TENSE", "SPOOKY", "TERRIFIED", "HORROR", "😱" -> "SCARY"
        "BORING", "BORED", "SLEEPY", "TIRED", "DULL", "😴" -> "BORING"
        "MASTERPIECE", "PERFECT", "GOAT", "AMAZING", "BRILLIANT", "CAPOLAVORO", "🤩" -> "MASTERPIECE"
        "MEH", "AVERAGE", "OK", "NEUTRAL", "SO-SO", "😐" -> "MEH"
        else -> clean
    }
}

fun findVibe(codeOrEmoji: String?): EmotionalVibe? {
    if (codeOrEmoji.isNullOrBlank()) return null
    val normalized = normalizeVibeCode(codeOrEmoji)
    val emojiPart = codeOrEmoji.trim().split(" ").firstOrNull()
    return ALL_VIBES.find { it.code == normalized || it.emoji == codeOrEmoji.trim() || (emojiPart != null && it.emoji == emojiPart) }
}
