package com.glitchstudio.app.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExportFormat(
    val label: String,
    val ext: String,
    val mime: String,
    val hasQuality: Boolean,
    val animated: Boolean = false,
) {
    PNG("PNG", "png", "image/png", false),
    JPEG("JPEG", "jpg", "image/jpeg", true),
    WEBP("WebP", "webp", "image/webp", true),
    GIF("GIF", "gif", "image/gif", false, animated = true),
}

/** Saves processed images/GIFs to the shared gallery and prepares share URIs. */
object ImageExporter {

    private const val ALBUM = "GlitchStudio"

    fun saveImage(context: Context, bitmap: Bitmap, format: ExportFormat, quality: Int): Uri? {
        val name = fileName(format.ext)
        return save(context, name, format.mime) { compress(bitmap, format, quality, it) }
    }

    fun saveGif(context: Context, bytes: ByteArray): Uri? {
        val name = fileName("gif")
        return save(context, name, "image/gif") { it.write(bytes) }
    }

    fun shareImage(context: Context, bitmap: Bitmap, format: ExportFormat, quality: Int): Uri {
        return cacheForShare(context, fileName(format.ext)) { compress(bitmap, format, quality, it) }
    }

    fun shareGif(context: Context, bytes: ByteArray): Uri {
        return cacheForShare(context, fileName("gif")) { it.write(bytes) }
    }

    // --- internals -----------------------------------------------------------

    private fun compress(bitmap: Bitmap, format: ExportFormat, quality: Int, os: OutputStream) {
        val cf = when (format) {
            ExportFormat.PNG -> Bitmap.CompressFormat.PNG
            ExportFormat.JPEG -> Bitmap.CompressFormat.JPEG
            ExportFormat.WEBP ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    Bitmap.CompressFormat.WEBP_LOSSY
                else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
            ExportFormat.GIF -> error("GIF is not produced via Bitmap.compress")
        }
        bitmap.compress(cf, quality, os)
    }

    private fun fileName(ext: String): String {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "Glitch_$ts.$ext"
    }

    private fun save(context: Context, name: String, mime: String, write: (OutputStream) -> Unit): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveScoped(context, name, mime, write)
        } else {
            saveLegacy(context, name, mime, write)
        }
    }

    private fun saveScoped(context: Context, name: String, mime: String, write: (OutputStream) -> Unit): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        resolver.openOutputStream(uri)?.use(write) ?: return null
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(context: Context, name: String, mime: String, write: (OutputStream) -> Unit): Uri? {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), ALBUM)
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, name)
        file.outputStream().use(write)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            put(MediaStore.Images.Media.DATA, file.absolutePath)
        }
        return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    private fun cacheForShare(context: Context, name: String, write: (OutputStream) -> Unit): Uri {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, name)
        file.outputStream().use(write)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
