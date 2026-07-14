package com.workshoptech.data.dao

import androidx.room.*
import com.workshoptech.data.entity.CasePhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {

    @Query("SELECT * FROM case_photos WHERE caseId = :caseId ORDER BY capturedAt DESC")
    fun observeByCase(caseId: String): Flow<List<CasePhotoEntity>>

    @Query("SELECT * FROM case_photos WHERE photoId = :id LIMIT 1")
    suspend fun getById(id: String): CasePhotoEntity?

    @Query("SELECT * FROM case_photos WHERE caseId = :caseId AND type = :type ORDER BY capturedAt DESC")
    suspend fun getByType(caseId: String, type: String): List<CasePhotoEntity>

    @Query("SELECT * FROM case_photos WHERE caseId = :caseId AND analyzed = 0")
    suspend fun getUnanalyzed(caseId: String): List<CasePhotoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: CasePhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photos: List<CasePhotoEntity>)

    @Query("UPDATE case_photos SET ocrText = :text, ocrConfidence = :conf, analyzed = 1 WHERE photoId = :id")
    suspend fun updateOcr(id: String, text: String, conf: Float)

    @Query("UPDATE case_photos SET analyzed = 1 WHERE photoId = :id")
    suspend fun markAnalyzed(id: String)

    @Query("DELETE FROM case_photos WHERE photoId = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM case_photos WHERE caseId = :caseId")
    suspend fun deleteByCase(caseId: String)
}
