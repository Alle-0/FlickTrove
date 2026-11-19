package com.cinetrack.data.repository.mood

/**
 * Data structures for query parsing, cinematic archetypes, and atmospheric modifiers
 * used in the Local Mood Search Engine.
 */

data class ParsedQueryIntent(
    val positiveText: String,
    val negatedText: String?,
    val negatedGenres: List<Long>,
    val negatedKeywords: List<Long>,
    val negatedTokens: List<String>
)

data class CinematicArchetype(
    val id: String,
    val nameIt: String,
    val nameEn: String,
    val triggersIt: List<String>,
    val triggersEn: List<String>,
    val primaryGenres: List<Long>,
    val secondaryGenres: List<Long> = emptyList(),
    val mandatoryKeywords: List<Long> = emptyList(),
    val secondaryKeywords: List<Long> = emptyList(),
    val negativeGenres: List<Long> = emptyList(),
    val anchorMovieIds: List<Long> = emptyList(),
    val rationaleTemplateIt: String,
    val rationaleTemplateEn: String
)

data class AtmosphericModifier(
    val id: String,
    val labelIt: String,
    val labelEn: String,
    val triggersIt: List<String>,
    val triggersEn: List<String>,
    val keywordIds: List<Long> = emptyList(),
    val genreBonus: List<Long> = emptyList(),
    val genrePenalty: List<Long> = emptyList()
)
