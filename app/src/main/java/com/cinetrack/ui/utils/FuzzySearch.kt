package com.cinetrack.ui.utils

/**
 * FuzzySearch
 * Provides client-side similarity scoring to compensate for TMDB's exact-match search API.
 * Uses Jaro-Winkler distance + word-level matching + Italian accent normalization.
 */
object FuzzySearch {

    private val ACCENT_MAP = mapOf(
        'à' to 'a', 'á' to 'a', 'â' to 'a', 'ã' to 'a', 'ä' to 'a',
        'è' to 'e', 'é' to 'e', 'ê' to 'e', 'ë' to 'e',
        'ì' to 'i', 'í' to 'i', 'î' to 'i', 'ï' to 'i',
        'ò' to 'o', 'ó' to 'o', 'ô' to 'o', 'õ' to 'o', 'ö' to 'o',
        'ù' to 'u', 'ú' to 'u', 'û' to 'u', 'ü' to 'u',
        'ñ' to 'n', 'ç' to 'c'
    )

    fun normalize(text: String): String {
        return text.lowercase().map { ACCENT_MAP[it] ?: it }.joinToString("").trim()
    }

    /**
     * Jaro similarity between two strings (0.0 = no match, 1.0 = exact match).
     */
    private fun jaro(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0

        val matchDist = maxOf(s1.length, s2.length) / 2 - 1
        val s1Matches = BooleanArray(s1.length)
        val s2Matches = BooleanArray(s2.length)
        var matches = 0

        for (i in s1.indices) {
            val start = maxOf(0, i - matchDist)
            val end = minOf(i + matchDist + 1, s2.length)
            for (j in start until end) {
                if (s2Matches[j] || s1[i] != s2[j]) continue
                s1Matches[i] = true
                s2Matches[j] = true
                matches++
                break
            }
        }

        if (matches == 0) return 0.0

        var transpositions = 0
        var k = 0
        for (i in s1.indices) {
            if (!s1Matches[i]) continue
            while (!s2Matches[k]) k++
            if (s1[i] != s2[k]) transpositions++
            k++
        }

        return (matches.toDouble() / s1.length +
                matches.toDouble() / s2.length +
                (matches - transpositions / 2.0) / matches) / 3.0
    }

    /**
     * Jaro-Winkler similarity. Boosts score for common prefixes (up to 4 chars).
     */
    fun jaroWinkler(s1: String, s2: String, prefixScale: Double = 0.1): Double {
        val jaro = jaro(s1, s2)
        var prefix = 0
        val maxPrefix = minOf(4, minOf(s1.length, s2.length))
        while (prefix < maxPrefix && s1[prefix] == s2[prefix]) prefix++
        return jaro + prefix * prefixScale * (1.0 - jaro)
    }

    /**
     * Compute an overall relevance score between a user query and a title.
     * Returns a value in [0.0, 1.0].
     */
    fun score(query: String, title: String): Double {
        val normQuery = normalize(query)
        val normTitle = normalize(title)

        if (normTitle.contains(normQuery)) return 1.0
        if (normQuery.contains(normTitle)) return 0.95

        val fullScore = jaroWinkler(normQuery, normTitle)

        // Word-level matching: useful for multi-word titles with partial typos
        val queryWords = normQuery.split("\\s+".toRegex()).filter { it.length > 2 }
        val titleWords = normTitle.split("\\s+".toRegex()).filter { it.length > 2 }

        if (queryWords.isEmpty() || titleWords.isEmpty()) return fullScore

        var wordMatchScore = 0.0
        var wordMatches = 0
        for (qWord in queryWords) {
            val bestMatch = titleWords.maxOfOrNull { jaroWinkler(qWord, it) } ?: 0.0
            if (bestMatch > 0.78) {
                wordMatchScore += bestMatch
                wordMatches++
            }
        }

        val wordRatio = wordMatches.toDouble() / queryWords.size
        val avgWordSim = if (wordMatches > 0) wordMatchScore / wordMatches else 0.0

        // Combined score: prefer word-level if good fraction of words matched
        return maxOf(fullScore, wordRatio * avgWordSim)
    }

    /**
     * Generate alternative search queries for a typo-prone input.
     * Strategy: use trigrams of the first significant word.
     */
    fun buildFallbackQuery(query: String): String? {
        val words = normalize(query).split("\\s+".toRegex()).filter { it.length > 3 }
        if (words.isEmpty()) return null
        // Take first 4 chars of longest word as fallback pivot
        val pivot = words.maxByOrNull { it.length } ?: return null
        return pivot.take(5)
    }
}
