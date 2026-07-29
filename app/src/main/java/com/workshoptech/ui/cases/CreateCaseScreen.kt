package com.workshoptech.ui.cases

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.workshoptech.WorkshopTechApp
import com.workshoptech.viewmodel.CreateCaseViewModel
import com.workshoptech.viewmodel.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCaseScreen(onNavigateBack: () -> Unit, onCaseCreated: (String) -> Unit) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as WorkshopTechApp
    val vm: CreateCaseViewModel = viewModel(factory = ViewModelFactory(app.repository))
    val state by vm.state.collectAsState()
    var showCountryDialog by remember { mutableStateOf(false) }
    val countries = listOf("LY" to "ليبيا", "EG" to "مصر", "SA" to "السعودية", "AE" to "الإمارات", "KW" to "الكويت", "QA" to "قطر", "BH" to "البحرين", "OM" to "عمان", "JO" to "الأردن", "LB" to "لبنان", "IQ" to "العراق", "MA" to "المغرب", "TN" to "تونس", "DZ" to "الجزائر", "SD" to "السودان", "YE" to "اليمن", "SY" to "سوريا")

    Scaffold(topBar = { TopAppBar(title = { Text("ملف جديد") }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "رجوع") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("الدولة", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = { showCountryDialog = true }, modifier = Modifier.fillMaxWidth()) { Text(countries.find { it.first == state.country }?.second ?: "اختر الدولة") }
            Text("رقم اللوحة", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(value = state.plateText, onValueChange = { vm.setPlateText(it) }, label = { Text("أدخل رقم اللوحة") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Text("المركبة", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = state.makeId ?: "", onValueChange = { vm.setMake(it) }, label = { Text("الماركة") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = state.modelId ?: "", onValueChange = { vm.setModel(it) }, label = { Text("الطراز") }, modifier = Modifier.weight(1f), singleLine = true)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = state.year?.toString() ?: "", onValueChange = { vm.setYear(it) }, label = { Text("السنة") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = state.colorName ?: "", onValueChange = { vm.setColor(it) }, label = { Text("اللون") }, modifier = Modifier.weight(1f), singleLine = true)
            }
            OutlinedTextField(value = state.freeText ?: "", onValueChange = { vm.setFreeText(it) }, label = { Text("ملاحظات إضافية") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
            if (state.error != null) Text(state.error!!, color = MaterialTheme.colorScheme.error)
            Button(onClick = { vm.submit(onCaseCreated) }, modifier = Modifier.fillMaxWidth(), enabled = !state.isSubmitting) {
                if (state.isSubmitting) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary) else Text("حفظ الملف")
            }
        }
    }
    if (showCountryDialog) AlertDialog(onDismissRequest = { showCountryDialog = false }, title = { Text("اختر الدولة") }, text = { Column { countries.forEach { (id, name) -> TextButton(onClick = { vm.setCountry(id); showCountryDialog = false }, modifier = Modifier.fillMaxWidth()) { Text(name) } } } }, confirmButton = {})
}
