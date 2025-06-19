package com.example.omniclient.data

import com.example.omniclient.api.ApiService
import com.example.omniclient.api.SmartLoginApiService
import com.google.gson.Gson
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException

class NotDoneTasksRepository(
    private val academyClient: SmartLoginApiService,
    private val collegeClient: SmartLoginApiService
) {
    private val gson = Gson()

    suspend fun getNotDoneTasks(divisionId: Int): NotDoneTasksData? {
        return try {
            val response = when (divisionId) {
                74 -> academyClient.getNotDoneTasks()
                458 -> collegeClient.getNotDoneTasks()
                else -> null
            } ?: return null

            parseResponse(response)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseResponse(response: Response<ResponseBody>): NotDoneTasksData? {
        if (!response.isSuccessful) return null
        return try {
            val bodyString = response.body()?.string() ?: return null
            val apiResponse = gson.fromJson(bodyString, NotDoneTasksResponse::class.java)

            NotDoneTasksData(
                newHomework = apiResponse.dzStud.new_home_work,
                reviewsData = apiResponse.reviewsStud,
                notDoneTasks = apiResponse.counterNodDoneTask
            )
        } catch (e: IOException) {
            null
        } catch (e: Exception) {
            null
        }
    }
}

data class NotDoneTasksResponse(
    val dzStud: DzStud,
    val reviewsStud: ReviewsStud,
    val counterNodDoneTask: Int
)

data class DzStud(
    val new_home_work: Int
)

data class ReviewsStud(
    val count_students: Int,
    val students_list: List<Student>
)

data class Student(
    val id_stud: String,
    val id_teach: String,
    val id_form: String,
    val date_last_comments: String? = null,
    val name_spec: String,
    val id_spec: String,
    val id_streams: String,
    val id_dir: String,
    val dir_name: String,
    val name_streams: String,
    val fio_stud: String,
    val name_tgroups: String
)

// Для удобного использования
data class NotDoneTasksData(
    val newHomework: Int,
    val reviewsData: ReviewsStud,
    val notDoneTasks: Int
)

