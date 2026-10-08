package com.cinetrack.domain

import com.cinetrack.data.model.Movie
import javax.inject.Inject
import kotlin.math.pow

data class UserGenreProfile(
    val genreAffinities: Map<Long, Float>,
    val maxAffinity: Float,
    val genreCounts: Map<Long, Int> = emptyMap(),
    val genreFrequencies: Map<Long, Float> = emptyMap(),
    val genrePairAffinities: Map<Long, Float> = emptyMap(),
    val negativeGenreScores: Map<Long, Float> = emptyMap(),
    val decadeCounts: Map<Int, Int> = emptyMap(),
    val totalWatchedCount: Int = 0,
    val averageUserRating: Float = 7.0f
) {
    fun getGenreCount(genreId: Long): Int = genreCounts[genreId] ?: 0
    fun getDecadeCount(decade: Int): Int = decadeCounts[decade] ?: 0
    val animationRatio: Float
        get() = genreFrequencies[16L] ?: 0f
}

/**
 * Advanced Netflix-Grade Recommendation Engine
 *
 * Implements:
 * 1. Multi-Vector Taste Profile with Recency Decay (Half-Life weighting).
 * 2. Micro-Cluster Synergy: Combinatorial Genre Pairs ("Action+Sci-Fi" vs "Action+Comedy").
 * 3. Repulsive Negative Vectors: Active damping for genres with low user ratings or dropped shows.
 * 4. Bayesian Weighted Quality (IMDb/Rotten Tomatoes Bayesian Mean Score).
 * 5. Organic Niche-Format Damping: Generalized TF-IDF penalty for unwatched formats (Animation, Documentary, Western, Musical)
 *    without arbitrary hardcoded exceptions.
 * 6. Era / Decade Affinity Modulation.
 */
class CalculateMatchScoreUseCase @Inject constructor() {

    // Niche/Format genres on TMDB that represent distinctive visual/stylistic mediums rather than universal themes
    private val nicheFormatGenres = setOf(
        16L,     // Animation
        99L,     // Documentary
        10402L,  // Music / Musical
        37L,     // Western
        10770L   // TV Movie
    )

