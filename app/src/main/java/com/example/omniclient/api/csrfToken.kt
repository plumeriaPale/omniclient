package com.example.omniclient.api

import android.util.Log
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

fun initializeCsrfToken(): String? {
    val cookies = (okHttpClient.cookieJar as MyCookieJar).loadForRequest("https://omni.top-academy.ru".toHttpUrlOrNull()!!)

    val csrfToken = cookies.find { it.name == "_csrf" }?.value
    Log.d("Dev:CSRF","Полученный CSRF-токен: $csrfToken")

    return csrfToken
}