package com.example.omniclient.ui.schedule

import LessonCard
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
    csrfToken: String,
    openDrawer: (() -> Unit)? = null,
    loginViewModel: LoginViewModel
) {
    val viewModel: ScheduleViewModel = viewModel(
        factory = ScheduleViewModelFactory(apiService, csrfToken)
    )

    val schedule by viewModel.schedule.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val currentDayIndex by viewModel.currentDayIndex.collectAsState()

    val daysOfWeek = viewModel.getDaysOfWeek()
    val daysWithFakes = listOf("fakePrev") + daysOfWeek + listOf("fakeNext")
    val isDataReady = currentDayIndex >= 0 && schedule != null

    if (isDataReady) {
        val pagerState = rememberPagerState(
            initialPage = currentDayIndex + 1,
            initialPageOffsetFraction = 0f,
            pageCount = { daysWithFakes.size }
        )

        val todayIndex = viewModel.getTodayDayIndex()

        LaunchedEffect(viewModel.currentWeek) {
            viewModel.preloadPreviousWeek()
        }
        LaunchedEffect(viewModel.currentWeek) {
            viewModel.preloadNextWeek()
        }

        LaunchedEffect(pagerState.currentPage) {
            when (pagerState.currentPage) {
                0 -> {
                    viewModel.loadPreviousWeek(setDayIndex = viewModel.getDaysOfWeek().lastIndex)
                    viewModel.onDaySelected(viewModel.getDaysOfWeek().lastIndex)
                    pagerState.scrollToPage(daysWithFakes.lastIndex - 1)
                }
                daysWithFakes.lastIndex -> {
                    viewModel.loadNextWeek(setDayIndex = 0)
                    viewModel.onDaySelected(0)
                    pagerState.scrollToPage(1)
                }
                else -> {
                    viewModel.onDaySelected(pagerState.currentPage - 1)
                }
            }
        }

        LaunchedEffect(currentDayIndex) {
            val targetPage = currentDayIndex + 1
            if (pagerState.currentPage != targetPage) {
                pagerState.scrollToPage(targetPage)
            }
        }

        Scaffold(
            topBar = {
                TopAppBarComponent(
                    title = "Расписание",
                    onLogoutClick = {
                        loginViewModel.logout()
                        navController.navigate("login") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    navController = navController,
                    onMenuClick = openDrawer
                )
            }
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
                                val color = if (pagerState.currentPage == iteration + 1) Color(0xFFDB173F) else Color.LightGray
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
                            beyondViewportPageCount = 1
                        ) { page ->
                            when (page) {
                                0, daysWithFakes.lastIndex -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize()
                                    ) {}
                                }
                                else -> {
                                    val dayOfWeek = daysOfWeek.getOrNull(page - 1)
                                    if (dayOfWeek != null) {
                                        val lessons = viewModel.getLessonsForDayAtIndex(page - 1)
                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            item {
                                                Text(text = viewModel.getDisplayDayWithDate(page - 1), fontWeight = FontWeight.SemiBold)
                                            }
                                            item { Spacer(modifier = Modifier.height(12.dp)) }
                                            if (lessons.isEmpty()) {
                                                item { Text(text = "Пар нет") }
                                            } else {
                                                items(lessons.sortedBy { it.lenta }) { lesson ->
                                                    LessonCard(
                                                        lesson,
                                                        isCurrentDay = (page - 1) == todayIndex,
                                                        onPresentClick = { clickedLesson -> 
                                                            navController.navigate("attendance/${clickedLesson.lenta}")
                                                        },
                                                        onMaterialsClick = {clickedLesson -> Log.d("Dev:ScheduleScreen", "${clickedLesson.name_spec}")}
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
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.Red, strokeWidth = 4.dp)
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