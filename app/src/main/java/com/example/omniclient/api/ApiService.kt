package com.example.omniclient.api

import android.util.Log
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

