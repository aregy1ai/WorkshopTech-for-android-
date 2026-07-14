package com.workshoptech.data.dao

import androidx.room.*
import com.workshoptech.data.entity.WorkflowTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkflowTaskDao {

    @Query("SELECT * FROM workflow_tasks WHERE caseId = :caseId ORDER BY priority DESC, plannedStart ASC")
    fun observeByCase(caseId: String): Flow<List<WorkflowTaskEntity>>

    @Query("""
        SELECT * FROM workflow_tasks
        WHERE assignedTo = :techId
          AND status NOT IN ('COMPLETED','CANCELLED')
        ORDER BY priority DESC, plannedStart ASC
    """)
    fun observeByTechnician(techId: String): Flow<List<WorkflowTaskEntity>>

    @Query("SELECT * FROM workflow_tasks WHERE taskId = :id LIMIT 1")
    suspend fun getById(id: String): WorkflowTaskEntity?

    @Query("SELECT COUNT(*) FROM workflow_tasks WHERE caseId = :caseId AND status = 'PENDING'")
    suspend fun countPending(caseId: String): Int

    @Upsert
    suspend fun upsert(task: WorkflowTaskEntity)

    @Upsert
    suspend fun upsertAll(tasks: List<WorkflowTaskEntity>)

    @Query("UPDATE workflow_tasks SET status = :status WHERE taskId = :id")
    suspend fun updateStatus(id: String, status: String)

    @Delete
    suspend fun delete(task: WorkflowTaskEntity)

    @Query("DELETE FROM workflow_tasks WHERE caseId = :caseId")
    suspend fun deleteByCase(caseId: String)
}
