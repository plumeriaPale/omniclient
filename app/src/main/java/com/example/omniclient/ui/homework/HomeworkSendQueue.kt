package com.example.omniclient.ui.homework

import android.util.Log
import com.example.omniclient.api.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

object HomeworkSendQueue {
    data class HomeworkSendTask(
        val homework: Homework,
        val mark: Int?,
        val comment: String?,
        val division: Int // 458 - колледж, 74 - академия
    )

    private val queue = ConcurrentLinkedQueue<HomeworkSendTask>()
    @Volatile private var isProcessing = false

    fun enqueue(
        task: HomeworkSendTask,
        apiService: ApiService,
        changeCity: suspend (Int) -> Unit
    ) {
        queue.add(task)
        processQueue(apiService, changeCity)
    }

    private fun processQueue(
        apiService: ApiService,
        changeCity: suspend (Int) -> Unit
    ) {
        if (isProcessing) return
        isProcessing = true
        CoroutineScope(Dispatchers.IO).launch {
            while (queue.isNotEmpty()) {
                val task = queue.poll() ?: continue
                try {
                    changeCity(task.division)
                    val body = mapOf("HomeworkForm" to generateSaveHomeworkBody(task.homework, task.mark, task.comment))
                    val response = apiService.saveHomework(body)
                    if (response.isSuccessful) {
                        Log.d("Dev:HomeworkQueue", "Успешно отправлено: ${task.homework.id}")
                    } else {
                        Log.e("Dev:HomeworkQueue", "Ошибка отправки: ${task.homework.id} code=${response.code()} message=${response.message()} body=${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e("Dev:HomeworkQueue", "Исключение при отправке: ${task.homework.id} ${e.message}")
                }
            }
            isProcessing = false
        }
    }
} 