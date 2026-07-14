package com.workshoptech.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single work task within a repair case workflow.
 * Added in DB migration v1→v2.
 */
@Entity(
    tableName = "workflow_tasks",
    indices = [
        Index("caseId"),
        Index("assignedTo"),
        Index("status"),
        Index("priority")
    ]
)
data class WorkflowTaskEntity(
    @PrimaryKey
    @ColumnInfo(name = "taskId")       val taskId: String,
    @ColumnInfo(name = "caseId")       val caseId: String,
    @ColumnInfo(name = "type")         val type: String,
    @ColumnInfo(name = "title")        val title: String = "",
    @ColumnInfo(name = "description")  val description: String? = null,
    @ColumnInfo(name = "assignedTo")   val assignedTo: String? = null,     // technicianId
    @ColumnInfo(name = "status")       val status: String = TaskStatus.PENDING,
    @ColumnInfo(name = "priority")     val priority: String = TaskPriority.MEDIUM,
    @ColumnInfo(name = "plannedStart") val plannedStart: Long? = null,
    @ColumnInfo(name = "plannedEnd")   val plannedEnd: Long? = null,
    @ColumnInfo(name = "actualStart")  val actualStart: Long? = null,
    @ColumnInfo(name = "actualEnd")    val actualEnd: Long? = null
)

object TaskStatus {
    const val PENDING     = "PENDING"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val COMPLETED   = "COMPLETED"
    const val BLOCKED     = "BLOCKED"
    const val CANCELLED   = "CANCELLED"
}

object TaskPriority {
    const val LOW    = "LOW"
    const val MEDIUM = "MEDIUM"
    const val HIGH   = "HIGH"
    const val URGENT = "URGENT"
}

object TaskType {
    const val BODY_WORK    = "BODY_WORK"
    const val PAINT        = "PAINT"
    const val POLISH       = "POLISH"
    const val INSPECTION   = "INSPECTION"
    const val PARTS        = "PARTS"
    const val DELIVERY     = "DELIVERY"
    const val OTHER        = "OTHER"
}
