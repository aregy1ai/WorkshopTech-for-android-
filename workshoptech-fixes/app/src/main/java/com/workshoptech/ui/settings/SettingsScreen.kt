package com.workshoptech.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.workshoptech.WorkshopTechApp
import com.workshoptech.ui.common.WorkshopTopBar
import com.workshoptech.ui.theme.*

private val COUNTRIES = listOf(
    "LY" to "🇱🇾 ليبيا",    "EG" to "🇪🇬 مصر",       "SA" to "🇸🇦 السعودية",
    "AE" to "🇦🇪 الإمارات",  "KW" to "🇰🇼 الكويت",    "QA" to "🇶🇦 قطر",
    "BH" to "🇧🇭 البحرين",   "OM" to "🇴🇲 عُمان",     "JO" to "🇯🇴 الأردن",
    "LB" to "🇱🇧 لبنان",    "SY" to "🇸🇾 سوريا",      "IQ" to "🇮🇶 العراق",
    "YE" to "🇾🇪 اليمن",    "PS" to "🇵🇸 فلسطين",    "SD" to "🇸🇩 السودان",
    "TN" to "🇹🇳 تونس",     "DZ" to "🇩🇿 الجزائر",   "MA" to "🇲🇦 المغرب",
    "MR" to "🇲🇷 موريتانيا","SO" to "🇸🇴 الصومال",   "DJ" to "🇩🇯 جيبوتي",
    "KM" to "🇰🇲 جزر القمر"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack:  () -> Unit,
    onDarkModeToggle: (Boolean) -> Unit
) {
    val app          = WorkshopTechApp.get()
    var darkMode     by remember { mutableStateOf(app.isDarkMode) }
    var country      by remember { mutableStateOf(app.currentCountry) }
    var showCountry  by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { WorkshopTopBar("الإعدادات", onBack = onNavigateBack) }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 16.dp,
                bottom = 32.dp, start = 16.dp, end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── الدولة ──────────────────────────────────────────────────────
            item {
                SettingsGroup(title = "بيانات الورشة") {
                    SettingsRow(
                        icon  = Icons.Default.Language,
                        label = "الدولة",
                        value = COUNTRIES.find { it.first == country }?.second ?: country,
                        onClick = { showCountry = true }
                    )
                }
            }

            // ── المظهر ──────────────────────────────────────────────────────
            item {
                SettingsGroup(title = "المظهر") {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DarkMode, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                        Text("الوضع الداكن", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked         = darkMode,
                            onCheckedChange = { v ->
                                darkMode = v
                                app.saveSettings(darkMode = v)
                                onDarkModeToggle(v)
                            }
                        )
                    }
                }
            }

            // ── عن التطبيق ───────────────────────────────────────────────────
            item {
                SettingsGroup(title = "عن التطبيق") {
                    SettingsInfoRow(Icons.Default.Info, "الإصدار", "1.0.0")
                    SettingsInfoRow(Icons.Default.Storage, "قاعدة البيانات", "v3 (13 جدول)")
                    SettingsInfoRow(Icons.Default.Psychology, "نماذج AI", "7 نماذج TFLite")
                    SettingsInfoRow(Icons.Default.Language, "الدول المدعومة", "22 دولة عربية")
                }
            }
        }
    }

    // Country picker
    if (showCountry) {
        ModalBottomSheet(onDismissRequest = { showCountry = false }) {
            Column(Modifier.padding(bottom = 32.dp)) {
                Text("اختر الدولة", Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                COUNTRIES.forEach { (code, label) ->
                    TextButton(
                        onClick = {
                            country = code
                            app.saveSettings(country = code)
                            showCountry = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label, Modifier.weight(1f))
                        if (code == country) Icon(Icons.Default.Check, null, tint = Green500)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Column {
            Text(title, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium, color = Blue600)
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(Icons.Default.ChevronRight, null, tint = Gray400)
    }
}

@Composable
private fun SettingsInfoRow(icon: ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
