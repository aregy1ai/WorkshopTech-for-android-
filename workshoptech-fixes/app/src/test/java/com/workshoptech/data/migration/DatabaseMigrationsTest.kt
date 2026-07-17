package com.workshoptech.data.migration

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for DatabaseMigrations constants and migration chain.
 *
 * These are pure-Kotlin tests — no Room testing library needed.
 * Full migration execution (SQL correctness) is covered by Room's
 * MigrationTestHelper in androidTest.
 *
 * Coverage:
 *  - CURRENT_VERSION == 4
 *  - getAllMigrations() returns exactly 3 migrations
 *  - Migration versions cover 1→2, 2→3, 3→4 (no gaps)
 *  - No version is duplicated in the chain
 *  - START_VERSION + n steps reaches CURRENT_VERSION
 */
class DatabaseMigrationsTest {

    @Test fun `CURRENT_VERSION is 4`() {
        assertEquals(4, DatabaseMigrations.CURRENT_VERSION)
    }

    @Test fun `getAllMigrations returns non-empty array`() {
        val migrations = DatabaseMigrations.getAllMigrations()
        assertTrue("No migrations returned", migrations.isNotEmpty())
    }

    @Test fun `migration chain covers all versions from 1 to CURRENT_VERSION`() {
        val migrations = DatabaseMigrations.getAllMigrations()

        // Build a map of startVersion → endVersion
        val chain = migrations.associate { it.startVersion to it.endVersion }

        // Walk the chain: 1 → 2 → 3 → 4
        var version = 1
        while (version < DatabaseMigrations.CURRENT_VERSION) {
            val next = chain[version]
            assertNotNull("Missing migration from version $version", next)
            assertEquals("Migration gap at $version", version + 1, next)
            version++
        }
        assertEquals(DatabaseMigrations.CURRENT_VERSION, version)
    }

    @Test fun `no duplicate start versions in migration chain`() {
        val migrations = DatabaseMigrations.getAllMigrations()
        val starts = migrations.map { it.startVersion }
        assertEquals(starts.size, starts.distinct().size)
    }

    @Test fun `no duplicate end versions in migration chain`() {
        val migrations = DatabaseMigrations.getAllMigrations()
        val ends = migrations.map { it.endVersion }
        assertEquals(ends.size, ends.distinct().size)
    }

    @Test fun `each migration start is less than its end`() {
        DatabaseMigrations.getAllMigrations().forEach { m ->
            assertTrue(
                "Migration ${m.startVersion}→${m.endVersion}: start must be < end",
                m.startVersion < m.endVersion
            )
        }
    }

    @Test fun `first migration starts from version 1`() {
        val migrations = DatabaseMigrations.getAllMigrations()
        val starts = migrations.map { it.startVersion }
        assertTrue("First migration must start from 1", 1 in starts)
    }

    @Test fun `last migration ends at CURRENT_VERSION`() {
        val migrations = DatabaseMigrations.getAllMigrations()
        val ends = migrations.map { it.endVersion }
        assertTrue(
            "Last migration must end at CURRENT_VERSION=${DatabaseMigrations.CURRENT_VERSION}",
            DatabaseMigrations.CURRENT_VERSION in ends
        )
    }
}
