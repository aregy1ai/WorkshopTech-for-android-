package com.workshoptech.domain

import com.workshoptech.domain.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for AppResult sealed class and AppException hierarchy.
 *
 * Coverage:
 *  - Success: isSuccess, getOrNull, exceptionOrNull
 *  - Error: isError, getOrNull returns null, exceptionOrNull non-null
 *  - Loading: isLoading
 *  - runCatchingAppResult: success path
 *  - runCatchingAppResult: AppException path → Error(appException)
 *  - runCatchingAppResult: generic Exception → Error(UnknownException)
 *  - Each AppException subtype has non-blank message
 */
class AppResultTest {

    // ── Success ───────────────────────────────────────────────────────────────

    @Test fun `Success - isSuccess true`() {
        assertTrue(AppResult.Success(42).isSuccess)
    }

    @Test fun `Success - isError false`() {
        assertFalse(AppResult.Success("hi").isError)
    }

    @Test fun `Success - isLoading false`() {
        assertFalse(AppResult.Success(Unit).isLoading)
    }

    @Test fun `Success - getOrNull returns data`() {
        assertEquals(99, AppResult.Success(99).getOrNull())
    }

    @Test fun `Success - exceptionOrNull returns null`() {
        assertNull(AppResult.Success("ok").exceptionOrNull())
    }

    // ── Error ─────────────────────────────────────────────────────────────────

    @Test fun `Error - isError true`() {
        val e = AppException.UnknownException()
        assertTrue(AppResult.Error(e).isError)
    }

    @Test fun `Error - isSuccess false`() {
        val e = AppException.UnknownException()
        assertFalse(AppResult.Error(e).isSuccess)
    }

    @Test fun `Error - getOrNull returns null`() {
        val e = AppException.UnknownException()
        assertNull(AppResult.Error(e).getOrNull())
    }

    @Test fun `Error - exceptionOrNull returns exception`() {
        val ex = AppException.DatabaseException("test failure")
        val result: AppResult<Unit> = AppResult.Error(ex)
        assertSame(ex, result.exceptionOrNull())
    }

    // ── Loading ───────────────────────────────────────────────────────────────

    @Test fun `Loading - isLoading true`() {
        assertTrue(AppResult.Loading.isLoading)
    }

    @Test fun `Loading - isSuccess and isError both false`() {
        assertFalse(AppResult.Loading.isSuccess)
        assertFalse(AppResult.Loading.isError)
    }

    // ── runCatchingAppResult ──────────────────────────────────────────────────

    @Test fun `runCatchingAppResult success returns Success wrapping value`() {
        val result = runCatchingAppResult { 42 }
        assertEquals(AppResult.Success(42), result)
    }

    @Test fun `runCatchingAppResult with AppException returns Error`() {
        val ex = AppException.ValidationException("bad input")
        val result = runCatchingAppResult<Unit> { throw ex }
        assertTrue(result.isError)
        assertSame(ex, result.exceptionOrNull())
    }

    @Test fun `runCatchingAppResult with generic Exception wraps in UnknownException`() {
        val result = runCatchingAppResult<Unit> { throw IllegalStateException("oops") }
        assertTrue(result.isError)
        val wrapped = result.exceptionOrNull()
        assertTrue(wrapped is AppException.UnknownException)
        assertNotNull(wrapped!!.cause)
    }

    @Test fun `runCatchingAppResult Unit block - success`() {
        val result = runCatchingAppResult { Unit }
        assertTrue(result.isSuccess)
    }

    // ── AppException subtypes ─────────────────────────────────────────────────

    @Test fun `DatabaseException has non-blank message`() {
        val e = AppException.DatabaseException("schema error")
        assertTrue(e.message!!.isNotBlank())
        assertTrue(e.message!!.contains("schema error"))
    }

    @Test fun `NetworkException has non-blank message`() {
        val e = AppException.NetworkException("timeout")
        assertTrue(e.message!!.isNotBlank())
    }

    @Test fun `ImageProcessingException has non-blank message`() {
        val e = AppException.ImageProcessingException("OOM")
        assertTrue(e.message!!.isNotBlank())
    }

    @Test fun `OcrException has non-blank message`() {
        val e = AppException.OcrException("no text found")
        assertTrue(e.message!!.isNotBlank())
    }

    @Test fun `PermissionException includes permission name in message`() {
        val e = AppException.PermissionException("CAMERA")
        assertTrue(e.message!!.contains("CAMERA"))
    }

    @Test fun `FileNotFoundException includes path in message`() {
        val e = AppException.FileNotFoundException("/photos/test.jpg")
        assertTrue(e.message!!.contains("/photos/test.jpg"))
    }

    @Test fun `ValidationException has Arabic error prefix`() {
        val e = AppException.ValidationException("حقل مطلوب")
        assertTrue(e.message!!.contains("حقل مطلوب"))
    }

    @Test fun `UnknownException can wrap a cause`() {
        val cause = RuntimeException("root cause")
        val e = AppException.UnknownException(cause)
        assertSame(cause, e.cause)
    }

    @Test fun `UnknownException with null cause does not throw`() {
        val e = AppException.UnknownException(null)
        assertNotNull(e.message)
    }
}
