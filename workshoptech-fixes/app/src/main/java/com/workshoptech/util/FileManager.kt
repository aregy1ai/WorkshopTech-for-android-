package com.workshoptech.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileManager {

    private const val PHOTOS_DIR  = "Pictures"
    private const val VIDEOS_DIR  = "Movies"
    private const val TEMP_DIR    = "temp"
    private val DATE_FORMAT = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun createPhotoFile(context: Context): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "cases")
        dir.mkdirs()
        return File(dir, "IMG_${DATE_FORMAT.format(Date())}.jpg")
    }

    fun createVideoFile(context: Context): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "cases")
        dir.mkdirs()
        return File(dir, "VID_${DATE_FORMAT.format(Date())}.mp4")
    }

    fun createThumbnailFile(context: Context, baseName: String): File {
        val dir = File(context.cacheDir, "thumbnails")
        dir.mkdirs()
        return File(dir, "THUMB_${baseName}.jpg")
    }

    fun getUriForFile(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun cleanupTempFiles(context: Context) {
        val tempDir = File(context.cacheDir, TEMP_DIR)
        if (tempDir.exists()) {
            val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
            tempDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoff) file.delete()
            }
        }
    }

    fun formatFileSize(bytes: Long): String = when {
        bytes < 1024       -> "$bytes B"
        bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
        else               -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
    }

    fun deleteFile(path: String): Boolean = runCatching { File(path).delete() }.getOrDefault(false)

    fun fileExists(path: String?): Boolean = !path.isNullOrBlank() && File(path).exists()
}