    fun buildUserProfile(localMovies: List<Movie>): UserGenreProfile? {
        val watchedMovies = localMovies.filter { it.watched || it.favorite || (it.personalRating ?: 0.0) > 0.0 }
        if (watchedMovies.size < 3) return null

        val genreScores = mutableMapOf<Long, Float>()
        val genreCounts = mutableMapOf<Long, Int>()
        val negativeScores = mutableMapOf<Long, Float>()
        val pairScores = mutableMapOf<Long, Float>()
        val pairCounts = mutableMapOf<Long, Int>()
        val decadeCounts = mutableMapOf<Int, Int>()
        var totalRatingSum = 0f
        var ratedMoviesCount = 0

        val nowEpoch = System.currentTimeMillis()

        // 1. Process all watched titles with Temporal Decay and Negative/Positive Split
        watchedMovies.forEach { m ->
            val rating = m.personalRating
            if (rating != null && rating > 0) {
                totalRatingSum += rating.toFloat()
                ratedMoviesCount++
            }

            // Recency Half-Life Decay: recent watches (past 6 months) have higher momentum
            val recencyMultiplier = run {
                val watchedTime = m.watchedAt?.let { parseEpoch(it) } ?: m.clientUpdatedAt.takeIf { it > 0 }
                if (watchedTime != null && watchedTime > 0) {
                    val daysAgo = ((nowEpoch - watchedTime) / (1000L * 60L * 60L * 24L)).coerceAtLeast(0L)
                    // Half-life of 240 days (~8 months), bounded between 0.55 and 1.15
                    (1.15f * 0.5f.pow(daysAgo / 240f)).coerceIn(0.55f, 1.15f)
                } else 0.85f
            }

            val isDisliked = rating != null && rating in 1.0..5.0
            val isDropped = m.dropped

            val ratingWeight = when {
                isDropped -> -0.8f
                rating != null && rating >= 8.5 -> ((rating.toFloat() - 6.0f) / 3.5f) * 1.35f
                rating != null && rating >= 6.5 -> ((rating.toFloat() - 5.5f) / 4.0f)
                rating != null && rating > 0 -> ((rating.toFloat() - 6.0f) / 4.0f) // negative if < 6
                m.favorite -> 1.25f
                else -> 0.35f
            } * recencyMultiplier

            val movieGenreIds = (m.genres?.map { it.id } ?: m.genreIds ?: emptyList()).distinct()

            movieGenreIds.forEach { id ->
                if (isDisliked || isDropped) {
                    negativeScores[id] = (negativeScores[id] ?: 0f) + (if (isDropped) 0.9f else (6.0f - (rating?.toFloat() ?: 5f)) * 0.4f)
                } else {
                    genreScores[id] = (genreScores[id] ?: 0f) + ratingWeight
                }
                genreCounts[id] = (genreCounts[id] ?: 0) + 1
            }

            // Micro-Clusters: Track 2-genre combinations for taste synergies
            if (movieGenreIds.size >= 2 && !isDisliked && !isDropped && ratingWeight > 0f) {
                for (i in 0 until movieGenreIds.size) {
                    for (j in i + 1 until movieGenreIds.size) {
                        val minId = if (movieGenreIds[i] < movieGenreIds[j]) movieGenreIds[i] else movieGenreIds[j]
                        val maxId = if (movieGenreIds[i] > movieGenreIds[j]) movieGenreIds[i] else movieGenreIds[j]
                        val pairKey = (minId.toLong() shl 32) or (maxId.toLong() and 0xFFFFFFFFL)
                        pairScores[pairKey] = (pairScores[pairKey] ?: 0f) + ratingWeight
                        pairCounts[pairKey] = (pairCounts[pairKey] ?: 0) + 1
                    }
                }
            }

            // Decade tracking
            extractYear(m)?.let { year ->
                val decade = (year / 10) * 10
                decadeCounts[decade] = (decadeCounts[decade] ?: 0) + 1
            }
        }

        if (genreScores.isEmpty()) return null

        val totalWatched = watchedMovies.size
        val avgUserRating = if (ratedMoviesCount > 0) totalRatingSum / ratedMoviesCount else 7.0f

        // 2. Bayesian Genre Affinity with Shrinkage
        val genreAffinities = genreScores.mapValues { (id, totalScore) ->
            val count = genreCounts[id] ?: 1
            val rawAvg = totalScore / count
            // Saturation curve: confidence increases with volume (1 movie = 25%, 3 = 50%, 9 = 75%)
            val confidence = count.toFloat() / (count + 3f)
            (rawAvg * confidence).coerceAtLeast(0f)
        }

        // Relative Genre Frequencies (TF-IDF vector)
        val genreFrequencies = genreCounts.mapValues { (_, count) ->
            count.toFloat() / totalWatched.toFloat()
        }

        // Micro-Cluster Normalized Affinities
        val pairAffinities = pairScores.mapValues { (pairKey, score) ->
            val count = pairCounts[pairKey] ?: 1
            val confidence = count.toFloat() / (count + 2f)
            (score / count) * confidence
        }

        // Guilty Pleasure Protection (Engagement > Low Rating)
        // If a user watches a lot of a genre (e.g. > 12%), it's a guilty pleasure. We bypass repulsion.
        val adjustedNegativeScores = negativeScores.mapValues { (id, score) ->
            val freq = genreFrequencies[id] ?: 0f
            when {
                freq >= 0.12f -> 0f       // Pure guilty pleasure, completely remove repulsion
                freq >= 0.08f -> score * 0.4f // Partial mitigation
                else -> score
            }
        }.filterValues { it > 0f }

        val maxAffinity = genreAffinities.values.maxOrNull()?.coerceAtLeast(0.1f) ?: 1f

        return UserGenreProfile(
            genreAffinities = genreAffinities,
            maxAffinity = maxAffinity,
            genreCounts = genreCounts,
            genreFrequencies = genreFrequencies,
            genrePairAffinities = pairAffinities,
            negativeGenreScores = adjustedNegativeScores,
            decadeCounts = decadeCounts,
            totalWatchedCount = totalWatched,
            averageUserRating = avgUserRating
        )
    }

