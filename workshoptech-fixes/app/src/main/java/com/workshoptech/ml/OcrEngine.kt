package com.workshoptech.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.arabic.ArabicTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * License-plate OCR using ML Kit.
 *
 * Tries Arabic recognizer first (22 Arab countries), falls back to Latin.
 * Returns [OcrResult] with cleaned plate text and confidence score.
 */
class OcrEngine {

    private val arabicRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(ArabicTextRecognizerOptions.Builder().build())
    }

    private val latinRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    data class OcrResult(
        val rawText: String,
        val cleanedText: String,
        val confidence: Float,
        val isArabic: Boolean
    )

    /**
     * Run OCR on [bitmap] and return the best plate candidate.
     * Returns null if no text detected.
     */
    suspend fun recognizePlate(bitmap: Bitmap): OcrResult? = withContext(Dispatchers.IO) {
        val image = InputImage.fromBitmap(bitmap, 0)
        try {
            val arabicResult = arabicRecognizer.process(image).await()
            if (arabicResult.text.isNotBlank()) {
                val cleaned = cleanPlateText(arabicResult.text)
                val conf    = estimateConfidence(arabicResult.text)
                return@withContext OcrResult(arabicResult.text, cleaned, conf, isArabic = true)
            }
            val latinResult = latinRecognizer.process(image).await()
            if (latinResult.text.isNotBlank()) {
                val cleaned = cleanPlateText(latinResult.text)
                val conf    = estimateConfidence(latinResult.text)
                return@withContext OcrResult(latinResult.text, cleaned, conf, isArabic = false)
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /** Remove noise characters, keep alphanumeric and Arabic letters. */
    private fun cleanPlateText(raw: String): String =
        raw.replace(Regex("[^\\w\\u0600-\\u06FF\\s]"), "")
            .trim()
            .uppercase()

    /** Rough confidence from block count and text length. */
    private fun estimateConfidence(text: String): Float =
        (text.length.coerceIn(3, 12) / 12f * 0.9f).coerceIn(0.1f, 0.95f)

    fun close() {
        arabicRecognizer.close()
        latinRecognizer.close()
    }
}
