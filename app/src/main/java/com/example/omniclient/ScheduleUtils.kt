package com.example.omniclient

import com.example.omniclient.api.Lesson
import com.example.omniclient.api.ScheduleResponse
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.util.Log

fun getCurrentDayOfWeek(curdate: String): String {
    return curdate.split(",")[0].trim()
}

fun getLessonsForDay(schedule: ScheduleResponse, dayOfWeek: String): List<Lesson> {
    val dayNumber = schedule.days.entries.find { it.value == dayOfWeek }?.key
    Log.d("ScheduleDebug", "dayOfWeek=$dayOfWeek, dayNumber=$dayNumber, days=${schedule.days}, bodyKeys=${schedule.body.keys}")
    return schedule.body.values.flatMap { lessons ->
        lessons.filter { it.key == dayNumber }.values
    }
}