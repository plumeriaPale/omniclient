package com.example.omniclient.components

import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun customColorsTextField() : TextFieldColors {
    return TextFieldDefaults.colors(
        focusedContainerColor = Color(0xFFFFFBFE),
        unfocusedContainerColor = Color(0xFFFFFBFE),
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        cursorColor = Color(0xFFDB173F),
        focusedIndicatorColor = Color(0xFFDB173F),
        focusedLabelColor = Color(0xFFDB173F),
        unfocusedLabelColor = Color.Black,
        selectionColors = TextSelectionColors(
            handleColor = Color(0xFFDB173F),
            backgroundColor = Color(0xFFDB173F).copy(alpha = 0.4f)
        )
    )
}