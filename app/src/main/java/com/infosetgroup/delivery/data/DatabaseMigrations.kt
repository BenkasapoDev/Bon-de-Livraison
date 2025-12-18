package com.infosetgroup.delivery.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // add a new column 'status' with default value 'PENDING'
            db.execSQL("ALTER TABLE pending_deliveries ADD COLUMN status TEXT DEFAULT 'PENDING'")
        }
    }
}
