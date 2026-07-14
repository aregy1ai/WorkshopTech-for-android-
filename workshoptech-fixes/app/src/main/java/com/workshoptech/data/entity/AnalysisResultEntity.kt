package com.workshoptech.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * AI analysis result stored per photo per analysis layer.
 *
 * Layers: QUALITY | OCR | DAMAGE | COST
 * version: model/algorithm version for result invalidation.
 */
@Entity(
    tableName = "analysis_results",
    indices = [
        Index("photoId"),
        Index(value = ["photoId", "layer"])
    ]
)
data class AnalysisResultEntity(
    @PrimaryKey
    @ColumnInfo(name = "resultId")    val resultId: String,
    @ColumnInfo(name = "photoId")     val photoId: String,
    @ColumnInfo(name = "layer")       val layer: String,       // QUALITY|OCR|DAMAGE|COST
    @ColumnInfo(name = "version")     val version: String,
    @ColumnInfo(name = "isOnline")    val isOnline: Boolean = false,
    @ColumnInfo(name = "rawJson")     val rawJson: String,
    @ColumnInfo(name = "confidence")  val confidence: Float,
    @ColumnInfo(name = "createdAt")   val createdAt: Long
)

object AnalysisLayer {
    const val QUALITY = "QUALITY"
    const val OCR     = "OCR"
    const val DAMAGE  = "DAMAGE"
    const val COST    = "COST"
}
