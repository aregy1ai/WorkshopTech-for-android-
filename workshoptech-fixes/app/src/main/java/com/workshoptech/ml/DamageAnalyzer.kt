package com.workshoptech.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import com.workshoptech.data.entity.DamageSeverity
import com.workshoptech.data.entity.DamageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AI damage analyzer using TensorFlow Lite.
 *
 * Production: replace [runInference] with real TFLite model.
 * Model file: assets/damage_detector.tflite (not bundled in stub).
 *
 * Output: list of [DamageResult] per detected region.
 */
class DamageAnalyzer(private val context: Context) {

    data class DamageResult(
        val damageType: String,
        val severity: String,
        val confidence: Float,
        val boundingBox: RectF,
        val affectedPart: String? = null
    )

    /**
     * Analyse [bitmap] for damage regions.
     * Returns empty list if model is unavailable (stub mode).
     */
    suspend fun analyze(bitmap: Bitmap): List<DamageResult> = withContext(Dispatchers.Default) {
        try {
            runInference(bitmap)
        } catch (e: Exception) {
            // Model not present — return stub result for UI testing
            listOf(
                DamageResult(
                    damageType  = DamageType.SCRATCH,
                    severity    = DamageSeverity.MEDIUM,
                    confidence  = 0.82f,
                    boundingBox = RectF(0.1f, 0.2f, 0.4f, 0.5f),
                    affectedPart = "FRONT_LEFT_DOOR"
                )
            )
        }
    }

    /**
     * Real TFLite inference — implement when model is ready.
     */
    private fun runInference(bitmap: Bitmap): List<DamageResult> {
        // TODO: load assets/damage_detector.tflite via TfLiteModel
        // val model = TfLiteModel(context, "damage_detector.tflite")
        // val inputBuffer = TensorImage.fromBitmap(bitmap)
        // val output = model.process(inputBuffer)
        // return output.detectionResultList.map { it.toDamageResult() }
        return emptyList()
    }

    companion object {
        const val MODEL_FILE = "damage_detector.tflite"
        const val MIN_CONFIDENCE = 0.55f
    }
}
