package com.workshoptech.data.repository

import app.cash.turbine.test
import com.workshoptech.data.dao.*
import com.workshoptech.data.entity.*
import com.workshoptech.domain.model.AppResult
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for WorkshopRepository.
 *
 * All DAOs are mocked. Tests verify:
 *  - Flow operators (distinctUntilChanged, catch → emptyList)
 *  - Error handling on suspend functions → AppResult.Error
 *  - Successful paths → AppResult.Success
 *  - Delegation to correct DAO methods
 */
@ExperimentalCoroutinesApi
class WorkshopRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var caseDao:          CaseDao
    private lateinit var customerDao:      CustomerDao
    private lateinit var photoDao:         PhotoDao
    private lateinit var inspectionDao:    InspectionDao
    private lateinit var workflowTaskDao:  WorkflowTaskDao
    private lateinit var technicianDao:    TechnicianDao
    private lateinit var inventoryDao:     InventoryDao
    private lateinit var damageFindingDao: DamageFindingDao
    private lateinit var analysisResultDao: AnalysisResultDao
    private lateinit var videoDao:         VideoDao
    private lateinit var motionDataDao:    MotionDataDao

    private lateinit var repo: WorkshopRepository

    @Before fun setUp() {
        caseDao           = mockk(relaxed = true)
        customerDao       = mockk(relaxed = true)
        photoDao          = mockk(relaxed = true)
        inspectionDao     = mockk(relaxed = true)
        workflowTaskDao   = mockk(relaxed = true)
        technicianDao     = mockk(relaxed = true)
        inventoryDao      = mockk(relaxed = true)
        damageFindingDao  = mockk(relaxed = true)
        analysisResultDao = mockk(relaxed = true)
        videoDao          = mockk(relaxed = true)
        motionDataDao     = mockk(relaxed = true)

        repo = WorkshopRepository(
            caseDao           = caseDao,
            customerDao       = customerDao,
            photoDao          = photoDao,
            inspectionDao     = inspectionDao,
            workflowTaskDao   = workflowTaskDao,
            technicianDao     = technicianDao,
            inventoryDao      = inventoryDao,
            damageFindingDao  = damageFindingDao,
            analysisResultDao = analysisResultDao,
            videoDao          = videoDao,
            motionDataDao     = motionDataDao
        )
    }

    @After fun tearDown() { unmockkAll() }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun case(id: String) = CaseEntity(
        caseId       = id,
        customerId   = "cust",
        licensePlate = "LY-$id",
        status       = CaseStatus.NEW,
        createdAt    = 0L,
        updatedAt    = 0L
    )

    private fun customer(id: String) = CustomerEntity(
        customerId = id,
        name       = "عميل $id",
        phone      = "+218901234567",
        createdAt  = 0L,
        updatedAt  = 0L
    )

    // ── observeCases ──────────────────────────────────────────────────────────

    @Test fun `observeCases null query delegates to observeAll`() = runTest {
        val expected = listOf(case("1"), case("2"))
        every { caseDao.observeAll() } returns MutableStateFlow(expected)

        repo.observeCases(null).test {
            assertEquals(expected, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `observeCases blank query delegates to observeAll`() = runTest {
        val expected = listOf(case("1"))
        every { caseDao.observeAll() } returns MutableStateFlow(expected)

        repo.observeCases("   ").test {
            assertEquals(expected, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `observeCases with query delegates to search`() = runTest {
        val expected = listOf(case("BMW-1"))
        every { caseDao.search("BMW") } returns MutableStateFlow(expected)

        repo.observeCases("BMW").test {
            assertEquals(expected, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `observeCases flow error emits empty list (catch)`() = runTest {
        every { caseDao.observeAll() } returns flow { throw RuntimeException("DB broken") }

        repo.observeCases(null).test {
            assertEquals(emptyList<CaseEntity>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── observeCase (single) ──────────────────────────────────────────────────

    @Test fun `observeCase emits from DAO`() = runTest {
        val c = case("42")
        every { caseDao.observeById("42") } returns MutableStateFlow(c)

        repo.observeCase("42").test {
            assertEquals(c, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `observeCase flow error emits null`() = runTest {
        every { caseDao.observeById(any()) } returns flow { throw RuntimeException() }

        repo.observeCase("bad").test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── observeCasesByStatus ──────────────────────────────────────────────────

    @Test fun `observeCasesByStatus delegates to dao`() = runTest {
        val inProgress = listOf(case("5"))
        every { caseDao.observeByStatus(CaseStatus.IN_PROGRESS) } returns MutableStateFlow(inProgress)

        repo.observeCasesByStatus(CaseStatus.IN_PROGRESS).test {
            assertEquals(inProgress, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── upsertCase ────────────────────────────────────────────────────────────

    @Test fun `upsertCase success returns AppResult_Success`() = runTest {
        val c = case("1")
        coEvery { caseDao.upsert(c) } just runs

        val result = repo.upsertCase(c)
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { caseDao.upsert(c) }
    }

    @Test fun `upsertCase DAO exception returns AppResult_Error`() = runTest {
        coEvery { caseDao.upsert(any()) } throws RuntimeException("constraint violation")

        val result = repo.upsertCase(case("1"))
        assertTrue(result.isError)
        assertNotNull(result.exceptionOrNull())
    }

    // ── updateCaseStatus ──────────────────────────────────────────────────────

    @Test fun `updateCaseStatus delegates to dao with correct args`() = runTest {
        coEvery { caseDao.updateStatus(any(), any(), any()) } just runs

        val result = repo.updateCaseStatus("case-1", CaseStatus.APPROVED)
        assertTrue(result.isSuccess)
        coVerify { caseDao.updateStatus("case-1", CaseStatus.APPROVED, any()) }
    }

    @Test fun `updateCaseStatus failure returns Error`() = runTest {
        coEvery { caseDao.updateStatus(any(), any(), any()) } throws RuntimeException()

        val result = repo.updateCaseStatus("case-1", CaseStatus.CANCELLED)
        assertTrue(result.isError)
    }

    // ── getCaseById ───────────────────────────────────────────────────────────

    @Test fun `getCaseById found returns Success with entity`() = runTest {
        val c = case("99")
        coEvery { caseDao.getById("99") } returns c

        val result = repo.getCaseById("99")
        assertTrue(result.isSuccess)
        assertEquals(c, result.getOrNull())
    }

    @Test fun `getCaseById not found returns Success with null`() = runTest {
        coEvery { caseDao.getById(any()) } returns null

        val result = repo.getCaseById("missing")
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    // ── Customers ─────────────────────────────────────────────────────────────

    @Test fun `observeCustomers blank query delegates to observeAll`() = runTest {
        val all = listOf(customer("c1"), customer("c2"))
        every { customerDao.observeAll() } returns MutableStateFlow(all)

        repo.observeCustomers(null).test {
            assertEquals(all, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `findCustomerById returns entity from dao`() = runTest {
        val c = customer("c1")
        coEvery { customerDao.findById("c1") } returns c

        assertEquals(c, repo.findCustomerById("c1"))
    }

    @Test fun `findCustomerById missing returns null`() = runTest {
        coEvery { customerDao.findById(any()) } returns null
        assertNull(repo.findCustomerById("ghost"))
    }

    @Test fun `upsertCustomer success returns Success`() = runTest {
        coEvery { customerDao.upsert(any()) } just runs
        val result = repo.upsertCustomer(customer("c1"))
        assertTrue(result.isSuccess)
    }

    // ── Photos ────────────────────────────────────────────────────────────────

    @Test fun `observePhotos delegates to photoDao`() = runTest {
        every { photoDao.observeByCase("case-1") } returns MutableStateFlow(emptyList())

        repo.observePhotos("case-1").test {
            assertEquals(emptyList<CasePhotoEntity>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `observePhotos error emits empty list`() = runTest {
        every { photoDao.observeByCase(any()) } returns flow { throw RuntimeException() }

        repo.observePhotos("bad-case").test {
            assertEquals(emptyList<CasePhotoEntity>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Inventory ─────────────────────────────────────────────────────────────

    @Test fun `observeLowStock delegates to inventoryDao`() = runTest {
        every { inventoryDao.observeLowStock() } returns MutableStateFlow(emptyList())

        repo.observeLowStock().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `observeLowStock error emits empty list`() = runTest {
        every { inventoryDao.observeLowStock() } returns flow { throw RuntimeException() }

        repo.observeLowStock().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
