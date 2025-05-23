package com.example.omniclient.ui.testing

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import com.example.omniclient.api.ApiService
import com.example.omniclient.api.LoginFormData
import com.example.omniclient.api.LoginRequest
import com.example.omniclient.api.MyCookieJar
import com.example.omniclient.api.ScheduleRequest
import com.example.omniclient.api.ScheduleResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import okhttp3.Interceptor
import okhttp3.Response as OkHttpResponse
import kotlinx.coroutines.launch

@Composable
fun TestingScreen() {
    val context = LocalContext.current
    var logText by remember { mutableStateOf("Нажмите кнопку для теста") }
    var isLoading by remember { mutableStateOf(false) }

    suspend fun createApiService(userAgent: String, cookieJar: MyCookieJar): ApiService {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("Dev: Testing", message)
        }.apply { level = HttpLoggingInterceptor.Level.BODY }
        val userAgentInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", userAgent)
                .build()
            chain.proceed(request)
        }
        val client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://omni.top-academy.ru/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(ApiService::class.java)
    }

    suspend fun loginAndGetCsrf(api: ApiService, username: String, password: String): String? {
        val loginRequest = LoginRequest(LoginFormData(username = username, password = password))
        val response = api.login(loginRequest)
        val body = response.body()?.string() ?: return null
        Log.d("Dev: Testing", "Login response: $body")
        val regex = Regex("\\\"IdLocalHash\\\":\\s*\\\"([a-f0-9]+)\\\"")
        val match = regex.find(body)
        return match?.groupValues?.getOrNull(1)
    }

    suspend fun changeCity(api: ApiService, cityId: Int): Boolean {
        val response = api.changeCity(cityId)
        Log.d("Dev: Testing", "changeCity($cityId): ${response.code()} ${response.message()}")
        return response.isSuccessful
    }

    suspend fun getSchedule(api: ApiService, csrf: String): ScheduleResponse? {
        val response = api.getSchedule(csrf, ScheduleRequest(week = 0))
        Log.d("Dev: Testing", "getSchedule: ${response.code()} ${response.message()}")
        return response.body()
    }

    val scope = rememberCoroutineScope()
    fun runTest() {
        isLoading = true
        logText = "Тестирование..."
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val cookieJar1 = MyCookieJar()
                    val cookieJar2 = MyCookieJar()
                    val userAgent1 = "TestAgent-Academy"
                    val userAgent2 = "TestAgent-College"
                    val api1 = createApiService(userAgent1, cookieJar1)
                    val api2 = createApiService(userAgent2, cookieJar2)
                    val username = "nikolaev_dm"
                    val password = "Qrz323Hre415!"
                    val csrf1 = loginAndGetCsrf(api1, username, password)
                    val csrf2 = loginAndGetCsrf(api2, username, password)
                    Log.d("Dev: Testing", "CSRF1: $csrf1, CSRF2: $csrf2")
                    if (csrf1 == null || csrf2 == null) {
                        logText = "Ошибка логина: CSRF не получен"
                        isLoading = false
                        return@withContext
                    }
                    val cityChanged = changeCity(api2, 458)
                    Log.d("Dev: Testing", "changeCity для колледжа: $cityChanged")
                    val schedule1 = getSchedule(api1, csrf1)
                    val schedule2 = getSchedule(api2, csrf2)
                    Log.d("Dev: Testing", "Schedule1 (Academy): ${schedule1 != null}")
                    Log.d("Dev: Testing", "Schedule2 (College): ${schedule2 != null}")
                    logText = "Academy расписание: ${if (schedule1 != null) "OK" else "Ошибка"}\n" +
                        "College расписание: ${if (schedule2 != null) "OK" else "Ошибка"}"
                } catch (e: Exception) {
                    logText = "Ошибка: ${e.message}"
                    Log.d("Dev: Testing", "Exception: ${e.message}")
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Тестировка", modifier = Modifier.padding(bottom = 16.dp))
        Button(onClick = { runTest() }, enabled = !isLoading) {
            Text(if (isLoading) "Тестируем..." else "Запустить тест")
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = logText)
    }
} 