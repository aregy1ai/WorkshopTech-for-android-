package com.workshoptech.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary          = Blue600,
    onPrimary        = White,
    primaryContainer = Blue100,
    onPrimaryContainer = Blue700,
    secondary        = Orange600,
    onSecondary      = White,
    secondaryContainer = Orange100,
    onSecondaryContainer = Orange700,
    background       = Gray100,
    onBackground     = Gray900,
    surface          = White,
    onSurface        = Gray900,
    surfaceVariant   = Gray200,
    onSurfaceVariant = Gray700,
    error            = Red700,
    onError          = White,
    errorContainer   = Red100,
    onErrorContainer = Red700,
    outline          = Gray400,
    outlineVariant   = Gray200,
)

private val DarkColors = darkColorScheme(
    primary          = Blue100,
    onPrimary        = Blue700,
    primaryContainer = Blue700,
    onPrimaryContainer = Blue100,
    secondary        = Orange100,
    onSecondary      = Orange700,
    secondaryContainer = Orange700,
    onSecondaryContainer = Orange100,
    background       = Gray900,
    onBackground     = Gray200,
    surface          = Gray800,
    onSurface        = Gray200,
    surfaceVariant   = Gray700,
    onSurfaceVariant = Gray400,
    error            = Red100,
    onError          = Red700,
    errorContainer   = Red700,
    onErrorContainer = Red100,
    outline          = Gray600,
    outlineVariant   = Gray700,
)

@Composable
fun WorkshopTechTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colors, typography = WorkshopTypography, content = content)
}
