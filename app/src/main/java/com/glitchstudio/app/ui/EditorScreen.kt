package com.glitchstudio.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glitchstudio.app.effects.EffectCategory
import com.glitchstudio.app.effects.EffectRepository
import com.glitchstudio.app.effects.ParamRole
import com.glitchstudio.app.effects.ShaderEffect
import com.glitchstudio.app.export.ExportFormat
import com.glitchstudio.app.gl.GLPreviewView
import com.glitchstudio.app.ui.components.CircleIconButton
import com.glitchstudio.app.ui.components.GlitchSlider
import com.glitchstudio.app.ui.components.SegmentedControl
import com.glitchstudio.app.ui.theme.GlitchTheme
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.roundToInt

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onPickNewImage: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val preview by viewModel.preview.collectAsState()
    val context = LocalContext.current
    var showExport by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(GlitchTheme.colors.backdrop)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(Modifier.fillMaxSize()) {
            EditorTopBar(
                effectName = state.effect.name,
                onBack = onBack,
                onCompareChanged = viewModel::setCompare,
                onExport = { showExport = true },
            )
            EffectCanvas(
                preview = preview,
                effect = state.effect,
                values = state.values,
                compare = state.compare,
                onSetValue = viewModel::setValue,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
            ToolPanel(
                effect = state.effect,
                values = state.values,
                category = state.category,
                onSetValue = viewModel::setValue,
                onResetParam = { i -> viewModel.setValue(i, state.effect.params[i].default) },
                onResetAll = viewModel::resetValues,
                onSelectCategory = viewModel::selectCategory,
                onSelectEffect = viewModel::selectEffect,
            )
        }

        if (state.isExporting) {
            ExportOverlay(progress = state.exportProgress, isAnimated = state.exportFormat.animated)
        }
    }

    if (showExport) {
        ExportSheet(
            state = state,
            onDismiss = { showExport = false },
            onFormat = viewModel::setExportFormat,
            onQuality = viewModel::setQuality,
            onFrames = viewModel::setGifFrames,
            onFps = viewModel::setGifFps,
            onSave = { viewModel.export(share = false) {} },
            onShare = {
                viewModel.export(share = true) { uri ->
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = state.exportFormat.mime
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share"))
                }
            },
        )
    }
}

// --- Top bar -----------------------------------------------------------------

@Composable
private fun EditorTopBar(
    effectName: String,
    onBack: () -> Unit,
    onCompareChanged: (Boolean) -> Unit,
    onExport: () -> Unit,
) {
    val colors = GlitchTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
            Spacer(Modifier.weight(1f))
            // Press and hold to compare with the original.
            CompareButton(onCompareChanged)
            Spacer(Modifier.width(10.dp))
            CircleIconButton(
                Icons.Rounded.FileDownload, "Export", onExport,
                background = colors.accent, tint = Color.White,
            )
        }
        Text(
            effectName,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textHigh,
        )
    }
}

@Composable
private fun CompareButton(onCompareChanged: (Boolean) -> Unit) {
    val colors = GlitchTheme.colors
    Box(
        Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(colors.panel)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    onCompareChanged(true)
                    waitForUpOrCancellation()
                    onCompareChanged(false)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Visibility, "Hold to compare", tint = colors.textHigh, modifier = Modifier.size(20.dp))
    }
}

// --- Canvas with on-image finger control -------------------------------------

@Composable
private fun EffectCanvas(
    preview: Bitmap?,
    effect: ShaderEffect,
    values: List<Float>,
    compare: Boolean,
    onSetValue: (Int, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GlitchTheme.colors
    Box(modifier.padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
        if (preview == null) {
            CircularProgressIndicator(color = colors.accent, modifier = Modifier.size(28.dp))
            return@Box
        }
        val aspect = remember(preview) { preview.width.toFloat() / preview.height.toFloat() }
        val lastBmp = remember { mutableStateOf<Bitmap?>(null) }
        val lastEffectId = remember { mutableStateOf<String?>(null) }
        val latest = rememberUpdatedState(EffectCanvasState(effect, values))

        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
                .clip(RoundedCornerShape(8.dp)),
        ) {
            androidx.compose.ui.viewinterop.AndroidView(
                modifier = Modifier.matchParentSize(),
                factory = { ctx -> GLPreviewView(ctx) },
                update = { view ->
                    if (preview !== lastBmp.value) {
                        view.setImage(preview)
                        lastBmp.value = preview
                    }
                    if (effect.id != lastEffectId.value) {
                        view.setEffect(effect)
                        lastEffectId.value = effect.id
                    }
                    view.setValues(values.toFloatArray())
                    view.setBypass(compare)
                },
            )

            // Gesture + handle overlay layer.
            Canvas(
                Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            val cur = latest.value
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            val h = size.height.toFloat().coerceAtLeast(1f)
                            applyCanvasDrag(cur, change.position.x, change.position.y, drag.x, drag.y, w, h, onSetValue)
                        }
                    },
            ) {
                if (compare) return@Canvas
                drawCanvasHandles(effect, values, colors.accent)
            }
        }
    }
}

