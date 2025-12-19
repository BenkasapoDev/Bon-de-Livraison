package com.infosetgroup.delivery.data

import android.content.Context

/**
 * Simple holder to make AppDatabase available where DI isn't set up yet.
 * Initialize early from Application or MainActivity: AppDatabaseHolder.init(context)
 */
object AppDatabaseHolder {
    @Volatile
    var instance: AppDatabase? = null
        private set

    fun init(context: Context) {
        instance = AppDatabase.getInstance(context.applicationContext)
    }
}

