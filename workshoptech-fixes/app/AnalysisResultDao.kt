package com.workshoptech.data.dao

import androidx.room.*
import com.workshoptech.data.entity.AnalysisResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalysisResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: AnalysisResultEntity)

    @Query("SELECT * FROM analysis_results WHERE photoId = :photoId ORDER BY createdAt DESC")
    fun observeByPhoto(photoId: String): Flow<List<AnalysisResultEntity>>

    @Query("SELECT * FROM analysis_results WHERE photoId = :photoId AND layer = :layer ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestByLayer(photoId: String, layer: String): AnalysisResultEntity?

    @Query("DELETE FROM analysis_results WHERE photoId = :photoId")
    suspend fun deleteByPhoto(photoId: String)
}
