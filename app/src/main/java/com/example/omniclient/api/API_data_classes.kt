package com.example.omniclient.api

data class LoginRequest(
    val LoginForm: LoginFormData
)

data class LoginFormData(
    val id_city: Int? = null,
    val username: String,
    val password: String
)

data class ScheduleRequest(
    val week: Int
)

data class ScheduleResponse(
    val body: Map<String, Map<String, Lesson>>,
    val lents: List<Map<String, Lent>>,
    val days: Map<String, String>,
    val daysShort: Map<String, String>,
    val dates: Map<String, String>,
    val curdate: String,
    val start_end: StartEnd
)

data class Lesson(
    val lenta: String,
    val weekday: String,
    val groups: String,
    val num_rooms: String,
    val name_spec: String,
    val id_form: String,
    val l_start: String,
    val l_end: String,
    val lessons_count: String,
    val activity_type_id: String,
    val entity_id: String,
    val scheduleType: String,
    val eventName: String?,
    val eventSubtypeName: String?
)

data class Lent(
    val n_lenta: String,
    val n_day: String,
    val l_start: String,
    val l_end: String
)

data class StartEnd(
    val monday: String,
    val sunday: String
)