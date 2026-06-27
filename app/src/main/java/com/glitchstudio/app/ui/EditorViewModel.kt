package com.glitchstudio.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glitchstudio.app.effects.Categories
import com.glitchstudio.app.effects.Effect
import com.glitchstudio.app.effects.EffectRegistry
import com.glitchstudio.app.export.ExportFormat
import com.glitchstudio.app.export.GifEncoder
import com.glitchstudio.app.export.MediaSaver
import com.glitchstudio.app.gl.GlPhotoView
import com.glitchstudio.app.gl.MaskType
import com.glitchstudio.app.gl.RenderLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class PanelTab(val label: String) {
    EFFECTS("Effects"), ADJUST("Adjust"), LAYERS("Layers"), MASK("Mask")
}

sealed interface ExportStatus {
    data object Working : ExportStatus
    data class Done(val uri: Uri, val animated: Boolean) : ExportStatus
    data object Error : ExportStatus
}

/** One effect layer as edited in the UI. */
data class LayerUiState(
    val id: Long,
    val effect: Effect,
    val params: List<Float>,
    val opacity: Float = 1f,
    val enabled: Boolean = true,
    val maskType: MaskType = MaskType.NONE,
    val maskCx: Float = 0.5f,
    val maskCy: Float = 0.5f,
    val maskSize: Float = 0.4f,
    val maskFeather: Float = 0.18f,
    val maskAngle: Float = 0f,
    val maskInvert: Boolean = false,
    val maskBitmap: Bitmap? = null,
    val maskRevision: Int = 0,
    val brushSize: Float = 0.12f,
    val brushHardness: Float = 0.7f,
    val brushErase: Boolean = false
) {
    fun toRenderLayer(): RenderLayer = RenderLayer(
        id = id,
        effect = effect,
        params = FloatArray(8) { params.getOrElse(it) { 0f } },
        opacity = opacity,
        enabled = enabled,
        maskType = maskType,
        maskCx = maskCx, maskCy = maskCy,
        maskSize = maskSize, maskFeather = maskFeather,
        maskAngle = maskAngle, maskInvert = maskInvert,
        maskBitmap = maskBitmap, maskRevision = maskRevision
    )
}

class EditorViewModel : ViewModel() {

    var hasImage by mutableStateOf(false); private set
    val layers = mutableStateListOf<LayerUiState>()
    var selectedIndex by mutableStateOf(0); private set
    var playing by mutableStateOf(true); private set
    var panelTab by mutableStateOf(PanelTab.EFFECTS)
    var category by mutableStateOf(Categories.GLITCH)
    var showExport by mutableStateOf(false)
    var exportStatus by mutableStateOf<ExportStatus?>(null)
    var maskEditing by mutableStateOf(false)

    /** Bumped on every state change so the GL layer can be refreshed cheaply. */
    var revision by mutableStateOf(0); private set

    private var nextId = 1L
    private var imageW = 1
    private var imageH = 1

    val selected: LayerUiState? get() = layers.getOrNull(selectedIndex)

    val continuousRender: Boolean
        get() = playing && layers.any { it.enabled && it.effect.animated }

    val hasAnimatedLayer: Boolean
        get() = layers.any { it.enabled && it.effect.animated }

    fun onImageLoaded(width: Int, height: Int) {
        imageW = width.coerceAtLeast(1)
        imageH = height.coerceAtLeast(1)
        if (layers.isEmpty()) {
            layers.add(makeLayer(EffectRegistry.original))
            selectedIndex = 0
        }
        hasImage = true
        touch()
    }

    fun renderLayers(): List<RenderLayer> = layers.map { it.toRenderLayer() }

    fun selectLayer(index: Int) {
        if (index in layers.indices) { selectedIndex = index; touch() }
    }

    fun togglePlaying() { playing = !playing; touch() }

    fun addLayer(effect: Effect = defaultNewEffect()) {
        layers.add(makeLayer(effect))
        selectedIndex = layers.lastIndex
        touch()
    }

    fun setEffectForSelected(effect: Effect) {
        val i = selectedIndex
        val layer = layers.getOrNull(i) ?: return
        layers[i] = layer.copy(effect = effect, params = effect.params.map { it.default })
        touch()
    }

    fun updateParam(paramIndex: Int, value: Float) {
        val i = selectedIndex
        val layer = layers.getOrNull(i) ?: return
        if (paramIndex !in layer.params.indices) return
        val np = layer.params.toMutableList().also { it[paramIndex] = value }
        layers[i] = layer.copy(params = np)
        touch()
    }

    fun resetSelectedParams() {
        val i = selectedIndex
        val layer = layers.getOrNull(i) ?: return
        layers[i] = layer.copy(params = layer.effect.params.map { it.default }, opacity = 1f)
        touch()
    }

    fun setOpacity(value: Float) = updateSelected { it.copy(opacity = value) }
    fun toggleEnabled(index: Int) {
        val layer = layers.getOrNull(index) ?: return
        layers[index] = layer.copy(enabled = !layer.enabled); touch()
    }

    fun removeLayer(index: Int) {
        if (layers.size <= 1 || index !in layers.indices) return
        layers.removeAt(index)
        if (selectedIndex >= layers.size) selectedIndex = layers.lastIndex
        touch()
    }

