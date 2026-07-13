package com.workshoptech.ui.cases

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.workshoptech.ui.common.*
import com.workshoptech.ui.theme.*
import com.workshoptech.viewmodel.CreateCaseViewModel

@Composable
fun CreateCaseScreen(
    viewModel: CreateCaseViewModel,
    onCaseCreated:  (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val state  by viewModel.state.collectAsState()
    val scroll = rememberScrollState()

    LaunchedEffect(state.createdCaseId) {
        state.createdCaseId?.let { id -> onCaseCreated(id) }
    }

    Scaffold(
        topBar = { WorkshopTopBar("ملف جديد", onBack = onNavigateBack) }
    ) { padding ->
        Box(Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── العميل ──────────────────────────────────────────────────
                SectionCard(title = "بيانات العميل") {
                    OutlinedTextField(
                        value    = state.customerPhone,
                        onValueChange = { viewModel.onPhoneChanged(it) },
                        label    = { Text("رقم الهاتف *") },
                        leadingIcon = { Icon(Icons.Default.Phone, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    state.existingCustomer?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Green500, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("عميل موجود: ${it.name}", style = MaterialTheme.typography.labelMedium, color = Green700)
                        }
                    }
                    OutlinedTextField(
                        value    = state.customerName,
                        onValueChange = { viewModel.onFieldChanged(name = it) },
                        label    = { Text("اسم العميل") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ── السيارة ──────────────────────────────────────────────────
                SectionCard(title = "بيانات السيارة") {
                    OutlinedTextField(
                        value    = state.licensePlate,
                        onValueChange = { viewModel.onFieldChanged(plate = it.uppercase()) },
                        label    = { Text("رقم اللوحة *") },
                        leadingIcon = { Icon(Icons.Default.ConfirmationNumber, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.make,
                            onValueChange = { viewModel.onFieldChanged(make = it) },
                            label    = { Text("الماركة") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = state.model,
                            onValueChange = { viewModel.onFieldChanged(model = it) },
                            label    = { Text("الموديل") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.year,
                            onValueChange = { viewModel.onFieldChanged(year = it) },
                            label    = { Text("السنة") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = state.color,
                            onValueChange = { viewModel.onFieldChanged(color = it) },
                            label    = { Text("اللون") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value    = state.notes,
                        onValueChange = { viewModel.onFieldChanged(notes = it) },
                        label    = { Text("ملاحظات") },
                        leadingIcon = { Icon(Icons.Default.Notes, null) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                state.error?.let { err ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(8.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.width(8.dp))
                            Text(err, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Button(
                    onClick  = { viewModel.createCase() },
                    enabled  = !state.isSaving && state.licensePlate.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Blue600)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text("إنشاء الملف", style = MaterialTheme.typography.titleSmall)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Blue600)
            HorizontalDivider()
            content()
        }
    }
}
