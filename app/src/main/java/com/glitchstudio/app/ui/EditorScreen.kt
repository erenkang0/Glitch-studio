package com.glitchstudio.app.ui

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.glitchstudio.app.effects.Categories
import com.glitchstudio.app.effects.Effect
import com.glitchstudio.app.effects.EffectRegistry
import com.glitchstudio.app.export.ExportFormat
import com.glitchstudio.app.gl.GlPhotoView
import com.glitchstudio.app.gl.MaskType
import com.glitchstudio.app.ui.components.GhostIconButton
import com.glitchstudio.app.ui.components.GlitchChip
import com.glitchstudio.app.ui.components.GradientButton
import com.glitchstudio.app.ui.components.LabeledSlider
import com.glitchstudio.app.ui.components.SegmentedTabs
import com.glitchstudio.app.ui.theme.GlitchColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val RAIL_HEIGHT = 70.dp

@Composable
fun EditorScreen(vm: EditorViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var glView by remember { mutableStateOf<GlPhotoView?>(null) }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) scope.launch {
            ImageLoader.load(context, uri)?.let { bmp ->
                imageBitmap = bmp
                glView?.setImage(bmp)
                vm.onImageLoaded(bmp.width, bmp.height)
            }
        }
    }
    fun pick() = picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    LaunchedEffect(glView) {
        val v = glView ?: return@LaunchedEffect
        snapshotFlow { vm.revision }.collect { v.setLayers(vm.renderLayers()) }
    }
    LaunchedEffect(glView) {
        val v = glView ?: return@LaunchedEffect
        snapshotFlow { vm.continuousRender }.collect { v.setContinuous(it) }
    }
    LaunchedEffect(glView) {
        val v = glView ?: return@LaunchedEffect
        snapshotFlow { Triple(vm.viewScale, vm.viewPanX, vm.viewPanY) }
            .collect { v.setViewTransform(it.first, it.second, it.third) }
    }
    LaunchedEffect(glView, imageBitmap) {
        imageBitmap?.let {
            glView?.setImage(it)
            glView?.setLayers(vm.renderLayers())
            glView?.setContinuous(vm.continuousRender)
        }
    }
    // Generate the missing thumbnails for the visible category.
    LaunchedEffect(glView, imageBitmap, vm.category) {
        val v = glView ?: return@LaunchedEffect
        if (imageBitmap == null) return@LaunchedEffect
        val missing = EffectRegistry.inCategory(vm.category).filter { it.id !in vm.thumbnails }
        if (missing.isNotEmpty()) v.renderThumbnails(missing, 160) { id, b -> vm.putThumbnail(id, b) }
    }

    Box(Modifier.fillMaxSize().background(GlitchColors.background)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            TopBar(
                hasImage = vm.hasImage,
                showPlay = vm.hasAnimatedLayer,
                playing = vm.playing,
                onPlayToggle = { vm.togglePlaying() },
                onImport = { pick() },
                onExport = { vm.showExport = true }
            )

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (vm.hasImage) {
                    CanvasWithPanel(vm, imageBitmap, glView, onReady = { glView = it })
                } else {
                    EmptyState(onImport = { pick() })
                }
            }

            if (vm.hasImage) CategoryRail(vm)
        }

        AnimatedVisibility(
            visible = vm.showExport,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180))
        ) {
            ExportSheet(vm, glView)
        }
    }
}

// --- canvas ----------------------------------------------------------------

/** Wraps the canvas and the slide-up tool panel in a dedicated Box so the panel's
 *  AnimatedVisibility resolves unambiguously (no enclosing ColumnScope). */
@Composable
private fun CanvasWithPanel(
    vm: EditorViewModel,
    imageBitmap: Bitmap?,
    glView: GlPhotoView?,
    onReady: (GlPhotoView) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        CanvasArea(vm, imageBitmap, glView, onReady)
        AnimatedVisibility(
            visible = vm.panelOpen,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(),
            exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(200))
        ) {
            PanelCard(vm, glView, imageBitmap)
        }
    }
}

