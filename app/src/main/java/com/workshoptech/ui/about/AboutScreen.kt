package com.workshoptech.ui.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onNavigateBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("عن التطبيق") }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "رجوع") } }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("AR-EGY", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00FF00))
            Text("ورشة تك", fontSize = 24.sp, color = Color(0xFFFF00FF))
            Spacer(modifier = Modifier.height(8.dp))
            Text("WorkshopTech v1.0", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            Text("نظام إدارة ورش تصليح السيارات", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            InfoRow("المصمم", "AR-EGY")
            InfoRow("التواصل", "aregy1ai@gmail.com")
            InfoRow("الإصدار", "1.0")
            InfoRow("Android", "8.0+")
            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            Text("الميزات الرئيسية:", fontWeight = FontWeight.Bold)
            FeatureItem("التعرف على اللوحات - 17 دولة عربية")
            FeatureItem("تحليل أضرار المركبات")
            FeatureItem("إدارة الملفات والعملاء")
            FeatureItem("تقارير وإحصائيات")
            FeatureItem("نسخ احتياطي مشفر")
            FeatureItem("تكامل واتساب")
            FeatureItem("نظام مصادقة آمن")
            Spacer(modifier = Modifier.height(32.dp))
            Text("© 2026 AR-EGY. جميع الحقوق محفوظة.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FeatureItem(text: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("• ", color = Color(0xFF00FF00))
        Text(text, fontSize = 13.sp)
    }
}
