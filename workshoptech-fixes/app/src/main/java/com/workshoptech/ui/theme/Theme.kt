package com.workshoptech.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

/**
 * WorkshopTech Compose theme.
 *
 * Android 15 (API 35):
 *  - enableEdgeToEdge() is called in MainActivity → window statusBar / navBar
 *    are fully transparent; status bar icon colour is handled by the OS.
 *  - Do NOT set window.statusBarColor here — it is deprecated on API 35 and
 *    conflicts with edge-to-edge enforcement.
 *  - WindowCompat.getInsetsController() is called in MainActivity if needed.
 */

private val LightColors = lightColorScheme(
    primary              = Blue600,
    onPrimary            = White,
    primaryContainer     = Blue100,
    onPrimaryContainer   = Blue700,
    secondary            = Orange600,
    onSecondary          = White,
    secondaryContainer   = Orange100,
    onSecondaryContainer = Orange700,
    background           = Gray100,
    onBackground         = Gray900,
    surface              = White,
    onSurface            = Gray900,
    surfaceVariant       = Gray200,
    onSurfaceVariant     = Gray700,
    error                = Red700,
    onError              = White,
    errorContainer       = Red100,
    onErrorContainer     = Red700,
    outline              = Gray400,
    outlineVariant       = Gray200,
)

private val DarkColors = darkColorScheme(
    primary              = Blue100,
    onPrimary            = Blue700,
    primaryContainer     = Blue700,
    onPrimaryContainer   = Blue100,
    secondary            = Orange100,
    onSecondary          = Orange700,
    secondaryContainer   = Orange700,
    onSecondaryContainer = Orange100,
    background           = Gray900,
    onBackground         = Gray200,
    surface              = Gray800,
    onSurface            = Gray200,
    surfaceVariant       = Gray700,
    onSurfaceVariant     = Gray400,
    error                = Red100,
    onError              = Red700,
    errorContainer       = Red700,
    onErrorContainer     = Red100,
    outline              = Gray600,
    outlineVariant       = Gray700,
)

@Composable
fun WorkshopTechTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    // Android 15 note: system bar colours managed via enableEdgeToEdge() in
    // MainActivity, NOT via SideEffect + window.statusBarColor (deprecated).
    // Compose Material3 automatically picks appropriate icon colours via
    // the colorScheme — nothing extra needed here.

    MaterialTheme(
        colorScheme = colors,
        typography  = WorkshopTypography,
        content     = content
    )
}
