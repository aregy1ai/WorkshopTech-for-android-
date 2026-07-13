package com.workshoptech.workers

import android.content.Context
import android.graphics.BitmapFactory
import androidx.work.*
import com.workshoptech.data.AppDatabase
import com.workshoptech.data.entity.AnalysisResultEntity
import com.workshoptech.data.entity.DamageFindingEntity
import com.workshoptech.ml.DamageAnalyzer
import com.workshoptech.ml.OcrEngine
import com.workshoptech.ml.QualityFilter
import com.workshoptech.ml.RecommendationEngine
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class LocalAnalysisWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db   = AppDatabase.getInstance(applicationContext)
    private val gson = Gson()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val photoId   = inputData.getString(KEY_PHOTO_ID)
            ?: return@withContext Result.failure(workDataOf("error" to "Missing photoId"))
        val photoPath = inputData.getString(KEY_PHOTO_PATH)
            ?: return@withContext Result.failure(workDataOf("error" to "Missing photoPath"))
        val countryId = inputData.getString(KEY_COUNTRY_ID) ?: "LY"

        try {
            val photoFile = File(photoPath)
            if (!photoFile.exists())
                return@withContext Result.failure(workDataOf("error" to "Photo not found: $photoPath"))

            val bitmap = BitmapFactory.decodeFile(photoPath)
                ?: return@withContext Result.failure(workDataOf("error" to "Cannot decode image"))

            // ── Layer 0: Quality ─────────────────────────────────────────────
            val quality = QualityFilter.analyze(bitmap)
            db.analysisResultDao().insert(
                AnalysisResultEntity(
                    resultId   = UUID.randomUUID().toString(),
                    photoId    = photoId,
                    layer      = "QUALITY",
                    version    = "1.0",
                    isOnline   = false,
                    rawJson    = gson.toJson(mapOf(
                        "isBlurry"        to quality.isBlurry,
                        "brightnessScore" to quality.brightnessScore,
                        "hasGlare"        to quality.hasGlare,
                        "isAcceptable"    to quality.isAcceptable,
                        "suggestions"     to quality.suggestions
                    )),
                    confidence = if (quality.isAcceptable) 0.95f else 0.3f,
                    createdAt  = System.currentTimeMillis()
                )
            )
            if (!quality.isAcceptable)
                return@withContext Result.success(
                    workDataOf("qualityRejected" to true,
                               "suggestions" to quality.suggestions.joinToString("|"))
                )

            // ── Layer 1-2: OCR ────────────────────────────────────────────────
            val ocr = OcrEngine.recognizePlate(bitmap, countryId)
            db.analysisResultDao().insert(
                AnalysisResultEntity(
                    resultId   = UUID.randomUUID().toString(),
                    photoId    = photoId,
                    layer      = "OCR",
                    version    = "1.0",
                    isOnline   = false,
                    rawJson    = gson.toJson(mapOf(
                        "text"         to ocr.text,
                        "rawText"      to ocr.rawText,
                        "confidence"   to ocr.confidence,
                        "candidates"   to ocr.candidates,
                        "country"      to ocr.country,
                        "needsReview"  to ocr.needsReview
                    )),
                    confidence = ocr.confidence,
                    createdAt  = System.currentTimeMillis()
                )
            )
            if (ocr.text.isNotBlank()) {
                db.photoDao().getById(photoId)?.let { photo ->
                    db.photoDao().insert(photo.copy(
                        ocrText       = ocr.text,
                        ocrConfidence = ocr.confidence
                    ))
                }
            }

            // ── Layer 3: Damage ───────────────────────────────────────────────
            val damage = DamageAnalyzer(applicationContext).analyze(bitmap)
            db.analysisResultDao().insert(
                AnalysisResultEntity(
                    resultId   = UUID.randomUUID().toString(),
                    photoId    = photoId,
                    layer      = "DAMAGE",
                    version    = "1.0",
                    isOnline   = false,
                    rawJson    = gson.toJson(mapOf(
                        "totalAffectedArea"  to damage.totalAffectedArea,
                        "estimatedSeverity"  to damage.estimatedSeverity,
                        "repairFeasibility"  to damage.repairFeasibility,
                        "regionsCount"       to damage.regions.size
                    )),
                    confidence = 0.75f,
                    createdAt  = System.currentTimeMillis()
                )
            )
            damage.regions.forEach { region ->
                db.damageFindingDao().insert(
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
                )
            }

            // ── Layer 4: Cost Estimation ──────────────────────────────────────
            val cost = RecommendationEngine.generateRecommendations(damage.regions)
            db.analysisResultDao().insert(
                AnalysisResultEntity(
                    resultId   = UUID.randomUUID().toString(),
                    photoId    = photoId,
                    layer      = "COST",
                    version    = "1.0",
                    isOnline   = false,
                    rawJson    = gson.toJson(mapOf(
                        "laborCost"            to cost.laborCost,
                        "materialsCost"        to cost.materialsCost,
                        "totalCost"            to cost.totalCost,
                        "currency"             to cost.currency,
                        "recommendationsCount" to cost.recommendations.size
                    )),
                    confidence = 0.8f,
                    createdAt  = System.currentTimeMillis()
                )
            )

            Result.success(workDataOf(
                "ocrText"       to ocr.text,
                "damageSeverity" to damage.estimatedSeverity,
                "totalCost"     to cost.totalCost
            ))

        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) Result.retry()
            else Result.failure(workDataOf("error" to (e.message ?: "Unknown error")))
        }
    }

    companion object {
        const val KEY_PHOTO_ID   = "photoId"
        const val KEY_PHOTO_PATH = "photoPath"
        const val KEY_COUNTRY_ID = "countryId"
        private const val MAX_RETRY_ATTEMPTS = 3

        fun enqueue(
            context:   Context,
            photoId:   String,
            photoPath: String,
            countryId: String = "LY"
        ): androidx.work.WorkRequest {
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
                .addTag("analysis_$photoId")
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueue(request)
            return request
        }
    }
}
