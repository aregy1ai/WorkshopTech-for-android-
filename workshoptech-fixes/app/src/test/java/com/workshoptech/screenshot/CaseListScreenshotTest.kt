package com.workshoptech.screenshot

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.workshoptech.data.entity.CaseEntity
import com.workshoptech.data.entity.CaseStatus
import com.workshoptech.ui.common.*
import com.workshoptech.ui.theme.*
import org.junit.Rule
import org.junit.Test

/**
 * Paparazzi screenshot tests for Case List UI states.
 *
 * Stateless [CaseListContent] composable mirrors [CaseListScreen] content
 * without requiring a ViewModel — receives data directly for hermetic rendering.
 *
 * State variants:
 *  - Loading
 *  - Empty (no cases at all)
 *  - Empty search result
 *  - Populated: 5 cases, no filter (light + dark)
 *  - Populated: filter active (IN_PROGRESS)
 *  - Long list: 15 cases (V_SCROLL rendering)
 *  - RTL layout (Arabic — Pixel 5)
 */
class CaseListScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig  = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.V_SCROLL,
        showSystemUi  = false
    )

    // ── Fake data ─────────────────────────────────────────────────────────────

    private fun fakeCase(
        id:     String,
        plate:  String = "LY-$id",
        make:   String = "تويوتا",
        model:  String = "كامري",
        status: String = CaseStatus.IN_PROGRESS
    ) = CaseEntity(
        caseId       = id,
        customerId   = "cust-1",
        licensePlate = plate,
        make         = make,
        model        = model,
        status       = status,
        createdAt    = 0L,
        updatedAt    = 0L
    )

    private val statusFilters = listOf(
        null to "الكل",
        "NEW"                to "جديد",
        "IN_PROGRESS"        to "قيد التنفيذ",
        "READY_FOR_DELIVERY" to "جاهز",
        "DELIVERED"          to "مُسلّم"
    )

    // ── Stateless content composable ──────────────────────────────────────────

    @Composable
    private fun CaseListContent(
        cases:        List<CaseEntity> = emptyList(),
        query:        String           = "",
        statusFilter: String?          = null,
        isLoading:    Boolean          = false,
        error:        String?          = null
    ) {
        Scaffold(
            topBar = {
                WorkshopTopBar(
                    title  = "الملفات",
                    onBack = {},
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Add, "إضافة")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {}, containerColor = Orange600) {
                    Icon(Icons.Default.Add, "ملف جديد", tint = White)
                }
            }
        ) { padding ->
            Column(
                Modifier
                    .padding(top = padding.calculateTopPadding())
                    .fillMaxSize()
            ) {
                // Search
                OutlinedTextField(
                    value         = query,
                    onValueChange = {},
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder   = { Text("بحث عن لوحة أو سيارة…") },
                    leadingIcon   = { Icon(Icons.Default.Search, null) },
                    shape         = RoundedCornerShape(12.dp),
                    singleLine    = true
                )

                // Status filter chips
                LazyRow(
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(statusFilters) { (key, label) ->
                        FilterChip(
                            selected = statusFilter == key,
                            onClick  = {},
                            label    = { Text(label) }
                        )
                    }
                }

                error?.let {
                    ErrorBanner(message = it, onDismiss = {}, modifier = Modifier.padding(horizontal = 16.dp))
                }

                when {
                    isLoading -> LoadingScreen()
                    cases.isEmpty() && query.isNotBlank() -> EmptyState(
                        icon     = Icons.Default.SearchOff,
                        title    = "لا توجد نتائج لـ «$query»",
                        subtitle = "جرّب بحثاً مختلفاً"
                    )
                    cases.isEmpty() -> EmptyState(
                        icon        = Icons.Default.Build,
                        title       = "لا توجد حالات",
                        subtitle    = "اضغط + لإنشاء ملف جديد",
                        actionLabel = "ملف جديد",
                        onAction    = {}
                    )
                    else -> LazyColumn(
                        contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement   = Arrangement.spacedBy(8.dp)
                    ) {
                        items(cases, key = { it.caseId }) { case ->
                            CaseCard(case = case)
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    @Composable
    private fun CaseCard(case: CaseEntity) {
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                Modifier.padding(12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(case.licensePlate, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${case.make} ${case.model}".trim(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusChip(status = case.status)
            }
        }
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test fun caseList_loading_light() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                CaseListContent(isLoading = true)
            }
        }
    }

    @Test fun caseList_empty_light() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                CaseListContent()
            }
        }
    }

    @Test fun caseList_empty_dark() {
        paparazzi.snapshot {
            WorkshopTechTheme(darkTheme = true) {
                CaseListContent()
            }
        }
    }

    @Test fun caseList_empty_search_result_light() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                CaseListContent(cases = emptyList(), query = "BMW-XYZ")
            }
        }
    }

    @Test fun caseList_populated_5_cases_light() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                CaseListContent(
                    cases = listOf(
                        fakeCase("1", "LY-1234", status = CaseStatus.NEW),
                        fakeCase("2", "LY-5678", status = CaseStatus.IN_PROGRESS),
                        fakeCase("3", "KSA-001", status = CaseStatus.APPROVED),
                        fakeCase("4", "UAE-555", status = CaseStatus.READY_FOR_DELIVERY),
                        fakeCase("5", "EG-9999", status = CaseStatus.DELIVERED)
                    )
                )
            }
        }
    }

    @Test fun caseList_populated_5_cases_dark() {
        paparazzi.snapshot {
            WorkshopTechTheme(darkTheme = true) {
                CaseListContent(
                    cases = listOf(
                        fakeCase("1", "LY-1234", status = CaseStatus.NEW),
                        fakeCase("2", "LY-5678", status = CaseStatus.IN_PROGRESS),
                        fakeCase("3", "KSA-001", status = CaseStatus.APPROVED)
                    )
                )
            }
        }
    }

    @Test fun caseList_status_filter_active_inProgress_light() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                CaseListContent(
                    cases        = listOf(
                        fakeCase("a", status = CaseStatus.IN_PROGRESS),
                        fakeCase("b", status = CaseStatus.IN_PROGRESS)
                    ),
                    statusFilter = CaseStatus.IN_PROGRESS
                )
            }
        }
    }

    @Test fun caseList_search_active_with_results_light() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                CaseListContent(
                    cases = listOf(fakeCase("bmw", "BMW-001", "بي إم دبليو", "X5")),
                    query = "BMW"
                )
            }
        }
    }

    @Test fun caseList_long_list_15_cases_light() {
        val cases = (1..15).map { i ->
            fakeCase(
                id     = "$i",
                plate  = "LY-${1000 + i}",
                status = when (i % 5) {
                    0    -> CaseStatus.NEW
                    1    -> CaseStatus.IN_PROGRESS
                    2    -> CaseStatus.APPROVED
                    3    -> CaseStatus.READY_FOR_DELIVERY
                    else -> CaseStatus.DELIVERED
                }
            )
        }
        paparazzi.snapshot {
            WorkshopTechTheme {
                CaseListContent(cases = cases)
            }
        }
    }

    @Test fun caseList_error_state_light() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                CaseListContent(
                    cases = emptyList(),
                    error = "تعذّر تحميل الملفات. تحقق من الاتصال."
                )
            }
        }
    }

    @Test fun caseList_mixed_statuses_dark() {
        paparazzi.snapshot {
            WorkshopTechTheme(darkTheme = true) {
                CaseListContent(
                    cases = listOf(
                        fakeCase("x1", "LY-0001", status = CaseStatus.ON_HOLD),
                        fakeCase("x2", "LY-0002", status = CaseStatus.CANCELLED),
                        fakeCase("x3", "LY-0003", status = CaseStatus.NEW)
                    )
                )
            }
        }
    }
}
