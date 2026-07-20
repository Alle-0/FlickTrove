package com.cinetrack.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cinetrack.data.model.Movie
import com.cinetrack.data.local.converters.FlickTroveConverters
import com.cinetrack.data.local.dao.CacheDao
import com.cinetrack.data.local.dao.FavoriteDao
import com.cinetrack.data.local.dao.FolderDao
import com.cinetrack.data.local.dao.SearchHistoryDao
import com.cinetrack.data.local.entities.ColorCacheEntity
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.data.local.entities.MovieDetailCacheEntity
import com.cinetrack.data.local.entities.SearchHistoryEntity

@Database(
    entities = [
        Movie::class,
        FolderEntity::class,
        ColorCacheEntity::class,
        MovieDetailCacheEntity::class,
        SearchHistoryEntity::class
    ],
    version = 11,
    exportSchema = true
)
@TypeConverters(FlickTroveConverters::class)
abstract class FlickTroveDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun folderDao(): FolderDao
    abstract fun cacheDao(): CacheDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        private const val DATABASE_NAME = "flicktrove.db"

        @Volatile
        private var instance: FlickTroveDatabase? = null

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE favorites ADD COLUMN next_episode_air_date TEXT")
                db.execSQL("ALTER TABLE favorites ADD COLUMN next_episode_string TEXT")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE favorites ADD COLUMN custom_backdrop_path TEXT")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE favorites ADD COLUMN department TEXT")
                } catch (e: Exception) {
                    // Ignore if column already exists
                }
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE favorites ADD COLUMN department TEXT")
                } catch (e: Exception) {
                    // Ignore if column already exists
                }
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS movie_details_cache")
                db.execSQL("CREATE TABLE IF NOT EXISTS `movie_details_cache` (`id` INTEGER NOT NULL, `media_type` TEXT NOT NULL, `data` TEXT NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`, `media_type`))")
            }
        }

        fun getInstance(context: Context): FlickTroveDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FlickTroveDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
            }
        }
    }
}