    fun calculateScore(currentMovie: Movie, profile: UserGenreProfile?): Int? {
        // 1. Direct Personal Rating: 100% ground truth if user explicitly rated it
        val personalRating = currentMovie.personalRating
        if (personalRating != null && personalRating > 0) {
            val personalScore = (personalRating * 10f).toInt()
            return personalScore.coerceIn(10, 99)
        }

        if (profile == null) return null

        val currentMovieGenreIds = (currentMovie.genres?.map { it.id } ?: currentMovie.genreIds ?: emptyList()).distinct()

        // 2. Core Genre Affinity: Blended Max Affinity (65%) + Average Affinity (35%)
        var coreMatchScore = 0f
        if (currentMovieGenreIds.isNotEmpty()) {
            var maxGenreAffinity = 0f
            var sumGenreAffinity = 0f
            currentMovieGenreIds.forEach { id ->
                val aff = profile.genreAffinities[id] ?: 0f
                if (aff > maxGenreAffinity) maxGenreAffinity = aff
                sumGenreAffinity += aff
            }
            val avgGenreAffinity = sumGenreAffinity / currentMovieGenreIds.size
            val blendedAffinity = (maxGenreAffinity * 0.65f) + (avgGenreAffinity * 0.35f)

            if (blendedAffinity > 0f) {
                // Scaled up to 36 points
                coreMatchScore = ((blendedAffinity / profile.maxAffinity) * 36f).coerceIn(0f, 36f)
            }
        }

        // 3. Micro-Cluster Synergy: Bonus for recognized genre pairs (up to +8 points)
        var pairSynergyBonus = 0f
        if (currentMovieGenreIds.size >= 2) {
            var highestPairScore = 0f
            for (i in 0 until currentMovieGenreIds.size) {
                for (j in i + 1 until currentMovieGenreIds.size) {
                    val minId = if (currentMovieGenreIds[i] < currentMovieGenreIds[j]) currentMovieGenreIds[i] else currentMovieGenreIds[j]
                    val maxId = if (currentMovieGenreIds[i] > currentMovieGenreIds[j]) currentMovieGenreIds[i] else currentMovieGenreIds[j]
                    val key = (minId shl 32) or (maxId and 0xFFFFFFFFL)
                    val pairAffinity = profile.genrePairAffinities[key] ?: 0f
                    if (pairAffinity > highestPairScore) {
                        highestPairScore = pairAffinity
                    }
                }
            }
            if (highestPairScore > 0f) {
                pairSynergyBonus = ((highestPairScore / profile.maxAffinity) * 8f).coerceIn(0f, 8f)
            }
        }

        // 4. Repulsive Forces: Penalties from disliked or dropped content
        var repulsionPenalty = 0f
        if (currentMovieGenreIds.isNotEmpty()) {
            var totalRepulsion = 0f
            currentMovieGenreIds.forEach { id ->
                val penalty = profile.negativeGenreScores[id]
                if (penalty != null) totalRepulsion += penalty
            }
            if (totalRepulsion > 0f) {
                repulsionPenalty = (totalRepulsion * 3.5f).coerceIn(0f, 18f)
            }
        }

        // 5. Organic Niche-Format Damping (TF-IDF based):
        // Automatically protects against dominating niche formats (Animation, Documentary, Western, Musical)
        // when the user has minimal or zero history with that format.
        var formatDamping = 0f
        if (profile.totalWatchedCount >= 6) {
            currentMovieGenreIds.forEach { nicheGenreId ->
                if (nicheGenreId !in nicheFormatGenres) return@forEach
                val freq = profile.genreFrequencies[nicheGenreId] ?: 0f
                val penaltyForNiche = when {
                    freq == 0f -> 35f          // Zero history: full damping
                    freq < 0.04f -> 25f        // Extremely rare (e.g. 1 in 30): strong damping
                    freq < 0.08f -> 14f        // Occasional (e.g. 1 in 15): moderate damping
                    else -> 0f                 // Genuine interest: no penalty
                }
                if (penaltyForNiche > formatDamping) {
                    formatDamping = penaltyForNiche
                }
            }
        }

        // 6. Era / Decade Modulation (Decade Affinity)
        var eraBonus = 0f
        val candidateYear = extractYear(currentMovie)
        val totalWithDecade = profile.decadeCounts.values.sum()
        if (candidateYear != null && totalWithDecade >= 5) {
            val candidateDecade = (candidateYear / 10) * 10
            val countInDecade = profile.getDecadeCount(candidateDecade)
            val decadeRatio = countInDecade.toFloat() / totalWithDecade
            val minDecadeDiff = if (profile.decadeCounts.isNotEmpty()) {
                profile.decadeCounts.keys.minOfOrNull { kotlin.math.abs(it - candidateDecade) } ?: 0
            } else 0

            eraBonus = when {
                decadeRatio >= 0.25f -> 4.5f
                decadeRatio >= 0.10f -> 2.5f
                decadeRatio > 0f -> 1.0f
                minDecadeDiff >= 30 && profile.totalWatchedCount >= 15 -> -4.0f
                minDecadeDiff >= 20 && profile.totalWatchedCount >= 15 -> -2.0f
                else -> 0f
            }
        }

        // 7. Bayesian True Quality Score (IMDb Weighted Mean Formula)
        // Eliminates artificial 10.0 ratings with 2 votes while rewarding universally praised films
        val rawTmdbRating = currentMovie.voteAverage ?: 0.0
        val voteCount = currentMovie.voteCount ?: 0

        val effectiveRating = if (voteCount > 0 && rawTmdbRating > 0.0) {
            val priorVotes = 45.0 // Confidence threshold
            val priorMean = 6.4   // Neutral global TMDB mean
            ((voteCount * rawTmdbRating) + (priorVotes * priorMean)) / (voteCount + priorVotes)
        } else if (rawTmdbRating > 0.0) {
            (rawTmdbRating + (6.4 * 3.0)) / 4.0
        } else {
            0.0
        }

        // Base quality score accounts for up to 48 points
        val baseScore = ((effectiveRating / 8.6) * 48.0).toFloat().coerceIn(0f, 48f)

        // Composite calculation
        val totalPositive = baseScore + coreMatchScore + pairSynergyBonus + eraBonus
        val totalNegative = repulsionPenalty + formatDamping
        val finalScore = (totalPositive - totalNegative).toInt()

        // 8. Predictive Cap at 95%: Scores 96%-99% reserved for user's personal 10/10 ratings
        return finalScore.coerceIn(10, 95)
    }

    operator fun invoke(currentMovie: Movie, localMovies: List<Movie>): Int? {
        val personalRating = currentMovie.personalRating
        if (personalRating != null && personalRating > 0) {
            return (personalRating * 10f).toInt().coerceIn(10, 99)
        }
        val profile = buildUserProfile(localMovies) ?: return null
        return calculateScore(currentMovie, profile)
    }

    private fun extractYear(movie: Movie): Int? {
        val yearStr = movie.releaseYear
            ?: movie.releaseDate?.take(4)
            ?: movie.firstAirDate?.take(4)
        return yearStr?.toIntOrNull()?.takeIf { it in 1880..2100 }
    }

    private fun parseEpoch(dateStr: String): Long? {
        return try {
            java.time.Instant.parse(dateStr).toEpochMilli()
        } catch (_: Exception) {
            try {
                java.time.LocalDate.parse(dateStr.take(10)).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
            } catch (_: Exception) {
                null
            }
        }
    }
}