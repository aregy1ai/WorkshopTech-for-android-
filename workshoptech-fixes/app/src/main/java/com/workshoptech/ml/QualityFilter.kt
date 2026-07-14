package com.workshoptech.ml

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

/**
 * Fast image quality assessment — runs before sending to DamageAnalyzer.
 *
 * Checks:
 *  1. Blurriness  — Laplacian variance
 *  2. Brightness  — average luminance
 *  3. Glare       — over-exposed pixel ratio
 *
 * Returns [QualityResult] with an overall [QualityGrade] and per-issue flags.
 */
class QualityFilter {

    enum class QualityGrade { EXCELLENT, ACCEPTABLE, POOR }

    data class QualityResult(
        val grade: QualityGrade,
        val isBlurry: Boolean,
        val isDark: Boolean,
        val isOverexposed: Boolean,
        val hasGlare: Boolean,
        val blurScore: Float,    // higher = sharper
        val brightnessScore: Float // 0..255
    ) {
        val passed: Boolean get() = grade != QualityGrade.POOR

        val tipAr: String get() = when {
            isBlurry       -> "ثبّت الهاتف وأعد التصوير"
            isDark         -> "استخدم الفلاش أو أضف إضاءة"
            isOverexposed  -> "قلّل الإضاءة أو ابتعد عن مصدر الضوء"
            hasGlare       -> "غيّر زاوية التصوير لتجنب الانعكاس"
            else           -> ""
        }
    }

    suspend fun evaluate(bitmap: Bitmap): QualityResult = withContext(Dispatchers.Default) {
        val grey = toGreyscale(bitmap)

        val blur       = computeBlurScore(grey)
        val brightness = computeAverageBrightness(grey)
        val glareRatio = computeGlareRatio(grey)

        val isBlurry      = blur < BLUR_THRESHOLD
        val isDark        = brightness < DARK_THRESHOLD
        val isOverexposed = brightness > BRIGHT_THRESHOLD
        val hasGlare      = glareRatio > GLARE_THRESHOLD

        val issueCount = listOf(isBlurry, isDark, isOverexposed, hasGlare).count { it }
        val grade = when {
            issueCount == 0 -> QualityGrade.EXCELLENT
            issueCount == 1 -> QualityGrade.ACCEPTABLE
            else            -> QualityGrade.POOR
        }

        QualityResult(
            grade          = grade,
            isBlurry       = isBlurry,
            isDark         = isDark,
            isOverexposed  = isOverexposed,
            hasGlare       = hasGlare,
            blurScore      = blur,
            brightnessScore = brightness
        )
    }

    // ── Pixel analysis helpers ──────────────────────────────────────────────

    private fun toGreyscale(bmp: Bitmap): IntArray {
        val w = bmp.width; val h = bmp.height
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        return IntArray(pixels.size) { i ->
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr  8) and 0xFF
            val b =  c         and 0xFF
            (0.299f * r + 0.587f * g + 0.114f * b).toInt()
        }
    }

    private fun computeBlurScore(grey: IntArray): Float {
        // Simplified Laplacian variance (3-point horizontal)
        var sum = 0.0; var sum2 = 0.0; val n = grey.size - 2
        for (i in 1..n) {
            val lap = (grey[i - 1] - 2 * grey[i] + grey[i + 1]).toDouble()
            sum  += lap
            sum2 += lap * lap
        }
        val variance = (sum2 - sum * sum / n) / n
        return sqrt(variance.coerceAtLeast(0.0)).toFloat()
    }

    private fun computeAverageBrightness(grey: IntArray): Float =
        grey.map { it.toFloat() }.average().toFloat()

    private fun computeGlareRatio(grey: IntArray): Float {
        val overexposed = grey.count { it > 240 }
        return overexposed.toFloat() / grey.size
    }

    companion object {
        private const val BLUR_THRESHOLD   = 12f
        private const val DARK_THRESHOLD   = 50f
        private const val BRIGHT_THRESHOLD = 210f
        private const val GLARE_THRESHOLD  = 0.15f
    }
}
