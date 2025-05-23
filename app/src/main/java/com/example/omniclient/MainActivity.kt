package com.example.omniclient

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
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
import androidx.navigation.compose.currentBackStackEntryAsState
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
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import com.example.omniclient.components.NavigationDrawer
import androidx.compose.material3.CircularProgressIndicator
import com.example.omniclient.ui.attendance.AttendanceScreen
import com.example.omniclient.ui.homework.HomeworkScreen


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
                systemUiController.setNavigationBarColor(
                    color = Color(0xFFFFF8F8),
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
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val username by loginViewModel.username.collectAsState()
    val password by loginViewModel.password.collectAsState()
    val isLoading by loginViewModel.isLoading.collectAsState()
    val responseText by loginViewModel.responseText.collectAsState()
    val schedule by loginViewModel.schedule.collectAsState()
    val isLoggedIn by loginViewModel.isLoggedIn.collectAsState()
    val csrfToken by loginViewModel.csrfToken.collectAsState()
    val autoLoginInProgress by loginViewModel.autoLoginInProgress.collectAsState()
    val allUsers by loginViewModel.allUsers.collectAsState()
    val triedAutoLogin by loginViewModel.triedAutoLogin.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val gesturesEnabled = navBackStackEntry?.destination?.route != "login"

    LaunchedEffect(Unit) {
        val savedUsername = loginViewModel.authPreferences.getUsername()
        val savedPassword = loginViewModel.authPreferences.getPassword()
        if (!savedUsername.isNullOrEmpty()) {
            loginViewModel.loadScheduleFromDbForUser(savedUsername)
        }
        Log.d("Dev:Login", "tryAutoLogin")
        if (!triedAutoLogin && !savedUsername.isNullOrEmpty() && !savedPassword.isNullOrEmpty()) {
            Log.d("Dev:Login", "tryAutoLogin first if")
            loginViewModel.autoLoginIfPossible(
                onAutoLoginSuccess = {
                    Log.d("Dev:Login", "tryAutoLogin success")
                    navController.navigate("schedule") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onAutoLoginFailed = {
                    Log.d("Dev:Login", "tryAutoLogin failed")
                    loginViewModel.setTriedAutoLogin(true)
                }
            )
        } else {
            Log.d("Dev:Login", "tryAutoLogin another else")
            loginViewModel.setTriedAutoLogin(true)
        }
    }

    LaunchedEffect(schedule) {
        val currentRoute = navBackStackEntry?.destination?.route
        if (schedule != null && currentRoute == "login") {
            navController.navigate("schedule") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    if (autoLoginInProgress && schedule == null) {
        Log.d("Dev:Login", "autoLogin")
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    NavigationDrawer(
        enableGesture = gesturesEnabled,
        drawerState = drawerState,
        navController = navController,
        scope = scope,
        users = allUsers,
        currentUsername = username,
        onUserSelected = { user ->
            scope.launch {
                loginViewModel.logout()
                loginViewModel.loadScheduleFromDbForUser(user.username)
                loginViewModel.selectUser(
                    user,
                    onAutoLoginSuccess = {
                        navController.navigate("schedule") {
                            popUpTo("login") { inclusive = true }
                        }
                        scope.launch { drawerState.close() }
                    },
                    onAutoLoginFailed = {
                        Toast.makeText(context, "Ошибка автологина", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        },
        onAddUser = {
            loginViewModel.onUsernameChange("")
            loginViewModel.onPasswordChange("")
            loginViewModel.authPreferences.clearCredentials()
            loginViewModel.setTriedAutoLogin(true)
            navController.navigate("login") {
                popUpTo("login") { inclusive = true }
            }
            scope.launch { drawerState.close() }
        }
    ) {
        NavHost(navController, startDestination = "login") {
            composable("login") {
                if (!triedAutoLogin) {
                    Log.d("Dev:Login", "triedAutoLogin")
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.Red, strokeWidth = 4.dp)
                    }
                } else {
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
            }
            composable("schedule") {
                if (schedule != null) {
                    ScheduleScreen(
                        navController = navController,
                        apiService = apiService,
                        csrfToken = csrfToken ?: "",
                        openDrawer = { scope.launch { drawerState.open() } },
                        loginViewModel = loginViewModel
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.navigate("login") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Ошибка: расписание не найдено. Возврат на экран входа...")
                    }
                }
            }
            composable("attendance/{lenta}") { backStackEntry ->
                val lenta = backStackEntry.arguments?.getString("lenta") ?: ""
                AttendanceScreen(
                    navController = navController, lenta = lenta,
                    loginViewModel = loginViewModel
                )
            }
            composable("homework") {
                HomeworkScreen(
                    navController = navController,
                    loginViewModel = loginViewModel,
                    openDrawer = { scope.launch { drawerState.open() } }
                )
            }
        }
    }

    /*
    if (responseText.isNotEmpty()) {
        LaunchedEffect(responseText) {
            Toast.makeText(context, responseText, Toast.LENGTH_SHORT).show()
        }
    }
    */
}


suspend fun fetchSchedule(csrfToken: String, week: Int = 0): ScheduleResponse? {
    return try {
        val request = ScheduleRequest(week = week)

        val cookies = (okHttpClient.cookieJar as MyCookieJar)
            .loadForRequest("https://omni.top-academy.ru".toHttpUrlOrNull()!!)
        println("Куки перед запросом расписания: "+ cookies.joinToString { "${it.name}=${it.value}" })

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

suspend fun fetchCombinedSchedule(csrfToken: String, week: Int = 0): ScheduleResponse? {
    if (!changeCity(458)) {
        Log.d("Dev:Schedule", "Error with change city in MainActivity.kt")
        return null
    }

    val schedule1 = fetchSchedule(csrfToken, week)?.let { response ->
        response.copy(
            body = response.body.mapValues { (_, dayLessons) ->
                dayLessons.mapValues { (_, lesson) ->
                    lesson.copy(divisionId = 458) // Колледж
                }
            }
        )
    } ?: return null

    if (!changeCity(74)) {
        Log.d("Dev:Schedule", "Error with change city in MainActivity.kt")
        return null
    }

    val schedule2 = fetchSchedule(csrfToken, week)?.let { response ->
        response.copy(
            body = response.body.mapValues { (_, dayLessons) ->
                dayLessons.mapValues { (_, lesson) ->
                    lesson.copy(divisionId = 74) // Академия
                }
            }
        )
    } ?: return null

    return mergeSchedules(schedule1, schedule2)
}
