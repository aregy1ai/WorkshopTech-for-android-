package com.workshoptech.ml

import android.graphics.Bitmap
import android.graphics.Matrix

object ImagePreprocessor {
    fun prepareForOcr(bitmap: Bitmap, targetWidth: Int = 640, targetHeight: Int = 480): Bitmap {
        val scaled = scaleToFit(bitmap, targetWidth, targetHeight)
        return if (scaled.width < scaled.height) rotateBitmap(scaled, 90f) else scaled
    }

    fun prepareForDamageAnalysis(bitmap: Bitmap, targetSize: Int = 224): Bitmap = centerCrop(scaleToFit(bitmap, targetSize, targetSize), targetSize, targetSize)

    fun scaleToFit(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val ratio = minOf(maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height)
        if (ratio >= 1f) return bitmap
        val newW = (bitmap.width * ratio).toInt()
        val newH = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    fun centerCrop(bitmap: Bitmap, targetW: Int, targetH: Int): Bitmap {
        val x = ((bitmap.width - targetW) / 2).coerceAtLeast(0)
        val y = ((bitmap.height - targetH) / 2).coerceAtLeast(0)
        val w = minOf(targetW, bitmap.width - x)
        val h = minOf(targetH, bitmap.height - y)
        return Bitmap.createBitmap(bitmap, x, y, w, h)
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun normalizePixels(bitmap: Bitmap): FloatArray {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return FloatArray(pixels.size * 3) { i ->
            val pixel = pixels[i / 3]
            when (i % 3) {
                0 -> ((pixel shr 16) and 0xFF) / 255f
                1 -> ((pixel shr 8) and 0xFF) / 255f
                2 -> (pixel and 0xFF) / 255f
                else -> 0f
            }
        }
    }
}
