package com.workshoptech.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "video_frames",
    indices = [Index("videoId"), Index("timestampMs")]
)
data class VideoFrameEntity(
    @PrimaryKey val frameId: String,
    val videoId: String,
    val framePath: String?,
    val timestampMs: Long,
    val frameNumber: Int,
    val hasDamage: Boolean = false,
    val damageJson: String? = null,
    val motionData: String? = null
)
