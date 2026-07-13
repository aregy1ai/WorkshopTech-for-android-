package com.workshoptech.data.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    const val CURRENT_VERSION = 3

    /** v1 → v2: workflow_tasks */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS workflow_tasks (
                    taskId TEXT PRIMARY KEY NOT NULL,
                    caseId TEXT NOT NULL,
                    type TEXT NOT NULL,
                    assignedTo TEXT,
                    status TEXT NOT NULL DEFAULT 'PENDING',
                    priority TEXT NOT NULL DEFAULT 'MEDIUM',
                    plannedStart INTEGER,
                    plannedEnd INTEGER,
                    actualStart INTEGER,
                    actualEnd INTEGER
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_wft_case ON workflow_tasks(caseId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_wft_tech ON workflow_tasks(assignedTo)")
        }
    }

    /** v2 → v3: analysis_results + technicians + inventory + video tables + case column additions */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {

            /* analysis_results */
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS analysis_results (
                    resultId TEXT PRIMARY KEY NOT NULL,
                    photoId TEXT NOT NULL,
                    layer TEXT NOT NULL,
                    version TEXT NOT NULL,
                    isOnline INTEGER NOT NULL DEFAULT 0,
                    rawJson TEXT NOT NULL,
                    confidence REAL NOT NULL,
                    createdAt INTEGER NOT NULL
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_ar_photo ON analysis_results(photoId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_ar_layer ON analysis_results(photoId, layer)")

            /* technicians */
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS technicians (
                    technicianId TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    specialty TEXT NOT NULL,
                    phone TEXT,
                    active INTEGER NOT NULL DEFAULT 1,
                    totalCompleted INTEGER NOT NULL DEFAULT 0,
                    averageRating REAL NOT NULL DEFAULT 0.0
                )
            """)

            /* inventory */
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS inventory (
                    itemId TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    category TEXT NOT NULL,
                    quantity INTEGER NOT NULL DEFAULT 0,
                    minQuantity INTEGER NOT NULL DEFAULT 0,
                    unitPrice REAL NOT NULL DEFAULT 0.0,
                    supplierId TEXT
                )
            """)

            /* videos */
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS videos (
                    videoId TEXT PRIMARY KEY NOT NULL,
                    caseId TEXT NOT NULL,
                    filePath TEXT NOT NULL,
                    thumbnailPath TEXT,
                    durationMs INTEGER NOT NULL DEFAULT 0,
                    frameCount INTEGER NOT NULL DEFAULT 0,
                    width INTEGER NOT NULL DEFAULT 0,
                    height INTEGER NOT NULL DEFAULT 0,
                    fps REAL NOT NULL DEFAULT 0.0,
                    videoType TEXT NOT NULL,
                    capturedAt INTEGER NOT NULL,
                    analyzed INTEGER NOT NULL DEFAULT 0
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_vid_case ON videos(caseId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_vid_at ON videos(capturedAt)")

            /* video_frames */
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS video_frames (
                    frameId TEXT PRIMARY KEY NOT NULL,
                    videoId TEXT NOT NULL,
                    framePath TEXT,
                    timestampMs INTEGER NOT NULL,
                    frameNumber INTEGER NOT NULL,
                    hasDamage INTEGER NOT NULL DEFAULT 0,
                    damageJson TEXT,
                    motionData TEXT
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_vf_video ON video_frames(videoId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_vf_ts ON video_frames(timestampMs)")

            /* motion_data */
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS motion_data (
                    motionId TEXT PRIMARY KEY NOT NULL,
                    videoId TEXT NOT NULL,
                    frameNumber INTEGER NOT NULL,
                    timestampMs INTEGER NOT NULL,
                    pointX REAL NOT NULL,
                    pointY REAL NOT NULL,
                    velocityX REAL NOT NULL DEFAULT 0.0,
                    velocityY REAL NOT NULL DEFAULT 0.0,
                    acceleration REAL NOT NULL DEFAULT 0.0,
                    trackedObject TEXT NOT NULL,
                    confidence REAL NOT NULL DEFAULT 0.0
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_md_video ON motion_data(videoId)")

            /* surface_defects */
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS surface_defects (
                    defectId TEXT PRIMARY KEY NOT NULL,
                    videoId TEXT NOT NULL,
                    frameId TEXT NOT NULL,
                    defectType TEXT NOT NULL,
                    severity TEXT NOT NULL,
                    areaPixels INTEGER NOT NULL DEFAULT 0,
                    perimeterPixels REAL NOT NULL DEFAULT 0.0,
                    centroidX REAL NOT NULL DEFAULT 0.0,
                    centroidY REAL NOT NULL DEFAULT 0.0,
                    boundingLeft REAL NOT NULL DEFAULT 0.0,
                    boundingTop REAL NOT NULL DEFAULT 0.0,
                    boundingRight REAL NOT NULL DEFAULT 0.0,
                    boundingBottom REAL NOT NULL DEFAULT 0.0,
                    reflectionScore REAL NOT NULL DEFAULT 0.0,
                    confidence REAL NOT NULL DEFAULT 0.0
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_sd_video ON surface_defects(videoId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_sd_frame ON surface_defects(frameId)")

            /* cases — new columns */
            runCatching { db.execSQL("ALTER TABLE cases ADD COLUMN colorCode TEXT") }
            runCatching { db.execSQL("ALTER TABLE cases ADD COLUMN colorName TEXT") }
            runCatching { db.execSQL("ALTER TABLE cases ADD COLUMN estimatedCost REAL") }
            runCatching { db.execSQL("ALTER TABLE cases ADD COLUMN actualCost REAL") }
            runCatching { db.execSQL("ALTER TABLE cases ADD COLUMN estimatedHours REAL") }
            runCatching { db.execSQL("ALTER TABLE cases ADD COLUMN actualHours REAL") }
        }
    }

    fun getAllMigrations() = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
}
