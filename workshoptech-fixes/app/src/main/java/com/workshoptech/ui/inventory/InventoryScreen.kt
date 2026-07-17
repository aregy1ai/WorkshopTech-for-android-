package com.workshoptech.ui.inventory

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
import com.workshoptech.data.entity.InventoryEntity
import com.workshoptech.ui.common.*
import com.workshoptech.ui.theme.*
import com.workshoptech.viewmodel.InventoryViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel:      InventoryViewModel,
    onNavigateBack: () -> Unit
) {
    val state  by viewModel.state.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { WorkshopTopBar("المخزون", onBack = onNavigateBack, actions = {
            IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, null) }
        }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, containerColor = Blue600) {
                Icon(Icons.Default.Add, null, tint = White)
            }
        }
    ) { padding ->
        Column(Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize()) {

            // Low stock warning banner
            if (state.lowStockItems.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors   = CardDefaults.cardColors(containerColor = Red100),
                    shape    = RoundedCornerShape(10.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = Red700, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("${state.lowStockItems.size} صنف بمخزون منخفض", style = MaterialTheme.typography.bodySmall, color = Red700)
                    }
                }
            }

            when {
                state.isLoading -> LoadingScreen()
                state.items.isEmpty() -> EmptyState(
                    icon  = Icons.Default.Inventory,
                    title = "المخزون فارغ",
                    subtitle = "اضغط + لإضافة صنف",
                    actionLabel = "إضافة صنف",
                    onAction = { showAdd = true }
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.lowStockItems.isNotEmpty()) {
                        item { SectionHeader("مخزون منخفض ⚠️") }
                        items(state.lowStockItems, key = { "low_${it.itemId}" }) { item ->
                            InventoryRow(item, isLow = true, onDecrement = { viewModel.decrement(item.itemId) })
                        }
                        item { Spacer(Modifier.height(4.dp)) }
                    }
                    item { SectionHeader("جميع الأصناف (${state.items.size})") }
                    items(state.items.filterNot { it in state.lowStockItems }, key = { it.itemId }) { item ->
                        InventoryRow(item, isLow = false, onDecrement = { viewModel.decrement(item.itemId) })
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showAdd) {
        AddItemDialog(
            onSave = { name, cat, qty, min, price ->
                viewModel.upsert(
                    InventoryEntity(
                        itemId      = UUID.randomUUID().toString(),
                        name        = name,
                        category    = cat,
                        quantity    = qty,
                        minQuantity = min,
                        unitPrice   = price
                    )
                )
                showAdd = false
            },
            onDismiss = { showAdd = false }
        )
    }
}

@Composable
private fun InventoryRow(item: InventoryEntity, isLow: Boolean, onDecrement: () -> Unit) {
    Card(
        shape  = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isLow) Red100.copy(0.5f) else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Inventory2, null, Modifier.size(36.dp), tint = if (isLow) Red500 else Blue600)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleSmall)
                Text("${item.category} • ${item.quantity} وحدة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (isLow) Text("الحد الأدنى: ${item.minQuantity}", style = MaterialTheme.typography.labelSmall, color = Red700)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${"%.2f".format(item.unitPrice)}", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = onDecrement, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp), modifier = Modifier.height(28.dp)) {
                    Text("استخدام", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun AddItemDialog(
    onSave:   (String, String, Int, Int, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var name  by remember { mutableStateOf("") }
    var cat   by remember { mutableStateOf("") }
    var qty   by remember { mutableStateOf("") }
    var min   by remember { mutableStateOf("5") }
    var price by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة صنف") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name,  onValueChange = { name  = it }, label = { Text("اسم الصنف *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = cat,   onValueChange = { cat   = it }, label = { Text("التصنيف") },    modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = qty,   onValueChange = { qty   = it }, label = { Text("الكمية") },   modifier = Modifier.weight(1f))
                    OutlinedTextField(value = min,   onValueChange = { min   = it }, label = { Text("الحد الأدنى") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("السعر") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) onSave(name, cat, qty.toIntOrNull() ?: 0, min.toIntOrNull() ?: 5, price.toDoubleOrNull() ?: 0.0)
                },
                enabled = name.isNotBlank()
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
