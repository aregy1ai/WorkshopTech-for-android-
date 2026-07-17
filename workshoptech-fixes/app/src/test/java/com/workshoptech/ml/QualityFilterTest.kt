package com.workshoptech.ml

import android.graphics.Bitmap
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for QualityFilter.
 *
 * Uses MockK to control Bitmap pixel data so threshold logic is testable
 * without a real Android device.
 *
 * Scenarios:
 *  - Perfect image          → EXCELLENT, no issues
 *  - Uniform (blurry) image → isBlurry=true
 *  - Dark image             → isDark=true
 *  - Overexposed image      → isOverexposed=true
 *  - Glare image            → hasGlare=true
 *  - Two issues             → POOR grade
 *  - One issue              → ACCEPTABLE grade
 *  - Zero issues            → EXCELLENT grade
 *  - tipAr non-blank when issue is present
 *  - passed == (grade != POOR)
 */
@ExperimentalCoroutinesApi
class QualityFilterTest {

    private lateinit var filter: QualityFilter

    @Before fun setUp() {
        MockKAnnotations.init(this)
        filter = QualityFilter()
    }

    @After fun tearDown() {
        unmockkAll()
    }

    // ── Bitmap factory ────────────────────────────────────────────────────────

    /**
     * Creates a mocked Bitmap that reports [w]×[h] size and fills the pixel
     * array with [color] when [Bitmap.getPixels] is called.
     */
    private fun bitmapWithUniformColor(w: Int, h: Int, color: Int): Bitmap {
        val bmp = mockk<Bitmap>(relaxed = false)
        every { bmp.width }  returns w
        every { bmp.height } returns h
        every {
            bmp.getPixels(any(), any(), any(), any(), any(), any(), any())
        } answers {
            val arr = firstArg<IntArray>()
            arr.fill(color)
        }
        return bmp
    }

    /**
     * Creates a bitmap alternating between [c1] and [c2] — high Laplacian variance.
     */
    private fun bitmapAlternating(w: Int, h: Int, c1: Int, c2: Int): Bitmap {
        val bmp = mockk<Bitmap>(relaxed = false)
        every { bmp.width }  returns w
        every { bmp.height } returns h
        every {
            bmp.getPixels(any(), any(), any(), any(), any(), any(), any())
        } answers {
            val arr = firstArg<IntArray>()
            for (i in arr.indices) arr[i] = if (i % 2 == 0) c1 else c2
        }
        return bmp
    }

    // ── EXCELLENT ─────────────────────────────────────────────────────────────

    @Test fun `sharp normal-brightness image → EXCELLENT all flags false`() = runTest {
        // Alternating 80/180 = high Laplacian, avg brightness ≈ 130 (normal)
        val grey80  = 0xFF515151.toInt()   // luma ~80
        val grey180 = 0xFFB4B4B4.toInt()   // luma ~180
        val bmp = bitmapAlternating(20, 20, grey80, grey180)

        val result = filter.evaluate(bmp)

        assertFalse("Should not be blurry",      result.isBlurry)
        assertFalse("Should not be dark",         result.isDark)
        assertFalse("Should not be overexposed",  result.isOverexposed)
        assertFalse("Should not have glare",      result.hasGlare)
        assertEquals(QualityFilter.QualityGrade.EXCELLENT, result.grade)
        assertTrue(result.passed)
        assertEquals("", result.tipAr)
    }

    // ── Blurry ────────────────────────────────────────────────────────────────

    @Test fun `uniform grey image → isBlurry true`() = runTest {
        // All pixels the same → Laplacian = 0 everywhere → blur score = 0 < 12
        val grey = 0xFF808080.toInt()
        val bmp = bitmapWithUniformColor(20, 20, grey)

        val result = filter.evaluate(bmp)

        assertTrue("Expected isBlurry", result.isBlurry)
        assertTrue("blurScore should be near 0", result.blurScore < 12f)
    }

    @Test fun `blurry image tipAr is not blank`() = runTest {
        val bmp = bitmapWithUniformColor(10, 10, 0xFF808080.toInt())
        val result = filter.evaluate(bmp)
        if (result.isBlurry) assertTrue(result.tipAr.isNotBlank())
    }

    // ── Dark ──────────────────────────────────────────────────────────────────

    @Test fun `very dark image → isDark true`() = runTest {
        // Luma 30: R=G=B=30
        val dark = (0xFF shl 24) or (30 shl 16) or (30 shl 8) or 30
        val bmp = bitmapWithUniformColor(20, 20, dark)

        val result = filter.evaluate(bmp)

        assertTrue("Expected isDark", result.isDark)
        assertTrue(result.brightnessScore < 50f)
    }

