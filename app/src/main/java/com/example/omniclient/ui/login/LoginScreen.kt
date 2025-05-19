package com.example.omniclient.ui.login

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.sp
import com.example.omniclient.R
import com.example.omniclient.components.customColorsTextField

@Composable
fun LoginScreen(
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isLoading: Boolean,
    onLogin: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.Red, strokeWidth = 4.dp)
            Log.d("Dev:Login", "Login is not visible")
        } else {
            Log.d("Dev:Login", "Login is visible")
            Text(
                text = "Omni",
                style = TextStyle(
                    fontFamily = FontFamily(Font(R.font.segoe_ui_light)),
                    fontSize = 54.sp
                )
            )
            Spacer(modifier = Modifier.height(32.dp))
            Column(
                modifier = Modifier
                    .drawBehind {
                        drawLine(
                            color = Color(0xFFDB173F),
                            start = Offset(0f, 0f),
                            end = Offset(size.width + 16, 0f),
                            strokeWidth = 32f
                        )
                        drawLine(
                            color = Color(0xFFDB173F),
                            start = Offset(size.width, 0f),
                            end = Offset(size.width, size.height),
                            strokeWidth = 32f
                        )
                    }
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text("Логин") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = customColorsTextField()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Пароль") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = customColorsTextField()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onLogin,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD91842),
                        contentColor = Color.White
                    )
                ) {
                    Text("Войти")
                }
            }
        }
    }
}