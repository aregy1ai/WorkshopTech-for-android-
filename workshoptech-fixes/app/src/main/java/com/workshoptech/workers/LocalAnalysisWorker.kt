package com.workshoptech.workers

import android.content.Context
import androidx.work.*
import com.workshoptech.data.AppDatabase
import com.workshoptech.data.entity.AnalysisResultEntity
import com.workshoptech.data.entity.DamageFindingEntity
import com.workshoptech.ml.DamageAnalyzer
import com.workshoptech.ml.OcrEngine
import com.workshoptech.ml.QualityFilter
import com.workshoptech.ml.RecommendationEngine
import com.workshoptech.util.BitmapUtils
import com.workshoptech.util.InputValidator
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Background worker that runs the 4-layer AI analysis pipeline.
 *
 * Performance:
 *  - BitmapUtils.loadSafe() caps image resolution to 1920px to prevent OOM.
 *  - Gson singleton reused across worker calls (thread-safe).
 *
 * Security:
 *  - InputValidator.isPathSafe() guards against path-traversal attacks.
 *  - Bitmap is explicitly recycled after use.
 *
 * Stability:
 *  - runAttemptCount < MAX_RETRIES with EXPONENTIAL backoff.
 *  - Each failure produces structured output data for debugging.
 */
class LocalAnalysisWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db = AppDatabase.getInstance(applicationContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val photoId   = inputData.getString(KEY_PHOTO_ID)
            ?: return@withContext failure("Missing photoId")
        val photoPath = inputData.getString(KEY_PHOTO_PATH)
            ?: return@withContext failure("Missing photoPath")
        val countryId = inputData.getString(KEY_COUNTRY_ID) ?: "LY"

        // ── Security: path traversal guard ───────────────────────────────────
        val allowedDir = applicationContext.filesDir.canonicalPath
        if (!InputValidator.isPathSafe(photoPath, allowedDir)) {
            return@withContext failure("Unsafe path rejected: $photoPath")
        }

        val photoFile = File(photoPath)
        if (!photoFile.exists()) return@withContext failure("Photo not found: $photoPath")

        // ── Performance: memory-safe bitmap loading with EXIF correction ─────
        val bitmap = BitmapUtils.loadSafe(photoPath)
            ?: return@withContext failure("Cannot decode image")

        try {
            // ── Layer 0: Quality ──────────────────────────────────────────────
            val quality = QualityFilter.analyze(bitmap)
            db.analysisResultDao().insert(analysisResult(photoId, "QUALITY",
                mapOf(
                    "isBlurry"        to quality.isBlurry,
                    "brightnessScore" to quality.brightnessScore,
                    "hasGlare"        to quality.hasGlare,
                    "isAcceptable"    to quality.isAcceptable,
                    "suggestions"     to quality.suggestions
                ),
                confidence = if (quality.isAcceptable) 0.95f else 0.3f
            ))

            if (!quality.isAcceptable) {
                return@withContext Result.success(workDataOf(
                    "qualityRejected" to true,
                    "suggestions"     to quality.suggestions.joinToString("|")
                ))
            }

            // ── Layer 1–2: OCR plate recognition ─────────────────────────────
            val ocr = OcrEngine.recognizePlate(bitmap, countryId)
            db.analysisResultDao().insert(analysisResult(photoId, "OCR",
                mapOf(
                    "text"        to ocr.text,
                    "rawText"     to ocr.rawText,
                    "confidence"  to ocr.confidence,
                    "candidates"  to ocr.candidates,
                    "country"     to ocr.country,
                    "needsReview" to ocr.needsReview
                ),
                confidence = ocr.confidence
            ))
            if (ocr.text.isNotBlank()) {
                db.photoDao().getById(photoId)?.let { photo ->
                    db.photoDao().insert(photo.copy(ocrText = ocr.text, ocrConfidence = ocr.confidence))
                }
            }

            // ── Layer 3: Damage detection ─────────────────────────────────────
            val damage = DamageAnalyzer(applicationContext).analyze(bitmap)
            db.analysisResultDao().insert(analysisResult(photoId, "DAMAGE",
                mapOf(
                    "totalAffectedArea" to damage.totalAffectedArea,
                    "estimatedSeverity" to damage.estimatedSeverity,
                    "repairFeasibility" to damage.repairFeasibility,
                    "regionsCount"      to damage.regions.size
                ),
                confidence = 0.75f
            ))
            if (damage.regions.isNotEmpty()) {
                db.damageFindingDao().insertAll(damage.regions.map { region ->
                    DamageFindingEntity(
                        findingId    = UUID.randomUUID().toString(),
                        photoId      = photoId,
                        damageType   = region.type,
                        severity     = region.severity,
                        confidence   = region.confidence,
                        left         = region.boundingBox.left,
                        top          = region.boundingBox.top,
                        right        = region.boundingBox.right,
                        bottom       = region.boundingBox.bottom,
                        affectedPart = region.affectedPart
                    )
                })
            }

            // ── Layer 4: Cost estimation ──────────────────────────────────────
            val cost = RecommendationEngine.generateRecommendations(damage.regions)
            db.analysisResultDao().insert(analysisResult(photoId, "COST",
                mapOf(
                    "laborCost"            to cost.laborCost,
                    "materialsCost"        to cost.materialsCost,
                    "totalCost"            to cost.totalCost,
                    "currency"             to cost.currency,
                    "recommendationsCount" to cost.recommendations.size
                ),
                confidence = 0.8f
            ))

            Result.success(workDataOf(
                "ocrText"        to ocr.text,
                "damageSeverity" to damage.estimatedSeverity,
                "totalCost"      to cost.totalCost
            ))

        } catch (e: OutOfMemoryError) {
            // OOM is not retryable — fail immediately
            failure("OutOfMemoryError processing image")
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRIES) Result.retry()
            else failure(e.message ?: "Unknown error after $MAX_RETRIES attempts")
        } finally {
            // ── Performance: always recycle bitmap ────────────────────────────
            BitmapUtils.recycleQuietly(bitmap)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun failure(msg: String): Result =
        Result.failure(workDataOf("error" to msg))

    private fun analysisResult(
        photoId: String,
        layer:   String,
        data:    Map<String, Any?>,
        confidence: Float
    ) = AnalysisResultEntity(
        resultId   = UUID.randomUUID().toString(),
        photoId    = photoId,
        layer      = layer,
        version    = "1.0",
        isOnline   = false,
        rawJson    = GSON.toJson(data),
        confidence = confidence,
        createdAt  = System.currentTimeMillis()
    )

    companion object {
        const val KEY_PHOTO_ID   = "photoId"
        const val KEY_PHOTO_PATH = "photoPath"
        const val KEY_COUNTRY_ID = "countryId"
        private const val MAX_RETRIES = 3

        // Gson is thread-safe and expensive to create — share one instance
        private val GSON = Gson()

        fun enqueue(
            context:   Context,
            photoId:   String,
            photoPath: String,
            countryId: String = "LY"
        ): WorkRequest {
            val request = OneTimeWorkRequestBuilder<LocalAnalysisWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .setInputData(workDataOf(
                    KEY_PHOTO_ID   to photoId,
                    KEY_PHOTO_PATH to photoPath,
                    KEY_COUNTRY_ID to countryId
                ))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag("analysis_$photoId")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "analysis_$photoId",
                ExistingWorkPolicy.KEEP,         // prevent duplicate analysis
                request as OneTimeWorkRequest
            )
            return request
        }
    }
}
