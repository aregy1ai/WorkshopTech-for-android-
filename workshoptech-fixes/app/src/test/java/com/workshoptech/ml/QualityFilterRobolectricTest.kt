package com.workshoptech.ml

import android.graphics.Bitmap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * QualityFilter tests using Robolectric so Bitmap.createBitmap() works on JVM.
 * These complement QualityFilterTest (MockK-based) with real pixel operations.
 *
 * @Config sdk = [35] targets Android 15 (same as compileSdk).
 *
 * Coverage:
 *  - Real uniform grey bitmap → isBlurry = true, correct brightness
 *  - Real all-dark bitmap → isDark = true
 *  - Real all-bright bitmap → isOverexposed = true
 *  - Real high-glare bitmap → hasGlare = true
 *  - Real sharp (alternating) bitmap → EXCELLENT grade
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class QualityFilterRobolectricTest {

    private lateinit var filter: QualityFilter

    @Before fun setUp() { filter = QualityFilter() }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Creates a [size]×[size] bitmap filled with a single ARGB [color]. */
    private fun uniformBitmap(size: Int, color: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size) { color }
        bmp.setPixels(pixels, 0, size, 0, 0, size, size)
        return bmp
    }

    /** Creates a [size]×[size] bitmap alternating between [c1] and [c2]. */
    private fun alternatingBitmap(size: Int, c1: Int, c2: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size) { i -> if (i % 2 == 0) c1 else c2 }
        bmp.setPixels(pixels, 0, size, 0, 0, size, size)
        return bmp
    }

    // ── Blurry ────────────────────────────────────────────────────────────────

    @Test fun `uniform grey bitmap → isBlurry true`() = runTest {
        val grey = 0xFF808080.toInt()
        val result = filter.evaluate(uniformBitmap(20, grey))
        assertTrue("Expected isBlurry with uniform bitmap", result.isBlurry)
        assertEquals(0f, result.blurScore, 0.5f)   // Laplacian ≈ 0
    }

    // ── Dark ──────────────────────────────────────────────────────────────────

    @Test fun `all-dark bitmap → isDark true and brightness less than 50`() = runTest {
        val dark = 0xFF1E1E1E.toInt()   // luma ≈ 30
        val result = filter.evaluate(uniformBitmap(20, dark))
        assertTrue("Expected isDark", result.isDark)
        assertTrue(result.brightnessScore < 50f)
    }

    // ── Overexposed ───────────────────────────────────────────────────────────

    @Test fun `all-bright bitmap → isOverexposed true`() = runTest {
        val bright = 0xFFDCDCDC.toInt()  // luma ≈ 220 > 210 threshold
        val result = filter.evaluate(uniformBitmap(20, bright))
        assertTrue("Expected isOverexposed", result.isOverexposed)
        assertTrue(result.brightnessScore > 210f)
    }

    // ── Glare ─────────────────────────────────────────────────────────────────

    @Test fun `bitmap with many near-white pixels → hasGlare true`() = runTest {
        // 25×25 = 625 pixels; 200 pixels > 240 luma ≈ 32% → hasGlare
        val bmp = Bitmap.createBitmap(25, 25, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(25 * 25) { i ->
            if (i < 200) 0xFFF5F5F5.toInt() else 0xFF505050.toInt()
        }
        bmp.setPixels(pixels, 0, 25, 0, 0, 25, 25)
        val result = filter.evaluate(bmp)
        assertTrue("Expected hasGlare", result.hasGlare)
    }

    // ── Sharp / EXCELLENT ─────────────────────────────────────────────────────

    @Test fun `sharp alternating bitmap in normal range → EXCELLENT`() = runTest {
        // Alternating luma 80/180 → high Laplacian variance, avg ≈ 130
        val c1 = 0xFF505050.toInt()   // luma ≈ 80
        val c2 = 0xFFB4B4B4.toInt()   // luma ≈ 180
        val result = filter.evaluate(alternatingBitmap(20, c1, c2))

        assertFalse("Should not be blurry",     result.isBlurry)
        assertFalse("Should not be dark",        result.isDark)
        assertFalse("Should not be overexposed", result.isOverexposed)
        assertFalse("Should not have glare",     result.hasGlare)
        assertEquals(QualityFilter.QualityGrade.EXCELLENT, result.grade)
        assertTrue(result.passed)
    }

    // ── Brightness value accuracy ─────────────────────────────────────────────

    @Test fun `brightness score matches expected luma for pure grey`() = runTest {
        // R=G=B=128 → luma ≈ 0.299*128 + 0.587*128 + 0.114*128 = 128
        val grey128 = 0xFF808080.toInt()
        val result = filter.evaluate(uniformBitmap(10, grey128))
        // Allow ±2 due to floating-point rounding in luma formula
        assertEquals(128f, result.brightnessScore, 2f)
    }

    // ── tipAr accuracy ────────────────────────────────────────────────────────

    @Test fun `tipAr is blank for EXCELLENT image`() = runTest {
        val c1 = 0xFF505050.toInt()
        val c2 = 0xFFB4B4B4.toInt()
        val result = filter.evaluate(alternatingBitmap(20, c1, c2))
        if (result.grade == QualityFilter.QualityGrade.EXCELLENT) {
            assertEquals("", result.tipAr)
        }
    }
}
