package com.example.omniclient.data

import com.example.omniclient.api.ApiService
import com.example.omniclient.api.ScheduleResponse
import com.example.omniclient.data.db.ScheduleDao
import com.example.omniclient.data.db.ScheduleEntity
import com.google.gson.Gson
import com.example.omniclient.fetchCombinedSchedule

class ScheduleRepository(
    private val apiService: ApiService,
    private val scheduleDao: ScheduleDao,
    private val username: String,
    private val csrfToken: String
) {
    suspend fun getScheduleFromDb(week: Int): ScheduleResponse? {
        val entity = scheduleDao.getSchedule(username, week)
        return entity?.scheduleJson?.let { Gson().fromJson(it, ScheduleResponse::class.java) }
    }

    suspend fun saveScheduleToDb(week: Int, schedule: ScheduleResponse) {
        val json = Gson().toJson(schedule)
        scheduleDao.insertOrUpdate(
            ScheduleEntity(
                username = username,
                week = week,
                scheduleJson = json,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun fetchAndUpdateSchedule(week: Int): ScheduleResponse? {
        val remote = fetchCombinedSchedule(csrfToken, week)
        val local = getScheduleFromDb(week)
        if (remote != null && remote != local) {
            saveScheduleToDb(week, remote)
        }
        return remote
    }

    suspend fun cleanupOldWeeks(weeks: List<Int>) {
        scheduleDao.deleteOldWeeks(username, weeks)
    }
} 