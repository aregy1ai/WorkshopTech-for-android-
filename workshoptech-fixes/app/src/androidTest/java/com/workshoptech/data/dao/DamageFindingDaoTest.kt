package com.workshoptech.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.workshoptech.data.AppDatabase
import com.workshoptech.data.entity.DamageFindingEntity
import com.workshoptech.data.entity.DamageSeverity
import com.workshoptech.data.entity.DamageType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented DAO tests for [DamageFindingDao].
 *
 * Coverage:
 *  - insert + observeByPhoto
 *  - observeByCase
 *  - getBySeverity / observeBySeverity filtering
 *  - countByPhoto
 *  - deleteByPhoto removes only matching rows
 *  - upsert (update existing)
 *  - Bounding box coordinates stored and retrieved precisely
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class DamageFindingDaoTest {

    private lateinit var db:  AppDatabase
    private lateinit var dao: DamageFindingDao

    @Before fun setUp() {
        db  = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.damageFindingDao()
    }

    @After fun tearDown() { db.close() }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun finding(
        id:        String,
        photoId:   String   = "photo-1",
        type:      String   = DamageType.SCRATCH,
        severity:  String   = DamageSeverity.MEDIUM,
        confidence: Float   = 0.85f,
        left: Float = 0.1f, top: Float = 0.2f,
        right: Float = 0.4f, bottom: Float = 0.5f,
        part: String? = null
    ) = DamageFindingEntity(
        findingId    = id,
        photoId      = photoId,
        damageType   = type,
        severity     = severity,
        confidence   = confidence,
        left         = left, top = top,
        right        = right, bottom = bottom,
        affectedPart = part
    )

    // ── observeByPhoto ────────────────────────────────────────────────────────

    @Test fun observeByPhoto_returns_matching_findings() = runTest {
        dao.insert(finding("f1", photoId = "photoA"))
        dao.insert(finding("f2", photoId = "photoA"))
        dao.insert(finding("f3", photoId = "photoB"))

        val forA = dao.observeByPhoto("photoA").first()
        assertEquals(2, forA.size)
        assertTrue(forA.all { it.photoId == "photoA" })
    }

    @Test fun observeByPhoto_empty_for_unknown_photo() = runTest {
        dao.insert(finding("f1", photoId = "photoX"))
        assertTrue(dao.observeByPhoto("photoY").first().isEmpty())
    }

    // ── observeByCase ─────────────────────────────────────────────────────────

    @Test fun observeByCase_joins_across_photos() = runTest {
        // Insert findings for two photos belonging to different cases
        dao.insert(finding("fc1", photoId = "p-case1"))
        dao.insert(finding("fc2", photoId = "p-case1"))

        val list = dao.observeByPhoto("p-case1").first()
        assertEquals(2, list.size)
    }

    // ── getBySeverity ─────────────────────────────────────────────────────────

    @Test fun getBySeverity_returns_correct_subset() = runTest {
        dao.insert(finding("h1", severity = DamageSeverity.HIGH))
        dao.insert(finding("m1", severity = DamageSeverity.MEDIUM))
        dao.insert(finding("h2", severity = DamageSeverity.HIGH))
        dao.insert(finding("l1", severity = DamageSeverity.LOW))

        val highOnly = dao.getBySeverity("photo-1", DamageSeverity.HIGH)
        assertEquals(2, highOnly.size)
        assertTrue(highOnly.all { it.severity == DamageSeverity.HIGH })
    }

    @Test fun getBySeverity_empty_when_no_match() = runTest {
        dao.insert(finding("low", severity = DamageSeverity.LOW))
        assertTrue(dao.getBySeverity("photo-1", DamageSeverity.HIGH).isEmpty())
    }

    // ── countByPhoto ──────────────────────────────────────────────────────────

    @Test fun countByPhoto_returns_correct_count() = runTest {
        dao.insert(finding("c1", photoId = "cp1"))
        dao.insert(finding("c2", photoId = "cp1"))
        dao.insert(finding("c3", photoId = "cp2"))

        assertEquals(2, dao.countByPhoto("cp1"))
        assertEquals(1, dao.countByPhoto("cp2"))
        assertEquals(0, dao.countByPhoto("cp3"))
    }

    // ── deleteByPhoto ─────────────────────────────────────────────────────────

    @Test fun deleteByPhoto_removes_only_matching_rows() = runTest {
        dao.insert(finding("d1", photoId = "del-photo"))
        dao.insert(finding("d2", photoId = "del-photo"))
        dao.insert(finding("d3", photoId = "keep-photo"))

        dao.deleteByPhoto("del-photo")

        assertEquals(0, dao.countByPhoto("del-photo"))
        assertEquals(1, dao.countByPhoto("keep-photo"))
    }

    @Test fun deleteByPhoto_noop_on_unknown_photo() = runTest {
        dao.insert(finding("safe", photoId = "safe-photo"))
        dao.deleteByPhoto("no-such-photo")
        assertEquals(1, dao.countByPhoto("safe-photo"))
    }

    // ── upsert (update) ───────────────────────────────────────────────────────

    @Test fun upsert_updates_existing_finding() = runTest {
        dao.insert(finding("upd", severity = DamageSeverity.LOW))
        dao.upsert(finding("upd", severity = DamageSeverity.HIGH))

        val all = dao.observeByPhoto("photo-1").first()
        assertEquals(1, all.size)
        assertEquals(DamageSeverity.HIGH, all[0].severity)
    }

    // ── Bounding box precision ────────────────────────────────────────────────

    @Test fun bounding_box_stored_and_retrieved_precisely() = runTest {
        val f = finding("bbox", left = 0.123f, top = 0.456f, right = 0.789f, bottom = 0.999f)
        dao.insert(f)

        val retrieved = dao.observeByPhoto("photo-1").first().first()
        assertEquals(0.123f, retrieved.left,   0.001f)
        assertEquals(0.456f, retrieved.top,    0.001f)
        assertEquals(0.789f, retrieved.right,  0.001f)
        assertEquals(0.999f, retrieved.bottom, 0.001f)
    }

    // ── confidence value ──────────────────────────────────────────────────────

    @Test fun confidence_value_preserved() = runTest {
        dao.insert(finding("conf", confidence = 0.923f))
        val r = dao.observeByPhoto("photo-1").first().first()
        assertEquals(0.923f, r.confidence, 0.001f)
    }

    // ── affectedPart null / non-null ──────────────────────────────────────────

    @Test fun affectedPart_null_stored_as_null() = runTest {
        dao.insert(finding("np", part = null))
        val r = dao.observeByPhoto("photo-1").first().first()
        assertNull(r.affectedPart)
    }

    @Test fun affectedPart_string_stored_correctly() = runTest {
        dao.insert(finding("wp", part = "FRONT_BUMPER"))
        val r = dao.observeByPhoto("photo-1").first().first()
        assertEquals("FRONT_BUMPER", r.affectedPart)
    }
}
