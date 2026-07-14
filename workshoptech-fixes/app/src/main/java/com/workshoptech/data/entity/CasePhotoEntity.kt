package com.workshoptech.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Photo attached to a repair case.
 * Types: PLATE (OCR), DAMAGE (AI), GENERAL, INSPECTION_T1..T6, COLOR
 */
@Entity(
    tableName = "case_photos",
    indices = [
        Index("caseId"),
        Index("type"),
        Index("capturedAt")
    ]
)
data class CasePhotoEntity(
    @PrimaryKey
    @ColumnInfo(name = "photoId")       val photoId: String,
    @ColumnInfo(name = "caseId")        val caseId: String,
    @ColumnInfo(name = "filePath")      val filePath: String,
    @ColumnInfo(name = "thumbnailPath") val thumbnailPath: String? = null,
    @ColumnInfo(name = "type")          val type: String = PhotoType.GENERAL,
    @ColumnInfo(name = "ocrText")       val ocrText: String? = null,
    @ColumnInfo(name = "ocrConfidence") val ocrConfidence: Float = 0f,
    @ColumnInfo(name = "analyzed")      val analyzed: Boolean = false,
    @ColumnInfo(name = "capturedAt")    val capturedAt: Long
)

object PhotoType {
    const val PLATE        = "PLATE"
    const val DAMAGE       = "DAMAGE"
    const val GENERAL      = "GENERAL"
    const val INSPECTION   = "INSPECTION"
    const val COLOR        = "COLOR"
    const val BEFORE       = "BEFORE"
    const val AFTER        = "AFTER"
}
