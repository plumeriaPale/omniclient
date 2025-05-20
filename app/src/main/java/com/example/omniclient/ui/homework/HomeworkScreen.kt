package com.example.omniclient.ui.homework

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.omniclient.components.TopAppBarComponent
import com.example.omniclient.viewmodels.LoginViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

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
                    SecondaryIndicator(
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(10) { i ->
                        Text(
                            text = if (page == 0) "ДЗ для колледжа #$i" else "ДЗ для академии #$i",
                            fontSize = 16.sp,
                            color = Color.Black,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
} 