package com.example.omniclient.api

import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class MyCookieJar : CookieJar {
    private val cookies = mutableMapOf<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val hostCookies = this.cookies.getOrPut(url.host) { mutableListOf() }

        cookies.forEach { newCookie ->
            hostCookies.removeIf { it.name == newCookie.name }
            hostCookies.add(newCookie)
            Log.d("Dev:Cookie", "${newCookie.name} cookie saved: ${newCookie.value}")
        }

        this.cookies[url.host] = hostCookies
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookies[url.host] ?: emptyList()
    }

    fun clearCookies() {
        cookies.clear()
    }
}