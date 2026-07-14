package com.workshoptech.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Quality checkpoint inspection (T1–T6).
 *
 * T1 = Vehicle reception
 * T2 = After body work
 * T3 = Handover to paint
 * T4 = Before spray
 * T5 = After paint
 * T6 = Final delivery
 */
@Entity(
    tableName = "inspections",
    indices = [
        Index("caseId"),
        Index(value = ["caseId", "type"], unique = false),
        Index("status"),
        Index("createdAt")
    ]
)
data class InspectionEntity(
    @PrimaryKey
    @ColumnInfo(name = "inspectionId")  val inspectionId: String,
    @ColumnInfo(name = "caseId")        val caseId: String,
    @ColumnInfo(name = "type")          val type: String,            // T1..T6
    @ColumnInfo(name = "status")        val status: String = InspectionStatus.PENDING,
    @ColumnInfo(name = "checklistJson") val checklistJson: String? = null,   // JSON array of check items
    @ColumnInfo(name = "defectsJson")   val defectsJson: String? = null,     // JSON array of found defects
    @ColumnInfo(name = "inspectedBy")   val inspectedBy: String? = null,
    @ColumnInfo(name = "notes")         val notes: String? = null,
    @ColumnInfo(name = "signaturePath") val signaturePath: String? = null,
    @ColumnInfo(name = "photoIds")      val photoIds: String? = null,        // comma-separated
    @ColumnInfo(name = "deltaE")        val deltaE: Float? = null,           // T4/T5 color match
    @ColumnInfo(name = "createdAt")     val createdAt: Long,
    @ColumnInfo(name = "completedAt")   val completedAt: Long? = null
)

object InspectionStatus {
    const val PENDING  = "PENDING"
    const val PASSED   = "PASSED"
    const val FAILED   = "FAILED"
    const val SKIPPED  = "SKIPPED"

    fun labelAr(s: String) = when (s) {
        PENDING -> "معلق"
        PASSED  -> "اجتاز"
        FAILED  -> "لم يجتز"
        SKIPPED -> "متجاوز"
        else    -> s
    }
}

object InspectionType {
    const val T1 = "T1"
    const val T2 = "T2"
    const val T3 = "T3"
    const val T4 = "T4"
    const val T5 = "T5"
    const val T6 = "T6"

    val all = listOf(T1, T2, T3, T4, T5, T6)

    fun labelAr(t: String) = when (t) {
        T1 -> "T1: استلام السيارة"
        T2 -> "T2: بعد السمكرة"
        T3 -> "T3: تسليم للدهان"
        T4 -> "T4: قبل الرش"
        T5 -> "T5: بعد الدهان"
        T6 -> "T6: التسليم النهائي"
        else -> t
    }
}