private data class EffectCanvasState(val effect: ShaderEffect, val values: List<Float>)

private fun applyCanvasDrag(
    cur: EffectCanvasState,
    px: Float,
    py: Float,
    dx: Float,
    dy: Float,
    w: Float,
    h: Float,
    onSetValue: (Int, Float) -> Unit,
) {
    val params = cur.effect.params
    val posX = params.indexOfFirst { it.role == ParamRole.POS_X }
    val posY = params.indexOfFirst { it.role == ParamRole.POS_Y }
    val angleIdx = params.indexOfFirst { it.role == ParamRole.ANGLE }
    when {
        posX >= 0 && posY >= 0 -> {
            onSetValue(posX, mapFraction(params[posX], (px / w).coerceIn(0f, 1f)))
            onSetValue(posY, mapFraction(params[posY], (py / h).coerceIn(0f, 1f)))
        }
        angleIdx >= 0 -> {
            var ang = atan2((py - h / 2f), (px - w / 2f)) / (2f * PI.toFloat())
            ang -= kotlin.math.floor(ang)
            onSetValue(angleIdx, mapFraction(params[angleIdx], ang))
        }
        else -> {
            if (params.isNotEmpty()) {
                val p = params[0]
                val v = cur.values.getOrElse(0) { p.default } + dx / w * (p.max - p.min)
                onSetValue(0, v.coerceIn(p.min, p.max))
            }
            if (params.size > 1) {
                val p = params[1]
                val v = cur.values.getOrElse(1) { p.default } - dy / h * (p.max - p.min)
                onSetValue(1, v.coerceIn(p.min, p.max))
            }
        }
    }
}

private fun mapFraction(p: com.glitchstudio.app.effects.ShaderParam, frac: Float): Float =
    p.min + frac.coerceIn(0f, 1f) * (p.max - p.min)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCanvasHandles(
    effect: ShaderEffect,
    values: List<Float>,
    accent: Color,
) {
    val params = effect.params
    val posX = params.indexOfFirst { it.role == ParamRole.POS_X }
    val posY = params.indexOfFirst { it.role == ParamRole.POS_Y }
    val angleIdx = params.indexOfFirst { it.role == ParamRole.ANGLE }
    if (posX >= 0 && posY >= 0) {
        val fx = fractionOf(params[posX], values.getOrElse(posX) { params[posX].default }) * size.width
        val fy = fractionOf(params[posY], values.getOrElse(posY) { params[posY].default }) * size.height
        val c = Offset(fx, fy)
        drawCircle(Color.White, radius = 13.dp.toPx(), center = c, style = Stroke(2.5.dp.toPx()))
        drawCircle(accent, radius = 4.dp.toPx(), center = c)
    } else if (angleIdx >= 0) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val frac = fractionOf(params[angleIdx], values.getOrElse(angleIdx) { params[angleIdx].default })
        val a = frac * 2f * PI.toFloat()
        val r = size.minDimension * 0.3f
        val tip = Offset(center.x + kotlin.math.cos(a) * r, center.y + kotlin.math.sin(a) * r)
        drawLine(Color.White, center, tip, strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(accent, radius = 5.dp.toPx(), center = tip)
        drawCircle(Color.White, radius = 3.dp.toPx(), center = center)
    }
}

private fun fractionOf(p: com.glitchstudio.app.effects.ShaderParam, v: Float): Float {
    val span = (p.max - p.min)
    return if (span <= 0f) 0f else ((v - p.min) / span).coerceIn(0f, 1f)
}

// --- Tool panel: advanced settings + effect browser --------------------------

