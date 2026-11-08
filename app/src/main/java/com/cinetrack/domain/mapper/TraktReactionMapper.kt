package com.cinetrack.domain.mapper

object TraktReactionMapper {

    private val EMOJI_TO_VIBE: Map<String, String> = mapOf(
        // MASTERPIECE
        "😍" to "MASTERPIECE",
        "👏" to "MASTERPIECE",
        "🤌" to "MASTERPIECE",

        // COZY
        "❤️" to "COZY",
        "🍿" to "COZY",
        "🥰" to "COZY",

        // FEELS_GOOD
        "🥳" to "FEELS_GOOD",
        "👍" to "FEELS_GOOD",

        // FUNNY
        "🤣" to "FUNNY",
        "😂" to "FUNNY", // robust fallback

        // IN_TEARS
        "😭" to "IN_TEARS",
        "🥺" to "IN_TEARS",
        "🥹" to "IN_TEARS",
        "🥲" to "IN_TEARS",
        "💔" to "IN_TEARS",

        // MIND_BLOWING
        "🤯" to "MIND_BLOWING",
        "😳" to "MIND_BLOWING",

        // HYPED
        "🔥" to "HYPED",
        "🤘" to "HYPED",

        // SCARY
        "😱" to "SCARY",
        "💀" to "SCARY",
        "😰" to "SCARY",
        "🫣" to "SCARY",
        "🥶" to "SCARY",

        // WEIRD
        "🥴" to "WEIRD",
        "🤔" to "WEIRD",
        "🤫" to "WEIRD",
        "🥸" to "WEIRD",
        "🤓" to "WEIRD",
        "🧐" to "WEIRD",

        // BORING
        "🥱" to "BORING",

        // MEH
        "😐" to "MEH",
        "🙄" to "MEH",
        "🫠" to "MEH",
        "😬" to "MEH",

        // DISAPPOINTED
        "👎" to "DISAPPOINTED",
        "🤮" to "DISAPPOINTED",
        "🤬" to "DISAPPOINTED",
        "😩" to "DISAPPOINTED"
    )

    private val VIBE_TO_CANONICAL_EMOJI: Map<String, String> = mapOf(
        "MASTERPIECE" to "😍",
        "COZY" to "❤️",
        "FEELS_GOOD" to "🥳",
        "FUNNY" to "🤣",
        "IN_TEARS" to "😭",
        "MIND_BLOWING" to "🤯",
        "HYPED" to "🔥",
        "SCARY" to "😱",
        "WEIRD" to "🥴",
        "BORING" to "🥱",
        "MEH" to "😐",
        "DISAPPOINTED" to "👎"
    )

    /**
     * Maps an incoming Trakt reaction emoji to FlickTrove's Emotional Vibe code.
     */
    fun emojiToVibeCode(emoji: String?): String? {
        if (emoji.isNullOrBlank()) return null
        return EMOJI_TO_VIBE[emoji.trim()]
    }

    /**
     * Maps a FlickTrove Emotional Vibe code to Trakt's canonical reaction emoji.
     */
    fun vibeCodeToEmoji(vibeCode: String?): String? {
        if (vibeCode.isNullOrBlank()) return null
        val clean = vibeCode.trim().uppercase()
        return VIBE_TO_CANONICAL_EMOJI[clean]
    }

    /**
     * Extracts the primary (first) vibe code from a comma-separated list of vibes.
     */
    fun extractPrimaryVibeCode(rawVibes: String?): String? {
        if (rawVibes.isNullOrBlank()) return null
        return rawVibes.split(",")
            .map { it.trim().uppercase() }
            .firstOrNull { it.isNotBlank() }
    }

    /**
     * Converts the primary vibe in a comma-separated list to the canonical Trakt emoji.
     */
    fun extractPrimaryEmoji(rawVibes: String?): String? {
        val primaryVibe = extractPrimaryVibeCode(rawVibes) ?: return null
        return vibeCodeToEmoji(primaryVibe)
    }

    /**
     * Merges a remote single vibe from Trakt into the local vibes string.
     * Replaces the primary vibe while preserving secondary and tertiary vibes up to a max of 3.
     */
    fun mergeRemoteVibe(localRawVibes: String?, remoteVibeCode: String): String {
        val normalizedRemote = remoteVibeCode.trim().uppercase()
        if (localRawVibes.isNullOrBlank()) return normalizedRemote

        val localList = localRawVibes.split(",")
            .map { it.trim().uppercase() }
            .filter { it.isNotBlank() }

        if (localList.isEmpty()) return normalizedRemote

        // Secondary vibes (skipping the old primary and filtering out duplicate of new remote)
        val secondaries = localList.drop(1).filter { it != normalizedRemote }
        val merged = listOf(normalizedRemote) + secondaries
        return merged.take(3).joinToString(",")
    }
}

