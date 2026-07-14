package com.workshoptech.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * App-private file management.
 *
 * Security:
 *  - Photos/videos stored in context.filesDir (MODE_PRIVATE, not world-readable).
 *  - External storage (getExternalFilesDir) avoided — app-sandbox is sufficient
 *    and avoids READ_EXTERNAL_STORAGE permission entirely.
 *  - isPathSafe() guards every inbound path against traversal attacks.
 *  - FileProvider used for sharing — never raw file:// URIs.
 *
 * Performance:
 *  - Temp files capped at 24 h; cleaned on app start.
 *  - DIR_PHOTOS and DIR_VIDEOS created lazily with mkdirs().
 */
object FileManager {

    private const val TAG             = "FileManager"
    private const val DIR_PHOTOS      = "case_photos"
    private const val DIR_VIDEOS      = "case_videos"
    private const val DIR_THUMBS      = "thumbnails"
    private const val DIR_TEMP        = "temp"
    private const val TEMP_MAX_AGE_MS = 24L * 60 * 60 * 1000   // 24 h

    private val DATE_FORMAT = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    // ── Directory accessors ───────────────────────────────────────────────────

    private fun photosDir(context: Context)  = File(context.filesDir, DIR_PHOTOS).also  { it.mkdirs() }
    private fun videosDir(context: Context)  = File(context.filesDir, DIR_VIDEOS).also  { it.mkdirs() }
    private fun thumbsDir(context: Context)  = File(context.filesDir, DIR_THUMBS).also  { it.mkdirs() }
    private fun tempDir(context: Context)    = File(context.filesDir, DIR_TEMP).also    { it.mkdirs() }

    // ── File creation ─────────────────────────────────────────────────────────

    fun createPhotoFile(context: Context, caseId: String? = null): File {
        val dir = if (caseId != null)
            File(photosDir(context), caseId).also { it.mkdirs() }
        else
            photosDir(context)
        return File(dir, "IMG_${DATE_FORMAT.format(Date())}.jpg")
    }

    fun createVideoFile(context: Context, caseId: String? = null): File {
        val dir = if (caseId != null)
            File(videosDir(context), caseId).also { it.mkdirs() }
        else
            videosDir(context)
        return File(dir, "VID_${DATE_FORMAT.format(Date())}.mp4")
    }

    fun createThumbnailFile(context: Context, baseName: String): File =
        File(thumbsDir(context), "THUMB_$baseName.jpg")

    fun createTempFile(context: Context, suffix: String = ".tmp"): File =
        File(tempDir(context), "tmp_${System.currentTimeMillis()}$suffix")

    // ── FileProvider URI ──────────────────────────────────────────────────────

    fun getUriForFile(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    // ── Security: path traversal guard ───────────────────────────────────────

    /**
     * Returns true iff [path] resolves inside context.filesDir.
     * Always validate before processing paths from external sources.
     */
    fun isPathSafe(context: Context, path: String): Boolean {
        return try {
            val canonical  = File(path).canonicalPath
            val allowedDir = context.filesDir.canonicalPath
            canonical.startsWith(allowedDir)
        } catch (_: Exception) { false }
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    fun cleanupTempFiles(context: Context) {
        val cutoff = System.currentTimeMillis() - TEMP_MAX_AGE_MS
        var deleted = 0
        tempDir(context).listFiles()?.forEach { f ->
            if (f.lastModified() < cutoff && f.delete()) deleted++
        }
        if (deleted > 0) Log.d(TAG, "Temp cleanup: removed $deleted file(s)")
    }

    fun deleteCaseFiles(context: Context, caseId: String) {
        listOf(
            File(photosDir(context), caseId),
            File(videosDir(context), caseId),
            File(thumbsDir(context), caseId)
        ).forEach { it.deleteRecursively() }
    }

    fun deleteFile(path: String): Boolean =
        runCatching { File(path).delete() }.getOrDefault(false)

    fun fileExists(path: String?): Boolean =
        !path.isNullOrBlank() && File(path).exists()

    // ── Utility ───────────────────────────────────────────────────────────────

    fun formatFileSize(bytes: Long): String = when {
        bytes < 1_024              -> "$bytes B"
        bytes < 1_048_576          -> "${"%.1f".format(bytes / 1_024.0)} KB"
        bytes < 1_073_741_824      -> "${"%.1f".format(bytes / 1_048_576.0)} MB"
        else                       -> "${"%.2f".format(bytes / 1_073_741_824.0)} GB"
    }

    fun totalSizeBytes(context: Context): Long =
        context.filesDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}
