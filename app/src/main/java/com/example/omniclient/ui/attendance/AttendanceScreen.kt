package com.example.omniclient.ui.attendance

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.omniclient.data.AttendanceRepository
import com.example.omniclient.api.AcademyClient
import com.example.omniclient.api.CollegeClient
import com.example.omniclient.components.TopAppBarComponent
import com.example.omniclient.viewmodels.LoginViewModel
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import android.util.Log
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Canvas
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.tooling.preview.Preview

// --- Data classes ---
data class PresentsResponse(
    val cur_lenta: Int,
    val students: List<PresentStudent>
)
data class PresentStudent(
    val id_stud: String,
    val fio_stud: String,
    val photo_pas: String?,
    val was: Int?,
    val mark2: Int?,
    val mark4: Int?,
    val prize: Int?,
    val group: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    navController: NavController,
    lenta: String,
    divisionId: Int,
    loginViewModel: LoginViewModel
) {
    val context = LocalContext.current
    var students by remember { mutableStateOf<List<PresentStudent>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val attendanceRepository = remember {
        AttendanceRepository(
            academyClient = AcademyClient,
            collegeClient = CollegeClient
        )
    }

    LaunchedEffect(lenta) {
        isLoading = true
        errorText = null
        try {
            val division = divisionId
            Log.d("Dev: Attendance", "divisionId: ${divisionId}")
            val studentsList = attendanceRepository.getPresents(division, mapOf("lenta" to lenta.toInt()))
            students = studentsList ?: emptyList()
            if (studentsList == null) {
                errorText = "Ошибка загрузки присутствующих"
            }
        } catch (e: Exception) {
            errorText = "Ошибка: ${e.message}"
            Log.d("Dev: Attendance", "Ошибка: ${e.message}")
        }
        isLoading = false
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
            when {
                isLoading -> CircularProgressIndicator(color = Color(0xFFDB173F), modifier = Modifier.align(Alignment.Center))
                errorText != null -> Text(errorText!!, color = Color.Red, modifier = Modifier.align(Alignment.Center))
                else -> Column(Modifier.fillMaxSize().padding(8.dp)) {
                    // --- Тема урока и кнопки ---
                    var lessonTheme by remember { mutableStateOf("") }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = lessonTheme,
                            onValueChange = { lessonTheme = it },
                            label = { Text("Тема урока") },
                            modifier = Modifier
                                .weight(1f)
                                .height(92.dp),
                            singleLine = false,
                            minLines = 2,
                            maxLines = 4
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.width(140.dp).padding(top = 8.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Button(
                                onClick = { /* TODO: Сохранить тему */ },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDB173F)),
                                shape = RoundedCornerShape(0.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Сохранить", color = Color.White)
                            }
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = { /* TODO: Добавить материал */ },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDB173F)),
                                shape = RoundedCornerShape(0.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Добавить материал", color = Color.White)
                            }
                        }
                    }
                    // --- Список студентов ---
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(students) { idx, stud ->
                            StudentCard(stud, idx + 1)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentCard(stud: PresentStudent, number: Int) {
    val context = LocalContext.current
    var mark2Expanded by remember { mutableStateOf(false) }
    var mark4Expanded by remember { mutableStateOf(false) }
    var mark2 by remember { mutableStateOf(stud.mark2) }
    var mark4 by remember { mutableStateOf(stud.mark4) }
    var prize by remember { mutableStateOf(stud.prize ?: 0) }
    var was by remember { mutableStateOf(stud.was ?: -1) } // -1 = не выбран

    LaunchedEffect(Unit) {
        Log.d("Dev: Attendance", "Студент: $stud")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .heightIn(min = 180.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Фото и номер
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(stud.photo_pas)
                            .decoderFactory(SvgDecoder.Factory())
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(stud.fio_stud, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Color.Black, maxLines = 2)
                    if (!stud.group.isNullOrBlank()) {
                        Text(stud.group, color = Color.Gray, fontSize = 15.sp)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(number.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFDB173F))
            }
            Spacer(Modifier.height(16.dp))
            // Радиокнопки присутствия
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = was == 1,
                    onClick = {
                        was = 1
                        Toast.makeText(context, "Присутствовал", Toast.LENGTH_SHORT).show()
                    },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color(0xFF4CAF50),
                        unselectedColor = Color(0xFF4CAF50),
                        disabledSelectedColor = Color(0xFF4CAF50),
                        disabledUnselectedColor = Color(0xFF4CAF50)
                    )
                )
                Spacer(Modifier.width(24.dp))
                RadioButton(
                    selected = was == 2,
                    onClick = {
                        was = 2
                        Toast.makeText(context, "Опоздал", Toast.LENGTH_SHORT).show()
                    },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color(0xFFFFC107),
                        unselectedColor = Color(0xFFFFC107),
                        disabledSelectedColor = Color(0xFFFFC107),
                        disabledUnselectedColor = Color(0xFFFFC107)
                    )
                )
                Spacer(Modifier.width(24.dp))
                RadioButton(
                    selected = was == 0,
                    onClick = {
                        was = 0
                        Toast.makeText(context, "Отсутствовал", Toast.LENGTH_SHORT).show()
                    },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color(0xFFF44336),
                        unselectedColor = Color(0xFFF44336),
                        disabledSelectedColor = Color(0xFFF44336),
                        disabledUnselectedColor = Color(0xFFF44336)
                    )
                )
            }
            Spacer(Modifier.height(12.dp))
            // Оценки
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(100.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = mark2Expanded,
                        onExpandedChange = { mark2Expanded = !mark2Expanded }
                    ) {
                        TextField(
                            value = mark2?.toString() ?: "-",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("КР") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(mark2Expanded) },
                            modifier = Modifier.menuAnchor().width(100.dp)
                        )
                        DropdownMenu(
                            expanded = mark2Expanded,
                            onDismissRequest = { mark2Expanded = false }
                        ) {
                            (1..12).forEach { value ->
                                DropdownMenuItem(
                                    text = { Text(value.toString()) },
                                    onClick = {
                                        mark2 = value
                                        mark2Expanded = false
                                        Toast.makeText(context, "Оценка КР: $value", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(24.dp))
                Box(Modifier.width(100.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = mark4Expanded,
                        onExpandedChange = { mark4Expanded = !mark4Expanded }
                    ) {
                        TextField(
                            value = mark4?.toString() ?: "-",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Класс") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(mark4Expanded) },
                            modifier = Modifier.menuAnchor().width(100.dp)
                        )
                        DropdownMenu(
                            expanded = mark4Expanded,
                            onDismissRequest = { mark4Expanded = false }
                        ) {
                            (1..12).forEach { value ->
                                DropdownMenuItem(
                                    text = { Text(value.toString()) },
                                    onClick = {
                                        mark4 = value
                                        mark4Expanded = false
                                        Toast.makeText(context, "Оценка класс: $value", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            // Монетки и отзыв
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Монетки:", fontWeight = FontWeight.Medium, fontSize = 15.sp, color = Color(0xFFDB173F))
                Spacer(Modifier.width(8.dp))
                (1..3).forEach { idx ->
                    IconButton(onClick = {
                        prize = idx
                        Toast.makeText(context, "Монеток: $idx", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            painterResource(android.R.drawable.star_on),
                            contentDescription = "Монетка $idx",
                            tint = if (prize >= idx) Color(0xFFFFD600) else Color.LightGray
                        )
                    }
                }
                Spacer(Modifier.width(24.dp))
                IconButton(onClick = { Toast.makeText(context, "Оставить отзыв", Toast.LENGTH_SHORT).show() }) {
                    Icon(painterResource(android.R.drawable.ic_menu_edit), contentDescription = "Отзыв", tint = Color(0xFFDB173F))
                }
            }
        }
    }
}