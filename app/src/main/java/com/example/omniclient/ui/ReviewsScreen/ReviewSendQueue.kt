package com.example.omniclient.ui.ReviewsScreen

import android.util.Log
import com.example.omniclient.api.AcademyClient
import com.example.omniclient.api.ApiService
import com.example.omniclient.api.CollegeClient
import com.example.omniclient.api.ReviewRequest
import com.example.omniclient.data.Student
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Response
import java.util.concurrent.ConcurrentLinkedQueue

object ReviewSendQueue {
    data class ReviewSendTask(
        val student: Student,
        val comment: String,
        val divisionId: Int // 458 - колледж, 74 - академия
    )

    val queue = ConcurrentLinkedQueue<Pair<ReviewSendTask, ((ReviewSendTask) -> Unit)?>>()
    @Volatile private var isProcessing = false

    private val _collegeQueueCount = MutableStateFlow(0)
    val collegeQueueCount: StateFlow<Int> = _collegeQueueCount.asStateFlow()
    private val _academyQueueCount = MutableStateFlow(0)
    val academyQueueCount: StateFlow<Int> = _academyQueueCount.asStateFlow()

    fun enqueue(
        task: ReviewSendTask,
        onSuccess: (ReviewSendTask) -> Unit, // Новый колбэк для успеха
        onFail: ((ReviewSendTask) -> Unit)? = null
    ) {
        queue.add(Pair(task, onFail))
        updateQueueCounts()
        processQueue(task.divisionId, onSuccess)
    }

    private fun processQueue(divisionId: Int, onSuccess: (ReviewSendTask) -> Unit) {
        if (isProcessing) return
        isProcessing = true

        CoroutineScope(Dispatchers.IO).launch {
            while (queue.isNotEmpty()) {
                val (task, onFail) = queue.poll() ?: continue
                try {
                    // Формируем запрос
                    val request = ReviewRequest(
                        studentId = task.student.id_stud,
                        comment = task.comment,
                        specializationId = task.student.id_spec,
                    )

                    Log.d("Dev:ReviewSendQueue", "${request}")

                    var response: Response<ResponseBody>? = null
                    if (divisionId == 74)
                        response = AcademyClient.sendReview(request)
                    if (divisionId == 458)
                        response = CollegeClient.sendReview(request)

                    if (response!!.isSuccessful) {
                        Log.d("Dev: ReviewSendQueue", "${response.body().toString()}")
                        Log.d("Dev: ReviewSendQueue", "Отзыв отправлен: ${task.student.id_stud}")

                        // Вызываем колбэк успеха
                        onSuccess(task)
                    } else {
                        Log.e("Dev: ReviewSendQueue", "Ошибка отправки: ${task.student.id_stud}")
                        onFail?.invoke(task)
                    }
                } catch (e: Exception) {
                    Log.e("Dev: ReviewSendQueue", "Исключение: ${task.student.id_stud} ${e.message}")
                    onFail?.invoke(task)
                } finally {
                    updateQueueCounts()
                    delay(500) // Задержка между запросами
                }
            }
            isProcessing = false
            updateQueueCounts()
        }
    }

    private fun updateQueueCounts() {
        val all = queue.map { it.first }
        _collegeQueueCount.value = all.count { it.divisionId == 458 }
        _academyQueueCount.value = all.count { it.divisionId == 74 }
    }
}