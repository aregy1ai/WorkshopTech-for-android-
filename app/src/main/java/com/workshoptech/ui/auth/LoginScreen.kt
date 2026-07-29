package com.workshoptech.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workshoptech.auth.AuthManager
import com.workshoptech.data.entity.TechnicianEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(technicians: List<TechnicianEntity>, onLoginSuccess: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val authManager = remember { AuthManager(context) }
    var attempts by remember { mutableStateOf(0) }
    val isLocked = attempts >= 5

    Scaffold(topBar = { TopAppBar(title = { Text("ورشة تك") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary)) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.DirectionsCar, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))
            Text("تسجيل الدخول", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("أدخل الرقم السري للمتابعة", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            OutlinedTextField(value = pin, onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) { pin = it; errorMessage = null } }, label = { Text("الرقم السري") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !isLocked, isError = errorMessage != null, supportingText = { errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) } })
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                if (pin.length >= 4) {
                    if (authManager.login(pin)) onLoginSuccess()
                    else { attempts++; errorMessage = if (attempts >= 5) "تم قفل التطبيق. حاول بعد 5 دقائق" else "رقم سري غير صحيح. متبقي ${5 - attempts} محاولات" }
                }
            }, modifier = Modifier.fillMaxWidth().height(52.dp), enabled = pin.length >= 4 && !isLocked) { Text("دخول", fontSize = 16.sp) }
            if (technicians.isNotEmpty()) { Spacer(Modifier.height(24.dp)); Text("اختيار سريع (للتطوير فقط):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(8.dp)); technicians.take(3).forEach { TextButton(onClick = { pin = "0000" }) { Text("${it.name} (${it.role})") } } }
        }
    }
}

@Composable
fun LockScreen(technicians: List<TechnicianEntity>, onUnlockSuccess: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val authManager = remember { AuthManager(context) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(Modifier.padding(32.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, null, Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text("تم قفل التطبيق", fontWeight = FontWeight.Bold)
                Text("أدخل الرقم السري للمتابعة", fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = pin, onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it }, label = { Text("الرقم السري") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true, isError = errorMessage != null)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { if (authManager.login(pin)) onUnlockSuccess() else errorMessage = "رقم سري غير صحيح" }, enabled = pin.length >= 4) { Text("فتح") }
            }
        }
    }
}
