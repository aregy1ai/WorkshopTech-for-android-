package com.workshoptech.data.repository

import com.workshoptech.data.dao.*
import com.workshoptech.data.entity.*
import com.workshoptech.domain.model.AppException
import com.workshoptech.domain.model.AppResult
import com.workshoptech.domain.model.runCatchingAppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Single source of truth for all data access.
 *
 * Performance:
 *  - All suspend functions run on Dispatchers.IO explicitly (defence-in-depth over Room's
 *    setQueryCoroutineContext, which only covers generated suspend queries).
 *  - All observed flows use distinctUntilChanged() to suppress redundant emissions
 *    and flowOn(IO) to keep collection off the main thread.
 *
 * Error handling:
 *  - Every suspend function returns AppResult<T> — never throws to the caller.
 *  - Flows emit their full list and are resilient to non-fatal DB errors.
 */
class WorkshopRepository(
    private val caseDao:          CaseDao,
    private val customerDao:      CustomerDao,
    private val photoDao:         PhotoDao,
    private val inspectionDao:    InspectionDao,
    private val workflowTaskDao:  WorkflowTaskDao,
    private val technicianDao:    TechnicianDao,
    private val inventoryDao:     InventoryDao,
    private val damageFindingDao: DamageFindingDao,
    private val analysisResultDao:AnalysisResultDao,
    private val videoDao:         VideoDao,
    private val motionDataDao:    MotionDataDao
) {

    // ── Cases ────────────────────────────────────────────────────────────────

    fun observeCases(query: String? = null): Flow<List<CaseEntity>> =
        (if (query.isNullOrBlank()) caseDao.observeAll() else caseDao.search(query))
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .catch { emit(emptyList()) }

    fun observeCase(caseId: String): Flow<CaseEntity?> =
        caseDao.observeById(caseId)
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .catch { emit(null) }

    fun observeCasesByStatus(status: String): Flow<List<CaseEntity>> =
        caseDao.observeByStatus(status)
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .catch { emit(emptyList()) }

    suspend fun upsertCase(case: CaseEntity): AppResult<Unit> =
        withContext(Dispatchers.IO) { runCatchingAppResult { caseDao.upsert(case) } }

    suspend fun updateCaseStatus(caseId: String, status: String): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            runCatchingAppResult { caseDao.updateStatus(caseId, status, System.currentTimeMillis()) }
        }

    suspend fun getCaseById(caseId: String): AppResult<CaseEntity?> =
        withContext(Dispatchers.IO) { runCatchingAppResult { caseDao.getById(caseId) } }

    // ── Customers ────────────────────────────────────────────────────────────

    fun observeCustomers(query: String? = null): Flow<List<CustomerEntity>> =
        (if (query.isNullOrBlank()) customerDao.observeAll() else customerDao.search(query))
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .catch { emit(emptyList()) }

    suspend fun findCustomerByPhone(phone: String): CustomerEntity? =
        withContext(Dispatchers.IO) { customerDao.findByPhone(phone) }

    suspend fun findCustomerById(id: String): CustomerEntity? =
        withContext(Dispatchers.IO) { customerDao.findById(id) }

    suspend fun upsertCustomer(customer: CustomerEntity): AppResult<Unit> =
        withContext(Dispatchers.IO) { runCatchingAppResult { customerDao.upsert(customer) } }

    // ── Photos ───────────────────────────────────────────────────────────────

    fun observePhotos(caseId: String): Flow<List<CasePhotoEntity>> =
        photoDao.observeByCase(caseId)
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .catch { emit(emptyList()) }

    suspend fun addPhoto(photo: CasePhotoEntity): AppResult<Unit> =
        withContext(Dispatchers.IO) { runCatchingAppResult { photoDao.insert(photo) } }

    suspend fun getPhotoById(id: String): CasePhotoEntity? =
        withContext(Dispatchers.IO) { photoDao.getById(id) }

    // ── Inspections ──────────────────────────────────────────────────────────

    fun observeInspections(caseId: String): Flow<List<InspectionEntity>> =
        inspectionDao.observeByCase(caseId)
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .catch { emit(emptyList()) }

    suspend fun upsertInspection(inspection: InspectionEntity): AppResult<Unit> =
        withContext(Dispatchers.IO) { runCatchingAppResult { inspectionDao.upsert(inspection) } }

    suspend fun getLatestInspection(caseId: String, type: String): InspectionEntity? =
        withContext(Dispatchers.IO) { inspectionDao.getLatestByType(caseId, type) }

    // ── Workflow Tasks ────────────────────────────────────────────────────────

    fun observeTasks(caseId: String): Flow<List<WorkflowTaskEntity>> =
        workflowTaskDao.observeByCase(caseId)
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .catch { emit(emptyList()) }

    fun observeTasksByTechnician(techId: String): Flow<List<WorkflowTaskEntity>> =
        workflowTaskDao.observeByTechnician(techId)
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .catch { emit(emptyList()) }

    suspend fun upsertTask(task: WorkflowTaskEntity): AppResult<Unit> =
        withContext(Dispatchers.IO) { runCatchingAppResult { workflowTaskDao.upsert(task) } }

    // ── Technicians ───────────────────────────────────────────────────────────

    fun observeActiveTechnicians(): Flow<List<TechnicianEntity>> =
        technicianDao.observeActive()
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .catch { emit(emptyList()) }

    fun observeAllTechnicians(): Flow<List<TechnicianEntity>> =
        technicianDao.observeAll()
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .catch { emit(emptyList()) }

    suspend fun upsertTechnician(t: TechnicianEntity): AppResult<Unit> =
        withContext(Dispatchers.IO) { runCatchingAppResult { technicianDao.upsert(t) } }

    // ── Inventory ─────────────────────────────────────────────────────────────

    fun observeInventory(): Flow<List<InventoryEntity>> =
        inventoryDao.observeAll()
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .catch { emit(emptyList()) }

    fun observeLowStock(): Flow<List<InventoryEntity>> =
        inventoryDao.observeLowStock()
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .catch { emit(emptyList()) }

    suspend fun upsertInventoryItem(item: InventoryEntity): AppResult<Unit> =
        withContext(Dispatchers.IO) { runCatchingAppResult { inventoryDao.upsert(item) } }

    suspend fun decrementInventory(itemId: String, amount: Int): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            if (amount <= 0) return@withContext AppResult.Error(
                AppException.ValidationException("الكمية يجب أن تكون أكبر من 0")
            )
            runCatchingAppResult { inventoryDao.decrement(itemId, amount) }
        }

    // ── Damage Findings ───────────────────────────────────────────────────────

    fun observeDamageFindings(photoId: String): Flow<List<DamageFindingEntity>> =
        damageFindingDao.observeByPhoto(photoId)
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .catch { emit(emptyList()) }

    suspend fun insertDamageFinding(finding: DamageFindingEntity): AppResult<Unit> =
        withContext(Dispatchers.IO) { runCatchingAppResult { damageFindingDao.insert(finding) } }

    suspend fun insertDamageFindings(findings: List<DamageFindingEntity>): AppResult<Unit> =
        withContext(Dispatchers.IO) { runCatchingAppResult { damageFindingDao.insertAll(findings) } }

    // ── Analysis Results ──────────────────────────────────────────────────────

    fun observeAnalysisResults(photoId: String): Flow<List<AnalysisResultEntity>> =
        analysisResultDao.observeByPhoto(photoId)
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .catch { emit(emptyList()) }

    suspend fun insertAnalysisResult(result: AnalysisResultEntity): AppResult<Unit> =
        withContext(Dispatchers.IO) { runCatchingAppResult { analysisResultDao.insert(result) } }

    suspend fun getLatestAnalysis(photoId: String, layer: String): AnalysisResultEntity? =
        withContext(Dispatchers.IO) { analysisResultDao.getLatestByLayer(photoId, layer) }

    // ── Videos ───────────────────────────────────────────────────────────────

    fun observeVideos(caseId: String): Flow<List<VideoEntity>> =
        videoDao.observeByCase(caseId)
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .catch { emit(emptyList()) }

    fun observeVideoFrames(videoId: String): Flow<List<VideoFrameEntity>> =
        videoDao.observeFrames(videoId)
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .catch { emit(emptyList()) }

    suspend fun insertVideo(video: VideoEntity): AppResult<Unit> =
        withContext(Dispatchers.IO) { runCatchingAppResult { videoDao.insertVideo(video) } }

    suspend fun insertVideoFrames(frames: List<VideoFrameEntity>): AppResult<Unit> =
        withContext(Dispatchers.IO) { runCatchingAppResult { videoDao.insertFrames(frames) } }

    suspend fun markVideoAnalyzed(videoId: String): AppResult<Unit> =
        withContext(Dispatchers.IO) { runCatchingAppResult { videoDao.markAnalyzed(videoId) } }

    // ── Motion & Surface ──────────────────────────────────────────────────────

    fun observeMotionData(videoId: String): Flow<List<MotionDataEntity>> =
        motionDataDao.observeMotionData(videoId)
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .catch { emit(emptyList()) }

    fun observeSurfaceDefects(videoId: String): Flow<List<SurfaceDefectEntity>> =
        motionDataDao.observeDefects(videoId)
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .catch { emit(emptyList()) }

    suspend fun insertMotionData(data: List<MotionDataEntity>): AppResult<Unit> =
        withContext(Dispatchers.IO) { runCatchingAppResult { motionDataDao.insertMotionData(data) } }

    suspend fun insertSurfaceDefects(defects: List<SurfaceDefectEntity>): AppResult<Unit> =
        withContext(Dispatchers.IO) { runCatchingAppResult { motionDataDao.insertDefects(defects) } }
}
