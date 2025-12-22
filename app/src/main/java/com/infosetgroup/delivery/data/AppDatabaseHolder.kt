package com.infosetgroup.delivery.data

import android.content.Context
import java.io.File
import androidx.room.Room

/**
 * Simple holder to make AppDatabase available where DI isn't set up yet.
 * Initialize early from Application or MainActivity: AppDatabaseHolder.init(context)
 */
object AppDatabaseHolder {
    @Volatile
    var instance: AppDatabase? = null
        private set

    fun init(context: Context) {
        // Try a safe initialization: Room may throw if the on-disk schema differs.
        try {
            instance = AppDatabase.getInstance(context.applicationContext)
        } catch (t: Throwable) {
            // If Room failed due to schema mismatch, delete the DB file and retry creation.
            try {
                val dbFile = File(context.applicationContext.getDatabasePath("delivery_app_v5.db").absolutePath)
                if (dbFile.exists()) dbFile.delete()
            } catch (_: Throwable) { /* best effort */ }

            // Retry creation (fallbackToDestructiveMigration in AppDatabase will recreate a clean DB)
            instance = AppDatabase.getInstance(context.applicationContext)
        }
    }
}
