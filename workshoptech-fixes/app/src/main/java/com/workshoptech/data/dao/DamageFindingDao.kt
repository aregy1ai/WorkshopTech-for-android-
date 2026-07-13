package com.workshoptech.data.dao

import androidx.room.*
import com.workshoptech.data.entity.DamageFindingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DamageFindingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(finding: DamageFindingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(findings: List<DamageFindingEntity>)

    @Query("SELECT * FROM damage_findings WHERE photoId = :photoId ORDER BY confidence DESC")
    fun observeByPhoto(photoId: String): Flow<List<DamageFindingEntity>>

    @Query("SELECT * FROM damage_findings WHERE photoId = :photoId")
    suspend fun getByPhoto(photoId: String): List<DamageFindingEntity>

    @Query("SELECT * FROM damage_findings WHERE photoId = :photoId AND damageType = :type")
    suspend fun getByType(photoId: String, type: String): List<DamageFindingEntity>

    @Query("SELECT COUNT(*) FROM damage_findings WHERE photoId = :photoId")
    suspend fun countByPhoto(photoId: String): Int

    @Query("DELETE FROM damage_findings WHERE photoId = :photoId")
    suspend fun deleteByPhoto(photoId: String)

    @Delete
    suspend fun delete(finding: DamageFindingEntity)
}
