package com.workshoptech.data.dao

import androidx.room.*
import com.workshoptech.data.entity.CaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CaseDao {

    @Query("SELECT * FROM cases ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<CaseEntity>>

    @Query("SELECT * FROM cases WHERE caseId = :id")
    fun observeById(id: String): Flow<CaseEntity?>

    @Query("SELECT * FROM cases WHERE caseId = :id LIMIT 1")
    suspend fun getById(id: String): CaseEntity?

    @Query("SELECT * FROM cases WHERE status = :status ORDER BY updatedAt DESC")
    fun observeByStatus(status: String): Flow<List<CaseEntity>>

    @Query("""
        SELECT * FROM cases
        WHERE licensePlate LIKE '%' || :q || '%'
           OR make        LIKE '%' || :q || '%'
           OR model       LIKE '%' || :q || '%'
           OR colorCode   LIKE '%' || :q || '%'
        ORDER BY updatedAt DESC
        LIMIT 100
    """)
    fun search(q: String): Flow<List<CaseEntity>>

    @Query("SELECT COUNT(*) FROM cases WHERE status = :status")
    suspend fun countByStatus(status: String): Int

    @Query("SELECT * FROM cases ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<CaseEntity>>

    @Upsert
    suspend fun upsert(case: CaseEntity)

    @Upsert
    suspend fun upsertAll(cases: List<CaseEntity>)

    @Query("UPDATE cases SET status = :status, updatedAt = :ts WHERE caseId = :id")
    suspend fun updateStatus(id: String, status: String, ts: Long)

    @Query("UPDATE cases SET colorCode = :code, colorName = :name, updatedAt = :ts WHERE caseId = :id")
    suspend fun updateColor(id: String, code: String, name: String, ts: Long)

    @Query("UPDATE cases SET actualCost = :cost, updatedAt = :ts WHERE caseId = :id")
    suspend fun updateActualCost(id: String, cost: Double, ts: Long)

    @Delete
    suspend fun delete(case: CaseEntity)

    @Query("DELETE FROM cases WHERE caseId = :id")
    suspend fun deleteById(id: String)
}
