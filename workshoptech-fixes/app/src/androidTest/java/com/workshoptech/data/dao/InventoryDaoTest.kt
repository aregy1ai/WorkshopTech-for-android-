package com.workshoptech.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.workshoptech.data.AppDatabase
import com.workshoptech.data.entity.InventoryCategory
import com.workshoptech.data.entity.InventoryEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented DAO tests for [InventoryDao].
 *
 * Coverage:
 *  - upsert + observeAll (category ASC, name ASC)
 *  - observeByCategory
 *  - observeLowStock (quantity <= minQuantity)
 *  - getById found / not found
 *  - search by name / nameAr / barcode
 *  - upsertAll
 *  - decrement — success reduces quantity; below-zero guard (atomic)
 *  - increment — success increases quantity
 *  - delete
 *  - InventoryEntity.isLowStock
 *  - InventoryEntity.totalValue
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class InventoryDaoTest {

    private lateinit var db:  AppDatabase
    private lateinit var dao: InventoryDao

    @Before fun setUp() {
        db  = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.inventoryDao()
    }

    @After fun tearDown() { db.close() }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun item(
        id:          String,
        name:        String = "Item $id",
        nameAr:      String = "عنصر $id",
        category:    String = InventoryCategory.PAINT,
        qty:         Int    = 10,
        minQty:      Int    = 5,
        barcode:     String? = null,
        unitPrice:   Double  = 10.0
    ) = InventoryEntity(
        itemId      = id,
        name        = name,
        nameAr      = nameAr,
        category    = category,
        quantity    = qty,
        minQuantity = minQty,
        unitPrice   = unitPrice,
        barcode     = barcode,
        updatedAt   = System.currentTimeMillis()
    )

    // ── observeAll ────────────────────────────────────────────────────────────

    @Test fun observeAll_sorted_by_category_then_name() = runTest {
        dao.upsert(item("i1", name = "Red",    category = InventoryCategory.PAINT))
        dao.upsert(item("i2", name = "Primer", category = InventoryCategory.MATERIALS))
        dao.upsert(item("i3", name = "Blue",   category = InventoryCategory.PAINT))

        val all = dao.observeAll().first()
        assertEquals(3, all.size)

        // MATERIALS < PAINT (alphabetical)
        assertEquals("i2", all[0].itemId)   // Primer/MATERIALS
        // Within PAINT: Blue before Red
        assertEquals("i3", all[1].itemId)   // Blue
        assertEquals("i1", all[2].itemId)   // Red
    }

    // ── observeByCategory ─────────────────────────────────────────────────────

    @Test fun observeByCategory_filters_correctly() = runTest {
        dao.upsert(item("c1", category = InventoryCategory.PAINT))
        dao.upsert(item("c2", category = InventoryCategory.TOOLS))
        dao.upsert(item("c3", category = InventoryCategory.PAINT))

        val paints = dao.observeByCategory(InventoryCategory.PAINT).first()
        assertEquals(2, paints.size)
        assertTrue(paints.all { it.category == InventoryCategory.PAINT })
    }

    @Test fun observeByCategory_empty_when_no_match() = runTest {
        dao.upsert(item("nc1", category = InventoryCategory.PAINT))
        val safety = dao.observeByCategory(InventoryCategory.SAFETY).first()
        assertTrue(safety.isEmpty())
    }

    // ── observeLowStock ───────────────────────────────────────────────────────

    @Test fun observeLowStock_returns_items_at_or_below_minQuantity() = runTest {
        dao.upsert(item("ls1", qty = 3,  minQty = 5))   // low: 3 <= 5
        dao.upsert(item("ls2", qty = 5,  minQty = 5))   // low: 5 <= 5 (boundary)
        dao.upsert(item("ls3", qty = 10, minQty = 5))   // ok

        val lowStock = dao.observeLowStock().first()
        assertEquals(2, lowStock.size)
        assertTrue(lowStock.all { it.itemId in listOf("ls1", "ls2") })
    }

    @Test fun observeLowStock_empty_when_all_ok() = runTest {
        dao.upsert(item("ok1", qty = 20, minQty = 5))
        assertTrue(dao.observeLowStock().first().isEmpty())
    }

    @Test fun observeLowStock_sorted_by_quantity_asc() = runTest {
        dao.upsert(item("lo1", qty = 4, minQty = 10))
        dao.upsert(item("lo2", qty = 1, minQty = 10))
        dao.upsert(item("lo3", qty = 7, minQty = 10))

        val list = dao.observeLowStock().first()
        assertEquals("lo2", list[0].itemId)   // qty=1 first
        assertEquals("lo1", list[1].itemId)   // qty=4
        assertEquals("lo3", list[2].itemId)   // qty=7
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test fun getById_returns_entity() = runTest {
        dao.upsert(item("gb1", name = "Primer"))
        val found = dao.getById("gb1")
        assertNotNull(found)
        assertEquals("Primer", found!!.name)
    }

    @Test fun getById_returns_null_when_missing() = runTest {
        assertNull(dao.getById("ghost"))
    }

    // ── search ────────────────────────────────────────────────────────────────

    @Test fun search_by_name() = runTest {
        dao.upsert(item("sn1", name = "White Pearl Paint"))
        dao.upsert(item("sn2", name = "Black Matte"))

        val results = dao.search("Pearl")
        assertEquals(1, results.size)
        assertEquals("sn1", results[0].itemId)
    }

    @Test fun search_by_nameAr() = runTest {
        dao.upsert(item("sa1", nameAr = "دهان أبيض"))
        dao.upsert(item("sa2", nameAr = "دهان أسود"))

        val results = dao.search("أبيض")
        assertEquals(1, results.size)
        assertEquals("sa1", results[0].itemId)
    }

    @Test fun search_by_barcode_exact() = runTest {
        dao.upsert(item("sb1", barcode = "1234567890"))
        dao.upsert(item("sb2", barcode = "0987654321"))

        val results = dao.search("1234567890")
        assertTrue(results.any { it.itemId == "sb1" })
    }

    @Test fun search_limit_is_30() = runTest {
        (1..40).forEach { dao.upsert(item("bulk-$it", name = "Paint $it")) }
        val results = dao.search("Paint")
        assertTrue("Expected at most 30 results, got ${results.size}", results.size <= 30)
    }

    // ── decrement ─────────────────────────────────────────────────────────────

    @Test fun decrement_reduces_quantity_when_sufficient_stock() = runTest {
        dao.upsert(item("dec1", qty = 10))
        dao.decrement("dec1", 3)
        val found = dao.getById("dec1")
        assertEquals(7, found!!.quantity)
    }

    @Test fun decrement_does_nothing_when_insufficient_stock() = runTest {
        dao.upsert(item("dec2", qty = 2))
        dao.decrement("dec2", 5)   // 2 < 5, guard: quantity >= amount
        val found = dao.getById("dec2")
        assertEquals(2, found!!.quantity)   // unchanged
    }

    @Test fun decrement_exact_amount_empties_stock() = runTest {
        dao.upsert(item("dec3", qty = 5))
        dao.decrement("dec3", 5)
        assertEquals(0, dao.getById("dec3")!!.quantity)
    }

    // ── increment ─────────────────────────────────────────────────────────────

    @Test fun increment_increases_quantity() = runTest {
        dao.upsert(item("inc1", qty = 5))
        dao.increment("inc1", 4)
        assertEquals(9, dao.getById("inc1")!!.quantity)
    }

    @Test fun increment_from_zero() = runTest {
        dao.upsert(item("inc2", qty = 0))
        dao.increment("inc2", 10)
        assertEquals(10, dao.getById("inc2")!!.quantity)
    }

    // ── upsertAll ─────────────────────────────────────────────────────────────

    @Test fun upsertAll_inserts_multiple() = runTest {
        val items = (1..5).map { item("batch-$it") }
        dao.upsertAll(items)
        val all = dao.observeAll().first()
        assertEquals(5, all.size)
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test fun delete_removes_item() = runTest {
        val i = item("del1")
        dao.upsert(i)
        dao.delete(i)
        assertNull(dao.getById("del1"))
    }

    // ── InventoryEntity computed properties ───────────────────────────────────

    @Test fun isLowStock_true_when_qty_below_minQty() {
        val i = item("prop1", qty = 3, minQty = 5)
        assertTrue(i.isLowStock)
    }

    @Test fun isLowStock_true_when_qty_equals_minQty() {
        val i = item("prop2", qty = 5, minQty = 5)
        assertTrue(i.isLowStock)
    }

    @Test fun isLowStock_false_when_qty_above_minQty() {
        val i = item("prop3", qty = 6, minQty = 5)
        assertFalse(i.isLowStock)
    }

    @Test fun totalValue_is_quantity_times_unitPrice() {
        val i = item("prop4", qty = 4, unitPrice = 25.0)
        assertEquals(100.0, i.totalValue, 0.01)
    }

    @Test fun totalValue_is_zero_when_qty_zero() {
        assertEquals(0.0, item("prop5", qty = 0).totalValue, 0.0)
    }
}
