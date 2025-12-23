package com.infosetgroup.delivery.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Bump version and filename to avoid on-disk schema mismatches during active development.
@Database(entities = [DeliveryEntity::class, HistoryEntity::class, RemoteKeys::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deliveryDao(): DeliveryDao
    abstract fun remoteKeysDao(): RemoteKeysDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        @Suppress("DEPRECATION")
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "delivery_app_v6.db" // bumped filename and version to avoid old schema conflicts during development
                )
                    // During active development it's safer to allow destructive fallback so schema mismatches
                    // don't crash the app; use proper migrations for production.
                    .fallbackToDestructiveMigration()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
