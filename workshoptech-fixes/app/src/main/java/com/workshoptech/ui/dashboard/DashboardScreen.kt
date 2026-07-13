package com.workshoptech.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.workshoptech.ui.common.*
import com.workshoptech.ui.theme.*
import com.workshoptech.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToCases:     () -> Unit,
    onNavigateToCase:      (String) -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToSettings:  () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ورشة تك") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, "إعدادات", tint = White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Blue600, titleContentColor = White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToCases,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("ملف جديد") },
                containerColor = Orange600
            )
        }
    ) { padding ->
        if (state.isLoading) {
            LoadingScreen()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom = 88.dp, start = 16.dp, end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // KPI grid
                item {
                    Text("نظرة سريعة", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricCard(
                            title = "ملفات نشطة",
                            value = state.totalActiveCases.toString(),
                            icon  = Icons.Default.DirectionsCar,
                            tint  = Blue600,
                            modifier = Modifier.weight(1f),
                            onClick  = onNavigateToCases
                        )
                        MetricCard(
                            title = "جاهز للتسليم",
                            value = state.todayDeliveries.toString(),
                            icon  = Icons.Default.CheckCircle,
                            tint  = Green700,
                            modifier = Modifier.weight(1f),
                            onClick  = onNavigateToCases
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricCard(
                            title = "فحوصات معلقة",
                            value = state.pendingInspections.toString(),
                            icon  = Icons.Default.Assignment,
                            tint  = Orange600,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "مخزون منخفض",
                            value = state.lowStockItems.size.toString(),
                            icon  = Icons.Default.Warning,
                            tint  = if (state.lowStockItems.isNotEmpty()) Red500 else Gray600,
                            modifier = Modifier.weight(1f),
                            onClick  = onNavigateToInventory
                        )
                    }
                }

                // Quick actions
                item {
                    Spacer(Modifier.height(4.dp))
                    SectionHeader("الإجراءات السريعة")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickAction(Icons.Default.People,   "العملاء",  Modifier.weight(1f), onNavigateToCustomers)
                        QuickAction(Icons.Default.Inventory,"المخزون",  Modifier.weight(1f), onNavigateToInventory)
                        QuickAction(Icons.Default.List,     "الملفات",  Modifier.weight(1f), onNavigateToCases)
                    }
                }

                // Ready for delivery
                if (state.readyCases.isNotEmpty()) {
                    item { SectionHeader("جاهز للتسليم", "عرض الكل", onNavigateToCases) }
                    items(state.readyCases.take(3)) { c ->
                        CaseSummaryCard(
                            plate    = c.licensePlate,
                            vehicle  = "${c.make} ${c.model}",
                            status   = c.status,
                            onClick  = { onNavigateToCase(c.caseId) }
                        )
                    }
                }

                // Active cases
                if (state.activeCases.isNotEmpty()) {
                    item { SectionHeader("قيد التنفيذ", "عرض الكل", onNavigateToCases) }
                    items(state.activeCases.take(5)) { c ->
                        CaseSummaryCard(
                            plate   = c.licensePlate,
                            vehicle = "${c.make} ${c.model}",
                            status  = c.status,
                            onClick = { onNavigateToCase(c.caseId) }
                        )
                    }
                }

                // Low stock warnings
                if (state.lowStockItems.isNotEmpty()) {
                    item {
                        SectionHeader("تحذيرات المخزون", "إدارة", onNavigateToInventory)
                        state.lowStockItems.take(3).forEach { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                colors = CardDefaults.cardColors(containerColor = Red100.copy(0.4f)),
                                shape  = RoundedCornerShape(8.dp)
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, null, tint = Red500, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("${item.name} — ${item.quantity} وحدة متبقية", style = MaterialTheme.typography.bodySmall, color = Red700)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Error
        state.error?.let { err ->
            ErrorBanner(err, onDismiss = { viewModel.refresh() }, modifier = Modifier.padding(top = padding.calculateTopPadding()))
        }
    }
}

@Composable
private fun QuickAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), onClick = onClick, elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(28.dp), tint = Blue600)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun CaseSummaryCard(plate: String, vehicle: String, status: String, onClick: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        onClick   = onClick
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DirectionsCar, null, Modifier.size(36.dp), tint = Blue600)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(plate, style = MaterialTheme.typography.titleSmall)
                Text(vehicle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusChip(status)
        }
    }
}
