package com.example.omniclient.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [UserEntity::class, ScheduleEntity::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun scheduleDao(): ScheduleDao
} 