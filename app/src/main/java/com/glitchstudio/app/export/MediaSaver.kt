package com.glitchstudio.app.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

enum class ExportFormat(
    val label: String,
    val mime: String,
    val extension: String,
    val animated: Boolean = false
) {
    PNG("PNG", "image/png", "png"),
    JPEG("JPEG", "image/jpeg", "jpg"),
    WEBP("WEBP", "image/webp", "webp"),
    GIF("GIF", "image/gif", "gif", animated = true)
}

/** Writes finished exports into the device gallery (Pictures/Glitch Studio). */
object MediaSaver {

    private const val FOLDER = "Glitch Studio"

    fun saveImage(context: Context, bitmap: Bitmap, format: ExportFormat, quality: Int): Uri? {
        val name = fileName(format.extension)
        return write(context, name, format.mime) { os ->
            val fmt = when (format) {
                ExportFormat.PNG -> Bitmap.CompressFormat.PNG
                ExportFormat.JPEG -> Bitmap.CompressFormat.JPEG
                ExportFormat.WEBP -> webpFormat()
                ExportFormat.GIF -> Bitmap.CompressFormat.PNG // unused; GIF goes through saveBytes
            }
            bitmap.compress(fmt, quality, os)
        }
    }

    fun saveBytes(context: Context, bytes: ByteArray, format: ExportFormat): Uri? {
        val name = fileName(format.extension)
        return write(context, name, format.mime) { os -> os.write(bytes) }
    }

    @Suppress("DEPRECATION")
    private fun webpFormat(): Bitmap.CompressFormat =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSLESS
        else Bitmap.CompressFormat.WEBP

    private fun fileName(ext: String): String =
        "glitch_${System.currentTimeMillis()}.$ext"

    private inline fun write(
        context: Context,
        name: String,
        mime: String,
        block: (OutputStream) -> Unit
    ): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, mime)
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$FOLDER")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
            resolver.openOutputStream(uri)?.use(block) ?: return null
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                FOLDER
            )
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, name)
            FileOutputStream(file).use(block)
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DATA, file.absolutePath)
                put(MediaStore.Images.Media.MIME_TYPE, mime)
            }
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: Uri.fromFile(file)
        }
    }
}
