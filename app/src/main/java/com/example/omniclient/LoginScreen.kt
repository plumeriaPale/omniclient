package com.example.omniclient

import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.sp
import com.example.omniclient.api.MyCookieJar
import com.example.omniclient.api.okHttpClient
import com.example.omniclient.components.customColorsTextField
import com.example.omniclient.preferences.AuthPreferences

//Накинуть Scaffold и отображать CircularProgressIndicator поверх ввода и добавить блюр на всю остальную область
@Composable
fun LoginScreen(
    isLoading: Boolean,
    onLogin: (String, String) -> Unit,
    context: Context
) {
    val authPreferences = remember { AuthPreferences(context) }
    var username by remember { mutableStateOf(authPreferences.getUsername() ?: "") }
    var password by remember { mutableStateOf(authPreferences.getPassword() ?: "") }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.Red,
                strokeWidth = 4.dp
            )
        } else {
            Text(text = "Omni", style = TextStyle(fontFamily = FontFamily(Font(R.font.segoe_ui_light)), fontSize = 54.sp))
            Spacer(modifier = Modifier.height(32.dp))
            Column(
                modifier = Modifier.drawBehind {
                    drawLine(
                        color = Color(0xFFDB173F),
                        start = Offset(0f, 0f),
                        end = Offset(size.width+16, 0f),
                        strokeWidth = 32f
                    )
                    drawLine(
                        color = Color(0xFFDB173F),
                        start = Offset(size.width, 0f),
                        end = Offset(size.width, size.height),
                        strokeWidth = 32f
                    )
                }.padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Логин") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = customColorsTextField()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = customColorsTextField()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        authPreferences.saveCredentials(username, password)
                        (okHttpClient.cookieJar as MyCookieJar).clearCookies()
                        onLogin(username, password)
                    },
                    modifier = Modifier,
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

