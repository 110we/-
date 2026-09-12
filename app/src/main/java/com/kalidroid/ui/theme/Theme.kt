package com.kalidroid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ForgeDark = darkColorScheme(
    primary = Color(0xFF3DE0C5),
    onPrimary = Color(0xFF052018),
    secondary = Color(0xFF7EE0FF),
    background = Color(0xFF07111B),
    surface = Color(0xFF0C1A27),
    onBackground = Color(0xFFDFF7F1),
    onSurface = Color(0xFFDFF7F1),
    error = Color(0xFFFF6B6B)
)

private val ForgeLight = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    secondary = Color(0xFF0369A1),
    background = Color(0xFFF4FAF8),
    surface = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A)
)

@Composable
fun KaliDroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) ForgeDark else ForgeLight,
        content = content
    )
}
