package com.workshoptech.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single AI-detected damage region on a photo.
 * Bounding box coordinates are normalised [0.0, 1.0].
 */
@Entity(
    tableName = "damage_findings",
    indices = [
        Index("photoId"),
        Index("severity")
    ]
)
data class DamageFindingEntity(
    @PrimaryKey
    @ColumnInfo(name = "findingId")    val findingId: String,
    @ColumnInfo(name = "photoId")      val photoId: String,
    @ColumnInfo(name = "damageType")   val damageType: String,
    @ColumnInfo(name = "severity")     val severity: String,
    @ColumnInfo(name = "confidence")   val confidence: Float = 0f,
    @ColumnInfo(name = "left")         val left: Float = 0f,
    @ColumnInfo(name = "top")          val top: Float = 0f,
    @ColumnInfo(name = "right")        val right: Float = 0f,
    @ColumnInfo(name = "bottom")       val bottom: Float = 0f,
    @ColumnInfo(name = "affectedPart") val affectedPart: String? = null
)

object DamageType {
    const val SCRATCH    = "SCRATCH"
    const val DENT       = "DENT"
    const val CRACK      = "CRACK"
    const val PAINT_PEEL = "PAINT_PEEL"
    const val RUST       = "RUST"
    const val GLASS      = "GLASS"
    const val STRUCTURAL = "STRUCTURAL"

    fun labelAr(type: String) = when (type) {
        SCRATCH    -> "خدش"
        DENT       -> "بعجة"
        CRACK      -> "تشقق"
        PAINT_PEEL -> "تقشر دهان"
        RUST       -> "صدأ"
        GLASS      -> "زجاج"
        STRUCTURAL -> "هيكلي"
        else       -> type
    }
}

object DamageSeverity {
    const val LOW    = "LOW"
    const val MEDIUM = "MEDIUM"
    const val HIGH   = "HIGH"

    fun labelAr(s: String) = when (s) {
        LOW    -> "خفيف"
        MEDIUM -> "متوسط"
        HIGH   -> "شديد"
        else   -> s
    }
}