@Composable
private fun CanvasArea(
    vm: EditorViewModel,
    imageBitmap: Bitmap?,
    glView: GlPhotoView?,
    onReady: (GlPhotoView) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = rememberHaptics()
    var view by remember { mutableStateOf<GlPhotoView?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    DisposableEffect(lifecycleOwner, view) {
        val v = view
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> v?.onResume()
                Lifecycle.Event.ON_PAUSE -> v?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val painting = vm.maskEditing && vm.selected?.maskType == MaskType.BRUSH && imageBitmap != null

    Box(Modifier.fillMaxSize().padding(8.dp)) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .onSizeChanged { canvasSize = it }
                .then(
                    if (painting) Modifier
                    else Modifier
                        .pointerInput(canvasSize) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                vm.applyTransform(zoom, pan.x, pan.y, canvasSize.width.toFloat(), canvasSize.height.toFloat())
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(onDoubleTap = { vm.resetZoom() })
                        }
                ),
            factory = { ctx -> GlPhotoView(ctx).also { view = it; onReady(it) } }
        )

        if (painting) {
            val imgW = imageBitmap!!.width.toFloat()
            val imgH = imageBitmap.height.toFloat()
            Box(
                Modifier.fillMaxSize().pointerInput(vm.selected?.id, canvasSize, imgW, imgH) {
                    var last = androidx.compose.ui.geometry.Offset.Zero
                    detectDragGestures(
                        onDragStart = { pos ->
                            last = pos
                            val uv = toUv(pos, canvasSize, imgW, imgH)
                            vm.paintMaskStroke(uv.x, uv.y, uv.x, uv.y)
                        },
                        onDrag = { change, _ ->
                            val a = toUv(last, canvasSize, imgW, imgH)
                            val b = toUv(change.position, canvasSize, imgW, imgH)
                            vm.paintMaskStroke(a.x, a.y, b.x, b.y)
                            last = change.position
                        }
                    )
                }
            )
        }

        Box(Modifier.fillMaxSize().border(1.dp, if (painting) GlitchColors.accent else GlitchColors.border, RoundedCornerShape(20.dp)))

        // Zoom indicator
        if (vm.viewScale > 1.01f && !painting) {
            Pill(Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                Text("${vm.viewScale.roundToInt()}x", color = GlitchColors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (painting) {
            Pill(Modifier.align(Alignment.BottomCenter).padding(10.dp), border = GlitchColors.accent) {
                Text(
                    if (vm.selected?.brushErase == true) "Erasing — drag on photo" else "Painting — drag on photo",
                    color = GlitchColors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                )
            }
        } else if (!vm.panelOpen) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xCC15151D))
                    .border(1.dp, GlitchColors.border, RoundedCornerShape(14.dp))
                    .pointerInput(glView) {
                        detectTapGestures(onPress = {
                            haptics.click(); glView?.setBypass(true); tryAwaitRelease(); glView?.setBypass(false)
                        })
                    }
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Visibility, null, tint = GlitchColors.textSecondary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Hold: original", color = GlitchColors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun Pill(modifier: Modifier = Modifier, border: Color = GlitchColors.border, content: @Composable () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC15151D))
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) { content() }
}

private fun toUv(
    pos: androidx.compose.ui.geometry.Offset,
    size: IntSize,
    imgW: Float,
    imgH: Float
): androidx.compose.ui.geometry.Offset {
    if (size.width == 0 || size.height == 0) return androidx.compose.ui.geometry.Offset(0.5f, 0.5f)
    val sw = size.width.toFloat(); val sh = size.height.toFloat()
    val imgA = imgW / imgH; val viewA = sw / sh
    val rw: Float; val rh: Float
    if (imgA > viewA) { rw = sw; rh = sw / imgA } else { rh = sh; rw = sh * imgA }
    val ox = (sw - rw) / 2f; val oy = (sh - rh) / 2f
    return androidx.compose.ui.geometry.Offset(
        ((pos.x - ox) / rw).coerceIn(0f, 1f),
        ((pos.y - oy) / rh).coerceIn(0f, 1f)
    )
}

// --- top bar + rail --------------------------------------------------------

@Composable
private fun TopBar(
    hasImage: Boolean,
    showPlay: Boolean,
    playing: Boolean,
    onPlayToggle: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(GlitchColors.brand))
        Spacer(Modifier.width(9.dp))
        Column {
            Text("Glitch Studio", color = GlitchColors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("${EffectRegistry.creativeCount} effects", color = GlitchColors.textMuted, fontSize = 10.sp)
        }
        Spacer(Modifier.weight(1f))
        if (showPlay) {
            GhostIconButton(
                if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play", onPlayToggle, active = playing
            )
            Spacer(Modifier.width(8.dp))
        }
        GhostIconButton(Icons.Rounded.Image, "Import", onImport)
        if (hasImage) {
            Spacer(Modifier.width(8.dp))
            GradientButton("Export", Icons.Rounded.IosShare, onExport)
        }
    }
}

