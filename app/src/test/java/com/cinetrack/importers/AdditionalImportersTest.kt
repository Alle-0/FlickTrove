package com.cinetrack.importers

import com.cinetrack.data.model.Movie
import com.cinetrack.data.repository.importers.BingersImporter
import com.cinetrack.data.repository.importers.MovieLookupService
import com.cinetrack.data.repository.importers.RefractImporter
import com.cinetrack.data.repository.importers.SofaImporter
import com.cinetrack.data.repository.importers.UniversalCsvImporter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class AdditionalImportersTest {

    private val bingersImporter = BingersImporter()
    private val sofaImporter = SofaImporter()

    // Mock MovieLookupService for Refract and UniversalCsv tests
    private val mockMovieLookup = object : MovieLookupService {
        override suspend fun findByImdbId(imdbId: String): Movie? {
            return when (imdbId) {
                "tt0060196" -> Movie(id = 429, title = "The Good, the Bad and the Ugly", mediaType = "movie")
                "tt0944947" -> Movie(id = 1399, name = "Game of Thrones", mediaType = "tv")
                "tt0903747" -> Movie(id = 1396, name = "Breaking Bad", mediaType = "tv")
                else -> null
            }
        }

        override suspend fun findByTvdbId(tvdbId: String): Movie? {
            return when (tvdbId) {
                "121361" -> Movie(id = 1399, name = "Game of Thrones", mediaType = "tv")
                "81189" -> Movie(id = 1396, name = "Breaking Bad", mediaType = "tv")
                else -> null
            }
        }

        override suspend fun searchMediaWithYear(query: String, year: String?, isTv: Boolean): Movie? {
            return if (isTv) {
                Movie(id = 99991, name = query, mediaType = "tv")
            } else {
                Movie(id = 99992, title = query, mediaType = "movie")
            }
        }
    }

    private val refractImporter = RefractImporter(mockMovieLookup)
    private val universalCsvImporter = UniversalCsvImporter(mockMovieLookup)

    // ==========================================
    // 1. Bingers Importer Tests
    // ==========================================

    @Test
    fun testBingersZipRecognition() {
        val validZip = mapOf(
            "library.csv" to ByteArray(0),
            "watches.csv" to ByteArray(0)
        )
        assertTrue(bingersImporter.isBingersZip(validZip))

        val invalidZip = mapOf(
            "random.csv" to ByteArray(0)
        )
        assertFalse(bingersImporter.isBingersZip(invalidZip))
    }

    @Test
    fun testBingersImportWithCommasAndEpisodes() = runTest {
        val libraryCsv = """
            type,tmdb_id,imdb_id,title,year,status,rating,watched_at
            movie,429,tt0060196,"Il Buono, il Brutto, il Cattivo",1966,watched,9.5,2024-05-10T20:00:00Z
            tv,1399,tt0944947,"Game of Thrones",2011,watching,,
            tv,1396,tt0903747,"Breaking Bad",2008,completed,10.0,2023-01-01T12:00:00Z
        """.trimIndent()

        val watchesCsv = """
            tmdb_id,season,episode,date_watched,type
            1399,1,1,2024-06-01T21:00:00Z,tv
            1399,1,2,2024-06-02T21:00:00Z,tv
            1399,2,1,2024-06-10T21:00:00Z,tv
        """.trimIndent()

        val ratingsCsv = """
            tmdb_id,rating
            1399,9.0
        """.trimIndent()

        val listsCsv = """
            list_name,tmdb_id,type,title
            "Epic Masterpieces",429,movie,"Il Buono, il Brutto, il Cattivo"
            "Fantasy, Sci-Fi & Magic",1399,tv,"Game of Thrones"
        """.trimIndent()

        val zipEntries = mapOf(
            "library.csv" to libraryCsv.toByteArray(),
            "watches.csv" to watchesCsv.toByteArray(),
            "ratings.csv" to ratingsCsv.toByteArray(),
            "lists.csv" to listsCsv.toByteArray()
        )

        val imported = mutableListOf<Pair<Movie, String?>>()
        val count = bingersImporter.migrateBingersZip(zipEntries) { batch, _ ->
            imported.addAll(batch)
        }

        assertEquals(3, count)

        // Verify Il Buono, il Brutto, il Cattivo with comma in title
        val movie = imported.first { it.first.id == 429L }.first
        assertEquals("Il Buono, il Brutto, il Cattivo", movie.title)
        assertTrue(movie.watched)
        assertEquals(9.5, movie.personalRating!!, 0.01)
        assertEquals("Epic Masterpieces", imported.first { it.first.id == 429L }.second)

        // Verify Game of Thrones episodes and folder with comma
        val got = imported.first { it.first.id == 1399L }.first
        assertTrue(got.watched) // has episodes watched
        assertEquals(listOf(1, 2), got.watchedEpisodes?.get("1"))
        assertEquals(listOf(1), got.watchedEpisodes?.get("2"))
        assertEquals(9.0, got.personalRating!!, 0.01)
        assertEquals("Fantasy, Sci-Fi & Magic", imported.first { it.first.id == 1399L }.second)
    }

    // ==========================================
    // 2. Sofa Importer Tests
    // ==========================================

    @Test
    fun testSofaZipRecognition() {
        val validZip = mapOf(
            "items.csv" to ByteArray(0),
            "categories.csv" to ByteArray(0)
        )
        assertTrue(sofaImporter.isSofaZip(validZip))
    }

    @Test
    fun testSofaStrictTmdbValidationAndCategoryFiltering() = runTest {
        val itemsCsv = """
            api_id,api_source,name,category,list,notes,completed_date,rating
            1917,tmdb,"1917",Movies,"War Favorites","Masterpiece, filmed in one shot",2024-01-15,9.0
            300,tvdb,"300",Movies,"Sparta",,,
            999,itunes,"The Hobbit",Audiobooks,"Books",,,
            1399,tmdb,"Game of Thrones",TV Shows,"Fantasy","Great show, loved season 1-4",2024-02-01,8.5
            1234,igdb,"The Witcher 3",Video Games,"Games",,,
        """.trimIndent()

        val zipEntries = mapOf(
            "items.csv" to itemsCsv.toByteArray(),
            "categories.csv" to "category\nMovies\nTV Shows".toByteArray()
        )

        val imported = mutableListOf<Pair<Movie, String?>>()
        val count = sofaImporter.migrateSofaZip(zipEntries) { batch, _ ->
            imported.addAll(batch)
        }

        // Only TMDB items (1917 and Game of Thrones) should be imported.
        // 300 (tvdb), The Hobbit (Audiobooks/itunes) and Witcher 3 (igdb) MUST be ignored!
        assertEquals(2, count)

        val movie1917 = imported.first { it.first.id == 1917L }.first
        assertEquals("1917", movie1917.title)
        assertTrue(movie1917.watched)
        assertEquals("Masterpiece, filmed in one shot", movie1917.personalNote)
        assertEquals("War Favorites", imported.first { it.first.id == 1917L }.second)

        val got = imported.first { it.first.id == 1399L }.first
        assertEquals("tv", got.mediaType)
        assertEquals("Game of Thrones", got.name)
        assertTrue(got.watched)
        assertEquals("Fantasy", imported.first { it.first.id == 1399L }.second)
    }

    // ==========================================
    // 3. Refract Importer Tests (Streaming JsonReader)
    // ==========================================

    @Test
    fun testRefractJsonStreamingImport() = runTest {
        val refractJson = """
        {
          "shows": [
            {
              "id": { "tvdb": "121361", "imdb": "tt0944947" },
              "title": "Game of Thrones",
              "status": "watching",
              "is_favorite": true,
              "seasons": [
                {
                  "number": 1,
                  "episodes": [
                    { "number": 1, "is_watched": true, "watched_at": "2024-01-01T20:00:00Z" },
                    { "number": 2, "is_watched": true, "watched_at": "2024-01-02T20:00:00Z" }
                  ]
                },
                {
                  "number": 2,
                  "episodes": [
                    { "number": 1, "is_watched": true, "watched_at": "2024-01-10T20:00:00Z" },
                    { "number": 2, "is_watched": false }
                  ]
                }
              ]
            }
          ],
          "movies": [
            {
              "id": { "imdb": "tt0060196" },
              "title": "Il Buono, il Brutto, il Cattivo",
              "year": "1966",
              "is_watched": true,
              "watched_at": "2024-03-01T15:00:00Z"
            }
          ]
        }
        """.trimIndent()

        assertTrue(refractImporter.isRefractJson(refractJson))

        val imported = mutableListOf<Pair<Movie, String?>>()
        val count = refractImporter.migrateRefractJson(
            inputStream = ByteArrayInputStream(refractJson.toByteArray())
        ) { batch, _ ->
            imported.addAll(batch)
        }

        assertEquals(2, count)

        // 1. Check TV Show
        val show = imported.first { it.first.id == 1399L }.first
        assertEquals("tv", show.mediaType)
        assertTrue(show.watched)
        assertEquals(listOf(1, 2), show.watchedEpisodes?.get("1"))
        assertEquals(listOf(1), show.watchedEpisodes?.get("2"))
        assertTrue(show.extractedWatchDates.contains("2024-01-01T20:00:00Z"))

        // 2. Check Movie
        val movie = imported.first { it.first.id == 429L }.first
        assertEquals("movie", movie.mediaType)
        assertTrue(movie.watched)
        assertEquals("2024-03-01T15:00:00Z", movie.watchedAt)
    }

    // ==========================================
    // 4. Universal CSV Importer (Seenr / Sofa single CSV)
    // ==========================================

    @Test
    fun testUniversalCsvWithSeenrHeadersAndSofaApiSource() = runTest {
        val seenrCsv = """
            title,media_type,series_imdb_id,rating,watched_date,status,list_title
            "Breaking Bad",tv,tt0903747,10.0,2023-05-20,watched,"All Time Top 10"
        """.trimIndent()

        val importedSeenr = mutableListOf<Pair<Movie, String?>>()
        val seenrCount = universalCsvImporter.migrateCsvStream(
            inputStream = ByteArrayInputStream(seenrCsv.toByteArray())
        ) { batch, _ ->
            importedSeenr.addAll(batch)
        }

        assertEquals(1, seenrCount)
        val bb = importedSeenr.first().first
        assertEquals(1396L, bb.id)
        assertEquals("tv", bb.mediaType)
        assertEquals(10.0, bb.personalRating!!, 0.01)
        assertEquals("All Time Top 10", importedSeenr.first().second)

        // Test Sofa Flat CSV with api_source checking
        val sofaFlatCsv = """
            api_id,api_source,category,name,rating,list_title
            429,tmdb,Movies,"The Good, the Bad and the Ugly",9.5,"Western Classics"
            999,igdb,Games,"Random Game",7.0,"Ignore Me"
        """.trimIndent()

        val importedSofa = mutableListOf<Pair<Movie, String?>>()
        val sofaCount = universalCsvImporter.migrateCsvStream(
            inputStream = ByteArrayInputStream(sofaFlatCsv.toByteArray())
        ) { batch, _ ->
            importedSofa.addAll(batch)
        }

        // Only TMDB item should be imported, IGDB must be discarded!
        assertEquals(1, sofaCount)
        val western = importedSofa.first().first
        assertEquals(429L, western.id)
        assertEquals("Western Classics", importedSofa.first().second)
    }
}
