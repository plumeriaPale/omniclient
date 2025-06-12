package com.example.omniclient.ui.schedule

import LessonCard
import android.content.Context
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
import androidx.compose.ui.platform.LocalContext

import com.example.omniclient.api.*
import com.example.omniclient.components.TopAppBarComponent
import com.example.omniclient.viewmodels.ScheduleViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.omniclient.viewmodels.LoginViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.flow.first
import com.google.gson.Gson
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.WeekFields

class ScheduleViewModelFactory(
    private val context: Context,
    private val apiService: ApiService,
    private val csrfToken: String,
    private val username: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScheduleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScheduleViewModel(context, apiService, csrfToken, username) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun ScheduleScreen(
    navController: NavController,
    apiService: ApiService,
    csrfToken: String,
    openDrawer: (() -> Unit)? = null,
    loginViewModel: LoginViewModel
) {
    val context = LocalContext.current
    val username by loginViewModel.username.collectAsState()
    val viewModel: ScheduleViewModel = viewModel(
        factory = ScheduleViewModelFactory(context, apiService, csrfToken, username)
    )

    // --- Бесконечный дневной пейджер ---
    val initialPage = 5000
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        initialPageOffsetFraction = 0F,
        pageCount = { 10000 }
    )
    val weeks by viewModel.weeks.collectAsState()
    val loadingWeeks by viewModel.loadingWeeks.collectAsState()
    val errorWeeks by viewModel.errorWeeks.collectAsState()

    val today = remember { LocalDate.now() }
    val currentDate = today.plusDays((pagerState.currentPage - initialPage).toLong())
    val weekFields = WeekFields.of(Locale.getDefault())
    val currentWeek = currentDate.get(weekFields.weekOfWeekBasedYear()) - today.get(weekFields.weekOfWeekBasedYear())
    val weekYear = currentDate.get(weekFields.weekBasedYear())
    val weekKey = (weekYear - today.get(weekFields.weekBasedYear())) * 52 + currentWeek // уникальный ключ недели
    val dayOfWeek = currentDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val dayOfWeekIndex = currentDate.dayOfWeek.value - 1 // 0 - Пн, 6 - Вс

    // Подгружаем нужную неделю
    LaunchedEffect(weekKey) {
        viewModel.ensureWeekLoaded(weekKey)
    }

    // Подгружаем соседние недели при смене недели
    LaunchedEffect(weekKey) {
        viewModel.ensureAdjacentWeeksLoaded(weekKey)
    }

    // При первом показе экрана — мгновенно подгружаем недели из БД
    LaunchedEffect(Unit) {
        viewModel.preloadWeeksFromDb(weekKey)
    }

    val schedule = weeks[weekKey]
    val isLoading = loadingWeeks.contains(weekKey)
    val errorMessage = errorWeeks[weekKey]
    val daysOfWeek = viewModel.getDaysOfWeek(weekKey)
    val lessons = viewModel.getLessonsForDayAtIndex(weekKey, dayOfWeekIndex)

    Scaffold(
        topBar = {
            TopAppBarComponent(
                title = "Расписание",
                navController = navController,
                onMenuClick = openDrawer
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Индикация дней недели
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                daysOfWeek.forEachIndexed { idx, dayName ->
                    val color = if (idx == dayOfWeekIndex) Color(0xFFDB173F) else Color.LightGray
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
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val date = today.plusDays((page - initialPage).toLong())
                val weekFields = WeekFields.of(Locale.getDefault())
                val week = date.get(weekFields.weekOfWeekBasedYear()) - today.get(weekFields.weekOfWeekBasedYear())
                val weekYear = date.get(weekFields.weekBasedYear())
                val weekKey = (weekYear - today.get(weekFields.weekBasedYear())) * 52 + week
                val dayOfWeekIdx = date.dayOfWeek.value - 1
                val daysOfWeekPage = viewModel.getDaysOfWeek(weekKey)
                val lessons = viewModel.getLessonsForDayAtIndex(weekKey, dayOfWeekIdx)
                val isLoadingPage = loadingWeeks.contains(weekKey)
                val errorPage = errorWeeks[weekKey]

                if (daysOfWeekPage.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            Text(text = formatDayWithDate(date), fontWeight = FontWeight.SemiBold)
                        }
                        item { Spacer(modifier = Modifier.height(12.dp)) }
                        if (lessons.isEmpty()) {
                            item { Text(text = "Пар нет") }
                        } else {

                            items(lessons.sortedBy { it.lenta }) { lesson ->
                                LessonCard(
                                    lesson,
                                    isCurrentDay = (date == today),
                                    onPresentClick = { clickedLesson ->
                                        navController.navigate("attendance/${clickedLesson.lenta}?divisionId=${clickedLesson.divisionId}")
                                    },
                                    onMaterialsClick = { clickedLesson -> Log.d("Dev:ScheduleScreen", "${clickedLesson.name_spec}") }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                } else if (isLoadingPage) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.Red, strokeWidth = 4.dp)
                    }
                } else if (errorPage != null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = errorPage, color = MaterialTheme.colorScheme.error)
                    }
                } else {
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


fun isCurrentTimeWithinLesson(lStart: String, lEnd: String): Boolean {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val now = Calendar.getInstance()

    val start = Calendar.getInstance().apply {
        time = timeFormat.parse(lStart) ?: return false
        set(Calendar.YEAR, now.get(Calendar.YEAR))
        set(Calendar.MONTH, now.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))
    }
    val end = Calendar.getInstance().apply {
        time = timeFormat.parse(lEnd) ?: return false
        set(Calendar.YEAR, now.get(Calendar.YEAR))
        set(Calendar.MONTH, now.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))
    }

    return now.timeInMillis in start.timeInMillis until end.timeInMillis
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

// Функция для форматирования даты в стиле "Понедельник, 19 Май"
fun formatDayWithDate(date: LocalDate): String {
    val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("ru"))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("ru")) else it.toString() }
    val day = date.dayOfMonth.toString()
    val month = when (date.month.value) {
        1 -> "Января"
        2 -> "Февраля"
        3 -> "Марта"
        4 -> "Апреля"
        5 -> "Мая"
        6 -> "Июня"
        7 -> "Июля"
        8 -> "Августа"
        9 -> "Сентября"
        10 -> "Октября"
        11 -> "Ноября"
        12 -> "Декабря"
        else -> date.month.getDisplayName(TextStyle.FULL, Locale("ru"))
    }
    return "$dayOfWeek, $day $month"
}