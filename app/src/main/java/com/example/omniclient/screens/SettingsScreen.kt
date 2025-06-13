package com.example.omniclient.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.omniclient.components.TopAppBarComponent
import com.example.omniclient.data.db.DatabaseProvider
import com.example.omniclient.data.db.UserDao
import com.example.omniclient.data.db.UserEntity
import com.example.omniclient.viewmodels.UserViewModel
import com.example.omniclient.viewmodels.UserViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    outerNavController: NavController,
    openDrawer: (() -> Unit)? = null,
    userDao: UserDao
) {
    val context = LocalContext.current
    val navController = rememberNavController()

    Scaffold(
        topBar = {
            TopAppBarComponent(
                title = "Настройки",
                navController = navController,
                onMenuClick = openDrawer
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Переносим NavHost сюда - он будет единственным источником контента
            NavHost(
                navController = navController,
                startDestination = "main_settings",
                modifier = Modifier.fillMaxSize()
            ) {
                composable("main_settings") {
                    MainSettingsScreen(navController)
                }
                composable("user_settings") {
                    UserSettingsScreen(navController, userDao = userDao)
                }
            }
        }
    }
}

@Composable
fun MainSettingsScreen(navController: NavController) {
    var darkThemeEnabled by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Группа "Общие"
        item { SettingsHeader(title = "Общие") }
        item {
            ClickableSetting(
                title = "Пользователи",
                onClick = { navController.navigate("user_settings") }
            )
        }
    }
}

@Composable
fun UserSettingsScreen(
    navController: NavController,
    userDao: UserDao
) {
    val viewModel: UserViewModel = viewModel(
        factory = UserViewModelFactory(userDao)
    )

    val users by viewModel.users.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newUsername by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Color.White,
            title = { Text("Добавить пользователя") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text("Логин") }
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Пароль") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addUser(newUsername, newPassword)
                        newUsername = ""
                        newPassword = ""
                        showAddDialog = false
                    },
                    enabled = newUsername.isNotBlank() && newPassword.isNotBlank()
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { SettingsHeader(title = "Пользователи") }

        items(users.size) { index ->
            UserItem(
                user = users[index],
                onDelete = { viewModel.deleteUser(users[index]) }
            )
        }

        item {
            ClickableSetting(
                title = "Добавить пользователя",
                onClick = { showAddDialog = true }
            )
        }
    }
}

@Composable
fun UserItem(
    user: UserEntity,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = user.username, style = MaterialTheme.typography.bodyLarge)

        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Удалить")
        }
    }
}

@Composable
fun ClickableSetting(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}

@Composable
fun SwitchSetting(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.primary
    )
}