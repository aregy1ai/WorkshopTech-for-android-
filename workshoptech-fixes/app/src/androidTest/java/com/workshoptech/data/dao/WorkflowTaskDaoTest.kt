package com.workshoptech.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.workshoptech.data.AppDatabase
import com.workshoptech.data.entity.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented DAO tests for [WorkflowTaskDao].
 *
 * Coverage:
 *  - upsert + observeByCase ordered priority DESC, plannedStart ASC
 *  - observeByTechnician excludes COMPLETED / CANCELLED
 *  - getById found / not found
 *  - countPending only counts PENDING status
 *  - updateStatus persists change
 *  - delete removes task
 *  - deleteByCase removes all tasks for a case
 *  - upsertAll inserts multiple rows
 *  - Priority ordering: URGENT > HIGH > MEDIUM > LOW
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class WorkflowTaskDaoTest {

    private lateinit var db:  AppDatabase
    private lateinit var dao: WorkflowTaskDao

    @Before fun setUp() {
        db  = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.workflowTaskDao()
    }

    @After fun tearDown() { db.close() }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun task(
        id:          String,
        caseId:      String  = "case-1",
        type:        String  = TaskType.BODY_WORK,
        status:      String  = TaskStatus.PENDING,
        priority:    String  = TaskPriority.MEDIUM,
        assignedTo:  String? = null,
        plannedStart: Long?  = null
    ) = WorkflowTaskEntity(
        taskId       = id,
        caseId       = caseId,
        type         = type,
        status       = status,
        priority     = priority,
        assignedTo   = assignedTo,
        plannedStart = plannedStart
    )

    // ── observeByCase ─────────────────────────────────────────────────────────

    @Test fun observeByCase_returns_tasks_for_case() = runTest {
        dao.upsert(task("t1", caseId = "caseA"))
        dao.upsert(task("t2", caseId = "caseA"))
        dao.upsert(task("t3", caseId = "caseB"))

        val forA = dao.observeByCase("caseA").first()
        assertEquals(2, forA.size)
        assertTrue(forA.all { it.caseId == "caseA" })
    }

    @Test fun observeByCase_priority_ordered_URGENT_first() = runTest {
        dao.upsert(task("low",    priority = TaskPriority.LOW,    plannedStart = 100L))
        dao.upsert(task("urgent", priority = TaskPriority.URGENT, plannedStart = 200L))
        dao.upsert(task("high",   priority = TaskPriority.HIGH,   plannedStart = 150L))

        val list = dao.observeByCase("case-1").first()
        assertEquals("urgent", list[0].taskId)
        assertEquals("high",   list[1].taskId)
        assertEquals("low",    list[2].taskId)
    }

    @Test fun observeByCase_same_priority_ordered_by_plannedStart_ASC() = runTest {
        dao.upsert(task("t-late",  priority = TaskPriority.MEDIUM, plannedStart = 3000L))
        dao.upsert(task("t-early", priority = TaskPriority.MEDIUM, plannedStart = 1000L))
        dao.upsert(task("t-mid",   priority = TaskPriority.MEDIUM, plannedStart = 2000L))

        val list = dao.observeByCase("case-1").first()
        assertEquals("t-early", list[0].taskId)
        assertEquals("t-mid",   list[1].taskId)
        assertEquals("t-late",  list[2].taskId)
    }

    @Test fun observeByCase_empty_for_unknown_case() = runTest {
        dao.upsert(task("tx", caseId = "caseX"))
        assertTrue(dao.observeByCase("caseY").first().isEmpty())
    }

    // ── observeByTechnician ───────────────────────────────────────────────────

    @Test fun observeByTechnician_excludes_COMPLETED_and_CANCELLED() = runTest {
        dao.upsert(task("active1", status = TaskStatus.PENDING,     assignedTo = "tech-1"))
        dao.upsert(task("active2", status = TaskStatus.IN_PROGRESS, assignedTo = "tech-1"))
        dao.upsert(task("done",    status = TaskStatus.COMPLETED,   assignedTo = "tech-1"))
        dao.upsert(task("canc",    status = TaskStatus.CANCELLED,   assignedTo = "tech-1"))

        val forTech = dao.observeByTechnician("tech-1").first()
        assertEquals(2, forTech.size)
        assertTrue(forTech.none { it.status in listOf(TaskStatus.COMPLETED, TaskStatus.CANCELLED) })
    }

    @Test fun observeByTechnician_empty_for_unknown_tech() = runTest {
        dao.upsert(task("tx", assignedTo = "tech-A"))
        assertTrue(dao.observeByTechnician("tech-Z").first().isEmpty())
    }

    @Test fun observeByTechnician_does_not_include_tasks_of_other_techs() = runTest {
        dao.upsert(task("t1", assignedTo = "tech-1"))
        dao.upsert(task("t2", assignedTo = "tech-2"))

        val forTech1 = dao.observeByTechnician("tech-1").first()
        assertEquals(1, forTech1.size)
        assertEquals("t1", forTech1[0].taskId)
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test fun getById_returns_entity() = runTest {
        dao.upsert(task("gb1"))
        val found = dao.getById("gb1")
        assertNotNull(found)
        assertEquals("gb1", found!!.taskId)
    }

    @Test fun getById_returns_null_when_missing() = runTest {
        assertNull(dao.getById("ghost"))
    }

    // ── countPending ──────────────────────────────────────────────────────────

    @Test fun countPending_returns_only_PENDING_count() = runTest {
        dao.upsert(task("p1", status = TaskStatus.PENDING))
        dao.upsert(task("p2", status = TaskStatus.PENDING))
        dao.upsert(task("ip", status = TaskStatus.IN_PROGRESS))
        dao.upsert(task("cp", status = TaskStatus.COMPLETED))

        assertEquals(2, dao.countPending("case-1"))
    }

    @Test fun countPending_zero_when_none_pending() = runTest {
        dao.upsert(task("done", status = TaskStatus.COMPLETED))
        assertEquals(0, dao.countPending("case-1"))
    }

    @Test fun countPending_isolates_by_case() = runTest {
        dao.upsert(task("c1t1", caseId = "caseA", status = TaskStatus.PENDING))
        dao.upsert(task("c2t1", caseId = "caseB", status = TaskStatus.PENDING))
        assertEquals(1, dao.countPending("caseA"))
        assertEquals(1, dao.countPending("caseB"))
    }

    // ── updateStatus ──────────────────────────────────────────────────────────

    @Test fun updateStatus_changes_status() = runTest {
        dao.upsert(task("us1", status = TaskStatus.PENDING))
        dao.updateStatus("us1", TaskStatus.IN_PROGRESS)

        val found = dao.getById("us1")
        assertEquals(TaskStatus.IN_PROGRESS, found!!.status)
    }

    @Test fun updateStatus_does_not_affect_other_tasks() = runTest {
        dao.upsert(task("us2"))
        dao.upsert(task("us3"))
        dao.updateStatus("us2", TaskStatus.COMPLETED)

        assertEquals(TaskStatus.PENDING, dao.getById("us3")!!.status)
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test fun delete_removes_task() = runTest {
        val t = task("del1")
        dao.upsert(t)
        dao.delete(t)
        assertNull(dao.getById("del1"))
    }

    @Test fun deleteByCase_removes_all_tasks_for_case() = runTest {
        dao.upsert(task("dc1", caseId = "caseD"))
        dao.upsert(task("dc2", caseId = "caseD"))
        dao.upsert(task("dc3", caseId = "caseE"))

        dao.deleteByCase("caseD")

        assertNull(dao.getById("dc1"))
        assertNull(dao.getById("dc2"))
        assertNotNull(dao.getById("dc3"))
    }

    // ── upsertAll ─────────────────────────────────────────────────────────────

    @Test fun upsertAll_inserts_multiple_tasks() = runTest {
        val tasks = (1..5).map { task("batch-$it") }
        dao.upsertAll(tasks)
        val all = dao.observeByCase("case-1").first()
        assertEquals(5, all.size)
    }

    @Test fun upsertAll_updates_existing_tasks() = runTest {
        dao.upsert(task("upd", status = TaskStatus.PENDING))
        dao.upsertAll(listOf(task("upd", status = TaskStatus.COMPLETED)))

        assertEquals(TaskStatus.COMPLETED, dao.getById("upd")!!.status)
    }

    // ── Constants ─────────────────────────────────────────────────────────────

    @Test fun TaskStatus_values_are_distinct() {
        val values = listOf(
            TaskStatus.PENDING, TaskStatus.IN_PROGRESS,
            TaskStatus.COMPLETED, TaskStatus.BLOCKED, TaskStatus.CANCELLED
        )
        assertEquals(values.size, values.distinct().size)
    }

    @Test fun TaskPriority_values_are_distinct() {
        val values = listOf(
            TaskPriority.LOW, TaskPriority.MEDIUM,
            TaskPriority.HIGH, TaskPriority.URGENT
        )
        assertEquals(values.size, values.distinct().size)
    }

    @Test fun TaskType_values_are_distinct() {
        val values = listOf(
            TaskType.BODY_WORK, TaskType.PAINT, TaskType.POLISH,
            TaskType.INSPECTION, TaskType.PARTS, TaskType.DELIVERY, TaskType.OTHER
        )
        assertEquals(values.size, values.distinct().size)
    }
}
