package com.workshoptech.screenshot

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.workshoptech.ui.common.*
import com.workshoptech.ui.theme.WorkshopTechTheme
import org.junit.Rule
import org.junit.Test

/**
 * Paparazzi screenshot tests for individual UI components in CommonComponents.kt.
 *
 * Each test renders a component in isolation (light + dark variants) and
 * generates a golden PNG under:
 *   app/src/test/snapshots/images/<TestClass>_<testName>.png
 *
 * Commands:
 *   ./gradlew recordPaparazziDebug   — generate / update goldens
 *   ./gradlew verifyPaparazziDebug   — compare against goldens (CI gate)
 */
class ComponentScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig         = DeviceConfig.PIXEL_5,
        renderingMode        = SessionParams.RenderingMode.SHRINK,
        showSystemUi         = false,
        validateAccessibility = true
    )

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun wrap(darkTheme: Boolean = false, content: @Composable () -> Unit) {
        paparazzi.snapshot {
            WorkshopTechTheme(darkTheme = darkTheme) {
                Surface {
                    Box(Modifier.padding(16.dp)) {
                        content()
                    }
                }
            }
        }
    }

    // ── StatusChip ────────────────────────────────────────────────────────────

    @Test fun statusChip_new_light() = wrap {
        StatusChip(status = "NEW")
    }

    @Test fun statusChip_approved_light() = wrap {
        StatusChip(status = "APPROVED")
    }

    @Test fun statusChip_inProgress_light() = wrap {
        StatusChip(status = "IN_PROGRESS")
    }

    @Test fun statusChip_readyForDelivery_light() = wrap {
        StatusChip(status = "READY_FOR_DELIVERY")
    }

    @Test fun statusChip_delivered_light() = wrap {
        StatusChip(status = "DELIVERED")
    }

    @Test fun statusChip_onHold_light() = wrap {
        StatusChip(status = "ON_HOLD")
    }

    @Test fun statusChip_cancelled_light() = wrap {
        StatusChip(status = "CANCELLED")
    }

    @Test fun statusChip_new_dark() = wrap(darkTheme = true) {
        StatusChip(status = "NEW")
    }

    @Test fun statusChip_inProgress_dark() = wrap(darkTheme = true) {
        StatusChip(status = "IN_PROGRESS")
    }

    // ── MetricCard ────────────────────────────────────────────────────────────

    @Test fun metricCard_light() = wrap {
        MetricCard(
            title    = "الحالات النشطة",
            value    = "14",
            icon     = Icons.Default.Build,
            modifier = Modifier.width(160.dp)
        )
    }

    @Test fun metricCard_dark() = wrap(darkTheme = true) {
        MetricCard(
            title    = "الحالات النشطة",
            value    = "14",
            icon     = Icons.Default.Build,
            modifier = Modifier.width(160.dp)
        )
    }

    @Test fun metricCard_large_value() = wrap {
        MetricCard(
            title    = "إجمالي الإيرادات",
            value    = "1,240,500",
            icon     = Icons.Default.AttachMoney,
            modifier = Modifier.width(160.dp)
        )
    }

    @Test fun metricCard_zero_value() = wrap {
        MetricCard(
            title    = "تسليم اليوم",
            value    = "0",
            icon     = Icons.Default.LocalShipping,
            modifier = Modifier.width(160.dp)
        )
    }

    // ── SectionHeader ─────────────────────────────────────────────────────────

    @Test fun sectionHeader_without_action_light() = wrap {
        SectionHeader(title = "الحالات النشطة")
    }

    @Test fun sectionHeader_with_action_light() = wrap {
        SectionHeader(title = "الحالات النشطة", action = "عرض الكل", onAction = {})
    }

    @Test fun sectionHeader_with_action_dark() = wrap(darkTheme = true) {
        SectionHeader(title = "المخزون المنخفض", action = "عرض الكل", onAction = {})
    }

    // ── WorkshopTopBar ────────────────────────────────────────────────────────

    @Test fun topBar_without_back_light() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                WorkshopTopBar(title = "ورشة تك")
            }
        }
    }

    @Test fun topBar_with_back_light() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                WorkshopTopBar(title = "الملفات", onBack = {})
            }
        }
    }

    @Test fun topBar_without_back_dark() {
        paparazzi.snapshot {
            WorkshopTechTheme(darkTheme = true) {
                WorkshopTopBar(title = "ورشة تك")
            }
        }
    }

    @Test fun topBar_with_long_title() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                WorkshopTopBar(title = "تفاصيل الملف — أ.ب 1234 — تويوتا كامري 2022")
            }
        }
    }

    // ── LoadingScreen ─────────────────────────────────────────────────────────

    @Test fun loadingScreen_light() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                Surface(Modifier.fillMaxSize()) {
                    LoadingScreen()
                }
            }
        }
    }

    @Test fun loadingScreen_custom_message_dark() {
        paparazzi.snapshot {
            WorkshopTechTheme(darkTheme = true) {
                Surface(Modifier.fillMaxSize()) {
                    LoadingScreen(message = "جاري تحليل الصورة…")
                }
            }
        }
    }

    // ── EmptyState ────────────────────────────────────────────────────────────

    @Test fun emptyState_no_action_light() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                Surface(Modifier.fillMaxSize()) {
                    EmptyState(
                        icon     = Icons.Default.Inbox,
                        title    = "لا توجد حالات",
                        subtitle = "اضغط + لإضافة ملف جديد"
                    )
                }
            }
        }
    }

    @Test fun emptyState_with_action_light() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                Surface(Modifier.fillMaxSize()) {
                    EmptyState(
                        icon        = Icons.Default.SearchOff,
                        title       = "لا توجد نتائج",
                        subtitle    = "جرّب بحثاً مختلفاً",
                        actionLabel = "مسح البحث",
                        onAction    = {}
                    )
                }
            }
        }
    }

    @Test fun emptyState_no_action_dark() {
        paparazzi.snapshot {
            WorkshopTechTheme(darkTheme = true) {
                Surface(Modifier.fillMaxSize()) {
                    EmptyState(
                        icon  = Icons.Default.Inventory2,
                        title = "المخزون فارغ"
                    )
                }
            }
        }
    }

    // ── ErrorBanner ───────────────────────────────────────────────────────────

    @Test fun errorBanner_light() = wrap {
        ErrorBanner(
            message   = "حدث خطأ في الاتصال بقاعدة البيانات",
            onDismiss = {}
        )
    }

    @Test fun errorBanner_dark() = wrap(darkTheme = true) {
        ErrorBanner(
            message   = "فشل تحميل الملفات. تحقق من الاتصال.",
            onDismiss = {}
        )
    }

    @Test fun errorBanner_long_message() = wrap {
        ErrorBanner(
            message   = "خطأ: لا يمكن حفظ الملف لأن القرص ممتلئ. يرجى حذف بعض الملفات القديمة وإعادة المحاولة.",
            onDismiss = {}
        )
    }

    // ── All StatusChip variants side-by-side ─────────────────────────────────

    @Test fun statusChip_all_variants_light() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                Surface {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "NEW", "APPROVED", "IN_PROGRESS",
                            "READY_FOR_DELIVERY", "DELIVERED",
                            "ON_HOLD", "CANCELLED"
                        ).forEach { status ->
                            StatusChip(status = status)
                        }
                    }
                }
            }
        }
    }

    @Test fun statusChip_all_variants_dark() {
        paparazzi.snapshot {
            WorkshopTechTheme(darkTheme = true) {
                Surface {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "NEW", "APPROVED", "IN_PROGRESS",
                            "READY_FOR_DELIVERY", "DELIVERED",
                            "ON_HOLD", "CANCELLED"
                        ).forEach { status ->
                            StatusChip(status = status)
                        }
                    }
                }
            }
        }
    }
}
