package com.workshoptech.screenshot

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.workshoptech.data.entity.InspectionEntity
import com.workshoptech.data.entity.InspectionStatus
import com.workshoptech.data.entity.InspectionType
import com.workshoptech.ui.common.*
import com.workshoptech.ui.theme.*
import org.junit.Rule
import org.junit.Test

/**
 * Paparazzi screenshot tests for Inspection UI states (T1–T6 quality checkpoints).
 *
 * Covers:
 *  - Loading
 *  - Empty (no inspections for case)
 *  - All 6 checkpoints listed (T1–T6) with mixed statuses
 *  - All PASSED (green) — light + dark
 *  - All PENDING (amber) — light + dark
 *  - FAILED checkpoint (red) — light + dark
 *  - T4/T5 with deltaE colour-match value
 */
class InspectionScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig  = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.V_SCROLL,
        showSystemUi  = false
    )

    // ── Fake data ─────────────────────────────────────────────────────────────

    private fun inspection(
        id:       String,
        type:     String,
        status:   String  = InspectionStatus.PENDING,
        deltaE:   Float?  = null,
        notes:    String? = null
    ) = InspectionEntity(
        inspectionId = id,
        caseId       = "case-1",
        type         = type,
        status       = status,
        deltaE       = deltaE,
        notes        = notes,
        createdAt    = 0L
    )

    // ── Stateless content composable ──────────────────────────────────────────

    @Composable
    private fun InspectionContent(
        inspections: List<InspectionEntity> = emptyList(),
        isLoading:   Boolean                = false,
        caseTitle:   String                 = "LY-1234"
    ) {
        Scaffold(
            topBar = {
                WorkshopTopBar(
                    title  = "فحوصات — $caseTitle",
                    onBack = {}
                )
            }
        ) { padding ->
            when {
                isLoading -> LoadingScreen()
                inspections.isEmpty() -> EmptyState(
                    icon     = Icons.Default.CheckCircle,
                    title    = "لا توجد فحوصات",
                    subtitle = "لم يبدأ أي نقطة فحص لهذا الملف"
                )
                else -> LazyColumn(
                    contentPadding      = PaddingValues(
                        horizontal = 16.dp,
                        top        = padding.calculateTopPadding() + 16.dp,
                        bottom     = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(inspections, key = { it.inspectionId }) { insp ->
                        InspectionCard(insp = insp)
                    }
                }
            }
        }
    }

    @Composable
    private fun InspectionCard(insp: InspectionEntity) {
        val (statusColor, statusIcon) = when (insp.status) {
            InspectionStatus.PASSED  -> Green700 to Icons.Default.CheckCircle
            InspectionStatus.FAILED  -> Red700   to Icons.Default.Cancel
            InspectionStatus.SKIPPED -> Gray600  to Icons.Default.SkipNext
            else                     -> Orange600 to Icons.Default.HourglassEmpty
        }

        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                Modifier.padding(14.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint     = statusColor,
                            modifier = Modifier.padding(8.dp).size(24.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(InspectionType.labelAr(insp.type), style = MaterialTheme.typography.titleSmall)
                        Text(
                            InspectionStatus.labelAr(insp.status),
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor
                        )
                        insp.notes?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // DeltaE badge for colour-match checkpoints
                insp.deltaE?.let { de ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (de < 2f) Green700.copy(0.15f) else Red700.copy(0.15f)
                    ) {
                        Text(
                            "ΔE ${String.format("%.1f", de)}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style    = MaterialTheme.typography.labelSmall,
                            color    = if (de < 2f) Green700 else Red700
                        )
                    }
                }
            }
        }
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test fun inspection_loading_light() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                InspectionContent(isLoading = true)
            }
        }
    }

    @Test fun inspection_empty_light() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                InspectionContent()
            }
        }
    }

    @Test fun inspection_all_pending_light() {
        val inspections = InspectionType.all.mapIndexed { i, type ->
            inspection("p$i", type, InspectionStatus.PENDING)
        }
        paparazzi.snapshot {
            WorkshopTechTheme {
                InspectionContent(inspections = inspections)
            }
        }
    }

    @Test fun inspection_all_pending_dark() {
        val inspections = InspectionType.all.mapIndexed { i, type ->
            inspection("p$i", type, InspectionStatus.PENDING)
        }
        paparazzi.snapshot {
            WorkshopTechTheme(darkTheme = true) {
                InspectionContent(inspections = inspections)
            }
        }
    }

    @Test fun inspection_all_passed_light() {
        val inspections = InspectionType.all.mapIndexed { i, type ->
            inspection("a$i", type, InspectionStatus.PASSED)
        }
        paparazzi.snapshot {
            WorkshopTechTheme {
                InspectionContent(inspections = inspections)
            }
        }
    }

    @Test fun inspection_all_passed_dark() {
        val inspections = InspectionType.all.mapIndexed { i, type ->
            inspection("a$i", type, InspectionStatus.PASSED)
        }
        paparazzi.snapshot {
            WorkshopTechTheme(darkTheme = true) {
                InspectionContent(inspections = inspections)
            }
        }
    }

    @Test fun inspection_mixed_statuses_light() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                InspectionContent(
                    inspections = listOf(
                        inspection("t1", InspectionType.T1, InspectionStatus.PASSED),
                        inspection("t2", InspectionType.T2, InspectionStatus.PASSED),
                        inspection("t3", InspectionType.T3, InspectionStatus.FAILED, notes = "خدش على الباب الأمامي"),
                        inspection("t4", InspectionType.T4, InspectionStatus.PENDING),
                        inspection("t5", InspectionType.T5, InspectionStatus.PENDING),
                        inspection("t6", InspectionType.T6, InspectionStatus.PENDING)
                    )
                )
            }
        }
    }

    @Test fun inspection_mixed_statuses_dark() {
        paparazzi.snapshot {
            WorkshopTechTheme(darkTheme = true) {
                InspectionContent(
                    inspections = listOf(
                        inspection("t1", InspectionType.T1, InspectionStatus.PASSED),
                        inspection("t2", InspectionType.T2, InspectionStatus.PASSED),
                        inspection("t3", InspectionType.T3, InspectionStatus.FAILED),
                        inspection("t4", InspectionType.T4, InspectionStatus.PENDING),
                        inspection("t5", InspectionType.T5, InspectionStatus.PENDING),
                        inspection("t6", InspectionType.T6, InspectionStatus.SKIPPED)
                    )
                )
            }
        }
    }

    @Test fun inspection_with_delta_e_good_match_light() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                InspectionContent(
                    inspections = listOf(
                        inspection("t4", InspectionType.T4, InspectionStatus.PASSED, deltaE = 1.2f),
                        inspection("t5", InspectionType.T5, InspectionStatus.PASSED, deltaE = 0.8f)
                    )
                )
            }
        }
    }

    @Test fun inspection_with_delta_e_poor_match_light() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                InspectionContent(
                    inspections = listOf(
                        inspection("t4", InspectionType.T4, InspectionStatus.FAILED, deltaE = 4.7f, notes = "تطابق اللون ضعيف — أعد الرش"),
                        inspection("t5", InspectionType.T5, InspectionStatus.PENDING)
                    )
                )
            }
        }
    }

    @Test fun inspection_failed_with_notes_dark() {
        paparazzi.snapshot {
            WorkshopTechTheme(darkTheme = true) {
                InspectionContent(
                    inspections = listOf(
                        inspection("t1", InspectionType.T1, InspectionStatus.FAILED,
                            notes = "خدوش على الباب الخلفي الأيسر ووجود صدمة بسيطة في المقدمة")
                    )
                )
            }
        }
    }
}