@Composable
private fun CategoryRail(vm: EditorViewModel) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(RAIL_HEIGHT)
            .background(GlitchColors.surface)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PanelTab.entries.forEach { tab ->
            val active = vm.panelOpen && vm.panelTab == tab
            Box(Modifier.weight(1f)) {
                GlitchChip(
                    label = tab.label,
                    selected = active,
                    onClick = { vm.togglePanel(tab) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onImport: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(100.dp).clip(RoundedCornerShape(34.dp)).background(GlitchColors.surfaceHigh)
                .border(1.dp, GlitchColors.border, RoundedCornerShape(34.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.AutoAwesome, null, tint = GlitchColors.accent, modifier = Modifier.size(42.dp)) }
        Spacer(Modifier.height(24.dp))
        Text("Create something glitchy", color = GlitchColors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Import a photo and stack ${EffectRegistry.creativeCount} shader effects with layers, masks, zoom and animation.",
            color = GlitchColors.textSecondary, fontSize = 14.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        GradientButton("Import a photo", Icons.Rounded.Image, onImport)
    }
}

// --- panel -----------------------------------------------------------------

@Composable
private fun PanelCard(vm: EditorViewModel, glView: GlPhotoView?, imageBitmap: Bitmap?) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(GlitchColors.surface)
            .border(1.dp, GlitchColors.border, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(vm.panelTab.label, color = GlitchColors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            GhostIconButton(Icons.Rounded.KeyboardArrowDown, "Close", onClick = { vm.closePanel() })
        }
        Spacer(Modifier.height(10.dp))
        AnimatedContent(
            targetState = vm.panelTab,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "panel"
        ) { tab ->
            Box(Modifier.heightIn(min = 220.dp, max = 300.dp)) {
                when (tab) {
                    PanelTab.EFFECTS -> EffectsPanel(vm, imageBitmap)
                    PanelTab.ADJUST -> AdjustPanel(vm)
                    PanelTab.LAYERS -> LayersPanel(vm)
                    PanelTab.MASK -> MaskPanel(vm)
                }
            }
        }
    }
}

@Composable
private fun EffectsPanel(vm: EditorViewModel, imageBitmap: Bitmap?) {
    Column {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
            items(Categories.ALL) { cat ->
                GlitchChip(cat, vm.category == cat, onClick = { vm.category = cat })
            }
        }
        Spacer(Modifier.height(12.dp))
        val effects = EffectRegistry.inCategory(vm.category)
        val activeId = vm.selected?.effect?.id
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                EffectCard(
                    name = "None", category = "Reset", thumb = imageBitmap, animated = false,
                    selected = activeId == EffectRegistry.original.id,
                    onClick = { vm.setEffectForSelected(EffectRegistry.original) }
                )
            }
            items(effects) { effect ->
                EffectCard(
                    name = effect.name, category = effect.category, thumb = vm.thumbnails[effect.id],
                    animated = effect.animated, selected = effect.id == activeId,
                    onClick = { vm.setEffectForSelected(effect) }
                )
            }
        }
    }
}

