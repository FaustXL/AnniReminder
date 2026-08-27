package com.faust.annireminder.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Sundance 2023 海报配色：橙 / 黑 / 灰 / 纸白 */
object C {
    val Black = Color(0xFF121212)
    val CardBlack = Color(0xFF1E1E1E)
    val Orange = Color(0xFFFF4B00)
    val OrangeDeep = Color(0xFFCC3D00)
    val Gray = Color(0xFFA6A69E)
    val GrayCard = Color(0xFFB4B4AC)
    val Paper = Color(0xFFF4F1EC)
}

val Mono = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 12.sp,
    letterSpacing = 0.5.sp,
    fontWeight = FontWeight.Medium
)

@Composable
fun AnniTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.Orange,
            onPrimary = C.Black,
            background = C.Black,
            onBackground = C.Paper,
            surface = C.CardBlack,
            onSurface = C.Paper,
            secondary = C.Gray,
            onSecondary = C.Black
        ),
        content = content
    )
}
