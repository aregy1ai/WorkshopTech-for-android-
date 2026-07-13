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

    private val database = AppDatabase.getInstance(context)
    private val gson = Gson()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val photoId = inputData.getString(KEY_PHOTO_ID)
            ?: return@withContext Result.failure(
                workDataOf("error" to "Missing photoId")
            )
        val photoPath = inputData.getString(KEY_PHOTO_PATH)
            ?: return@withContext Result.failure(
                workDataOf("error" to "Missing photoPath")
            )
        val countryId = inputData.getString(KEY_COUNTRY_ID) ?: "LY"

        try {
            val photoFile = File(photoPath)
            if (!photoFile.exists()) {
                return@withContext Result.failure(
                    workDataOf("error" to "Photo file not found: $photoPath")
                )
            }

            val bitmap = BitmapFactory.decodeFile(photoPath)
                ?: return@withContext Result.failure(
                    workDataOf("error" to "Cannot decode image")
                )

            // 1. Quality check
            val qualityResult = QualityFilter.analyze(bitmap)
            val qualityEntity = AnalysisResultEntity(
                resultId = UUID.randomUUID().toString(),
                photoId = photoId,
                layer = "QUALITY",
                version = "1.0",
                isOnline = false,
                rawJson = gson.toJson(
                    mapOf(
                        "isBlurry" to qualityResult.isBlurry,
                        "brightnessScore" to qualityResult.brightnessScore,
                        "hasGlare" to qualityResult.hasGlare,
                        "isAcceptable" to qualityResult.isAcceptable,
                        "suggestions" to qualityResult.suggestions
                    )
                ),
                confidence = if (qualityResult.isAcceptable) 0.95f else 0.3f,
                createdAt = System.currentTimeMillis()
            )
            database.analysisResultDao().insert(qualityEntity)

            if (!qualityResult.isAcceptable) {
                return@withContext Result.success(
                    workDataOf("qualityRejected" to true, "suggestions" to qualityResult.suggestions.joinToString("|"))
                )
            }

            // 2. OCR plate recognition
            val ocrResult = OcrEngine.recognizePlate(bitmap, countryId)
            val ocrEntity = AnalysisResultEntity(
                resultId = UUID.randomUUID().toString(),
                photoId = photoId,
                layer = "OCR",
                version = "1.0",
                isOnline = false,
                rawJson = gson.toJson(
                    mapOf(
                        "text" to ocrResult.text,
                        "rawText" to ocrResult.rawText,
                        "confidence" to ocrResult.confidence,
                        "candidates" to ocrResult.candidates,
                        "country" to ocrResult.country,
                        "needsReview" to ocrResult.needsReview
                    )
                ),
                confidence = ocrResult.confidence,
                createdAt = System.currentTimeMillis()
            )
            database.analysisResultDao().insert(ocrEntity)

            // Update photo with OCR text if found
            if (ocrResult.text.isNotBlank()) {
                val photo = database.photoDao().getById(photoId)
                if (photo != null) {
                    database.photoDao().insert(
                        photo.copy(
                            ocrText = ocrResult.text,
                            ocrConfidence = ocrResult.confidence
                        )
                    )
                }
            }

            // 3. Damage analysis
            val damageReport = DamageAnalyzer(applicationContext).analyze(bitmap)
            val damageEntity = AnalysisResultEntity(
                resultId = UUID.randomUUID().toString(),
                photoId = photoId,
                layer = "DAMAGE",
                version = "1.0",
                isOnline = false,
                rawJson = gson.toJson(
                    mapOf(
                        "totalAffectedArea" to damageReport.totalAffectedArea,
                        "estimatedSeverity" to damageReport.estimatedSeverity,
                        "repairFeasibility" to damageReport.repairFeasibility,
                        "regionsCount" to damageReport.regions.size
                    )
                ),
                confidence = 0.75f,
                createdAt = System.currentTimeMillis()
            )
            database.analysisResultDao().insert(damageEntity)

            // Persist individual damage findings
            damageReport.regions.forEach { region ->
                val finding = DamageFindingEntity(
                    findingId = UUID.randomUUID().toString(),
                    photoId = photoId,
                    damageType = region.type,
                    severity = region.severity,
                    confidence = region.confidence,
                    left = region.boundingBox.left,
                    top = region.boundingBox.top,
                    right = region.boundingBox.right,
                    bottom = region.boundingBox.bottom,
                    affectedPart = region.affectedPart
                )
                database.damageFindingDao().insert(finding)
            }

            // 4. Cost estimation
            val costEstimate = RecommendationEngine.generateRecommendations(damageReport.regions)
            val costEntity = AnalysisResultEntity(
                resultId = UUID.randomUUID().toString(),
                photoId = photoId,
                layer = "COST",
                version = "1.0",
                isOnline = false,
                rawJson = gson.toJson(
                    mapOf(
                        "laborCost" to costEstimate.laborCost,
                        "materialsCost" to costEstimate.materialsCost,
                        "totalCost" to costEstimate.totalCost,
                        "currency" to costEstimate.currency,
                        "recommendationsCount" to costEstimate.recommendations.size
                    )
                ),
                confidence = 0.8f,
                createdAt = System.currentTimeMillis()
            )
            database.analysisResultDao().insert(costEntity)

            Result.success(
                workDataOf(
                    "ocrText" to ocrResult.text,
                    "damageSeverity" to damageReport.estimatedSeverity,
                    "totalCost" to costEstimate.totalCost
                )
            )
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure(workDataOf("error" to (e.message ?: "Unknown error")))
            }
        }
    }

    companion object {
        const val KEY_PHOTO_ID = "photoId"
        const val KEY_PHOTO_PATH = "photoPath"
        const val KEY_COUNTRY_ID = "countryId"
        private const val MAX_RETRY_ATTEMPTS = 3

        fun enqueue(context: Context, photoId: String, photoPath: String, countryId: String): androidx.work.WorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val inputData = workDataOf(
                KEY_PHOTO_ID to photoId,
                KEY_PHOTO_PATH to photoPath,
                KEY_COUNTRY_ID to countryId
            )

            return OneTimeWorkRequestBuilder<LocalAnalysisWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .addTag("analysis_$photoId")
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()
                .also { request ->
                    WorkManager.getInstance(context).enqueue(request)
                }
        }
    }
}
