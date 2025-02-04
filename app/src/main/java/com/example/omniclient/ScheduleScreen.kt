package com.example.omniclient

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.navigation.NavController

import com.example.omniclient.api.*
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState


@OptIn(ExperimentalPagerApi::class)
@Composable
fun ScheduleScreen(schedule: ScheduleResponse, navController: NavController) {
    val daysOfWeek = schedule.days.values.toList()
    val currentDayOfWeek = getCurrentDayOfWeek(schedule.curdate)
    val initialPage = daysOfWeek.indexOf(currentDayOfWeek)

    val pagerState = rememberPagerState(initialPage = initialPage)

    Scaffold(
        topBar = {
            TopAppBarComponent(
                title = "Расписание",
                onLogoutClick = {navController.navigate("login")},
                navController = navController,
            )

        },
    ){
        innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ){
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(daysOfWeek.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) Color(0xFFDB173F) else Color.LightGray
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .background(color, RectangleShape)
                            .weight(1f)
                            .height(2.dp)
                    )
                }
            }

            HorizontalPager(
                count = daysOfWeek.size,
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val dayOfWeek = daysOfWeek[page]
                val lessons = getLessonsForDay(schedule, dayOfWeek)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item { Text(text = dayOfWeek) }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                    item { if (lessons.isEmpty()) {
                        Text(text = "Пар нет")
                    } else {
                        lessons.sortedBy { lessons -> lessons.lenta }.forEach { lesson ->
                            LessonCard(lesson)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    } }
                }
            }
        }
    }



}

@Composable
fun LessonCard(lesson: Lesson) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = lesson.name_spec)
            Text(text = "Группа: ${lesson.groups}")
            Text(text = "Аудитория: ${lesson.num_rooms}")
            Text(text = "Время: ${lesson.l_start} - ${lesson.l_end}")
        }
    }
}

suspend fun changeCity(cityId: Int): Boolean {
    return try {
        val response = apiService.changeCity(cityId)
        response.isSuccessful
    } catch (e: Exception) {
        false
    }
}



fun mergeSchedules(schedule1: ScheduleResponse, schedule2: ScheduleResponse): ScheduleResponse {
    val mergedBody = (schedule1.body.toList() + schedule2.body.toList())
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, values) -> values.flatMap { it.entries }.associate { it.key to it.value } }

    return ScheduleResponse(
        body = mergedBody,
        lents = schedule1.lents + schedule2.lents,
        days = schedule1.days,
        daysShort = schedule1.daysShort,
        dates = schedule1.dates,
        curdate = schedule1.curdate,
        start_end = schedule1.start_end
    )
}

fun getCurrentDayOfWeek(curdate: String): String {
    return curdate.split(",")[0].trim()
}

fun getLessonsForDay(schedule: ScheduleResponse, dayOfWeek: String): List<Lesson> {
    val dayNumber = schedule.days.entries.find { it.value == dayOfWeek }?.key
    return schedule.body.values.flatMap { lessons ->
        lessons.filter { it.key == dayNumber }.values
    }
}