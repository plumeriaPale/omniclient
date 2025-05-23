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
        val schedule = entity?.scheduleJson?.let { Gson().fromJson(it, ScheduleResponse::class.java) }
        // Автозаполнение divisionId для старых данных
        if (schedule != null) {
            val allLessons = schedule.body.values.flatMap { it.values }
            val allDivisions = allLessons.map { it.divisionId }.toSet()
            if (allDivisions.all { it == 0 }) {
                // Если все divisionId == 0, пробуем определить по divisionId из entity
                val fixed = schedule.copy(
                    body = schedule.body.mapValues { (_, dayLessons) ->
                        dayLessons.mapValues { (_, lesson) ->
                            lesson.copy(divisionId = entity.divisionId)
                        }
                    }
                )
                return fixed
            }
        }
        return schedule
    }

    suspend fun saveScheduleToDb(week: Int, schedule: ScheduleResponse, divisionId: Int = 0) {
        val json = Gson().toJson(schedule)
        scheduleDao.insertOrUpdate(
            ScheduleEntity(
                username = username,
                week = week,
                scheduleJson = json,
                updatedAt = System.currentTimeMillis(),
                divisionId = divisionId
            )
        )
    }

    suspend fun fetchAndUpdateSchedule(week: Int): ScheduleResponse? {
        val remote = fetchCombinedSchedule(csrfToken, week)
        val local = getScheduleFromDb(week)
        if (remote != null && remote != local) {
            // Определяем divisionId: если все пары колледж — 458, если все академия — 74, иначе 0
            val allLessons = remote.body.values.flatMap { it.values }
            val allDivisions = allLessons.map { it.divisionId }.toSet()
            val divisionId = when {
                allDivisions.size == 1 -> allDivisions.first()
                else -> 0
            }
            saveScheduleToDb(week, remote, divisionId)
        }
        return remote
    }

    suspend fun cleanupOldWeeks(weeks: List<Int>) {
        scheduleDao.deleteOldWeeks(username, weeks)
    }
} 