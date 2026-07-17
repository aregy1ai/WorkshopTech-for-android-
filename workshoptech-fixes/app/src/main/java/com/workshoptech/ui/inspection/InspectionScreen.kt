package com.workshoptech.ui.inspection

import androidx.compose.animation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.workshoptech.data.entity.InspectionEntity
import com.workshoptech.ui.common.*
import com.workshoptech.ui.theme.*
import com.workshoptech.viewmodel.InspectionCheckpoint
import com.workshoptech.viewmodel.InspectionViewModel

// ─── Checklist items per checkpoint ──────────────────────────────────────────

private val CHECKLISTS: Map<InspectionCheckpoint, List<String>> = mapOf(
    InspectionCheckpoint.T1 to listOf(
        "تصوير اللوحة", "تصوير العداد", "تصوير الضرر (3 زوايا)",
        "تصوير شامل (6 جهات)", "توثيق الخدوش القديمة",
        "فحص الإطارات", "توقيع العميل", "فحص AI للأضرار"
    ),
    InspectionCheckpoint.T2 to listOf(
        "استواء السطح", "تناسق خطوط التصميم",
        "سماكة المعجون < 3 مم", "لا زوايا حادة",
        "لا فقاعات", "صنفرة جيدة", "حواف نظيفة",
        "تجربة تركيب", "الأجزاء المتحركة تعمل"
    ),
    InspectionCheckpoint.T3 to listOf(
        "معاينة فني الدهان", "توقيع استلام",
        "اتفاق مناطق الدهان", "تحديد اللون والخلطة",
        "تغطية المناطق", "تنظيف السطح",
        "درجة حرارة 18–25°", "رطوبة < 60%"
    ),
    InspectionCheckpoint.T4 to listOf(
        "اختبار الخلطة على بطاقة", "تجفيف البطاقة",
        "مقارنة اللون (شمس + مصباح)", "موافقة على اللون",
        "تسجيل كود الخلطة", "حساب كمية الدهان",
        "تجهيز مسدس الرش", "تنظيف منطقة الرش", "تثبيت صحيح"
    ),
    InspectionCheckpoint.T5 to listOf(
        "لا ترهل (sags)", "لا تجعّد (orange peel)", "لا حبوب (seeds)", "لا بقع (fisheye)",
        "سماكة طبقة الطلاء", "جودة البريق", "لا خدوش تحت الطلاء",
        "لا فقاعات هواء", "لا تسرب حواف",
        "انتظام الحواف", "سمك حواف الأبواب", "لا تقشر",
        "إخفاء منطقة التغطية", "تطابق اللون الإجمالي",
        "مقارنة قبل/بعد", "موافقة مراقب الجودة",
        "توقيع الفني", "التوثيق الفوتوغرافي"
    ),
    InspectionCheckpoint.T6 to listOf(
        "تركيب جميع الأجزاء", "تثبيت المسامير",
        "سلاسة الفتح/الإغلاق", "عمل النوافذ",
        "تلميع", "تنظيف",
        "مقارنة قبل/بعد", "صور نهائية",
        "صور مقربة للحواف", "تقرير التسليم"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionScreen(
    caseId:         String,
    viewModel:      InspectionViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var activeCheckpoint by remember { mutableStateOf<InspectionCheckpoint?>(null) }

    LaunchedEffect(caseId) { viewModel.loadForCase(caseId) }

    LaunchedEffect(state.savedSuccess) {
        if (state.savedSuccess) {
            activeCheckpoint = null
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        topBar = { WorkshopTopBar("نقاط التفتيش", onBack = onNavigateBack) }
    ) { padding ->
        Column(Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize()) {

            CheckpointProgressRow(
                inspections = state.inspections,
                onSelectCheckpoint = { cp ->
                    activeCheckpoint = cp
                    viewModel.openInspection(caseId, cp)
                }
            )

            HorizontalDivider()

            AnimatedContent(
                targetState = activeCheckpoint,
                transitionSpec = { slideInHorizontally { it } togetherWith slideOutHorizontally { -it } },
                label = "insp_content"
            ) { cp ->
                if (cp == null) {
                    InspectionOverview(state.inspections)
                } else {
                    ChecklistEditor(
                        checkpoint = cp,
                        existing   = state.currentInspection,
                        isSaving   = state.isSaving,
                        onSave     = { status, checklistJson, notes ->
                            val insp = state.currentInspection
                            if (insp != null) {
                                viewModel.saveInspection(
                                    inspectionId  = insp.inspectionId,
                                    caseId        = caseId,
                                    type          = cp.name,
                                    status        = status,
                                    inspectedBy   = null,
                                    checklistJson = checklistJson,
                                    notes         = notes,
                                    photoIds      = null
                                )
                            }
                        },
                        onCancel = { activeCheckpoint = null }
                    )
                }
            }
        }
    }
}

// ─── Progress row ────────────────────────────────────────────────────────────

@Composable
private fun CheckpointProgressRow(
    inspections: List<InspectionEntity>,
    onSelectCheckpoint: (InspectionCheckpoint) -> Unit
) {
    val statusMap = inspections.associate { it.type to it.status }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        InspectionCheckpoint.entries.forEach { cp ->
            val status = statusMap[cp.name]
            val (bg, fg) = when (status) {
                "PASSED" -> Green100 to Green700
                "FAILED" -> Red100   to Red700
                else     -> Gray200  to Gray700
            }
            Surface(
                onClick        = { onSelectCheckpoint(cp) },
                modifier       = Modifier.weight(1f),
                shape          = RoundedCornerShape(8.dp),
                color          = bg,
                tonalElevation = 2.dp
            ) {
                Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(cp.name, style = MaterialTheme.typography.labelLarge, color = fg)
                    Text(
                        when (status) { "PASSED" -> "✓" "FAILED" -> "✗" else -> "○" },
                        color = fg, style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }
}

// ─── Overview ────────────────────────────────────────────────────────────────

@Composable
private fun InspectionOverview(inspections: List<InspectionEntity>) {
    if (inspections.isEmpty()) {
        EmptyState(Icons.Default.Assignment, "اختر نقطة تفتيش أعلاه للبدء")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(inspections) { insp ->
            Card(shape = RoundedCornerShape(10.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    val icon = when (insp.status) {
                        "PASSED" -> Icons.Default.CheckCircle
                        "FAILED" -> Icons.Default.Cancel
                        else     -> Icons.Default.RadioButtonUnchecked
                    }
                    val tint = when (insp.status) { "PASSED" -> Green500 "FAILED" -> Red500 else -> Gray400 }
                    Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        val label = runCatching { InspectionCheckpoint.valueOf(insp.type).label }
                            .getOrDefault(insp.type)
                        Text(label, style = MaterialTheme.typography.titleSmall)
                        insp.notes?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

// ─── Checklist editor ────────────────────────────────────────────────────────

@Composable
private fun ChecklistEditor(
    checkpoint: InspectionCheckpoint,
    existing:   InspectionEntity?,
    isSaving:   Boolean,
    onSave:     (status: String, checklistJson: String, notes: String) -> Unit,
    onCancel:   () -> Unit
) {
    val items   = CHECKLISTS[checkpoint] ?: emptyList()
    val checked = remember { mutableStateMapOf<Int, Boolean>().apply { items.indices.forEach { put(it, false) } } }
    var notes   by remember { mutableStateOf(existing?.notes ?: "") }

    val passedCount = checked.values.count { it }
    val allPassed   = passedCount == items.size && items.isNotEmpty()

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Text(checkpoint.label, style = MaterialTheme.typography.titleMedium, color = Blue600)
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { if (items.isEmpty()) 0f else passedCount.toFloat() / items.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (allPassed) Green500 else Orange500
            )
            Text("$passedCount / ${items.size} بنود",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
        }

        items(items.size) { idx ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked         = checked[idx] ?: false,
                    onCheckedChange = { checked[idx] = it },
                    colors          = CheckboxDefaults.colors(checkedColor = Green500)
                )
                Text(items[idx], Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value         = notes,
                onValueChange = { notes = it },
                label         = { Text("ملاحظات الفحص") },
                minLines      = 2,
                modifier      = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("رجوع") }
                Button(
                    onClick  = {
                        val status = if (allPassed) "PASSED" else "IN_PROGRESS"
                        val json   = items.indices.joinToString(",") { i -> if (checked[i] == true) "1" else "0" }
                        onSave(status, json, notes)
                    },
                    enabled  = !isSaving,
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = if (allPassed) Green700 else Orange600
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = White, strokeWidth = 2.dp)
                    } else {
                        Text(if (allPassed) "✓ اجتاز" else "حفظ")
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
