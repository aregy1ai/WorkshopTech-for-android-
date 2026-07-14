package com.workshoptech.data.dao

import androidx.room.*
import com.workshoptech.data.entity.TechnicianEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TechnicianDao {

    @Query("SELECT * FROM technicians ORDER BY name ASC")
    fun observeAll(): Flow<List<TechnicianEntity>>

    @Query("SELECT * FROM technicians WHERE active = 1 ORDER BY name ASC")
    fun observeActive(): Flow<List<TechnicianEntity>>

    @Query("SELECT * FROM technicians WHERE specialty = :specialty AND active = 1 ORDER BY averageRating DESC")
    fun observeBySpecialty(specialty: String): Flow<List<TechnicianEntity>>

    @Query("SELECT * FROM technicians WHERE technicianId = :id LIMIT 1")
    suspend fun getById(id: String): TechnicianEntity?

    @Upsert
    suspend fun upsert(technician: TechnicianEntity)

    @Query("UPDATE technicians SET active = :active WHERE technicianId = :id")
    suspend fun setActive(id: String, active: Boolean)

    @Query("""
        UPDATE technicians
        SET totalCompleted = totalCompleted + 1,
            averageRating = (averageRating * totalCompleted + :rating) / (totalCompleted + 1)
        WHERE technicianId = :id
    """)
    suspend fun recordCompletion(id: String, rating: Float)

    @Delete
    suspend fun delete(technician: TechnicianEntity)
}
