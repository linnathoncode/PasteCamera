package com.pastecamera.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PasteCameraColors = darkColorScheme(
    primary = Color(0xFFFFB000),
    onPrimary = Color(0xFF241A00),
    secondary = Color(0xFFB7C5FF),
    background = Color(0xFF07090D),
    surface = Color(0xFF11141A)
)

@Composable
fun PasteCameraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PasteCameraColors, content = content)
}
