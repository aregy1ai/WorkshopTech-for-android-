package com.workshoptech.ml

import com.workshoptech.data.entity.DamageFindingEntity
import com.workshoptech.data.entity.DamageSeverity
import com.workshoptech.data.entity.DamageType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for RecommendationEngine — pure Kotlin, no Android deps.
 *
 * Coverage:
 *  - Empty findings → empty list
 *  - Single scratch LOW/HIGH severity → correct repairType + cost range
 *  - Single dent LOW/HIGH
 *  - Rust → URGENT priority
 *  - Crack → PART_REPLACEMENT
 *  - PAINT_PEEL → FULL_REPAINT
 *  - Unknown type → GENERAL_REPAIR
 *  - Multiple types → sorted by priority weight
 *  - estimateTotalCost — sum of min/max ranges
 *  - costRangeDisplay — currency formatting
 *  - Deduplication: same type, multiple severities → uses highest severity
 */
class RecommendationEngineTest {

    private lateinit var engine: RecommendationEngine

    @Before fun setUp() {
        engine = RecommendationEngine("LYD")
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun finding(
        id: String,
        type: String,
        severity: String,
        part: String? = null
    ) = DamageFindingEntity(
        findingId    = id,
        photoId      = "photo-1",
        damageType   = type,
        severity     = severity,
        confidence   = 0.9f,
        affectedPart = part
    )

    // ── Empty input ───────────────────────────────────────────────────────────

    @Test fun `empty findings returns empty recommendations`() {
        val recs = engine.recommend(emptyList())
        assertTrue(recs.isEmpty())
    }

    // ── Scratch ───────────────────────────────────────────────────────────────

    @Test fun `scratch LOW severity → PAINT_TOUCH_UP MEDIUM priority low cost`() {
        val recs = engine.recommend(listOf(finding("f1", DamageType.SCRATCH, DamageSeverity.LOW)))
        assertEquals(1, recs.size)
        val r = recs[0]
        assertEquals("PAINT_TOUCH_UP", r.repairType)
        assertEquals("MEDIUM", r.priority)
        assertEquals(50.0, r.estimatedMinCost, 0.01)
        assertEquals(150.0, r.estimatedMaxCost, 0.01)
        assertTrue(r.estimatedHours > 0)
    }

    @Test fun `scratch HIGH severity → PAINT_TOUCH_UP HIGH priority higher cost`() {
        val recs = engine.recommend(listOf(finding("f1", DamageType.SCRATCH, DamageSeverity.HIGH)))
        val r = recs[0]
        assertEquals("HIGH", r.priority)
        assertTrue(r.estimatedMinCost >= 200.0)
        assertTrue(r.estimatedMaxCost >= 500.0)
    }

    // ── Dent ──────────────────────────────────────────────────────────────────

    @Test fun `dent always has HIGH priority`() {
        val recs = engine.recommend(listOf(finding("f1", DamageType.DENT, DamageSeverity.LOW)))
        assertEquals("HIGH", recs[0].priority)
        assertEquals("BODY_REPAIR", recs[0].repairType)
    }

    @Test fun `dent HIGH severity has higher cost than LOW`() {
        val low  = engine.recommend(listOf(finding("f1", DamageType.DENT, DamageSeverity.LOW)))
        val high = engine.recommend(listOf(finding("f1", DamageType.DENT, DamageSeverity.HIGH)))
        assertTrue(high[0].estimatedMinCost > low[0].estimatedMinCost)
    }

    // ── Rust ──────────────────────────────────────────────────────────────────

    @Test fun `rust → URGENT priority and RUST_TREATMENT`() {
        val recs = engine.recommend(listOf(finding("f1", DamageType.RUST, DamageSeverity.MEDIUM)))
        val r = recs[0]
        assertEquals("RUST_TREATMENT", r.repairType)
        assertEquals("URGENT", r.priority)
    }

    // ── Crack ─────────────────────────────────────────────────────────────────

    @Test fun `crack → PART_REPLACEMENT HIGH priority`() {
        val recs = engine.recommend(listOf(finding("f1", DamageType.CRACK, DamageSeverity.HIGH)))
        assertEquals("PART_REPLACEMENT", recs[0].repairType)
        assertEquals("HIGH", recs[0].priority)
    }

    // ── Paint peel ────────────────────────────────────────────────────────────

    @Test fun `paint peel → FULL_REPAINT MEDIUM priority`() {
        val recs = engine.recommend(listOf(finding("f1", DamageType.PAINT_PEEL, DamageSeverity.MEDIUM)))
        assertEquals("FULL_REPAINT", recs[0].repairType)
        assertEquals("MEDIUM", recs[0].priority)
    }

    // ── Unknown type ──────────────────────────────────────────────────────────

    @Test fun `unknown damage type → GENERAL_REPAIR LOW priority`() {
        val recs = engine.recommend(listOf(finding("f1", "MYSTERY_DAMAGE", DamageSeverity.LOW)))
        assertEquals("GENERAL_REPAIR", recs[0].repairType)
        assertEquals("LOW", recs[0].priority)
    }

    // ── Priority sorting ──────────────────────────────────────────────────────

    @Test fun `multiple types sorted by priority — URGENT first`() {
        val findings = listOf(
            finding("f1", DamageType.SCRATCH,    DamageSeverity.LOW),   // MEDIUM
            finding("f2", DamageType.RUST,       DamageSeverity.LOW),   // URGENT
            finding("f3", DamageType.PAINT_PEEL, DamageSeverity.LOW)    // MEDIUM
        )
        val recs = engine.recommend(findings)
        assertEquals("RUST_TREATMENT", recs[0].repairType)  // URGENT comes first
    }

    @Test fun `multiple types sorted — HIGH before MEDIUM`() {
        val findings = listOf(
            finding("f1", DamageType.SCRATCH, DamageSeverity.LOW),  // → MEDIUM
            finding("f2", DamageType.DENT,    DamageSeverity.LOW)   // → HIGH
        )
        val recs = engine.recommend(findings)
        assertEquals("BODY_REPAIR", recs[0].repairType)
    }

    // ── Deduplication (multiple findings of same type) ────────────────────────

    @Test fun `two scratches different severity — uses worst severity`() {
        val findings = listOf(
            finding("f1", DamageType.SCRATCH, DamageSeverity.LOW),
            finding("f2", DamageType.SCRATCH, DamageSeverity.HIGH)
        )
        val recs = engine.recommend(findings)
        // should be grouped into one recommendation using HIGH severity
        assertEquals(1, recs.size)
        assertEquals("HIGH", recs[0].priority)
    }

    @Test fun `affected parts aggregated across same-type findings`() {
        val findings = listOf(
            finding("f1", DamageType.DENT, DamageSeverity.LOW, "HOOD"),
            finding("f2", DamageType.DENT, DamageSeverity.LOW, "ROOF"),
            finding("f3", DamageType.DENT, DamageSeverity.LOW, "HOOD") // duplicate
        )
        val recs = engine.recommend(findings)
        assertEquals(1, recs.size)
        // HOOD appears only once (distinct)
        assertEquals(2, recs[0].affectedParts.size)
        assertTrue(recs[0].affectedParts.contains("HOOD"))
        assertTrue(recs[0].affectedParts.contains("ROOF"))
    }

    // ── estimateTotalCost ─────────────────────────────────────────────────────

    @Test fun `estimateTotalCost sums min and max ranges correctly`() {
        val findings = listOf(
            finding("f1", DamageType.SCRATCH, DamageSeverity.LOW),  // 50–150
            finding("f2", DamageType.PAINT_PEEL, DamageSeverity.LOW) // 100–400
        )
        val recs = engine.recommend(findings)
        val (min, max) = engine.estimateTotalCost(recs)
        assertEquals(150.0, min, 0.01)
        assertEquals(550.0, max, 0.01)
    }

    @Test fun `estimateTotalCost on empty list returns 0,0`() {
        val (min, max) = engine.estimateTotalCost(emptyList())
        assertEquals(0.0, min, 0.0)
        assertEquals(0.0, max, 0.0)
    }

    // ── costRangeDisplay ──────────────────────────────────────────────────────

    @Test fun `costRangeDisplay includes currency code`() {
        val recs = engine.recommend(listOf(finding("f1", DamageType.SCRATCH, DamageSeverity.LOW)))
        assertTrue(recs[0].costRangeDisplay.contains("LYD"))
    }

    @Test fun `custom currency shown in display`() {
        val sar = RecommendationEngine("SAR")
        val recs = sar.recommend(listOf(finding("f1", DamageType.RUST, DamageSeverity.LOW)))
        assertTrue(recs[0].costRangeDisplay.contains("SAR"))
    }

    // ── Arabic description ────────────────────────────────────────────────────

    @Test fun `all recommendation types have Arabic description`() {
        val types = listOf(
            DamageType.SCRATCH, DamageType.DENT, DamageType.RUST,
            DamageType.CRACK, DamageType.PAINT_PEEL, "UNKNOWN"
        )
        types.forEach { type ->
            val recs = engine.recommend(listOf(finding("f", type, DamageSeverity.LOW)))
            assertTrue("Missing AR desc for $type", recs[0].descriptionAr.isNotBlank())
        }
    }

    // ── Positive cost guards ──────────────────────────────────────────────────

    @Test fun `all cost ranges are positive`() {
        val types = listOf(DamageType.SCRATCH, DamageType.DENT, DamageType.RUST,
                           DamageType.CRACK, DamageType.PAINT_PEEL)
        types.forEach { type ->
            val r = engine.recommend(listOf(finding("f", type, DamageSeverity.HIGH)))[0]
            assertTrue("Min cost negative for $type", r.estimatedMinCost > 0)
            assertTrue("Max < min for $type", r.estimatedMaxCost >= r.estimatedMinCost)
            assertTrue("Hours non-positive for $type", r.estimatedHours > 0)
        }
    }
}
