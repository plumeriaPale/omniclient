package com.example.omniclient.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.omniclient.api.ApiService
import com.example.omniclient.api.Lesson
import com.example.omniclient.api.ScheduleResponse
import com.example.omniclient.data.ScheduleRepository
import com.example.omniclient.data.db.DatabaseProvider
import com.example.omniclient.getCurrentDayOfWeek
import com.example.omniclient.getLessonsForDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineExceptionHandler

class ScheduleViewModel(
    private val context: Context,
    private val apiService: ApiService,
    private val csrfToken: String,
    private val username: String
) : ViewModel() {
    private val scheduleRepository: ScheduleRepository by lazy {
        ScheduleRepository(
            apiService = apiService,
            scheduleDao = DatabaseProvider.getDatabase(context).scheduleDao(),
            username = username,
            csrfToken = csrfToken
        )
    }

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("ScheduleViewModel", "Coroutine exception: ", throwable)
    }

    // Кэш всех недель
    private val _weeks = MutableStateFlow<Map<Int, ScheduleResponse?>>(emptyMap())
    val weeks: StateFlow<Map<Int, ScheduleResponse?>> = _weeks.asStateFlow()

    // Для отслеживания загрузки по неделям
    private val _loadingWeeks = MutableStateFlow<Set<Int>>(emptySet())
    val loadingWeeks: StateFlow<Set<Int>> = _loadingWeeks.asStateFlow()

    // Для ошибок по неделям
    private val _errorWeeks = MutableStateFlow<Map<Int, String?>>(emptyMap())
    val errorWeeks: StateFlow<Map<Int, String?>> = _errorWeeks.asStateFlow()

    init {
        preloadWeeksFromDb(0)
    }

    // Получить расписание для недели
    fun getScheduleForWeek(week: Int): ScheduleResponse? = _weeks.value[week]

    // Загрузить расписание для недели (если нет в кэше)
    fun ensureWeekLoaded(week: Int) {
        if (_weeks.value.containsKey(week) || _loadingWeeks.value.contains(week)) return
        _loadingWeeks.value = _loadingWeeks.value + week
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                val local = scheduleRepository.getScheduleFromDb(week)
                if (local != null) {
                    _weeks.value = _weeks.value + (week to local)
                }
                val remote = scheduleRepository.fetchAndUpdateSchedule(week)
                if (remote != null) {
                    _weeks.value = _weeks.value + (week to remote)
                }
            } catch (e: Exception) {
                _errorWeeks.value = _errorWeeks.value + (week to e.message)
            } finally {
                _loadingWeeks.value = _loadingWeeks.value - week
            }
        }
    }

    // Получить дни недели для недели
    fun getDaysOfWeek(week: Int): List<String> {
        return _weeks.value[week]?.days?.values?.toList() ?: emptyList()
    }

    // Получить уроки для дня недели (по индексу) для недели
    fun getLessonsForDayAtIndex(week: Int, index: Int): List<Lesson> {
        val daysOfWeek = getDaysOfWeek(week)
        val dayOfWeek = daysOfWeek.getOrNull(index)
        val schedule = _weeks.value[week]
        return if (dayOfWeek != null && schedule != null) {
            getLessonsForDay(schedule, dayOfWeek)
        } else {
            emptyList()
        }
    }

    fun getDisplayDayWithDate(index: Int): String {
        val schedule = _weeks.value[index] ?: return getDaysOfWeek(index).getOrNull(index) ?: ""
        val daysList = getDaysOfWeek(index)
        val daysMap = schedule.days
        val datesMap = schedule.dates

        val dayKey = daysMap.keys.elementAtOrNull(index) ?: return daysList.getOrNull(index) ?: ""
        val dayName = daysList.getOrNull(index) ?: ""
        val dateStr = datesMap[dayKey]?.let { formatDateForDisplay(it) } ?: ""
        return if (dateStr.isNotEmpty()) "$dayName, $dateStr" else dayName
    }

    fun formatDateForDisplay(date: String): String {
        return try {
            val parts = date.split("-")
            if (parts.size == 3) {
                val day = parts[2]
                val month = when (parts[1]) {
                    "01" -> "Январь"
                    "02" -> "Февраль"
                    "03" -> "Март"
                    "04" -> "Апрель"
                    "05" -> "Май"
                    "06" -> "Июнь"
                    "07" -> "Июль"
                    "08" -> "Август"
                    "09" -> "Сентябрь"
                    "10" -> "Октябрь"
                    "11" -> "Ноябрь"
                    "12" -> "Декабрь"
                    else -> parts[1]
                }
                "$day $month"
            } else date
        } catch (e: Exception) {
            date
        }
    }

    // Подгрузить соседние недели (слева и справа), если их нет в кэше
    fun ensureAdjacentWeeksLoaded(centerWeek: Int) {
        val left = centerWeek - 1
        val right = centerWeek + 1
        if (!weeks.value.containsKey(left)) ensureWeekLoaded(left)
        if (!weeks.value.containsKey(right)) ensureWeekLoaded(right)
        // Очистка старых недель, оставляем диапазон [centerWeek-2, ..., centerWeek+2]
        viewModelScope.launch(coroutineExceptionHandler) {
            scheduleRepository.cleanupOldWeeks(listOf(centerWeek - 2, left, centerWeek, right, centerWeek + 2))
        }
    }

    // Предзагрузка недель из БД для мгновенного отображения
    fun preloadWeeksFromDb(centerWeek: Int) {
        viewModelScope.launch(coroutineExceptionHandler) {
            val weeksToLoad = (centerWeek - 2)..(centerWeek + 2)
            val loaded = weeksToLoad.associateWith { scheduleRepository.getScheduleFromDb(it) }
            _weeks.value = _weeks.value + loaded.filterValues { it != null }
        }
    }
}