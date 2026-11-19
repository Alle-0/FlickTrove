package com.cinetrack.data.repository

import android.content.Context
import com.cinetrack.data.api.IrisWeeklyResponse
import com.cinetrack.data.api.SemanticSearchApi
import com.cinetrack.data.model.IrisWeeklyCacheDto
import com.cinetrack.data.model.IrisWeeklyMovieDto
import com.cinetrack.data.model.IrisWeeklyPicksData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IrisWeeklyRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: SemanticSearchApi
) {
    companion object {
        private const val CACHE_FILE_NAME = "iris_weekly_cache.json"
        private const val MAX_CACHE_AGE_MS = 7L * 24 * 60 * 60 * 1000L // 7 giorni
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    private val cacheFile: File by lazy {
        File(context.filesDir, CACHE_FILE_NAME)
    }

    // In-memory cache multi-cluster per rotazioni a zero millisecondi e zero chiamate di rete
    private val memoryCacheByOffset = java.util.concurrent.ConcurrentHashMap<Int, IrisWeeklyPicksData>()

    suspend fun getCachedPicks(clusterOffset: Int? = null): IrisWeeklyPicksData? = withContext(Dispatchers.IO) {
        if (clusterOffset != null) {
            val inMemory = memoryCacheByOffset[clusterOffset]
            if (inMemory != null && isCacheValid(inMemory)) {
                return@withContext inMemory
            }
        }

        if (!cacheFile.exists()) return@withContext null

        runCatching {
            val content = cacheFile.readText()
            val dto = json.decodeFromString<IrisWeeklyCacheDto>(content)
            val parsed = IrisWeeklyPicksData(
                weekNumber = dto.weekNumber,
                year = dto.year,
                focusClusterTitle = dto.focusClusterTitle,
                focusSubtitle = dto.focusSubtitle,
                usedSeedTitles = dto.usedSeedTitles,
                timestamp = dto.timestamp,
                clusterOffset = dto.clusterOffset,
                hasMultipleClusters = dto.hasMultipleClusters,
                candidateMovies = dto.movies.map { it.toMovieResult() }
            )
            memoryCacheByOffset[parsed.clusterOffset] = parsed
            parsed
        }.getOrNull()
    }

    suspend fun savePicks(
        data: IrisWeeklyPicksData,
        collectionIdMap: Map<Long, Long?>
    ): Unit = withContext(Dispatchers.IO) {
        memoryCacheByOffset[data.clusterOffset] = data
        runCatching {
            val movieDtos = data.candidateMovies.map { movie ->
                IrisWeeklyMovieDto.fromMovieResult(movie, collectionIdMap[movie.id])
            }
            val dto = IrisWeeklyCacheDto(
                weekNumber = data.weekNumber,
                year = data.year,
                focusClusterTitle = data.focusClusterTitle,
                focusSubtitle = data.focusSubtitle,
                usedSeedTitles = data.usedSeedTitles,
                timestamp = data.timestamp,
                clusterOffset = data.clusterOffset,
                hasMultipleClusters = data.hasMultipleClusters,
                movies = movieDtos
            )
            val text = json.encodeToString(IrisWeeklyCacheDto.serializer(), dto)
            cacheFile.writeText(text)
        }.onFailure { e ->
            android.util.Log.w("IrisWeeklyRepository", "Non-fatal: failed to write iris weekly cache", e)
        }
    }

    fun isCacheValid(cache: IrisWeeklyPicksData): Boolean {
        val cal = Calendar.getInstance()
        val currentWeek = cal.get(Calendar.WEEK_OF_YEAR)
        val currentYear = cal.get(Calendar.YEAR)

        val isSameWeek = cache.weekNumber == currentWeek && cache.year == currentYear
        val isNotExpired = (System.currentTimeMillis() - cache.timestamp) < MAX_CACHE_AGE_MS
        return isSameWeek && isNotExpired
    }

    suspend fun fetchRemotePicks(seedIds: List<Long>, lang: String): Result<IrisWeeklyResponse> = runCatching {
        val seedsParam = seedIds.joinToString(",")
        val response = api.getIrisWeeklyPicks(seeds = seedsParam, lang = lang)
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code()}: ${response.message()}")
        }
        val body = response.body() ?: throw Exception("Empty response body from IRIS Weekly endpoint")
        if (body.error != null && body.results.isEmpty()) {
            throw Exception(body.error)
        }
        body
    }

    fun clearMemoryCache() {
        memoryCacheByOffset.clear()
    }
}
