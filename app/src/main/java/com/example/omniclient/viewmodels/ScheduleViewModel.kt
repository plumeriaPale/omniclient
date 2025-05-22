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
    private val selectedDayIndexPerWeek = mutableMapOf<Int, Int>()
    private val firstLoadPerWeek = mutableSetOf<Int>()

    private var isFirstLoad = true
    private var userSelectedDayIndex: Int? = null

    init {
        loadSchedule()
        Log.d("Dev:Schedule", "Navigate")
    }

    private fun loadSchedule(week: Int = currentWeek, setDayIndex: Int? = null) {
        Log.d("Dev:Schedule", "Navigate")
        viewModelScope.launch {
            _errorMessage.value = null

            val cachedSchedule = weekCache[week]
            if (cachedSchedule != null) {
                Log.d("Dev:Schedule", "Cached was not null")
                _schedule.value = cachedSchedule
                _isLoading.value = false
                currentWeek = week
                val savedIndex = selectedDayIndexPerWeek[week]
                val daysOfWeek = cachedSchedule.days.values.toList()
                val dayIndex = when {
                    userSelectedDayIndex != null -> userSelectedDayIndex!!
                    setDayIndex != null -> setDayIndex
                    savedIndex != null -> savedIndex
                    else -> 0
                }
                _currentDayIndex.value = dayIndex
                userSelectedDayIndex = null // сбрасываем после применения
                return@launch
            } else {
                Log.d("Dev:Schedule", "Cached was null")
                _isLoading.value = true
            }

            val localSchedule = scheduleRepository.getScheduleFromDb(week)
            if (localSchedule != null) {
                _schedule.value = localSchedule
                weekCache[week] = localSchedule
                if (!selectedDayIndexPerWeek.containsKey(week)) {
                    // Первый вход на неделю
                    val daysOfWeek = localSchedule.days.values.toList()
                    val currentDayOfWeek = getCurrentDayOfWeek(localSchedule.curdate)
                    val initialPage = daysOfWeek.indexOf(currentDayOfWeek)
                    val idx = if (week == 0 && initialPage != -1) initialPage else 0
                    _currentDayIndex.value = idx
                    selectedDayIndexPerWeek[week] = idx
                }
                _isLoading.value = false
            } else if (cachedSchedule == null) {
                // Только если нет кэша и нет локального расписания — показываем лоадер
                _isLoading.value = true
            }
            val remoteSchedule = scheduleRepository.fetchAndUpdateSchedule(week)
            if (remoteSchedule != null) {
                weekCache[week] = remoteSchedule
                if (_schedule.value != remoteSchedule) {
                    _schedule.value = remoteSchedule
                }
            }
            scheduleRepository.cleanupOldWeeks()

            currentWeek = week
            val savedIndex = selectedDayIndexPerWeek[week]
            val scheduleToUse = remoteSchedule ?: localSchedule
            scheduleToUse?.let {
                val daysOfWeek = it.days.values.toList()
                val dayIndex = when {
                    userSelectedDayIndex != null -> userSelectedDayIndex!!
                    setDayIndex != null -> setDayIndex
                    savedIndex != null -> savedIndex
                    else -> 0
                }
                _currentDayIndex.value = dayIndex
                userSelectedDayIndex = null // сбрасываем после применения
            }
            _isLoading.value = false
        }
    }

    fun loadNextWeek(setDayIndex: Int = 0) {
        Log.d("Dev:loadNextWeek",currentWeek.toString())
        loadSchedule(currentWeek + 1, setDayIndex)
    }

    fun loadPreviousWeek(setDayIndex: Int) {
        Log.d("Dev:loadPreviousWeek",currentWeek.toString())
        loadSchedule(currentWeek - 1, setDayIndex)
    }

    suspend fun preloadNextWeek() {
        val nextWeek = currentWeek + 1
        Log.d("Dev:preloadNextWeek", "preloading week $nextWeek (current: $currentWeek)")
        if (!weekCache.containsKey(nextWeek)) {
            // Сначала из БД, потом из сети
            val local = scheduleRepository.getScheduleFromDb(nextWeek)
            if (local != null) weekCache[nextWeek] = local
            val remote = scheduleRepository.fetchAndUpdateSchedule(nextWeek)
            if (remote != null) weekCache[nextWeek] = remote
            scheduleRepository.cleanupOldWeeks()
        }
    }

    suspend fun preloadPreviousWeek() {
        val prevWeek = currentWeek - 1
        Log.d("Dev:preloadPreviousWeek", "preloading week $prevWeek (current: $currentWeek)")
        if (!weekCache.containsKey(prevWeek)) {
            val local = scheduleRepository.getScheduleFromDb(prevWeek)
            if (local != null) weekCache[prevWeek] = local
            val remote = scheduleRepository.fetchAndUpdateSchedule(prevWeek)
            if (remote != null) weekCache[prevWeek] = remote
            scheduleRepository.cleanupOldWeeks()
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

    fun onDaySelected(index: Int, week: Int = currentWeek) {
        _currentDayIndex.value = index
        selectedDayIndexPerWeek[week] = index
        userSelectedDayIndex = index
    }

    fun getDaysOfWeek(): List<String> {
        return _schedule.value?.days?.values?.toList() ?: emptyList()
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

    fun getTodayDayIndex(): Int? {
        val schedule = _schedule.value ?: return null
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Calendar.getInstance().time)
        val daysList = getDaysOfWeek()
        val daysMap = schedule.days
        val datesMap = schedule.dates

        val todayKey = datesMap.entries.find { it.value == today }?.key ?: return null
        return daysMap.keys.indexOf(todayKey)
    }
}