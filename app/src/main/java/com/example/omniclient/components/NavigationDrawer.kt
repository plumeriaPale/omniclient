package com.example.omniclient.components

import android.gesture.Gesture
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHost
import com.example.omniclient.data.db.UserEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationDrawer(
    enableGesture: Boolean,
    drawerState: DrawerState,
    users: List<UserEntity> = emptyList(),
    currentUsername: String = "",
    onUserSelected: ((UserEntity) -> Unit)? = null,
    onAddUser: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = enableGesture,
        drawerContent = {
            ModalDrawerSheet {
                if (users.isNotEmpty() && onUserSelected != null && onAddUser != null) {
                    var expanded by remember { mutableStateOf(false) }
                    val filteredUsers = users.filter { it.username != currentUsername }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        TextField(
                            value = currentUsername,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Пользователь") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.menuAnchor().padding(16.dp)
                        )
                        ExposedDropdownMenu(
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
                                }
                            )
                        }
                    }
                }
                Text("Раздел 1", modifier = Modifier.padding(16.dp))
                Text("Раздел 2", modifier = Modifier.padding(16.dp))
                Text("Раздел 3", modifier = Modifier.padding(16.dp))
            }
        }
    ) {
        content()
    }
}