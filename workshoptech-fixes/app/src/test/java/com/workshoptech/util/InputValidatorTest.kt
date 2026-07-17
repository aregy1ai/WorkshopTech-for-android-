package com.workshoptech.util

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for InputValidator — pure Kotlin, no Android dependencies.
 *
 * Coverage:
 *  - Plate number validation (valid, blank, short, long, illegal chars)
 *  - Phone validation (optional field, bad chars, lengths)
 *  - Name validation (blank, short, long, control chars)
 *  - Notes validation (length cap)
 *  - Year validation (range, non-numeric)
 *  - Cost validation (negative, too large, non-numeric)
 *  - sanitizeText (control char stripping, hard cap)
 *  - isPathSafe (traversal guard)
 *  - ValidationResult helpers
 */
class InputValidatorTest {

    // ── Plate ────────────────────────────────────────────────────────────────

    @Test fun `validatePlate - valid Arabic plate`() {
        val r = InputValidator.validatePlate("أ ب ت 123")
        assertTrue(r.isValid)
        assertEquals("أ ب ت 123", r.valueOrNull())
    }

    @Test fun `validatePlate - valid Latin plate`() {
        val r = InputValidator.validatePlate("abc-123")
        assertTrue(r.isValid)
        assertEquals("ABC-123", r.valueOrNull())   // must be uppercased
    }

    @Test fun `validatePlate - blank returns error`() {
        val r = InputValidator.validatePlate("   ")
        assertFalse(r.isValid)
        assertNotNull(r.errorMessage)
    }

    @Test fun `validatePlate - single char is too short`() {
        val r = InputValidator.validatePlate("A")
        assertFalse(r.isValid)
    }

    @Test fun `validatePlate - 21 chars is too long`() {
        val r = InputValidator.validatePlate("A".repeat(21))
        assertFalse(r.isValid)
    }

    @Test fun `validatePlate - special chars rejected`() {
        val r = InputValidator.validatePlate("ABC@#!")
        assertFalse(r.isValid)
    }

    // ── Phone ────────────────────────────────────────────────────────────────

    @Test fun `validatePhone - blank is valid (optional)`() {
        val r = InputValidator.validatePhone("")
        assertTrue(r.isValid)
    }

    @Test fun `validatePhone - valid international format`() {
        val r = InputValidator.validatePhone("+218-91-234-5678")
        assertTrue(r.isValid)
    }

    @Test fun `validatePhone - letters rejected`() {
        val r = InputValidator.validatePhone("abc-phone")
        assertFalse(r.isValid)
    }

    @Test fun `validatePhone - too short (5 chars) rejected`() {
        val r = InputValidator.validatePhone("12345")
        assertFalse(r.isValid)
    }

    // ── Name ─────────────────────────────────────────────────────────────────

    @Test fun `validateName - valid Arabic name`() {
        val r = InputValidator.validateName("محمد علي")
        assertTrue(r.isValid)
        assertEquals("محمد علي", r.valueOrNull())
    }

    @Test fun `validateName - blank returns error with field label`() {
        val r = InputValidator.validateName("", "اسم العميل")
        assertFalse(r.isValid)
        assertTrue(r.errorMessage!!.contains("اسم العميل"))
    }

    @Test fun `validateName - single char is too short`() {
        assertFalse(InputValidator.validateName("أ").isValid)
    }

    @Test fun `validateName - 101 chars is too long`() {
        assertFalse(InputValidator.validateName("أ".repeat(101)).isValid)
    }

    @Test fun `validateName - control chars stripped before length check`() {
        // "\u0007" is a bell char — sanitizeText should strip it
        val r = InputValidator.validateName("Ali\u0007Ahmed")
        assertTrue(r.isValid)
        assertFalse(r.valueOrNull()!!.contains("\u0007"))
    }

    // ── Notes ────────────────────────────────────────────────────────────────

    @Test fun `validateNotes - within default 1000 limit is valid`() {
        val r = InputValidator.validateNotes("ملاحظة".repeat(100))
        assertTrue(r.isValid)
    }

