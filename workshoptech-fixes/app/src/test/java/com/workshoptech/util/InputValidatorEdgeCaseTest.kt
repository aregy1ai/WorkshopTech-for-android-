package com.workshoptech.util

import org.junit.Assert.*
import org.junit.Test

/**
 * Additional edge-case tests for InputValidator.
 * Focuses on boundary values and Arabic-specific edge cases.
 */
class InputValidatorEdgeCaseTest {

    // ── Plate boundaries ──────────────────────────────────────────────────────

    @Test fun `plate exactly 2 chars is valid`() {
        assertTrue(InputValidator.validatePlate("AB").isValid)
    }

    @Test fun `plate exactly 20 chars is valid`() {
        assertTrue(InputValidator.validatePlate("A".repeat(20)).isValid)
    }

    @Test fun `plate with Arabic and digits mixed`() {
        assertTrue(InputValidator.validatePlate("أ 1 2").isValid)
    }

    // ── Cost boundaries ───────────────────────────────────────────────────────

    @Test fun `cost exactly 0 is valid`() {
        assertTrue(InputValidator.validateCost("0").isValid)
    }

    @Test fun `cost exactly 1000000 is valid`() {
        assertTrue(InputValidator.validateCost("1000000").isValid)
    }

    @Test fun `cost 1000000_01 is invalid`() {
        assertFalse(InputValidator.validateCost("1000000.01").isValid)
    }

    // ── Year boundaries ───────────────────────────────────────────────────────

    @Test fun `year exactly 1950 is valid`() {
        assertTrue(InputValidator.validateYear("1950").isValid)
    }

    @Test fun `year exactly 2030 is valid`() {
        assertTrue(InputValidator.validateYear("2030").isValid)
    }

    @Test fun `year 1950 and 2030 edge values produce correct string output`() {
        assertEquals("1950", InputValidator.validateYear("1950").valueOrNull())
        assertEquals("2030", InputValidator.validateYear("2030").valueOrNull())
    }

    // ── Name boundaries ───────────────────────────────────────────────────────

    @Test fun `name exactly 2 chars is valid`() {
        assertTrue(InputValidator.validateName("أب").isValid)
    }

    @Test fun `name exactly 100 chars is valid`() {
        assertTrue(InputValidator.validateName("أ".repeat(100)).isValid)
    }

    // ── Notes edge cases ──────────────────────────────────────────────────────

    @Test fun `notes exactly at maxLength is valid`() {
        val notes = "A".repeat(1000)
        assertTrue(InputValidator.validateNotes(notes, maxLength = 1000).isValid)
    }

    @Test fun `notes one over maxLength is invalid`() {
        val notes = "A".repeat(1001)
        assertFalse(InputValidator.validateNotes(notes, maxLength = 1000).isValid)
    }

    // ── sanitizeText edge cases ───────────────────────────────────────────────

    @Test fun `sanitizeText keeps newlines (not a control char)`() {
        val text = "line1\nline2"
        val result = InputValidator.sanitizeText(text)
        assertTrue(result.contains("\n"))
    }

    @Test fun `sanitizeText keeps tabs`() {
        val text = "col1\tcol2"
        val result = InputValidator.sanitizeText(text)
        assertTrue(result.contains("\t"))
    }

    @Test fun `sanitizeText Arabic-only string unchanged`() {
        val arabic = "هذا نص عربي بحت"
        assertEquals(arabic, InputValidator.sanitizeText(arabic))
    }

    @Test fun `sanitizeText strips DEL char (0x7F)`() {
        val text = "hello\u007Fworld"
        assertFalse(InputValidator.sanitizeText(text).contains("\u007F"))
    }

    // ── Phone edge cases ──────────────────────────────────────────────────────

    @Test fun `phone with exactly 7 chars is valid`() {
        assertTrue(InputValidator.validatePhone("1234567").isValid)
    }

    @Test fun `phone with exactly 20 chars is valid`() {
        assertTrue(InputValidator.validatePhone("+".padEnd(20, '1')).isValid)
    }

    @Test fun `phone with parentheses and spaces is valid`() {
        assertTrue(InputValidator.validatePhone("(+218) 91 234 5678").isValid)
    }
}
