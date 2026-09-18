package com.cinetrack.data.repository.importers

import com.cinetrack.data.model.Movie
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.data.repository.importers.ImporterUtils.parseAndNormalizeWatchedDate
import com.cinetrack.data.repository.importers.ImporterUtils.parseCsvLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import com.squareup.moshi.JsonReader
import okio.buffer
import okio.source
import java.io.InputStream
import javax.inject.Inject

class RefractImporter(
    private val movieLookup: MovieLookupService
) {
    @Inject constructor(movieRepository: MovieRepository) : this(movieRepository as MovieLookupService)

    fun isRefractZip(zipEntries: Map<String, ByteArray>): Boolean {
        val keys = zipEntries.keys.map { it.lowercase() }
        val hasSeries = keys.any { it.endsWith("series.csv") }
        val hasEpisodes = keys.any { it.endsWith("episodes.csv") }
        val hasMovies = keys.any { it.endsWith("movies.csv") }
        return (hasSeries && hasEpisodes) || (hasSeries && hasMovies)
    }

    fun isRefractJson(content: String): Boolean {
        val preview = content.take(1500)
        return (preview.contains("\"shows\"") || preview.contains("\"movies\"")) &&
                (preview.contains("\"tvdb\"") || preview.contains("\"is_watched\"") || preview.contains("\"is_specials\"") || preview.contains("\"seasons\""))
    }

    suspend fun migrateRefractZip(
        zipEntries: Map<String, ByteArray>,
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> },
        onBatchReady: suspend (List<Pair<Movie, String?>>, suspend (Int, Int) -> Unit) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val seriesEntry = zipEntries.entries.firstOrNull { it.key.lowercase().endsWith("series.csv") }?.value
        val episodesEntry = zipEntries.entries.firstOrNull { it.key.lowercase().endsWith("episodes.csv") }?.value
        val moviesEntry = zipEntries.entries.firstOrNull { it.key.lowercase().endsWith("movies.csv") }?.value

        val episodeMap = mutableMapOf<String, MutableMap<String, MutableSet<Int>>>() // key = series_imdb_id or series_tvdb_id
        val watchDatesMap = mutableMapOf<String, MutableSet<String>>()

        // Parse episodes.csv
        if (episodesEntry != null) {
            val lines = episodesEntry.decodeToString().lineSequence().filter { it.isNotBlank() }.iterator()
            if (lines.hasNext()) {
                val headers = parseCsvLine(lines.next()).map { it.trim().lowercase() }
                val tvdbIdx = headers.indexOfFirst { it in listOf("series_tvdb_id", "tvdb_id") }
                val imdbIdx = headers.indexOfFirst { it in listOf("series_imdb_id", "imdb_id") }
                val seasonIdx = headers.indexOfFirst { it == "season" }
                val epIdx = headers.indexOfFirst { it == "episode" }
                val watchedIdx = headers.indexOfFirst { it == "is_watched" }
                val watchedAtIdx = headers.indexOfFirst { it == "watched_at" }

                while (lines.hasNext()) {
                    val cols = parseCsvLine(lines.next())
                    val imdbId = if (imdbIdx != -1 && cols.size > imdbIdx) cols[imdbIdx].trim().takeIf { it.isNotBlank() } else null
                    val tvdbId = if (tvdbIdx != -1 && cols.size > tvdbIdx) cols[tvdbIdx].trim().takeIf { it.isNotBlank() } else null
                    val key = imdbId ?: tvdbId ?: continue

                    val season = if (seasonIdx != -1 && cols.size > seasonIdx) cols[seasonIdx].toIntOrNull() else null
                    val ep = if (epIdx != -1 && cols.size > epIdx) cols[epIdx].toIntOrNull() else null
                    val isWatched = if (watchedIdx != -1 && cols.size > watchedIdx) cols[watchedIdx].trim() in listOf("1", "true") else true
                    val watchedAt = if (watchedAtIdx != -1 && cols.size > watchedAtIdx) parseAndNormalizeWatchedDate(cols[watchedAtIdx]) else null

                    if (isWatched && season != null && ep != null && season > 0 && ep > 0) {
                        episodeMap.getOrPut(key) { mutableMapOf() }.getOrPut(season.toString()) { mutableSetOf() }.add(ep)
                    }
                    if (watchedAt != null) {
                        watchDatesMap.getOrPut(key) { mutableSetOf() }.add(watchedAt)
                    }
                }
            }
        }

        val allItems = mutableListOf<Pair<Movie, String?>>()

        // Parse series.csv
        if (seriesEntry != null) {
            val lines = seriesEntry.decodeToString().lineSequence().filter { it.isNotBlank() }.iterator()
            if (lines.hasNext()) {
                val headers = parseCsvLine(lines.next()).map { it.trim().lowercase() }
                val tvdbIdx = headers.indexOfFirst { it in listOf("tvdb_id", "tvdb") }
                val imdbIdx = headers.indexOfFirst { it in listOf("imdb_id", "imdb") }
                val titleIdx = headers.indexOfFirst { it == "title" }
                val statusIdx = headers.indexOfFirst { it == "status" }

                val rows = lines.asSequence().toList()
                val chunks = rows.chunked(20)
                for (chunk in chunks) {
                    val deferreds = chunk.map { line ->
                        async {
                            val cols = parseCsvLine(line)
                            val imdbId = if (imdbIdx != -1 && cols.size > imdbIdx) cols[imdbIdx].trim().takeIf { it.isNotBlank() } else null
                            val tvdbId = if (tvdbIdx != -1 && cols.size > tvdbIdx) cols[tvdbIdx].trim().takeIf { it.isNotBlank() } else null
                            val title = if (titleIdx != -1 && cols.size > titleIdx) cols[titleIdx].trim() else ""
                            val status = if (statusIdx != -1 && cols.size > statusIdx) cols[statusIdx].trim().lowercase() else ""

                            val tmdbMovie = resolveTmdb(imdbId, tvdbId, title, null, isTv = true) ?: return@async null
                            val matchKey = imdbId ?: tvdbId
                            val eps = matchKey?.let { episodeMap[it] }?.mapValues { it.value.sorted() }
                            val dates = matchKey?.let { watchDatesMap[it] } ?: emptySet()

                            val isDropped = status in listOf("dropped", "paused")
                            val movie = Movie(
                                id = tmdbMovie.id,
                                mediaType = "tv",
                                title = tmdbMovie.title,
                                name = tmdbMovie.name ?: title,
                                watched = !isDropped && (eps?.isNotEmpty() == true || status in listOf("watched", "completed")),
                                favorite = isDropped || status == "watching",
                                dropped = isDropped,
                                watchedEpisodes = eps,
                                syncStatus = "pending",
                                clientUpdatedAt = System.currentTimeMillis()
                            )
                            movie.extractedWatchDates.addAll(dates)
                            Pair(movie, null)
                        }
                    }
                    allItems.addAll(deferreds.awaitAll().filterNotNull())
                }
            }
        }

        // Parse movies.csv
        if (moviesEntry != null) {
            val lines = moviesEntry.decodeToString().lineSequence().filter { it.isNotBlank() }.iterator()
            if (lines.hasNext()) {
                val headers = parseCsvLine(lines.next()).map { it.trim().lowercase() }
                val tvdbIdx = headers.indexOfFirst { it in listOf("tvdb_id", "tvdb") }
                val imdbIdx = headers.indexOfFirst { it in listOf("imdb_id", "imdb") }
                val titleIdx = headers.indexOfFirst { it == "title" }
                val yearIdx = headers.indexOfFirst { it == "year" }
                val watchedIdx = headers.indexOfFirst { it == "is_watched" }
                val watchedAtIdx = headers.indexOfFirst { it == "watched_at" }

                val rows = lines.asSequence().toList()
                val chunks = rows.chunked(20)
                for (chunk in chunks) {
                    val deferreds = chunk.map { line ->
                        async {
                            val cols = parseCsvLine(line)
                            val imdbId = if (imdbIdx != -1 && cols.size > imdbIdx) cols[imdbIdx].trim().takeIf { it.isNotBlank() } else null
                            val tvdbId = if (tvdbIdx != -1 && cols.size > tvdbIdx) cols[tvdbIdx].trim().takeIf { it.isNotBlank() } else null
                            val title = if (titleIdx != -1 && cols.size > titleIdx) cols[titleIdx].trim() else ""
                            val year = if (yearIdx != -1 && cols.size > yearIdx) cols[yearIdx].trim() else null
                            val isWatched = if (watchedIdx != -1 && cols.size > watchedIdx) cols[watchedIdx].trim() in listOf("1", "true") else true
                            val watchedAt = if (watchedAtIdx != -1 && cols.size > watchedAtIdx) parseAndNormalizeWatchedDate(cols[watchedAtIdx]) else null

                            val tmdbMovie = resolveTmdb(imdbId, tvdbId, title, year, isTv = false) ?: return@async null
                            val movie = Movie(
                                id = tmdbMovie.id,
                                mediaType = "movie",
                                title = tmdbMovie.title ?: title,
                                watched = isWatched,
                                favorite = !isWatched,
                                watchedAt = watchedAt,
                                releaseYear = year,
                                syncStatus = "pending",
                                clientUpdatedAt = System.currentTimeMillis()
                            )
                            if (watchedAt != null) movie.extractedWatchDates.add(watchedAt)
                            Pair(movie, null)
                        }
                    }
                    allItems.addAll(deferreds.awaitAll().filterNotNull())
                }
            }
        }

        emitBatches(allItems, onProgress, onBatchReady)
    }

    private class RefractShowRaw(
        val imdbId: String?,
        val tvdbId: String?,
        val title: String,
        val status: String,
        val isFavorite: Boolean,
        val watchedEpisodes: Map<String, List<Int>>,
        val watchDates: Set<String>
    )

    private class RefractMovieRaw(
        val imdbId: String?,
        val tvdbId: String?,
        val title: String,
        val year: String?,
        val isWatched: Boolean,
        val isFavorite: Boolean,
        val watchedAt: String?
    )

    suspend fun migrateRefractJson(
        inputStream: InputStream,
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> },
        onBatchReady: suspend (List<Pair<Movie, String?>>, suspend (Int, Int) -> Unit) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val allItems = mutableListOf<Pair<Movie, String?>>()
        val showsChunk = mutableListOf<RefractShowRaw>()
        val moviesChunk = mutableListOf<RefractMovieRaw>()

        suspend fun flushShows() {
            if (showsChunk.isEmpty()) return
            val current = showsChunk.toList()
            showsChunk.clear()
            val deferreds = current.map { raw ->
                async {
                    val tmdbMovie = resolveTmdb(raw.imdbId, raw.tvdbId, raw.title, null, isTv = true) ?: return@async null
                    val isDropped = raw.status in listOf("dropped", "paused")
                    val movie = Movie(
                        id = tmdbMovie.id,
                        mediaType = "tv",
                        title = tmdbMovie.title,
                        name = tmdbMovie.name ?: raw.title,
                        watched = !isDropped && (raw.watchedEpisodes.isNotEmpty() || raw.status in listOf("watched", "completed")),
                        favorite = raw.isFavorite || isDropped || raw.status == "watching",
                        dropped = isDropped,
                        watchedEpisodes = if (raw.watchedEpisodes.isNotEmpty()) raw.watchedEpisodes else null,
                        syncStatus = "pending",
                        clientUpdatedAt = System.currentTimeMillis()
                    )
                    movie.extractedWatchDates.addAll(raw.watchDates)
                    Pair(movie, null)
                }
            }
            allItems.addAll(deferreds.awaitAll().filterNotNull())
        }

        suspend fun flushMovies() {
            if (moviesChunk.isEmpty()) return
            val current = moviesChunk.toList()
            moviesChunk.clear()
            val deferreds = current.map { raw ->
                async {
                    val tmdbMovie = resolveTmdb(raw.imdbId, raw.tvdbId, raw.title, raw.year, isTv = false) ?: return@async null
                    val movie = Movie(
                        id = tmdbMovie.id,
                        mediaType = "movie",
                        title = tmdbMovie.title ?: raw.title,
                        watched = raw.isWatched,
                        favorite = raw.isFavorite || !raw.isWatched,
                        watchedAt = raw.watchedAt,
                        releaseYear = raw.year,
                        syncStatus = "pending",
                        clientUpdatedAt = System.currentTimeMillis()
                    )
                    if (raw.watchedAt != null) movie.extractedWatchDates.add(raw.watchedAt)
                    Pair(movie, null)
                }
            }
            allItems.addAll(deferreds.awaitAll().filterNotNull())
        }

        try {
            val reader = JsonReader.of(inputStream.source().buffer())
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "shows" -> {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            showsChunk.add(readSingleShow(reader))
                            if (showsChunk.size >= 20) {
                                flushShows()
                            }
                        }
                        reader.endArray()
                        flushShows()
                    }
                    "movies" -> {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            moviesChunk.add(readSingleMovie(reader))
                            if (moviesChunk.size >= 20) {
                                flushMovies()
                            }
                        }
                        reader.endArray()
                        flushMovies()
                    }
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
        } catch (_: Exception) {
            // On stream end or format variance, continue with parsed elements
        }

        emitBatches(allItems, onProgress, onBatchReady)
    }

    private fun readSingleShow(reader: JsonReader): RefractShowRaw {
        var imdbId: String? = null
        var tvdbId: String? = null
        var title = ""
        var status = ""
        var isFavorite = false
        val epsMap = mutableMapOf<String, MutableSet<Int>>()
        val dates = mutableSetOf<String>()

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> {
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "tvdb" -> tvdbId = reader.nextStringOrNull()
                            "imdb" -> imdbId = reader.nextStringOrNull()
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                }
                "title" -> title = reader.nextStringOrNull() ?: ""
                "status" -> status = reader.nextStringOrNull()?.lowercase() ?: ""
                "is_favorite" -> isFavorite = reader.nextBooleanOrDefault(false)
                "seasons" -> {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        reader.beginObject()
                        var sNum = 1
                        while (reader.hasNext()) {
                            when (reader.nextName()) {
                                "number" -> sNum = reader.nextIntOrDefault(1)
                                "episodes" -> {
                                    reader.beginArray()
                                    while (reader.hasNext()) {
                                        reader.beginObject()
                                        var epNum = 0
                                        var isWatched = false
                                        var watchedAt: String? = null
                                        while (reader.hasNext()) {
                                            when (reader.nextName()) {
                                                "number" -> epNum = reader.nextIntOrDefault(0)
                                                "is_watched" -> isWatched = reader.nextBooleanOrDefault(false)
                                                "watched_at" -> watchedAt = reader.nextStringOrNull()
                                                else -> reader.skipValue()
                                            }
                                        }
                                        reader.endObject()
                                        if (isWatched && sNum > 0 && epNum > 0) {
                                            epsMap.getOrPut(sNum.toString()) { mutableSetOf() }.add(epNum)
                                        }
                                        if (!watchedAt.isNullOrBlank()) {
                                            parseAndNormalizeWatchedDate(watchedAt)?.let { dates.add(it) }
                                        }
                                    }
                                    reader.endArray()
                                }
                                else -> reader.skipValue()
                            }
                        }
                        reader.endObject()
                    }
                    reader.endArray()
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return RefractShowRaw(
            imdbId = imdbId,
            tvdbId = tvdbId,
            title = title,
            status = status,
            isFavorite = isFavorite,
            watchedEpisodes = epsMap.mapValues { it.value.sorted() },
            watchDates = dates
        )
    }

    private fun readSingleMovie(reader: JsonReader): RefractMovieRaw {
        var imdbId: String? = null
        var tvdbId: String? = null
        var title = ""
        var year: String? = null
        var isWatched = true
        var isFavorite = false
        var watchedAt: String? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> {
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "tvdb" -> tvdbId = reader.nextStringOrNull()
                            "imdb" -> imdbId = reader.nextStringOrNull()
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                }
                "title" -> title = reader.nextStringOrNull() ?: ""
                "year" -> year = reader.nextStringOrNull()
                "is_watched" -> isWatched = reader.nextBooleanOrDefault(true)
                "is_favorite" -> isFavorite = reader.nextBooleanOrDefault(false)
                "watched_at" -> watchedAt = parseAndNormalizeWatchedDate(reader.nextStringOrNull())
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return RefractMovieRaw(
            imdbId = imdbId,
            tvdbId = tvdbId,
            title = title,
            year = year,
            isWatched = isWatched,
            isFavorite = isFavorite,
            watchedAt = watchedAt
        )
    }

    private fun JsonReader.nextStringOrNull(): String? {
        if (peek() == JsonReader.Token.NULL) {
            nextNull<String>()
            return null
        }
        return nextString()
    }

    private fun JsonReader.nextBooleanOrDefault(default: Boolean): Boolean {
        return when (peek()) {
            JsonReader.Token.NULL -> {
                nextNull<Unit>()
                default
            }
            JsonReader.Token.BOOLEAN -> nextBoolean()
            JsonReader.Token.NUMBER -> nextInt() != 0
            JsonReader.Token.STRING -> nextString().trim().lowercase() in listOf("1", "true", "yes")
            else -> {
                skipValue()
                default
            }
        }
    }

    private fun JsonReader.nextIntOrDefault(default: Int): Int {
        return when (peek()) {
            JsonReader.Token.NULL -> {
                nextNull<Unit>()
                default
            }
            JsonReader.Token.NUMBER -> nextInt()
            JsonReader.Token.STRING -> nextString().toIntOrNull() ?: default
            else -> {
                skipValue()
                default
            }
        }
    }

    private suspend fun resolveTmdb(imdbId: String?, tvdbId: String?, title: String, year: String?, isTv: Boolean): Movie? {
        if (!imdbId.isNullOrBlank() && imdbId.startsWith("tt")) {
            val byImdb = movieLookup.findByImdbId(imdbId)
            if (byImdb != null) return byImdb
        }
        if (!tvdbId.isNullOrBlank()) {
            val byTvdb = movieLookup.findByTvdbId(tvdbId)
            if (byTvdb != null) return byTvdb
        }
        if (title.isNotBlank()) {
            return movieLookup.searchMediaWithYear(title, year, isTv = isTv)
        }
        return null
    }

    private suspend fun emitBatches(
        allItems: List<Pair<Movie, String?>>,
        onProgress: suspend (Int, Int) -> Unit,
        onBatchReady: suspend (List<Pair<Movie, String?>>, suspend (Int, Int) -> Unit) -> Unit
    ): Int {
        if (allItems.isEmpty()) return 0
        val chunks = allItems.chunked(50)
        var processed = 0
        for (chunk in chunks) {
            onBatchReady(chunk) { cur, _ ->
                onProgress(processed + cur, allItems.size)
            }
            processed += chunk.size
            onProgress(processed, allItems.size)
        }
        return allItems.size
    }
}
