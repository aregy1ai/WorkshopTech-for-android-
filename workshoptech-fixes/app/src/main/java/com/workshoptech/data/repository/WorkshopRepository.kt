package com.workshoptech.data.repository

import com.workshoptech.data.dao.*
import com.workshoptech.data.entity.*
import com.workshoptech.domain.model.AppException
import com.workshoptech.domain.model.AppResult
import com.workshoptech.domain.model.runCatchingAppResult
import kotlinx.coroutines.flow.Flow

class WorkshopRepository(
    private val caseDao: CaseDao,
    private val customerDao: CustomerDao,
    private val photoDao: PhotoDao,
    private val inspectionDao: InspectionDao,
    private val workflowTaskDao: WorkflowTaskDao,
    private val technicianDao: TechnicianDao,
    private val inventoryDao: InventoryDao,
    private val damageFindingDao: DamageFindingDao,
    private val analysisResultDao: AnalysisResultDao,
    private val videoDao: VideoDao,
    private val motionDataDao: MotionDataDao
) {

    // ─── Cases ──────────────────────────────────────────────────────────────
    fun observeCases(query: String? = null): Flow<List<CaseEntity>> =
        if (query.isNullOrBlank()) caseDao.observeAll() else caseDao.search(query)

    fun observeCase(caseId: String): Flow<CaseEntity?> = caseDao.observeById(caseId)

    fun observeCasesByStatus(status: String): Flow<List<CaseEntity>> =
        caseDao.observeByStatus(status)

    suspend fun upsertCase(case: CaseEntity): AppResult<Unit> = runCatchingAppResult {
        caseDao.upsert(case)
    }

    suspend fun updateCaseStatus(caseId: String, status: String): AppResult<Unit> =
        runCatchingAppResult {
            caseDao.updateStatus(caseId, status, System.currentTimeMillis())
        }

    // ─── Customers ──────────────────────────────────────────────────────────
    fun observeCustomers(query: String? = null): Flow<List<CustomerEntity>> =
        if (query.isNullOrBlank()) customerDao.observeAll() else customerDao.search(query)

    suspend fun findCustomerByPhone(phone: String): CustomerEntity? =
        customerDao.findByPhone(phone)

    suspend fun findCustomerById(id: String): CustomerEntity? = customerDao.findById(id)

    suspend fun upsertCustomer(customer: CustomerEntity): AppResult<Unit> =
        runCatchingAppResult { customerDao.upsert(customer) }

    // ─── Photos ─────────────────────────────────────────────────────────────
    fun observePhotos(caseId: String): Flow<List<CasePhotoEntity>> =
        photoDao.observeByCase(caseId)

    suspend fun addPhoto(photo: CasePhotoEntity): AppResult<Unit> =
        runCatchingAppResult { photoDao.insert(photo) }

    suspend fun getPhotoById(id: String): CasePhotoEntity? = photoDao.getById(id)

    // ─── Inspections ────────────────────────────────────────────────────────
    fun observeInspections(caseId: String): Flow<List<InspectionEntity>> =
        inspectionDao.observeByCase(caseId)

    suspend fun upsertInspection(inspection: InspectionEntity): AppResult<Unit> =
        runCatchingAppResult { inspectionDao.upsert(inspection) }

    suspend fun getLatestInspection(caseId: String, type: String): InspectionEntity? =
        inspectionDao.getLatestByType(caseId, type)

    // ─── Workflow Tasks ──────────────────────────────────────────────────────
    fun observeTasks(caseId: String): Flow<List<WorkflowTaskEntity>> =
        workflowTaskDao.observeByCase(caseId)

    fun observeTasksByTechnician(techId: String): Flow<List<WorkflowTaskEntity>> =
        workflowTaskDao.observeByTechnician(techId)

    suspend fun upsertTask(task: WorkflowTaskEntity): AppResult<Unit> =
        runCatchingAppResult { workflowTaskDao.upsert(task) }

    // ─── Technicians ─────────────────────────────────────────────────────────
    fun observeActiveTechnicians(): Flow<List<TechnicianEntity>> =
        technicianDao.observeActive()

    fun observeAllTechnicians(): Flow<List<TechnicianEntity>> =
        technicianDao.observeAll()

    suspend fun upsertTechnician(t: TechnicianEntity): AppResult<Unit> =
        runCatchingAppResult { technicianDao.upsert(t) }

    // ─── Inventory ───────────────────────────────────────────────────────────
    fun observeInventory(): Flow<List<InventoryEntity>> = inventoryDao.observeAll()
    fun observeLowStock(): Flow<List<InventoryEntity>> = inventoryDao.observeLowStock()

    suspend fun upsertInventoryItem(item: InventoryEntity): AppResult<Unit> =
        runCatchingAppResult { inventoryDao.upsert(item) }

    suspend fun decrementInventory(itemId: String, amount: Int): AppResult<Unit> =
        runCatchingAppResult { inventoryDao.decrement(itemId, amount) }

    // ─── Damage Findings ─────────────────────────────────────────────────────
    fun observeDamageFindings(photoId: String): Flow<List<DamageFindingEntity>> =
        damageFindingDao.observeByPhoto(photoId)

    suspend fun insertDamageFinding(finding: DamageFindingEntity): AppResult<Unit> =
        runCatchingAppResult { damageFindingDao.insert(finding) }

    suspend fun insertDamageFindings(findings: List<DamageFindingEntity>): AppResult<Unit> =
        runCatchingAppResult { damageFindingDao.insertAll(findings) }

    // ─── Analysis Results ────────────────────────────────────────────────────
    fun observeAnalysisResults(photoId: String): Flow<List<AnalysisResultEntity>> =
        analysisResultDao.observeByPhoto(photoId)

    suspend fun insertAnalysisResult(result: AnalysisResultEntity): AppResult<Unit> =
        runCatchingAppResult { analysisResultDao.insert(result) }

    suspend fun getLatestAnalysis(photoId: String, layer: String): AnalysisResultEntity? =
        analysisResultDao.getLatestByLayer(photoId, layer)

    // ─── Videos ──────────────────────────────────────────────────────────────
    fun observeVideos(caseId: String): Flow<List<VideoEntity>> =
        videoDao.observeByCase(caseId)

    fun observeVideoFrames(videoId: String): Flow<List<VideoFrameEntity>> =
        videoDao.observeFrames(videoId)

    suspend fun insertVideo(video: VideoEntity): AppResult<Unit> =
        runCatchingAppResult { videoDao.insertVideo(video) }

    suspend fun insertVideoFrames(frames: List<VideoFrameEntity>): AppResult<Unit> =
        runCatchingAppResult { videoDao.insertFrames(frames) }

    suspend fun markVideoAnalyzed(videoId: String): AppResult<Unit> =
        runCatchingAppResult { videoDao.markAnalyzed(videoId) }

    // ─── Motion & Surface ────────────────────────────────────────────────────
    fun observeMotionData(videoId: String): Flow<List<MotionDataEntity>> =
        motionDataDao.observeMotionData(videoId)

    fun observeSurfaceDefects(videoId: String): Flow<List<SurfaceDefectEntity>> =
        motionDataDao.observeDefects(videoId)

    suspend fun insertMotionData(data: List<MotionDataEntity>): AppResult<Unit> =
        runCatchingAppResult { motionDataDao.insertMotionData(data) }

    suspend fun insertSurfaceDefects(defects: List<SurfaceDefectEntity>): AppResult<Unit> =
        runCatchingAppResult { motionDataDao.insertDefects(defects) }
}
