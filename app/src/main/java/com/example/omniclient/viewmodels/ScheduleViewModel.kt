package com.example.omniclient.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.omniclient.api.ApiService
import com.example.omniclient.api.Lesson
import com.example.omniclient.api.ScheduleResponse
import com.example.omniclient.fetchCombinedSchedule
import com.example.omniclient.getCurrentDayOfWeek
import com.example.omniclient.getLessonsForDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScheduleViewModel(
    private val apiService: ApiService,
    private val csrfToken: String
) : ViewModel() {


    private val _schedule = MutableStateFlow<ScheduleResponse?>(null)
    val schedule: StateFlow<ScheduleResponse?> = _schedule.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentDayIndex = MutableStateFlow(0)
    val currentDayIndex: StateFlow<Int> = _currentDayIndex.asStateFlow()

    var currentWeek = 0

    private val weekCache = mutableMapOf<Int, ScheduleResponse?>()

    init {
        loadSchedule()
        Log.d("Schedule", "Navigate")
    }

    private fun loadSchedule(week: Int = currentWeek, setDayIndex: Int? = null) {
        Log.d("Schedule", "Navigate")
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val loadedSchedule = weekCache[week] ?: fetchCombinedSchedule(csrfToken, week).also { weekCache[week] = it }
                _schedule.value = loadedSchedule

                loadedSchedule?.let {
                    val daysOfWeek = it.days.values.toList()
                    val dayIndex = when {
                        setDayIndex != null -> setDayIndex
                        week == 0 -> {
                            val currentDayOfWeek = getCurrentDayOfWeek(it.curdate)
                            val initialPage = daysOfWeek.indexOf(currentDayOfWeek)
                            if (initialPage != -1) initialPage else 0
                        }
                        else -> 0
                    }
                    _currentDayIndex.value = dayIndex
                }
                currentWeek = week
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки расписания: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadNextWeek(setDayIndex: Int = 0) {
        Log.d("loadNextWeek",currentWeek.toString())
        loadSchedule(currentWeek + 1, setDayIndex)
    }

    fun loadPreviousWeek(setDayIndex: Int) {
        Log.d("loadPreviousWeek",currentWeek.toString())
        loadSchedule(currentWeek - 1, setDayIndex)
    }

    fun preloadNextWeek() {
        Log.d("preloadNextWeek",currentWeek.toString())
        val nextWeek = currentWeek + 1
        if (!weekCache.containsKey(nextWeek)) {
            viewModelScope.launch {
                val schedule = fetchCombinedSchedule(csrfToken, nextWeek)
                weekCache[nextWeek] = schedule
            }
        }
    }

    fun preloadPreviousWeek() {
        Log.d("preloadNextWeek",currentWeek.toString())
        val prevWeek = currentWeek - 1
        if (!weekCache.containsKey(prevWeek)) {
            viewModelScope.launch {
                val schedule = fetchCombinedSchedule(csrfToken, prevWeek)
                weekCache[prevWeek] = schedule
            }
        }
    }

    fun getLessonsForDayAtIndex(index: Int): List<Lesson> {
        val daysOfWeek = getDaysOfWeek()
        val dayOfWeek = daysOfWeek.getOrNull(index)

        return if (dayOfWeek != null && _schedule.value != null) {
            getLessonsForDay(_schedule.value!!, dayOfWeek)
        } else {
            emptyList()
        }
    }

    fun onDaySelected(index: Int) {
        _currentDayIndex.value = index
    }

    fun getDaysOfWeek(): List<String> {
        return _schedule.value?.days?.values?.toList() ?: emptyList()
    }

    fun getLessonsForCurrentDay(): List<Lesson> {
        Log.d("getLessonsForCurrentDay", "Start")
        val currentDayOfWeek = getDaysOfWeek().getOrNull(_currentDayIndex.value)
        return if (currentDayOfWeek != null && _schedule.value != null) {
            getLessonsForDay(_schedule.value!!, currentDayOfWeek)
        } else {
            emptyList()
        }
    }

    fun getDisplayDayWithDate(index: Int): String {
        val schedule = _schedule.value ?: return getDaysOfWeek().getOrNull(index) ?: ""
        val daysList = getDaysOfWeek()
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
}