    @Test fun `validateNotes - exceeding custom limit returns error`() {
        val r = InputValidator.validateNotes("A".repeat(51), maxLength = 50)
        assertFalse(r.isValid)
    }

    @Test fun `validateNotes - empty notes is valid`() {
        assertTrue(InputValidator.validateNotes("").isValid)
    }

    // ── Year ─────────────────────────────────────────────────────────────────

    @Test fun `validateYear - blank is valid (optional)`() {
        assertTrue(InputValidator.validateYear("").isValid)
    }

    @Test fun `validateYear - year 2023 is valid`() {
        val r = InputValidator.validateYear("2023")
        assertTrue(r.isValid)
        assertEquals("2023", r.valueOrNull())
    }

    @Test fun `validateYear - year 1949 is out of range`() {
        assertFalse(InputValidator.validateYear("1949").isValid)
    }

    @Test fun `validateYear - year 2031 is out of range`() {
        assertFalse(InputValidator.validateYear("2031").isValid)
    }

    @Test fun `validateYear - non-numeric returns error`() {
        assertFalse(InputValidator.validateYear("abc").isValid)
    }

    // ── Cost ─────────────────────────────────────────────────────────────────

    @Test fun `validateCost - blank is valid (optional)`() {
        assertTrue(InputValidator.validateCost("").isValid)
    }

    @Test fun `validateCost - valid decimal cost`() {
        val r = InputValidator.validateCost("1500.50")
        assertTrue(r.isValid)
    }

    @Test fun `validateCost - negative cost rejected`() {
        assertFalse(InputValidator.validateCost("-1").isValid)
    }

    @Test fun `validateCost - cost over 1M rejected`() {
        assertFalse(InputValidator.validateCost("1000001").isValid)
    }

    @Test fun `validateCost - non-numeric rejected`() {
        assertFalse(InputValidator.validateCost("five hundred").isValid)
    }

    // ── sanitizeText ─────────────────────────────────────────────────────────

    @Test fun `sanitizeText - strips ASCII control chars`() {
        val result = InputValidator.sanitizeText("hello\u0007world\u001F!")
        assertFalse(result.contains("\u0007"))
        assertFalse(result.contains("\u001F"))
        assertTrue(result.contains("hello"))
        assertTrue(result.contains("world"))
    }

    @Test fun `sanitizeText - preserves Arabic text`() {
        val ar = "إصلاح السيارة"
        assertEquals(ar, InputValidator.sanitizeText(ar))
    }

    @Test fun `sanitizeText - trims surrounding whitespace`() {
        assertEquals("test", InputValidator.sanitizeText("  test  "))
    }

    @Test fun `sanitizeText - hard caps at 2000 chars`() {
        val long = "A".repeat(3000)
        assertEquals(2000, InputValidator.sanitizeText(long).length)
    }

    // ── isPathSafe ────────────────────────────────────────────────────────────

    @Test fun `isPathSafe - file in allowed dir is safe`() {
        val dir = System.getProperty("java.io.tmpdir")!!
        val file = java.io.File(dir, "test.jpg")
        assertTrue(InputValidator.isPathSafe(file.absolutePath, dir))
    }

    @Test fun `isPathSafe - path traversal is detected`() {
        val allowed = "/data/user/0/com.workshoptech/files"
        val evil    = "/data/user/0/com.workshoptech/files/../../../etc/passwd"
        // canonical path should resolve traversal and fail the prefix check
        assertFalse(InputValidator.isPathSafe(evil, allowed))
    }

    // ── ValidationResult helpers ──────────────────────────────────────────────

    @Test fun `ValidationResult Valid - isValid true, valueOrNull non-null`() {
        val r = ValidationResult.Valid("hello")
        assertTrue(r.isValid)
        assertEquals("hello", r.valueOrNull())
        assertNull(r.errorMessage)
    }

    @Test fun `ValidationResult Error - isValid false, errorMessage non-null`() {
        val r = ValidationResult.Error("فشل")
        assertFalse(r.isValid)
        assertEquals("فشل", r.errorMessage)
        assertNull(r.valueOrNull())
    }
}
