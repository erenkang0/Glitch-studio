package com.glitchstudio.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.glitchstudio.app.effects.ParamKind
import com.glitchstudio.app.ui.clickableNoRipple
import com.glitchstudio.app.ui.theme.GlitchTheme
import kotlin.math.abs
import kotlin.math.roundToInt

/** Formats a parameter value the way Lightroom does: signed for bipolar controls. */
fun formatValue(value: Float, bipolar: Boolean): String {
    val rounded = if (abs(value) >= 10f) value.roundToInt().toString()
    else ((value * 100f).roundToInt() / 100f).toString()
    return if (bipolar && value > 0f) "+$rounded" else rounded
}

private val HueStops: List<Color> = listOf(
    Color.hsv(0f, 0.75f, 1f),
    Color.hsv(60f, 0.75f, 1f),
    Color.hsv(120f, 0.75f, 1f),
    Color.hsv(180f, 0.75f, 1f),
    Color.hsv(240f, 0.75f, 1f),
    Color.hsv(300f, 0.75f, 1f),
    Color.hsv(360f, 0.75f, 1f),
)

private fun sliderText(
    value: Float,
    bipolar: Boolean,
    kind: ParamKind,
    range: ClosedFloatingPointRange<Float>,
): String {
    val isUnitAngle = kind == ParamKind.ANGLE && range.start >= 0f && range.endInclusive <= 1.001f
    return if (isUnitAngle) "${(value * 360f).roundToInt()}°" else formatValue(value, bipolar)
}

/**
 * Lightroom-style horizontal slider: label on the left, live value on the right,
 * a thin track with an accent fill and a white thumb. Drag to scrub, tap to jump,
 * double-tap to reset.
 */
@Composable
fun GlitchSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    bipolar: Boolean = false,
    kind: ParamKind = ParamKind.LINEAR,
    valueText: String = sliderText(value, bipolar, kind, valueRange),
    onReset: (() -> Unit)? = null,
) {
    val colors = GlitchTheme.colors
    var trackWidth by remember { mutableFloatStateOf(1f) }
    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)

    fun fraction(v: Float): Float = ((v - valueRange.start) / span).coerceIn(0f, 1f)
    fun valueAt(x: Float): Float =
        valueRange.start + (x / trackWidth).coerceIn(0f, 1f) * span

    Column(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = androidx.compose.material3.MaterialTheme.typography.labelLarge, color = colors.textMed)
            Text(valueText, style = androidx.compose.material3.MaterialTheme.typography.labelLarge, color = colors.textHigh)
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .onSizeChanged { trackWidth = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(valueRange) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        onValueChange(valueAt(change.position.x))
                    }
                }
                .pointerInput(valueRange) {
                    detectTapGestures(
                        onDoubleTap = { onReset?.invoke() },
                        onTap = { onValueChange(valueAt(it.x)) },
                    )
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val cy = size.height / 2f
                val trackH = 3.dp.toPx()
                val thumbR = 8.dp.toPx()
                val fx = (fraction(value) * size.width).coerceIn(0f, size.width)
                if (kind == ParamKind.HUE) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(HueStops),
                        topLeft = Offset(0f, cy - trackH / 2f),
                        size = Size(size.width, trackH),
                        cornerRadius = CornerRadius(trackH / 2f),
                    )
                } else {
                    drawLine(colors.stroke, Offset(0f, cy), Offset(size.width, cy), trackH, cap = StrokeCap.Round)
                    val from = if (bipolar) size.width / 2f else 0f
                    drawLine(colors.accent, Offset(from, cy), Offset(fx, cy), trackH, cap = StrokeCap.Round)
                }
                drawCircle(Color.White, radius = thumbR, center = Offset(fx, cy))
                drawCircle(colors.backdrop, radius = thumbR, center = Offset(fx, cy), style = Stroke(width = 2.dp.toPx()))
            }
        }
    }
}

/** Compact segmented control used for export format / discrete choices. */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GlitchTheme.colors
    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.panel)
            .border(1.dp, colors.stroke, RoundedCornerShape(10.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) colors.panelElevated else Color.Transparent)
                    .clickableNoRipple { onSelect(index) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                    color = if (selected) colors.textHigh else colors.textMed,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Round icon button for the top bar / canvas overlays. */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = GlitchTheme.colors.textHigh,
    background: Color = GlitchTheme.colors.panel,
    size: androidx.compose.ui.unit.Dp = 44.dp,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickableNoRipple { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.46f))
    }
}
