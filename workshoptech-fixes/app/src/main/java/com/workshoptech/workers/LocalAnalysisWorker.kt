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
 * Background worker — 4-layer AI analysis pipeline.
 *
 * Layers:
 *   0. QUALITY  — image quality gate (blur / brightness / glare)
 *   1. OCR      — Arabic + Latin license plate recognition (ML Kit)
 *   2. DAMAGE   — damage region detection (TFLite)
 *   3. COST     — repair cost estimation (rule-based)
 *
 * Performance:  BitmapUtils.loadSafe() caps to 1920px (OOM guard), Gson singleton.
 * Security:     InputValidator.isPathSafe() blocks path traversal.
 * Stability:    OOM fails immediately (no retry), other errors retry up to 3x.
 */
class LocalAnalysisWorker(
    context: Context,
    params:  WorkerParameters
) : CoroutineWorker(context, params) {

    private val db             = AppDatabase.getInstance(applicationContext)
    private val qualityFilter  = QualityFilter()
    private val ocrEngine      = OcrEngine()
    private val recommendations = RecommendationEngine()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val photoId   = inputData.getString(KEY_PHOTO_ID)   ?: return@withContext failure("Missing photoId")
        val photoPath = inputData.getString(KEY_PHOTO_PATH) ?: return@withContext failure("Missing photoPath")

        // ── Security: block path traversal ───────────────────────────────────
        val safeDir = applicationContext.filesDir.canonicalPath
        if (!InputValidator.isPathSafe(photoPath, safeDir)) {
            return@withContext failure("Unsafe path: $photoPath")
        }

        if (!File(photoPath).exists()) return@withContext failure("Photo missing: $photoPath")

        // ── Performance: memory-safe decode + EXIF rotation ───────────────────
        val bitmap = BitmapUtils.loadSafe(photoPath) ?: return@withContext failure("Cannot decode image")

        try {
            // ── Layer 0: Quality ──────────────────────────────────────────────
            val quality = qualityFilter.evaluate(bitmap)
            db.analysisResultDao().insert(
                buildResult(photoId, "QUALITY",
                    mapOf(
                        "grade"          to quality.grade.name,
                        "isBlurry"       to quality.isBlurry,
                        "isDark"         to quality.isDark,
                        "isOverexposed"  to quality.isOverexposed,
                        "hasGlare"       to quality.hasGlare,
                        "blurScore"      to quality.blurScore,
                        "brightness"     to quality.brightnessScore,
                        "tip"            to quality.tipAr
                    ),
                    confidence = if (quality.passed) 0.95f else 0.35f
                )
            )

            if (!quality.passed) {
                return@withContext Result.success(workDataOf(
                    KEY_QUALITY_GRADE to quality.grade.name,
                    KEY_QUALITY_TIP   to quality.tipAr
                ))
            }

            // ── Layer 1: OCR (plate recognition) ─────────────────────────────
            val ocrResult = ocrEngine.recognizePlate(bitmap)
            if (ocrResult != null) {
                db.analysisResultDao().insert(
                    buildResult(photoId, "OCR",
                        mapOf(
                            "rawText"   to ocrResult.rawText,
                            "cleaned"   to ocrResult.cleanedText,
                            "isArabic"  to ocrResult.isArabic
                        ),
                        confidence = ocrResult.confidence
                    )
                )
                // Persist OCR text to the photo row
                db.photoDao().updateOcr(photoId, ocrResult.cleanedText, ocrResult.confidence)
            }

            // ── Layer 2: Damage detection ─────────────────────────────────────
            val damageResults = DamageAnalyzer(applicationContext).analyze(bitmap)
            db.analysisResultDao().insert(
                buildResult(photoId, "DAMAGE",
                    mapOf("regionsFound" to damageResults.size),
                    confidence = if (damageResults.isNotEmpty()) 0.80f else 0.70f
                )
            )
            if (damageResults.isNotEmpty()) {
                val findings = damageResults.map { r ->
                    DamageFindingEntity(
                        findingId    = UUID.randomUUID().toString(),
                        photoId      = photoId,
                        damageType   = r.damageType,
                        severity     = r.severity,
                        confidence   = r.confidence,
                        left         = r.boundingBox.left,
                        top          = r.boundingBox.top,
                        right        = r.boundingBox.right,
                        bottom       = r.boundingBox.bottom,
                        affectedPart = r.affectedPart
                    )
                }
                db.damageFindingDao().insertAll(findings)

                // ── Layer 3: Cost estimation ──────────────────────────────────
                val findings2 = db.damageFindingDao().getByPhoto(photoId)
                val recs      = recommendations.recommend(findings2)
                val (minCost, maxCost) = recommendations.estimateTotalCost(recs)

                db.analysisResultDao().insert(
                    buildResult(photoId, "COST",
                        mapOf(
                            "recommendationsCount" to recs.size,
                            "estimatedMinCost"     to minCost,
                            "estimatedMaxCost"     to maxCost
                        ),
                        confidence = 0.75f
                    )
                )
            }

            db.photoDao().markAnalyzed(photoId)

            Result.success(workDataOf(
                KEY_OCR_TEXT       to (ocrResult?.cleanedText ?: ""),
                KEY_DAMAGE_COUNT   to damageResults.size,
                KEY_QUALITY_GRADE  to quality.grade.name
            ))

        } catch (e: OutOfMemoryError) {
            failure("OOM — استخدام صورة أصغر")     // OOM: no retry
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRIES) Result.retry()
            else failure("خطأ بعد $MAX_RETRIES محاولات: ${e.localizedMessage}")
        } finally {
            BitmapUtils.recycleQuietly(bitmap)
            ocrEngine.close()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun failure(msg: String) = Result.failure(workDataOf(KEY_ERROR to msg))

    private fun buildResult(
        photoId:    String,
        layer:      String,
        data:       Map<String, Any?>,
        confidence: Float
    ) = AnalysisResultEntity(
        resultId   = UUID.randomUUID().toString(),
        photoId    = photoId,
        layer      = layer,
        version    = PIPELINE_VERSION,
        isOnline   = false,
        rawJson    = GSON.toJson(data),
        confidence = confidence,
        createdAt  = System.currentTimeMillis()
    )

    companion object {
        const val KEY_PHOTO_ID      = "photoId"
        const val KEY_PHOTO_PATH    = "photoPath"
        const val KEY_ERROR         = "error"
        const val KEY_OCR_TEXT      = "ocrText"
        const val KEY_DAMAGE_COUNT  = "damageCount"
        const val KEY_QUALITY_GRADE = "qualityGrade"
        const val KEY_QUALITY_TIP   = "qualityTip"
        private const val MAX_RETRIES      = 3
        private const val PIPELINE_VERSION = "1.3.0"

        private val GSON = Gson()   // thread-safe singleton

        /**
         * Enqueue a unique analysis job — KEEP policy prevents duplicate runs.
         */
        fun enqueue(context: Context, photoId: String, photoPath: String): WorkRequest {
            val request = OneTimeWorkRequestBuilder<LocalAnalysisWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .setInputData(workDataOf(
                    KEY_PHOTO_ID   to photoId,
                    KEY_PHOTO_PATH to photoPath
                ))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag("analysis_$photoId")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "analysis_$photoId",
                ExistingWorkPolicy.KEEP,
                request as OneTimeWorkRequest
            )
            return request
        }
    }
}
