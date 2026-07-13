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

    @Query("SELECT * FROM damage_findings WHERE photoId = :photoId")
    fun observeByPhoto(photoId: String): Flow<List<DamageFindingEntity>>

    @Query("SELECT * FROM damage_findings WHERE photoId = :photoId")
    suspend fun getByPhoto(photoId: String): List<DamageFindingEntity>

    @Query("DELETE FROM damage_findings WHERE photoId = :photoId")
    suspend fun deleteByPhoto(photoId: String)
}
