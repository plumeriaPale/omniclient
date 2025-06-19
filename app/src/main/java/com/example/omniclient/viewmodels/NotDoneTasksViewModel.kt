package com.example.omniclient.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.omniclient.data.NotDoneTasksData
import com.example.omniclient.data.NotDoneTasksRepository
import kotlinx.coroutines.launch

class NotDoneTasksViewModel(
    private val repository: NotDoneTasksRepository
) : ViewModel() {
    private val _state = mutableStateOf<NotDoneTasksState>(NotDoneTasksState.Loading)
    val state: State<NotDoneTasksState> = _state

    fun loadData(divisionId: Int) {
        viewModelScope.launch {
            _state.value = NotDoneTasksState.Loading
            try {
                val data = repository.getNotDoneTasks(divisionId)
                _state.value = if (data != null) {
                    NotDoneTasksState.Success(data)
                } else {
                    NotDoneTasksState.Error("Data is null")
                }
            } catch (e: Exception) {
                _state.value = NotDoneTasksState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class NotDoneTasksState {
    object Loading : NotDoneTasksState()
    data class Success(val data: NotDoneTasksData) : NotDoneTasksState()
    data class Error(val message: String) : NotDoneTasksState()
}