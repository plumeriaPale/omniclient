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