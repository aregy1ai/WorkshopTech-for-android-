package com.workshoptech.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Workshop customer — referenced by CaseEntity.customerId.
 *
 * Indexes:
 *  - phone: fast lookup when scanning plate / entering phone number
 *  - name: search by name
 */
@Entity(
    tableName = "customers",
    indices = [
        Index("phone"),
        Index("name")
    ]
)
data class CustomerEntity(
    @PrimaryKey
    @ColumnInfo(name = "customerId")  val customerId: String,
    @ColumnInfo(name = "name")        val name: String,
    @ColumnInfo(name = "phone")       val phone: String? = null,
    @ColumnInfo(name = "email")       val email: String? = null,
    @ColumnInfo(name = "country")     val country: String = "LY",
    @ColumnInfo(name = "notes")       val notes: String? = null,
    @ColumnInfo(name = "totalCases")  val totalCases: Int = 0,
    @ColumnInfo(name = "createdAt")   val createdAt: Long,
    @ColumnInfo(name = "updatedAt")   val updatedAt: Long = createdAt
)
