package com.trasparenza.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database for Trasparenza app
 */
@Database(
    entities = [SavedProductEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    
    companion object {
        const val DATABASE_NAME = "trasparenza_db"
    }
}

