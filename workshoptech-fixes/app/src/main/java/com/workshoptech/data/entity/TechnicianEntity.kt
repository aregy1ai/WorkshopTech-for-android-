package com.workshoptech.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Workshop technician / employee.
 * Added in DB migration v2→v3.
 */
@Entity(
    tableName = "technicians",
    indices = [
        Index("active"),
        Index("specialty")
    ]
)
data class TechnicianEntity(
    @PrimaryKey
    @ColumnInfo(name = "technicianId")    val technicianId: String,
    @ColumnInfo(name = "name")            val name: String,
    @ColumnInfo(name = "specialty")       val specialty: String,
    @ColumnInfo(name = "phone")           val phone: String? = null,
    @ColumnInfo(name = "email")           val email: String? = null,
    @ColumnInfo(name = "active")          val active: Boolean = true,
    @ColumnInfo(name = "totalCompleted")  val totalCompleted: Int = 0,
    @ColumnInfo(name = "averageRating")   val averageRating: Float = 0f,
    @ColumnInfo(name = "hiredAt")         val hiredAt: Long? = null
)

object Specialty {
    const val BODY_WORK     = "BODY_WORK"
    const val PAINTING      = "PAINTING"
    const val POLISHING     = "POLISHING"
    const val MECHANICAL    = "MECHANICAL"
    const val QUALITY_CONTROL = "QUALITY_CONTROL"
    const val MANAGER       = "MANAGER"

    fun labelAr(s: String) = when (s) {
        BODY_WORK       -> "سمكرة"
        PAINTING        -> "دهان"
        POLISHING       -> "تلميع"
        MECHANICAL      -> "ميكانيكا"
        QUALITY_CONTROL -> "مراقبة الجودة"
        MANAGER         -> "مدير"
        else            -> s
    }
}
