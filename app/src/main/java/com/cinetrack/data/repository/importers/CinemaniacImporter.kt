package com.cinetrack.data.repository.importers

import com.cinetrack.data.model.Movie
import com.cinetrack.data.repository.importers.ImporterUtils.parseAndNormalizeWatchedDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.InputStream
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class CinemaniacImporter @Inject constructor() {

    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
        coerceInputValues = true
    }

    fun isCinemaniacJson(content: String, fileName: String? = null): Boolean {
        val preview = content.take(3000)
        if (preview.contains("\"app\":\"Cinemaniac\"", ignoreCase = true) ||
            preview.contains("\"app\": \"Cinemaniac\"", ignoreCase = true)) {
            return true
        }
        if (preview.contains("Cinemaniac", ignoreCase = true) &&
            (preview.contains("\"movies\"") || preview.contains("\"shows\"") || preview.contains("\"ratings\""))) {
            return true
        }
        if (preview.contains("\"id_movie\"") && preview.contains("\"seen\"")) {
            return true
        }
        if (preview.contains("\"id_tv\"") && (preview.contains("\"episodes_seen\"") || preview.contains("\"seasons\""))) {
            return true
        }
        if (fileName?.endsWith(".bak", ignoreCase = true) == true &&
            (preview.contains("\"movies\"") || preview.contains("\"shows\"") || preview.contains("backupVersion"))) {
            return true
        }
        return false
    }

    fun isCinemaniacZip(zipEntries: Map<String, ByteArray>): Boolean {
        for ((name, bytes) in zipEntries) {
            val lowerName = name.lowercase()
            if (lowerName.endsWith(".bak") || lowerName.contains("cinemaniac")) {
                val preview = bytes.take(3000).toByteArray().decodeToString()
                if (isCinemaniacJson(preview, name)) return true
            }
        }
        for ((name, bytes) in zipEntries) {
            val preview = bytes.take(1000).toByteArray().decodeToString().trimStart()
            if (preview.startsWith("{") && isCinemaniacJson(preview, name)) {
                return true
            }
        }
        return false
    }

    suspend fun migrateCinemaniacZip(
        zipEntries: Map<String, ByteArray>,
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> },
        onBatchReady: suspend (List<Pair<Movie, String?>>, suspend (Int, Int) -> Unit) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val targetEntry = zipEntries.entries.firstOrNull { (name, bytes) ->
            val preview = bytes.take(3000).toByteArray().decodeToString()
            isCinemaniacJson(preview, name)
        } ?: return@withContext 0

        migrateJsonStream(
            inputStream = java.io.ByteArrayInputStream(targetEntry.value),
            onProgress = onProgress,
            fileName = targetEntry.key,
            onBatchReady = onBatchReady
        )
    }

    suspend fun migrateJsonStream(
        inputStream: InputStream,
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> },
        fileName: String? = null,
        onBatchReady: suspend (List<Pair<Movie, String?>>, suspend (Int, Int) -> Unit) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val content = inputStream.bufferedReader().use { it.readText() }
        if (content.isBlank()) return@withContext 0

        val rootObj = try {
            json.parseToJsonElement(content).jsonObject
        } catch (e: Exception) {
            return@withContext 0
        }

        // 1. Parse user ratings: Cinemaniac uses negative IDs for TV shows, positive for movies
        val movieRatings = mutableMapOf<Long, Double>()
        val showRatings = mutableMapOf<Long, Double>()
        val fallbackRatings = mutableMapOf<Long, Double>()
        val ratingsArray = rootObj["ratings"] as? JsonArray
        ratingsArray?.forEach { rElem ->
            val rObj = rElem as? JsonObject ?: return@forEach
            val id = rObj["id"]?.jsonPrimitive?.longOrNull ?: return@forEach
            val rating = rObj["r"]?.jsonPrimitive?.doubleOrNull ?: return@forEach
            if (id > 0) {
                movieRatings[id] = rating
                fallbackRatings[id] = rating
            } else if (id < 0) {
                val absId = abs(id)
                showRatings[absId] = rating
                fallbackRatings[absId] = rating
            }
        }

        // 2. Parse custom categories / lists
        val categoriesMap = mutableMapOf<String, String>()
        val categoriesArray = rootObj["categories"] as? JsonArray
        categoriesArray?.forEach { cElem ->
            val cObj = cElem as? JsonObject ?: return@forEach
            val id = cObj["id"]?.jsonPrimitive?.contentOrNull?.trim() ?: return@forEach
            val name = cObj["name"]?.jsonPrimitive?.contentOrNull?.trim() ?: return@forEach
            if (id.isNotBlank() && name.isNotBlank()) {
                categoriesMap[id] = name
            }
        }

        val moviesWithFolders = mutableListOf<Pair<Movie, String?>>()

        // 3. Process Movies
        val moviesArray = rootObj["movies"] as? JsonArray
        moviesArray?.forEach { mElem ->
            val mObj = mElem as? JsonObject ?: return@forEach
            val idMovie = mObj["id_movie"]?.jsonPrimitive?.longOrNull ?: return@forEach
            if (idMovie <= 0L) return@forEach

            val title = mObj["title"]?.jsonPrimitive?.contentOrNull?.trim() ?: "Unknown ($idMovie)"
            val (releaseDate, releaseYear) = formatEpochOrStringDate(mObj["year"])
            val voteAverage = mObj["rating"]?.jsonPrimitive?.doubleOrNull
            val duration = mObj["duration"]?.jsonPrimitive?.intOrNull
            val runtime = if (duration != null && duration > 0) duration else null
            val genreIds = parseGenreIds(mObj["genres"])
            val overview = mObj["overview"]?.jsonPrimitive?.contentOrNull?.trim()?.ifBlank { null }
            val rawBackdrop = mObj["backdrop"]?.jsonPrimitive?.contentOrNull?.trim()?.ifBlank { null }
            val backdropPath = if (!rawBackdrop.isNullOrBlank()) {
                if (rawBackdrop.startsWith("/")) rawBackdrop else "/$rawBackdrop"
            } else null

            val seen = mObj["seen"]?.jsonPrimitive?.intOrNull ?: 0
            val isWatched = seen == 1
            val isFavorite = seen == 0 // In Cinemaniac, seen == 0 represents the user's Watchlist ("Da Vedere")
            val watchedAt = if (isWatched) parseWatchedDate(mObj["date"]) else null
            val personalRating = movieRatings[idMovie] ?: fallbackRatings[idMovie]
            val personalNote = mObj["note"]?.jsonPrimitive?.contentOrNull?.trim()?.ifBlank { null }

            val movie = Movie(
                id = idMovie,
                mediaType = "movie",
                title = title,
                overview = overview,
                backdropPath = backdropPath,
                voteAverage = voteAverage,
                releaseDate = releaseDate,
                releaseYear = releaseYear,
                runtime = runtime,
                genreIds = genreIds,
                watched = isWatched,
                favorite = isFavorite,
                personalRating = personalRating,
                personalNote = personalNote,
                watchedAt = watchedAt,
                syncStatus = "pending",
                clientUpdatedAt = System.currentTimeMillis()
            )

            val folders = parseCategories(mObj["categories"], categoriesMap)
            if (folders.isEmpty()) {
                moviesWithFolders.add(Pair(movie, null))
            } else {
                folders.forEach { folder ->
                    moviesWithFolders.add(Pair(movie, folder))
                }
            }
        }

        // 4. Process TV Shows
        val showsArray = rootObj["shows"] as? JsonArray
        showsArray?.forEach { sElem ->
            val sObj = sElem as? JsonObject ?: return@forEach
            val idTv = sObj["id_tv"]?.jsonPrimitive?.longOrNull ?: return@forEach
            if (idTv <= 0L) return@forEach

            val title = sObj["title"]?.jsonPrimitive?.contentOrNull?.trim() ?: "Unknown ($idTv)"
            val (firstAirDate, releaseYear) = formatEpochOrStringDate(sObj["first_air_date"])
            val (lastAirDate, _) = formatEpochOrStringDate(sObj["last_air_date"])
            val voteAverage = sObj["rating"]?.jsonPrimitive?.doubleOrNull
            val duration = sObj["duration"]?.jsonPrimitive?.intOrNull
            val episodeRunTime = if (duration != null && duration > 0) listOf(duration) else null
            val genreIds = parseGenreIds(sObj["genres"])
            val overview = sObj["overview"]?.jsonPrimitive?.contentOrNull?.trim()?.ifBlank { null }
            val rawBackdrop = sObj["backdrop"]?.jsonPrimitive?.contentOrNull?.trim()?.ifBlank { null }
            val backdropPath = if (!rawBackdrop.isNullOrBlank()) {
                if (rawBackdrop.startsWith("/")) rawBackdrop else "/$rawBackdrop"
            } else null

            val watching = sObj["watching"]?.jsonPrimitive?.intOrNull ?: 1
            val episodes = sObj["episodes"]?.jsonPrimitive?.intOrNull ?: 0
            val episodesSeen = sObj["episodes_seen"]?.jsonPrimitive?.intOrNull ?: 0
            val watchedEpsMap = parseWatchedEpisodes(sObj["seasons"])
            val totalWatchedEps = if (episodesSeen > 0) episodesSeen else watchedEpsMap.values.sumOf { it.size }
            val totalEps = if (episodes > 0) episodes else null

            val isFullyWatched = totalEps != null && totalWatchedEps >= totalEps && totalEps > 0
            val hasWatchedEps = totalWatchedEps > 0 || watchedEpsMap.isNotEmpty()

            val isWatched = isFullyWatched || hasWatchedEps
            val isFavorite = watching == 1 || !isFullyWatched
            val watchedAt = if (isWatched) parseWatchedDate(sObj["date"]) else null
            val personalRating = showRatings[idTv] ?: fallbackRatings[idTv]
            val personalNote = sObj["note"]?.jsonPrimitive?.contentOrNull?.trim()?.ifBlank { null }

            val numSeasons = watchedEpsMap.keys.mapNotNull { it.toIntOrNull() }.maxOrNull()
            val calculatedProgress = if (totalEps != null && totalEps > 0) {
                (totalWatchedEps.toDouble() / totalEps).coerceIn(0.0, 1.0)
            } else if (isFullyWatched) 1.0 else 0.0

            val show = Movie(
                id = idTv,
                mediaType = "tv",
                title = null,
                name = title,
                overview = overview,
                backdropPath = backdropPath,
                voteAverage = voteAverage,
                firstAirDate = firstAirDate,
                lastAirDate = lastAirDate,
                releaseYear = releaseYear,
                episodeRunTime = episodeRunTime,
                genreIds = genreIds,
                numberOfEpisodes = totalEps,
                numberOfSeasons = numSeasons,
                progress = calculatedProgress,
                watchedEpisodes = if (watchedEpsMap.isNotEmpty()) watchedEpsMap else null,
                watched = isWatched,
                favorite = isFavorite,
                personalRating = personalRating,
                personalNote = personalNote,
                watchedAt = watchedAt,
                syncStatus = "pending",
                clientUpdatedAt = System.currentTimeMillis()
            )

            val folders = parseCategories(sObj["categories"], categoriesMap)
            if (folders.isEmpty()) {
                moviesWithFolders.add(Pair(show, null))
            } else {
                folders.forEach { folder ->
                    moviesWithFolders.add(Pair(show, folder))
                }
            }
        }

        if (moviesWithFolders.isEmpty()) return@withContext 0

        // 5. Emit in batches of 100 items
        val chunks = moviesWithFolders.chunked(100)
        var processed = 0
        for (chunk in chunks) {
            onBatchReady(chunk) { cur, _ ->
                onProgress(processed + cur, moviesWithFolders.size)
            }
            processed += chunk.size
            onProgress(processed, moviesWithFolders.size)
        }

        return@withContext moviesWithFolders.size
    }

    private fun formatEpochOrStringDate(element: kotlinx.serialization.json.JsonElement?): Pair<String?, String?> {
        if (element == null) return Pair(null, null)
        val primitive = try { element.jsonPrimitive } catch (_: Exception) { return Pair(null, null) }
        val longVal = primitive.longOrNull
        if (longVal != null) {
            if (longVal <= 0L) return Pair(null, null)
            if (longVal in 1880L..2150L) {
                return Pair(null, longVal.toString())
            }
            val instant = when {
                longVal > 100_000_000_000L -> try { Instant.ofEpochMilli(longVal) } catch (_: Exception) { null }
                longVal > 100_000_000L -> try { Instant.ofEpochSecond(longVal) } catch (_: Exception) { null }
                else -> null
            }
            if (instant != null) {
                val zdt = instant.atZone(ZoneOffset.UTC)
                val dateStr = zdt.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val yearStr = zdt.year.toString()
                return Pair(dateStr, yearStr)
            }
        }
        val str = primitive.contentOrNull?.trim() ?: return Pair(null, null)
        if (str.isBlank()) return Pair(null, null)
        val iso = parseAndNormalizeWatchedDate(str)
        val datePart = iso?.take(10) ?: if (str.length >= 4 && str.take(4).all { it.isDigit() }) str.take(10) else null
        val yearPart = datePart?.take(4) ?: if (str.length >= 4 && str.take(4).all { it.isDigit() }) str.take(4) else null
        return Pair(datePart, yearPart)
    }

    private fun parseWatchedDate(element: kotlinx.serialization.json.JsonElement?): String? {
        if (element == null) return null
        val primitive = try { element.jsonPrimitive } catch (_: Exception) { return null }
        val longVal = primitive.longOrNull
        if (longVal != null) {
            if (longVal <= 0L) return null
            val instant = when {
                longVal > 100_000_000_000L -> try { Instant.ofEpochMilli(longVal) } catch (_: Exception) { null }
                longVal > 100_000_000L -> try { Instant.ofEpochSecond(longVal) } catch (_: Exception) { null }
                else -> null
            }
            if (instant != null) return instant.toString()
        }
        val str = primitive.contentOrNull?.trim() ?: return null
        if (str.isBlank()) return null
        return parseAndNormalizeWatchedDate(str)
    }

    private fun parseGenreIds(element: kotlinx.serialization.json.JsonElement?): List<Long>? {
        if (element == null) return null
        return try {
            when (element) {
                is JsonArray -> element.mapNotNull { it.jsonPrimitive.longOrNull }
                else -> {
                    val str = element.jsonPrimitive.contentOrNull ?: return null
                    str.split(",")
                        .mapNotNull { it.trim().toLongOrNull() }
                        .filter { it > 0L }
                }
            }
        } catch (_: Exception) {
            null
        }?.ifEmpty { null }
    }

    private fun parseCategories(
        element: kotlinx.serialization.json.JsonElement?,
        categoriesMap: Map<String, String>
    ): List<String> {
        if (element == null) return emptyList()
        val tokens = mutableListOf<String>()
        try {
            when (element) {
                is JsonArray -> {
                    element.forEach { item ->
                        val token = item.jsonPrimitive.contentOrNull?.trim()
                        if (!token.isNullOrBlank()) tokens.add(token)
                    }
                }
                else -> {
                    val str = element.jsonPrimitive.contentOrNull?.trim()
                    if (!str.isNullOrBlank()) {
                        tokens.addAll(str.split(",").map { it.trim() }.filter { it.isNotBlank() })
                    }
                }
            }
        } catch (_: Exception) {
            return emptyList()
        }

        return tokens.mapNotNull { token ->
            val mapped = categoriesMap[token]
            when {
                !mapped.isNullOrBlank() -> mapped
                token.all { it.isDigit() } -> null
                else -> token
            }
        }.distinct()
    }

    private fun parseWatchedEpisodes(element: kotlinx.serialization.json.JsonElement?): Map<String, List<Int>> {
        if (element == null) return emptyMap()
        val result = mutableMapOf<String, MutableList<Int>>()

        val jsonArray = try {
            when (element) {
                is JsonArray -> element
                else -> {
                    val str = element.jsonPrimitive.contentOrNull?.trim()
                    if (!str.isNullOrBlank() && str.startsWith("[")) {
                        json.parseToJsonElement(str).jsonArray
                    } else null
                }
            }
        } catch (_: Exception) {
            null
        } ?: return emptyMap()

        for (sIndex in 0 until jsonArray.size) {
            val seasonNum = (sIndex + 1).toString()
            val sArr = try { jsonArray[sIndex].jsonArray } catch (_: Exception) { null } ?: continue
            for (eIndex in 0 until sArr.size) {
                val epNum = eIndex + 1
                val seenVal = try { sArr[eIndex].jsonPrimitive.intOrNull ?: 0 } catch (_: Exception) { 0 }
                if (seenVal == 1) {
                    result.getOrPut(seasonNum) { mutableListOf() }.add(epNum)
                }
            }
        }
        return result.mapValues { it.value.sorted() }
    }
}
