package com.workshoptech.ui.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.workshoptech.ui.theme.Blue600
import com.workshoptech.ui.theme.White
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        delay(1800)
        onSplashFinished()
    }

    val scale by animateFloatAsState(
        targetValue  = if (visible) 1f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "splash_scale"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Blue600),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.scale(scale)
        ) {
            Icon(
                Icons.Default.Build,
                contentDescription = null,
                tint     = White,
                modifier = Modifier.size(80.dp)
            )
            AnimatedVisibility(visible = visible, enter = fadeIn() + slideInVertically()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "ورشة تك",
                        style = MaterialTheme.typography.displaySmall,
                        color = White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "WorkshopTech",
                        style = MaterialTheme.typography.titleMedium,
                        color = White.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "إدارة ورشتك بالذكاء الاصطناعي",
                        style = MaterialTheme.typography.bodyMedium,
                        color = White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = visible,
            enter   = fadeIn(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
        ) {
            CircularProgressIndicator(color = White.copy(alpha = 0.6f), strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
        }
    }
}
