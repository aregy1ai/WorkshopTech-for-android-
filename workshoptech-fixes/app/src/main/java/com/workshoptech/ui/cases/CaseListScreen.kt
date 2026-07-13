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
import com.workshoptech.viewmodel.CaseListViewModel

private val STATUS_FILTERS = listOf(
    null to "الكل",
    "NEW" to "جديد",
    "IN_PROGRESS" to "قيد التنفيذ",
    "READY_FOR_DELIVERY" to "جاهز",
    "DELIVERED" to "مُسلّم"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseListScreen(
    viewModel: CaseListViewModel,
    onCreateCase:  () -> Unit,
    onOpenCase:    (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            WorkshopTopBar(
                title  = "الملفات",
                onBack = onNavigateBack,
                actions = {
                    IconButton(onClick = onCreateCase) { Icon(Icons.Default.Add, "إضافة") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateCase, containerColor = Orange600) {
                Icon(Icons.Default.Add, "ملف جديد", tint = White)
            }
        }
    ) { padding ->
        Column(Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize()) {

            // Search bar
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onSearch,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("بحث عن لوحة أو سيارة…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (state.query.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearch("") }) { Icon(Icons.Default.Clear, null) }
                    }
                },
                shape  = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Status filters
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(STATUS_FILTERS) { (key, label) ->
                    val selected = state.statusFilter == key
                    FilterChip(
                        selected = selected,
                        onClick  = { viewModel.onStatusFilter(key) },
                        label    = { Text(label) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            when {
                state.isLoading -> LoadingScreen()
                state.cases.isEmpty() -> EmptyState(
                    icon     = Icons.Default.DirectionsCar,
                    title    = "لا توجد ملفات",
                    subtitle = "اضغط + لإنشاء ملف جديد",
                    actionLabel = "ملف جديد",
                    onAction = onCreateCase
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.cases, key = { it.caseId }) { c ->
                        Card(
                            modifier  = Modifier.fillMaxWidth(),
                            shape     = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(2.dp),
                            onClick   = { onOpenCase(c.caseId) }
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DirectionsCar, null, Modifier.size(42.dp), tint = Blue600)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(c.licensePlate, style = MaterialTheme.typography.titleMedium)
                                        Spacer(Modifier.width(8.dp))
                                        StatusChip(c.status)
                                    }
                                    Text("${c.make} ${c.model} ${c.year ?: ""}".trim(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    c.estimatedCost?.let { cost ->
                                        Text("تقدير: ${"%.0f".format(cost)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Orange600)
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, null, tint = Gray400)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }
}
