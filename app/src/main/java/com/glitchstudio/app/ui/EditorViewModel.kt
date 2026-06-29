package com.glitchstudio.app.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.glitchstudio.app.effects.EffectCategory
import com.glitchstudio.app.effects.EffectRepository
import com.glitchstudio.app.effects.ShaderEffect
import com.glitchstudio.app.export.ExportFormat
import com.glitchstudio.app.export.GifRenderer
import com.glitchstudio.app.export.ImageExporter
import com.glitchstudio.app.export.ImageRenderer
import com.glitchstudio.app.util.BitmapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns the editing session: the loaded image, the selected effect and its live
 * parameter values, and the export pipeline. The view layer observes [state] and
 * feeds [preview] into the GL preview.
 */
class EditorViewModel(app: Application) : AndroidViewModel(app) {

    data class State(
        val sourceLoaded: Boolean = false,
        val isLoading: Boolean = false,
        val category: EffectCategory = EffectRepository.categories.first(),
        val effect: ShaderEffect = EffectRepository.firstEffect,
        val values: List<Float> = EffectRepository.firstEffect.defaultValues().toList(),
        val compare: Boolean = false,
        val exportFormat: ExportFormat = ExportFormat.PNG,
        val quality: Int = 95,
        val gifFrames: Int = 24,
        val gifFps: Int = 16,
        val isExporting: Boolean = false,
        val exportProgress: Float = 0f,
        val message: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _preview = MutableStateFlow<Bitmap?>(null)
    val preview: StateFlow<Bitmap?> = _preview.asStateFlow()

    private var sourceBitmap: Bitmap? = null

    fun loadImage(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val ctx = getApplication<Application>()
            val src = withContext(Dispatchers.IO) {
                BitmapUtils.load(ctx, uri, BitmapUtils.SOURCE_MAX)
            }
            if (src == null) {
                _state.update { it.copy(isLoading = false, message = "Couldn't open that image") }
                return@launch
            }
            val previewBmp = withContext(Dispatchers.Default) {
                BitmapUtils.scaledDown(src, BitmapUtils.PREVIEW_MAX)
            }
            sourceBitmap?.recycle()
            sourceBitmap = src
            _preview.value?.recycle()
            _preview.value = previewBmp
            _state.update { it.copy(sourceLoaded = true, isLoading = false) }
        }
    }

    fun selectCategory(category: EffectCategory) {
        _state.update { it.copy(category = category) }
    }

    fun selectEffect(effect: ShaderEffect) {
        _state.update {
            it.copy(effect = effect, values = effect.defaultValues().toList())
        }
    }

    fun setValue(index: Int, value: Float) {
        _state.update {
            if (index !in it.values.indices) it
            else it.copy(values = it.values.toMutableList().apply { this[index] = value })
        }
    }

    fun resetValues() {
        _state.update { it.copy(values = it.effect.defaultValues().toList()) }
    }

    fun setCompare(compare: Boolean) {
        _state.update { it.copy(compare = compare) }
    }

    fun setExportFormat(format: ExportFormat) = _state.update { it.copy(exportFormat = format) }
    fun setQuality(quality: Int) = _state.update { it.copy(quality = quality.coerceIn(10, 100)) }
    fun setGifFrames(frames: Int) = _state.update { it.copy(gifFrames = frames.coerceIn(4, 60)) }
    fun setGifFps(fps: Int) = _state.update { it.copy(gifFps = fps.coerceIn(4, 30)) }

    fun clearImage() {
        sourceBitmap?.recycle()
        sourceBitmap = null
        _preview.value?.recycle()
        _preview.value = null
        _state.update {
            State().copy(
                exportFormat = it.exportFormat,
                quality = it.quality,
                gifFrames = it.gifFrames,
                gifFps = it.gifFps,
            )
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    /**
     * Renders the current effect at full resolution and either saves it to the
     * gallery or hands a shareable [Uri] to [onShare].
     */
    fun export(share: Boolean, onShare: (Uri) -> Unit) {
        val src = sourceBitmap ?: return
        val s = _state.value
        if (s.isExporting) return
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true, exportProgress = 0f, message = null) }
            val uri = withContext(Dispatchers.Default) {
                try {
                    val eff = s.effect
                    val vals = s.values.toFloatArray()
                    if (s.exportFormat == ExportFormat.GIF) {
                        val bytes = GifRenderer.render(src, eff, vals, s.gifFrames, s.gifFps) { p ->
                            _state.update { it.copy(exportProgress = p) }
                        }
                        if (share) ImageExporter.shareGif(ctx, bytes) else ImageExporter.saveGif(ctx, bytes)
                    } else {
                        val bmp = ImageRenderer.render(src, eff, vals)
                        val out = if (share) ImageExporter.shareImage(ctx, bmp, s.exportFormat, s.quality)
                        else ImageExporter.saveImage(ctx, bmp, s.exportFormat, s.quality)
                        bmp.recycle()
                        out
                    }
                } catch (e: Exception) {
                    null
                }
            }
            _state.update {
                it.copy(
                    isExporting = false,
                    exportProgress = 0f,
                    message = when {
                        uri == null -> "Export failed"
                        share -> null
                        else -> "Saved to Pictures/GlitchStudio"
                    },
                )
            }
            if (share && uri != null) onShare(uri)
        }
    }

    override fun onCleared() {
        super.onCleared()
        sourceBitmap?.recycle()
        sourceBitmap = null
        _preview.value?.recycle()
        _preview.value = null
    }
}