@Composable
private fun EffectCard(
    name: String, category: String, thumb: Bitmap?, animated: Boolean, selected: Boolean, onClick: () -> Unit
) {
    val haptics = rememberHaptics()
    Column(
        Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) GlitchColors.accentSoft else GlitchColors.surfaceHigh)
            .border(1.dp, if (selected) GlitchColors.accent else GlitchColors.border, RoundedCornerShape(16.dp))
            .clickable { haptics.click(); onClick() }
            .padding(8.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(12.dp)).background(GlitchColors.background)) {
            if (thumb != null) {
                androidx.compose.foundation.Image(
                    thumb.asImageBitmap(), null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                )
            } else {
                Box(Modifier.fillMaxSize().background(GlitchColors.brand))
            }
            if (animated) {
                Box(Modifier.align(Alignment.TopEnd).padding(4.dp).size(16.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xAA000000)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(10.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(name, color = GlitchColors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(category, color = GlitchColors.textMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AdjustPanel(vm: EditorViewModel) {
    val layer = vm.selected ?: run { PanelHint("Select a layer to adjust."); return }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(layer.effect.name, color = GlitchColors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            ResetChip { vm.resetSelectedParams() }
        }
        Spacer(Modifier.height(6.dp))
        LabeledSlider("Opacity", layer.opacity, 0f, 1f, onChange = { vm.setOpacity(it) })
        if (layer.effect.params.isEmpty()) {
            PanelHint("This effect has no adjustable parameters.")
        } else {
            layer.effect.params.forEachIndexed { index, p ->
                LabeledSlider(p.name, layer.params.getOrElse(index) { p.default }, p.min, p.max, onChange = { vm.updateParam(index, it) })
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ResetChip(onClick: () -> Unit) {
    val haptics = rememberHaptics()
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).background(GlitchColors.surfaceHigh)
            .border(1.dp, GlitchColors.border, RoundedCornerShape(10.dp))
            .clickable { haptics.click(); onClick() }.padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Refresh, null, tint = GlitchColors.textSecondary, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text("Reset", color = GlitchColors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LayersPanel(vm: EditorViewModel) {
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Layer stack", color = GlitchColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            GradientButton("Add", Icons.Rounded.Add, onClick = { vm.addLayer() })
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(vm.layers) { index, layer ->
                LayerRow(
                    index = index, name = layer.effect.name, enabled = layer.enabled,
                    selected = index == vm.selectedIndex, canRemove = vm.layers.size > 1,
                    onSelect = { vm.selectLayer(index) }, onToggle = { vm.toggleEnabled(index) },
                    onUp = { if (index < vm.layers.lastIndex) vm.moveLayer(index, index + 1) },
                    onDown = { if (index > 0) vm.moveLayer(index, index - 1) },
                    onRemove = { vm.removeLayer(index) }
                )
            }
        }
    }
}

@Composable
private fun LayerRow(
    index: Int, name: String, enabled: Boolean, selected: Boolean, canRemove: Boolean,
    onSelect: () -> Unit, onToggle: () -> Unit, onUp: () -> Unit, onDown: () -> Unit, onRemove: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (selected) GlitchColors.accentSoft else GlitchColors.surfaceHigh)
            .border(1.dp, if (selected) GlitchColors.accent else GlitchColors.border, RoundedCornerShape(14.dp))
            .clickable { onSelect() }.padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SmallIcon(if (enabled) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff, onToggle,
            tint = if (enabled) GlitchColors.textPrimary else GlitchColors.textMuted)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = GlitchColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Layer ${index + 1}", color = GlitchColors.textMuted, fontSize = 10.sp)
        }
        SmallIcon(Icons.Rounded.KeyboardArrowUp, onUp, tint = GlitchColors.textSecondary)
        SmallIcon(Icons.Rounded.KeyboardArrowDown, onDown, tint = GlitchColors.textSecondary)
        if (canRemove) SmallIcon(Icons.Rounded.Delete, onRemove, tint = GlitchColors.danger)
    }
}

@Composable
private fun SmallIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, tint: Color) {
    val haptics = rememberHaptics()
    Box(Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).clickable { haptics.click(); onClick() }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun MaskPanel(vm: EditorViewModel) {
    val layer = vm.selected ?: run { PanelHint("Select a layer to mask."); return }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        SegmentedTabs(
            items = listOf("None", "Radial", "Linear", "Brush"),
            selectedIndex = layer.maskType.ordinal,
            onSelect = { vm.setMaskType(MaskType.entries[it]) }
        )
        Spacer(Modifier.height(10.dp))
        when (layer.maskType) {
            MaskType.NONE -> PanelHint("The effect applies to the whole image. Pick a shape or brush to blend it locally.")
            MaskType.BRUSH -> {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    GradientButton(if (vm.maskEditing) "Done" else "Paint", Icons.Rounded.Brush, onClick = { vm.toggleMaskEditing() })
                    GlitchChip(if (layer.brushErase) "Erase" else "Draw", layer.brushErase, onClick = { vm.toggleBrushErase() })
                    Spacer(Modifier.weight(1f))
                    GlitchChip("Fill", false, onClick = { vm.fillMask(true) })
                    GlitchChip("Clear", false, onClick = { vm.fillMask(false) })
                }
                Spacer(Modifier.height(8.dp))
                LabeledSlider("Brush Size", layer.brushSize, 0.02f, 0.4f, onChange = { vm.setBrushSize(it) })
                LabeledSlider("Hardness", layer.brushHardness, 0f, 1f, onChange = { vm.setBrushHardness(it) })
                MaskInvertRow(layer.maskInvert) { vm.toggleMaskInvert() }
                Spacer(Modifier.height(12.dp))
            }
            else -> {
                LabeledSlider("Center X", layer.maskCx, 0f, 1f, onChange = { vm.setMaskCenter(it, layer.maskCy) })
                LabeledSlider("Center Y", layer.maskCy, 0f, 1f, onChange = { vm.setMaskCenter(layer.maskCx, it) })
                if (layer.maskType == MaskType.RADIAL) LabeledSlider("Size", layer.maskSize, 0.05f, 1f, onChange = { vm.setMaskSize(it) })
                LabeledSlider("Feather", layer.maskFeather, 0.001f, 0.6f, onChange = { vm.setMaskFeather(it) })
                if (layer.maskType == MaskType.LINEAR) LabeledSlider("Angle", layer.maskAngle, 0f, 6.2831f, onChange = { vm.setMaskAngle(it) })
                MaskInvertRow(layer.maskInvert) { vm.toggleMaskInvert() }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun MaskInvertRow(inverted: Boolean, onToggle: () -> Unit) {
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Invert mask", color = GlitchColors.textSecondary, fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
        GlitchChip(if (inverted) "On" else "Off", inverted, onClick = onToggle)
    }
}

@Composable
private fun PanelHint(text: String) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, color = GlitchColors.textMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

// --- export ----------------------------------------------------------------

@Composable
private fun ExportSheet(vm: EditorViewModel, glView: GlPhotoView?) {
    val context = LocalContext.current
    var format by remember { mutableStateOf(ExportFormat.PNG) }
    var quality by remember { mutableStateOf(95f) }
    var frames by remember { mutableStateOf(24f) }
    var duration by remember { mutableStateOf(2f) }
    var loop by remember { mutableStateOf(true) }
    val status = vm.exportStatus

    Box(
        Modifier.fillMaxSize().background(Color(0xCC050507)).clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null
        ) { if (status !is ExportStatus.Working) { vm.showExport = false; vm.clearExportStatus() } },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(GlitchColors.surface).border(1.dp, GlitchColors.border, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .windowInsetsPadding(WindowInsets.safeDrawing).padding(20.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Export", color = GlitchColors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                GhostIconButton(Icons.Rounded.Close, "Close", onClick = {
                    if (status !is ExportStatus.Working) { vm.showExport = false; vm.clearExportStatus() }
                })
            }
            Spacer(Modifier.height(16.dp))
            Text("Format", color = GlitchColors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExportFormat.entries.forEach { f -> GlitchChip(f.label, format == f, onClick = { format = f }) }
            }
            Spacer(Modifier.height(14.dp))
            if (format == ExportFormat.GIF) {
                if (!vm.hasAnimatedLayer) PanelHint("Add an animated effect (marked with a spark) for a moving GIF.")
                LabeledSlider("Frames", frames, 8f, 48f, onChange = { frames = it }, valueText = frames.roundToInt().toString())
                LabeledSlider("Duration (s)", duration, 0.5f, 4f, onChange = { duration = it })
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Loop forever", color = GlitchColors.textSecondary, fontSize = 13.sp)
                    Spacer(Modifier.weight(1f))
                    GlitchChip(if (loop) "On" else "Off", loop, onClick = { loop = !loop })
                }
            } else if (format != ExportFormat.PNG) {
                LabeledSlider("Quality", quality, 50f, 100f, onChange = { quality = it }, valueText = quality.roundToInt().toString())
            }
            Spacer(Modifier.height(18.dp))
            when (status) {
                ExportStatus.Working -> StatusRow(true, "Rendering…")
                is ExportStatus.Done -> StatusRow(false, if (status.animated) "GIF saved to Gallery" else "Saved to Gallery")
                ExportStatus.Error -> StatusRow(false, "Could not save. Try again.")
                null -> Unit
            }
            if (status != null) Spacer(Modifier.height(12.dp))
            GradientButton(
                text = if (status is ExportStatus.Done) "Done" else "Save to Gallery",
                icon = if (status is ExportStatus.Done) Icons.Rounded.Check else Icons.Rounded.IosShare,
                onClick = {
                    val v = glView ?: return@GradientButton
                    when (status) {
                        ExportStatus.Working -> Unit
                        is ExportStatus.Done -> { vm.showExport = false; vm.clearExportStatus() }
                        else -> if (format == ExportFormat.GIF) vm.exportGif(context, v, frames.roundToInt(), duration, loop)
                        else vm.exportStill(context, v, format, quality.roundToInt())
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatusRow(working: Boolean, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (working) {
            CircularProgressIndicator(color = GlitchColors.accent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, color = GlitchColors.textSecondary, fontSize = 13.sp)
    }
}
