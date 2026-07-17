package com.workshoptech.data.migration

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.workshoptech.data.AppDatabase
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Instrumented migration tests using Room's [MigrationTestHelper].
 *
 * Each test creates the database at an earlier version using raw SQL,
 * migrates to the next version, then verifies:
 *  - The migration SQL ran without exception
 *  - New columns are queryable with correct defaults
 *  - Existing data survived the migration intact
 *
 * Test matrix:
 *  - Migration 1→2: workflow_tasks table created with all required columns
 *  - Migration 2→3: analysis_results, technicians, inventory, videos,
 *                   video_frames, motion_data, surface_defects created;
 *                   cases extra columns added safely
 *  - Migration 3→4: column additions (idempotent ALTER TABLE)
 *  - Full chain 1→4: data seeded at v1 reaches v4 intact
 *  - Schema v4 against Room entity definitions (auto-checked by Room)
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private val testDb = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    // ── Migration 1 → 2 ──────────────────────────────────────────────────────

    @Test
    @Throws(IOException::class)
    fun migrate1_to_2_creates_workflow_tasks_table() {
        // Create v1 schema manually
        helper.createDatabase(testDb, 1).apply {
            seedV1Cases(this)
            close()
        }

        // Run migration 1→2
        val db = helper.runMigrationsAndValidate(
            testDb, 2, true,
            DatabaseMigrations.MIGRATION_1_2
        )

        // workflow_tasks table must exist and be queryable
        val cursor = db.query("SELECT * FROM workflow_tasks")
        assertNotNull("workflow_tasks table missing", cursor)
        cursor.close()

        // Verify required column names exist
        val cols = tableColumnNames(db, "workflow_tasks")
        assertTrue("taskId missing",    "taskId"    in cols)
        assertTrue("caseId missing",    "caseId"    in cols)
        assertTrue("type missing",      "type"      in cols)
        assertTrue("status missing",    "status"    in cols)
        assertTrue("priority missing",  "priority"  in cols)
        assertTrue("assignedTo missing","assignedTo" in cols)

        db.close()
    }

    @Test
    fun migrate1_to_2_preserves_existing_case_data() {
        helper.createDatabase(testDb, 1).apply {
            seedV1Cases(this)
            close()
        }

        val db = helper.runMigrationsAndValidate(
            testDb, 2, true,
            DatabaseMigrations.MIGRATION_1_2
        )

        val cursor = db.query("SELECT caseId FROM cases WHERE caseId = 'seed-case-1'")
        assertTrue("Seeded case missing after 1→2 migration", cursor.moveToFirst())
        cursor.close()
        db.close()
    }

    // ── Migration 2 → 3 ──────────────────────────────────────────────────────

    @Test
    fun migrate2_to_3_creates_all_new_tables() {
        helper.createDatabase(testDb, 1).apply { close() }
        helper.runMigrationsAndValidate(testDb, 2, true, DatabaseMigrations.MIGRATION_1_2)

        val db = helper.runMigrationsAndValidate(
            testDb, 3, true,
            DatabaseMigrations.MIGRATION_2_3
        )

        val requiredTables = listOf(
            "analysis_results", "technicians", "inventory",
            "videos", "video_frames", "motion_data", "surface_defects"
        )
        requiredTables.forEach { table ->
            val cursor = db.query("SELECT * FROM $table LIMIT 0")
            assertNotNull("Table '$table' missing after 2→3", cursor)
            cursor.close()
        }
        db.close()
    }

    @Test
    fun migrate2_to_3_analysis_results_has_correct_columns() {
        helper.createDatabase(testDb, 1).apply { close() }
        helper.runMigrationsAndValidate(testDb, 2, true, DatabaseMigrations.MIGRATION_1_2)
        val db = helper.runMigrationsAndValidate(testDb, 3, true, DatabaseMigrations.MIGRATION_2_3)

        val cols = tableColumnNames(db, "analysis_results")
        assertTrue("resultId missing",  "resultId"   in cols)
        assertTrue("photoId missing",   "photoId"    in cols)
        assertTrue("layer missing",     "layer"      in cols)
        assertTrue("rawJson missing",   "rawJson"    in cols)
        assertTrue("confidence missing","confidence" in cols)
        assertTrue("isOnline missing",  "isOnline"   in cols)
        db.close()
    }

    @Test
    fun migrate2_to_3_inventory_has_correct_columns() {
        helper.createDatabase(testDb, 1).apply { close() }
        helper.runMigrationsAndValidate(testDb, 2, true, DatabaseMigrations.MIGRATION_1_2)
        val db = helper.runMigrationsAndValidate(testDb, 3, true, DatabaseMigrations.MIGRATION_2_3)

        val cols = tableColumnNames(db, "inventory")
        assertTrue("itemId missing",      "itemId"      in cols)
        assertTrue("name missing",        "name"        in cols)
        assertTrue("category missing",    "category"    in cols)
        assertTrue("quantity missing",    "quantity"    in cols)
        assertTrue("minQuantity missing", "minQuantity" in cols)
        db.close()
    }

    @Test
    fun migrate2_to_3_videos_has_duration_and_fps() {
        helper.createDatabase(testDb, 1).apply { close() }
        helper.runMigrationsAndValidate(testDb, 2, true, DatabaseMigrations.MIGRATION_1_2)
        val db = helper.runMigrationsAndValidate(testDb, 3, true, DatabaseMigrations.MIGRATION_2_3)

        val cols = tableColumnNames(db, "videos")
        assertTrue("durationMs missing", "durationMs" in cols)
        assertTrue("fps missing",        "fps"        in cols)
        assertTrue("videoType missing",  "videoType"  in cols)
        db.close()
    }

    @Test
    fun migrate2_to_3_cases_extra_columns_added_safely() {
        // Seed a case at v2 (which already has the base columns from MIGRATION_BASE)
        helper.createDatabase(testDb, 1).apply {
            seedV1Cases(this)
            close()
        }
        helper.runMigrationsAndValidate(testDb, 2, true, DatabaseMigrations.MIGRATION_1_2)
        val db = helper.runMigrationsAndValidate(testDb, 3, true, DatabaseMigrations.MIGRATION_2_3)

        // Query cases — the runCatching ALTER TABLE statements should not throw
        val cursor = db.query("SELECT colorCode, colorName FROM cases WHERE caseId = 'seed-case-1'")
        assertTrue("Case missing after 2→3", cursor.moveToFirst())
        cursor.close()
        db.close()
    }

    // ── Migration 3 → 4 ──────────────────────────────────────────────────────

    @Test
    fun migrate3_to_4_adds_title_and_description_to_workflow_tasks() {
        helper.createDatabase(testDb, 1).apply { close() }
        helper.runMigrationsAndValidate(testDb, 2, true, DatabaseMigrations.MIGRATION_1_2)
        helper.runMigrationsAndValidate(testDb, 3, true, DatabaseMigrations.MIGRATION_2_3)

        val db = helper.runMigrationsAndValidate(
            testDb, 4, true,
            DatabaseMigrations.MIGRATION_3_4
        )

        val cols = tableColumnNames(db, "workflow_tasks")
        assertTrue("title missing after 3→4",       "title"       in cols)
        assertTrue("description missing after 3→4", "description" in cols)
        db.close()
    }

    @Test
    fun migrate3_to_4_adds_nameAr_and_barcode_to_inventory() {
        helper.createDatabase(testDb, 1).apply { close() }
        helper.runMigrationsAndValidate(testDb, 2, true, DatabaseMigrations.MIGRATION_1_2)
        helper.runMigrationsAndValidate(testDb, 3, true, DatabaseMigrations.MIGRATION_2_3)
        val db = helper.runMigrationsAndValidate(testDb, 4, true, DatabaseMigrations.MIGRATION_3_4)

        val cols = tableColumnNames(db, "inventory")
        assertTrue("nameAr missing",   "nameAr"   in cols)
        assertTrue("barcode missing",  "barcode"  in cols)
        assertTrue("currency missing", "currency" in cols)
        assertTrue("updatedAt missing","updatedAt" in cols)
        db.close()
    }

    @Test
    fun migrate3_to_4_adds_email_and_hiredAt_to_technicians() {
        helper.createDatabase(testDb, 1).apply { close() }
        helper.runMigrationsAndValidate(testDb, 2, true, DatabaseMigrations.MIGRATION_1_2)
        helper.runMigrationsAndValidate(testDb, 3, true, DatabaseMigrations.MIGRATION_2_3)
        val db = helper.runMigrationsAndValidate(testDb, 4, true, DatabaseMigrations.MIGRATION_3_4)

        val cols = tableColumnNames(db, "technicians")
        assertTrue("email missing",   "email"   in cols)
        assertTrue("hiredAt missing", "hiredAt" in cols)
        db.close()
    }

    @Test
    fun migrate3_to_4_adds_updatedAt_to_customers() {
        helper.createDatabase(testDb, 1).apply { close() }
        helper.runMigrationsAndValidate(testDb, 2, true, DatabaseMigrations.MIGRATION_1_2)
        helper.runMigrationsAndValidate(testDb, 3, true, DatabaseMigrations.MIGRATION_2_3)
        val db = helper.runMigrationsAndValidate(testDb, 4, true, DatabaseMigrations.MIGRATION_3_4)

        val cols = tableColumnNames(db, "customers")
        assertTrue("country missing",   "country"   in cols)
        assertTrue("notes missing",     "notes"     in cols)
        assertTrue("updatedAt missing", "updatedAt" in cols)
        db.close()
    }

    @Test
    fun migrate3_to_4_adds_analyzed_to_case_photos() {
        helper.createDatabase(testDb, 1).apply { close() }
        helper.runMigrationsAndValidate(testDb, 2, true, DatabaseMigrations.MIGRATION_1_2)
        helper.runMigrationsAndValidate(testDb, 3, true, DatabaseMigrations.MIGRATION_2_3)
        val db = helper.runMigrationsAndValidate(testDb, 4, true, DatabaseMigrations.MIGRATION_3_4)

        val cols = tableColumnNames(db, "case_photos")
        assertTrue("analyzed flag missing", "analyzed" in cols)
        db.close()
    }

    @Test
    fun migrate3_to_4_adds_checklist_and_delta_to_inspections() {
        helper.createDatabase(testDb, 1).apply { close() }
        helper.runMigrationsAndValidate(testDb, 2, true, DatabaseMigrations.MIGRATION_1_2)
        helper.runMigrationsAndValidate(testDb, 3, true, DatabaseMigrations.MIGRATION_2_3)
        val db = helper.runMigrationsAndValidate(testDb, 4, true, DatabaseMigrations.MIGRATION_3_4)

        val cols = tableColumnNames(db, "inspections")
        assertTrue("checklistJson missing",  "checklistJson"  in cols)
        assertTrue("defectsJson missing",    "defectsJson"    in cols)
        assertTrue("signaturePath missing",  "signaturePath"  in cols)
        assertTrue("photoIds missing",       "photoIds"       in cols)
        assertTrue("deltaE missing",         "deltaE"         in cols)
        db.close()
    }

    // ── Full chain 1 → 4 ─────────────────────────────────────────────────────

    @Test
    fun full_migration_chain_1_to_4_preserves_data() {
        // Seed data at v1
        helper.createDatabase(testDb, 1).apply {
            seedV1Cases(this)
            seedV1Customers(this)
            seedV1Photos(this)
            close()
        }

        // Run the full chain in sequence
        val db = helper.runMigrationsAndValidate(
            testDb, 4, true,
            DatabaseMigrations.MIGRATION_1_2,
            DatabaseMigrations.MIGRATION_2_3,
            DatabaseMigrations.MIGRATION_3_4
        )

        // Cases intact
        val caseCursor = db.query("SELECT caseId FROM cases WHERE caseId = 'seed-case-1'")
        assertTrue("Case lost in full migration", caseCursor.moveToFirst())
        caseCursor.close()

        // Customers intact
        val custCursor = db.query("SELECT customerId FROM customers WHERE customerId = 'seed-cust-1'")
        assertTrue("Customer lost in full migration", custCursor.moveToFirst())
        custCursor.close()

        // Photos intact
        val photoCursor = db.query("SELECT photoId FROM case_photos WHERE photoId = 'seed-photo-1'")
        assertTrue("Photo lost in full migration", photoCursor.moveToFirst())
        photoCursor.close()

        db.close()
    }

    @Test
    fun full_chain_workflow_task_inserted_at_v2_survives_to_v4() {
        helper.createDatabase(testDb, 1).apply { close() }

        val dbV2 = helper.runMigrationsAndValidate(testDb, 2, true, DatabaseMigrations.MIGRATION_1_2)
        dbV2.execSQL("""
            INSERT INTO workflow_tasks (taskId, caseId, type, status, priority)
            VALUES ('task-1', 'case-1', 'PAINT', 'PENDING', 'HIGH')
        """)
        dbV2.close()

        val db = helper.runMigrationsAndValidate(
            testDb, 4, true,
            DatabaseMigrations.MIGRATION_2_3,
            DatabaseMigrations.MIGRATION_3_4
        )
        val cursor = db.query("SELECT taskId, title FROM workflow_tasks WHERE taskId = 'task-1'")
        assertTrue("Task lost after v2→v4", cursor.moveToFirst())
        // title should default to '' after v3→v4 adds the column
        val titleIdx = cursor.getColumnIndex("title")
        if (titleIdx != -1) assertEquals("", cursor.getString(titleIdx))
        cursor.close()
        db.close()
    }

    @Test
    fun full_chain_inventory_inserted_at_v3_survives_to_v4() {
        helper.createDatabase(testDb, 1).apply { close() }
        helper.runMigrationsAndValidate(testDb, 2, true, DatabaseMigrations.MIGRATION_1_2)

        val dbV3 = helper.runMigrationsAndValidate(testDb, 3, true, DatabaseMigrations.MIGRATION_2_3)
        dbV3.execSQL("""
            INSERT INTO inventory (itemId, name, category, quantity, minQuantity, unitPrice, updatedAt)
            VALUES ('item-1', 'Red Paint', 'PAINT', 10, 2, 25.0, 0)
        """)
        dbV3.close()

        val db = helper.runMigrationsAndValidate(testDb, 4, true, DatabaseMigrations.MIGRATION_3_4)
        val cursor = db.query("SELECT itemId, nameAr, currency FROM inventory WHERE itemId = 'item-1'")
        assertTrue("Inventory item lost after v3→v4", cursor.moveToFirst())

        // nameAr should default to '' and currency to 'LYD'
        val nameArIdx  = cursor.getColumnIndex("nameAr")
        val currencyIdx = cursor.getColumnIndex("currency")
        if (nameArIdx  != -1) assertNotNull(cursor.getString(nameArIdx))
        if (currencyIdx != -1) assertEquals("LYD", cursor.getString(currencyIdx))

        cursor.close()
        db.close()
    }

    // ── Room entity validation (v4 = CURRENT_VERSION) ─────────────────────────

    @Test
    fun schema_v4_matches_room_entity_definitions() {
        // MigrationTestHelper.runMigrationsAndValidate with validateDroppedTables=true
        // automatically compares the migrated schema against Room's expected schema.
        helper.createDatabase(testDb, 1).apply { close() }

        // This throws if any column type, nullability, or default mismatches
        val db = helper.runMigrationsAndValidate(
            testDb,
            DatabaseMigrations.CURRENT_VERSION,
            true,       // validateDroppedTables
            DatabaseMigrations.MIGRATION_1_2,
            DatabaseMigrations.MIGRATION_2_3,
            DatabaseMigrations.MIGRATION_3_4
        )
        db.close()
        // If we reach here, Room's schema hash matches the entity definitions
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun tableColumnNames(db: SupportSQLiteDatabase, table: String): List<String> {
        val cursor = db.query("PRAGMA table_info($table)")
        val names  = mutableListOf<String>()
        val nameIdx = cursor.getColumnIndex("name")
        if (nameIdx == -1) { cursor.close(); return names }
        while (cursor.moveToNext()) names += cursor.getString(nameIdx)
        cursor.close()
        return names
    }

    private fun seedV1Cases(db: SupportSQLiteDatabase) {
        db.execSQL("""
            INSERT INTO cases (caseId, customerId, licensePlate, status, createdAt, updatedAt)
            VALUES ('seed-case-1', 'seed-cust-1', 'LY-1234', 'NEW', 0, 0)
        """)
    }

    private fun seedV1Customers(db: SupportSQLiteDatabase) {
        db.execSQL("""
            INSERT INTO customers (customerId, name, createdAt, updatedAt)
            VALUES ('seed-cust-1', 'محمد علي', 0, 0)
        """)
    }

    private fun seedV1Photos(db: SupportSQLiteDatabase) {
        db.execSQL("""
            INSERT INTO case_photos (photoId, caseId, filePath, type, capturedAt)
            VALUES ('seed-photo-1', 'seed-case-1', '/data/test.jpg', 'GENERAL', 0)
        """)
    }
}
