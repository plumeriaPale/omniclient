package com.example.omniclient.api

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import kotlinx.coroutines.*
import okio.Buffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import retrofit2.http.Path

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ResponseBody>

    @GET("auth/change-city")
    suspend fun changeCity(@Query("city") cityId: Int): Response<ResponseBody>

    @POST("schedule/get-schedule")
    @Headers(
        "Content-Type: application/json;charset=UTF-8",
        "Accept: application/json",
        "X-Requested-With: XMLHttpRequest"
    )
    suspend fun getSchedule(
        @Header("X-CSRF-Token") csrfToken: String,
        @Body request: ScheduleRequest
    ): Response<ScheduleResponse>

    @POST("homework/get-new-homeworks")
    @Headers(
        "Content-Type: application/json;charset=UTF-8",
        "Accept: application/json",
        "X-Requested-With: XMLHttpRequest"
    )
    suspend fun getNewHomeworks(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): retrofit2.Response<okhttp3.ResponseBody>

    @POST("homework/save-homework")
    @Headers(
        "Content-Type: application/json;charset=UTF-8",
        "Accept: application/json",
        "X-Requested-With: XMLHttpRequest"
    )
    suspend fun saveHomework(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): retrofit2.Response<okhttp3.ResponseBody>

    @POST("presents/get-presents")
    @Headers(
        "Content-Type: application/json;charset=UTF-8",
        "Accept: application/json",
        "X-Requested-With: XMLHttpRequest"
    )
    suspend fun getPresents(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): retrofit2.Response<okhttp3.ResponseBody>

    @POST("profile/get-profile")
    @Headers(
        "Content-Type: application/json;charset=UTF-8",
        "Accept: application/json",
        "X-Requested-With: XMLHttpRequest"
    )
    suspend fun getProfile(): Response<ProfileResponse>
}

interface SmartLoginApiService : ApiService {
    suspend fun loginWithCity(request: LoginRequest): Response<ResponseBody>
}

class AcademyClientImpl(val delegate: ApiService) : SmartLoginApiService, ApiService by delegate {
    override suspend fun loginWithCity(request: LoginRequest): Response<ResponseBody> {
        val loginResp = delegate.login(request)
        if (loginResp.isSuccessful) {
            delegate.changeCity(74)
        }
        return loginResp
    }
}

class CollegeClientImpl(val delegate: ApiService) : SmartLoginApiService, ApiService by delegate {
    override suspend fun loginWithCity(request: LoginRequest): Response<ResponseBody> {
        val loginResp = delegate.login(request)
        if (loginResp.isSuccessful) {
            delegate.changeCity(458)
        }
        return loginResp
    }
}

val loggingInterceptor = HttpLoggingInterceptor { message ->
    //Log.d("Dev:ApiService", message)
}.apply {
    level = HttpLoggingInterceptor.Level.BODY
}

val okHttpClient = OkHttpClient.Builder()
    .cookieJar(MyCookieJar())
    .addInterceptor(loggingInterceptor)
    .build()

val retrofit = Retrofit.Builder()
    .baseUrl("https://omni.top-academy.ru/")
    .client(okHttpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val apiService = retrofit.create(ApiService::class.java)

val academyCookieJar = MyCookieJar()
val collegeCookieJar = MyCookieJar()

private fun buildApiServiceWithUserAgent(userAgent: String, cookieJar: okhttp3.CookieJar, cookieTag: String): ApiService {
    val customClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(loggingInterceptor)
        .addInterceptor(Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", userAgent)
                .build()
            // Логируем сырой запрос
            val buffer = Buffer()
            val requestBody = request.body
            var bodyString = ""
            if (requestBody != null) {
                requestBody.writeTo(buffer)
                val charset: Charset = requestBody.contentType()?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
                bodyString = buffer.readString(charset)
            }
            val rawRequest = buildString {
                append("---- $cookieTag RAW REQUEST ----\n")
                append("${request.method} ${request.url}\n")
                request.headers.forEach { append("${it.first}: ${it.second}\n") }
                if (bodyString.isNotEmpty()) {
                    append("\n$bodyString\n")
                }
                append("-----------------------------")
            }
            //Log.d("Dev:$cookieTag-RAW", rawRequest)
            try {
                return@Interceptor chain.proceed(request)
            } catch (e: Exception) {
                Log.e("ApiInterceptor", "Network error: ${e.message}", e)
                return@Interceptor okhttp3.Response.Builder()
                    .request(request)
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(503)
                    .message("Network error: ${e.message}")
                    .body(okhttp3.ResponseBody.create(null, ""))
                    .build()
            }
        })
        .build()
    return Retrofit.Builder()
        .baseUrl("https://omni.top-academy.ru/")
        .client(customClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)
}

val AcademyClient: SmartLoginApiService = AcademyClientImpl(buildApiServiceWithUserAgent("Mozilla/5.0 (rv:68.0) Gecko/68.0 Firefox/68.0", academyCookieJar, "AcademyClient"))
val CollegeClient: SmartLoginApiService = CollegeClientImpl(buildApiServiceWithUserAgent("Opera/9.80 (X11; Linux x86_64; U; en) Presto/2.12.388 Version/12.18", collegeCookieJar, "CollegeClient"))

data class ProfileResponse(
    val teach_info: TeachInfo?
)

data class TeachInfo(
    val fio_teach: String?,
    val photo_pas: String?
)

