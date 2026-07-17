package com.workshoptech.ui.customers

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
import com.workshoptech.data.entity.CustomerEntity
import com.workshoptech.ui.common.*
import com.workshoptech.ui.theme.*
import com.workshoptech.viewmodel.CustomerViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(
    viewModel:      CustomerViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { WorkshopTopBar("العملاء", onBack = onNavigateBack, actions = {
            IconButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.PersonAdd, null) }
        }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = Blue600) {
                Icon(Icons.Default.Add, null, tint = White)
            }
        }
    ) { padding ->
        Column(Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize()) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onSearch,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("بحث عن عميل…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            when {
                state.isLoading -> LoadingScreen()
                state.customers.isEmpty() -> EmptyState(
                    icon  = Icons.Default.People,
                    title = "لا يوجد عملاء",
                    subtitle = "اضغط + لإضافة عميل جديد",
                    actionLabel = "إضافة عميل",
                    onAction = { showAddDialog = true }
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.customers, key = { it.customerId }) { cust ->
                        CustomerCard(cust)
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCustomerDialog(
            onSave    = { name, phone ->
                val now = System.currentTimeMillis()
                viewModel.upsert(
                    CustomerEntity(
                        customerId = UUID.randomUUID().toString(),
                        name       = name,
                        phone      = phone.takeIf { it.isNotBlank() },
                        createdAt  = now,
                        updatedAt  = now
                    )
                )
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun CustomerCard(customer: CustomerEntity) {
    Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(44.dp), shape = RoundedCornerShape(22.dp), color = Blue100) {
                Box(contentAlignment = Alignment.Center) {
                    Text(customer.name.take(1), style = MaterialTheme.typography.titleMedium, color = Blue700)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(customer.name, style = MaterialTheme.typography.titleSmall)
                customer.phone?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = Gray400)
        }
    }
}

@Composable
private fun AddCustomerDialog(onSave: (String, String) -> Unit, onDismiss: () -> Unit) {
    var name  by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة عميل") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("الهاتف") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onSave(name, phone) }, enabled = name.isNotBlank()) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
