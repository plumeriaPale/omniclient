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

    init {
        loadSchedule()
        Log.d("Schedule", "Navigate")
    }

    private fun loadSchedule() {
        Log.d("Schedule", "Navigate")
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val loadedSchedule = fetchCombinedSchedule(csrfToken)
                _schedule.value = loadedSchedule

                loadedSchedule?.let {
                    val daysOfWeek = it.days.values.toList()
                    val currentDayOfWeek = getCurrentDayOfWeek(it.curdate)
                    val initialPage = daysOfWeek.indexOf(currentDayOfWeek)
                    _currentDayIndex.value = if (initialPage != -1) initialPage else 0
                }

            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки расписания: ${e.message}"
            } finally {
                _isLoading.value = false
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
        val currentDayOfWeek = getDaysOfWeek().getOrNull(_currentDayIndex.value)
        return if (currentDayOfWeek != null && _schedule.value != null) {
            getLessonsForDay(_schedule.value!!, currentDayOfWeek)
        } else {
            emptyList()
        }
    }
}