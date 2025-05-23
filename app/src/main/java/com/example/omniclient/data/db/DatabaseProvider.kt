package com.example.omniclient.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS schedule (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    username TEXT NOT NULL,
                    week INTEGER NOT NULL,
                    scheduleJson TEXT NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """)
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE schedule ADD COLUMN divisionId INTEGER NOT NULL DEFAULT 0")
        }
    }

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "omniclient_db"
            )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
            INSTANCE = instance
            instance
        }
    }
} 