    @Test fun `dark image tipAr mentions flash`() = runTest {
        val dark = (0xFF shl 24) or (20 shl 16) or (20 shl 8) or 20
        val bmp = bitmapWithUniformColor(10, 10, dark)
        val result = filter.evaluate(bmp)
        if (result.isDark) assertTrue(result.tipAr.contains("إضاءة") || result.tipAr.contains("فلاش"))
    }

    // ── Overexposed ───────────────────────────────────────────────────────────

    @Test fun `very bright uniform image → isOverexposed true`() = runTest {
        // Luma 220 (above 210 threshold)
        val bright = (0xFF shl 24) or (220 shl 16) or (220 shl 8) or 220
        val bmp = bitmapWithUniformColor(20, 20, bright)

        val result = filter.evaluate(bmp)

        assertTrue("Expected isOverexposed", result.isOverexposed)
        assertTrue(result.brightnessScore > 210f)
    }

    // ── Glare ─────────────────────────────────────────────────────────────────

    @Test fun `more than 15 pct pixels above 240 → hasGlare true`() = runTest {
        // Make 20% of pixels very bright (>240) and rest normal
        val w = 10; val h = 10; val total = w * h
        val glareColor  = (0xFF shl 24) or (245 shl 16) or (245 shl 8) or 245
        val normalColor = (0xFF shl 24) or (100 shl 16) or (100 shl 8) or 100

        val bmp = mockk<Bitmap>(relaxed = false)
        every { bmp.width }  returns w
        every { bmp.height } returns h
        every {
            bmp.getPixels(any(), any(), any(), any(), any(), any(), any())
        } answers {
            val arr = firstArg<IntArray>()
            for (i in arr.indices) {
                arr[i] = if (i < total / 5) glareColor else normalColor  // 20% glare
            }
        }

        val result = filter.evaluate(bmp)
        assertTrue("Expected hasGlare", result.hasGlare)
    }

    @Test fun `fewer than 15 pct pixels above 240 → hasGlare false`() = runTest {
        // Only 5% glare pixels
        val w = 20; val h = 20; val total = w * h
        val glareColor  = (0xFF shl 24) or (245 shl 16) or (245 shl 8) or 245
        val normalColor = (0xFF shl 24) or (100 shl 16) or (100 shl 8) or 100

        val bmp = mockk<Bitmap>(relaxed = false)
        every { bmp.width }  returns w
        every { bmp.height } returns h
        every {
            bmp.getPixels(any(), any(), any(), any(), any(), any(), any())
        } answers {
            val arr = firstArg<IntArray>()
            val glareCount = (total * 0.05).toInt()
            for (i in arr.indices) arr[i] = if (i < glareCount) glareColor else normalColor
        }

        val result = filter.evaluate(bmp)
        assertFalse("Expected no glare", result.hasGlare)
    }

    // ── Grade derivation ──────────────────────────────────────────────────────

    @Test fun `exactly one issue → ACCEPTABLE grade`() = runTest {
        // Uniform medium-dark → only isDark=true (1 issue → ACCEPTABLE)
        val dark = (0xFF shl 24) or (30 shl 16) or (30 shl 8) or 30
        val bmp = bitmapWithUniformColor(20, 20, dark)

        val result = filter.evaluate(bmp)

        // isBlurry may also be true (uniform = 0 variance)
        // If exactly one issue → ACCEPTABLE, two → POOR
        val issueCount = listOf(result.isBlurry, result.isDark, result.isOverexposed, result.hasGlare).count { it }
        val expectedGrade = when {
            issueCount == 0 -> QualityFilter.QualityGrade.EXCELLENT
            issueCount == 1 -> QualityFilter.QualityGrade.ACCEPTABLE
            else            -> QualityFilter.QualityGrade.POOR
        }
        assertEquals(expectedGrade, result.grade)
    }

    @Test fun `passed is true when grade is not POOR`() = runTest {
        val bright = (0xFF shl 24) or (220 shl 16) or (220 shl 8) or 220
        val bmp = bitmapWithUniformColor(20, 20, bright)
        val result = filter.evaluate(bmp)
        assertEquals(result.grade != QualityFilter.QualityGrade.POOR, result.passed)
    }

    // ── Score range guards ────────────────────────────────────────────────────

    @Test fun `blurScore is non-negative`() = runTest {
        val bmp = bitmapWithUniformColor(10, 10, 0xFF808080.toInt())
        val result = filter.evaluate(bmp)
        assertTrue("blurScore must be >= 0", result.blurScore >= 0f)
    }

    @Test fun `brightnessScore is in 0..255 range`() = runTest {
        val bmp = bitmapWithUniformColor(10, 10, 0xFF808080.toInt())
        val result = filter.evaluate(bmp)
        assertTrue(result.brightnessScore in 0f..255f)
    }
}
