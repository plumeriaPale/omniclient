package com.example.omniclient.ui.homework

import android.util.Log
import com.example.omniclient.api.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.omniclient.data.HomeworkRepository

object HomeworkSendQueue {
    data class HomeworkSendTask(
        val homework: Homework,
        val mark: Int?,
        val comment: String?,
        val division: Int // 458 - колледж, 74 - академия
    )

    private val queue = ConcurrentLinkedQueue<Pair<HomeworkSendTask, ((HomeworkSendTask) -> Unit)?>>()
    @Volatile private var isProcessing = false

    private val _collegeQueueCount = MutableStateFlow(0)
    val collegeQueueCount: StateFlow<Int> = _collegeQueueCount.asStateFlow()
    private val _academyQueueCount = MutableStateFlow(0)
    val academyQueueCount: StateFlow<Int> = _academyQueueCount.asStateFlow()

    lateinit var homeworkRepository: HomeworkRepository

    fun enqueue(
        task: HomeworkSendTask,
        onFail: ((HomeworkSendTask) -> Unit)? = null
    ) {
        queue.add(Pair(task, onFail))
        updateQueueCounts()
        processQueue()
    }

    private fun processQueue() {
        if (isProcessing) return
        isProcessing = true
        CoroutineScope(Dispatchers.IO).launch {
            while (queue.isNotEmpty()) {
                val (task, onFail) = queue.poll() ?: continue
                try {
                    val body = mapOf("HomeworkForm" to generateSaveHomeworkBody(task.homework, task.mark, task.comment))
                    val success = homeworkRepository.sendHomework(task.division, body)
                    if (success) {
                        Log.d("Dev:HomeworkQueue", "Успешно отправлено: ${task.homework.id}")
                    } else {
                        Log.e("Dev:HomeworkQueue", "Ошибка отправки: ${task.homework.id}")
                        onFail?.invoke(task)
                    }
                } catch (e: Exception) {
                    Log.e("Dev:HomeworkQueue", "Исключение при отправке: ${task.homework.id} ${e.message}")
                    onFail?.invoke(task)
                }
                updateQueueCounts()
            }
            isProcessing = false
            updateQueueCounts()
        }
    }

    private fun updateQueueCounts() {
        val all = queue.map { it.first }
        _collegeQueueCount.value = all.count { it.division == 458 }
        _academyQueueCount.value = all.count { it.division == 74 }
    }
} 