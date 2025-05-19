import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.omniclient.api.Lesson
import com.example.omniclient.data.Division
import com.example.omniclient.ui.schedule.isCurrentTimeWithinLesson

@Composable
fun LessonCard(
    lesson: Lesson,
    isCurrentDay: Boolean,
    onPresentClick: (Lesson) -> Unit,
    onMaterialsClick: (Lesson) -> Unit
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    val isCurrent = isCurrentDay && isCurrentTimeWithinLesson(lesson.l_start, lesson.l_end)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .wrapContentHeight(),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
            contentColor = Color.Black
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .height(4.dp)
                .fillMaxWidth()
                .background(Division.fromId(lesson.divisionId)?.color ?: Color.Gray)
        )
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.Top
        ) {
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(Division.fromId(lesson.divisionId)?.color ?: Color.Gray)
                )
            }
            else{
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(Color.Transparent)
                )
            }

            Column(
                modifier = Modifier.weight(1f).padding(start = 12.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
            ) {
                Text(
                    text = lesson.name_spec,
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(modifier = Modifier.padding(start = 8.dp), text = "Группа: ${lesson.groups}", color = Color.Black, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(modifier = Modifier.padding(start = 8.dp), text = "Аудитория: ${lesson.num_rooms}", color = Color.Black, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(modifier = Modifier.padding(start = 8.dp), text = "${lesson.l_start} - ${lesson.l_end}", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Box(modifier = Modifier.padding(16.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(24.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = lesson.lenta,
                        fontSize = 24.sp,
                        color = Color(0xFFDB173F).copy(alpha = 0.1f),
                        fontWeight = FontWeight.Normal
                    )
                }

                Box {
                    IconButton(
                        onClick = { isMenuExpanded = true },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Меню",
                            tint = Color.Black
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        modifier = Modifier
                            .background(Color.White)
                            .border(
                                width = 1.dp,
                                color = Color.LightGray,
                                shape = RoundedCornerShape(4.dp)
                            )
                    ) {
                        DropdownMenuItem(
                            onClick = {
                                isMenuExpanded = false
                                onPresentClick(lesson)
                            },
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Присутствующие",
                                color = Color.Black,
                                fontSize = 14.sp)
                        }
                        DropdownMenuItem(
                            onClick = {
                                isMenuExpanded = false
                                onMaterialsClick(lesson)
                            },
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Выдать материалы",
                                color = Color.Black,
                                fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}


@Composable
@Preview
fun LessonCardPreview(
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .wrapContentHeight(),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
            contentColor = Color.Black
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .height(4.dp)
                .fillMaxWidth()
                .background(Division.ACADEMY.color)
        )
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.Top
        ) {
            if (false) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(Division.ACADEMY.color)
                )
            }
            else{
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(Color.Transparent)
                )
            }

            Column(
                modifier = Modifier.weight(1f).padding(start = 12.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
            ) {
                Text(
                    text = "Платформа Microsoft .NET и язык программирования C#",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(modifier = Modifier.padding(start = 8.dp), text = "Группа: 11/1-РПО-24/1", color = Color.Black, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(modifier = Modifier.padding(start = 8.dp), text = "Аудитория: Новый кампус 1-08", color = Color.Black, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(modifier = Modifier.padding(start = 8.dp), text = "17:40 - 19:10", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Box(modifier = Modifier.padding(16.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(24.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = "6",
                        fontSize = 24.sp,
                        color = Color(0xFFDB173F).copy(alpha = 0.1f),
                    )
                }

                Box {
                    IconButton(
                        onClick = { isMenuExpanded = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Меню",
                            tint = Color.Black
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        modifier = Modifier
                            .background(Color.White)
                            .border(
                                width = 1.dp,
                                color = Color.LightGray,
                                shape = RoundedCornerShape(4.dp)
                            )
                    ) {
                        DropdownMenuItem(
                            onClick = {
                                isMenuExpanded = false

                            },
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Присутствующие",
                                color = Color.Black,
                                fontSize = 14.sp)
                        }
                        DropdownMenuItem(
                            onClick = {
                                isMenuExpanded = false

                            },
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Выдать материалы",
                                color = Color.Black,
                                fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}