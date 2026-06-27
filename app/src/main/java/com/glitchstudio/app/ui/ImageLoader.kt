package com.glitchstudio.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/** Decodes a gallery image to an upright, reasonably sized [Bitmap]. */
object ImageLoader {

    suspend fun load(context: Context, uri: Uri, maxSize: Int = 2560): Bitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver

                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                val (w, h) = bounds.outWidth to bounds.outHeight
                if (w <= 0 || h <= 0) return@runCatching null

                var sample = 1
                while (max(w, h) / sample > maxSize) sample *= 2

                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val decoded = resolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                } ?: return@runCatching null

                val rotation = resolver.openInputStream(uri)?.use { readRotation(it) } ?: 0
                if (rotation == 0) decoded else rotate(decoded, rotation)
            }.getOrNull()
        }

    private fun readRotation(stream: java.io.InputStream): Int {
        return try {
            when (ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (_: Throwable) {
            0
        }
    }

    private fun rotate(bmp: Bitmap, degrees: Int): Bitmap {
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }
}
