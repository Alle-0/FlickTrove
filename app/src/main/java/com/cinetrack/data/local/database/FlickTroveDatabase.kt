package com.cinetrack.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cinetrack.data.Movie
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
    version = 8,
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

        fun getInstance(context: Context): FlickTroveDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FlickTroveDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(MIGRATION_6_7, MIGRATION_7_8)
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
            }
        }
    }
}
