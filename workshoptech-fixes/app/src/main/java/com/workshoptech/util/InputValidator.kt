package com.workshoptech.util

/**
 * Central input validation — prevents injection and bad data entering the DB.
 *
 * Security: All user-supplied strings pass through here before persistence.
 */
object InputValidator {

    // ── Plate number ─────────────────────────────────────────────────────────
    private val PLATE_REGEX = Regex("""^[A-Za-z0-9\u0600-\u06FF\s\-/]{2,20}$""")

    fun validatePlate(plate: String): ValidationResult {
        val trimmed = plate.trim()
        if (trimmed.isBlank())  return ValidationResult.Error("رقم اللوحة مطلوب")
        if (trimmed.length < 2) return ValidationResult.Error("رقم اللوحة قصير جداً")
        if (trimmed.length > 20) return ValidationResult.Error("رقم اللوحة طويل جداً")
        if (!PLATE_REGEX.matches(trimmed)) return ValidationResult.Error("رقم اللوحة يحتوي على رموز غير مسموح بها")
        return ValidationResult.Valid(trimmed.uppercase())
    }

    // ── Phone number ──────────────────────────────────────────────────────────
    private val PHONE_REGEX = Regex("""^[\d\+\-\s\(\)]{7,20}$""")

    fun validatePhone(phone: String): ValidationResult {
        val trimmed = phone.trim()
        if (trimmed.isBlank()) return ValidationResult.Valid(trimmed) // optional field
        if (!PHONE_REGEX.matches(trimmed)) return ValidationResult.Error("رقم الهاتف غير صحيح")
        return ValidationResult.Valid(trimmed)
    }

    // ── Name ──────────────────────────────────────────────────────────────────
    fun validateName(name: String, field: String = "الاسم"): ValidationResult {
        val trimmed = sanitizeText(name)
        if (trimmed.isBlank()) return ValidationResult.Error("$field مطلوب")
        if (trimmed.length < 2) return ValidationResult.Error("$field قصير جداً")
        if (trimmed.length > 100) return ValidationResult.Error("$field طويل جداً (الحد 100 حرف)")
        return ValidationResult.Valid(trimmed)
    }

    // ── Notes / free text ─────────────────────────────────────────────────────
    fun validateNotes(notes: String, maxLength: Int = 1000): ValidationResult {
        val trimmed = sanitizeText(notes)
        if (trimmed.length > maxLength) return ValidationResult.Error("النص طويل جداً (الحد $maxLength حرف)")
        return ValidationResult.Valid(trimmed)
    }

    // ── Year ──────────────────────────────────────────────────────────────────
    fun validateYear(year: String): ValidationResult {
        if (year.isBlank()) return ValidationResult.Valid(year)
        val y = year.trim().toIntOrNull() ?: return ValidationResult.Error("سنة الصنع غير صحيحة")
        if (y < 1950 || y > 2030) return ValidationResult.Error("سنة الصنع خارج النطاق (1950–2030)")
        return ValidationResult.Valid(y.toString())
    }

    // ── Cost ──────────────────────────────────────────────────────────────────
    fun validateCost(cost: String): ValidationResult {
        if (cost.isBlank()) return ValidationResult.Valid(cost)
        val c = cost.trim().toDoubleOrNull() ?: return ValidationResult.Error("التكلفة غير صحيحة")
        if (c < 0) return ValidationResult.Error("التكلفة لا يمكن أن تكون سالبة")
        if (c > 1_000_000) return ValidationResult.Error("التكلفة كبيرة جداً")
        return ValidationResult.Valid(c.toString())
    }

    // ── Sanitize free-form text: strip control chars ──────────────────────────
    fun sanitizeText(text: String): String =
        text.trim()
            .replace(Regex("""[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]"""), "") // strip control chars
            .take(2000) // hard cap

    // ── Path traversal guard ──────────────────────────────────────────────────
    fun isPathSafe(path: String, allowedPrefix: String): Boolean {
        val canonical = java.io.File(path).canonicalPath
        return canonical.startsWith(allowedPrefix)
    }
}

sealed class ValidationResult {
    data class Valid(val value: String) : ValidationResult()
    data class Error(val message: String) : ValidationResult()

    val isValid get() = this is Valid
    val errorMessage get() = (this as? Error)?.message
    fun valueOrNull() = (this as? Valid)?.value
}
