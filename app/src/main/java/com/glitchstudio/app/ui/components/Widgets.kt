package com.glitchstudio.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glitchstudio.app.ui.Haptics
import com.glitchstudio.app.ui.rememberHaptics
import com.glitchstudio.app.ui.theme.GlitchColors
import kotlin.math.roundToInt

@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueText: String = formatValue(value),
    haptics: Haptics = rememberHaptics()
) {
    var lastNotch by remember { mutableStateOf(Int.MIN_VALUE) }
    Box(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = GlitchColors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(valueText, color = GlitchColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = value,
            onValueChange = {
                val notch = (((it - min) / (max - min)) * 24f).roundToInt()
                if (notch != lastNotch) { lastNotch = notch; haptics.tick() }
                onChange(it)
            },
            valueRange = min..max,
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            colors = SliderDefaults.colors(
                thumbColor = GlitchColors.accent,
                activeTrackColor = GlitchColors.accent,
                inactiveTrackColor = GlitchColors.surfaceTop
            )
        )
    }
}

private fun formatValue(v: Float): String =
    if (kotlin.math.abs(v) >= 10f) v.roundToInt().toString()
    else String.format("%.2f", v)

@Composable
fun SegmentedTabs(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHaptics()
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GlitchColors.surfaceHigh)
            .border(1.dp, GlitchColors.border, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val bg by animateColorAsState(
                if (selected) GlitchColors.accent else Color.Transparent,
                tween(220), label = "tabBg"
            )
            val fg by animateColorAsState(
                if (selected) Color.White else GlitchColors.textSecondary,
                tween(220), label = "tabFg"
            )
            Box(
                Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { if (!selected) { haptics.click(); onSelect(index) } },
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun GlitchChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHaptics()
    val bg by animateColorAsState(
        if (selected) GlitchColors.accentSoft else GlitchColors.surfaceHigh, tween(200), label = "chipBg"
    )
    val borderColor by animateColorAsState(
        if (selected) GlitchColors.accent else GlitchColors.border, tween(200), label = "chipBorder"
    )
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { haptics.click(); onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            color = if (selected) GlitchColors.textPrimary else GlitchColors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun GradientButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    brush: Brush = GlitchColors.accentGradient,
    enabled: Boolean = true
) {
    val haptics = rememberHaptics()
    val scale by animateFloatAsState(if (enabled) 1f else 0.98f, label = "btnScale")
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(brush)
            .clickable(enabled = enabled) { haptics.click(); onClick() }
            .padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GhostIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = GlitchColors.textPrimary,
    active: Boolean = false
) {
    val haptics = rememberHaptics()
    val bg by animateColorAsState(
        if (active) GlitchColors.accentSoft else GlitchColors.surfaceHigh, tween(200), label = "iconBg"
    )
    Box(
        modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, if (active) GlitchColors.accent else GlitchColors.border, RoundedCornerShape(12.dp))
            .clickable { haptics.click(); onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = if (active) GlitchColors.accent else tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun ColorDot(color: Color, modifier: Modifier = Modifier, size: Int = 8) {
    Box(modifier.size(size.dp).clip(CircleShape).background(color))
}

@Composable
fun pressScale(pressed: Boolean): Float {
    val scale by animateFloatAsState(
        if (pressed) 0.94f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "press"
    )
    return scale
}

fun outlinedBorder(color: Color = GlitchColors.border) = BorderStroke(1.dp, color)
