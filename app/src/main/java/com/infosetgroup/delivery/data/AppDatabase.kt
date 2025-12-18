package com.infosetgroup.delivery.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.infosetgroup.delivery.data.DatabaseMigrations

@Database(entities = [DeliveryEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deliveryDao(): DeliveryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "delivery_app.db"
                ).addMigrations(DatabaseMigrations.MIGRATION_1_2)
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
