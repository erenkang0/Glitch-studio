package com.glitchstudio.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlin.math.max

object BitmapUtils {

    /** Largest edge kept for the live preview; keeps the GL pipeline smooth. */
    const val PREVIEW_MAX = 1600

    /** Safety cap for the in-memory source used for export. */
    const val SOURCE_MAX = 4096

    fun load(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        val resolver = context.contentResolver
        // First pass: bounds only, to compute an integer sample size.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null

        val rotated = applyExifRotation(context, uri, decoded)
        return clampToMax(rotated, maxDimension)
    }

    private fun sampleSize(w: Int, h: Int, maxDimension: Int): Int {
        var sample = 1
        var longest = max(w, h)
        while (longest / 2 >= maxDimension) {
            longest /= 2
            sample *= 2
        }
        return sample
    }

    private fun clampToMax(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / longest
        val w = max(1, (bitmap.width * scale).toInt())
        val h = max(1, (bitmap.height * scale).toInt())
        val scaled = Bitmap.createScaledBitmap(bitmap, w, h, true)
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = try {
            context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    fun aspectRatio(bitmap: Bitmap): Float =
        bitmap.width.toFloat() / max(1, bitmap.height).toFloat()

    /** Returns a new, independent bitmap no larger than [maxDimension] on its long edge. */
    fun scaledDown(src: Bitmap, maxDimension: Int): Bitmap {
        val longest = max(src.width, src.height)
        if (longest <= maxDimension) return src.copy(Bitmap.Config.ARGB_8888, false)
        val scale = maxDimension.toFloat() / longest
        val w = max(1, (src.width * scale).toInt())
        val h = max(1, (src.height * scale).toInt())
        return Bitmap.createScaledBitmap(src, w, h, true)
    }
}
