package com.workshoptech.data.entity

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for CaseEntity, CaseStatus, DamageType, DamageSeverity constants.
 *
 * These are pure-Kotlin tests with no Android dependencies.
 *
 * Coverage:
 *  - CaseStatus.isActive for all statuses
 *  - CaseStatus.labelAr for all known statuses
 *  - CaseStatus.all contains all 7 statuses
 *  - DamageType.labelAr for all types
 *  - DamageSeverity.labelAr for all severities
 *  - CaseEntity copy / equality
 */
class CaseEntityTest {

    private val now = System.currentTimeMillis()

    private fun makeCase(status: String = CaseStatus.NEW) = CaseEntity(
        caseId       = "c1",
        customerId   = "cust-1",
        licensePlate = "ABC-123",
        status       = status,
        createdAt    = now,
        updatedAt    = now
    )

    // ── CaseStatus.isActive ───────────────────────────────────────────────────

    @Test fun `NEW is active`()              { assertTrue(CaseStatus.isActive(CaseStatus.NEW)) }
    @Test fun `APPROVED is active`()         { assertTrue(CaseStatus.isActive(CaseStatus.APPROVED)) }
    @Test fun `IN_PROGRESS is active`()      { assertTrue(CaseStatus.isActive(CaseStatus.IN_PROGRESS)) }
    @Test fun `READY_FOR_DELIVERY active`()  { assertTrue(CaseStatus.isActive(CaseStatus.READY_FOR_DELIVERY)) }
    @Test fun `ON_HOLD is active`()          { assertTrue(CaseStatus.isActive(CaseStatus.ON_HOLD)) }
    @Test fun `DELIVERED is not active`()    { assertFalse(CaseStatus.isActive(CaseStatus.DELIVERED)) }
    @Test fun `CANCELLED is not active`()   { assertFalse(CaseStatus.isActive(CaseStatus.CANCELLED)) }

    // ── CaseStatus.all ────────────────────────────────────────────────────────

    @Test fun `CaseStatus_all contains all 7 statuses`() {
        assertEquals(7, CaseStatus.all.size)
        val expected = setOf(
            CaseStatus.NEW, CaseStatus.APPROVED, CaseStatus.IN_PROGRESS,
            CaseStatus.READY_FOR_DELIVERY, CaseStatus.DELIVERED,
            CaseStatus.ON_HOLD, CaseStatus.CANCELLED
        )
        assertEquals(expected, CaseStatus.all.toSet())
    }

    // ── CaseStatus.labelAr ────────────────────────────────────────────────────

    @Test fun `labelAr for all known statuses is non-blank`() {
        CaseStatus.all.forEach { status ->
            val label = CaseStatus.labelAr(status)
            assertTrue("Empty label for $status", label.isNotBlank())
        }
    }

    @Test fun `labelAr for unknown status returns the status itself`() {
        assertEquals("CUSTOM_STATUS", CaseStatus.labelAr("CUSTOM_STATUS"))
    }

    // ── DamageType ────────────────────────────────────────────────────────────

    @Test fun `DamageType labelAr for all types is non-blank`() {
        val types = listOf(
            DamageType.SCRATCH, DamageType.DENT, DamageType.CRACK,
            DamageType.PAINT_PEEL, DamageType.RUST, DamageType.GLASS, DamageType.STRUCTURAL
        )
        types.forEach { type ->
            assertTrue("Empty label for $type", DamageType.labelAr(type).isNotBlank())
        }
    }

    @Test fun `DamageType labelAr unknown returns type string`() {
        assertEquals("MYSTERY", DamageType.labelAr("MYSTERY"))
    }

    // ── DamageSeverity ────────────────────────────────────────────────────────

    @Test fun `DamageSeverity labelAr for LOW MEDIUM HIGH is non-blank`() {
        listOf(DamageSeverity.LOW, DamageSeverity.MEDIUM, DamageSeverity.HIGH).forEach { s ->
            assertTrue("Empty label for $s", DamageSeverity.labelAr(s).isNotBlank())
        }
    }

    @Test fun `DamageSeverity labelAr unknown returns input`() {
        assertEquals("CRITICAL", DamageSeverity.labelAr("CRITICAL"))
    }

    // ── CaseEntity equality / copy ────────────────────────────────────────────

    @Test fun `two CaseEntity with same fields are equal`() {
        val c1 = makeCase()
        val c2 = makeCase()
        assertEquals(c1, c2)
        assertEquals(c1.hashCode(), c2.hashCode())
    }

    @Test fun `copy preserves unchanged fields`() {
        val original = makeCase(CaseStatus.NEW)
        val updated  = original.copy(status = CaseStatus.APPROVED, actualCost = 500.0)
        assertEquals(original.caseId,       updated.caseId)
        assertEquals(original.licensePlate, updated.licensePlate)
        assertEquals(CaseStatus.APPROVED,   updated.status)
        assertEquals(500.0,                 updated.actualCost)
    }

    @Test fun `CaseEntity with different IDs are not equal`() {
        val c1 = makeCase().copy(caseId = "case-1")
        val c2 = makeCase().copy(caseId = "case-2")
        assertNotEquals(c1, c2)
    }

    @Test fun `default values are correct`() {
        val c = makeCase()
        assertEquals("", c.make)
        assertEquals("", c.model)
        assertNull(c.year)
        assertEquals("", c.color)
        assertNull(c.colorCode)
        assertNull(c.colorName)
        assertNull(c.notes)
        assertNull(c.estimatedCost)
        assertNull(c.actualCost)
    }
}
