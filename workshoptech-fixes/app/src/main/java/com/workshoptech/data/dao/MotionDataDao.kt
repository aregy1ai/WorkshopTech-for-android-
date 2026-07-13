package com.workshoptech.data.dao

import androidx.room.*
import com.workshoptech.data.entity.MotionDataEntity
import com.workshoptech.data.entity.SurfaceDefectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MotionDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMotionData(data: List<MotionDataEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDefects(defects: List<SurfaceDefectEntity>)

    @Query("SELECT * FROM motion_data WHERE videoId = :videoId ORDER BY frameNumber ASC")
    fun observeMotionData(videoId: String): Flow<List<MotionDataEntity>>

    @Query("SELECT * FROM surface_defects WHERE videoId = :videoId ORDER BY confidence DESC")
    fun observeDefects(videoId: String): Flow<List<SurfaceDefectEntity>>

    @Query("SELECT * FROM surface_defects WHERE videoId = :videoId AND severity = 'HIGH'")
    suspend fun getHighSeverityDefects(videoId: String): List<SurfaceDefectEntity>

    @Query("SELECT COUNT(*) FROM surface_defects WHERE videoId = :videoId")
    suspend fun countDefects(videoId: String): Int

    @Query("DELETE FROM motion_data WHERE videoId = :videoId")
    suspend fun deleteMotionData(videoId: String)

    @Query("DELETE FROM surface_defects WHERE videoId = :videoId")
    suspend fun deleteDefects(videoId: String)
}