@Composable
private fun ToolPanel(
    effect: ShaderEffect,
    values: List<Float>,
    category: EffectCategory,
    onSetValue: (Int, Float) -> Unit,
    onResetParam: (Int) -> Unit,
    onResetAll: () -> Unit,
    onSelectCategory: (EffectCategory) -> Unit,
    onSelectEffect: (ShaderEffect) -> Unit,
) {
    val colors = GlitchTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(colors.panel)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(top = 14.dp, bottom = 10.dp),
    ) {
        // Header: effect name + reset
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(effect.name, style = MaterialTheme.typography.titleMedium, color = colors.textHigh)
                Text(
                    effect.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textLow,
                    maxLines = 1,
                )
            }
            if (effect.params.isNotEmpty()) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickableNoRipple { onResetAll() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Refresh, "Reset", tint = colors.textMed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reset", style = MaterialTheme.typography.labelLarge, color = colors.textMed)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Advanced settings (sliders) for the active effect.
        if (effect.params.isEmpty()) {
            Text(
                "This effect has no parameters.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textLow,
                modifier = Modifier.padding(horizontal = 18.dp),
            )
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                effect.params.forEachIndexed { i, p ->
                    GlitchSlider(
                        label = p.name,
                        value = values.getOrElse(i) { p.default },
                        valueRange = p.min..p.max,
                        onValueChange = { onSetValue(i, it) },
                        bipolar = p.bipolar,
                        kind = p.kind,
                        onReset = { onResetParam(i) },
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        CategoryRow(selected = category, onSelect = onSelectCategory)
        Spacer(Modifier.height(12.dp))
        EffectRow(category = category, selected = effect, onSelect = onSelectEffect)
    }
}

@Composable
private fun CategoryRow(selected: EffectCategory, onSelect: (EffectCategory) -> Unit) {
    val colors = GlitchTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        EffectRepository.categories.forEach { cat ->
            val isSel = cat == selected
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickableNoRipple { onSelect(cat) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    cat.label.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSel) colors.textHigh else colors.textLow,
                    fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun EffectRow(
    category: EffectCategory,
    selected: ShaderEffect,
    onSelect: (ShaderEffect) -> Unit,
) {
    val colors = GlitchTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EffectRepository.inCategory(category).forEach { fx ->
            val isSel = fx.id == selected.id
            Box(
                Modifier
                    .width(84.dp)
                    .height(58.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSel) colors.panelElevated else colors.panelPressed)
                    .then(
                        if (isSel) Modifier.border(1.5.dp, colors.accent, RoundedCornerShape(12.dp))
                        else Modifier
                    )
                    .clickableNoRipple { onSelect(fx) }
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    fx.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSel) colors.textHigh else colors.textMed,
                    maxLines = 2,
                )
            }
        }
    }
}

// --- Export ------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportSheet(
    state: EditorViewModel.State,
    onDismiss: () -> Unit,
    onFormat: (ExportFormat) -> Unit,
    onQuality: (Int) -> Unit,
    onFrames: (Int) -> Unit,
    onFps: (Int) -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    val colors = GlitchTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.panel,
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Export", style = MaterialTheme.typography.titleLarge, color = colors.textHigh)
                Spacer(Modifier.weight(1f))
                CircleIconButton(Icons.Rounded.Close, "Close", onDismiss, size = 36.dp)
            }
            Spacer(Modifier.height(18.dp))

            Text("FORMAT", style = MaterialTheme.typography.labelSmall, color = colors.textLow)
            Spacer(Modifier.height(8.dp))
            val formats = ExportFormat.entries
            SegmentedControl(
                options = formats.map { it.label },
                selectedIndex = formats.indexOf(state.exportFormat),
                onSelect = { onFormat(formats[it]) },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))

            if (state.exportFormat.animated) {
                GlitchSlider(
                    label = "Frames",
                    value = state.gifFrames.toFloat(),
                    valueRange = 4f..60f,
                    onValueChange = { onFrames(it.roundToInt()) },
                    valueText = state.gifFrames.toString(),
                )
                Spacer(Modifier.height(16.dp))
                GlitchSlider(
                    label = "Frame rate",
                    value = state.gifFps.toFloat(),
                    valueRange = 4f..30f,
                    onValueChange = { onFps(it.roundToInt()) },
                    valueText = "${state.gifFps} fps",
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Animated effects loop; still effects export a single frame.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textLow,
                )
            } else if (state.exportFormat.hasQuality) {
                GlitchSlider(
                    label = "Quality",
                    value = state.quality.toFloat(),
                    valueRange = 10f..100f,
                    onValueChange = { onQuality(it.roundToInt()) },
                    valueText = state.quality.toString(),
                )
            } else {
                Text(
                    "PNG exports are lossless at full resolution.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textLow,
                )
            }

            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExportButton("Save", Icons.Rounded.FileDownload, filled = true, modifier = Modifier.weight(1f)) {
                    onSave(); onDismiss()
                }
                ExportButton("Share", Icons.Rounded.Share, filled = false, modifier = Modifier.weight(1f)) {
                    onShare(); onDismiss()
                }
            }
        }
    }
}

@Composable
private fun ExportButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    filled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = GlitchTheme.colors
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (filled) colors.accent else colors.panelElevated)
            .clickableNoRipple { onClick() }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon, null,
            tint = if (filled) Color.White else colors.textHigh,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = if (filled) Color.White else colors.textHigh,
        )
    }
}

@Composable
private fun ExportOverlay(progress: Float, isAnimated: Boolean) {
    val colors = GlitchTheme.colors
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = colors.accent, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                if (isAnimated) "Rendering GIF ${(progress * 100).roundToInt()}%" else "Exporting…",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
        }
    }
}
