package com.workshoptech.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.workshoptech.data.AppDatabase
import com.workshoptech.data.entity.InspectionEntity
import com.workshoptech.data.entity.InspectionStatus
import com.workshoptech.data.entity.InspectionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented DAO tests for [InspectionDao].
 *
 * Coverage:
 *  - upsert + observeByCase ordered T1→T6
 *  - getLatestByType returns newest entry
 *  - getById found / not found
 *  - observePending PENDING only, ordered by createdAt ASC
 *  - updateStatus persists change + completedAt timestamp
 *  - delete / deleteByCase
 *  - InspectionType.all has exactly 6 entries
 *  - InspectionStatus.labelAr non-blank for all statuses
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class InspectionDaoTest {

    private lateinit var db:  AppDatabase
    private lateinit var dao: InspectionDao

    @Before fun setUp() {
        db  = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.inspectionDao()
    }

    @After fun tearDown() { db.close() }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun inspection(
        id:     String,
        caseId: String   = "case-1",
        type:   String   = InspectionType.T1,
        status: String   = InspectionStatus.PENDING,
        createdAt: Long  = System.currentTimeMillis()
    ) = InspectionEntity(
        inspectionId = id,
        caseId       = caseId,
        type         = type,
        status       = status,
        createdAt    = createdAt
    )

    // ── upsert + observeByCase ────────────────────────────────────────────────

    @Test fun observeByCase_returns_inspections_for_case() = runTest {
        dao.upsert(inspection("i1", caseId = "caseA", type = InspectionType.T1))
        dao.upsert(inspection("i2", caseId = "caseA", type = InspectionType.T3))
        dao.upsert(inspection("i3", caseId = "caseB", type = InspectionType.T1))

        val forA = dao.observeByCase("caseA").first()
        assertEquals(2, forA.size)
        assertTrue(forA.all { it.caseId == "caseA" })
    }

    @Test fun observeByCase_ordered_T1_to_T6() = runTest {
        // Insert in reverse order to verify sorting
        dao.upsert(inspection("t6", type = InspectionType.T6))
        dao.upsert(inspection("t3", type = InspectionType.T3))
        dao.upsert(inspection("t1", type = InspectionType.T1))

        val list = dao.observeByCase("case-1").first()
        assertEquals(InspectionType.T1, list[0].type)
        assertEquals(InspectionType.T3, list[1].type)
        assertEquals(InspectionType.T6, list[2].type)
    }

    @Test fun observeByCase_empty_for_unknown_case() = runTest {
        dao.upsert(inspection("ix", caseId = "caseX"))
        assertTrue(dao.observeByCase("caseY").first().isEmpty())
    }

    // ── getLatestByType ───────────────────────────────────────────────────────

    @Test fun getLatestByType_returns_newest_entry() = runTest {
        dao.upsert(inspection("old-t1", type = InspectionType.T1, createdAt = 1000L))
        dao.upsert(inspection("new-t1", type = InspectionType.T1, createdAt = 9999L))
        dao.upsert(inspection("t2",     type = InspectionType.T2, createdAt = 5000L))

        val latest = dao.getLatestByType("case-1", InspectionType.T1)
        assertNotNull(latest)
        assertEquals("new-t1", latest!!.inspectionId)
    }

    @Test fun getLatestByType_returns_null_when_no_match() = runTest {
        assertNull(dao.getLatestByType("case-1", InspectionType.T4))
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test fun getById_returns_entity() = runTest {
        dao.upsert(inspection("insp-99"))
        val found = dao.getById("insp-99")
        assertNotNull(found)
        assertEquals("insp-99", found!!.inspectionId)
    }

    @Test fun getById_returns_null_when_missing() = runTest {
        assertNull(dao.getById("ghost"))
    }

    // ── observePending ────────────────────────────────────────────────────────

    @Test fun observePending_returns_only_pending_status() = runTest {
        dao.upsert(inspection("p1", status = InspectionStatus.PENDING))
        dao.upsert(inspection("p2", status = InspectionStatus.PASSED))
        dao.upsert(inspection("p3", status = InspectionStatus.PENDING))

        val pending = dao.observePending().first()
        assertEquals(2, pending.size)
        assertTrue(pending.all { it.status == InspectionStatus.PENDING })
    }

    @Test fun observePending_sorted_by_createdAt_asc() = runTest {
        dao.upsert(inspection("late",  status = InspectionStatus.PENDING, createdAt = 3000L))
        dao.upsert(inspection("early", status = InspectionStatus.PENDING, createdAt = 1000L))
        dao.upsert(inspection("mid",   status = InspectionStatus.PENDING, createdAt = 2000L))

        val list = dao.observePending().first()
        assertEquals("early", list[0].inspectionId)
        assertEquals("mid",   list[1].inspectionId)
        assertEquals("late",  list[2].inspectionId)
    }

    @Test fun observePending_empty_when_none_pending() = runTest {
        dao.upsert(inspection("done", status = InspectionStatus.PASSED))
        assertTrue(dao.observePending().first().isEmpty())
    }

    // ── updateStatus ──────────────────────────────────────────────────────────

    @Test fun updateStatus_changes_status_and_sets_completedAt() = runTest {
        dao.upsert(inspection("us1"))
        val ts = System.currentTimeMillis()
        dao.updateStatus("us1", InspectionStatus.PASSED, ts)

        val entity = dao.getById("us1")
        assertEquals(InspectionStatus.PASSED, entity!!.status)
        assertEquals(ts, entity.completedAt)
    }

    @Test fun updateStatus_does_not_affect_other_inspections() = runTest {
        dao.upsert(inspection("us2"))
        dao.upsert(inspection("us3"))
        dao.updateStatus("us2", InspectionStatus.FAILED, 0L)

        assertEquals(InspectionStatus.PENDING, dao.getById("us3")!!.status)
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test fun delete_removes_inspection() = runTest {
        val i = inspection("del1")
        dao.upsert(i)
        dao.delete(i)
        assertNull(dao.getById("del1"))
    }

    @Test fun deleteByCase_removes_all_for_case() = runTest {
        dao.upsert(inspection("dc1", caseId = "caseD"))
        dao.upsert(inspection("dc2", caseId = "caseD"))
        dao.upsert(inspection("dc3", caseId = "caseE"))

        dao.deleteByCase("caseD")

        assertNull(dao.getById("dc1"))
        assertNull(dao.getById("dc2"))
        assertNotNull(dao.getById("dc3"))  // different case, unaffected
    }

    @Test fun deleteByCase_no_op_when_no_matching_rows() = runTest {
        dao.upsert(inspection("safe", caseId = "caseS"))
        dao.deleteByCase("nonexistent")
        assertNotNull(dao.getById("safe"))
    }

    // ── InspectionType constants ───────────────────────────────────────────────

    @Test fun InspectionType_all_has_6_entries() {
        assertEquals(6, InspectionType.all.size)
    }

    @Test fun InspectionType_all_contains_T1_through_T6() {
        val expected = setOf(
            InspectionType.T1, InspectionType.T2, InspectionType.T3,
            InspectionType.T4, InspectionType.T5, InspectionType.T6
        )
        assertEquals(expected, InspectionType.all.toSet())
    }

    @Test fun InspectionType_labelAr_non_blank_for_all() {
        InspectionType.all.forEach { t ->
            assertTrue("Empty label for $t", InspectionType.labelAr(t).isNotBlank())
        }
    }

    // ── InspectionStatus constants ────────────────────────────────────────────

    @Test fun InspectionStatus_labelAr_non_blank_for_all() {
        listOf(
            InspectionStatus.PENDING, InspectionStatus.PASSED,
            InspectionStatus.FAILED,  InspectionStatus.SKIPPED
        ).forEach { s ->
            assertTrue("Empty label for $s", InspectionStatus.labelAr(s).isNotBlank())
        }
    }
}
