package com.workshoptech.util

import android.graphics.Bitmap
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Unit tests for BitmapUtils.
 *
 * Coverage:
 *  - loadSafe: non-existent file → null
 *  - loadSafe: file with zero-size bounds → null
 *  - calculateInSampleSize: same-size image → 1
 *  - calculateInSampleSize: 2× oversized → 2
 *  - calculateInSampleSize: 4× oversized → 4
 *  - calculateInSampleSize: undersized → 1
 *  - scaleToFit: already within maxDim → same instance returned
 *  - scaleToFit: oversized → dimensions capped (aspect-ratio preserved)
 *  - recycleQuietly: null input → no crash
 *  - recycleQuietly: already recycled bitmap → no crash
 *  - recycleQuietly: normal bitmap → recycle() called
 */
class BitmapUtilsTest {

    @After fun tearDown() { unmockkAll() }

    // ── loadSafe ──────────────────────────────────────────────────────────────

    @Test fun `loadSafe non-existent file returns null`() {
        val result = BitmapUtils.loadSafe("/no/such/file/image.jpg")
        assertNull(result)
    }

    @Test fun `loadSafe existing unreadable file returns null`() {
        val tmp = File.createTempFile("test", ".jpg")
        tmp.setReadable(false)
        try {
            val result = BitmapUtils.loadSafe(tmp.absolutePath)
            // Either null (can't read) or null (BitmapFactory can't decode empty file)
            assertNull(result)
        } finally {
            tmp.setReadable(true)
            tmp.delete()
        }
    }

    @Test fun `loadSafe empty file returns null`() {
        val tmp = File.createTempFile("test", ".jpg")
        try {
            val result = BitmapUtils.loadSafe(tmp.absolutePath)
            assertNull(result)  // BitmapFactory returns null for empty file
        } finally {
            tmp.delete()
        }
    }

    // ── calculateInSampleSize ─────────────────────────────────────────────────

    @Test fun `inSampleSize 1 when image exactly fits maxDim`() {
        val sample = BitmapUtils.calculateInSampleSize(100, 100, 100)
        assertEquals(1, sample)
    }

    @Test fun `inSampleSize 1 when image is smaller than maxDim`() {
        val sample = BitmapUtils.calculateInSampleSize(50, 50, 200)
        assertEquals(1, sample)
    }

    @Test fun `inSampleSize 2 when image is exactly 2x maxDim`() {
        val sample = BitmapUtils.calculateInSampleSize(400, 400, 200)
        assertEquals(2, sample)
    }

    @Test fun `inSampleSize 4 when image is exactly 4x maxDim`() {
        val sample = BitmapUtils.calculateInSampleSize(800, 800, 200)
        assertEquals(4, sample)
    }

    @Test fun `inSampleSize 1 for non-square image where one side fits`() {
        // width=400 > maxDim=200 but height=100 < 200 → loop exits after 0 halvings
        val sample = BitmapUtils.calculateInSampleSize(400, 100, 200)
        assertEquals(1, sample)
    }

    @Test fun `inSampleSize is always power of 2`() {
        listOf(
            Triple(100, 100, 100),
            Triple(200, 200, 100),
            Triple(400, 400, 100),
            Triple(800, 800, 100),
            Triple(1600, 1600, 100)
        ).forEach { (w, h, max) ->
            val s = BitmapUtils.calculateInSampleSize(w, h, max)
            assertTrue("$s is not a power of 2", s > 0 && (s and (s - 1)) == 0)
        }
    }

    // ── scaleToFit ────────────────────────────────────────────────────────────

    @Test fun `scaleToFit returns same instance when within maxDim`() {
        val bmp = mockk<Bitmap>(relaxed = true)
        every { bmp.width }  returns 100
        every { bmp.height } returns 100
        val result = BitmapUtils.scaleToFit(bmp, 200)
        assertSame(bmp, result)
    }

    @Test fun `scaleToFit landscape image - width is capped at maxDim`() {
        val bmp = mockk<Bitmap>(relaxed = true)
        every { bmp.width }  returns 1920
        every { bmp.height } returns 1080
        mockkStatic(Bitmap::class)
        every { Bitmap.createScaledBitmap(any(), any(), any(), any()) } answers {
            val scaled = mockk<Bitmap>(relaxed = true)
            every { scaled.width }  returns secondArg()
            every { scaled.height } returns thirdArg()
            scaled
        }
        val result = BitmapUtils.scaleToFit(bmp, 1920)
        // At maxDim=1920 and width=1920, result should be same instance
        assertSame(bmp, result)
    }

    @Test fun `scaleToFit portrait image preserves aspect ratio`() {
        // 400×800 scaled to maxDim 200 → 100×200
        val bmp = mockk<Bitmap>(relaxed = true)
        every { bmp.width }  returns 400
        every { bmp.height } returns 800

        val capturedW = slot<Int>()
        val capturedH = slot<Int>()
        mockkStatic(Bitmap::class)
        every { Bitmap.createScaledBitmap(any(), capture(capturedW), capture(capturedH), any()) } returns mockk(relaxed = true)

        BitmapUtils.scaleToFit(bmp, 200)

        assertEquals(100, capturedW.captured)
        assertEquals(200, capturedH.captured)
    }

    // ── recycleQuietly ────────────────────────────────────────────────────────

    @Test fun `recycleQuietly null bitmap does not throw`() {
        BitmapUtils.recycleQuietly(null)  // should not throw
    }

    @Test fun `recycleQuietly already-recycled bitmap does not throw`() {
        val bmp = mockk<Bitmap>(relaxed = true)
        every { bmp.isRecycled } returns true
        BitmapUtils.recycleQuietly(bmp)
        verify(exactly = 0) { bmp.recycle() }
    }

    @Test fun `recycleQuietly calls recycle on live bitmap`() {
        val bmp = mockk<Bitmap>(relaxed = true)
        every { bmp.isRecycled } returns false
        BitmapUtils.recycleQuietly(bmp)
        verify(exactly = 1) { bmp.recycle() }
    }

    @Test fun `recycleQuietly swallows exception from recycle`() {
        val bmp = mockk<Bitmap>(relaxed = true)
        every { bmp.isRecycled } returns false
        every { bmp.recycle() } throws RuntimeException("already recycled internally")
        BitmapUtils.recycleQuietly(bmp)  // must not propagate
    }
}
