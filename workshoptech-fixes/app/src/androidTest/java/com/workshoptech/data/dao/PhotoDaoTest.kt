package com.workshoptech.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.workshoptech.data.AppDatabase
import com.workshoptech.data.entity.CasePhotoEntity
import com.workshoptech.data.entity.PhotoType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented DAO tests for [PhotoDao].
 *
 * Coverage:
 *  - insert + observeByCase
 *  - observeByType filter
 *  - getById found / not found
 *  - getUnanalyzed (analyzed=false)
 *  - markAnalyzed sets analyzed flag
 *  - delete removes photo
 *  - deleteByCase removes all for a case
 *  - OCR fields (ocrText, ocrConfidence) stored correctly
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class PhotoDaoTest {

    private lateinit var db:  AppDatabase
    private lateinit var dao: PhotoDao

    @Before fun setUp() {
        db  = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.photoDao()
    }

    @After fun tearDown() { db.close() }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun photo(
        id:         String,
        caseId:     String   = "case-1",
        type:       String   = PhotoType.GENERAL,
        analyzed:   Boolean  = false,
        ocrText:    String?  = null,
        ocrConf:    Float    = 0f
    ) = CasePhotoEntity(
        photoId       = id,
        caseId        = caseId,
        filePath      = "/photos/$id.jpg",
        type          = type,
        analyzed      = analyzed,
        ocrText       = ocrText,
        ocrConfidence = ocrConf,
        capturedAt    = System.currentTimeMillis()
    )

    // ── observeByCase ─────────────────────────────────────────────────────────

    @Test fun observeByCase_returns_all_photos_for_case() = runTest {
        dao.insert(photo("p1", caseId = "caseA"))
        dao.insert(photo("p2", caseId = "caseA"))
        dao.insert(photo("p3", caseId = "caseB"))

        val listA = dao.observeByCase("caseA").first()
        assertEquals(2, listA.size)
        assertTrue(listA.all { it.caseId == "caseA" })
    }

    @Test fun observeByCase_empty_for_unknown_case() = runTest {
        dao.insert(photo("px", caseId = "caseX"))
        assertTrue(dao.observeByCase("caseY").first().isEmpty())
    }

    // ── observeByType ─────────────────────────────────────────────────────────

    @Test fun observeByType_filters_correctly() = runTest {
        dao.insert(photo("pt1", type = PhotoType.PLATE))
        dao.insert(photo("pt2", type = PhotoType.DAMAGE))
        dao.insert(photo("pt3", type = PhotoType.PLATE))

        val plates = dao.observeByType("case-1", PhotoType.PLATE).first()
        assertEquals(2, plates.size)
        assertTrue(plates.all { it.type == PhotoType.PLATE })
    }

    @Test fun observeByType_empty_when_no_match() = runTest {
        dao.insert(photo("g1", type = PhotoType.GENERAL))
        assertTrue(dao.observeByType("case-1", PhotoType.COLOR).first().isEmpty())
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test fun getById_returns_entity() = runTest {
        dao.insert(photo("gbid"))
        val found = dao.getById("gbid")
        assertNotNull(found)
        assertEquals("gbid", found!!.photoId)
    }

    @Test fun getById_returns_null_when_missing() = runTest {
        assertNull(dao.getById("no-photo"))
    }

    // ── getUnanalyzed ─────────────────────────────────────────────────────────

    @Test fun getUnanalyzed_returns_only_unanalyzed_photos() = runTest {
        dao.insert(photo("ua1", analyzed = false))
        dao.insert(photo("ua2", analyzed = true))
        dao.insert(photo("ua3", analyzed = false))

        val unanalyzed = dao.getUnanalyzed("case-1")
        assertEquals(2, unanalyzed.size)
        assertTrue(unanalyzed.none { it.analyzed })
    }

    @Test fun getUnanalyzed_empty_when_all_analyzed() = runTest {
        dao.insert(photo("allA", analyzed = true))
        assertTrue(dao.getUnanalyzed("case-1").isEmpty())
    }

    // ── markAnalyzed ──────────────────────────────────────────────────────────

    @Test fun markAnalyzed_sets_analyzed_flag_true() = runTest {
        dao.insert(photo("ma1", analyzed = false))
        dao.markAnalyzed("ma1")

        val entity = dao.getById("ma1")
        assertTrue(entity!!.analyzed)
    }

    @Test fun markAnalyzed_does_not_affect_other_photos() = runTest {
        dao.insert(photo("ma2", analyzed = false))
        dao.insert(photo("ma3", analyzed = false))
        dao.markAnalyzed("ma2")

        assertFalse(dao.getById("ma3")!!.analyzed)
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test fun delete_removes_photo() = runTest {
        val p = photo("del1")
        dao.insert(p)
        dao.delete(p)
        assertNull(dao.getById("del1"))
    }

    @Test fun deleteByCase_removes_all_photos_for_case() = runTest {
        dao.insert(photo("dc1", caseId = "caseD"))
        dao.insert(photo("dc2", caseId = "caseD"))
        dao.insert(photo("dc3", caseId = "caseE"))

        dao.deleteByCase("caseD")

        assertNull(dao.getById("dc1"))
        assertNull(dao.getById("dc2"))
        assertNotNull(dao.getById("dc3"))
    }

    // ── OCR fields ────────────────────────────────────────────────────────────

    @Test fun ocrText_and_ocrConfidence_stored_correctly() = runTest {
        dao.insert(photo("ocr1", ocrText = "أ ب 1 2 3", ocrConf = 0.88f))
        val found = dao.getById("ocr1")
        assertEquals("أ ب 1 2 3", found!!.ocrText)
        assertEquals(0.88f, found.ocrConfidence, 0.001f)
    }

    @Test fun ocrText_null_when_not_set() = runTest {
        dao.insert(photo("nocr"))
        assertNull(dao.getById("nocr")!!.ocrText)
    }
}
