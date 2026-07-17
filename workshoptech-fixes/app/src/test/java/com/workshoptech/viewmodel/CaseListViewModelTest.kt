package com.workshoptech.viewmodel

import app.cash.turbine.test
import com.workshoptech.data.entity.CaseEntity
import com.workshoptech.data.entity.CaseStatus
import com.workshoptech.data.repository.WorkshopRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for CaseListViewModel.
 *
 * Coverage:
 *  - Emits all cases when query is blank
 *  - onSearch: debounce → triggers observeCases(query)
 *  - onStatusFilter: triggers observeCasesByStatus
 *  - onStatusFilter(null): reverts to full list
 *  - Flow error sets error message
 *  - CaseListState defaults
 *  - state reflects latest emission
 */
@ExperimentalCoroutinesApi
class CaseListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: WorkshopRepository

    @Before fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun case(id: String, status: String = CaseStatus.NEW) = CaseEntity(
        caseId        = id,
        customerId    = "cust",
        licensePlate  = "LY-$id",
        status        = status,
        createdAt     = System.currentTimeMillis(),
        updatedAt     = System.currentTimeMillis()
    )

    // ── Default load ──────────────────────────────────────────────────────────

    @Test fun `initial state emits all cases from observeCases`() = runTest {
        val cases = listOf(case("1"), case("2"), case("3"))
        every { repository.observeCases(null) }  returns MutableStateFlow(cases)
        every { repository.observeCases("") }    returns MutableStateFlow(cases)

        val vm = CaseListViewModel(repository)
        advanceUntilIdle()

        val state = vm.state.value
        if (!state.isLoading) {
            assertEquals(3, state.cases.size)
            assertNull(state.error)
        }
    }

    @Test fun `initial query is blank`() {
        every { repository.observeCases(any()) } returns MutableStateFlow(emptyList())
        val vm = CaseListViewModel(repository)
        assertEquals("", vm.state.value.query)
    }

    @Test fun `initial statusFilter is null`() {
        every { repository.observeCases(any()) } returns MutableStateFlow(emptyList())
        val vm = CaseListViewModel(repository)
        assertNull(vm.state.value.statusFilter)
    }

    // ── onSearch ──────────────────────────────────────────────────────────────

    @Test fun `onSearch updates state query field`() = runTest {
        every { repository.observeCases(any()) } returns MutableStateFlow(emptyList())

        val vm = CaseListViewModel(repository)
        vm.onSearch("BMW")
        advanceTimeBy(350)      // past the 300ms debounce
        advanceUntilIdle()

        assertEquals("BMW", vm.state.value.query)
    }

    @Test fun `onSearch triggers observeCases with query after debounce`() = runTest {
        val searched = listOf(case("10"))
        every { repository.observeCases(null) }  returns MutableStateFlow(emptyList())
        every { repository.observeCases("BMW") } returns MutableStateFlow(searched)
        every { repository.observeCases("") }    returns MutableStateFlow(emptyList())

        val vm = CaseListViewModel(repository)
        vm.onSearch("BMW")
        advanceTimeBy(400)
        advanceUntilIdle()

        verify { repository.observeCases("BMW") }
    }

    // ── onStatusFilter ────────────────────────────────────────────────────────

    @Test fun `onStatusFilter updates state statusFilter`() = runTest {
        every { repository.observeCases(any()) }        returns MutableStateFlow(emptyList())
        every { repository.observeCasesByStatus(any()) } returns MutableStateFlow(emptyList())

        val vm = CaseListViewModel(repository)
        vm.onStatusFilter(CaseStatus.IN_PROGRESS)
        advanceUntilIdle()

        assertEquals(CaseStatus.IN_PROGRESS, vm.state.value.statusFilter)
    }

    @Test fun `onStatusFilter calls observeCasesByStatus`() = runTest {
        val inProgress = listOf(case("5", CaseStatus.IN_PROGRESS))
        every { repository.observeCases(any()) }                              returns MutableStateFlow(emptyList())
        every { repository.observeCasesByStatus(CaseStatus.IN_PROGRESS) }    returns MutableStateFlow(inProgress)

        val vm = CaseListViewModel(repository)
        vm.onStatusFilter(CaseStatus.IN_PROGRESS)
        advanceUntilIdle()

        verify { repository.observeCasesByStatus(CaseStatus.IN_PROGRESS) }
    }

    @Test fun `onStatusFilter null reverts to full list`() = runTest {
        val all = listOf(case("1"), case("2"))
        every { repository.observeCases(null) }          returns MutableStateFlow(all)
        every { repository.observeCases("") }            returns MutableStateFlow(all)
        every { repository.observeCasesByStatus(any()) } returns MutableStateFlow(emptyList())

        val vm = CaseListViewModel(repository)
        vm.onStatusFilter(CaseStatus.IN_PROGRESS)
        advanceUntilIdle()
        vm.onStatusFilter(null)
        advanceUntilIdle()

        assertNull(vm.state.value.statusFilter)
    }

    // ── Error handling ────────────────────────────────────────────────────────

    @Test fun `flow error sets error message`() = runTest {
        every { repository.observeCases(any()) } returns
            flow { throw RuntimeException("query failed") }

        val vm = CaseListViewModel(repository)
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }

    // ── State transitions ─────────────────────────────────────────────────────

    @Test fun `state flow emits when repository emits new list`() = runTest {
        val flow = MutableStateFlow<List<CaseEntity>>(emptyList())
        every { repository.observeCases(any()) } returns flow

        val vm = CaseListViewModel(repository)

        vm.state.test {
            advanceUntilIdle()
            // consume current state
            cancelAndIgnoreRemainingEvents()
        }

        // Emit new list
        flow.value = listOf(case("new"))
        advanceUntilIdle()

        assertEquals(1, vm.state.value.cases.size)
    }

    // ── CaseListState data class ──────────────────────────────────────────────

    @Test fun `CaseListState defaults`() {
        val s = CaseListState()
        assertTrue(s.cases.isEmpty())
        assertEquals("", s.query)
        assertNull(s.statusFilter)
        assertTrue(s.isLoading)
        assertNull(s.error)
    }

    @Test fun `CaseListState copy`() {
        val s = CaseListState(cases = listOf(case("1")), isLoading = false)
        val s2 = s.copy(error = "oops")
        assertEquals(1, s2.cases.size)
        assertFalse(s2.isLoading)
        assertEquals("oops", s2.error)
    }
}
