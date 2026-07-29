package com.workshoptech.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.workshoptech.WorkshopTechApp
import com.workshoptech.viewmodel.CaseListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onNavigateToCases: () -> Unit, onNavigateToCustomers: () -> Unit, onNavigateToCreateCase: () -> Unit, onNavigateToReports: () -> Unit = {}, onNavigateToAbout: () -> Unit = {}) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as WorkshopTechApp
    val vm: CaseListViewModel = viewModel(factory = com.workshoptech.viewmodel.ViewModelFactory(app.repository))
    val stats by vm.dashboardStats.collectAsState()
    LaunchedEffect(Unit) { vm.refreshDashboardStats() }

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val error = MaterialTheme.colorScheme.error

    val statCards = remember(stats, primary, secondary, tertiary, error) {
        listOf(
            StatCardData("استقبال اليوم", stats?.todayReceived?.toString() ?: "-", Icons.Default.DirectionsCar, primary),
            StatCardData("جاري الإصلاح", stats?.inProgress?.toString() ?: "-", Icons.Default.Build, secondary),
            StatCardData("تسليم اليوم", stats?.todayDelivered?.toString() ?: "-", Icons.Default.CheckCircle, tertiary),
            StatCardData("متأخر", stats?.onHold?.toString() ?: "-", Icons.Default.Warning, error)
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("ورشة تك") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = primary, titleContentColor = MaterialTheme.colorScheme.onPrimary)) },
        floatingActionButton = { FloatingActionButton(onClick = onNavigateToCreateCase, containerColor = primary) { Icon(Icons.Default.Add, "ملف جديد") } }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("لوحة القيادة", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(200.dp), userScrollEnabled = false) {
                items(statCards, key = { it.title }) { DashboardCard(it) }
            }
            NavigationCard("جميع الملفات", "عرض وإدارة ملفات السيارات", onNavigateToCases)
            NavigationCard("العملاء", "إدارة بيانات العملاء", onNavigateToCustomers)
            NavigationCard("التقارير والإحصائيات", "رسوم بيانية، إيرادات، وتقارير", onNavigateToReports)
            NavigationCard("عن التطبيق", "AR-EGY — aregy1ai@gmail.com", onNavigateToAbout)
        }
    }
}

data class StatCardData(val title: String, val value: String, val icon: ImageVector, val color: Color)

@Composable
fun DashboardCard(data: StatCardData) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(data.icon, null, tint = data.color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(data.value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = data.color)
            Text(data.title, fontSize = 11.sp)
        }
    }
}

@Composable
fun NavigationCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, style = MaterialTheme.typography.bodySmall) }
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}
