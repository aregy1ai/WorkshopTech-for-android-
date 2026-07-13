package com.workshoptech.data.dao

import androidx.room.*
import com.workshoptech.data.entity.VideoEntity
import com.workshoptech.data.entity.VideoFrameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFrames(frames: List<VideoFrameEntity>)

    @Query("SELECT * FROM videos WHERE caseId = :caseId ORDER BY capturedAt DESC")
    fun observeByCase(caseId: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE videoId = :videoId LIMIT 1")
    suspend fun getById(videoId: String): VideoEntity?

    @Query("SELECT * FROM video_frames WHERE videoId = :videoId ORDER BY frameNumber ASC")
    fun observeFrames(videoId: String): Flow<List<VideoFrameEntity>>

    @Query("SELECT * FROM video_frames WHERE videoId = :videoId AND hasDamage = 1 ORDER BY frameNumber ASC")
    suspend fun getDamageFrames(videoId: String): List<VideoFrameEntity>

    @Query("UPDATE videos SET analyzed = 1 WHERE videoId = :videoId")
    suspend fun markAnalyzed(videoId: String)

    @Query("DELETE FROM video_frames WHERE videoId = :videoId")
    suspend fun deleteFrames(videoId: String)

    @Delete
    suspend fun deleteVideo(video: VideoEntity)
}
