package com.workshoptech.ui.cases

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.workshoptech.WorkshopTechApp
import com.workshoptech.viewmodel.CaseDetailViewModel
import com.workshoptech.viewmodel.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseDetailScreen(caseId: String, onNavigateBack: () -> Unit) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as WorkshopTechApp
    val vm: CaseDetailViewModel = viewModel(factory = ViewModelFactory(app.repository))
    val state by vm.state.collectAsState()
    LaunchedEffect(caseId) { vm.loadCase(caseId) }

    Scaffold(topBar = { TopAppBar(title = { Text(state.case?.plateText ?: "تفاصيل الملف") }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "رجوع") } }) }) { padding ->
        if (state.isLoading) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else {
            val case = state.case ?: return@Scaffold
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("معلومات المركبة", fontWeight = FontWeight.Bold, fontSize = 16.sp); Spacer(Modifier.height(8.dp)); DetailRow("رقم اللوحة", case.plateText); DetailRow("الدولة", case.plateCountry); DetailRow("المركبة", case.vehicleFreeText ?: "${case.vehicleMakeId ?: ""} ${case.vehicleModelId ?: ""} ${case.vehicleYear ?: ""}"); DetailRow("اللون", case.colorName ?: "-") } }
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("حالة الملف", fontWeight = FontWeight.Bold, fontSize = 16.sp); Spacer(Modifier.height(8.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("NEW" to "جديد", "IN_PROGRESS" to "جاري", "ON_HOLD" to "متوقف", "COMPLETED" to "مكتمل", "DELIVERED" to "تم التسليم").forEach { (key, label) -> FilterChip(selected = case.status == key, onClick = { vm.updateStatus(caseId, key) }, label = { Text(label, fontSize = 11.sp) }) } } } }
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("التكاليف", fontWeight = FontWeight.Bold, fontSize = 16.sp); Spacer(Modifier.height(8.dp)); DetailRow("التكلفة التقديرية", "${case.estimatedCost ?: "-"} د.ل"); DetailRow("التكلفة الفعلية", "${case.actualCost ?: "-"} د.ل"); DetailRow("الساعات التقديرية", "${case.estimatedHours ?: "-"}"); DetailRow("الساعات الفعلية", "${case.actualHours ?: "-"}") } }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 13.sp); Text(value, fontWeight = FontWeight.Medium, fontSize = 13.sp) } }
