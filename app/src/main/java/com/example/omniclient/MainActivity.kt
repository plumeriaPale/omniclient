package com.example.omniclient

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.omniclient.api.LoginFormData
import com.example.omniclient.api.LoginRequest
import com.example.omniclient.api.MyCookieJar
import com.example.omniclient.api.ScheduleRequest
import com.example.omniclient.api.ScheduleResponse
import com.example.omniclient.api.apiService
import com.example.omniclient.api.initializeCsrfToken
import com.example.omniclient.api.okHttpClient
import com.example.omniclient.ui.theme.OMNIClientTheme
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import com.google.accompanist.systemuicontroller.rememberSystemUiController


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val systemUiController = rememberSystemUiController()
            enableEdgeToEdge()
            SideEffect {
                systemUiController.setStatusBarColor(
                    color = Color.Transparent,
                    darkIcons = true
                )
            }

            OMNIClientTheme {
                MyApp()
            }
        }
    }
}

@Composable
fun MyApp() {
    var isLoggedIn by remember { mutableStateOf(false) }
    var responseText by remember { mutableStateOf("") }
    var schedule by remember { mutableStateOf<ScheduleResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val navController = rememberNavController()

    NavHost(navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                isLoading = isLoading,
                onLogin = { username, password ->
                    coroutineScope.launch {
                        isLoading = true
                        val request = LoginRequest(LoginFormData(username = username, password = password))
                        try {
                            val response = apiService.login(request)
                            if (response.isSuccessful) {
                                Log.d("AuthResponseBody", response.toString())
                                isLoggedIn = true
                                responseText = "Успешная авторизация!"

                                val csrfToken = initializeCsrfToken()
                                Log.d("SecondToken", csrfToken.toString())
                                if (csrfToken != null) {
                                    schedule = fetchCombinedSchedule(csrfToken)
                                    navController.navigate("schedule")
                                } else {
                                    responseText = "Не удалось получить CSRF-токен"
                                }
                            } else {
                                responseText = "Ошибка: ${response.code()} - ${response.message()}"
                            }
                        } catch (e: Exception) {
                            responseText = "Ошибка: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                }, context = context
            )
        }

        composable("schedule") {
            schedule?.let {
                ScheduleScreen(it)
            } ?: run {
                Column {
                    Text(text = "Ошибка получения расписания")
                }
            }
        }
    }

    if (responseText.isNotEmpty()) {
        LaunchedEffect(responseText) {
            Toast.makeText(context, responseText, Toast.LENGTH_SHORT).show()
        }
    }
}

suspend fun fetchSchedule(csrfToken: String): ScheduleResponse? {
    return try {
        val request = ScheduleRequest(week = 0)

        val cookies = (okHttpClient.cookieJar as MyCookieJar)
            .loadForRequest("https://omni.top-academy.ru".toHttpUrlOrNull()!!)
        println("Куки перед запросом расписания: ${cookies.joinToString { "${it.name}=${it.value}" }}")

        val response = apiService.getSchedule(csrfToken, request)

        if (response.isSuccessful) {
            response.body()
        } else {
            println("Ошибка получения расписания: ${response.code()} - ${response.message()}")
            null
        }
    } catch (e: Exception) {
        println("Исключение при получении расписания: ${e.message}")
        null
    }
}

suspend fun fetchCombinedSchedule(csrfToken: String): ScheduleResponse? {
    val schedule1 = fetchSchedule(csrfToken) ?: return null

    if (!changeCity(458)) {
        return null
    }

    val schedule2 = fetchSchedule(csrfToken) ?: return null

    return mergeSchedules(schedule1, schedule2)
}
