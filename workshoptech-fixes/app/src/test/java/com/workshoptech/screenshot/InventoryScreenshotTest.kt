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
import com.workshoptech.data.entity.InventoryEntity
import com.workshoptech.ui.common.*
import com.workshoptech.ui.theme.*
import org.junit.Rule
import org.junit.Test

/**
 * Paparazzi screenshot tests for Inventory UI states.
 *
 * Stateless [InventoryContent] composable mirrors [InventoryScreen] content
 * without requiring a ViewModel.
 *
 * State variants:
 *  - Loading
 *  - Empty
 *  - Normal list (light + dark)
 *  - Low-stock warning banner visible
 *  - Mix of low-stock and ok items
 *  - Single-item list (edge case)
 *  - Large quantity numbers
 */
class InventoryScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig  = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.V_SCROLL,
        showSystemUi  = false
    )

    // ── Fake data ─────────────────────────────────────────────────────────────

    private fun item(
        id:       String,
        name:     String = "عنصر $id",
        nameAr:   String = "عنصر $id",
        category: String = "PAINT",
        qty:      Int    = 10,
        minQty:   Int    = 5,
        price:    Double = 25.0
    ) = InventoryEntity(
        itemId      = id,
        name        = name,
        nameAr      = nameAr,
        category    = category,
        quantity    = qty,
        minQuantity = minQty,
        unitPrice   = price,
        updatedAt   = 0L
    )

    // ── Stateless content composable ──────────────────────────────────────────

    @Composable
    private fun InventoryContent(
        items:         List<InventoryEntity> = emptyList(),
        lowStockItems: List<InventoryEntity> = emptyList(),
        isLoading:     Boolean               = false,
        error:         String?               = null
    ) {
        Scaffold(
            topBar = {
                WorkshopTopBar(
                    title  = "المخزون",
                    onBack = {},
                    actions = {
                        IconButton(onClick = {}) { Icon(Icons.Default.Add, null) }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {}, containerColor = Blue600) {
                    Icon(Icons.Default.Add, null, tint = White)
                }
            }
        ) { padding ->
            Column(
                Modifier
                    .padding(top = padding.calculateTopPadding())
                    .fillMaxSize()
            ) {
                // Low stock warning banner
                if (lowStockItems.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        colors   = CardDefaults.cardColors(containerColor = Red100),
                        shape    = RoundedCornerShape(10.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = Red700, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${lowStockItems.size} عنصر في مستوى منخفض",
                                style = MaterialTheme.typography.bodySmall,
                                color = Red700
                            )
                        }
                    }
                }

                error?.let {
                    ErrorBanner(message = it, onDismiss = {})
                }

                when {
                    isLoading  -> LoadingScreen()
                    items.isEmpty() -> EmptyState(
                        icon     = Icons.Default.Inventory2,
                        title    = "المخزون فارغ",
                        subtitle = "اضغط + لإضافة عنصر جديد"
                    )
                    else -> LazyColumn(
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items, key = { it.itemId }) { inv ->
                            InventoryItemCard(inv = inv)
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    @Composable
    private fun InventoryItemCard(inv: InventoryEntity) {
        val isLow = inv.quantity <= inv.minQuantity
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(12.dp),
            colors    = CardDefaults.cardColors(
                containerColor = if (isLow) Red100.copy(alpha = 0.2f)
                                 else       MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                Modifier.padding(12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(inv.nameAr.ifBlank { inv.name }, style = MaterialTheme.typography.titleSmall)
                    Text(
                        inv.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${inv.quantity} / ${inv.minQuantity}",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isLow) Red700 else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${inv.unitPrice} د.ل",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test fun inventory_loading_light() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                InventoryContent(isLoading = true)
            }
        }
    }

    @Test fun inventory_empty_light() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                InventoryContent()
            }
        }
    }

    @Test fun inventory_empty_dark() {
        paparazzi.snapshot {
            WorkshopTechTheme(darkTheme = true) {
                InventoryContent()
            }
        }
    }

    @Test fun inventory_populated_light() {
        val items = listOf(
            item("p1", "دهان أبيض لؤلؤي",  category = "PAINT",     qty = 15, minQty = 5,  price = 45.0),
            item("p2", "برايمر رمادي",       category = "PAINT",     qty = 8,  minQty = 5,  price = 30.0),
            item("m1", "ورق صنفرة 400",      category = "MATERIALS", qty = 50, minQty = 20, price = 5.0),
            item("t1", "بندقية رش هوائية",   category = "TOOLS",     qty = 3,  minQty = 1,  price = 200.0),
            item("c1", "مذيب تنر صناعي",     category = "CHEMICALS", qty = 20, minQty = 10, price = 15.0)
        )
        paparazzi.snapshot {
            WorkshopTechTheme {
                InventoryContent(items = items)
            }
        }
    }

    @Test fun inventory_populated_dark() {
        val items = listOf(
            item("p1", "دهان أسود مطفي",  category = "PAINT",     qty = 6,  minQty = 5),
            item("m1", "ورق صنفرة 600",   category = "MATERIALS", qty = 30, minQty = 10),
            item("t1", "مسدس دهان",        category = "TOOLS",     qty = 2,  minQty = 1)
        )
        paparazzi.snapshot {
            WorkshopTechTheme(darkTheme = true) {
                InventoryContent(items = items)
            }
        }
    }

    @Test fun inventory_low_stock_warning_light() {
        val lowItems = listOf(
            item("l1", "دهان أبيض", qty = 1, minQty = 5),
            item("l2", "برايمر",    qty = 0, minQty = 3)
        )
        val allItems = lowItems + listOf(
            item("ok1", "ورق صنفرة", qty = 40, minQty = 10)
        )
        paparazzi.snapshot {
            WorkshopTechTheme {
                InventoryContent(items = allItems, lowStockItems = lowItems)
            }
        }
    }

    @Test fun inventory_low_stock_warning_dark() {
        val lowItems = listOf(
            item("l1", "دهان أحمر", qty = 2, minQty = 5)
        )
        paparazzi.snapshot {
            WorkshopTechTheme(darkTheme = true) {
                InventoryContent(items = lowItems, lowStockItems = lowItems)
            }
        }
    }

    @Test fun inventory_all_low_stock_light() {
        val items = listOf(
            item("a1", "دهان أبيض",  qty = 1, minQty = 5),
            item("a2", "دهان أسود",  qty = 0, minQty = 3),
            item("a3", "برايمر رمادي", qty = 2, minQty = 5)
        )
        paparazzi.snapshot {
            WorkshopTechTheme {
                InventoryContent(items = items, lowStockItems = items)
            }
        }
    }

    @Test fun inventory_single_item_light() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                InventoryContent(items = listOf(item("solo", "دهان لؤلؤي مميز")))
            }
        }
    }

    @Test fun inventory_large_quantities_light() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                InventoryContent(
                    items = listOf(
                        item("big1", "دهان بالجملة",  qty = 5000, minQty = 500, price = 1250.0),
                        item("big2", "ورق صنفرة كميات", qty = 10000, minQty = 1000, price = 0.5)
                    )
                )
            }
        }
    }

    @Test fun inventory_error_state_light() {
        paparazzi.snapshot {
            WorkshopTechTheme {
                InventoryContent(
                    items = emptyList(),
                    error = "تعذّر تحميل بيانات المخزون"
                )
            }
        }
    }
}
