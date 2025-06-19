package com.example.omniclient.ui.ReviewsScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.omniclient.components.TopAppBarComponent
import com.example.omniclient.screens.MainSettingsScreen
import com.example.omniclient.screens.UserSettingsScreen
import com.example.omniclient.ui.homework.TypingDots
import com.example.omniclient.viewmodels.LoginViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsScreen(
    navController: NavController,
    openDrawer: () -> Unit,
    loginViewModel: LoginViewModel
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val scope = rememberCoroutineScope()

    // Объявляем список вкладок
    val tabs = listOf("Колледж", "Академия")

    // Получаем данные о студентах через StateFlow
    val academyReviews by loginViewModel.academyReviews.collectAsState()
    val collegeReviews by loginViewModel.collegeReviews.collectAsState()

    // Получаем статус очереди
    val collegeQueueCount by ReviewSendQueue.collegeQueueCount.collectAsState()
    val academyQueueCount by ReviewSendQueue.academyQueueCount.collectAsState()

    Scaffold(
        topBar = {
            TopAppBarComponent(
                title = "Отзывы",
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
                    TabRowDefaults.SecondaryIndicator(
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
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        selectedContentColor = Color(0xFFDB173F),
                        unselectedContentColor = Color.Black,
                        text = {
                            Box(
                                Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                val count = when (index) {
                                    0 -> collegeReviews?.size ?: 0
                                    1 -> academyReviews?.size ?: 0
                                    else -> 0
                                }
                                val queueCount = when (index) {
                                    0 -> collegeQueueCount
                                    1 -> academyQueueCount
                                    else -> 0
                                }

                                val text = if (index == 0) "Колледж" else "Академия"
                                val totalText = if (count > 0 || queueCount > 0) {
                                    "$text (${count + queueCount})"
                                } else {
                                    text
                                }

                                Text(
                                    text = totalText,
                                    fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (queueCount > 0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.align(Alignment.CenterEnd)
                                    ) {
                                        Text(
                                            text = queueCount.toString(),
                                            color = Color(0xFFDB173F),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        TypingDots(color = Color(0xFFDB173F), dotSize = 5, space = 2)
                                    }
                                }
                            }
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
                        if (collegeReviews == null) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFFDB173F))
                            }
                        } else if (collegeReviews!!.isEmpty() && collegeQueueCount == 0) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    "Нет студентов для отзыва в колледже",
                                    color = Color.Gray
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp)
                            ) {
                                // Показываем студентов в очереди
                                items(ReviewSendQueue.queue.toList()
                                    .filter { it.first.divisionId == 458 }
                                    .map { it.first.student }
                                ) { student ->
                                    ReviewCard(
                                        student = student,
                                        divisionId = 458,
                                        onSendReview = { _, _ -> },
                                        isInQueue = true
                                    )
                                }

                                // Показываем остальных студентов
                                items(collegeReviews!!.filter { student ->
                                    ReviewSendQueue.queue.none {
                                        it.first.divisionId == 458 &&
                                                it.first.student.id_stud == student.id_stud
                                    }
                                }) { student ->
                                    ReviewCard(
                                        student = student,
                                        divisionId = 458,
                                        onSendReview = { student, comment ->
                                            loginViewModel.sendReview(student, comment, 458)
                                        },
                                        isInQueue = false
                                    )
                                }
                            }
                        }
                    }
                    1 -> { // Академия
                        if (academyReviews == null) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFFDB173F))
                            }
                        } else if (academyReviews!!.isEmpty() && academyQueueCount == 0) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    "Нет студентов для отзыва в академии",
                                    color = Color.Gray
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp)
                            ) {
                                // Показываем студентов в очереди
                                items(ReviewSendQueue.queue.toList()
                                    .filter { it.first.divisionId == 74 }
                                    .map { it.first.student }
                                ) { student ->
                                    ReviewCard(
                                        student = student,
                                        divisionId = 74,
                                        onSendReview = { _, _ -> },
                                        isInQueue = true
                                    )
                                }

                                // Показываем остальных студентов
                                items(academyReviews!!.filter { student ->
                                    ReviewSendQueue.queue.none {
                                        it.first.divisionId == 74 &&
                                                it.first.student.id_stud == student.id_stud
                                    }
                                }) { student ->
                                    ReviewCard(
                                        student = student,
                                        divisionId = 74,
                                        onSendReview = { student, comment ->
                                            loginViewModel.sendReview(student, comment, 74)
                                        },
                                        isInQueue = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}