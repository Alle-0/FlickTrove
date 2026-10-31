package com.cinetrack.domain

import com.cinetrack.data.model.Movie
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.roundToInt

data class UserGenreProfile(
    val genreAffinities: Map<Long, Float>,
    val maxAffinity: Float,
    val genreCounts: Map<Long, Int> = emptyMap(),
    val genreFrequencies: Map<Long, Float> = emptyMap(),
    val genrePairAffinities: Map<Long, Float> = emptyMap(),
    val negativeGenreScores: Map<Long, Float> = emptyMap(),
    val decadeCounts: Map<Int, Int> = emptyMap(),
    val countryFrequencies: Map<String, Float> = emptyMap(),
    val directorAffinities: Map<Long, Float> = emptyMap(),
    val actorAffinities: Map<Long, Float> = emptyMap(),
    val isIndieSeeker: Boolean = false,
    val totalWatchedCount: Int = 0,
    val averageUserRating: Float = 7.0f
) {
    fun getGenreCount(genreId: Long): Int = genreCounts[genreId] ?: 0
    fun getDecadeCount(decade: Int): Int = decadeCounts[decade] ?: 0
    val animationRatio: Float
        get() = genreFrequencies[16L] ?: 0f
}

/**
 * Advanced Netflix-Grade Recommendation Engine (v3 - Multi-Vector Taste Matrix)
 *
 * Implements:
 * 1. Multi-Vector Taste Profile with Recency Decay (Half-Life weighting).
 * 2. Director & Star Auteur Boost: Learns favorite directors and actors from library history (up to +8 pts).
 * 3. Implicit Watchlist Intent: Short-term mood boost from recent watchlist additions (half-life of 21 days).
 * 4. Micro-Cluster Synergy: Combinatorial Genre Pairs ("Action+Sci-Fi" vs "Action+Comedy").
 * 5. Repulsive Negative Vectors: Active damping for genres with low user ratings or dropped shows.
 * 6. Arthouse vs Blockbuster Calibration: Rewarding indie gems (80..2500 votes, rating >= 7.2) for cinephiles (up to +4 pts).
 * 7. Bayesian Weighted Quality (IMDb/Rotten Tomatoes Bayesian Mean Score).
 * 8. Continuous Lerp Curves (Anti-Cliff): Eliminating integer step jumps in Country, Decade, and Niche Damping to prevent mass ties.
 * 9. Era / Decade Affinity Modulation.
 */
class CalculateMatchScoreUseCase @Inject constructor() {

    // Niche/Format genres on TMDB that represent distinctive visual/stylistic mediums rather than universal themes
    private val nicheFormatGenres = setOf(
        16L,     // Animation
        99L,     // Documentary
        10402L,  // Music / Musical
        37L,     // Western
        10770L,  // TV Movie
        10762L,  // Kids (TV)
        10764L,  // Reality (TV)
        10766L,  // Soap (TV)
        10767L   // Talk (TV)
    )

