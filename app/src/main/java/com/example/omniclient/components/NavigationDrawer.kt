package com.example.omniclient.components

import android.gesture.Gesture
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHost
import com.example.omniclient.data.db.UserEntity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationDrawer(
    enableGesture: Boolean,
    drawerState: DrawerState,
    navController: NavController,
    scope: CoroutineScope,
    users: List<UserEntity> = emptyList(),
    currentUsername: String = "",
    onUserSelected: ((UserEntity) -> Unit)? = null,
    onAddUser: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = enableGesture,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Color(0xFFFEF8F8)){
                if (users.isNotEmpty() && onUserSelected != null && onAddUser != null) {
                    var expanded by remember { mutableStateOf(false) }
                    val filteredUsers = users.filter { it.username != currentUsername }
                    ExposedDropdownMenuBox(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        TextField(
                            value = currentUsername,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Пользователь") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            modifier = Modifier.background(Color(0xFFFEF8F8)),
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            filteredUsers.forEach { user ->
                                DropdownMenuItem(
                                    text = { Text(user.username) },
                                    onClick = {
                                        expanded = false
                                        onUserSelected(user)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Добавить пользователя") },
                                onClick = {
                                    expanded = false
                                    onAddUser()
                                },
                            )
                        }
                    }
                }
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = if (currentRoute == "schedule") Color(0xFFDB173F) else Color.Black) },
                    label = { Text("Расписание") },
                    selected = currentRoute == "schedule",
                    onClick = {
                        navController.navigate("schedule") {
                            popUpTo("schedule") { inclusive = true }
                        }
                        scope.launch { drawerState.close() }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0x00FFFFFF),
                        unselectedContainerColor = Color(0x00FFFFFF),
                    ),
                    shape = RectangleShape
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null, tint = if (currentRoute == "homework") Color(0xFFDB173F) else Color.Black) },
                    label = { Text("ДЗ") },
                    selected = currentRoute == "homework",
                    onClick = {
                        navController.navigate("homework") {
                            popUpTo("schedule") { inclusive = true }
                        }
                        scope.launch { drawerState.close() }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0x00FFFFFF),
                        unselectedContainerColor = Color(0x00FFFFFF),
                    ),
                    shape = RectangleShape
                )
            }
        }
    ) {
        content()
    }
}