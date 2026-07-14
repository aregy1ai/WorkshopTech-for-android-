package com.workshoptech.data.dao

import androidx.room.*
import com.workshoptech.data.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun observeAll(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE customerId = :id LIMIT 1")
    suspend fun findById(id: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE phone = :phone LIMIT 1")
    suspend fun findByPhone(phone: String): CustomerEntity?

    @Query("""
        SELECT * FROM customers
        WHERE name  LIKE '%' || :q || '%'
           OR phone LIKE '%' || :q || '%'
           OR email LIKE '%' || :q || '%'
        ORDER BY name ASC
        LIMIT 50
    """)
    fun search(q: String): Flow<List<CustomerEntity>>

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(customer: CustomerEntity)

    @Query("UPDATE customers SET totalCases = totalCases + 1 WHERE customerId = :id")
    suspend fun incrementTotalCases(id: String)

    @Delete
    suspend fun delete(customer: CustomerEntity)
}
