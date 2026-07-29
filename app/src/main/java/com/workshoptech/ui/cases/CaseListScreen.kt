package com.workshoptech.ui.cases

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.workshoptech.WorkshopTechApp
import com.workshoptech.data.entity.CaseEntity
import com.workshoptech.viewmodel.CaseListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseListScreen(onNavigateBack: () -> Unit, onNavigateToCase: (String) -> Unit, onNavigateToCreate: () -> Unit) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as WorkshopTechApp
    val vm: CaseListViewModel = viewModel(factory = com.workshoptech.viewmodel.ViewModelFactory(app.repository))
    val searchQuery by vm.searchQuery.collectAsState()
    val cases = vm.cases.collectAsLazyPagingItems()

    Scaffold(
        topBar = { TopAppBar(title = { Text("ملفات السيارات") }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "رجوع") } }, actions = { IconButton(onClick = onNavigateToCreate) { Icon(Icons.Default.Add, "ملف جديد") } }) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(value = searchQuery, onValueChange = { vm.setQuery(it) }, label = { Text("بحث باللوحة...") }, leadingIcon = { Icon(Icons.Default.Search, null) }, trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Default.Clear, "مسح") } }, modifier = Modifier.fillMaxWidth().padding(16.dp), singleLine = true)
            when {
                cases.loadState.refresh is LoadState.Loading && cases.itemCount == 0 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                cases.loadState.refresh is LoadState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("حدث خطأ", color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp)); Button(onClick = { cases.retry() }) { Text("إعادة المحاولة") } } }
                cases.loadState.refresh is LoadState.NotLoading && cases.itemCount == 0 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("لا توجد ملفات") }
                else -> LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(count = cases.itemCount, key = cases.itemKey { it.caseId }) { index -> cases[index]?.let { CaseListItem(it) { onNavigateToCase(it.caseId) } } }
                    when (cases.loadState.append) {
                        is LoadState.Loading -> item(key = "loading") { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) } }
                        is LoadState.Error -> item(key = "error") { Button(onClick = { cases.retry() }, modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text("إعادة تحميل المزيد") } }
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun CaseListItem(case: CaseEntity, onClick: () -> Unit) {
    val statusColor = when (case.status) { "DELIVERED" -> MaterialTheme.colorScheme.tertiary; "IN_PROGRESS" -> MaterialTheme.colorScheme.primary; "ON_HOLD" -> MaterialTheme.colorScheme.error; else -> MaterialTheme.colorScheme.onSurface }
    Card(Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(case.plateText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(case.vehicleFreeText ?: listOfNotNull(case.vehicleMakeId, case.vehicleModelId, case.vehicleYear?.toString()).joinToString(" "), style = MaterialTheme.typography.bodyMedium)
                Text(case.status, style = MaterialTheme.typography.bodySmall, color = statusColor)
            }
            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.align(Alignment.CenterVertically))
        }
    }
}