    fun buildUserProfile(localMovies: List<Movie>): UserGenreProfile? {
        val watchedMovies = localMovies.filter {
            it.watched || it.favorite || it.dropped || (it.personalRating ?: 0.0) > 0.0
        }
        if (watchedMovies.size < 3) return null

        val genreScores = mutableMapOf<Long, Float>()
        val genreCounts = mutableMapOf<Long, Int>()
        val negativeScores = mutableMapOf<Long, Float>()
        val pairScores = mutableMapOf<Long, Float>()
        val pairCounts = mutableMapOf<Long, Int>()
        val decadeCounts = mutableMapOf<Int, Int>()
        val countryCounts = mutableMapOf<String, Int>()
        val directorScores = mutableMapOf<Long, Float>()
        val directorCounts = mutableMapOf<Long, Int>()
        val actorScores = mutableMapOf<Long, Float>()
        val actorCounts = mutableMapOf<Long, Int>()

        val nowEpoch = System.currentTimeMillis()

        // Pre-calcolo della media voti reale dell'utente per calibrare la severità
        val ratedList = watchedMovies.mapNotNull { it.personalRating?.toFloat()?.takeIf { r -> r > 0f } }
        val avgUserRating = if (ratedList.isNotEmpty()) ratedList.average().toFloat() else 7.0f
        // Pivot neutro dinamico: 6.0 di default, ma scende per i critici severi e sale per i votanti generosi
        val neutralPivot = (avgUserRating - 0.8f).coerceIn(5.2f, 6.6f)

        // 1. Process all watched titles with Temporal Decay and Negative/Positive Split
        watchedMovies.forEach { m ->
            val rating = m.personalRating?.toFloat()

            // Recency Half-Life Decay: recent watches (past 6-8 months) have higher momentum
            val recencyMultiplier = run {
                val watchedTime = m.watchedAt?.let { parseEpoch(it) } ?: m.clientUpdatedAt.takeIf { it > 0 }
                if (watchedTime != null && watchedTime > 0) {
                    val daysAgo = ((nowEpoch - watchedTime) / (1000L * 60L * 60L * 24L)).coerceAtLeast(0L)
                    // Half-life of 240 days (~8 months), bounded between 0.55 and 1.15
                    (1.15f * 0.5f.pow(daysAgo / 240f)).coerceIn(0.55f, 1.15f)
                } else 0.85f
            }

            val isDisliked = rating != null && rating in 1.0f..5.0f
            val isDropped = m.dropped

            val baseWeight = when {
                isDropped -> -0.8f
                rating != null && rating >= 8.5f -> ((rating - neutralPivot) / 3.5f) * 1.35f
                rating != null && rating >= 6.5f -> ((rating - (neutralPivot - 0.3f)) / 4.0f)
                rating != null && rating > 0f -> ((rating - neutralPivot) / 4.0f)
                m.favorite -> 1.15f
                else -> 0.35f
            }
            // Il cuore dei Preferiti ora aggiunge un boost reale anche ai film già votati!
            val favoriteBonus = if (m.favorite && rating != null && !isDropped) 0.25f else 0f
            val ratingWeight = (baseWeight + favoriteBonus) * recencyMultiplier

            val movieGenreIds = (m.genres?.map { it.id } ?: m.genreIds ?: emptyList()).distinct()

            movieGenreIds.forEach { id ->
                if (isDisliked || isDropped) {
                    val rawPenalty = if (isDropped) 0.9f else (6.0f - (rating ?: 5f)) * 0.4f
                    negativeScores[id] = (negativeScores[id] ?: 0f) + (rawPenalty * recencyMultiplier)
                    if (!isDropped) genreCounts[id] = (genreCounts[id] ?: 0) + 1
                } else {
                    genreScores[id] = (genreScores[id] ?: 0f) + ratingWeight
                    genreCounts[id] = (genreCounts[id] ?: 0) + 1
                }
            }

            // Micro-Clusters: Track 2-genre combinations for taste synergies
            if (movieGenreIds.size >= 2 && !isDisliked && !isDropped && ratingWeight > 0f) {
                for (i in 0 until movieGenreIds.size) {
                    for (j in i + 1 until movieGenreIds.size) {
                        val minId = minOf(movieGenreIds[i], movieGenreIds[j])
                        val maxId = maxOf(movieGenreIds[i], movieGenreIds[j])
                        val pairKey = (minId shl 32) or (maxId and 0xFFFFFFFFL)
                        pairScores[pairKey] = (pairScores[pairKey] ?: 0f) + ratingWeight
                        pairCounts[pairKey] = (pairCounts[pairKey] ?: 0) + 1
                    }
                }
            }

            // Decade tracking: solo per i titoli non odiati e non droppati!
            if (!isDisliked && !isDropped) {
                extractYear(m)?.let { year ->
                    val decade = (year / 10) * 10
                    decadeCounts[decade] = (decadeCounts[decade] ?: 0) + 1
                }
            }

            // Origin Country tracking: solo per i titoli non odiati e non droppati!
            if (!isDisliked && !isDropped) {
                val countries = m.originCountry?.takeIf { it.isNotEmpty() }
                    ?: com.cinetrack.data.mapper.MovieMapper.fallbackCountryFromLanguage(m.originalLanguage)
                countries?.forEach { c ->
                    val code = c.trim().uppercase()
                    if (code.isNotEmpty()) {
                        countryCounts[code] = (countryCounts[code] ?: 0) + 1
                    }
                }
            }

            // Vettore 1: Autore & Cast Affinity (Director & Top Cast)
            if (!isDisliked && !isDropped && ratingWeight > 0f) {
                val dirId = m.directorId
                if (dirId != null && dirId > 0L) {
                    directorScores[dirId] = (directorScores[dirId] ?: 0f) + ratingWeight
                    directorCounts[dirId] = (directorCounts[dirId] ?: 0) + 1
                }
                m.topCastData?.take(3)?.forEach { actor ->
                    if (actor.id > 0L && actor.name.isNotBlank()) {
                        actorScores[actor.id] = (actorScores[actor.id] ?: 0f) + (ratingWeight * 0.7f)
                        actorCounts[actor.id] = (actorCounts[actor.id] ?: 0) + 1
                    }
                }
            }
        }

        // Vettore 2: Segnale di Intento Implicito dalla Watchlist ("Mood" a Breve Termine)
        // Analizza gli ultimi 20 titoli aggiunti in watchlist non ancora visti e non droppati
        val watchlistMovies = localMovies.filter { !it.watched && !it.dropped }
            .sortedByDescending { it.clientUpdatedAt.takeIf { t -> t > 0L } ?: (parseEpoch(it.createdAt ?: "") ?: 0L) }
            .take(20)

        watchlistMovies.forEach { m ->
            val addedTime = m.clientUpdatedAt.takeIf { it > 0L } ?: parseEpoch(m.createdAt ?: "")
            val intentRecency = if (addedTime != null && addedTime > 0L) {
                val daysAgo = ((nowEpoch - addedTime) / (1000L * 60L * 60L * 24L)).coerceAtLeast(0L)
                // Emivita rapida di 21 giorni (3 settimane)
                (0.5f.pow(daysAgo / 21f)).coerceIn(0.05f, 1.0f)
            } else 0.5f

            val intentWeight = 0.18f * intentRecency
            val movieGenreIds = (m.genres?.map { it.id } ?: m.genreIds ?: emptyList()).distinct()
            movieGenreIds.forEach { id ->
                genreScores[id] = (genreScores[id] ?: 0f) + intentWeight
            }
            // Pair synergy per la watchlist
            if (movieGenreIds.size >= 2) {
                for (i in 0 until movieGenreIds.size) {
                    for (j in i + 1 until movieGenreIds.size) {
                        val minId = minOf(movieGenreIds[i], movieGenreIds[j])
                        val maxId = maxOf(movieGenreIds[i], movieGenreIds[j])
                        val pairKey = (minId shl 32) or (maxId and 0xFFFFFFFFL)
                        pairScores[pairKey] = (pairScores[pairKey] ?: 0f) + intentWeight
                    }
                }
            }
        }

        if (genreScores.isEmpty()) return null

        // Esclude i titoli droppati dal denominatore per allinearsi a genreCounts
        val totalCompleted = watchedMovies.count { !it.dropped }.coerceAtLeast(1)

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
            count.toFloat() / totalCompleted.toFloat()
        }

