package com.workshoptech.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.workshoptech.data.dao.*
import com.workshoptech.data.entity.*
import com.workshoptech.data.migration.DatabaseMigrations

@Database(
    entities = [
        CaseEntity::class,
        CasePhotoEntity::class,
        CustomerEntity::class,
        DamageFindingEntity::class,
        InspectionEntity::class,
        WorkflowTaskEntity::class,
        TechnicianEntity::class,
        InventoryEntity::class,
        AnalysisResultEntity::class
    ],
    version = DatabaseMigrations.CURRENT_VERSION,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun caseDao(): CaseDao
    abstract fun customerDao(): CustomerDao
    abstract fun photoDao(): PhotoDao
    abstract fun inspectionDao(): InspectionDao
    abstract fun workflowTaskDao(): WorkflowTaskDao
    abstract fun technicianDao(): TechnicianDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun damageFindingDao(): DamageFindingDao
    abstract fun analysisResultDao(): AnalysisResultDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "workshop_tech.db"
            )
                .addMigrations(*DatabaseMigrations.getAllMigrations())
                .build()
    }
}
