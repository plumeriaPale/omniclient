package com.example.omniclient.ui.attendance

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.omniclient.components.TopAppBarComponent
import com.example.omniclient.viewmodels.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    navController: NavController,
    lenta: String,
    date: String,
    loginViewModel: LoginViewModel
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        Toast.makeText(
            context,
            "Данные карточки: {\"lenta\":\"$lenta\", \"date\":\"$date\"}",
            Toast.LENGTH_LONG
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBarComponent(
                title = "Присутствующие",
                onLogoutClick = {
                    loginViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                navController = navController
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Здесь будет основной контент экрана
        }
    }
} 