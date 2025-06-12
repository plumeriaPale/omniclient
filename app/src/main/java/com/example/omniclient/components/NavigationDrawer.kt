package com.example.omniclient.components

import android.gesture.Gesture
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHost
import com.example.omniclient.data.db.UserEntity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.TextField
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.omniclient.api.ProfileResponse
import com.example.omniclient.viewmodels.LoginViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext

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
    loginViewModel: LoginViewModel,
    content: @Composable () -> Unit
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val miniProfile by loginViewModel.miniProfile.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = enableGesture,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Color(0xFFFEF8F8)){
                Column(modifier = Modifier.fillMaxHeight()) {
                    // MINI PROFILE
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val photoUrl = miniProfile?.teach_info?.photo_pas
                            val isSvg = photoUrl?.contains("avatarka.svg") == true || photoUrl?.endsWith(".svg") == true
                            if (!photoUrl.isNullOrBlank() && !isSvg) {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                )
                            } else if (!photoUrl.isNullOrBlank() && isSvg) {
                                // SVG-заглушка: Box с инициалом
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE0E0E0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = miniProfile?.teach_info?.fio_teach?.firstOrNull()?.toString() ?: "?",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.DarkGray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE0E0E0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = miniProfile?.teach_info?.fio_teach?.firstOrNull()?.toString() ?: "?",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.DarkGray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = miniProfile?.teach_info?.fio_teach ?: "",
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
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
                    Column(modifier = Modifier.weight(1f)) {
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
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = if (currentRoute == "homework") Color(0xFFDB173F) else Color.Black) },
                            label = { Text("Настройки") },
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
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = if (currentRoute == "homework") Color(0xFFDB173F) else Color.Black) },
                            label = { Text("Профиль") },
                            selected = currentRoute == "homework",
                            onClick = {

                                scope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = Color(0x00FFFFFF),
                                unselectedContainerColor = Color(0x00FFFFFF),
                            ),
                            shape = RectangleShape
                        )
                    }
                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = if (currentRoute == "homework") Color(0xFFDB173F) else Color.Black) },
                        label = { Text("Выйти") },
                        selected = currentRoute == "homework",
                        onClick = {
                            loginViewModel.logout()
                            navController.navigate("login") {
                                popUpTo("login") { inclusive = true }
                            }
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0x00FFFFFF),
                            unselectedContainerColor = Color(0x00FFFFFF),
                        ),
                        shape = RectangleShape,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    ) {
        content()
    }
}
//onLogoutClick