package com.workshoptech.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.workshoptech.data.dao.*
import com.workshoptech.data.entity.*
import com.workshoptech.data.migration.DatabaseMigrations
import kotlinx.coroutines.Dispatchers

/**
 * Room database — v4 (current).
 *
 * Schema version history:
 *  v1 → v2: initial base tables
 *  v2 → v3: video & motion analysis tables
 *  v3 → v4: full column coverage (status, timestamps, analysis fields)
 *
 * Performance:
 *  - WAL journal mode for better concurrent read/write throughput.
 *  - setQueryCoroutineContext ensures all suspend queries run on IO pool.
 *
 * Security:
 *  - exportSchema = true keeps migration history auditable.
 *  - enableMultiInstanceInvalidation disabled (single-process app).
 *  - Filesystem access is restricted by FileProvider + app sandbox.
 */
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
        AnalysisResultEntity::class,
        VideoEntity::class,
        VideoFrameEntity::class,
        MotionDataEntity::class,
        SurfaceDefectEntity::class
    ],
    version      = DatabaseMigrations.CURRENT_VERSION,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun caseDao():         CaseDao
    abstract fun customerDao():     CustomerDao
    abstract fun photoDao():        PhotoDao
    abstract fun inspectionDao():   InspectionDao
    abstract fun workflowTaskDao(): WorkflowTaskDao
    abstract fun technicianDao():   TechnicianDao
    abstract fun inventoryDao():    InventoryDao
    abstract fun damageFindingDao():  DamageFindingDao
    abstract fun analysisResultDao(): AnalysisResultDao
    abstract fun videoDao():          VideoDao
    abstract fun motionDataDao():     MotionDataDao

    companion object {
        private const val DB_NAME = "workshop_tech.db"

        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            )
            // ── Migrations ───────────────────────────────────────────────────
            .addMigrations(*DatabaseMigrations.getAllMigrations())
            // NEVER fallbackToDestructiveMigration — data loss risk
            // ── Performance ──────────────────────────────────────────────────
            // WAL enables concurrent reads while writing
            .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
            // All Room suspend queries default to IO dispatcher
            .setQueryCoroutineContext(Dispatchers.IO)
            // ── Correctness ──────────────────────────────────────────────────
            // Keep single instance — no cross-process access needed
            .build()

        /**
         * Test-only: destroy the singleton so tests get a fresh in-memory DB.
         */
        @Synchronized
        fun destroyInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
