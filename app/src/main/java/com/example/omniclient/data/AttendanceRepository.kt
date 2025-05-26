package com.example.omniclient.data

import com.example.omniclient.api.ApiService
import com.example.omniclient.ui.attendance.PresentStudent
import com.example.omniclient.ui.attendance.PresentsResponse
import com.google.gson.Gson

class AttendanceRepository(
    private val academyClient: ApiService,
    private val collegeClient: ApiService
) {
    suspend fun getPresents(divisionId: Int, params: Map<String, Any>): List<PresentStudent>? {
        val client = when (divisionId) {
            74 -> academyClient
            458 -> collegeClient
            else -> return null
        }
        val response = client.getPresents(params)
        if (response.isSuccessful) {
            val body = response.body()?.string()
            val presents = body?.let { Gson().fromJson(it, PresentsResponse::class.java) }
            return presents?.students
        }
        return null
    }
} 