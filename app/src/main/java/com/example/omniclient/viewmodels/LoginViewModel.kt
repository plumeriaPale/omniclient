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
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import com.example.omniclient.data.db.DatabaseProvider
import com.example.omniclient.data.db.UserEntity
import com.example.omniclient.ui.homework.Homework
import com.example.omniclient.ui.homework.generateSaveHomeworkBody
import com.example.omniclient.ui.schedule.changeCity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import java.net.UnknownHostException
import com.example.omniclient.api.AcademyClient
import com.example.omniclient.api.CollegeClient
import com.example.omniclient.api.academyCookieJar
import com.example.omniclient.api.collegeCookieJar
import com.example.omniclient.api.ProfileResponse

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

    private val _miniProfile = MutableStateFlow<ProfileResponse?>(null)
    val miniProfile: StateFlow<ProfileResponse?> = _miniProfile.asStateFlow()

    private val userDao by lazy {
        DatabaseProvider.getDatabase(context).userDao()
    }

    private fun loadAllUsers() {
        viewModelScope.launch(Dispatchers.IO) {
            _allUsers.value = userDao.getAllUsers()
        }
    }

    private fun loadMiniProfile() {
        viewModelScope.launch {
            try {
                val resp = com.example.omniclient.api.AcademyClient.getProfile()
                Log.d("MiniProfile", "Academy response: isSuccessful=${resp.isSuccessful}, body=${resp.body()}")
                if (resp.isSuccessful && resp.body() != null) {
                    Log.d("MiniProfile", "Academy teach_info: ${resp.body()!!.teach_info}")
                    _miniProfile.value = resp.body()
                    return@launch
                }
            } catch (e: Exception) {
                Log.e("MiniProfile", "Academy error: ${e.message}", e)
            }
            try {
                val resp = com.example.omniclient.api.CollegeClient.getProfile()
                Log.d("MiniProfile", "College response: isSuccessful=${resp.isSuccessful}, body=${resp.body()}")
                if (resp.isSuccessful && resp.body() != null) {
                    Log.d("MiniProfile", "College teach_info: ${resp.body()!!.teach_info}")
                    _miniProfile.value = resp.body()
                    return@launch
                }
            } catch (e: Exception) {
                Log.e("MiniProfile", "College error: ${e.message}", e)
            }
            Log.w("MiniProfile", "No mini profile found for user")
            _miniProfile.value = null
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
                        loadMiniProfile()
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
        Log.d("Dev: Login", "autoLoginIfPossible")
        val username = authPreferences.getUsername()
        val password = authPreferences.getPassword()
        Log.d("Dev: Login", "Getting cred's username: ${username}, pass: ${password}")
        if (!username.isNullOrEmpty() && !password.isNullOrEmpty()) {
            _username.value = username
            _password.value = password
            _autoLoginInProgress.value = true
            viewModelScope.launch {
                val request = LoginRequest(LoginFormData(username = username, password = password))
                while (true) {
                    try {
                        // Параллельно логинимся в оба клиента
                        val academyDeferred = async { AcademyClient.loginWithCity(request) }
                        val collegeDeferred = async { CollegeClient.loginWithCity(request) }
                        val academyResp = academyDeferred.await()
                        val collegeResp = collegeDeferred.await()
                        val academyOk = academyResp.isSuccessful
                        val collegeOk = collegeResp.isSuccessful
                        if (academyOk || collegeOk) {
                            _isLoggedIn.value = true
                            _responseText.value = "Успешная авторизация!"
                            authPreferences.saveCredentials(username, password)
                            withContext(Dispatchers.IO) {
                                userDao.insertUser(UserEntity(username, password))
                            }
                            // Параллельно получаем расписания
                            val academyScheduleDeferred = async { com.example.omniclient.api.AcademyClient.getSchedule("", com.example.omniclient.api.ScheduleRequest(0)).body() }
                            val collegeScheduleDeferred = async { com.example.omniclient.api.CollegeClient.getSchedule("", com.example.omniclient.api.ScheduleRequest(0)).body() }
                            val academySchedule = academyScheduleDeferred.await()
                            val collegeSchedule = collegeScheduleDeferred.await()

                            Log.d("Dev: Login", "Academy schedule: ${academySchedule}")
                            Log.d("Dev: Login", "College schedule: ${collegeSchedule}")

                            if (academySchedule == null && collegeSchedule == null){
                                _responseText.value = "Ошибка входа: Академия: ${academyResp.code()} ${academyResp.message()}, Колледж: ${collegeResp.code()} ${collegeResp.message()}"
                                _autoLoginInProgress.value = false
                                onAutoLoginFailed?.invoke()
                                break
                            }

                            val merged = when {
                                academySchedule != null && collegeSchedule != null -> com.example.omniclient.ui.schedule.mergeSchedules(academySchedule, collegeSchedule)
                                academySchedule != null -> academySchedule
                                collegeSchedule != null -> collegeSchedule
                                else -> null
                            }
                            _schedule.value = merged
                            loadMiniProfile()
                            _autoLoginInProgress.value = false
                            onAutoLoginSuccess()
                            break
                        } else {
                            _responseText.value = "Ошибка входа: Академия: ${academyResp.code()} ${academyResp.message()}, Колледж: ${collegeResp.code()} ${collegeResp.message()}"
                            _autoLoginInProgress.value = false
                            onAutoLoginFailed?.invoke()
                            break
                        }
                    } catch (e: Exception) {
                        if (e is UnknownHostException) {
                            _responseText.value = "Нет соединения с сервером. Повтор через 30 секунд..."
                            delay(30_000)
                            continue
                        } else {
                            _responseText.value = "Ошибка: ${e.message}"
                            _autoLoginInProgress.value = false
                            onAutoLoginFailed?.invoke()
                            break
                        }
                    }
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
        viewModelScope.launch {
            Log.d("Dev:LoginViewModel", "[selectUser] Start for user: ${user.username}")
            academyCookieJar.clearCookies()
            Log.d("Dev:LoginViewModel", "[selectUser] AcademyClient cookies cleared (direct)")
            collegeCookieJar.clearCookies()
            Log.d("Dev:LoginViewModel", "[selectUser] CollegeClient cookies cleared (direct)")
            val request = LoginRequest(LoginFormData(username = user.username, password = user.password))
            Log.d("Dev:LoginViewModel", "[selectUser] AcademyClient.loginWithCity for ${user.username}")
            val academyResp = AcademyClient.loginWithCity(request)
            Log.d("Dev:LoginViewModel", "[selectUser] AcademyClient login resp: code=${academyResp.code()} msg=${academyResp.message()} body=${academyResp.errorBody()?.string()}")
            Log.d("Dev:LoginViewModel", "[selectUser] CollegeClient.loginWithCity for ${user.username}")
            val collegeResp = CollegeClient.loginWithCity(request)
            Log.d("Dev:LoginViewModel", "[selectUser] CollegeClient login resp: code=${collegeResp.code()} msg=${collegeResp.message()} body=${collegeResp.errorBody()?.string()}")
            val academyOk = academyResp.isSuccessful
            val collegeOk = collegeResp.isSuccessful
            if (academyOk || collegeOk) {
                loadMiniProfile()
                Log.d("Dev:LoginViewModel", "[selectUser] Success for user: ${user.username}")
                onAutoLoginSuccess()
            } else {
                Log.d("Dev:LoginViewModel", "[selectUser] Failed for user: ${user.username}")
                onAutoLoginFailed?.invoke()
            }
        }
    }

    fun logout() {
        (okHttpClient.cookieJar as? MyCookieJar)?.clearCookies()
        Log.d("Dev:Login", "Logout")
        _username.value = ""
        _password.value = ""
        authPreferences.clearCredentials()
        setTriedAutoLogin(true)
    }

    // Загрузка расписания из БД для пользователя (без логина)
    fun loadScheduleFromDbForUser(username: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val scheduleDao = DatabaseProvider.getDatabase(context).scheduleDao()
            val scheduleEntity = scheduleDao.getSchedule(username, 0)
            val schedule = scheduleEntity?.scheduleJson?.let { com.google.gson.Gson().fromJson(it, com.example.omniclient.api.ScheduleResponse::class.java) }
            withContext(Dispatchers.Main) {
                _schedule.value = schedule
            }
        }
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
