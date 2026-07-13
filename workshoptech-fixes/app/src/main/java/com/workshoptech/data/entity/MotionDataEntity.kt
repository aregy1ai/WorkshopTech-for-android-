package com.workshoptech.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "motion_data",
    indices = [Index("videoId")]
)
data class MotionDataEntity(
    @PrimaryKey val motionId: String,
    val videoId: String,
    val frameNumber: Int,
    val timestampMs: Long,
    val pointX: Float,
    val pointY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val acceleration: Float,
    val trackedObject: String,
    val confidence: Float
)
