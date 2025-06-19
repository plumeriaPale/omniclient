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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.launch

// --- Data classes ---
data class PresentsResponse(
    val cur_lenta: Int,
    val students: List<PresentStudent>
)
data class PresentStudent(
    val id_vizit: String,
    val id_stud: String,
    val fio_stud: String,
    val photo_pas: String?,
    val id_rasp: String,
    val was: Int?,
    val mark2: Int?,
    val mark4: Int?,
    val prize: Int?,
    val group: String? = null,
    val theme: String? = null
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
    var lessonInitTheme by remember { mutableStateOf("") }

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
            Log.d("Dev: Attendance", "divisionId: \${divisionId}")
            val studentsList = attendanceRepository.getPresents(division, mapOf("lenta" to lenta.toInt()))
            students = studentsList ?: emptyList()
            if (studentsList != null) {
                lessonInitTheme = if (studentsList.firstOrNull()?.theme == null) "" else studentsList.firstOrNull()?.theme.toString()
            }
            if (studentsList == null) {
                errorText = "Ошибка загрузки присутствующих"
            }
        } catch (e: Exception) {
            errorText = "Ошибка: \${e.message}"
            Log.d("Dev: Attendance", "Ошибка: \${e.message}")
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBarComponent(
                title = "Присутствующие",
                navController = navController
            )
        }
    ) { paddingValues ->
        val coroutineScope = rememberCoroutineScope()
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
                    var lessonTheme by remember { mutableStateOf(lessonInitTheme) }
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
                                onClick = {
                                    // Сохранить тему
                                    val themeToSave = lessonTheme
                                    val lentaInt = lenta.toIntOrNull() ?: 0
                                    val date = java.time.LocalDate.now().toString()
                                    val body = mapOf(
                                        "date" to date,
                                        "lenta" to lentaInt,
                                        "theme" to themeToSave,
                                        "schedule" to students.firstOrNull()?.id_rasp,
                                        "scheduleType" to "lesson",
                                        "teach_type" to 0,
                                    )
                                    coroutineScope.launch {
                                        Log.d("Dev: AttendanceSetTheme", "divisionId: ${divisionId} body: ${body}")
                                        val success = attendanceRepository.setTheme(divisionId, body)
                                        withContext(Dispatchers.Main) {
                                            if (!success)
                                                Toast.makeText(context, "Ошибка сохранения темы", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
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
                            StudentCard(
                                stud = stud,
                                number = idx + 1,
                                divisionId = divisionId,
                                onWasChange = { newWas ->
                                    students = students.toMutableList().also { it[idx] = it[idx].copy(was = newWas) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentCard(
    stud: PresentStudent,
    number: Int,
    divisionId: Int,
    onWasChange: (Int) -> Unit
) {
    val context = LocalContext.current
    var mark2Expanded by remember { mutableStateOf(false) }
    var mark4Expanded by remember { mutableStateOf(false) }
    var mark2 by remember { mutableStateOf(stud.mark2) }
    var mark4 by remember { mutableStateOf(stud.mark4) }
    var prize by remember { mutableStateOf(stud.prize ?: 0) }
    var was by remember { mutableStateOf(stud.was ?: -1) } // -1 = не выбран
    val attendanceRepository = remember {
        AttendanceRepository(
            academyClient = AcademyClient,
            collegeClient = CollegeClient
        )
    }

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
        val coroutineScope = rememberCoroutineScope()
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
                        val now = java.util.Calendar.getInstance()
                        val key = "${now.get(java.util.Calendar.SECOND)}${now.get(java.util.Calendar.MILLISECOND)}"

                        was = 1
                        onWasChange(1)
                        val body: Map<String, Any?> = mapOf(
                            "visits" to mapOf(
                                key to mapOf(
                                    "was" to was,
                                    "vizit" to stud.id_vizit,
                                    "id_stud" to stud.id_stud,
                                    "id_schedule" to stud.id_rasp,
                                    "primary_teach" to "0",
                                    "theme" to stud.theme
                                )
                            ),
                            "schedule" to stud.id_rasp
                        )
                        coroutineScope.launch {
                            val success = attendanceRepository.setWas(divisionId, body)
                            Log.d("Dev: AttendanceStatus", "divisionId: ${divisionId}, body: ${body}")
                            withContext(Dispatchers.Main) {
                                if (!success)
                                    Toast.makeText(context, "Ошибка сохранения посещения", Toast.LENGTH_SHORT).show()
                            }
                        }
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
                        val now = java.util.Calendar.getInstance()
                        val key = "${now.get(java.util.Calendar.SECOND)}${now.get(java.util.Calendar.MILLISECOND)}"

                        was = 2
                        onWasChange(2)
                        val body: Map<String, Any?> = mapOf(
                            "visits" to mapOf(
                                key to mapOf(
                                    "was" to was,
                                    "vizit" to stud.id_vizit,
                                    "id_stud" to stud.id_stud,
                                    "id_schedule" to stud.id_rasp,
                                    "primary_teach" to "0",
                                    "theme" to stud.theme
                                )
                            ),
                            "schedule" to stud.id_rasp
                        )
                        coroutineScope.launch {
                            val success = attendanceRepository.setWas(divisionId, body)
                            Log.d("Dev: AttendanceStatus", "divisionId: ${divisionId}, body: ${body}, success: ${success}")
                            withContext(Dispatchers.Main) {
                                if (!success)
                                    Toast.makeText(context, "Ошибка сохранения посещения", Toast.LENGTH_SHORT).show()
                            }
                        }
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
                        val now = java.util.Calendar.getInstance()
                        val key = "${now.get(java.util.Calendar.SECOND)}${now.get(java.util.Calendar.MILLISECOND)}"

                        was = 0
                        onWasChange(0)
                        val body: Map<String, Any?> = mapOf(
                            "visits" to mapOf(
                                key to mapOf(
                                    "was" to was,
                                    "vizit" to stud.id_vizit,
                                    "id_stud" to stud.id_stud,
                                    "id_schedule" to stud.id_rasp,
                                    "primary_teach" to "0",
                                    "theme" to stud.theme
                                )
                            ),
                            "schedule" to stud.id_rasp
                        )
                        coroutineScope.launch {
                            val success = attendanceRepository.setWas(divisionId, body)
                            withContext(Dispatchers.Main) {
                                if (!success)
                                    Toast.makeText(context, "Ошибка сохранения посещения", Toast.LENGTH_SHORT).show()
                            }
                        }
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
                            modifier = Modifier
                                .background(Color.White)
                                .heightIn(max = 250.dp),
                            expanded = mark2Expanded,
                            onDismissRequest = { mark2Expanded = false }
                        ) {
                            Column(
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .width(IntrinsicSize.Min)
                            ) {
                                if (divisionId == 74)
                                    (1..12).forEach { value ->
                                        DropdownMenuItem(
                                            modifier = Modifier.width(IntrinsicSize.Max),
                                            text = {
                                                Text(
                                                    text = value.toString(),
                                                    modifier = Modifier.padding(horizontal = 16.dp)
                                                )
                                            },
                                            onClick = {
                                                mark2 = value
                                                mark2Expanded = false

                                                val now = java.util.Calendar.getInstance()
                                                val key = "${now.get(java.util.Calendar.SECOND)}${now.get(java.util.Calendar.MILLISECOND)}"

                                                val body: Map<String, Any?> = mapOf(
                                                    "marks" to mapOf(
                                                        key to mapOf(
                                                            "type" to 2,
                                                            "mark" to value,
                                                            "vizit" to stud.id_vizit,
                                                        )
                                                    ),
                                                    "schedule" to stud.id_rasp
                                                )
                                                coroutineScope.launch {
                                                    val success = attendanceRepository.setMark(divisionId, body)
                                                    withContext(Dispatchers.Main) {
                                                        if (!success)
                                                            Toast.makeText(context, "Ошибка сохранения оценки КР", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        )
                                    }
                                if (divisionId == 458)
                                    (1..5).forEach { value ->
                                        DropdownMenuItem(
                                            modifier = Modifier.width(IntrinsicSize.Max),
                                            text = {
                                                Text(
                                                    text = value.toString(),
                                                    modifier = Modifier.padding(horizontal = 16.dp)
                                                )
                                            },
                                            onClick = {
                                                mark2 = value
                                                mark2Expanded = false

                                                val now = java.util.Calendar.getInstance()
                                                val key = "${now.get(java.util.Calendar.SECOND)}${now.get(java.util.Calendar.MILLISECOND)}"

                                                val body: Map<String, Any?> = mapOf(
                                                    "marks" to mapOf(
                                                        key to mapOf(
                                                            "type" to 2,
                                                            "mark" to value,
                                                            "vizit" to stud.id_vizit,
                                                        )
                                                    ),
                                                    "schedule" to stud.id_rasp
                                                )
                                                coroutineScope.launch {
                                                    val success = attendanceRepository.setMark(divisionId, body)
                                                    withContext(Dispatchers.Main) {
                                                        if (!success)
                                                            Toast.makeText(context, "Ошибка сохранения оценки КР", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        )
                                    }
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
                            if (divisionId == 74)
                                (1..12).forEach { value ->
                                    DropdownMenuItem(
                                        modifier = Modifier.background(Color.White),
                                        text = { Text(value.toString()) },
                                        onClick = {
                                            mark4 = value
                                            mark4Expanded = false

                                            val now = java.util.Calendar.getInstance()
                                            val key = "${now.get(java.util.Calendar.SECOND)}${now.get(java.util.Calendar.MILLISECOND)}"

                                            val body: Map<String, Any?> = mapOf(
                                                "marks" to mapOf(
                                                    key to mapOf(
                                                        "type" to 4,
                                                        "mark" to value,
                                                        "vizit" to stud.id_vizit,
                                                    )
                                                ),
                                                "schedule" to stud.id_rasp
                                            )
                                            coroutineScope.launch {
                                                val success = attendanceRepository.setMark(divisionId, body)
                                                withContext(Dispatchers.Main) {
                                                    if (!success)
                                                        Toast.makeText(context, "Ошибка сохранения оценки", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    )
                                }
                            if (divisionId == 458)
                                (1..5).forEach { value ->
                                    DropdownMenuItem(
                                        modifier = Modifier.background(Color.White),
                                        text = { Text(value.toString()) },
                                        onClick = {
                                            mark4 = value
                                            mark4Expanded = false

                                            val now = java.util.Calendar.getInstance()
                                            val key = "${now.get(java.util.Calendar.SECOND)}${now.get(java.util.Calendar.MILLISECOND)}"

                                            val body: Map<String, Any?> = mapOf(
                                                "marks" to mapOf(
                                                    key to mapOf(
                                                        "type" to 4,
                                                        "mark" to value,
                                                        "vizit" to stud.id_vizit,
                                                    )
                                                ),
                                                "schedule" to stud.id_rasp
                                            )
                                            coroutineScope.launch {
                                                val success = attendanceRepository.setMark(divisionId, body)
                                                withContext(Dispatchers.Main) {
                                                    if (!success)
                                                        Toast.makeText(context, "Ошибка сохранения оценки", Toast.LENGTH_SHORT).show()
                                                }
                                            }
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