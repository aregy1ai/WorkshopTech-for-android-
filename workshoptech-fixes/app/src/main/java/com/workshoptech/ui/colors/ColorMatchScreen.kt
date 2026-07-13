package com.workshoptech.ui.colors

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.workshoptech.ui.common.WorkshopTopBar
import com.workshoptech.ui.theme.*
import kotlin.math.*

@Composable
fun ColorMatchScreen(onNavigateBack: () -> Unit) {
    var colorCode   by remember { mutableStateOf("") }
    var vehicleAge  by remember { mutableStateOf(5f) }
    var sunExposure by remember { mutableStateOf(1) }  // 0=low 1=medium 2=high
    var deltaE      by remember { mutableStateOf<Float?>(null) }
    var mixture     by remember { mutableStateOf<String?>(null) }

    val scroll = rememberScrollState()

    Scaffold(topBar = { WorkshopTopBar("مطابقة الألوان", onBack = onNavigateBack) }) { padding ->
        Column(
            Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize()
                .verticalScroll(scroll).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── Color code input ──────────────────────────────────────────────
            ColorInputCard(colorCode = colorCode, onCodeChange = { colorCode = it.uppercase() })

            // ── Vehicle age slider ────────────────────────────────────────────
            AgingCard(vehicleAge = vehicleAge, onAgeChange = { vehicleAge = it })

            // ── Sun exposure ──────────────────────────────────────────────────
            SunExposureCard(selected = sunExposure, onSelect = { sunExposure = it })

            // ── Calculate button ──────────────────────────────────────────────
            Button(
                onClick = {
                    val result = calculateMixture(colorCode, vehicleAge, sunExposure)
                    mixture = result.first
                    deltaE  = result.second
                },
                enabled  = colorCode.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Blue600)
            ) {
                Icon(Icons.Default.Palette, null)
                Spacer(Modifier.width(8.dp))
                Text("احسب الخلطة", style = MaterialTheme.typography.titleSmall)
            }

            // ── Result ────────────────────────────────────────────────────────
            if (mixture != null && deltaE != null) {
                MixtureResult(mixture = mixture!!, deltaE = deltaE!!)
            }
        }
    }
}

@Composable
private fun ColorInputCard(colorCode: String, onCodeChange: (String) -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("كود اللون", style = MaterialTheme.typography.titleSmall, color = Blue600)
            OutlinedTextField(
                value = colorCode,
                onValueChange = onCodeChange,
                label    = { Text("مثال: 070 White Pearl") },
                leadingIcon = { Icon(Icons.Default.ColorLens, null) },
                modifier = Modifier.fillMaxWidth()
            )
            // Color swatch preview
            if (colorCode.isNotBlank()) {
                val swatchColor = colorFromCode(colorCode)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                            .background(swatchColor)
                            .border(1.dp, Gray400, RoundedCornerShape(8.dp))
                    )
                    Text("معاينة اللون", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AgingCard(vehicleAge: Float, onAgeChange: (Float) -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row {
                Text("عمر السيارة", style = MaterialTheme.typography.titleSmall, color = Blue600, modifier = Modifier.weight(1f))
                Text("${vehicleAge.roundToInt()} سنة", style = MaterialTheme.typography.labelMedium, color = Orange600)
            }
            Spacer(Modifier.height(4.dp))
            Slider(
                value = vehicleAge,
                onValueChange = onAgeChange,
                valueRange = 0f..25f,
                steps  = 24,
                colors = SliderDefaults.colors(thumbColor = Orange600, activeTrackColor = Orange600)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("جديدة", style = MaterialTheme.typography.labelSmall, color = Gray600)
                Text("25 سنة", style = MaterialTheme.typography.labelSmall, color = Gray600)
            }
        }
    }
}

@Composable
private fun SunExposureCard(selected: Int, onSelect: (Int) -> Unit) {
    val options = listOf("منخفض" to Icons.Default.WbCloudy, "متوسط" to Icons.Default.WbSunny, "عالٍ" to Icons.Default.Brightness7)
    Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("التعرض للشمس", style = MaterialTheme.typography.titleSmall, color = Blue600)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEachIndexed { i, (label, icon) ->
                    FilterChip(
                        selected  = selected == i,
                        onClick   = { onSelect(i) },
                        label     = { Text(label) },
                        leadingIcon = { Icon(icon, null, Modifier.size(16.dp)) },
                        modifier  = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MixtureResult(mixture: String, deltaE: Float) {
    val (grade, gradeColor) = when {
        deltaE < 2.0f -> "ممتاز ✓" to Green700
        deltaE < 3.5f -> "مقبول"   to Yellow700
        else          -> "غير مقبول ✗" to Red700
    }
    val animColor by animateColorAsState(gradeColor, label = "deltaE_color")

    Card(
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = gradeColor.copy(alpha = 0.08f)),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Science, null, tint = animColor, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("نتيجة المطابقة", style = MaterialTheme.typography.titleSmall)
            }
            HorizontalDivider()
            ResultRow("الخلطة المقترحة", mixture)
            ResultRow("Delta E", "${"%.2f".format(deltaE)}")
            Surface(color = animColor.copy(0.15f), shape = RoundedCornerShape(8.dp)) {
                Text(grade, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = animColor, style = MaterialTheme.typography.titleSmall)
            }
            Text(
                when {
                    deltaE < 2.0f -> "اللون مطابق بشكل ممتاز. يمكن الرش مباشرة."
                    deltaE < 3.5f -> "اللون مقبول. يُنصح باختبار على البطاقة في الشمس."
                    else          -> "الفرق كبير. أعد ضبط الخلطة."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

// ── Utility ───────────────────────────────────────────────────────────────────

private fun calculateMixture(code: String, age: Float, sun: Int): Pair<String, Float> {
    val fadePercent = (age / 25f) * (0.3f + sun * 0.15f)
    val lightenAdd  = (fadePercent * 100).roundToInt()
    val base = code.substringBefore(" ").ifBlank { code }
    val mixture = "$base + W${lightenAdd.coerceIn(0, 30)}%"
    // Simulate Delta E based on fade
    val dE = (1.0f + fadePercent * 4.5f + (-0.3f..0.3f).random().toFloat()).coerceIn(0.5f, 6f)
    return mixture to dE
}

private fun colorFromCode(code: String): Color {
    val hash = code.hashCode()
    val r = ((hash shr 16) and 0xFF).toFloat() / 255f
    val g = ((hash shr 8)  and 0xFF).toFloat() / 255f
    val b = (hash          and 0xFF).toFloat() / 255f
    return Color(r.coerceIn(0.2f, 0.9f), g.coerceIn(0.2f, 0.9f), b.coerceIn(0.2f, 0.9f))
}

private fun Float.roundToInt() = kotlin.math.roundToInt(this)
private fun ClosedFloatingPointRange<Float>.random() = start + (endInclusive - start) * Math.random()
