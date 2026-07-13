package com.workshoptech.ui.camera

import android.content.Context
import android.net.Uri
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.*
import com.workshoptech.ui.theme.*
import com.workshoptech.util.FileManager
import java.util.concurrent.Executor

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    caseId:          String,
    mode:            String = "general",
    onImageCaptured: (Uri) -> Unit,
    onBack:          () -> Unit
) {
    val permState = rememberPermissionState(android.Manifest.permission.CAMERA)

    when {
        permState.status.isGranted -> CameraContent(caseId, mode, onImageCaptured, onBack)
        permState.status.shouldShowRationale -> {
            CameraPermissionRationale(
                onRequest = { permState.launchPermissionRequest() },
                onBack    = onBack
            )
        }
        else -> {
            LaunchedEffect(Unit) { permState.launchPermissionRequest() }
            CameraPermissionRationale(
                onRequest = { permState.launchPermissionRequest() },
                onBack    = onBack
            )
        }
    }
}

@Composable
private fun CameraContent(
    caseId:          String,
    mode:            String,
    onImageCaptured: (Uri) -> Unit,
    onBack:          () -> Unit
) {
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var flashEnabled by remember { mutableStateOf(false) }
    var isTakingPhoto by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val modeLabel = when (mode) {
        "plate"  -> "تصوير اللوحة"
        "damage" -> "تصوير الضرر"
        "video"  -> "تسجيل فيديو"
        else     -> "تصوير"
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        // Camera preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { pv ->
                    val providerFuture = ProcessCameraProvider.getInstance(ctx)
                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        val preview  = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
                        val capture  = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                            .build()
                        imageCapture = capture
                        try {
                            provider.unbindAll()
                            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                        } catch (_: Exception) { }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 48.dp).align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.background(Color.Black.copy(0.4f), CircleShape)) {
                Icon(Icons.Default.ArrowBack, null, tint = White)
            }
            Spacer(Modifier.width(8.dp))
            Surface(color = Color.Black.copy(0.4f), shape = RoundedCornerShape(20.dp)) {
                Text(modeLabel, Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = White, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = { flashEnabled = !flashEnabled },
                modifier = Modifier.background(Color.Black.copy(0.4f), CircleShape)
            ) {
                Icon(if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff, null, tint = White)
            }
        }

        // Mode overlay guide
        when (mode) {
            "plate" -> PlateGuideOverlay()
            "damage" -> DamageGuideOverlay()
        }

        // Bottom controls
        Column(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            errorMsg?.let { msg ->
                Surface(color = Color.Red.copy(0.8f), shape = RoundedCornerShape(8.dp)) {
                    Text(msg, Modifier.padding(horizontal = 16.dp, vertical = 6.dp), color = White, style = MaterialTheme.typography.labelSmall)
                }
            }

            // Shutter
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(80.dp).clip(CircleShape)
                        .background(if (isTakingPhoto) Gray400 else White)
                        .border(4.dp, White.copy(0.6f), CircleShape)
                )
                IconButton(
                    onClick = {
                        if (!isTakingPhoto) {
                            isTakingPhoto = true
                            errorMsg = null
                            takePhoto(context, imageCapture, ContextCompat.getMainExecutor(context),
                                onSuccess = { uri ->
                                    isTakingPhoto = false
                                    onImageCaptured(uri)
                                },
                                onError = { msg ->
                                    isTakingPhoto = false
                                    errorMsg = msg
                                }
                            )
                        }
                    },
                    modifier = Modifier.size(80.dp)
                ) {
                    if (isTakingPhoto) {
                        CircularProgressIndicator(Modifier.size(32.dp), color = Blue600, strokeWidth = 3.dp)
                    }
                }
            }
        }
    }
}

private fun takePhoto(
    context:   Context,
    capture:   ImageCapture?,
    executor:  Executor,
    onSuccess: (Uri) -> Unit,
    onError:   (String) -> Unit
) {
    val photoFile    = FileManager.createPhotoFile(context)
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    capture?.takePicture(outputOptions, executor, object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
            onSuccess(Uri.fromFile(photoFile))
        }
        override fun onError(exc: ImageCaptureException) {
            onError(exc.message ?: "فشل الالتقاط")
        }
    }) ?: onError("الكاميرا غير جاهزة")
}

@Composable
private fun PlateGuideOverlay() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxWidth(0.85f).height(120.dp)
                .border(2.dp, Yellow700, RoundedCornerShape(8.dp))
        )
        Text(
            "ضع اللوحة داخل الإطار",
            Modifier.align(Alignment.Center).offset(y = 80.dp)
                .background(Color.Black.copy(0.5f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            color = White, style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun DamageGuideOverlay() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.7f)
                .border(2.dp, Orange500, RoundedCornerShape(8.dp))
        )
        Text(
            "صوّر منطقة الضرر كاملة",
            Modifier.align(Alignment.Center).offset(y = 220.dp)
                .background(Color.Black.copy(0.5f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            color = White, style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun CameraPermissionRationale(onRequest: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.CameraAlt, null, Modifier.size(72.dp), tint = Blue600)
        Spacer(Modifier.height(16.dp))
        Text("يلزم إذن الكاميرا", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("لتصوير السيارات واللوحات يحتاج التطبيق إذن الكاميرا", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequest) { Text("منح الإذن") }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) { Text("رجوع") }
    }
}