        // Relative Country Frequencies
        val countryFrequencies = countryCounts.mapValues { (_, count) ->
            count.toFloat() / totalCompleted.toFloat()
        }

        // Micro-Cluster Normalized Affinities
        val pairAffinities = pairScores.mapValues { (pairKey, score) ->
            val count = pairCounts[pairKey] ?: 1
            val confidence = count.toFloat() / (count + 2f)
            (score / count) * confidence
        }

        // Director & Star Normalized Affinities with Shrinkage
        val directorAffinities = directorScores.mapValues { (dirId, score) ->
            val count = directorCounts[dirId] ?: 1
            val confidence = count.toFloat() / (count + 1.5f)
            (score / count) * confidence
        }

        val actorAffinities = actorScores.mapValues { (actId, score) ->
            val count = actorCounts[actId] ?: 1
            val confidence = count.toFloat() / (count + 2.0f)
            (score / count) * confidence
        }

        // Vettore 3: Indice Arthouse vs Blockbuster (Mediana voteCount sui film amati con voto >= 8.0)
        val highRatedVotes = watchedMovies.filter {
            val r = it.personalRating ?: 0.0
            r >= 8.0 || (it.favorite && r >= 7.0)
        }.mapNotNull { it.voteCount?.takeIf { v -> v > 30 } }

