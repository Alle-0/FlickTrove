package com.cinetrack.data.model

import com.google.firebase.firestore.PropertyName

data class EditorialCollection(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val backdropPath: String? = null,
    val posterPath: String? = null,
    val items: List<EditorialCollectionItem> = emptyList(),
    val createdAt: String = "",
    val keywords: List<String> = emptyList()
) {
    /**
     * Intelligent multilingual query matching:
     * 1. Substring / exact match on normalized title & description
     * 2. Comprehensive multilingual keyword & character alias matching
     * 3. Multi-word composite token matching (e.g. "signore anelli", "guerre stellari", "godzilla kong")
     * 4. Typo-tolerant fuzzy matching (score >= 0.82 for queries >= 4 chars)
     */
    fun matchesQuery(query: String): Boolean {
        if (query.isBlank()) return false
        val cleanQuery = com.cinetrack.ui.utils.FuzzySearch.normalize(query.trim())
        if (cleanQuery.isEmpty()) return false

        // 1. Direct or substring match on title & description
        val normTitle = com.cinetrack.ui.utils.FuzzySearch.normalize(title)
        if (normTitle.contains(cleanQuery) || cleanQuery.contains(normTitle)) return true

        val normDesc = com.cinetrack.ui.utils.FuzzySearch.normalize(description)
        if (normDesc.contains(cleanQuery)) return true

        // 2. Keyword exact, substring or reverse substring match
        val matchedKeyword = keywords.any { kw ->
            val normKw = com.cinetrack.ui.utils.FuzzySearch.normalize(kw)
            normKw == cleanQuery || normKw.contains(cleanQuery) || (cleanQuery.length >= 4 && cleanQuery.contains(normKw))
        }
        if (matchedKeyword) return true

        // 3. Word-level multi-token match: all significant words (len >= 3) match in title, desc or keywords
        val queryWords = cleanQuery.split("\\s+".toRegex()).filter { it.length >= 3 }
        if (queryWords.size >= 2) {
            val allWordsMatch = queryWords.all { qWord ->
                normTitle.contains(qWord) || normDesc.contains(qWord) || keywords.any { kw ->
                    com.cinetrack.ui.utils.FuzzySearch.normalize(kw).contains(qWord)
                }
            }
            if (allWordsMatch) return true
        }

        // 4. Fuzzy typo tolerance (Levenshtein / Jaro-Winkler) for queries >= 4 chars
        if (cleanQuery.length >= 4) {
            val fuzzyMatch = keywords.any { kw ->
                val normKw = com.cinetrack.ui.utils.FuzzySearch.normalize(kw)
                com.cinetrack.ui.utils.FuzzySearch.score(cleanQuery, normKw) >= 0.82
            }
            if (fuzzyMatch) return true
        }

        return false
    }
}

data class EditorialCollectionItem(
    @get:PropertyName("tmdbId") @set:PropertyName("tmdbId")
    var tmdbId: Long = 0,
    
    @get:PropertyName("mediaType") @set:PropertyName("mediaType")
    var mediaType: String = "",
    
    @get:PropertyName("timelineIndex") @set:PropertyName("timelineIndex")
    var timelineIndex: Int = 0,
    
    @get:PropertyName("addedAt") @set:PropertyName("addedAt")
    var addedAt: String = "",

    @get:PropertyName("seasonScope") @set:PropertyName("seasonScope")
    var seasonScope: String? = null
)
