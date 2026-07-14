package com.workshoptech.data.dao

import androidx.room.*
import com.workshoptech.data.entity.InventoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {

    @Query("SELECT * FROM inventory ORDER BY category ASC, name ASC")
    fun observeAll(): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM inventory WHERE category = :category ORDER BY name ASC")
    fun observeByCategory(category: String): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM inventory WHERE quantity <= minQuantity ORDER BY quantity ASC")
    fun observeLowStock(): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM inventory WHERE itemId = :id LIMIT 1")
    suspend fun getById(id: String): InventoryEntity?

    @Query("""
        SELECT * FROM inventory
        WHERE name   LIKE '%' || :q || '%'
           OR nameAr LIKE '%' || :q || '%'
           OR barcode = :q
        ORDER BY name ASC
        LIMIT 30
    """)
    suspend fun search(q: String): List<InventoryEntity>

    @Upsert
    suspend fun upsert(item: InventoryEntity)

    @Upsert
    suspend fun upsertAll(items: List<InventoryEntity>)

    @Query("UPDATE inventory SET quantity = quantity - :amount, updatedAt = :ts WHERE itemId = :id AND quantity >= :amount")
    suspend fun decrement(id: String, amount: Int, ts: Long = System.currentTimeMillis())

    @Query("UPDATE inventory SET quantity = quantity + :amount, updatedAt = :ts WHERE itemId = :id")
    suspend fun increment(id: String, amount: Int, ts: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(item: InventoryEntity)
}
