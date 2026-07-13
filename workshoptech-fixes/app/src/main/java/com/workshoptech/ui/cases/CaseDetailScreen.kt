package com.workshoptech.ui.cases

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.workshoptech.viewmodel.CaseDetailViewModel

private val WORKFLOW_STATUSES = listOf(
    "NEW", "APPROVED", "IN_PROGRESS",
    "READY_FOR_DELIVERY", "DELIVERED", "ON_HOLD", "CANCELLED"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseDetailScreen(
    caseId:    String,
    viewModel: CaseDetailViewModel,
    onTakePhoto:   (String, String) -> Unit,
    onInspect:     (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showStatusSheet by remember { mutableStateOf(false) }

    LaunchedEffect(caseId) { viewModel.loadCase(caseId) }

    Scaffold(
        topBar = {
            WorkshopTopBar(
                title  = state.case?.licensePlate ?: "تفاصيل الملف",
                onBack = onNavigateBack,
                actions = {
                    IconButton(onClick = { showStatusSheet = true }) { Icon(Icons.Default.Edit, "تغيير الحالة") }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingScreen()
            state.case == null -> EmptyState(Icons.Default.Error, "لم يُعثر على الملف")
            else -> {
                val c = state.case!!
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding() + 12.dp,
                        bottom = 24.dp, start = 16.dp, end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header card
                    item {
                        Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DirectionsCar, null, Modifier.size(48.dp), tint = Blue600)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(c.licensePlate, style = MaterialTheme.typography.titleLarge)
                                        Text("${c.make} ${c.model} ${c.year ?: ""}".trim(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    StatusChip(c.status)
                                }
                                HorizontalDivider()
                                state.customer?.let { cust ->
                                    InfoRow(Icons.Default.Person, "العميل", cust.name)
                                    cust.phone?.let { InfoRow(Icons.Default.Phone, "الهاتف", it) }
                                }
                                c.estimatedCost?.let { InfoRow(Icons.Default.AttachMoney, "التكلفة التقديرية", "${"%.0f".format(it)}") }
                                c.actualCost?.let { InfoRow(Icons.Default.Receipt, "التكلفة الفعلية", "${"%.0f".format(it)}") }
                            }
                        }
                    }

                    // Actions row
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionButton(Icons.Default.CameraAlt, "تصوير", Modifier.weight(1f)) { onTakePhoto(caseId, "damage") }
                            ActionButton(Icons.Default.Assignment, "تفتيش", Modifier.weight(1f)) { onInspect(caseId) }
                            ActionButton(Icons.Default.VideoCall, "فيديو", Modifier.weight(1f)) { onTakePhoto(caseId, "video") }
                        }
                    }

                    // Inspections
                    if (state.inspections.isNotEmpty()) {
                        item { SectionHeader("نقاط التفتيش") }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(state.inspections) { insp ->
                                    val (bg, fg) = when (insp.status) {
                                        "PASSED" -> Green100 to Green700
                                        "FAILED" -> Red100   to Red700
                                        else     -> Yellow100 to Yellow700
                                    }
                                    Surface(shape = RoundedCornerShape(8.dp), color = bg, tonalElevation = 1.dp) {
                                        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(insp.type, style = MaterialTheme.typography.labelLarge, color = fg)
                                            Text(when(insp.status) { "PASSED" -> "✓ اجتاز" "FAILED" -> "✗ فشل" else -> "◌ معلق" }, style = MaterialTheme.typography.labelSmall, color = fg)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Tasks
                    if (state.tasks.isNotEmpty()) {
                        item { SectionHeader("مهام سير العمل") }
                        items(state.tasks) { task ->
                            TaskRow(task.type, task.status, task.assignedTo)
                        }
                    }

                    // Notes
                    c.notes?.let { notes ->
                        item {
                            SectionHeader("ملاحظات")
                            Card(shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                Text(notes, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }

    // Status bottom sheet
    if (showStatusSheet) {
        ModalBottomSheet(onDismissRequest = { showStatusSheet = false }) {
            Column(Modifier.padding(bottom = 32.dp)) {
                Text("تغيير الحالة", Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                WORKFLOW_STATUSES.forEach { s ->
                    TextButton(
                        onClick = {
                            viewModel.updateStatus(caseId, s)
                            showStatusSheet = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatusChip(s, Modifier.padding(end = 8.dp))
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }

    state.error?.let { err ->
        LaunchedEffect(err) { viewModel.clearError() }
        ErrorBanner(err, onDismiss = { viewModel.clearError() })
    }
}

@Composable private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text("$label: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable private fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(48.dp), contentPadding = PaddingValues(4.dp)) {
        Icon(icon, null, Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable private fun TaskRow(type: String, status: String, assignedTo: String?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        val icon = when (status) { "DONE" -> Icons.Default.CheckCircle "IN_PROGRESS" -> Icons.Default.RadioButtonChecked else -> Icons.Default.RadioButtonUnchecked }
        val tint = when (status) { "DONE" -> Green500 "IN_PROGRESS" -> Orange500 else -> Gray400 }
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(type, style = MaterialTheme.typography.bodySmall)
            assignedTo?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
