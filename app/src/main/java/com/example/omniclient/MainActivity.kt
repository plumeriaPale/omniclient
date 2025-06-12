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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.omniclient.api.CollegeClient
import com.example.omniclient.ui.attendance.AttendanceScreen
import com.example.omniclient.ui.homework.HomeworkScreen


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("GlobalException", "Uncaught exception in thread "+thread.name, throwable)
            // Можно добавить показ тоста или отправку логов
        }

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

    var didNavigateToSchedule by remember { mutableStateOf(false) }

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
                    didNavigateToSchedule = false
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
        if (schedule != null && currentRoute == "login" && !didNavigateToSchedule) {
            navController.navigate("schedule") {
                popUpTo("login") { inclusive = true }
            }
            didNavigateToSchedule = true
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
                navController.navigate("schedule") {
                    popUpTo("login") { inclusive = true }
                }
                scope.launch { drawerState.close() }
                loginViewModel.selectUser(
                    user,
                    onAutoLoginSuccess = {
                        // Можно обновить расписание, если нужно
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
        },
        loginViewModel = loginViewModel
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
                                didNavigateToSchedule = false
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
            composable("attendance/{lenta}?divisionId={divisionId}") { backStackEntry ->
                val lenta = backStackEntry.arguments?.getString("lenta") ?: ""
                val divisionId = backStackEntry.arguments?.getString("divisionId")?.toIntOrNull() ?: 0
                AttendanceScreen(
                    navController = navController, lenta = lenta, divisionId = divisionId,
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
