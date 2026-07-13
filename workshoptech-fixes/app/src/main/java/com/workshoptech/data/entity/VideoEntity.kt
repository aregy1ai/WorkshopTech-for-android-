package com.workshoptech.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "videos",
    indices = [Index("caseId"), Index("capturedAt")]
)
data class VideoEntity(
    @PrimaryKey val videoId: String,
    val caseId: String,
    val filePath: String,
    val thumbnailPath: String?,
    val durationMs: Long,
    val frameCount: Int,
    val width: Int,
    val height: Int,
    val fps: Float,
    val videoType: String,
    val capturedAt: Long,
    val analyzed: Boolean = false
)
