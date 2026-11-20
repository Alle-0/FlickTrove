package com.cinetrack.data.repository

import android.content.Context
import com.cinetrack.R
import com.cinetrack.data.api.SemanticSearchApi
import com.cinetrack.data.api.TMDBSearchResult
import com.cinetrack.data.model.GenreConstants
import com.cinetrack.data.repository.mood.MoodVocabularies
import com.cinetrack.util.TranslationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    ): List<TMDBSearchResult.MovieResult> = withContext(Dispatchers.Default) {
        val rawQuery = query.trim().take(500)
        if (rawQuery.isBlank()) return@withContext emptyList<TMDBSearchResult.MovieResult>()

        val langCode = language.take(2).lowercase()
        // La lingua dei titoli e delle label rispetta la preferenza lingua utente/sistema
        val isItalian = langCode == "it"

        // 1. Sanitizzazione query a monte (Bypass typo-correction locale per BGE-M3)
        val sanitizedQuery = rawQuery.trim()

        val cacheKey = "$sanitizedQuery|$langCode|$isItalian"
        sessionCache.get(cacheKey)?.let { cached ->
            return@withContext cached
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
        val baseExpectedGenres = localEngine.extractQueryGenreIds(normalizedPositiveText, language)
        val matchedArchetype = localEngine.detectArchetype(positiveText, language)
            ?: localEngine.detectArchetype(normalizedPositiveText, language)
        val expectedGenres = if (matchedArchetype != null) {
            (baseExpectedGenres + matchedArchetype.primaryGenres).distinct()
        } else baseExpectedGenres
        val matchedAtmosphere = if (matchedArchetype == null) {
            localEngine.detectAtmosphere(positiveText, language)
                ?: localEngine.detectAtmosphere(normalizedPositiveText, language)
        } else null
        val detectedPerson = localEngine.detectIconicPerson(positiveText)

        // Token candidati per il match dinamico di autori e crew tecnica (DoP, Compositore, Regista, Sceneggiatore)
        // Token >= 4 caratteri estratti dalla SOLA parte positiva (la negazione non deve mai premiare un autore!)
        val queryAuthorTokens = Regex("""\b\p{L}{4,}\b""")
            .findAll(positiveText.lowercase(Locale.ROOT))
            .map { it.value }
            .filter { it !in ROLE_QUERY_STOPWORDS }
            .toList()

        val isMusicQuery = getRoleMarkerRegex(
            R.string.iris_music_markers,
            listOf("music by", "soundtrack by", "score by", "scored by", "composer")
        ).containsMatchIn(positiveText)

        val isCinematographyQuery = getRoleMarkerRegex(
            R.string.iris_cinematography_markers,
            listOf("cinematography by", "director of photography", "dop")
        ).containsMatchIn(positiveText)

        val isDirectorQuery = getRoleMarkerRegex(
            R.string.iris_director_markers,
            listOf("directed by", "director", "direction by")
        ).containsMatchIn(positiveText)

        val capitalizedAuthor = if (isMusicQuery || isCinematographyQuery || isDirectorQuery) {
            // Cerca un token esplicitamente capitalizzato dall'utente nella query positiva originale (evitando la prima parola se a inizio frase)
            val rawWords = positiveText.trim().split(Regex("""\s+"""))
            rawWords.drop(1).firstOrNull { word ->
                word.length >= 4 &&
                    word.first().isUpperCase() &&
                    word.lowercase(Locale.ROOT) !in ROLE_QUERY_STOPWORDS &&
                    queryAuthorTokens.contains(word.lowercase(Locale.ROOT))
            }
        } else null

        val authorTarget = detectedPerson?.first?.split(Regex("""\s+"""))?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase(Locale.ROOT) } }
            ?: capitalizedAuthor

        val authorPrefix = when {
            authorTarget == null -> ""
            isMusicQuery -> "$authorTarget original music score soundtrack"
            isCinematographyQuery -> "$authorTarget cinematography director of photography"
            isDirectorQuery -> "$authorTarget directed by film direction"
            else -> "$authorTarget movie film"
        }

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
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                null
            }
        } else null

        // Iniezione di lessico cinematografico ad alta densità gergale (es. "microspie" -> wiretap, "assedio" -> precinct siege)
        // e ancoraggio esplicito dell'autore (es. Roger Deakins cinematography / Hans Zimmer score) PRIMA di BGE-M3 su Cloudflare
        val cinematicEnrichment = localEngine.extractCinematicEnrichment(normalizedPositiveText, queryEn.orEmpty())
        val baseEn = if (!queryEn.isNullOrBlank()) {
            if (cinematicEnrichment.isNotBlank()) "$queryEn $cinematicEnrichment" else queryEn
        } else if (cinematicEnrichment.isNotBlank()) {
            cinematicEnrichment
        } else null

        val finalQueryEn = if (!baseEn.isNullOrBlank()) {
            if (authorPrefix.isNotBlank()) "$authorPrefix $baseEn" else baseEn
        } else if (authorPrefix.isNotBlank()) {
            authorPrefix
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
                    val negatedGenreIds = parsedIntent.negatedGenres.toMutableSet()

                    // Valutazione di finestre composte di 2-3 parole dopo la negazione (es. "commedia romantica" nega Romance 10749L, NON Comedy 35L)
                    val isRomComNegated = parsedIntent.negatedText?.contains(
                        Regex("""\b(commedi[ae]\s+romantics?\w*|commedi[ae]\s+sentimentali?\w*|commedi[ae]\s+d[' ]amore|romantic\s+comed(?:y|ies)|rom-?coms?)\b""", RegexOption.IGNORE_CASE)
                    ) == true
                    if (isRomComNegated) {
                        negatedGenreIds.add(10749L)
                        negatedGenreIds.remove(35L)
                    }

                    val negationRegex: Regex? = if (negatedTokens.isNotEmpty()) {
                        val patterns = mutableListOf<String>()
                        for (rawNeg in negatedTokens) {
                            val negToken = rawNeg.lowercase(Locale.ROOT).trim()
                            if (negToken.length < 3) continue

                            // Se la negazione è commedia romantica, non negare "commedia" come parola o genere
                            if (isRomComNegated && (negToken in listOf("commedia", "comedy", "comico", "comici", "divertente") || negToken.startsWith("commedi"))) {
                                continue
                            }

                            // 1. Mappatura diretta dei generi negati (Hard Genre Exclusion su genreIds)
                            when (negToken) {
                                "animazione", "animation", "cartoni", "cartone", "anime" -> negatedGenreIds.add(16L)
                                "horror", "paura" -> negatedGenreIds.add(27L)
                                "musical", "canzoni", "musica", "music" -> negatedGenreIds.add(10402L)
                                "documentario", "documentari", "documentary", "documentaries", "doc", "docs", "docufilm" -> negatedGenreIds.add(99L)
                                "western" -> negatedGenreIds.add(37L)
                                "commedia", "comedy", "comico", "comici" -> if (!isRomComNegated) negatedGenreIds.add(35L)
                                "romantico", "romance", "sentimentale", "amore" -> negatedGenreIds.add(10749L)
                                "guerra", "war", "bellico" -> negatedGenreIds.add(10752L)
                                "fantascienza", "sci-fi", "scifi" -> negatedGenreIds.add(878L)
                            }

                            // 2. Stemming radice italiana sicura con boundary rigorosi
                            if (negToken == "mostro" || negToken == "mostri") {
                                patterns += """\bmostr[eoiu]\w*\b"""
                            } else if (negToken.length <= 4) {
                                // Per token corti (es. "tim", "saw"), word boundary rigoroso all'inizio E alla fine per evitare falsi match (es. "tim" -> "time")
                                patterns += """\b${Regex.escape(negToken)}\b"""
                            } else {
                                val stem = if (negToken.last() in "aeiou") negToken.dropLast(1) else negToken
                                patterns += """\b${Regex.escape(stem)}\w*\b"""
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
                            englishEquivalents.forEach { patterns += """\b${Regex.escape(it)}\w*\b""" }
                        }
                        // Un genere esplicitamente atteso dalla parte positiva non deve MAI essere negato!
                        negatedGenreIds.removeAll(expectedGenres.toSet())
                        if (patterns.isNotEmpty()) {
                            Regex(patterns.joinToString("|"), RegexOption.IGNORE_CASE)
                        } else null
                    } else null

                    // Calcolo univoco delle condizioni di query (valutate su positiveText per non essere contaminate da negazioni)
                    val wantsShorts = getRoleMarkerRegex(
                        R.string.iris_condition_shorts,
                        listOf("short film", "short films", "shorts", "short movie")
                    ).containsMatchIn(positiveText)

                    val isSoundtrackQuery = getRoleMarkerRegex(
                        R.string.iris_music_markers,
                        listOf("soundtrack", "score", "music", "composer")
                    ).containsMatchIn(positiveText)

                    val wantsDoc = positiveText.contains(
                        Regex("""\b(documentar\w*|docs|docu\w*)\b|\bdoc\b(?!\s+(?:brown|holliday|hudson|ock|savage)\b)""", RegexOption.IGNORE_CASE)
                    ) && !negatedGenreIds.contains(99L)
                    val wantsConcertOrDoc = wantsDoc || positiveText.contains(Regex("""\b(live|concerto|concerti|concert[s]?)\b""", RegexOption.IGNORE_CASE))
                    val targetDecade = detectTargetDecade(positiveText)

                    val isMatureOrDarkQuery = (matchedArchetype != null && DARK_MATURE_ARCHETYPE_IDS.contains(matchedArchetype.id)) ||
                        (matchedAtmosphere != null && matchedAtmosphere.id == "dark_disturbing") ||
                        getRoleMarkerRegex(
                            R.string.iris_condition_mature,
                            listOf("cyberpunk", "noir", "dystopian", "neon", "thriller", "horror", "gore", "splatter", "killer", "dark", "violence", "heist", "crime")
                        ).containsMatchIn(positiveText)

                    val wantsFamily = getRoleMarkerRegex(
                        R.string.iris_condition_family,
                        listOf("family", "family friendly", "kids", "children", "disney", "pixar")
                    ).containsMatchIn(positiveText)

                    val isAnimalSubject = MoodVocabularies.detectAnimalOrNonHumanSubject(positiveText)

                    // Guardie ad alta confidenza per macro-generi inequivocabili (localizzate via strings.xml):
                    val isStrictSciFiIntent = expectedGenres.contains(878L) ||
                        (matchedArchetype != null && matchedArchetype.primaryGenres.contains(878L)) ||
                        getRoleMarkerRegex(
                            R.string.iris_condition_scifi,
                            listOf("cyberpunk", "sci-fi", "science fiction", "synthwave", "deep space", "wormhole", "spaceship", "alien", "android", "cyborg", "robot", "time dilation", "black hole", "dystopian")
                        ).containsMatchIn(positiveText)

                    val isStrictEmotionalIntent = getRoleMarkerRegex(
                        R.string.iris_condition_emotional,
                        listOf("cry", "crying", "tearjerker", "tears", "heartbreaking", "devastating", "sad", "touching", "moving")
                    ).containsMatchIn(positiveText)

                    val wantsHorror = getRoleMarkerRegex(
                        R.string.iris_condition_horror,
                        listOf("horror", "scary", "terror", "fear", "frightening", "spooky", "creepy")
                    ).containsMatchIn(positiveText)

                    val wantsTvMovie = getRoleMarkerRegex(
                        R.string.iris_condition_tv_movie,
                        listOf("tv movie", "television film", "made for tv", "tv film")
                    ).containsMatchIn(positiveText)

                    val cleanCandidates = rawResults.filter { dto ->
                        // Anti-Slop: poster obbligatorio, titolo valido
                        if (dto.posterPath.isNullOrBlank() || dto.title.isBlank()) return@filter false

                        // Esclusione rigida di default per Documentary (99L) se non esplicitamente richiesto dalla query
                        if (!wantsDoc && dto.genreIds?.contains(99L) == true) {
                            return@filter false
                        }

                        // Esclusione rigida sui generi negati (es. "senza animazione", "senza horror")
                        if (negatedGenreIds.isNotEmpty() && dto.genreIds?.any { negatedGenreIds.contains(it) } == true) {
                            return@filter false
                        }

                        // Filtro Cortometraggi: esclude featurette e corti (1..45 min), a meno che la query non chieda esplicitamente corti
                        if (!wantsShorts && dto.runtime != null && dto.runtime in 1..45) {
                            return@filter false
                        }

                        // Esclusione video-concerti e documentari musicali su query di colonna sonora/soundtrack narrativa
                        if (isSoundtrackQuery && !wantsConcertOrDoc && (dto.genreIds?.contains(10402L) == true || dto.genreIds?.contains(99L) == true)) {
                            return@filter false
                        }

                        // Filtro Genere Family (10751): taglia film per bambini ESCLUSIVAMENTE su query mature/dark autentiche,
                        // preservando Goonies, E.T., Ratatouille, e l'animazione d'autore che non ha il genere Family!
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

                        // Filtro Negazioni Corazzato con Word Boundary (\b), Keywords e Crew/Cast Completo
                        if (negationRegex != null) {
                            val titleMatches = negationRegex.containsMatchIn(dto.title)
                            val titleItMatches = !dto.titleIt.isNullOrBlank() && negationRegex.containsMatchIn(dto.titleIt)
                            val overviewMatches = !dto.overview.isNullOrBlank() && negationRegex.containsMatchIn(dto.overview)
                            val keywordsMatch = !dto.keywords.isNullOrBlank() && negationRegex.containsMatchIn(dto.keywords)
                            val crewMatch = listOfNotNull(dto.director, dto.composer, dto.cinematographer, dto.writer, dto.cast, dto.producers)
                                .any { negationRegex.containsMatchIn(it) }
                            if (titleMatches || titleItMatches || overviewMatches || keywordsMatch || crewMatch) return@filter false
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

                            // Match dinamico con gli autori / crew tecnica (DoP, Compositore, Regista, Sceneggiatore, Cast)
                            val matchedCrew = findMatchingCrewRole(dto, queryAuthorTokens, positiveText)
                            val crewMultiplier = if (matchedCrew != null) 1.55 else 1.0

                            // B0.1 Damping e Boost Decennio:
                            // Se il film è uscito nel decennio indicato: +25% (1.25x).
                            // Se è uscito molto lontano (es. > 15 anni di scarto, come Bicycle Thieves 1948 rispetto agli anni '80):
                            // attenuazione moderata (0.80x) che preserva film ambientati nell'epoca (Super 8, IT, Summer of 84)
                            val decadeMultiplier = if (targetDecade != null) {
                                val y = dto.year?.toIntOrNull()
                                when {
                                    y == null -> 1.0
                                    y in targetDecade -> 1.25
                                    y < targetDecade.first - 15 || y > targetDecade.last + 15 -> 0.80
                                    else -> 1.0
                                }
                            } else 1.0

                            // B2. Quality & Popularity Prior Morbido:
                            // Sfrutta vote_count e vote_average con scala logaritmica (zero hardcoded IDs!):
                            // - Sotto i 100 voti: scala dolce da 0.88x a 0.94x (smorza produzioni amatoriali e B-movie oscuri)
                            // - Tra 100 e 5000 voti: neutrale / morbido (0.96x - 1.02x)
                            // - Sopra i 5000 voti con vote_average >= 7.0: boost logaritmico fino a 1.08x per capolavori acclamati (Interstellar, Blade Runner, ecc.)
                            val votes = dto.voteCount ?: 0
                            val voteAvg = dto.voteAverage ?: 6.0
                            val popularityMultiplier = when {
                                votes == 0 -> 0.88
                                votes < 100 -> 0.88 + (votes / 100.0) * 0.06
                                votes >= 5000 && voteAvg >= 7.0 -> {
                                    val logBonus = (kotlin.math.log10(votes.toDouble()) - 3.69897).coerceIn(0.0, 1.0) * 0.05
                                    (1.03 + logBonus).coerceAtMost(1.08)
                                }
                                votes >= 1000 && voteAvg >= 6.8 -> 1.03
                                votes >= 300 -> 1.00
                                else -> 0.95
                            }

                            // B3. Moltiplicatore Qualità Critica (Voto medio TMDB con freno progressivo severo sotto voto 6.0)
                            val qualityMultiplier = when {
                                dto.voteCount == null || dto.voteCount < 20 -> 1.0
                                voteAvg >= 8.0 -> 1.06
                                voteAvg >= 7.2 -> 1.03
                                voteAvg >= 6.5 -> 1.00
                                voteAvg in 6.0..<6.5 -> 0.95
                                // Freno progressivo severo sotto voto 6.0: affossa film mediocri (es. Extraterrestrial 5.6 e The Black Hole 5.8)
                                votes >= 50 && voteAvg < 5.0 -> 0.72
                                votes >= 50 && voteAvg < 6.0 -> 0.84
                                voteAvg < 5.0 -> 0.80
                                else -> 0.88
                            }

                            // B3.1 Penalità TV Movie (10770L): applica 0.80x quando non espressamente richiesto
                            val tvMovieMultiplier = if (!wantsTvMovie && dto.genreIds?.contains(10770L) == true) 0.80 else 1.0

                            // B3.2 Boost Animazione/Family per soggetti animali/non umani (es. topi/roditori: Ratatouille batte omonimi live action)
                            val animalMultiplier = if (isAnimalSubject && (dto.genreIds?.contains(16L) == true || dto.genreIds?.contains(10751L) == true)) 1.25 else 1.0

                            // 1. Calcolo moltiplicatori strutturali di base
                            var finalScore = rawAffinity * popularityMultiplier * qualityMultiplier * crewMultiplier * decadeMultiplier * animalMultiplier * tvMovieMultiplier

                            // 2. Moltiplicatore Macro-Cluster di Genere (+12% se overlap, 0.78x se deviazione standard)
                            val movieGenres = dto.genreIds.orEmpty()
                            val clusterMultiplier = if (expandedExpectedGenres.isNotEmpty() && movieGenres.isNotEmpty()) {
                                val hasOverlap = movieGenres.any { expandedExpectedGenres.contains(it) }
                                if (hasOverlap) 1.12 else 0.78
                            } else 1.0
                            finalScore *= clusterMultiplier

                            // 3. HARD PENALTIES di Genere ad Alta Confidenza (ULTIMO MOLTIPLICATORE prima del sort finale)
                            if (isStrictSciFiIntent && !movieGenres.contains(878L)) {
                                finalScore *= 0.60
                            }
                            if (isStrictEmotionalIntent) {
                                val hasEmotionalGenre = movieGenres.contains(18L) || movieGenres.contains(10749L)
                                if (!hasEmotionalGenre) {
                                    finalScore *= 0.60
                                }
                                if (movieGenres.contains(27L) && !wantsHorror) {
                                    finalScore *= 0.50
                                }
                            }

                            Pair(dto, finalScore)
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

                        // Filtraggio di sicurezza contro rumore e B-movie affossati dal reranker:
                        val validRanked = finalRanked.filter { (dto, score) ->
                            // Se la query cercava chiaramente Sci-Fi (es. cyberpunk, synthwave, spazio, wormhole) e il film non ha 878L,
                            // con lo 0.60x finale non deve superare la soglia e viene estromesso
                            if (isStrictSciFiIntent && dto.genreIds?.contains(878L) != true && score < 0.35) {
                                return@filter false
                            }
                            score >= (thresholdFloor * 0.70)
                        }

                        validRanked.sortedByDescending { it.second }
                        .map { (dto, score) ->
                            val contextualReason = formatCinematicReason(
                                dto = dto,
                                matchedArchetype = matchedArchetype,
                                matchedAtmosphere = matchedAtmosphere,
                                detectedPerson = detectedPerson,
                                expectedGenres = expectedGenres,
                                language = language,
                                queryAuthorTokens = queryAuthorTokens,
                                positiveQuery = positiveText,
                                effectiveScore = score
                            )

                            dto.toMovieResult(customReason = contextualReason, isItalian = isItalian)
                        }
                    }
                } else null
            }
        } catch (_: IOException) {
            null
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }

        if (!cloudResults.isNullOrEmpty()) {
            sessionCache.put(cacheKey, cloudResults)
            return@withContext cloudResults
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
        localResult.movies
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
        queryAuthorTokens: List<String> = emptyList(),
        positiveQuery: String = "",
        effectiveScore: Double? = null
    ): String {
        val baseSim = dto.rawSimilarity ?: dto.score ?: extractScoreFromReason(dto.aiReason) ?: 0.5
        // Curva psicologica non-lineare per la UI:
        // Se effectiveScore è fornito dal reranker, usiamo il punteggio armonizzato che include penalità di genere (0.60x),
        // TV movie (0.80x) e quality prior logaritmico, evitando percentuali gonfiate per film penalizzati.
        val scoreToDisplay = if (effectiveScore != null) {
            val ratio = (effectiveScore / baseSim.coerceAtLeast(0.01)).coerceIn(0.15, 1.40)
            baseSim * ratio
        } else baseSim

        val affinityPercent = if (scoreToDisplay < 0.32f) {
            val lowNorm = (scoreToDisplay.toFloat() / 0.32f).coerceIn(0f, 1f)
            (lowNorm * 28f + 38f).toInt().coerceIn(38, 66)
        } else {
            val normalizedCosine = ((scoreToDisplay.toFloat() - 0.32f) / (0.65f - 0.32f)).coerceIn(0f, 1f)
            (normalizedCosine * 28f + 70f).toInt().coerceIn(68, 98)
        }
        val affinitySuffix = context.getString(R.string.semantic_affinity_suffix)
        val isItalian = language.lowercase().startsWith("it")

        fun formatAffinity(label: String): String =
            context.getString(R.string.semantic_affinity_format, label, affinityPercent, affinitySuffix)

        // 1. Corrispondenza Dinamica con Autori / Ruoli Tecnici cercati nella query (massima priorità)
        val matchedCrew = findMatchingCrewRole(dto, queryAuthorTokens, positiveQuery)
        if (matchedCrew != null) {
            val (roleType, personName) = matchedCrew
            val roleLabel = when (roleType) {
                "composer" -> context.getString(R.string.semantic_reason_score_by, personName)
                "cinematographer" -> context.getString(R.string.semantic_reason_cinematography_by, personName)
                "writer" -> context.getString(R.string.semantic_reason_written_by, personName)
                "cast" -> context.getString(R.string.semantic_reason_cinema_of, personName)
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

        // 7. Classico di genere storico (pre-2010 con buon gradimento)
        if (year != null && year <= 2010 && vote >= 6.8) {
            val classicLabel = context.getString(R.string.semantic_reason_genre_classic, year.toString())
            return formatAffinity(classicLabel)
        }

        // 8. Tagline cinematografica ufficiale o Fallback pulito e internazionalizzato
        val tag = dto.tagline?.trim()
        if (!tag.isNullOrBlank()) {
            val shortTag = if (tag.length > 50) "${tag.take(47)}..." else tag
            return context.getString(R.string.semantic_quote_format, shortTag, affinityPercent)
        }

        val plotMatchLabel = context.getString(R.string.semantic_reason_plot_match)
        return context.getString(R.string.semantic_plot_match_format, plotMatchLabel, affinityPercent)
    }

    private fun getRoleMarkerRegex(resId: Int, fallbackEn: List<String>): Regex {
        val localized = try {
            context.getString(resId).split(',').map { it.trim() }.filter { it.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
        val allTriggers = (localized + fallbackEn).distinct()
        val pattern = allTriggers.joinToString("|") { Regex.escape(it) }
        return Regex("""\b(?:$pattern)\b""", RegexOption.IGNORE_CASE)
    }

    private fun findMatchingCrewRole(
        dto: com.cinetrack.data.api.SemanticMovieDto,
        authorTokens: List<String>,
        queryText: String = ""
    ): Pair<String, String>? {
        if (authorTokens.isEmpty()) return null

        val lowerQuery = queryText.lowercase(Locale.ROOT)
        val musicRegex = getRoleMarkerRegex(
            R.string.iris_music_markers,
            listOf("music by", "soundtrack by", "score by", "scored by", "composer")
        )
        val cinematographyRegex = getRoleMarkerRegex(
            R.string.iris_cinematography_markers,
            listOf("cinematography by", "director of photography", "dop")
        )
        val directorRegex = getRoleMarkerRegex(
            R.string.iris_director_markers,
            listOf("directed by", "director", "direction by")
        )
        val writerRegex = getRoleMarkerRegex(
            R.string.iris_writer_markers,
            listOf("written by", "screenplay by", "writer")
        )
        val actorRegex = getRoleMarkerRegex(
            R.string.iris_actor_markers,
            listOf("starring", "star", "lead actor")
        )

        fun matchesPerson(fieldValue: String?, roleMarkerRegex: Regex?): Boolean {
            if (fieldValue.isNullOrBlank()) return false
            val nameParts = fieldValue.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }
            if (nameParts.isEmpty()) return false
            val lastName = nameParts.last().lowercase(Locale.ROOT)
            val firstName = nameParts.first().lowercase(Locale.ROOT)

            // 1. Nome e cognome completo (2+ parole): match libero e sempre attivo su regista, compositore, DoP e cast.
            // Nessun tema cinematografico si chiama "Christopher Nolan" o "Hans Zimmer" -> zero falsi positivi strutturali!
            if (nameParts.size >= 2) {
                val fullNameRegex = Regex("""\b${Regex.escape(firstName)}\s+${Regex.escape(lastName)}\b""", RegexOption.IGNORE_CASE)
                if (fullNameRegex.containsMatchIn(lowerQuery)) return true
            }

            // 2. Cognome singolo (1 parola, >= 5 caratteri): scatta ESCLUSIVAMENTE se preceduto da un marcatore di ruolo esplicito
            // (regia di, diretto da, musica di, colonna sonora di, fotografia di, sceneggiatura di, interpretato da).
            // La preposizione generica isolata "di" NON è un marcatore di ruolo, prevenendo falsi positivi (es. "film di natale").
            if (roleMarkerRegex != null && lastName.length >= 5 && lastName !in ROLE_QUERY_STOPWORDS) {
                val roleWithLastNameRegex = Regex(
                    """(?:${roleMarkerRegex.pattern})\s+(?:d[ei]\s+|the\s+)?${Regex.escape(lastName)}\b""",
                    RegexOption.IGNORE_CASE
                )
                if (roleWithLastNameRegex.containsMatchIn(lowerQuery)) return true
            }

            return false
        }

        // Valutazione gerarchica sui campi dei metadati (composer, cinematography, director, writer, cast):
        val comp = dto.composer?.trim()
        if (matchesPerson(comp, musicRegex)) return Pair("composer", comp!!)

        val dop = dto.cinematographer?.trim()
        if (matchesPerson(dop, cinematographyRegex)) return Pair("cinematographer", dop!!)

        val dir = dto.director?.trim()
        if (matchesPerson(dir, directorRegex)) return Pair("director", dir!!)

        val wrt = dto.writer?.trim()
        if (matchesPerson(wrt, writerRegex)) return Pair("writer", wrt!!)

        val castMembers = dto.cast?.split(Regex(""",\s*"""))?.filter { it.isNotBlank() }.orEmpty()
        for (actor in castMembers.take(4)) {
            if (matchesPerson(actor, actorRegex)) return Pair("cast", actor)
        }

        return null
    }

    private fun detectTargetDecade(query: String): IntRange? {
        val lower = query.lowercase(Locale.ROOT)
        return when {
            lower.contains(Regex("""\b(anni\s+'?80|80s|'80s|eighties|1980s|anni\s+ottanta)\b""")) -> 1980..1989
            lower.contains(Regex("""\b(anni\s+'?70|70s|'70s|seventies|1970s|anni\s+settanta)\b""")) -> 1970..1979
            lower.contains(Regex("""\b(anni\s+'?90|90s|'90s|nineties|1990s|anni\s+novanta)\b""")) -> 1990..1999
            lower.contains(Regex("""\b(anni\s+'?60|60s|'60s|sixties|1960s|anni\s+sessanta)\b""")) -> 1960..1969
            lower.contains(Regex("""\b(anni\s+'?50|50s|'50s|fifties|1950s|anni\s+cinquanta)\b""")) -> 1950..1959
            lower.contains(Regex("""\b(anni\s+'?2000|2000s|duemila)\b""")) -> 2000..2009
            lower.contains(Regex("""\b(anni\s+'?20|20s|'20s|roaring\s+twenties|anni\s+venti)\b""")) -> 1920..1929
            lower.contains(Regex("""\b(anni\s+'?30|30s|'30s|thirties|anni\s+trenta)\b""")) -> 1930..1939
            lower.contains(Regex("""\b(anni\s+'?40|40s|'40s|forties|anni\s+quaranta)\b""")) -> 1940..1949
            else -> null
        }
    }

    companion object {
        private val DARK_MATURE_ARCHETYPE_IDS = setOf(
            "cyberpunk",
            "neo_noir",
            "psychological_thriller",
            "post_apocalyptic",
            "revenge_action",
            "war_epic",
            "heist_caper",
            "courtroom_drama",
            "claustrophobic_thriller",
            "giallo_italiano",
            "haunted_house",
            "submarine_deep_sea",
            "gladiator_arena",
            "tragic_hero_demise",
            "metamorphosis_mutation"
        )

        private val ROLE_QUERY_STOPWORDS = setOf(
            "film", "movie", "cinema", "musica", "music", "musiche", "colonna", "sonora", "soundtrack",
            "fotografia", "cinematography", "regia", "regista", "director", "directed", "sceneggiatura",
            "sceneggiatore", "scritto", "scritta", "written", "writer", "autore", "autori", "produzione",
            "produttore", "producers", "attore", "attori", "protagonista", "protagonisti", "starring",
            "story", "storia", "trama", "sound", "score"
        )
    }

    private fun looksLikeNonEnglish(query: String): Boolean {
        // 1. Intercetta istantaneamente oltre 20 lingue non-ASCII (cirillico, arabo, CJK, hindi, greco, thai)
        // e qualsiasi parola europea con accenti o dieresi (français, español, deutsch, italiano con à/è/ì/ò/ù)
        if (query.any { it.code > 127 }) return true

        // 2. Manciata di articoli e preposizioni ASCII essenziali per lingue latine pure
        val lower = query.lowercase(Locale.ROOT)
        val tokens = Regex("""\b\p{L}+\b""").findAll(lower).map { it.value }.toSet()
        val nonEnglishMarkers = setOf(
            "un", "una", "uno", "il", "lo", "la", "gli", "le", "dei", "degli", "delle",
            "del", "della", "dello", "con", "senza", "per", "tra", "fra", "che", "chi",
            "cosa", "dove", "come", "quando", "perche", "piu", "tutto", "tutti",
            "storia", "film", "serie", "personaggio", "amore", "morte", "paura", "spazio", "futuro",
            "el", "los", "las", "para", "por", "avec", "sans", "pour", "dans", "und", "der", "die", "das"
        )
        return tokens.any { it in nonEnglishMarkers }
    }
}
