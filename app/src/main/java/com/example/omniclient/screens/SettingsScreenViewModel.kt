package com.example.omniclient.screens

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.omniclient.R
import com.example.omniclient.preferences.AuthPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder

class SettingsViewModel(private val context: Context) : ViewModel() {
    private val prefs = AuthPreferences(context)

    private val _backgroundModeEnabled = MutableStateFlow(prefs.getBackgroundModeEnabled())
    val backgroundModeEnabled: StateFlow<Boolean> = _backgroundModeEnabled

    private val _homeworkNotificationsEnabled = MutableStateFlow(prefs.getHomeworkNotificationsEnabled())
    val homeworkNotificationsEnabled: StateFlow<Boolean> = _homeworkNotificationsEnabled

    private val _reviewsNotificationsEnabled = MutableStateFlow(prefs.getReviewsNotificationsEnabled())
    val reviewsNotificationsEnabled: StateFlow<Boolean> = _reviewsNotificationsEnabled

    // Последние известные значения
    private var lastHomeworkCount = prefs.getLastHomeworkCount()
    private var lastReviewsCount = prefs.getLastReviewsCount()

    fun setBackgroundModeEnabled(enabled: Boolean) {
        _backgroundModeEnabled.value = enabled
        prefs.setBackgroundModeEnabled(enabled)
        if (enabled) {
            scheduleBackgroundWork()
        } else {
            cancelBackgroundWork()
        }
    }

    fun setHomeworkNotificationsEnabled(enabled: Boolean) {
        _homeworkNotificationsEnabled.value = enabled
        prefs.setHomeworkNotificationsEnabled(enabled)
    }

    fun setReviewsNotificationsEnabled(enabled: Boolean) {
        _reviewsNotificationsEnabled.value = enabled
        prefs.setReviewsNotificationsEnabled(enabled)
    }

    private fun scheduleBackgroundWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<BackgroundWorker>(
            15, // Каждые 1 минут
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "background_notifications",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun cancelBackgroundWork() {
        WorkManager.getInstance(context).cancelUniqueWork("background_notifications")
    }

    // Вызывается из фонового воркера
    fun checkForUpdates(newHomeworkCount: Int, newReviewsCount: Int) {
        Log.d("Dev: SettingsViewModel", "CheckForUpdates: newHomework=$newHomeworkCount, last=$lastHomeworkCount")

        val context = context

        Log.d("Dev: SettingsViewModel", "homeworkNotificationsEnabled: value=${homeworkNotificationsEnabled.value}, last=$lastHomeworkCount")
        // Проверка новых ДЗ

        if (homeworkNotificationsEnabled.value && newHomeworkCount > lastHomeworkCount) {
            showNotification(
                context,
                "Новые домашние задания",
                "Появилось $newHomeworkCount новых ДЗ",
                NotificationChannels.HOMEWORK_CHANNEL_ID
            )
        }


        /*
        if (homeworkNotificationsEnabled.value && newHomeworkCount > -1) {
            Log.d("Dev: SettingsViewModel", "showNotificationShouldStart")
            showNotification(
                context,
                "Новые домашние задания",
                "Появилось $newHomeworkCount новых ДЗ",
                NotificationChannels.HOMEWORK_CHANNEL_ID
            )
        }
        */

        // Проверка новых отзывов
        if (reviewsNotificationsEnabled.value && newReviewsCount > lastReviewsCount) {
            showNotification(
                context,
                "Новые студенты на проверке",
                "Ожидают проверки $newReviewsCount студентов",
                NotificationChannels.REVIEWS_CHANNEL_ID
            )
        }

        // Сохраняем новые значения
        lastHomeworkCount = newHomeworkCount
        lastReviewsCount = newReviewsCount
        prefs.setLastHomeworkCount(newHomeworkCount)
        prefs.setLastReviewsCount(newReviewsCount)
    }

    private fun showNotification(context: Context, title: String, message: String, channelId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Создаем канал для уведомлений (требуется для Android 8.0+)
        createNotificationChannel(
            context,
            channelId,
            if (channelId == NotificationChannels.HOMEWORK_CHANNEL_ID) "Домашние задания" else "Проверки студентов"
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        Log.d("Dev: showNotification", "notificationManager.notify should start")
        notificationManager.notify(Random.nextInt(), notification)
    }

    private fun createNotificationChannel(context: Context, channelId: String, channelName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Уведомления о $channelName"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

object NotificationChannels {
    const val HOMEWORK_CHANNEL_ID = "homework_channel"
    const val REVIEWS_CHANNEL_ID = "reviews_channel"
}