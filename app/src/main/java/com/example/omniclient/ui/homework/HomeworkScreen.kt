package com.example.omniclient.ui.homework

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRowDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.omniclient.api.apiService
import com.example.omniclient.components.TopAppBarComponent
import com.example.omniclient.ui.schedule.changeCity
import com.example.omniclient.viewmodels.LoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import android.widget.Toast
import com.example.omniclient.api.MyCookieJar
import com.example.omniclient.api.okHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import com.example.omniclient.ui.homework.HomeworkSendQueue

// --- data class для парса ответа API ---
data class HomeworkApiResponse(val homework: List<Homework>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeworkScreen(
    navController: NavController,
    loginViewModel: LoginViewModel,
    openDrawer: (() -> Unit)? = null
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val tabs = listOf("Колледж", "Академия")

    // Состояния для данных и загрузки
    var collegeHomework by remember { mutableStateOf<List<Homework>?>(null) }
    var academyHomework by remember { mutableStateOf<List<Homework>?>(null) }
    var isCollegeLoading by remember { mutableStateOf(true) }
    var isAcademyLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // Оценки для каждой карточки (id -> mark)
    val collegeMarks = remember { mutableStateMapOf<String, Int>() }
    val academyMarks = remember { mutableStateMapOf<String, Int>() }

    // Загрузка данных при открытии экрана
    LaunchedEffect(Unit) {
        // Колледж
        isCollegeLoading = true
        errorText = null
        val collegeLoaded = withContext(Dispatchers.IO) {
            try {
                changeCity(458)
                val response = apiService.getNewHomeworks(
                    mapOf(
                        "id_tgroups" to "33",
                        "id_spec" to "56",
                        "limit" to 10,
                        "offset" to 0,
                        "type" to 0,
                        "transferred" to false,
                        "year" to "",
                        "month" to ""
                    )
                )
                if (response.isSuccessful) {
                    val body = response.body()?.string()
                    val homeworkList = body?.let { Gson().fromJson(it, HomeworkApiResponse::class.java).homework }
                    homeworkList
                } else null
            } catch (e: Exception) {
                errorText = "Ошибка загрузки колледжа: ${e.message}"
                null
            }
        }
        collegeHomework = collegeLoaded
        isCollegeLoading = false

        // Академия
        isAcademyLoading = true
        val academyLoaded = withContext(Dispatchers.IO) {
            try {
                changeCity(74)
                val response = apiService.getNewHomeworks(
                    mapOf(
                        "id_tgroups" to "33",
                        "id_spec" to "56",
                        "limit" to 10,
                        "offset" to 0,
                        "type" to 0,
                        "transferred" to false,
                        "year" to "",
                        "month" to ""
                    )
                )
                if (response.isSuccessful) {
                    val body = response.body()?.string()
                    val homeworkList = body?.let { Gson().fromJson(it, HomeworkApiResponse::class.java).homework }
                    homeworkList
                } else null
            } catch (e: Exception) {
                errorText = "Ошибка загрузки академии: ${e.message}"
                null
            }
        }
        academyHomework = academyLoaded
        isAcademyLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBarComponent(
                title = "Домашние задания",
                onLogoutClick = {
                    loginViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                navController = navController,
                onMenuClick = openDrawer
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                contentColor = Color(0xFFDB173F),
                indicator = { tabPositions: List<TabPosition> ->
                    androidx.compose.material3.TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        height = 2.dp,
                        color = Color(0xFFDB173F)
                    )
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            if (pagerState.currentPage != index) {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                        },
                        selectedContentColor = Color(0xFFDB173F),
                        unselectedContentColor = Color.Black,
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 16.sp
                            )
                        }
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> { // Колледж
                        if (isCollegeLoading) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFFDB173F))
                            }
                        } else if (collegeHomework != null) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                verticalArrangement = Arrangement.Top
                            ) {
                                items(collegeHomework!!, key = { it.id }) { hw ->
                                    var isSending by remember { mutableStateOf(false) }
                                    HomeworkCard(
                                        homework = hw,
                                        marks = (1..5).toList(),
                                        selectedMark = collegeMarks[hw.id],
                                        onMarkSelected = { mark -> collegeMarks[hw.id] = mark },
                                        onSave = { mark, comment ->
                                            if (isSending) return@HomeworkCard
                                            isSending = true
                                            collegeHomework = collegeHomework!!.filter { it.id != hw.id }
                                            HomeworkSendQueue.enqueue(
                                                HomeworkSendQueue.HomeworkSendTask(hw, mark, comment, 458),
                                                apiService = apiService,
                                                changeCity = { division -> changeCity(division) }
                                            )
                                            isSending = false
                                        }
                                    )
                                }
                            }
                        } else if (errorText != null) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(errorText!!, color = Color.Red)
                            }
                        }
                    }
                    1 -> { // Академия
                        if (isAcademyLoading) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFFDB173F))
                            }
                        } else if (academyHomework != null) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                verticalArrangement = Arrangement.Top
                            ) {
                                items(academyHomework!!, key = { it.id }) { hw ->
                                    var isSending by remember { mutableStateOf(false) }
                                    HomeworkCard(
                                        homework = hw,
                                        marks = (1..12).toList(),
                                        selectedMark = academyMarks[hw.id],
                                        onMarkSelected = { mark -> academyMarks[hw.id] = mark },
                                        onSave = { mark, comment ->
                                            if (isSending) return@HomeworkCard
                                            isSending = true
                                            // Сразу убираем карточку из списка
                                            academyHomework = academyHomework!!.filter { it.id != hw.id }
                                            // Кладём задачу в очередь на отправку
                                            HomeworkSendQueue.enqueue(
                                                HomeworkSendQueue.HomeworkSendTask(hw, mark, comment, 74),
                                                apiService = apiService,
                                                changeCity = { division -> changeCity(division) }
                                            )
                                            isSending = false
                                        }
                                    )
                                }
                            }
                        } else if (errorText != null) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(errorText!!, color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun generateSaveHomeworkBody(hw: Homework, mark: Int?, comment: String?): Map<String, Any?> {
    val now = java.util.Calendar.getInstance()
    val key = "${now.get(java.util.Calendar.SECOND)}${now.get(java.util.Calendar.MILLISECOND)}"
    return mapOf(
        "id_domzadstud" to hw.id,
        "filename" to hw.st_filename,
        "id_teach" to "56",
        "id_stud" to hw.id_stud,
        "time" to hw.stud_date,
        "id_domzad" to hw.id_domzad,
        "ospr" to hw.ospr,
        "bad_dz" to hw.bad_dz,
        "coment" to comment.takeIf { !it.isNullOrBlank() },
        "fake_dz" to "0",
        "mark" to mark?.toString(),
        "nlenta" to hw.lenta,
        "date_vizit" to hw.teach_date,
        "tmp_file" to hw.tmp_file,
        "is_retake" to "0",
        "is_system_delete" to "0",
        "assessment_without_homework" to "0",
        "id_domzad_teach" to hw.teach_Dz,
        "stud_stud" to hw.id_stud,
        "comment_attach" to null,
        "comment_attach_file" to null,
        "disabled" to hw.disabled,
        "automark" to "0",
        "answer_text" to hw.answer_text,
        "theme" to hw.theme,
        "fio_stud" to hw.fio_stud,
        "group" to hw.group,
        "dzs_answer_status" to hw.dzs_answer_status,
        "teach_filename" to hw.file_teach,
        "download_url" to hw.download_url,
        "download_url_stud" to hw.download_url_stud,
        "status" to 2,
        "old_comment" to null,
        "marks" to mapOf(
            key to mapOf(
                "id" to hw.id_domzad,
                "mark" to mark?.toString(),
                "ospr" to hw.ospr,
                "stud" to hw.id_stud
            )
        )
    )
}