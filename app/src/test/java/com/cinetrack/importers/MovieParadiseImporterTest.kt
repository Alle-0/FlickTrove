package com.cinetrack.importers

import com.cinetrack.data.model.Movie
import com.cinetrack.data.repository.importers.MovieParadiseImporter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipInputStream

class MovieParadiseImporterTest {

    private val importer = MovieParadiseImporter()

    @Test
    fun testIsMovieParadiseJson() {
        val sampleJson = """
            {"exportVersion":1,"generatedAt":"2026-09-18T15:46:07Z","profile":{"id":11640},"library":[]}
        """.trimIndent()
        assertTrue(importer.isMovieParadiseJson(sampleJson))
    }

    @Test
    fun testRealMovieParadiseZipImport() = runTest {
        val zipFile = File("../movie-paradise-export-2026-09-18.zip").takeIf { it.exists() }
            ?: File("movie-paradise-export-2026-09-18.zip")

        if (!zipFile.exists()) {
            println("Skipping real zip test because personal export file is not present")
            return@runTest
        }

        val zipEntries = mutableMapOf<String, ByteArray>()
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val name = entry.name.lowercase().substringAfterLast("/")
                    zipEntries[name] = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        assertTrue("Should be recognized as Movie Paradise zip", importer.isMovieParadiseZip(zipEntries))

        val importedBatches = mutableListOf<Pair<Movie, String?>>()
        val totalCount = importer.migrateMovieParadiseZip(
            zipEntries = zipEntries,
            onProgress = { _, _ -> }
        ) { batch, _ ->
            importedBatches.addAll(batch)
        }

        println("Total distinct items mapped: $totalCount")
        println("Total items including folder associations: ${importedBatches.size}")

        // 1. Check total count (should be ~637)
        assertTrue("Should import over 600 items", totalCount >= 600)

        // 2. Check Dark Matter TV show (196322)
        val darkMatterEntries = importedBatches.filter { it.first.id == 196322L && it.first.mediaType == "tv" }
        assertTrue("Dark Matter should be imported", darkMatterEntries.isNotEmpty())
        val darkMatter = darkMatterEntries.first().first
        assertNotNull("Dark Matter should have watched episodes", darkMatter.watchedEpisodes)
        val s1Eps = darkMatter.watchedEpisodes?.get("1")
        assertNotNull("Dark Matter Season 1 episodes should exist", s1Eps)
        assertEquals("Dark Matter should have 9 episodes in season 1", 9, s1Eps?.size)

        // Check rewatch dates in extractedWatchDates (2024 original + 2026 rewatch from watchEvents)
        assertTrue("Dark Matter should contain rewatch dates in extractedWatchDates", darkMatter.extractedWatchDates.size >= 2)
        println("Dark Matter watch dates: ${darkMatter.extractedWatchDates}")

        // 3. Check Wednesday (119051)
        val wednesdayEntries = importedBatches.filter { it.first.id == 119051L }
        assertTrue("Wednesday should be imported", wednesdayEntries.isNotEmpty())
        val wednesday = wednesdayEntries.first().first
        assertNotNull(wednesday.watchedEpisodes)
        println("Wednesday MVP character: ${wednesday.favoriteActorCharacter}, actor: ${wednesday.favoriteActorName}")
        assertEquals("Wednesday Addams", wednesday.favoriteActorCharacter)
        assertEquals("Jenna Ortega", wednesday.favoriteActorName)

        // 4. Check Forgotten (488623) movie reactions & vibes
        val forgotten = importedBatches.firstOrNull { it.first.id == 488623L }?.first
        assertNotNull("Forgotten should be imported", forgotten)
        println("Forgotten vibes: ${forgotten?.emotionalVibes}")
        assertTrue(forgotten?.emotionalVibes?.contains("SHOCKED") == true)
        assertTrue(forgotten?.emotionalVibes?.contains("SCARED") == true)
        assertTrue(forgotten?.emotionalVibes?.contains("THRILLED") == true)
        assertEquals("Yoo-seok / Choi Seong-uk", forgotten?.favoriteActorCharacter)
        assertEquals("Kim Moo-yul", forgotten?.favoriteActorName)

        // 5. Check Folders / Lists
        val folders = importedBatches.mapNotNull { it.second }.toSet()
        println("Imported folders: $folders")
        assertTrue("Thriller folder should exist", folders.contains("Thriller"))
        assertTrue("Sci-fi folder should exist", folders.contains("Sci-fi"))
        assertTrue("comedy folder should exist", folders.contains("comedy"))
        assertTrue("TV Time Favorites folder should exist", folders.contains("TV Time Favorites"))
    }
}
