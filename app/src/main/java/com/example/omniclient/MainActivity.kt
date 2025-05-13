package com.example.omniclient

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.omniclient.api.MyCookieJar
import com.example.omniclient.api.ScheduleRequest
import com.example.omniclient.api.ScheduleResponse
import com.example.omniclient.api.apiService
import com.example.omniclient.api.okHttpClient
import com.example.omniclient.ui.login.LoginScreen
import com.example.omniclient.ui.schedule.ScheduleScreen
import com.example.omniclient.ui.schedule.changeCity
import com.example.omniclient.ui.schedule.mergeSchedules
import com.example.omniclient.ui.theme.OMNIClientTheme
import com.example.omniclient.viewmodels.LoginViewModel
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
    val context = LocalContext.current
    val loginViewModel = remember {
        LoginViewModel(context, apiService)
    }

    val navController = rememberNavController()

    val username by loginViewModel.username.collectAsState()
    val password by loginViewModel.password.collectAsState()
    val isLoading by loginViewModel.isLoading.collectAsState()
    val responseText by loginViewModel.responseText.collectAsState()
    val schedule by loginViewModel.schedule.collectAsState()
    val isLoggedIn by loginViewModel.isLoggedIn.collectAsState()
    val csrfToken by loginViewModel.csrfToken.collectAsState()

    LaunchedEffect(isLoggedIn, csrfToken) {
        if (isLoggedIn && csrfToken != null) {
            navController.navigate("schedule") {
                popUpTo("login") { inclusive = true }
            }
        }
    }


    NavHost(navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                username = username,
                onUsernameChange = loginViewModel::onUsernameChange,
                password = password,
                onPasswordChange = loginViewModel::onPasswordChange,
                isLoading = isLoading,
                onLogin = {
                    loginViewModel.login {
                        navController.navigate("schedule")
                    }
                }
            )
        }
        composable("schedule") {
            val currentCsrfToken = csrfToken
            if (currentCsrfToken != null) {
                ScheduleScreen(
                    navController = navController,
                    apiService = apiService,
                    csrfToken = currentCsrfToken
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Ошибка: CSRF-токен не найден. Возврат на экран входа...")
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
