package com.example.omniclient.preferences

import android.content.Context

class AuthPreferences(context: Context) {

    private val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveCredentials(username: String, password: String) {
        sharedPreferences.edit().apply {
            putString("username", username)
            putString("password", password)
            apply()
        }
    }

    // Фоновый режим
    fun setBackgroundModeEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("background_mode_enabled", enabled).apply()
    }

    fun getBackgroundModeEnabled(): Boolean {
        return sharedPreferences.getBoolean("background_mode_enabled", false)
    }

    // Уведомления о ДЗ
    fun setHomeworkNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("homework_notifications_enabled", enabled).apply()
    }

    fun getHomeworkNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean("homework_notifications_enabled", false)
    }

    // Уведомления об отзывах
    fun setReviewsNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("reviews_notifications_enabled", enabled).apply()
    }

    fun getReviewsNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean("reviews_notifications_enabled", false)
    }

    // Последние значения
    fun setLastHomeworkCount(count: Int) {
        sharedPreferences.edit().putInt("last_homework_count", count).apply()
    }

    fun getLastHomeworkCount(): Int {
        return sharedPreferences.getInt("last_homework_count", 0)
    }

    fun setLastReviewsCount(count: Int) {
        sharedPreferences.edit().putInt("last_reviews_count", count).apply()
    }

    fun getLastReviewsCount(): Int {
        return sharedPreferences.getInt("last_reviews_count", 0)
    }

    fun getUsername(): String? {
        return sharedPreferences.getString("username", null)
    }

    fun getPassword(): String? {
        return sharedPreferences.getString("password", null)
    }

    fun clearCredentials() {
        sharedPreferences.edit().clear().apply()
    }
}