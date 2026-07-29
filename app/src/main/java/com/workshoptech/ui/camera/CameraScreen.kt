package com.workshoptech.ui.camera

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(onPhotosCaptured: (List<Uri>) -> Unit = {}, onPlateRecognized: (String) -> Unit = {}, onNavigateBack: () -> Unit) {
    var currentAngle by remember { mutableStateOf(0) }
    val angles = listOf("أمام", "خلف", "جانب أيسر", "جانب أيمن", "لوحة")
    val photos = remember { mutableStateListOf<Uri>() }

    Scaffold(topBar = { TopAppBar(title = { Text("تصوير المركبة - ${angles[currentAngle]}") }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "رجوع") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text("الكاميرا", fontSize = 18.sp)
                    Text("سيتم تفعيل الكاميرا على الجهاز الحقيقي", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                angles.forEachIndexed { index, name -> FilterChip(selected = currentAngle == index, onClick = { currentAngle = index }, label = { Text(name, fontSize = 11.sp) }, modifier = Modifier.weight(1f)) }
            }
            Button(onClick = {
                photos.add(Uri.parse("file://photo_${photos.size}.jpg"))
                if (currentAngle < angles.size - 1) currentAngle++ else { onPhotosCaptured(photos.toList()); onNavigateBack() }
            }, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Icon(Icons.Default.Camera, null); Spacer(Modifier.width(8.dp))
                Text(if (currentAngle < angles.size - 1) "التقط ${angles[currentAngle]} (${currentAngle + 1}/${angles.size})" else "إنهاء التصوير")
            }
        }
    }
}
