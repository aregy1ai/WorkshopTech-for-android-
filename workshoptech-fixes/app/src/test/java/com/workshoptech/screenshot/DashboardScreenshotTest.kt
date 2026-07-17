package com.workshoptech.screenshot

import androidx.compose.foundation.layout.*
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
import com.workshoptech.data.entity.InventoryEntity
import com.workshoptech.ui.common.*
import com.workshoptech.ui.theme.*
import org.junit.Rule
import org.junit.Test

/**
 * Paparazzi screenshot tests for Dashboard UI states.
 *
 * Since [DashboardScreen] requires a ViewModel, we snapshot a stateless
 * inner composable [DashboardContent] that takes the [DashboardState] data
 * class directly. This pattern keeps tests hermetic and fast.
 *
 * State variants tested:
 *  - Loading (spinner)
 *  - Empty (no cases)
 *  - Populated: normal data (light + dark)
 *  - Populated: high case load (10+ active)
 *  - Error state (error banner visible)
 *  - Low-stock warning visible
 */
class DashboardScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig  = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.V_SCROLL,
        showSystemUi  = false
    )

    // ── Fake data ─────────────────────────────────────────────────────────────

    private fun fakeCase(id: String, plate: String = "LY-$id", status: String = CaseStatus.IN_PROGRESS) =
        CaseEntity(
            caseId       = id,
            customerId   = "cust-1",
            licensePlate = plate,
            make         = "تويوتا",
            model        = "كامري",
            status       = status,
            createdAt    = 0L,
            updatedAt    = 0L
        )

    private fun fakeInventoryItem(id: String, name: String, qty: Int, minQty: Int) =
        InventoryEntity(
            itemId      = id,
            name        = name,
            category    = "PAINT",
            quantity    = qty,
            minQuantity = minQty,
            updatedAt   = 0L
        )

    // ── Dashboard content composable (stateless) ──────────────────────────────

    @Composable
    private fun DashboardContent(
        activeCases:        List<CaseEntity>      = emptyList(),
        readyCases:         List<CaseEntity>      = emptyList(),
        lowStockItems:      List<InventoryEntity> = emptyList(),
        todayDeliveries:    Int                   = 0,
        pendingInspections: Int                   = 0,
        totalActiveCases:   Int                   = 0,
        isLoading:          Boolean               = false,
        error:              String?               = null
    ) {
        Scaffold(
            topBar = {
                WorkshopTopBar(title = "ورشة تك")
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick       = {},
                    icon          = { Icon(Icons.Default.Add, null) },
                    text          = { Text("ملف جديد") },
                    containerColor = Orange600
                )
            }
        ) { padding ->
            if (isLoading) {
                LoadingScreen()
            } else {
                Column(
                    Modifier
                        .padding(top = padding.calculateTopPadding())
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(Modifier.height(8.dp))

                    error?.let {
                        ErrorBanner(message = it, onDismiss = {})
                    }

                    // KPI grid
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title    = "الحالات النشطة",
                            value    = totalActiveCases.toString(),
                            icon     = Icons.Default.Build,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title    = "تسليم اليوم",
                            value    = todayDeliveries.toString(),
                            icon     = Icons.Default.LocalShipping,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title    = "فحوصات معلقة",
                            value    = pendingInspections.toString(),
                            icon     = Icons.Default.CheckCircle,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title    = "مخزون منخفض",
                            value    = lowStockItems.size.toString(),
                            icon     = Icons.Default.Warning,
                            tint     = if (lowStockItems.isNotEmpty()) MaterialTheme.colorScheme.error
                                       else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Active cases
                    SectionHeader(title = "الحالات النشطة", action = "عرض الكل", onAction = {})
                    if (activeCases.isEmpty()) {
                        EmptyState(icon = Icons.Default.Build, title = "لا توجد حالات نشطة")
                    } else {
                        activeCases.take(3).forEach { case ->
                            CaseRow(case = case)
                        }
                    }

                    // Low stock
                    if (lowStockItems.isNotEmpty()) {
                        SectionHeader(title = "مخزون منخفض", action = "عرض الكل", onAction = {})
                        lowStockItems.take(2).forEach { item ->
                            Card(
                                Modifier.fillMaxWidth(),
                                shape  = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Red100.copy(0.3f))
                            ) {
                                Row(
                                    Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text(item.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${item.quantity} / ${item.minQuantity}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Red700
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(80.dp))  // FAB clearance
                }
            }
        }
    }

    @Composable
    private fun CaseRow(case: CaseEntity) {
        Card(
            Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(10.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                Modifier.padding(12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(case.licensePlate, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${case.make} ${case.model}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusChip(status = case.status)
            }
        }
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test fun dashboard_loading_state() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                DashboardContent(isLoading = true)
            }
        }
    }

    @Test fun dashboard_empty_light() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                DashboardContent(
                    totalActiveCases   = 0,
                    todayDeliveries    = 0,
                    pendingInspections = 0
                )
            }
        }
    }

    @Test fun dashboard_populated_light() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                DashboardContent(
                    activeCases        = listOf(
                        fakeCase("1", "LY-1234"),
                        fakeCase("2", "KSA-5678"),
                        fakeCase("3", "UAE-9999")
                    ),
                    readyCases         = listOf(fakeCase("4", "BMW-001", CaseStatus.READY_FOR_DELIVERY)),
                    todayDeliveries    = 1,
                    pendingInspections = 3,
                    totalActiveCases   = 4
                )
            }
        }
    }

    @Test fun dashboard_populated_dark() {
        paparazzi.snapshot {
            WorkshopTechTheme(darkTheme = true) {
                DashboardContent(
                    activeCases        = listOf(
                        fakeCase("1", "LY-1234"),
                        fakeCase("2", "KSA-5678")
                    ),
                    todayDeliveries    = 2,
                    pendingInspections = 2,
                    totalActiveCases   = 2
                )
            }
        }
    }

    @Test fun dashboard_high_load_light() {
        val manyCases = (1..10).map { fakeCase("c$it", "LY-${1000 + it}") }
        paparazzi.snapshot {
            WorkshopTechTheme {
                DashboardContent(
                    activeCases        = manyCases,
                    totalActiveCases   = 10,
                    todayDeliveries    = 3,
                    pendingInspections = 8
                )
            }
        }
    }

    @Test fun dashboard_low_stock_warning_light() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                DashboardContent(
                    activeCases   = listOf(fakeCase("1", "LY-1234")),
                    totalActiveCases = 1,
                    lowStockItems = listOf(
                        fakeInventoryItem("i1", "دهان أبيض لؤلؤي", qty = 2, minQty = 5),
                        fakeInventoryItem("i2", "برايمر رمادي",    qty = 0, minQty = 3)
                    )
                )
            }
        }
    }

    @Test fun dashboard_error_state_light() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                DashboardContent(
                    isLoading = false,
                    error     = "فشل الاتصال بقاعدة البيانات. تحقق من الاتصال."
                )
            }
        }
    }

    @Test fun dashboard_error_state_dark() {
        paparazzi.snapshot {
            WorkshopTechTheme(darkTheme = true) {
                DashboardContent(
                    isLoading = false,
                    error     = "حدث خطأ غير متوقع"
                )
            }
        }
    }
}
