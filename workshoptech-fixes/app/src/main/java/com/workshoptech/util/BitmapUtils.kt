package com.workshoptech.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File

/**
 * Memory-safe bitmap loading utilities.
 *
 * Performance: Uses inSampleSize to prevent OOM on large camera images.
 * Security: Validates file existence before decoding.
 */
object BitmapUtils {

    private const val MAX_DIMENSION = 1920   // px — caps both width and height
    private const val JPEG_QUALITY  = 85     // %

    /**
     * Load a bitmap from file with auto-downsampling to avoid OOM.
     * Corrects EXIF rotation automatically.
     *
     * @return Bitmap or null if the file cannot be decoded.
     */
    fun loadSafe(path: String, maxDim: Int = MAX_DIMENSION): Bitmap? {
        val file = File(path)
        if (!file.exists() || !file.canRead()) return null

        // Step 1: decode bounds only
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, opts)
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null

        // Step 2: compute sample size
        opts.inSampleSize    = calculateInSampleSize(opts.outWidth, opts.outHeight, maxDim)
        opts.inJustDecodeBounds = false
        opts.inPreferredConfig  = Bitmap.Config.ARGB_8888

        val raw = BitmapFactory.decodeFile(path, opts) ?: return null

        // Step 3: fix EXIF rotation
        return fixRotation(raw, path)
    }

    /**
     * Compute the largest power-of-2 inSampleSize that keeps the image
     * within [maxDim × maxDim].
     */
    fun calculateInSampleSize(srcW: Int, srcH: Int, maxDim: Int): Int {
        var sample = 1
        var hw = srcH / 2
        var ww = srcW / 2
        while (hw >= maxDim && ww >= maxDim) {
            sample *= 2
            hw /= 2
            ww /= 2
        }
        return sample
    }

    /**
     * Read EXIF orientation and rotate/flip the bitmap accordingly.
     */
    private fun fixRotation(src: Bitmap, path: String): Bitmap {
        val exif = try { ExifInterface(path) } catch (_: Exception) { return src }
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90  -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL   -> matrix.postScale(1f, -1f)
            else -> return src
        }
        return try {
            Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
                .also { if (it !== src) src.recycle() }
        } catch (_: OutOfMemoryError) {
            src // return original if OOM during rotation
        }
    }

    /**
     * Scale bitmap to fit within maxDim × maxDim, preserving aspect ratio.
     */
    fun scaleToFit(src: Bitmap, maxDim: Int = MAX_DIMENSION): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= maxDim && h <= maxDim) return src
        val scale = maxDim.toFloat() / maxOf(w, h)
        val nw = (w * scale).toInt()
        val nh = (h * scale).toInt()
        return Bitmap.createScaledBitmap(src, nw, nh, true)
    }

    /**
     * Recycle bitmap safely (noop if already recycled or null).
     */
    fun recycleQuietly(bm: Bitmap?) {
        try { if (bm != null && !bm.isRecycled) bm.recycle() } catch (_: Exception) { }
    }
}
