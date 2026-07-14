package com.workshoptech.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Inventory item (paint, materials, spare parts).
 * Added in DB migration v2→v3.
 *
 * Low-stock alert triggers when quantity <= minQuantity.
 */
@Entity(
    tableName = "inventory",
    indices = [
        Index("category"),
        Index("quantity"),
        Index("name")
    ]
)
data class InventoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "itemId")      val itemId: String,
    @ColumnInfo(name = "name")        val name: String,
    @ColumnInfo(name = "nameAr")      val nameAr: String = name,
    @ColumnInfo(name = "category")    val category: String,
    @ColumnInfo(name = "unit")        val unit: String = "قطعة",
    @ColumnInfo(name = "quantity")    val quantity: Int = 0,
    @ColumnInfo(name = "minQuantity") val minQuantity: Int = 0,
    @ColumnInfo(name = "unitPrice")   val unitPrice: Double = 0.0,
    @ColumnInfo(name = "currency")    val currency: String = "LYD",
    @ColumnInfo(name = "supplierId")  val supplierId: String? = null,
    @ColumnInfo(name = "barcode")     val barcode: String? = null,
    @ColumnInfo(name = "updatedAt")   val updatedAt: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean get() = quantity <= minQuantity
    val totalValue: Double  get() = quantity * unitPrice
}

object InventoryCategory {
    const val PAINT       = "PAINT"
    const val MATERIALS   = "MATERIALS"
    const val TOOLS       = "TOOLS"
    const val SPARE_PARTS = "SPARE_PARTS"
    const val CHEMICALS   = "CHEMICALS"
    const val SAFETY      = "SAFETY"
    const val OTHER       = "OTHER"

    fun labelAr(c: String) = when (c) {
        PAINT       -> "دهانات"
        MATERIALS   -> "مواد"
        TOOLS       -> "أدوات"
        SPARE_PARTS -> "قطع غيار"
        CHEMICALS   -> "مواد كيميائية"
        SAFETY      -> "سلامة"
        OTHER       -> "أخرى"
        else        -> c
    }
}
