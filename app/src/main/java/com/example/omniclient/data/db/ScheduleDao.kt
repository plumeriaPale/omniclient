package com.example.omniclient.data.db

import androidx.room.*

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule WHERE username = :username AND week = :week LIMIT 1")
    suspend fun getSchedule(username: String, week: Int): ScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(schedule: ScheduleEntity)

    @Query("DELETE FROM schedule WHERE username = :username AND week NOT IN (:weeks)")
    suspend fun deleteOldWeeks(username: String, weeks: List<Int>)
} 