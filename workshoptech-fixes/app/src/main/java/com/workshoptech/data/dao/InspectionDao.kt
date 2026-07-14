package com.workshoptech.data.dao

import androidx.room.*
import com.workshoptech.data.entity.InspectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InspectionDao {

    @Query("""
        SELECT * FROM inspections
        WHERE caseId = :caseId
        ORDER BY
            CASE type WHEN 'T1' THEN 1 WHEN 'T2' THEN 2 WHEN 'T3' THEN 3
                      WHEN 'T4' THEN 4 WHEN 'T5' THEN 5 WHEN 'T6' THEN 6
                      ELSE 7 END
    """)
    fun observeByCase(caseId: String): Flow<List<InspectionEntity>>

    @Query("""
        SELECT * FROM inspections
        WHERE caseId = :caseId AND type = :type
        ORDER BY createdAt DESC
        LIMIT 1
    """)
    suspend fun getLatestByType(caseId: String, type: String): InspectionEntity?

    @Query("SELECT * FROM inspections WHERE inspectionId = :id LIMIT 1")
    suspend fun getById(id: String): InspectionEntity?

    @Query("SELECT * FROM inspections WHERE status = 'PENDING' ORDER BY createdAt ASC")
    fun observePending(): Flow<List<InspectionEntity>>

    @Upsert
    suspend fun upsert(inspection: InspectionEntity)

    @Query("UPDATE inspections SET status = :status, completedAt = :ts WHERE inspectionId = :id")
    suspend fun updateStatus(id: String, status: String, ts: Long)

    @Delete
    suspend fun delete(inspection: InspectionEntity)

    @Query("DELETE FROM inspections WHERE caseId = :caseId")
    suspend fun deleteByCase(caseId: String)
}
