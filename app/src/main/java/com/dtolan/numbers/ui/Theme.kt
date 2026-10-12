package com.dtolan.numbers.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Blue = Color(0xFF0055A4)
private val Red = Color(0xFFEF4135)
private val LightBackground = Color(0xFFF4F6F9)
private val LightSurface = Color(0xFFFFFFFF)
private val DarkBackground = Color(0xFF121417)
private val DarkSurface = Color(0xFF1C2733)

private val LightColors = lightColorScheme(
    primary = Blue,
    secondary = Red,
    background = LightBackground,
    surface = LightSurface,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FB2E5),
    secondary = Color(0xFFFF8A75),
    background = DarkBackground,
    surface = DarkSurface,
)

@Composable
fun NumbersTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
