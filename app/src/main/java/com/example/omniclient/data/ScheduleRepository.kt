package com.example.omniclient.data

import com.example.omniclient.api.ApiService
import com.example.omniclient.api.ScheduleResponse
import com.example.omniclient.data.db.ScheduleDao
import com.example.omniclient.data.db.ScheduleEntity
import com.google.gson.Gson
import com.example.omniclient.api.AcademyClient
import com.example.omniclient.api.CollegeClient
import com.example.omniclient.ui.schedule.mergeSchedules
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.net.UnknownHostException

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

    private fun withDivisionId(schedule: ScheduleResponse, divisionId: Int): ScheduleResponse {
        return schedule.copy(
            body = schedule.body.mapValues { (_, dayLessons) ->
                dayLessons.mapValues { (_, lesson) ->
                    lesson.copy(divisionId = divisionId)
                }
            }
        )
    }

    suspend fun fetchAndUpdateSchedule(week: Int): ScheduleResponse? = coroutineScope {
        try {
            val academyDeferred = async { AcademyClient.getSchedule(csrfToken, com.example.omniclient.api.ScheduleRequest(week)).body() }
            val collegeDeferred = async { CollegeClient.getSchedule(csrfToken, com.example.omniclient.api.ScheduleRequest(week)).body() }
            val academy = academyDeferred.await()?.let { withDivisionId(it, 74) }
            val college = collegeDeferred.await()?.let { withDivisionId(it, 458) }
            val remote = when {
                academy != null && college != null -> mergeSchedules(academy, college)
                academy != null -> academy
                college != null -> college
                else -> null
            }
            val local = getScheduleFromDb(week)
            if (remote != null && remote != local) {
                val allLessons = remote.body.values.flatMap { it.values }
                val allDivisions = allLessons.map { it.divisionId }.toSet()
                val divisionId = when {
                    allDivisions.size == 1 -> allDivisions.first()
                    else -> 0
                }
                saveScheduleToDb(week, remote, divisionId)
            }
            remote
        } catch (e: Exception) {
            if (e is UnknownHostException) {
                null
            } else {
                throw e
            }
        }
    }

    suspend fun cleanupOldWeeks(weeks: List<Int>) {
        scheduleDao.deleteOldWeeks(username, weeks)
    }
} 