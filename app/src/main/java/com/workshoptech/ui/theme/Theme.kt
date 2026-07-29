package com.workshoptech.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(primary = Color(0xFF90CAF9), secondary = Color(0xFFCE93D8), tertiary = Color(0xFFA5D6A7), background = Color(0xFF121212), surface = Color(0xFF1E1E1E))
private val LightColorScheme = lightColorScheme(primary = Color(0xFF1565C0), secondary = Color(0xFF7B1FA2), tertiary = Color(0xFF2E7D32), background = Color(0xFFF5F5F5), surface = Color.White)

@Composable
fun WorkshopTechTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme, typography = Typography(), content = content)
}
