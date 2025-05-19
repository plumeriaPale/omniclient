package com.example.omniclient.data

import androidx.compose.ui.graphics.Color

enum class Division(val id: Int, val color: Color) {
    COLLEGE(458, Color(0xFF9300d5)),
    ACADEMY(74, Color(0xFFDB173F));

    companion object {
        fun fromId(id: Int): Division? = values().find { it.id == id }
    }
} 