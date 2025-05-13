package com.example.omniclient.ui.schedule

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

import com.example.omniclient.api.*
import com.example.omniclient.components.TopAppBarComponent
import com.example.omniclient.viewmodels.ScheduleViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ScheduleViewModelFactory(
    private val apiService: ApiService,
    private val csrfToken: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScheduleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScheduleViewModel(apiService, csrfToken) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun ScheduleScreen(
    navController: NavController,
    apiService: ApiService,
    csrfToken: String
) {
    val viewModel: ScheduleViewModel = viewModel(
        factory = ScheduleViewModelFactory(apiService, csrfToken)
    )

    val schedule by viewModel.schedule.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val currentDayIndex by viewModel.currentDayIndex.collectAsState()

    val daysOfWeek = viewModel.getDaysOfWeek()
    val pagerState = rememberPagerState(initialPage = currentDayIndex)
    LaunchedEffect(pagerState.currentPage) {
        viewModel.onDaySelected(pagerState.currentPage)
    }
    LaunchedEffect(currentDayIndex) {
        if (pagerState.currentPage != currentDayIndex) {
            pagerState.animateScrollToPage(currentDayIndex)
        }
    }

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
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ){
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.Red, strokeWidth = 4.dp)
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = errorMessage ?: "Неизвестная ошибка", color = MaterialTheme.colorScheme.error)
                    }
                }
                schedule != null -> {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 16.dp),
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
                        val dayOfWeek = daysOfWeek.getOrNull(page)
                        if (dayOfWeek != null) {

                            val lessons = viewModel.getLessonsForCurrentDay()

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                item { Text(text = dayOfWeek) }
                                item { Spacer(modifier = Modifier.height(12.dp)) }
                                if (lessons.isEmpty()) {
                                    item { Text(text = "Пар нет") }
                                } else {
                                    items(lessons.sortedBy { it.lenta }) { lesson ->
                                        LessonCard(
                                            lesson,
                                            onPresentClick = {clickedLesson -> Log.d("ScheduleScreen", "${clickedLesson.name_spec}")},
                                            onMaterialsClick = {clickedLesson -> Log.d("ScheduleScreen", "${clickedLesson.name_spec}")}
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("День не найден")
                            }
                        }
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Загрузка расписания...")
                    }
                }
            }
        }
    }
}

@Composable
fun LessonCard(lesson: Lesson,
               onPresentClick: (Lesson) -> Unit,
               onMaterialsClick: (Lesson) -> Unit)
{
    var isMenuExpanded by remember { mutableStateOf(false) }
    val isCurrent = isCurrentTimeWithinLesson(lesson.l_end)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .wrapContentHeight(),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
            contentColor = Color.Black
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.Top
        ) {
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(Color(0xFFDB173F))
                )
            }
            else{
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(Color.Transparent)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = lesson.name_spec,
                    color = Color.Black,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Группа: ${lesson.groups}", color = Color.Black, fontSize = 14.sp)
                Text(text = "Аудитория: ${lesson.num_rooms}", color = Color.Black, fontSize = 14.sp)
                Text(text = "Время: ${lesson.l_start} - ${lesson.l_end}", color = Color.Black, fontSize = 14.sp)
            }

            Box {
                IconButton(
                    onClick = { isMenuExpanded = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Меню",
                        tint = Color.Black
                    )
                }

                DropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false },
                    modifier = Modifier
                        .background(Color.White)
                        .border(
                            width = 1.dp,
                            color = Color.LightGray,
                            shape = RoundedCornerShape(4.dp)
                        )
                ) {
                    DropdownMenuItem(
                        onClick = {
                            isMenuExpanded = false
                            onPresentClick(lesson)
                        },
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Присутствующие",
                            color = Color.Black,
                            fontSize = 14.sp)
                    }
                    DropdownMenuItem(
                        onClick = {
                            isMenuExpanded = false
                            onMaterialsClick(lesson)
                        },
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Выдать материалы",
                            color = Color.Black,
                            fontSize = 14.sp)

                    }
                }
            }
        }
    }
}

fun isCurrentTimeWithinLesson(endTime: String): Boolean {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val currentTime = Calendar.getInstance().time
    val lessonEndTime = timeFormat.parse(endTime)

    return timeFormat.format(currentTime) == lessonEndTime?.let { timeFormat.format(it) }
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