        val isIndieSeeker = if (highRatedVotes.size >= 3) {
            val sortedVotes = highRatedVotes.sorted()
            val medianVote = sortedVotes[sortedVotes.size / 2]
            medianVote <= 3500
        } else false

        // Guilty Pleasure Protection (Engagement > Low Rating)
        // If a user watches a lot of a genre (e.g. >= 12% and >= 4 titles), it's a guilty pleasure. We bypass repulsion.
        val adjustedNegativeScores = negativeScores.mapValues { (id, score) ->
            val freq = genreFrequencies[id] ?: 0f
            val count = genreCounts[id] ?: 0
            when {
                count >= 4 && freq >= 0.12f -> 0f           // Pure guilty pleasure, completely remove repulsion
                count >= 3 && freq >= 0.08f -> score * 0.4f // Partial mitigation
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
            countryFrequencies = countryFrequencies,
            directorAffinities = directorAffinities,
            actorAffinities = actorAffinities,
            isIndieSeeker = isIndieSeeker,
            totalWatchedCount = totalCompleted,
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
            var lowAffinityCount = 0
            currentMovieGenreIds.forEach { id ->
                val aff = profile.genreAffinities[id] ?: 0f
                if (aff > maxGenreAffinity) maxGenreAffinity = aff
                sumGenreAffinity += aff
                if (aff < profile.maxAffinity * 0.15f) {
                    lowAffinityCount++
                }
            }
            val avgGenreAffinity = sumGenreAffinity / currentMovieGenreIds.size

            // Multi-Genre Saturation ("Minestrone Effect"):
            val isMinestrone = currentMovieGenreIds.size >= 4 && lowAffinityCount > (currentMovieGenreIds.size / 2)
            val maxWeight = if (isMinestrone) 0.45f else 0.65f
            val avgWeight = if (isMinestrone) 0.55f else 0.35f

            val blendedAffinity = (maxGenreAffinity * maxWeight) + (avgGenreAffinity * avgWeight)

            if (blendedAffinity > 0f) {
                // Scaled up to 50 points (prioritizing personal taste over global rating)
                coreMatchScore = ((blendedAffinity / profile.maxAffinity) * 50f).coerceIn(0f, 50f)
            }
        }

        // 3. Micro-Cluster Synergy: Bonus for recognized genre pairs (up to +10 points)
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
                pairSynergyBonus = ((highestPairScore / profile.maxAffinity) * 10f).coerceIn(0f, 10f)
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

        // 5. Upgrade 4: Organic Niche-Format Damping (Curva Continua senza scalini)
        // Automatically protects against dominating niche formats (Animation, Documentary, Western, Musical)
        // Curva continua convessa: da -35pt (allo 0%) fino a 0pt (all'8% di frequenza)
        var formatDamping = 0f
        if (profile.totalWatchedCount >= 6) {
            currentMovieGenreIds.forEach { nicheGenreId ->
                if (nicheGenreId !in nicheFormatGenres) return@forEach
                val freq = profile.genreFrequencies[nicheGenreId] ?: 0f
                val penaltyForNiche = if (freq < 0.08f) {
                    35f * (1f - (freq / 0.08f)).pow(1.4f)
                } else 0f
                if (penaltyForNiche > formatDamping) {
                    formatDamping = penaltyForNiche
                }
            }
        }

        // 6. Upgrade 4: Era / Decade Modulation (Curva Continua Lerp)
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
                decadeRatio > 0f -> (decadeRatio / 0.28f * 6.0f).coerceIn(0.5f, 6.0f)
                minDecadeDiff >= 20 && profile.totalWatchedCount >= 15 ->
                    -((minDecadeDiff - 15f) / 20f * 4.0f).coerceIn(1.0f, 4.0f)
                else -> 0f
            }
        }

        // 7. Upgrade 4: Country / Cultural Affinity Bonus (Curva Continua Lerp)
        var countryBonus = 0f
        val rawCountries = currentMovie.originCountry?.takeIf { it.isNotEmpty() }
            ?: com.cinetrack.data.mapper.MovieMapper.fallbackCountryFromLanguage(currentMovie.originalLanguage)
        val candidateCountries = rawCountries?.map { it.trim().uppercase() } ?: emptyList()
        if (candidateCountries.isNotEmpty() && profile.totalWatchedCount >= 5) {
            val maxFreq = candidateCountries
                .filter { it != "US" }
                .maxOfOrNull { profile.countryFrequencies[it] ?: 0f } ?: 0f

            countryBonus = if (maxFreq > 0.05f) {
                (((maxFreq - 0.05f) / 0.30f) * 5.0f).coerceIn(0f, 5.0f)
            } else 0f
        }

        // 8. Upgrade 1: Il Vettore "Autore & Star" (Auteur / Cast Boost fino a +8 pt)
        var auteurBoost = 0f
        val dirAff = currentMovie.directorId?.let { profile.directorAffinities[it] } ?: 0f
        val bestActorAff = currentMovie.topCastData?.take(3)?.mapNotNull { profile.actorAffinities[it.id] }?.maxOrNull() ?: 0f
        if (dirAff > 0f || bestActorAff > 0f) {
            val dirComponent = (dirAff / 0.5f) * 6.0f
            val actorComponent = (bestActorAff / 0.5f) * 4.0f
            auteurBoost = (dirComponent + actorComponent).coerceIn(0f, 8.0f)
        }

        // 9. Bayesian True Quality Score (IMDb Weighted Mean Formula)
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

        val baseScore = ((effectiveRating / 8.6) * 30.0).toFloat().coerceIn(0f, 30f)

        // 10. Upgrade 3: Indice "Arthouse vs Blockbuster" (Indie Gem Boost fino a +4 pt)
        val indieGemBonus = if (profile.isIndieSeeker && voteCount in 80..2500 && rawTmdbRating >= 7.2) {
            (((rawTmdbRating.toFloat() - 7.2f) / 1.6f) * 4.0f).coerceIn(1.5f, 4.0f)
        } else 0f

        // Composite calculation con arrotondamento finale (Zero pareggi di massa)
        val totalPositive = baseScore + coreMatchScore + pairSynergyBonus + eraBonus + countryBonus + auteurBoost + indieGemBonus
        val totalNegative = repulsionPenalty + formatDamping
        val finalScore = (totalPositive - totalNegative).roundToInt()

        // Predictive Cap at 95%: Scores 96%-99% reserved for user's personal 10/10 ratings
        return finalScore.coerceIn(10, 95)
    }

    operator fun invoke(currentMovie: Movie, localMovies: List<Movie>): Int? {
        val personalRating = currentMovie.personalRating
        if (personalRating != null && personalRating > 0) {
            return (personalRating * 10f).toInt().coerceIn(10, 99)
        }
        val isTv = currentMovie.mediaType == "tv"
        val filteredMovies = localMovies.filter { 
            if (isTv) it.mediaType == "tv" else it.mediaType != "tv" 
        }
        val profile = buildUserProfile(filteredMovies) ?: buildUserProfile(localMovies) ?: return null
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
            if (dateStr.length > 10 && dateStr.contains('T')) {
                java.time.Instant.parse(dateStr).toEpochMilli()
            } else {
                java.time.LocalDate.parse(dateStr.take(10))
                    .atStartOfDay(java.time.ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
            }
        } catch (_: Exception) {
            null
        }
    }
}
