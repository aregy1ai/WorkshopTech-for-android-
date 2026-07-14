package com.workshoptech

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.workshoptech.ui.navigation.NavGraph
import com.workshoptech.ui.theme.WorkshopTechTheme

/**
 * Single-activity host for WorkshopTech.
 *
 * Android 15 (API 35) setup:
 *  - installSplashScreen()      — AndroidX SplashScreen API (replaces custom splash activity)
 *  - enableEdgeToEdge()         — full-screen, transparent system bars (mandatory on API 35)
 *  - SystemBarStyle             — explicit light/dark system-bar icon style
 *
 * Deep-link extras:
 *  - "caseId"       → navigates directly to CaseDetailScreen
 *  - "inspType"     → opens inspection directly (e.g. from notification)
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // ── Android 12+ Splash Screen API ─────────────────────────────────────
        // Must be called BEFORE super.onCreate()
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Keep splash visible while app initialises (e.g. loading prefs)
        splashScreen.setKeepOnScreenCondition { false }

        // ── Android 15 Edge-to-Edge ────────────────────────────────────────────
        // enforceEdgeToEdge() is automatic on targetSdk 35 — enableEdgeToEdge()
        // still called explicitly so we can style the system bars.
        enableEdgeToEdge(
            statusBarStyle      = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle  = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        val deepLinkCaseId   = intent.getStringExtra("caseId")
        val deepLinkInspType = intent.getStringExtra("inspectionType")

        setContent {
            val app = WorkshopTechApp.get()
            var darkMode by remember { mutableStateOf(app.isDarkMode) }

            WorkshopTechTheme(darkTheme = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    NavGraph(
                        deepLinkCaseId   = deepLinkCaseId,
                        deepLinkInspType = deepLinkInspType,
                        onDarkModeToggle = { enabled ->
                            darkMode = enabled
                            app.saveSettings(darkMode = enabled)
                        }
                    )
                }
            }
        }
    }
}
