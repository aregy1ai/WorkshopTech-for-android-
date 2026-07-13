package com.workshoptech

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.workshoptech.ui.navigation.NavGraph
import com.workshoptech.ui.theme.WorkshopTechTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val deepLinkCaseId    = intent.getStringExtra("caseId")
        val deepLinkInspType  = intent.getStringExtra("inspectionType")

        setContent {
            val app = WorkshopTechApp.get()
            var darkMode by remember { mutableStateOf(app.isDarkMode) }

            WorkshopTechTheme(darkTheme = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph(
                        deepLinkCaseId       = deepLinkCaseId,
                        deepLinkInspType     = deepLinkInspType,
                        onDarkModeToggle     = { enabled ->
                            darkMode = enabled
                            app.saveSettings(darkMode = enabled)
                        }
                    )
                }
            }
        }
    }
}
