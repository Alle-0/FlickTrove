package com.cinetrack.data.repository

import android.content.Context
import com.cinetrack.R
import com.cinetrack.data.api.SemanticSearchApi
import com.cinetrack.data.api.TMDBSearchResult
import com.cinetrack.data.model.GenreConstants
import com.cinetrack.util.TranslationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SemanticSearchRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: SemanticSearchApi,
    private val localEngine: LocalMoodSearchEngine,
    private val translationManager: TranslationManager
) {

    // LruCache in RAM per la sessione: 20 ricerche istantanee a costo 0ms
    private val sessionCache = android.util.LruCache<String, List<TMDBSearchResult.MovieResult>>(20)

    suspend fun search(
        query: String,
        language: String = java.util.Locale.getDefault().toLanguageTag(),
        limit: Int = 100
    ): List<TMDBSearchResult.MovieResult> {
        val rawQuery = query.trim().take(500)
        if (rawQuery.isBlank()) return emptyList()

        val langCode = language.take(2).lowercase()
        // La lingua dei titoli e delle label rispetta la preferenza lingua utente/sistema
        val isItalian = langCode == "it"

        // 1. Fuzzy Typo-Correction a monte (Punto 1: Filtro anti-analfabetismo Levenshtein)
        // Corregge "viagio" -> "viaggio", "pscicologica" -> "psicologica", ecc.
        val sanitizedQuery = localEngine.correctQueryTypos(rawQuery, language)

        val cacheKey = "$sanitizedQuery|$langCode|$isItalian"
        sessionCache.get(cacheKey)?.let { cached ->
            return cached
        }

        // 2. Risoluzione Intento & Negazioni (Punto 2)
        // Estraiamo la parte positiva e i token negati (es. "film nello spazio senza alieni" -> positive: "film nello spazio", negati: ["alieni"])
        val parsedIntent = localEngine.parseIntent(sanitizedQuery, language)
        val positiveText = parsedIntent.positiveText.ifBlank { sanitizedQuery }
        val negatedTokens = parsedIntent.negatedTokens

        // Normalizzazione idiomatica per prevenire allucinazioni semantiche dei vettori
        // (es. "isola deserta" -> "isola disabitata naufrago castaway" per non matchare film sul deserto come Sahara)
        val normalizedPositiveText = localEngine.normalizeIdiomaticExpressions(positiveText, language)

        // Rilevamento concetti chiave e generi attesi per l'Hard Gate di Genere
        val expectedGenres = localEngine.extractQueryGenreIds(normalizedPositiveText, language)
        val matchedArchetype = localEngine.detectArchetype(positiveText, language)
            ?: localEngine.detectArchetype(normalizedPositiveText, language)
        val matchedAtmosphere = if (matchedArchetype == null) {
            localEngine.detectAtmosphere(positiveText, language)
                ?: localEngine.detectAtmosphere(normalizedPositiveText, language)
        } else null
        val detectedPerson = if (matchedArchetype == null && matchedAtmosphere == null) localEngine.detectIconicPerson(positiveText) else null

        // 3. Query Expansion Zero-Neurons: traduzione verso l'inglese della SOLA parte positiva
        val shouldTranslate = langCode != "en" || looksLikeNonEnglish(normalizedPositiveText)
        val queryEn = if (shouldTranslate) {
            try {
                withTimeoutOrNull(1200L) {
                    val translated = translationManager.translateOnline(normalizedPositiveText, "auto", "en")
                    if (!translated.isNullOrBlank() && !translated.equals(normalizedPositiveText, ignoreCase = true)) {
                        translated
                    } else if (langCode != "en") {
                        translated
                    } else null
                }
            } catch (_: Exception) {
                null
            }
        } else null

        // Iniezione di lessico cinematografico ad alta densità gergale (es. "microspie" -> wiretap, "assedio" -> precinct siege)
        // direttamente in queryEn PRIMA del calcolo dell'embedding BGE-M3 su Cloudflare
        val cinematicEnrichment = localEngine.extractCinematicEnrichment(normalizedPositiveText, queryEn.orEmpty())
        val finalQueryEn = if (!queryEn.isNullOrBlank()) {
            if (cinematicEnrichment.isNotBlank()) "$queryEn $cinematicEnrichment" else queryEn
        } else if (cinematicEnrichment.isNotBlank()) {
            cinematicEnrichment
        } else null

        // 4. Oversampling su Vectorize: Pesca a strascico con limit = 100 candidati (Punto 2)
        val cloudResults = try {
            withTimeoutOrNull(2800L) {
                val response = api.searchSemantics(
                    query = normalizedPositiveText,
                    lang = langCode,
                    queryEn = finalQueryEn,
                    limit = limit
                )
                if (response.isSuccessful) {
                    val rawResults = response.body()?.results.orEmpty()

                    // 5. RERANKER ALGORITMICO LOCALE KOTLIN (Punto 3: Hybrid Search 100% locale)
                    // A. Preparazione Regex per Ghigliottina Negazioni con Word Boundary (\b), Stemming e Filtro Generi
                    val negatedGenreIds = mutableSetOf<Long>()
                    val negationRegex: Regex? = if (negatedTokens.isNotEmpty()) {
                        val patterns = mutableListOf<String>()
                        for (rawNeg in negatedTokens) {
                            val negToken = rawNeg.lowercase(Locale.ROOT).trim()
                            if (negToken.length < 3) continue

                            // 1. Mappatura diretta dei generi negati (Hard Genre Exclusion su genreIds)
                            when (negToken) {
                                "animazione", "cartoni", "cartone", "anime" -> negatedGenreIds.add(16L)
                                "horror", "paura", "mostro", "mostri" -> negatedGenreIds.add(27L)
                                "musical", "canzoni", "musica" -> negatedGenreIds.add(10402L)
                                "documentario", "documentari" -> negatedGenreIds.add(99L)
                                "western" -> negatedGenreIds.add(37L)
                                "romantico", "romance", "sentimentale", "amore" -> negatedGenreIds.add(10749L)
                                "guerra", "war", "bellico" -> negatedGenreIds.add(10752L)
                            }

                            // 2. Stemming radice italiana sicura
                            if (negToken == "mostro" || negToken == "mostri") {
                                // mostr[eoiu] intercetta mostro, mostri, mostruoso, ma protegge verbi come mostra, mostrare, mostrando!
                                patterns += """mostr[eoiu]"""
                            } else {
                                val stem = if (negToken.length >= 5 && negToken.last() in "aeiou") negToken.dropLast(1) else negToken
                                patterns += Regex.escape(stem)
                            }

                            // 3. Espansione semantica verso l'inglese per le overview TMDB
                            val englishEquivalents = when (negToken) {
                                "mostro", "mostri" -> listOf("monster", "creature")
                                "alieno", "alieni" -> listOf("alien", "extraterrestrial")
                                "zombie", "zombi" -> listOf("zombie", "undead", "infected")
                                "fantasma", "fantasmi" -> listOf("ghost", "spirit", "haunt", "specter")
                                "demone", "demoni" -> listOf("demon", "demonic", "devil")
                                "vampiro", "vampiri" -> listOf("vampire", "dracula", "bloodsucker")
                                "robot", "cyborg", "androide", "androidi" -> listOf("robot", "cyborg", "android")
                                "sangue", "splatter", "gore" -> listOf("blood", "gore", "slasher", "splatter")
                                "bambino", "bambini", "ragazzini" -> listOf("child", "children", "kid", "kids")
                                "animazione", "cartoni", "cartone" -> listOf("animation", "animated", "cartoon")
                                else -> emptyList()
                            }
                            englishEquivalents.forEach { patterns += Regex.escape(it) }
                        }
                        if (patterns.isNotEmpty()) {
                            // \b a inizio parola evita falsi positivi (es. \bmostr[eoiu] non matcha "dimostrare", \bwar non matcha "award")
                            Regex("""\b(${patterns.joinToString("|")})""", RegexOption.IGNORE_CASE)
                        } else null
                    } else null

                    // Token candidati per il match dinamico di autori e crew tecnica (DoP, Compositore, Regista, Sceneggiatore)
                    // Token >= 4 caratteri escludendo parole cinematografiche comuni o stop words
                    val queryAuthorTokens = Regex("""\b\p{L}{4,}\b""")
                        .findAll(sanitizedQuery.lowercase(Locale.ROOT))
                        .map { it.value }
                        .filter { it !in ROLE_QUERY_STOPWORDS }
                        .toList()

                    val cleanCandidates = rawResults.filter { dto ->
                        // Anti-Slop: poster obbligatorio, titolo valido
                        if (dto.posterPath.isNullOrBlank() || dto.title.isBlank()) return@filter false

                        // Esclusione rigida sui generi negati (es. "senza animazione", "senza horror")
                        if (negatedGenreIds.isNotEmpty() && dto.genreIds?.any { negatedGenreIds.contains(it) } == true) {
                            return@filter false
                        }

                        // Filtro Cortometraggi: esclude featurette e corti (1..45 min), a meno che la query non chieda esplicitamente corti
                        val wantsShorts = sanitizedQuery.contains(Regex("""\b(cort[oi]|cortometraggi[oi]|short[s]?|short film)\b""", RegexOption.IGNORE_CASE))
                        if (!wantsShorts && dto.runtime != null && dto.runtime in 1..45) {
                            return@filter false
                        }

                        // Filtro Genere Family (10751): taglia film per bambini su query cyberpunk/mature/noir/thriller/horror,
                        // preservando al 100% l'animazione d'autore (Akira, Ghost in the Shell, Paprika, Redline) che NON ha il genere Family!
                        val isMatureOrDarkQuery = matchedArchetype != null ||
                            sanitizedQuery.contains(Regex("""\b(cyberpunk|noir|dystop|neon|thriller|horror|gore|splatter|killer|dark|oscur|violenz|cup[oa]|rapin|heist|criminal|delitt)\b""", RegexOption.IGNORE_CASE))
                        val wantsFamily = sanitizedQuery.contains(Regex("""\b(famigli[ae]|family|bambin[oi]|ragazzin[oi]|kids?|disney|pixar)\b""", RegexOption.IGNORE_CASE))

                        if (isMatureOrDarkQuery && !wantsFamily && dto.genreIds?.contains(10751L) == true) {
                            return@filter false
                        }

                        // Floor di base: taglia solo il rumore geometrico casuale (< 0.35)
                        val rawSim = dto.rawSimilarity
                        val affinity = rawSim ?: dto.score ?: extractScoreFromReason(dto.aiReason) ?: 0.5
                        if (affinity < 0.35) return@filter false

                        // Anti-Slop aggressivo per film "fantasma" con voto perfetto ma senza reale consenso.
                        val vCount = dto.voteCount
                        val vAvg = dto.voteAverage ?: 5.0
                        if (vCount == null && vAvg >= 9.5) return@filter false
                        if (vAvg < 4.0 && (vCount ?: 100) >= 20) return@filter false

                        // Filtro Negazioni Corazzato con Word Boundary (\b)
                        if (negationRegex != null) {
                            val titleMatches = negationRegex.containsMatchIn(dto.title)
                            val overviewMatches = !dto.overview.isNullOrBlank() && negationRegex.containsMatchIn(dto.overview)
                            if (titleMatches || overviewMatches) return@filter false
                        }
                        true
                    }

                    if (cleanCandidates.isEmpty() && rawResults.isNotEmpty()) {
                        null
                    } else {
                        // Floor Dinamico Adattivo a 2 scaglioni:
                        // Se ci sono >= 12 candidati con similarità solida (>= 0.39), mantieni la selezione ristretta.
                        // Se invece la query è di nicchia o ultra-specifica (< 12 candidati), allarga la maglia fino a 0.36
                        // per non lasciare mai l'utente con 1 solo film spoglio!
                        val highTierCount = cleanCandidates.count { (it.rawSimilarity ?: it.score ?: 0.0) >= 0.39 }
                        val thresholdFloor = if (highTierCount >= 12) 0.39 else 0.36
                        val candidatesToScore = cleanCandidates.filter {
                            (it.rawSimilarity ?: it.score ?: 0.0) >= thresholdFloor
                        }

                        // B. Calcolo del Punteggio Ibrido con Match Autori Dinamico, Macro-Cluster di Genere e Damping Popolarità
                        val expandedExpectedGenres = expandGenreClusters(expectedGenres)
                        val scoredCandidates = candidatesToScore.map { dto ->
                            val rawAffinity = dto.rawSimilarity ?: dto.score ?: extractScoreFromReason(dto.aiReason) ?: 0.5

                            // Match dinamico con gli autori / crew tecnica (DoP, Compositore, Regista, Sceneggiatore)
                            val matchedCrew = findMatchingCrewRole(dto, queryAuthorTokens)
                            val crewMultiplier = if (matchedCrew != null) 1.55 else 1.0

                            // B1. Macro-Cluster di Genere:
                            // Se il film condivide un genere o un genere narrativamente affine del cluster, riceve +12%.
                            // Se c'è mismatch totale ma la sinossi è eccellente, subisce un'attenuazione moderata (0.78x anziché 0.50x).
                            val movieGenres = dto.genreIds.orEmpty()
                            val genreMultiplier = if (expandedExpectedGenres.isNotEmpty() && movieGenres.isNotEmpty()) {
                                val hasOverlap = movieGenres.any { expandedExpectedGenres.contains(it) }
                                if (hasOverlap) 1.12 else 0.78 // Penalità attenuata: 0.78x preserva grandi film di confine
                            } else {
                                1.0
                            }

                            // B2. Damping di Popolarità & Anno:
                            val releaseYear = dto.year?.toIntOrNull() ?: 2020
                            val isRecentOrFuture = releaseYear >= 2025
                            val votes = dto.voteCount
                            val popularityMultiplier = when {
                                dto.voteCount == null && isRecentOrFuture -> 0.85
                                dto.voteCount == null -> 1.0
                                // Se l'utente ha cercato un autore specifico (es. Zimmer, Deakins), non penalizzare i suoi capolavori celebri!
                                matchedCrew != null -> if (votes != null && votes >= 2000) 1.15 else 1.0
                                else -> {
                                    when {
                                        votes != null && votes > 6000 -> {
                                            val excessLog = kotlin.math.ln(votes.toDouble()) - kotlin.math.ln(6000.0)
                                            (1.0 - (0.06 * excessLog.coerceIn(0.0, 3.5))).coerceAtLeast(0.78)
                                        }
                                        votes != null && votes in 350..5500 && !isRecentOrFuture -> 1.12
                                        votes != null && votes < 50 -> 0.88
                                        else -> 1.0
                                    }
                                }
                            }

                            // B3. Moltiplicatore Qualità Critica (Voto medio TMDB)
                            val voteAvg = if (dto.voteCount == null) 6.5 else (dto.voteAverage ?: 6.0)
                            val qualityMultiplier = 0.85 + (voteAvg / 10.0) * 0.25

                            // Score finale composito con boost autore prioritario
                            val compositeScore = rawAffinity * genreMultiplier * popularityMultiplier * qualityMultiplier * crewMultiplier
                            Pair(dto, compositeScore)
                        }
                        .sortedByDescending { it.second }

                        // B4. Anti-Cloni Franchise / Sequel nei Top 15:
                        // Limita a max 2 capitoli per franchise e applica penalità 0.82x al secondo capitolo
                        val franchiseCounts = mutableMapOf<String, Int>()
                        val finalRanked = mutableListOf<Pair<com.cinetrack.data.api.SemanticMovieDto, Double>>()

                        for ((dto, score) in scoredCandidates) {
                            val franchiseKey = dto.collectionId?.takeIf { it > 0 }?.toString()
                                ?: dto.franchise?.takeIf { it.isNotBlank() }?.lowercase(Locale.ROOT)
                                ?: localEngine.detectFranchiseKey(dto.title, dto.genreIds.orEmpty())
                                ?: dto.title.split(Regex("[:\\-–]")).first().trim().lowercase(Locale.ROOT).takeIf { it.length >= 6 }

                            var adjustedScore = score
                            if (franchiseKey != null) {
                                val count = franchiseCounts.getOrDefault(franchiseKey, 0)
                                if (count >= 2 && finalRanked.size < 15) {
                                    // Già 2 film della saga nei top 15: scarta il terzo per varietà cinematografica
                                    continue
                                } else if (count == 1) {
                                    adjustedScore *= 0.82 // Penalità progressiva al secondo sequel
                                }
                                franchiseCounts[franchiseKey] = count + 1
                            }
                            finalRanked.add(Pair(dto, adjustedScore))
                        }

                        finalRanked.sortedByDescending { it.second }
                        .map { (dto, _) ->
                            val contextualReason = formatCinematicReason(
                                dto = dto,
                                matchedArchetype = matchedArchetype,
                                matchedAtmosphere = matchedAtmosphere,
                                detectedPerson = detectedPerson,
                                expectedGenres = expectedGenres,
                                language = language,
                                queryAuthorTokens = queryAuthorTokens
                            )

                            dto.toMovieResult(customReason = contextualReason, isItalian = isItalian)
                        }
                    }
                } else null
            }
        } catch (_: IOException) {
            null
        } catch (_: Exception) {
            null
        }

        if (!cloudResults.isNullOrEmpty()) {
            sessionCache.put(cacheKey, cloudResults)
            return cloudResults
        }

        // 2. Fallback Invisibile: LocalMoodSearchEngine in RAM (Zero-Crash & Offline-First)
        val localResult = localEngine.searchLocalMood(
            query = sanitizedQuery,
            language = language,
            page = 1,
            isQuotaExhaustedFallback = false
        )
        if (localResult.movies.isNotEmpty()) {
            sessionCache.put(cacheKey, localResult.movies)
        }
        return localResult.movies
    }

    private fun expandGenreClusters(genres: List<Long>): Set<Long> {
        val expanded = genres.toMutableSet()
        for (g in genres) {
            when (g) {
                // Cluster Action / Crime / Thriller (assedio, poliziesco, rapina, vendetta)
                28L, 80L, 53L -> {
                    expanded.add(28L)
                    expanded.add(80L)
                    expanded.add(53L)
                }
                // Cluster Sci-Fi / Supernatural / Mystery / Horror / Thriller (loop temporali, anomalie, claustrofobia)
                878L, 9648L, 27L -> {
                    expanded.add(878L)
                    expanded.add(9648L)
                    expanded.add(53L)
                    expanded.add(27L)
                }
                // Cluster War / History / Drama / Action (guerra, storico, epico)
                10752L, 36L -> {
                    expanded.add(10752L)
                    expanded.add(36L)
                    expanded.add(18L)
                    expanded.add(28L)
                }
                // Cluster Adventure / Survival / Drama (naufragio, deserto, sopravvivenza)
                12L -> {
                    expanded.add(12L)
                    expanded.add(18L)
                }
                // Cluster Comedy / Romance (commedia romantica, sentimentale)
                35L -> {
                    expanded.add(35L)
                }
                10749L -> {
                    expanded.add(10749L)
                    expanded.add(18L)
                    expanded.add(35L)
                }
            }
        }
        return expanded
    }

    private fun extractScoreFromReason(aiReason: String?): Double? {
        if (aiReason.isNullOrBlank()) return null
        val match = Regex("""(\d{1,3})%\s*affinity""", RegexOption.IGNORE_CASE).find(aiReason)
        return match?.groupValues?.get(1)?.toDoubleOrNull()?.let { it / 100.0 }
    }

    private fun formatCinematicReason(
        dto: com.cinetrack.data.api.SemanticMovieDto,
        matchedArchetype: com.cinetrack.data.repository.mood.CinematicArchetype?,
        matchedAtmosphere: com.cinetrack.data.repository.mood.AtmosphericModifier?,
        detectedPerson: Pair<String, Long>?,
        expectedGenres: List<Long>,
        language: String,
        queryAuthorTokens: List<String> = emptyList()
    ): String {
        val rawAffinity = dto.rawSimilarity ?: dto.score ?: extractScoreFromReason(dto.aiReason) ?: 0.5
        // Curva psicologica non-lineare per la UI:
        // Tarata sul nuovo Floor di base (0.34f - 0.65f) per garantire differenziazione graduale (71% - 98%)
        // anche per gemme ripescate nella fascia 0.36 - 0.39.
        val normalizedCosine = ((rawAffinity.toFloat() - 0.34f) / (0.65f - 0.34f)).coerceIn(0f, 1f)
        val affinityPercent = (normalizedCosine * 28f + 70f).toInt().coerceIn(68, 98)
        val affinitySuffix = context.getString(R.string.semantic_affinity_suffix)
        val isItalian = language.lowercase().startsWith("it")

        fun formatAffinity(label: String): String =
            context.getString(R.string.semantic_affinity_format, label, affinityPercent, affinitySuffix)

        // 1. Corrispondenza Dinamica con Autori / Ruoli Tecnici cercati nella query (massima priorità)
        val matchedCrew = findMatchingCrewRole(dto, queryAuthorTokens)
        if (matchedCrew != null) {
            val (roleType, personName) = matchedCrew
            val roleLabel = when (roleType) {
                "composer" -> context.getString(R.string.semantic_reason_score_by, personName)
                "cinematographer" -> context.getString(R.string.semantic_reason_cinematography_by, personName)
                "writer" -> context.getString(R.string.semantic_reason_written_by, personName)
                else -> context.getString(R.string.semantic_reason_directed_by, personName)
            }
            return formatAffinity(roleLabel)
        }

        // 2. Archetipo narrativo identificato (es. "Time Loop & Paradossi Temporali")
        if (matchedArchetype != null) {
            val archetypeName = if (isItalian) matchedArchetype.nameIt else matchedArchetype.nameEn
            return formatAffinity(archetypeName)
        }

        // 3. Atmosfera / Mood visivo identificato (es. "Claustrofobico & Tensione Sotterranea")
        if (matchedAtmosphere != null) {
            val moodName = if (isItalian) matchedAtmosphere.labelIt else matchedAtmosphere.labelEn
            val moodLabel = context.getString(R.string.semantic_reason_atmosphere, moodName)
            return formatAffinity(moodLabel)
        }

        // 4. Autore o Interprete cardine classico
        if (detectedPerson != null) {
            val pName = detectedPerson.first
            val roleLabel = when {
                dto.director?.contains(pName, ignoreCase = true) == true ->
                    context.getString(R.string.semantic_reason_directed_by, dto.director)
                dto.writer?.contains(pName, ignoreCase = true) == true ->
                    context.getString(R.string.semantic_reason_written_by, dto.writer)
                dto.composer?.contains(pName, ignoreCase = true) == true ->
                    context.getString(R.string.semantic_reason_score_by, dto.composer)
                dto.cinematographer?.contains(pName, ignoreCase = true) == true ->
                    context.getString(R.string.semantic_reason_cinematography_by, dto.cinematographer)
                dto.producers?.contains(pName, ignoreCase = true) == true ->
                    context.getString(R.string.semantic_reason_produced_by, dto.producers)
                else ->
                    context.getString(R.string.semantic_reason_cinema_of, pName)
            }
            return formatAffinity(roleLabel)
        }

        // 5. Corrispondenza con Generi chiave della query (Risoluzione multilingue via GenreConstants)
        val matchingGenre = dto.genreIds?.firstOrNull { expectedGenres.contains(it) }
        val genreName = matchingGenre?.let { GenreConstants.getLocalizedName(it, language, "") }?.takeIf { it.isNotBlank() }
        if (genreName != null) {
            return formatAffinity(genreName)
        }

        // 6. Film di rilievo critico conclamato (escludiamo finte etichette di Cloudflare)
        val vote = dto.voteAverage ?: 0.0
        val year = dto.year?.toIntOrNull()
        if (vote >= 7.8) {
            val voteStr = String.format(Locale.US, "%.1f", vote)
            val acclaimLabel = context.getString(R.string.semantic_reason_critically_acclaimed, voteStr)
            return formatAffinity(acclaimLabel)
        }

        // 7. Regista di spicco se presente
        val dir = dto.director?.trim()
        if (!dir.isNullOrBlank()) {
            val dirLabel = context.getString(R.string.semantic_reason_directed_by, dir)
            return formatAffinity(dirLabel)
        }

        // 8. Classico di genere storico (pre-2010 con buon gradimento)
        if (year != null && year <= 2010 && vote >= 6.8) {
            val classicLabel = context.getString(R.string.semantic_reason_genre_classic, year.toString())
            return formatAffinity(classicLabel)
        }

        // 9. Tagline cinematografica ufficiale o Fallback pulito e internazionalizzato
        val tag = dto.tagline?.trim()
        if (!tag.isNullOrBlank()) {
            val shortTag = if (tag.length > 50) "${tag.take(47)}..." else tag
            return context.getString(R.string.semantic_quote_format, shortTag, affinityPercent)
        }

        val plotMatchLabel = context.getString(R.string.semantic_reason_plot_match)
        return context.getString(R.string.semantic_plot_match_format, plotMatchLabel, affinityPercent)
    }

    private fun findMatchingCrewRole(
        dto: com.cinetrack.data.api.SemanticMovieDto,
        authorTokens: List<String>
    ): Pair<String, String>? {
        if (authorTokens.isEmpty()) return null
        for (token in authorTokens) {
            val regex = Regex("""\b${Regex.escape(token)}\b""", RegexOption.IGNORE_CASE)

            val comp = dto.composer?.trim()
            if (!comp.isNullOrBlank() && regex.containsMatchIn(comp)) {
                return Pair("composer", comp)
            }

            val dop = dto.cinematographer?.trim()
            if (!dop.isNullOrBlank() && regex.containsMatchIn(dop)) {
                return Pair("cinematographer", dop)
            }

            val dir = dto.director?.trim()
            if (!dir.isNullOrBlank() && regex.containsMatchIn(dir)) {
                return Pair("director", dir)
            }

            val wrt = dto.writer?.trim()
            if (!wrt.isNullOrBlank() && regex.containsMatchIn(wrt)) {
                return Pair("writer", wrt)
            }
        }
        return null
    }

    companion object {
        private val ROLE_QUERY_STOPWORDS = setOf(
            "film", "movie", "cinema", "musica", "music", "musiche", "colonna", "sonora", "soundtrack",
            "fotografia", "cinematography", "regia", "regista", "director", "directed", "sceneggiatura",
            "sceneggiatore", "scritto", "scritta", "written", "writer", "autore", "autori", "produzione",
            "produttore", "producers", "piano", "piani", "sequenza", "sequenze", "ambientato", "ambientata",
            "storia", "trama", "attore", "attori", "protagonista", "protagonisti", "delle", "degli", "dello",
            "della", "dallo", "dalla", "senza", "without", "with", "about", "scene", "scenes", "colpo", "luci", "neon"
        )
    }

    private fun looksLikeNonEnglish(query: String): Boolean {
        val lower = query.lowercase(Locale.ROOT)
        val tokens = Regex("""\b\p{L}+\b""").findAll(lower).map { it.value }.toSet()
        val nonEnglishMarkers = setOf(
            "un", "una", "uno", "il", "lo", "la", "gli", "le", "dei", "degli", "delle",
            "del", "della", "dello", "con", "senza", "per", "tra", "fra", "che", "chi",
            "cosa", "dove", "come", "quando", "perché", "perche", "più", "piu", "tutto", "tutti",
            "storia", "racconto", "bello", "belli", "specchio", "fata", "ragazzo", "drago",
            "film", "serie", "personaggio", "amore", "morte", "paura", "spazio", "futuro",
            "el", "los", "las", "para", "por", "avec", "sans", "pour", "dans", "und", "der", "die", "das"
        )
        return tokens.any { it in nonEnglishMarkers }
    }
}
