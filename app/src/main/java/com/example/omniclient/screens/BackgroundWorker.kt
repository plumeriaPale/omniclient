package com.example.omniclient.screens

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.omniclient.api.AcademyClient
import com.example.omniclient.api.CollegeClient
import com.example.omniclient.data.NotDoneTasksRepository

class BackgroundWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {  // Изменили на CoroutineWorker

    override suspend fun doWork(): Result {  // Теперь метод suspend
        return try {
            Log.d("Dev: Worker", "doWork started")
            // Получаем необходимые зависимости
            val settingsViewModel = SettingsViewModel(applicationContext)
            val notDoneTasksRepository = NotDoneTasksRepository(
                academyClient = AcademyClient,
                collegeClient = CollegeClient
            )

            // Получаем данные для академии и колледжа
            val academyData = notDoneTasksRepository.getNotDoneTasks(74)
            val collegeData = notDoneTasksRepository.getNotDoneTasks(458)

            // Суммируем значения
            val totalHomework = (academyData?.newHomework ?: 0) + (collegeData?.newHomework ?: 0)
            val totalReviews = (academyData?.reviewsData?.count_students ?: 0) +
                    (collegeData?.reviewsData?.count_students ?: 0)

            // Проверяем обновления
            settingsViewModel.checkForUpdates(totalHomework, totalReviews)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}