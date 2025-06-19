package com.example.omniclient.ui.ReviewsScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.omniclient.data.Student

@Composable
fun ReviewCard(
    student: Student,
    divisionId: Int,
    onSendReview: (Student, String) -> Unit,
    isInQueue: Boolean
) {
    var commentText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Заголовок с ФИО
            Text(
                text = student.fio_stud.trim(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Информация о студенте
            StudentInfoRow("Группа", student.name_tgroups.trim())
            StudentInfoRow("Поток", student.name_streams.trim())
            StudentInfoRow("Специализация", student.dir_name.trim())
            StudentInfoRow("Предмет", student.name_spec.trim())

            // Поле для ввода отзыва
            TextField(
                value = commentText,
                onValueChange = { commentText = it },
                label = { Text("Ваш отзыв") },
                placeholder = { Text("Введите ваш отзыв здесь...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                enabled = !isInQueue,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFFDB173F),
                    unfocusedIndicatorColor = Color.LightGray
                )
            )

            // Кнопка отправки
            Button(
                onClick = { onSendReview(student, commentText) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                enabled = commentText.isNotBlank() && !isInQueue,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDB173F),
                    contentColor = Color.White
                )
            ) {
                if (isInQueue) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Отправка...")
                    }
                } else {
                    Text("Отправить отзыв")
                }
            }
        }
    }
}

@Composable
fun StudentInfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(vertical = 4.dp)) {
        Text(
            text = "$label: ",
            fontWeight = FontWeight.SemiBold,
            color = Color.Gray
        )
        Text(
            text = value,
            color = Color.Black
        )
    }
}