    fun moveLayer(from: Int, to: Int) {
        if (from !in layers.indices || to !in layers.indices) return
        val item = layers.removeAt(from)
        layers.add(to, item)
        selectedIndex = to
        touch()
    }

    fun setMaskType(type: MaskType) {
        updateSelected { layer ->
            if (type == MaskType.BRUSH && layer.maskBitmap == null)
                layer.copy(maskType = type, maskBitmap = newMaskBitmap())
            else layer.copy(maskType = type)
        }
        if (type != MaskType.BRUSH) maskEditing = false
    }
    fun setMaskCenter(x: Float, y: Float) = updateSelected { it.copy(maskCx = x, maskCy = y) }
    fun setMaskSize(v: Float) = updateSelected { it.copy(maskSize = v) }
    fun setMaskFeather(v: Float) = updateSelected { it.copy(maskFeather = v) }
    fun setMaskAngle(v: Float) = updateSelected { it.copy(maskAngle = v) }
    fun toggleMaskInvert() = updateSelected { it.copy(maskInvert = !it.maskInvert) }

    // --- brush masking ------------------------------------------------------

    fun setBrushSize(v: Float) = updateSelected { it.copy(brushSize = v) }
    fun setBrushHardness(v: Float) = updateSelected { it.copy(brushHardness = v) }
    fun toggleBrushErase() = updateSelected { it.copy(brushErase = !it.brushErase) }
    fun toggleMaskEditing() { maskEditing = !maskEditing }

    /** Paints a stroke (in 0..1 image space) onto the selected layer's mask. */
    fun paintMaskStroke(x0: Float, y0: Float, x1: Float, y1: Float) {
        val i = selectedIndex
        val layer = layers.getOrNull(i) ?: return
        if (layer.maskType != MaskType.BRUSH) return
        val bmp = layer.maskBitmap ?: newMaskBitmap().also {
            layers[i] = layer.copy(maskBitmap = it)
        }
        val target = layers[i].maskBitmap ?: return
        val canvas = Canvas(target)
        val minDim = minOf(target.width, target.height).toFloat()
        val radius = (layer.brushSize * minDim * 0.5f).coerceAtLeast(1f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (layer.brushErase) Color.BLACK else Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = radius * 2f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            val blur = (1f - layer.brushHardness) * radius
            if (blur > 0.5f) maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
        }
        val w = target.width
        val h = target.height
        canvas.drawLine(x0 * w, y0 * h, x1 * w, y1 * h, paint)
        layers[i] = layers[i].copy(maskRevision = layers[i].maskRevision + 1)
        touch()
    }

    fun fillMask(white: Boolean) {
        val i = selectedIndex
        val layer = layers.getOrNull(i) ?: return
        if (layer.maskType != MaskType.BRUSH) return
        val target = layer.maskBitmap ?: newMaskBitmap()
        target.eraseColor(if (white) Color.WHITE else Color.BLACK)
        layers[i] = layer.copy(maskBitmap = target, maskRevision = layer.maskRevision + 1)
        touch()
    }

    private fun newMaskBitmap(): Bitmap {
        val long = MASK_RES
        val (w, h) = if (imageW >= imageH) long to (long * imageH / imageW).coerceAtLeast(1)
        else (long * imageW / imageH).coerceAtLeast(1) to long
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLACK) }
    }

    private inline fun updateSelected(transform: (LayerUiState) -> LayerUiState) {
        val i = selectedIndex
        val layer = layers.getOrNull(i) ?: return
        layers[i] = transform(layer)
        touch()
    }

    private fun makeLayer(effect: Effect) =
        LayerUiState(nextId++, effect, effect.params.map { it.default })

    private fun defaultNewEffect(): Effect =
        EffectRegistry.inCategory(Categories.GLITCH).firstOrNull() ?: EffectRegistry.original

    private fun touch() { revision++ }

    // --- export -------------------------------------------------------------

    fun exportStill(context: Context, view: GlPhotoView, format: ExportFormat, quality: Int) {
        exportStatus = ExportStatus.Working
        view.captureStill(MAX_STILL) { bmp ->
            viewModelScope.launch(Dispatchers.IO) {
                val uri = bmp?.let { MediaSaver.saveImage(context, it, format, quality) }
                exportStatus = if (uri != null) ExportStatus.Done(uri, animated = false) else ExportStatus.Error
            }
        }
    }

    fun exportGif(context: Context, view: GlPhotoView, frames: Int, durationSec: Float, loop: Boolean) {
        exportStatus = ExportStatus.Working
        view.captureFrames(MAX_GIF, frames, durationSec) { list ->
            viewModelScope.launch(Dispatchers.Default) {
                if (list.isEmpty()) {
                    exportStatus = ExportStatus.Error
                    return@launch
                }
                val delay = (durationSec * 1000f / frames).toInt().coerceAtLeast(20)
                // repeat: 0 = loop forever, 1 = play once.
                val bytes = GifEncoder.encode(list, delayMs = delay, repeat = if (loop) 0 else 1)
                list.forEach { it.recycle() }
                val uri = MediaSaver.saveBytes(context, bytes, ExportFormat.GIF)
                exportStatus = if (uri != null) ExportStatus.Done(uri, animated = true) else ExportStatus.Error
            }
        }
    }

    fun clearExportStatus() { exportStatus = null }

    companion object {
        const val MAX_STILL = 4096
        const val MAX_GIF = 480
        const val MASK_RES = 1024
    }
}
