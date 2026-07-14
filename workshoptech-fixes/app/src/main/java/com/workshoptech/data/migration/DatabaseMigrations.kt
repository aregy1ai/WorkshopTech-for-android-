package com.workshoptech.data.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database migration chain.
 *
 * v1 (baseline)  — cases, customers, case_photos, damage_findings, inspections
 * v2             — workflow_tasks
 * v3             — analysis_results, technicians, inventory, videos, video_frames,
 *                  motion_data, surface_defects; cases extra columns
 * v4             — workflow_tasks extra columns (title, description)
 *                  inventory extra columns (nameAr, unit, currency, barcode, updatedAt)
 *                  technicians extra columns (email, hiredAt)
 *                  cases baseline creation guard (in case fresh install starts at v1)
 *
 * NEVER call fallbackToDestructiveMigration() — it causes data loss.
 */
object DatabaseMigrations {

    const val CURRENT_VERSION = 4

    // ── v1 baseline — defined as CREATE TABLE IF NOT EXISTS for fresh installs ─
    val MIGRATION_BASE = object : Migration(0, 1) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS cases (
                    caseId TEXT PRIMARY KEY NOT NULL,
                    customerId TEXT NOT NULL,
                    licensePlate TEXT NOT NULL,
                    make TEXT NOT NULL DEFAULT '',
                    model TEXT NOT NULL DEFAULT '',
                    year INTEGER,
                    color TEXT NOT NULL DEFAULT '',
                    colorCode TEXT,
                    colorName TEXT,
                    status TEXT NOT NULL DEFAULT 'NEW',
                    notes TEXT,
                    estimatedCost REAL,
                    actualCost REAL,
                    estimatedHours REAL,
                    actualHours REAL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_case_status ON cases(status)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_case_plate ON cases(licensePlate)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_case_cust ON cases(customerId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_case_created ON cases(createdAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_case_updated ON cases(updatedAt)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS customers (
                    customerId TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    phone TEXT,
                    email TEXT,
                    country TEXT NOT NULL DEFAULT 'LY',
                    notes TEXT,
                    totalCases INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_cust_phone ON customers(phone)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_cust_name ON customers(name)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS case_photos (
                    photoId TEXT PRIMARY KEY NOT NULL,
                    caseId TEXT NOT NULL,
                    filePath TEXT NOT NULL,
                    thumbnailPath TEXT,
                    type TEXT NOT NULL DEFAULT 'GENERAL',
                    ocrText TEXT,
                    ocrConfidence REAL NOT NULL DEFAULT 0.0,
                    analyzed INTEGER NOT NULL DEFAULT 0,
                    capturedAt INTEGER NOT NULL
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_photo_case ON case_photos(caseId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_photo_type ON case_photos(type)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS damage_findings (
                    findingId TEXT PRIMARY KEY NOT NULL,
                    photoId TEXT NOT NULL,
                    damageType TEXT NOT NULL,
                    severity TEXT NOT NULL,
                    confidence REAL NOT NULL DEFAULT 0.0,
                    left REAL NOT NULL,
                    top REAL NOT NULL,
                    right REAL NOT NULL,
                    bottom REAL NOT NULL,
                    affectedPart TEXT
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_df_photo ON damage_findings(photoId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_df_sev ON damage_findings(severity)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS inspections (
                    inspectionId TEXT PRIMARY KEY NOT NULL,
                    caseId TEXT NOT NULL,
                    type TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'PENDING',
                    checklistJson TEXT,
                    defectsJson TEXT,
                    inspectedBy TEXT,
                    notes TEXT,
                    signaturePath TEXT,
                    photoIds TEXT,
                    deltaE REAL,
                    createdAt INTEGER NOT NULL,
                    completedAt INTEGER
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_insp_case ON inspections(caseId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_insp_status ON inspections(status)")
        }
    }

    /** v1 → v2: workflow_tasks (minimal columns — extras added in v3→v4) */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS workflow_tasks (
                    taskId TEXT PRIMARY KEY NOT NULL,
                    caseId TEXT NOT NULL,
                    type TEXT NOT NULL,
                    title TEXT NOT NULL DEFAULT '',
                    description TEXT,
                    assignedTo TEXT,
                    status TEXT NOT NULL DEFAULT 'PENDING',
                    priority TEXT NOT NULL DEFAULT 'MEDIUM',
                    plannedStart INTEGER,
                    plannedEnd INTEGER,
                    actualStart INTEGER,
                    actualEnd INTEGER
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_wft_case  ON workflow_tasks(caseId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_wft_tech  ON workflow_tasks(assignedTo)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_wft_stat  ON workflow_tasks(status)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_wft_pri   ON workflow_tasks(priority)")
        }
    }

    /** v2 → v3: AI tables + video pipeline + cases new columns */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {

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

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS technicians (
                    technicianId TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    specialty TEXT NOT NULL,
                    phone TEXT,
                    email TEXT,
                    active INTEGER NOT NULL DEFAULT 1,
                    totalCompleted INTEGER NOT NULL DEFAULT 0,
                    averageRating REAL NOT NULL DEFAULT 0.0,
                    hiredAt INTEGER
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_tech_active ON technicians(active)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_tech_spec ON technicians(specialty)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS inventory (
                    itemId TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    nameAr TEXT NOT NULL DEFAULT '',
                    category TEXT NOT NULL,
                    unit TEXT NOT NULL DEFAULT 'قطعة',
                    quantity INTEGER NOT NULL DEFAULT 0,
                    minQuantity INTEGER NOT NULL DEFAULT 0,
                    unitPrice REAL NOT NULL DEFAULT 0.0,
                    currency TEXT NOT NULL DEFAULT 'LYD',
                    supplierId TEXT,
                    barcode TEXT,
                    updatedAt INTEGER NOT NULL DEFAULT 0
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_inv_cat ON inventory(category)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_inv_qty ON inventory(quantity)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_inv_name ON inventory(name)")

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

            // cases extra columns (safe: runCatching suppresses AlreadyExistsException)
            runCatching { db.execSQL("ALTER TABLE cases ADD COLUMN colorCode TEXT") }
            runCatching { db.execSQL("ALTER TABLE cases ADD COLUMN colorName TEXT") }
            runCatching { db.execSQL("ALTER TABLE cases ADD COLUMN estimatedCost REAL") }
            runCatching { db.execSQL("ALTER TABLE cases ADD COLUMN actualCost REAL") }
            runCatching { db.execSQL("ALTER TABLE cases ADD COLUMN estimatedHours REAL") }
            runCatching { db.execSQL("ALTER TABLE cases ADD COLUMN actualHours REAL") }
        }
    }

    /**
     * v3 → v4: add any columns that were missing from previous migrations
     * + ensure customers.country and customers.updatedAt exist
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // workflow_tasks: title + description (if created without them in v1→v2)
            runCatching { db.execSQL("ALTER TABLE workflow_tasks ADD COLUMN title TEXT NOT NULL DEFAULT ''") }
            runCatching { db.execSQL("ALTER TABLE workflow_tasks ADD COLUMN description TEXT") }

            // inventory: extra columns from entity
            runCatching { db.execSQL("ALTER TABLE inventory ADD COLUMN nameAr TEXT NOT NULL DEFAULT ''") }
            runCatching { db.execSQL("ALTER TABLE inventory ADD COLUMN unit TEXT NOT NULL DEFAULT 'قطعة'") }
            runCatching { db.execSQL("ALTER TABLE inventory ADD COLUMN currency TEXT NOT NULL DEFAULT 'LYD'") }
            runCatching { db.execSQL("ALTER TABLE inventory ADD COLUMN barcode TEXT") }
            runCatching { db.execSQL("ALTER TABLE inventory ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0") }

            // technicians: email + hiredAt
            runCatching { db.execSQL("ALTER TABLE technicians ADD COLUMN email TEXT") }
            runCatching { db.execSQL("ALTER TABLE technicians ADD COLUMN hiredAt INTEGER") }

            // customers: country + notes + updatedAt
            runCatching { db.execSQL("ALTER TABLE customers ADD COLUMN country TEXT NOT NULL DEFAULT 'LY'") }
            runCatching { db.execSQL("ALTER TABLE customers ADD COLUMN notes TEXT") }
            runCatching { db.execSQL("ALTER TABLE customers ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0") }

            // case_photos: analyzed flag
            runCatching { db.execSQL("ALTER TABLE case_photos ADD COLUMN analyzed INTEGER NOT NULL DEFAULT 0") }

            // inspections: new columns
            runCatching { db.execSQL("ALTER TABLE inspections ADD COLUMN checklistJson TEXT") }
            runCatching { db.execSQL("ALTER TABLE inspections ADD COLUMN defectsJson TEXT") }
            runCatching { db.execSQL("ALTER TABLE inspections ADD COLUMN signaturePath TEXT") }
            runCatching { db.execSQL("ALTER TABLE inspections ADD COLUMN photoIds TEXT") }
            runCatching { db.execSQL("ALTER TABLE inspections ADD COLUMN deltaE REAL") }
        }
    }

    fun getAllMigrations() = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
}
