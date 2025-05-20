package com.example.omniclient.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.omniclient.api.MyCookieJar
import com.example.omniclient.api.okHttpClient
import com.example.omniclient.preferences.AuthPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log
import com.example.omniclient.api.ApiService
import com.example.omniclient.api.LoginFormData
import com.example.omniclient.api.LoginRequest
import com.example.omniclient.api.ScheduleResponse
import com.example.omniclient.api.initializeCsrfToken
import com.example.omniclient.fetchCombinedSchedule
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import com.example.omniclient.data.db.DatabaseProvider
import com.example.omniclient.data.db.UserEntity
import com.example.omniclient.ui.homework.Homework
import com.example.omniclient.ui.homework.generateSaveHomeworkBody
import com.example.omniclient.ui.schedule.changeCity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.ConcurrentLinkedQueue

class LoginViewModel(
    private val context: Context,
    private val apiService: ApiService
) : ViewModel() {

    public val authPreferences = AuthPreferences(context)

    private val _username = MutableStateFlow(authPreferences.getUsername() ?: "")
    val username: StateFlow<String> = _username

    private val _password = MutableStateFlow(authPreferences.getPassword() ?: "")
    val password: StateFlow<String> = _password

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _responseText = MutableStateFlow("")
    val responseText: StateFlow<String> = _responseText

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _csrfToken = MutableStateFlow<String?>(null)
    val csrfToken: StateFlow<String?> = _csrfToken.asStateFlow()

    private val _schedule = MutableStateFlow<ScheduleResponse?>(null)
    val schedule: StateFlow<ScheduleResponse?> = _schedule

    private val _autoLoginInProgress = MutableStateFlow(false)
    val autoLoginInProgress: StateFlow<Boolean> = _autoLoginInProgress

    private val _allUsers = MutableStateFlow<List<UserEntity>>(emptyList())
    val allUsers: StateFlow<List<UserEntity>> = _allUsers

    private val _triedAutoLogin = MutableStateFlow(false)
    val triedAutoLogin: StateFlow<Boolean> = _triedAutoLogin
    fun setTriedAutoLogin(value: Boolean) {
        Log.d("Dev:Login", "setTriedAutoLogin")
        _triedAutoLogin.value = value }

    private val userDao by lazy {
        DatabaseProvider.getDatabase(context).userDao()
    }

    private fun loadAllUsers() {
        viewModelScope.launch(Dispatchers.IO) {
            _allUsers.value = userDao.getAllUsers()
        }
    }

    init {
        loadAllUsers()
    }

    fun onUsernameChange(newUsername: String) {
        _username.value = newUsername
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
    }

    fun login(onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _responseText.value = ""
            val usernameValue = _username.value
            val passwordValue = _password.value
            val request = LoginRequest(LoginFormData(username = usernameValue, password = passwordValue))
            try {
                val response = apiService.login(request)
                if (response.isSuccessful) {
                    Log.d("Dev:AuthResponseBody", response.toString())
                    _isLoggedIn.value = true
                    _responseText.value = "Успешная авторизация!"
                    authPreferences.saveCredentials(usernameValue, passwordValue)

                    withContext(Dispatchers.IO) {
                        userDao.insertUser(UserEntity(usernameValue, passwordValue))
                    }

                    val cookies = (okHttpClient.cookieJar as MyCookieJar)
                        .loadForRequest("https://omni.top-academy.ru".toHttpUrlOrNull()!!)
                    val csrfTokenValue = cookies.firstOrNull { it.name == "_csrf" }?.value
                    _csrfToken.value = csrfTokenValue
                    Log.d("Dev:LoginViewModel", "CSRF-токен после входа: $csrfTokenValue")

                    if (csrfTokenValue != null) {
                        onLoginSuccess()
                    } else {
                        _responseText.value = "Авторизация успешна, но не удалось получить CSRF-токен."
                    }

                    loadAllUsers()

                } else {
                    _responseText.value = "Ошибка: ${response.code()} - ${response.message()}"
                }
            } catch (e: Exception) {
                _responseText.value = "Ошибка: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun autoLoginIfPossible(onAutoLoginSuccess: () -> Unit, onAutoLoginFailed: (() -> Unit)? = null) {
        val username = authPreferences.getUsername()
        val password = authPreferences.getPassword()
        if (!username.isNullOrEmpty() && !password.isNullOrEmpty()) {
            _username.value = username
            _password.value = password
            _autoLoginInProgress.value = true
            viewModelScope.launch {
                val request = LoginRequest(LoginFormData(username = username, password = password))
                try {
                    val response = apiService.login(request)
                    if (response.isSuccessful) {
                        Log.d("Dev:AuthResponseBody", response.toString())
                        _isLoggedIn.value = true
                        _responseText.value = "Успешная авторизация!"
                        authPreferences.saveCredentials(username, password)
                        withContext(Dispatchers.IO) {
                            userDao.insertUser(UserEntity(username, password))
                        }
                        val cookies = (okHttpClient.cookieJar as MyCookieJar)
                            .loadForRequest("https://omni.top-academy.ru".toHttpUrlOrNull()!!)
                        val csrfTokenValue = cookies.firstOrNull { it.name == "_csrf" }?.value
                        _csrfToken.value = csrfTokenValue
                        Log.d("Dev:LoginViewModel", "CSRF-токен после входа: $csrfTokenValue")
                        _autoLoginInProgress.value = false
                        if (csrfTokenValue != null) {
                            onAutoLoginSuccess()
                        } else {
                            _responseText.value = "Авторизация успешна, но не удалось получить CSRF-токен."
                            onAutoLoginFailed?.invoke()
                        }
                    } else {
                        _responseText.value = "Ошибка: ${response.code()} - ${response.message()}"
                        _autoLoginInProgress.value = false
                        onAutoLoginFailed?.invoke()
                    }
                } catch (e: Exception) {
                    _responseText.value = "Ошибка: ${e.message}"
                    _autoLoginInProgress.value = false
                    onAutoLoginFailed?.invoke()
                }
            }
        } else {
            onAutoLoginFailed?.invoke()
        }

        loadAllUsers()
    }

    fun selectUser(user: UserEntity, onAutoLoginSuccess: () -> Unit, onAutoLoginFailed: (() -> Unit)? = null) {
        authPreferences.saveCredentials(user.username, user.password)
        _username.value = user.username
        _password.value = user.password
        autoLoginIfPossible(onAutoLoginSuccess, onAutoLoginFailed)
    }

    fun logout() {
        (okHttpClient.cookieJar as? MyCookieJar)?.clearCookies()
        Log.d("Dev:Login", "Logout")
        _username.value = ""
        _password.value = ""
        authPreferences.clearCredentials()
        setTriedAutoLogin(true)
    }

    // Очередь на отправку оценок
    data class HomeworkSendTask(
        val homework: Homework,
        val mark: Int?,
        val comment: String?,
        val division: Int // 458 - колледж, 74 - академия
    )

    private val homeworkSendQueue = ConcurrentLinkedQueue<HomeworkSendTask>()
    private var isSendingHomework = false

    fun enqueueHomeworkSend(hw: Homework, mark: Int?, comment: String?, division: Int) {
        homeworkSendQueue.add(HomeworkSendTask(hw, mark, comment, division))
        processHomeworkQueue()
    }

    private fun processHomeworkQueue() {
        if (isSendingHomework) return
        isSendingHomework = true
        viewModelScope.launch {
            while (homeworkSendQueue.isNotEmpty()) {
                val task = homeworkSendQueue.poll() ?: continue
                try {
                    changeCity(task.division)
                    val body = mapOf("HomeworkForm" to generateSaveHomeworkBody(task.homework, task.mark, task.comment))
                    val response = apiService.saveHomework(body)
                    if (response.isSuccessful) {
                        Log.d("Dev:HomeworkQueue", "Успешно отправлено: ${task.homework.id}")
                    } else {
                        Log.e("Dev:HomeworkQueue", "Ошибка отправки: ${task.homework.id} code=${response.code()} message=${response.message()} body=${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e("Dev:HomeworkQueue", "Исключение при отправке: ${task.homework.id} ${e.message}")
                }
            }
            isSendingHomework = false
        }
    }
}
