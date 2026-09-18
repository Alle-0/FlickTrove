package com.cinetrack.data.repository.importers

import com.cinetrack.data.model.Movie
import com.cinetrack.data.repository.importers.ImporterUtils.parseAndNormalizeWatchedDate
import com.cinetrack.data.repository.importers.ImporterUtils.parseCsvLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SofaImporter @Inject constructor() {

    private class SofaItemData(
        val tmdbId: Long,
        val mediaType: String,
        var title: String = "",
        var isWatched: Boolean = false,
        var isFavorite: Boolean = false,
        var personalRating: Double? = null,
        var personalNote: String? = null,
        var watchedAt: String? = null,
        val folders: MutableSet<String> = mutableSetOf()
    )

    fun isSofaZip(zipEntries: Map<String, ByteArray>): Boolean {
        val keys = zipEntries.keys.map { it.lowercase() }
        val hasItems = keys.any { it.endsWith("items.csv") }
        val hasCategoriesOrReadme = keys.any { it.endsWith("categories.csv") } || keys.any { it.endsWith("readme.md") }
        return hasItems && hasCategoriesOrReadme
    }

    suspend fun migrateSofaZip(
        zipEntries: Map<String, ByteArray>,
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> },
        onBatchReady: suspend (List<Pair<Movie, String?>>, suspend (Int, Int) -> Unit) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val itemsEntry = zipEntries.entries.firstOrNull { it.key.lowercase().endsWith("items.csv") }?.value
            ?: return@withContext 0

        val itemsMap = mutableMapOf<String, SofaItemData>()
        parseItemsCsv(itemsEntry.decodeToString(), itemsMap)

        emitImportedBatches(itemsMap, onProgress, onBatchReady)
    }

    private fun parseItemsCsv(csvContent: String, itemsMap: MutableMap<String, SofaItemData>) {
        val lines = csvContent.lineSequence().filter { it.isNotBlank() }.iterator()
        if (!lines.hasNext()) return
        val headers = parseCsvLine(lines.next()).map { it.trim().lowercase() }

        val apiIdIdx = headers.indexOfFirst { it in listOf("api_id", "api id", "tmdb_id", "tmdbid") }
        val apiSourceIdx = headers.indexOfFirst { it in listOf("api_source", "api source", "source") }
        val nameIdx = headers.indexOfFirst { it in listOf("name", "title", "item_name") }
        val categoryIdx = headers.indexOfFirst { it in listOf("category", "category_name", "type", "media_type") }
        val listIdx = headers.indexOfFirst { it in listOf("list", "list_name", "section", "folder") }
        val notesIdx = headers.indexOfFirst { it in listOf("notes", "note", "comment") }
        val completedDateIdx = headers.indexOfFirst { it in listOf("completed_date", "date_completed", "watched_date", "watched_at") }
        val ratingIdx = headers.indexOfFirst { it in listOf("rating", "user_rating", "score") }

        while (lines.hasNext()) {
            val cols = parseCsvLine(lines.next())

            // Guardrail: Verify that api_source is strictly TMDB or empty with valid TMDB ID
            val apiSource = if (apiSourceIdx != -1 && cols.size > apiSourceIdx) cols[apiSourceIdx].trim().lowercase() else ""
            val rawApiId = if (apiIdIdx != -1 && cols.size > apiIdIdx) cols[apiIdIdx].trim() else ""
            val tmdbId = rawApiId.toLongOrNull()

            if (tmdbId == null || tmdbId <= 0L) continue
            if (apiSource.isNotBlank() && apiSource != "tmdb") continue

            // Filter by Category: Only import Movies and TV Shows (Sofa also tracks books, podcasts, etc.)
            val category = if (categoryIdx != -1 && cols.size > categoryIdx) cols[categoryIdx].trim().lowercase() else ""
            val isMovie = category.contains("movie") || category.contains("film")
            val isTv = category.contains("tv") || category.contains("show") || category.contains("series")
            if (!isMovie && !isTv && category.isNotBlank()) continue

            val mediaType = if (isTv) "tv" else "movie"
            val title = if (nameIdx != -1 && cols.size > nameIdx) cols[nameIdx].trim() else "Unknown ($tmdbId)"
            val listName = if (listIdx != -1 && cols.size > listIdx) cols[listIdx].trim() else ""
            val note = if (notesIdx != -1 && cols.size > notesIdx) cols[notesIdx].trim().take(5000) else null
            val rawCompleted = if (completedDateIdx != -1 && cols.size > completedDateIdx) cols[completedDateIdx].trim() else ""
            val completedDate = parseAndNormalizeWatchedDate(rawCompleted)
            val rating = if (ratingIdx != -1 && cols.size > ratingIdx) cols[ratingIdx].toDoubleOrNull() else null

            val isWatched = !completedDate.isNullOrBlank()
            val item = itemsMap.getOrPut("${mediaType}_$tmdbId") {
                SofaItemData(
                    tmdbId = tmdbId,
                    mediaType = mediaType,
                    title = title,
                    isWatched = isWatched,
                    isFavorite = !isWatched,
                    personalRating = rating,
                    personalNote = note,
                    watchedAt = completedDate
                )
            }

            if (isWatched) {
                item.isWatched = true
                if (item.watchedAt == null) item.watchedAt = completedDate
            }
            if (!note.isNullOrBlank() && item.personalNote.isNullOrBlank()) {
                item.personalNote = note
            }
            if (listName.isNotBlank() && listName.lowercase() !in listOf("all", "default")) {
                item.folders.add(listName)
            }
        }
    }

    private suspend fun emitImportedBatches(
        itemsMap: Map<String, SofaItemData>,
        onProgress: suspend (Int, Int) -> Unit,
        onBatchReady: suspend (List<Pair<Movie, String?>>, suspend (Int, Int) -> Unit) -> Unit
    ): Int {
        val moviesWithFolders = mutableListOf<Pair<Movie, String?>>()

        for (item in itemsMap.values) {
            val movie = Movie(
                id = item.tmdbId,
                mediaType = item.mediaType,
                title = if (item.mediaType == "movie") item.title else null,
                name = if (item.mediaType == "tv") item.title else null,
                watched = item.isWatched,
                favorite = item.isFavorite,
                personalRating = item.personalRating,
                personalNote = item.personalNote,
                watchedAt = item.watchedAt,
                syncStatus = "pending",
                clientUpdatedAt = System.currentTimeMillis()
            )

            if (item.folders.isEmpty()) {
                moviesWithFolders.add(Pair(movie, null))
            } else {
                item.folders.forEach { folder ->
                    moviesWithFolders.add(Pair(movie, folder))
                }
            }
        }

        if (moviesWithFolders.isEmpty()) return 0

        val chunks = moviesWithFolders.chunked(100)
        var processed = 0
        for (chunk in chunks) {
            onBatchReady(chunk) { cur, _ ->
                onProgress(processed + cur, moviesWithFolders.size)
            }
            processed += chunk.size
            onProgress(processed, moviesWithFolders.size)
        }

        return itemsMap.size
    }
}
