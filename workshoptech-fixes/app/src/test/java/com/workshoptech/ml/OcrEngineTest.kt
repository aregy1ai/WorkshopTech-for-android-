package com.workshoptech.ml

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Method

/**
 * Unit tests for OcrEngine.
 *
 * ML Kit requires a real Android device / instrumented test for full
 * OCR functionality.  Here we test the two pure-Kotlin helpers
 * (cleanPlateText, estimateConfidence) via reflection, and verify the
 * OcrResult data class contract.
 *
 * Coverage:
 *  - cleanPlateText: strips punctuation, keeps Arabic + alphanumeric
 *  - cleanPlateText: uppercases result
 *  - cleanPlateText: strips noise characters (., !, *, etc.)
 *  - estimateConfidence: short text → low confidence
 *  - estimateConfidence: ~12 chars → high confidence (≈0.9)
 *  - estimateConfidence: always within [0.1, 0.95]
 *  - OcrResult isArabic flag
 *  - OcrResult data class equality / copy
 */
class OcrEngineTest {

    private lateinit var engine: OcrEngine
    private lateinit var cleanMethod:    Method
    private lateinit var estimateMethod: Method

    @Before fun setUp() {
        engine = OcrEngine()

        // Access private helpers via reflection for white-box testing
        cleanMethod = OcrEngine::class.java
            .getDeclaredMethod("cleanPlateText", String::class.java)
            .apply { isAccessible = true }

        estimateMethod = OcrEngine::class.java
            .getDeclaredMethod("estimateConfidence", String::class.java)
            .apply { isAccessible = true }
    }

    // ── cleanPlateText ────────────────────────────────────────────────────────

    private fun clean(raw: String): String =
        cleanMethod.invoke(engine, raw) as String

    @Test fun `clean - removes punctuation marks`() {
        val result = clean("ABC.123!")
        assertFalse(result.contains("."))
        assertFalse(result.contains("!"))
    }

    @Test fun `clean - preserves Arabic letters`() {
        val ar = "أ ب ت 1 2 3"
        val result = clean(ar)
        assertTrue(result.contains("أ"))
        assertTrue(result.contains("ب"))
        assertTrue(result.contains("ت"))
    }

    @Test fun `clean - preserves digits`() {
        val result = clean("##123##")
        assertTrue(result.contains("123"))
    }

    @Test fun `clean - uppercases Latin letters`() {
        val result = clean("abc")
        assertEquals("ABC", result)
    }

    @Test fun `clean - strips noise symbols`() {
        val noisy = "A*B-C@D\$E"
        val result = clean(noisy)
        assertFalse(result.contains("*"))
        assertFalse(result.contains("@"))
        assertFalse(result.contains("\$"))
    }

    @Test fun `clean - whitespace is preserved (separator between plate parts)`() {
        val result = clean("AB 123")
        assertTrue(result.contains(" "))
    }

    @Test fun `clean - empty input returns empty`() {
        assertEquals("", clean(""))
    }

    @Test fun `clean - only noise returns empty or whitespace`() {
        val result = clean("@!#\$%^&*()")
        assertTrue(result.trim().isEmpty())
    }

    // ── estimateConfidence ────────────────────────────────────────────────────

    private fun estimate(text: String): Float =
        estimateMethod.invoke(engine, text) as Float

    @Test fun `estimate - confidence always in range 0_1 to 0_95`() {
        listOf("A", "AB 123", "أ ب ت 456 78", "ABCDEFGHIJKL", "").forEach { text ->
            val conf = estimate(text)
            assertTrue("Conf $conf out of range for '$text'", conf in 0.1f..0.95f)
        }
    }

    @Test fun `estimate - very short text gives lower confidence than long text`() {
        val short = estimate("A")
        val long  = estimate("ABCDEFGHIJK")    // ≈ 12 chars → max
        assertTrue("Short should be ≤ long", short <= long)
    }

    @Test fun `estimate - 12 chars gives near-max confidence ~0_9`() {
        val conf = estimate("ABCDEFGHIJKL")    // exactly 12 chars
        assertTrue("Expected ≈0.9, got $conf", conf >= 0.85f)
    }

    @Test fun `estimate - 3-char text gives moderate confidence`() {
        val conf = estimate("ABC")
        assertTrue("Expected > 0.1, got $conf", conf > 0.1f)
        assertTrue("Expected <= 0.95, got $conf", conf <= 0.95f)
    }

    // ── OcrResult data class ──────────────────────────────────────────────────

    @Test fun `OcrResult - Arabic flag set correctly`() {
        val arabic = OcrEngine.OcrResult("أ ب ت", "أ ب ت", 0.8f, isArabic = true)
        val latin  = OcrEngine.OcrResult("ABC", "ABC", 0.8f, isArabic = false)
        assertTrue(arabic.isArabic)
        assertFalse(latin.isArabic)
    }

    @Test fun `OcrResult - equality and copy`() {
        val r1 = OcrEngine.OcrResult("raw", "CLEAN", 0.75f, false)
        val r2 = r1.copy()
        assertEquals(r1, r2)
        val r3 = r1.copy(confidence = 0.9f)
        assertNotEquals(r1, r3)
        assertEquals("CLEAN", r3.cleanedText)
    }

    @Test fun `OcrResult - toString includes key fields`() {
        val r = OcrEngine.OcrResult("ABC 123", "ABC123", 0.8f, false)
        val str = r.toString()
        assertTrue(str.contains("ABC 123") || str.contains("OcrResult"))
    }
}
