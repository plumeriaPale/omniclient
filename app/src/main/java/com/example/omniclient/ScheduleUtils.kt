package com.example.omniclient

import com.example.omniclient.api.Lesson
import com.example.omniclient.api.ScheduleResponse
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun getCurrentDayOfWeek(curdate: String): String {
    return curdate.split(",")[0].trim()
}

fun getLessonsForDay(schedule: ScheduleResponse, dayOfWeek: String): List<Lesson> {
    val dayNumber = schedule.days.entries.find { it.value == dayOfWeek }?.key
    return schedule.body.values.flatMap { lessons ->
        lessons.filter { it.key == dayNumber }.values
    }
}

/*
fun getCurrentDayOfWeek(curdate: String): String {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val date = format.parse(curdate)
    val calendar = Calendar.getInstance()
    date?.let { calendar.time = it }
    return when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "Понедельник"
        Calendar.TUESDAY -> "Вторник"
        Calendar.WEDNESDAY -> "Среда"
        Calendar.THURSDAY -> "Четверг"
        Calendar.FRIDAY -> "Пятница"
        Calendar.SATURDAY -> "Суббота"
        Calendar.SUNDAY -> "Воскресенье"
        else -> ""
    }
}



fun getLessonsForDay(schedule: ScheduleResponse, dayOfWeek: String): List<Lesson> {
    val dayKey = schedule.days.entries.firstOrNull { it.value == dayOfWeek }?.key

    return if (dayKey != null) {
        val lessonsForDayMap = schedule.body[dayKey]

        lessonsForDayMap?.values?.toList() ?: emptyList()
    } else {
        emptyList()
    }
}
*/