package com.workshoptech.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.workshoptech.data.AppDatabase
import com.workshoptech.data.entity.CaseEntity
import com.workshoptech.data.entity.CaseStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented DAO tests for [CaseDao] using an in-memory Room database.
 *
 * Coverage:
 *  - upsert + observeAll order (updatedAt DESC)
 *  - getById found / not found
 *  - observeById flow emissions
 *  - observeByStatus filter
 *  - search matching licensePlate / make / model
 *  - countByStatus
 *  - updateStatus persists change
 *  - updateColor persists colorCode + colorName
 *  - updateActualCost persists cost
 *  - delete removes row
 *  - deleteById removes row
 *  - upsertAll inserts multiple rows
 *  - observeRecent respects limit
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class CaseDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: CaseDao

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
        .allowMainThreadQueries()
        .build()
        dao = db.caseDao()
    }

    @After fun tearDown() { db.close() }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun case(id: String, status: String = CaseStatus.NEW, updatedAt: Long = 0L) = CaseEntity(
        caseId       = id,
        customerId   = "cust-1",
        licensePlate = "LY-$id",
        make         = "Toyota",
        model        = "Corolla",
        status       = status,
        createdAt    = 0L,
        updatedAt    = updatedAt
    )

    // ── upsert + observeAll ───────────────────────────────────────────────────

    @Test fun upsert_and_observe_all() = runTest {
        dao.upsert(case("a", updatedAt = 1000L))
        dao.upsert(case("b", updatedAt = 2000L))

        val list = dao.observeAll().first()
        assertEquals(2, list.size)
        // DESC by updatedAt → "b" first
        assertEquals("b", list[0].caseId)
        assertEquals("a", list[1].caseId)
    }

    @Test fun upsert_updates_existing_row() = runTest {
        dao.upsert(case("x", status = CaseStatus.NEW))
        dao.upsert(case("x", status = CaseStatus.APPROVED))

        val list = dao.observeAll().first()
        assertEquals(1, list.size)
        assertEquals(CaseStatus.APPROVED, list[0].status)
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test fun getById_found() = runTest {
        dao.upsert(case("c1"))
        val found = dao.getById("c1")
        assertNotNull(found)
        assertEquals("c1", found!!.caseId)
    }

    @Test fun getById_not_found_returns_null() = runTest {
        assertNull(dao.getById("ghost"))
    }

    // ── observeById ───────────────────────────────────────────────────────────

    @Test fun observeById_emits_entity_when_present() = runTest {
        dao.upsert(case("c2"))
        val entity = dao.observeById("c2").first()
        assertNotNull(entity)
        assertEquals("c2", entity!!.caseId)
    }

    @Test fun observeById_emits_null_when_missing() = runTest {
        val entity = dao.observeById("missing").first()
        assertNull(entity)
    }

    // ── observeByStatus ───────────────────────────────────────────────────────

    @Test fun observeByStatus_filters_correctly() = runTest {
        dao.upsert(case("s1", CaseStatus.IN_PROGRESS))
        dao.upsert(case("s2", CaseStatus.READY_FOR_DELIVERY))
        dao.upsert(case("s3", CaseStatus.IN_PROGRESS))

        val inProgress = dao.observeByStatus(CaseStatus.IN_PROGRESS).first()
        assertEquals(2, inProgress.size)
        assertTrue(inProgress.all { it.status == CaseStatus.IN_PROGRESS })
    }

    @Test fun observeByStatus_empty_when_no_match() = runTest {
        dao.upsert(case("s1", CaseStatus.NEW))
        val ready = dao.observeByStatus(CaseStatus.READY_FOR_DELIVERY).first()
        assertTrue(ready.isEmpty())
    }

    // ── search ────────────────────────────────────────────────────────────────

    @Test fun search_matches_licensePlate() = runTest {
        dao.upsert(case("p1").copy(licensePlate = "BMW-001"))
        dao.upsert(case("p2").copy(licensePlate = "TOY-999"))

        val results = dao.search("BMW").first()
        assertEquals(1, results.size)
        assertEquals("p1", results[0].caseId)
    }

    @Test fun search_matches_make() = runTest {
        dao.upsert(case("m1").copy(make = "Hyundai"))
        dao.upsert(case("m2").copy(make = "Toyota"))

        val results = dao.search("Hyundai").first()
        assertEquals(1, results.size)
        assertEquals("m1", results[0].caseId)
    }

    @Test fun search_case_insensitive_for_make() = runTest {
        dao.upsert(case("ci").copy(make = "Toyota"))
        // LIKE is case-insensitive in SQLite for ASCII
        val results = dao.search("toyota").first()
        assertEquals(1, results.size)
    }

    @Test fun search_empty_query_can_match_any() = runTest {
        dao.upsert(case("any1"))
        // LIKE '%' || '' || '%' matches all rows
        val results = dao.search("").first()
        assertTrue(results.isNotEmpty())
    }

    // ── countByStatus ─────────────────────────────────────────────────────────

    @Test fun countByStatus_returns_correct_count() = runTest {
        dao.upsert(case("c1", CaseStatus.NEW))
        dao.upsert(case("c2", CaseStatus.NEW))
        dao.upsert(case("c3", CaseStatus.APPROVED))

        assertEquals(2, dao.countByStatus(CaseStatus.NEW))
        assertEquals(1, dao.countByStatus(CaseStatus.APPROVED))
        assertEquals(0, dao.countByStatus(CaseStatus.DELIVERED))
    }

    // ── updateStatus ──────────────────────────────────────────────────────────

    @Test fun updateStatus_persists_new_status() = runTest {
        dao.upsert(case("u1", CaseStatus.NEW))
        dao.updateStatus("u1", CaseStatus.IN_PROGRESS, System.currentTimeMillis())

        val entity = dao.getById("u1")
        assertNotNull(entity)
        assertEquals(CaseStatus.IN_PROGRESS, entity!!.status)
    }

    @Test fun updateStatus_does_not_affect_other_rows() = runTest {
        dao.upsert(case("u1", CaseStatus.NEW))
        dao.upsert(case("u2", CaseStatus.NEW))
        dao.updateStatus("u1", CaseStatus.APPROVED, 0L)

        val u2 = dao.getById("u2")
        assertEquals(CaseStatus.NEW, u2!!.status)
    }

    // ── updateColor ───────────────────────────────────────────────────────────

    @Test fun updateColor_persists_colorCode_and_colorName() = runTest {
        dao.upsert(case("uc1"))
        dao.updateColor("uc1", "#FF0000", "أحمر", System.currentTimeMillis())

        val entity = dao.getById("uc1")
        assertEquals("#FF0000", entity!!.colorCode)
        assertEquals("أحمر",   entity.colorName)
    }

    // ── updateActualCost ──────────────────────────────────────────────────────

    @Test fun updateActualCost_persists_cost() = runTest {
        dao.upsert(case("cost1"))
        dao.updateActualCost("cost1", 750.5, System.currentTimeMillis())

        val entity = dao.getById("cost1")
        assertEquals(750.5, entity!!.actualCost!!, 0.01)
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test fun delete_removes_row() = runTest {
        val c = case("del1")
        dao.upsert(c)
        dao.delete(c)

        assertNull(dao.getById("del1"))
    }

    @Test fun deleteById_removes_row() = runTest {
        dao.upsert(case("del2"))
        dao.deleteById("del2")

        assertNull(dao.getById("del2"))
    }

    @Test fun deleteById_non_existent_does_not_crash() = runTest {
        dao.deleteById("ghost")  // must not throw
    }

    // ── upsertAll ─────────────────────────────────────────────────────────────

    @Test fun upsertAll_inserts_multiple_rows() = runTest {
        val cases = (1..5).map { case("batch-$it") }
        dao.upsertAll(cases)

        val all = dao.observeAll().first()
        assertEquals(5, all.size)
    }

    @Test fun upsertAll_updates_existing_rows() = runTest {
        dao.upsert(case("e1", CaseStatus.NEW))
        dao.upsertAll(listOf(case("e1", CaseStatus.DELIVERED), case("e2", CaseStatus.NEW)))

        val all = dao.observeAll().first()
        assertEquals(2, all.size)
        assertEquals(CaseStatus.DELIVERED, all.first { it.caseId == "e1" }.status)
    }

    // ── observeRecent ─────────────────────────────────────────────────────────

    @Test fun observeRecent_respects_limit() = runTest {
        (1..10).forEach { dao.upsert(case("r$it", updatedAt = it.toLong())) }

        val recent = dao.observeRecent(limit = 3).first()
        assertEquals(3, recent.size)
        // Newest (updatedAt = 10) should be first
        assertEquals("r10", recent[0].caseId)
    }

    @Test fun observeRecent_returns_all_when_fewer_than_limit() = runTest {
        dao.upsert(case("r1"))
        dao.upsert(case("r2"))

        val recent = dao.observeRecent(limit = 20).first()
        assertEquals(2, recent.size)
    }
}
