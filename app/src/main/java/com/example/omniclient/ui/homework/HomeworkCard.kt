package com.example.omniclient.ui.homework

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.Button
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.omniclient.icons.DownloadIcon
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler

// data class для дз
// (лучше вынести в отдельный файл, но для примера оставим тут)
data class Homework(
    val fio_stud: String,
    val group: String,
    val theme: String,
    val mark: Int?,
    val id_stud: String,
    val file_teach: String?,
    val teach_date: String,
    val lenta: String,
    val stud_date: String,
    val id: String,
    val teach_Dz: String?,
    val answer_text: String?,
    val download_url: String?,
    val download_url_stud: String?,
    val ospr: String?,
    val bad_dz: String?,
    val coment: String?,
    val disabled: String?,
    val tmp_file: String?,
    val id_domzad: String?,
    val st_filename: String?,
    val dzs_answer_status: String?
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeworkCard(
    homework: Homework,
    marks: List<Int>,
    selectedMark: Int?,
    onMarkSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onSave: (mark: Int?, comment: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var comment by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        val uriHandler = LocalUriHandler.current
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = homework.fio_stud,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(${homework.group})",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Тема:", fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = homework.theme, color = Color.Black)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Задание преподавателя:", fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                if (homework.download_url != null) {
                    IconButton(onClick = { uriHandler.openUri(homework.download_url)   }) {
                        Icon(
                            imageVector = DownloadIcon,
                            contentDescription = "Скачать задание",
                            tint = Color(0xFFDB173F)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Дата выдачи:", fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = homework.teach_date, color = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = homework.lenta + " пара", color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "ДЗ от студента:", fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                if (homework.download_url_stud != null) {
                    IconButton(onClick = { uriHandler.openUri(homework.download_url_stud)  }) {
                        Icon(
                            imageVector = DownloadIcon,
                            contentDescription = "Скачать ДЗ студента",
                            tint = Color(0xFFDB173F)
                        )
                    }
                }
            }
            if (homework.stud_date.isNotEmpty()) {
                Text(text = "Дата загрузки: ", fontWeight = FontWeight.Bold, color = Color.Black)
                Text(text = homework.stud_date, color = Color.Gray)
            }
            if (!homework.answer_text.isNullOrEmpty()) {
                Text(text = homework.answer_text, color = Color(0xFFDB173F), fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Поставить оценку:", fontWeight = FontWeight.Bold, color = Color.Black)
            FlowRow(
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.Center,
                maxItemsInEachRow = 6,
                modifier = Modifier.padding(top = 4.dp).fillMaxWidth()
            ) {
                marks.forEach { mark ->
                    Box(
                        modifier = Modifier
                            .background(
                                if (selectedMark == mark) Color(0xFFDB173F) else Color(0xFFF5F5F5),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { onMarkSelected(mark) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mark.toString(),
                            color = if (selectedMark == mark) Color.White else Color.Black,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Комментарий:", fontWeight = FontWeight.Bold, color = Color.Black)
            TextField(
                value = comment,
                onValueChange = { comment = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Введите комментарий") },
                singleLine = false,
                maxLines = 4
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    isSaving = true
                    onSave(selectedMark, comment)
                    isSaving = false
                },
                modifier = Modifier.align(Alignment.End),
                enabled = !isSaving
            ) {
                Text(if (isSaving) "Сохраняю..." else "Сохранить")
            }
        }
    }
} 