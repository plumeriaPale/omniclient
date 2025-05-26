package com.example.omniclient.data

import com.example.omniclient.api.ApiService
import com.example.omniclient.ui.homework.Homework
import com.google.gson.Gson

class HomeworkRepository(
    private val academyClient: ApiService,
    private val collegeClient: ApiService
) {
    suspend fun getHomeworks(divisionId: Int, params: Map<String, Any>): List<Homework>? {
        val client = when (divisionId) {
            74 -> academyClient
            458 -> collegeClient
            else -> return null
        }
        val response = client.getNewHomeworks(params)
        if (response.isSuccessful) {
            val body = response.body()?.string()
            return body?.let { Gson().fromJson(it, com.example.omniclient.ui.homework.HomeworkApiResponse::class.java).homework }
        }
        return null
    }

    suspend fun sendHomework(divisionId: Int, body: Map<String, Any?>): Boolean {
        val client = when (divisionId) {
            74 -> academyClient
            458 -> collegeClient
            else -> return false
        }
        val response = client.saveHomework(body)
        return response.isSuccessful
    }
} 