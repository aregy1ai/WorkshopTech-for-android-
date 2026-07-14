package com.workshoptech.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Main repair case — one row per vehicle repair job.
 *
 * Indexes:
 *  - status: dashboard filter (IN_PROGRESS / READY_FOR_DELIVERY)
 *  - licensePlate: quick lookup and search
 *  - customerId: customer history queries
 *  - createdAt: date-range sorting
 */
@Entity(
    tableName = "cases",
    indices = [
        Index("status"),
        Index("licensePlate"),
        Index("customerId"),
        Index("createdAt"),
        Index("updatedAt")
    ]
)
data class CaseEntity(
    @PrimaryKey
    @ColumnInfo(name = "caseId")       val caseId: String,
    @ColumnInfo(name = "customerId")   val customerId: String,
    @ColumnInfo(name = "licensePlate") val licensePlate: String,
    @ColumnInfo(name = "make")         val make: String = "",
    @ColumnInfo(name = "model")        val model: String = "",
    @ColumnInfo(name = "year")         val year: Int? = null,
    @ColumnInfo(name = "color")        val color: String = "",
    @ColumnInfo(name = "colorCode")    val colorCode: String? = null,
    @ColumnInfo(name = "colorName")    val colorName: String? = null,
    @ColumnInfo(name = "status")       val status: String = CaseStatus.NEW,
    @ColumnInfo(name = "notes")        val notes: String? = null,
    @ColumnInfo(name = "estimatedCost") val estimatedCost: Double? = null,
    @ColumnInfo(name = "actualCost")    val actualCost: Double? = null,
    @ColumnInfo(name = "estimatedHours") val estimatedHours: Double? = null,
    @ColumnInfo(name = "actualHours")    val actualHours: Double? = null,
    @ColumnInfo(name = "createdAt")    val createdAt: Long,
    @ColumnInfo(name = "updatedAt")    val updatedAt: Long
)

object CaseStatus {
    const val NEW                = "NEW"
    const val APPROVED           = "APPROVED"
    const val IN_PROGRESS        = "IN_PROGRESS"
    const val READY_FOR_DELIVERY = "READY_FOR_DELIVERY"
    const val DELIVERED          = "DELIVERED"
    const val ON_HOLD            = "ON_HOLD"
    const val CANCELLED          = "CANCELLED"

    val all = listOf(NEW, APPROVED, IN_PROGRESS, READY_FOR_DELIVERY, DELIVERED, ON_HOLD, CANCELLED)

    fun labelAr(status: String) = when (status) {
        NEW                -> "جديد"
        APPROVED           -> "معتمد"
        IN_PROGRESS        -> "قيد التنفيذ"
        READY_FOR_DELIVERY -> "جاهز للتسليم"
        DELIVERED          -> "تم التسليم"
        ON_HOLD            -> "معلق"
        CANCELLED          -> "ملغي"
        else               -> status
    }

    fun isActive(status: String) = status in setOf(NEW, APPROVED, IN_PROGRESS, READY_FOR_DELIVERY, ON_HOLD)
}
