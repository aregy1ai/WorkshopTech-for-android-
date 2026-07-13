package com.workshoptech.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "surface_defects",
    indices = [Index("videoId"), Index("frameId")]
)
data class SurfaceDefectEntity(
    @PrimaryKey val defectId: String,
    val videoId: String,
    val frameId: String,
    val defectType: String,
    val severity: String,
    val areaPixels: Int,
    val perimeterPixels: Float,
    val centroidX: Float,
    val centroidY: Float,
    val boundingLeft: Float,
    val boundingTop: Float,
    val boundingRight: Float,
    val boundingBottom: Float,
    val reflectionScore: Float,
    val confidence: Float
)
