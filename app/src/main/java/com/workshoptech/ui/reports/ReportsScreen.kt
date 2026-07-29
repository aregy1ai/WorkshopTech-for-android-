package com.workshoptech.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ReportStats(val totalCases: Int = 0, val totalRevenue: Double = 0.0, val avgRepairTime: Double = 0.0, val topCustomers: List<Pair<String, Int>> = emptyList(), val statusBreakdown: Map<String, Int> = emptyMap(), val monthlyRevenue: Map<String, Double> = emptyMap())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(stats: ReportStats, onExportPdf: () -> Unit, onNavigateBack: () -> Unit) {
    var selectedPeriod by remember { mutableStateOf("month") }
    Scaffold(topBar = { TopAppBar(title = { Text("التقارير والإحصائيات") }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "رجوع") } }, actions = { IconButton(onClick = onExportPdf) { Icon(Icons.Default.PictureAsPdf, "تصدير PDF") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("week" to "أسبوع", "month" to "شهر", "year" to "سنة").forEach { (key, label) -> FilterChip(selected = selectedPeriod == key, onClick = { selectedPeriod = key }, label = { Text(label, fontSize = 12.sp) }) } } }
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatCard("إجمالي الملفات", "${stats.totalCases}", Icons.Default.Folder, Color(0xFF1565C0), Modifier.weight(1f)); StatCard("الإيرادات", "${stats.totalRevenue.toInt()} د.ل", Icons.Default.AttachMoney, Color(0xFF2E7D32), Modifier.weight(1f)) } }
            item { StatCard("متوسط وقت الإصلاح", "${"%.1f".format(stats.avgRepairTime)} ساعة", Icons.Default.Timer, Color(0xFFE65100), Modifier.fillMaxWidth()) }
            item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text("الملفات حسب الحالة", fontWeight = FontWeight.Bold, fontSize = 16.sp); Spacer(Modifier.height(12.dp))
                val statusLabels = mapOf("NEW" to "جديد", "IN_PROGRESS" to "جاري", "ON_HOLD" to "متوقف", "COMPLETED" to "مكتمل", "DELIVERED" to "تم التسليم")
                val colors = listOf(Color(0xFF1565C0), Color(0xFFE65100), Color(0xFFC62828), Color(0xFF2E7D32), Color(0xFF6A1B9A))
                val total = stats.statusBreakdown.values.sum().coerceAtLeast(1)
                stats.statusBreakdown.entries.forEachIndexed { index, (key, count) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(statusLabels[key] ?: key, Modifier.width(60.dp), fontSize = 12.sp)
                        Box(Modifier.weight(1f).height(16.dp).clip(RoundedCornerShape(4.dp)).padding(horizontal = 4.dp)) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(count.toFloat() / total).clip(RoundedCornerShape(4.dp)).background(colors.getOrElse(index) { Color.Gray }))
                        }
                        Spacer(Modifier.width(6.dp)); Text("$count", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } } }
            item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text("الإيرادات الشهرية", fontWeight = FontWeight.Bold, fontSize = 16.sp); Spacer(Modifier.height(12.dp))
                val maxRevenue = stats.monthlyRevenue.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                stats.monthlyRevenue.entries.take(6).forEach { (month, revenue) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(month, Modifier.width(40.dp), fontSize = 11.sp)
                        Box(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(4.dp))) { Box(Modifier.fillMaxHeight().fillMaxWidth((revenue / maxRevenue).toFloat()).clip(RoundedCornerShape(4.dp)).background(Color(0xFF1565C0))) }
                        Spacer(Modifier.width(6.dp)); Text("${revenue.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } } }
            item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text("أكثر العملاء", fontWeight = FontWeight.Bold, fontSize = 16.sp); Spacer(Modifier.height(8.dp))
                stats.topCustomers.take(5).forEachIndexed { index, (name, visits) -> Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("${index + 1}. $name", fontSize = 13.sp); Text("$visits زيارة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0)) } }
            } } }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(modifier) { Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, tint = color, Modifier.size(24.dp)); Spacer(Modifier.height(4.dp)); Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color); Text(title, fontSize = 11.sp) } }
}
