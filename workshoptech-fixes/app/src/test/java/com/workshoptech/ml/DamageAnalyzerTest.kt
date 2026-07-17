package com.workshoptech.ml

import android.content.Context
import android.graphics.Bitmap
import com.workshoptech.data.entity.DamageSeverity
import com.workshoptech.data.entity.DamageType
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for DamageAnalyzer.
 *
 * Since the TFLite model file is not bundled in the stub, [runInference]
 * always throws/returns empty → [analyze] returns the well-known stub result.
 * We verify the stub contract exhaustively so the real implementation can
 * be swapped in later with confidence.
 *
 * Coverage:
 *  - analyze() returns non-empty list when model unavailable
 *  - Stub result has valid confidence (within [MIN_CONFIDENCE, 1.0])
 *  - Stub bounding box coordinates are normalized [0..1]
 *  - Stub damageType / severity are known constants
 *  - Multiple calls on same bitmap produce consistent results
 *  - DamageResult data class equality / copy
 *  - MIN_CONFIDENCE constant value
 */
@ExperimentalCoroutinesApi
class DamageAnalyzerTest {

    private lateinit var context: Context
    private lateinit var bitmap:  Bitmap
    private lateinit var analyzer: DamageAnalyzer

    @Before fun setUp() {
        context = mockk(relaxed = true)
        bitmap  = mockk(relaxed = true)
        analyzer = DamageAnalyzer(context)
    }

    @After fun tearDown() {
        unmockkAll()
    }

    // ── Stub contract ─────────────────────────────────────────────────────────

    @Test fun `analyze returns non-empty list when model not available`() = runTest {
        val results = analyzer.analyze(bitmap)
        assertFalse("Stub should produce at least one result", results.isEmpty())
    }

    @Test fun `stub result confidence is at least MIN_CONFIDENCE`() = runTest {
        val results = analyzer.analyze(bitmap)
        results.forEach { r ->
            assertTrue(
                "Confidence ${r.confidence} below MIN_CONFIDENCE ${DamageAnalyzer.MIN_CONFIDENCE}",
                r.confidence >= DamageAnalyzer.MIN_CONFIDENCE
            )
        }
    }

    @Test fun `stub result confidence does not exceed 1_0`() = runTest {
        val results = analyzer.analyze(bitmap)
        results.forEach { r ->
            assertTrue("Confidence > 1.0: ${r.confidence}", r.confidence <= 1.0f)
        }
    }

    @Test fun `stub bounding box is normalised within 0_0 to 1_0`() = runTest {
        val results = analyzer.analyze(bitmap)
        results.forEach { r ->
            val box = r.boundingBox
            assertTrue("left out of range: ${box.left}",   box.left   in 0f..1f)
            assertTrue("top out of range: ${box.top}",    box.top    in 0f..1f)
            assertTrue("right out of range: ${box.right}",  box.right  in 0f..1f)
            assertTrue("bottom out of range: ${box.bottom}", box.bottom in 0f..1f)
            assertTrue("left >= right",   box.left < box.right)
            assertTrue("top >= bottom",   box.top  < box.bottom)
        }
    }

    @Test fun `stub damageType is a known constant`() = runTest {
        val known = setOf(
            DamageType.SCRATCH, DamageType.DENT, DamageType.CRACK,
            DamageType.PAINT_PEEL, DamageType.RUST, DamageType.GLASS, DamageType.STRUCTURAL
        )
        val results = analyzer.analyze(bitmap)
        results.forEach { r ->
            assertTrue("Unknown damageType: ${r.damageType}", r.damageType in known)
        }
    }

    @Test fun `stub severity is a known constant`() = runTest {
        val known = setOf(DamageSeverity.LOW, DamageSeverity.MEDIUM, DamageSeverity.HIGH)
        val results = analyzer.analyze(bitmap)
        results.forEach { r ->
            assertTrue("Unknown severity: ${r.severity}", r.severity in known)
        }
    }

    // ── Consistency ───────────────────────────────────────────────────────────

    @Test fun `two calls return same number of results`() = runTest {
        val first  = analyzer.analyze(bitmap)
        val second = analyzer.analyze(bitmap)
        assertEquals(first.size, second.size)
    }

    @Test fun `two calls produce identical stub results`() = runTest {
        val first  = analyzer.analyze(bitmap)
        val second = analyzer.analyze(bitmap)
        assertEquals(first, second)
    }

    // ── DamageResult data class ───────────────────────────────────────────────

    @Test fun `DamageResult copy preserves unchanged fields`() = runTest {
        val original = analyzer.analyze(bitmap).first()
        val copied   = original.copy(severity = DamageSeverity.HIGH)
        assertEquals(original.damageType,  copied.damageType)
        assertEquals(original.confidence,  copied.confidence, 0.001f)
        assertEquals(original.boundingBox, copied.boundingBox)
        assertEquals(DamageSeverity.HIGH,  copied.severity)
    }

    @Test fun `two DamageResults with same values are equal`() = runTest {
        val r1 = analyzer.analyze(bitmap).first()
        val r2 = analyzer.analyze(bitmap).first()
        assertEquals(r1, r2)
    }

    @Test fun `DamageResults with different types are not equal`() = runTest {
        val r1 = analyzer.analyze(bitmap).first()
        val r2 = r1.copy(damageType = DamageType.DENT)
        assertNotEquals(r1, r2)
    }

    // ── Constants ─────────────────────────────────────────────────────────────

    @Test fun `MIN_CONFIDENCE is between 0 and 1 exclusive`() {
        assertTrue(DamageAnalyzer.MIN_CONFIDENCE > 0f)
        assertTrue(DamageAnalyzer.MIN_CONFIDENCE < 1f)
    }

    @Test fun `MODEL_FILE name ends with tflite extension`() {
        assertTrue(DamageAnalyzer.MODEL_FILE.endsWith(".tflite"))
    }
}
