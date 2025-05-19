package com.example.omniclient.components

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun TopAppBarComponent(
    title: String,
    onLogoutClick: () -> Unit,
    navController: NavController,
    onMenuClick: (() -> Unit)? = null
) {
    TopAppBar(
        title = { Text(text = title) },
        backgroundColor = Color(0xFFFFF8F8),
        contentColor = Color(0xFFFFF8F8),
        elevation = 0.dp,
        navigationIcon = {
            if (onMenuClick != null) {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Filled.Menu, contentDescription = "Меню", tint = Color(0xFFDB173F))
                }
            } else {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color(0xFFDB173F))
                }
            }
        },
        actions = {
            IconButton(onClick = onLogoutClick) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Выход", tint = Color(0xFFDB173F))
            }
        },
        modifier = Modifier.statusBarsPadding()
    )
}