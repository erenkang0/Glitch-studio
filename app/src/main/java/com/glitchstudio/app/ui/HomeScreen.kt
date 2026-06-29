package com.glitchstudio.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.glitchstudio.app.effects.EffectCategory
import com.glitchstudio.app.effects.EffectRepository
import com.glitchstudio.app.ui.theme.GlitchColors
import com.glitchstudio.app.ui.theme.GlitchTheme

@Composable
fun HomeScreen(
    onPickImage: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = GlitchTheme.colors
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(colors.panel.copy(alpha = 0.35f), colors.backdrop, colors.backdrop),
                ),
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LensMark()

            Spacer(Modifier.height(30.dp))

            Text(
                "Glitch Studio",
                style = MaterialTheme.typography.headlineLarge,
                color = colors.textHigh,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(10.dp))

            Text(
                "A shader darkroom for your photos.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textMed,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(26.dp))

            CategoryTeaser()

            Spacer(Modifier.height(40.dp))

            AddPhotoButton(onClick = onPickImage, isLoading = isLoading)

            Spacer(Modifier.height(18.dp))

            MetaLine()
        }
    }
}

@Composable
private fun MetaLine() {
    val colors = GlitchTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            "${EffectRepository.count} effects",
            style = MaterialTheme.typography.labelMedium,
            color = colors.textMed,
        )
        Dot(colors.textLow)
        Text(
            "PNG · JPEG · WebP · GIF",
            style = MaterialTheme.typography.labelMedium,
            color = colors.textLow,
        )
    }
}

@Composable
private fun Dot(tint: Color) {
    Box(
        Modifier
            .padding(horizontal = 10.dp)
            .size(3.dp)
            .clip(CircleShape)
            .background(tint),
    )
}

/** A restrained, horizontally-scrolling teaser of the effect families. */
@Composable
private fun CategoryTeaser() {
    val colors = GlitchTheme.colors
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        items(EffectRepository.categories) { category ->
            CategoryChip(label = category.label, dot = categoryColor(category, colors))
        }
    }
}

@Composable
private fun CategoryChip(label: String, dot: Color) {
    val colors = GlitchTheme.colors
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.panel)
            .border(1.dp, colors.stroke, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dot),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textMed,
        )
    }
}

/** Maps each effect family to one of the restrained category accent hues. */
private fun categoryColor(category: EffectCategory, colors: GlitchColors): Color = when (category) {
    EffectCategory.COLOR -> colors.accent
    EffectCategory.LIGHT -> colors.accentAmber
    EffectCategory.STYLIZE -> colors.accentPurple
    EffectCategory.DISTORT -> colors.accentTeal
    EffectCategory.GLITCH -> colors.accentPink
    EffectCategory.BLUR -> colors.accentBright
    EffectCategory.TEXTURE -> colors.accentTeal
    EffectCategory.RETRO -> colors.accentAmber
}

@Composable
private fun AddPhotoButton(onClick: () -> Unit, isLoading: Boolean) {
    val colors = GlitchTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isLoading) colors.accent.copy(alpha = 0.55f) else colors.accent)
            .clickableNoRipple(enabled = !isLoading) { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Preparing photo…",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Rounded.AddPhotoAlternate,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Open a photo",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
            }
        }
    }
}

/**
 * The RGB-split lens brand mark, drawn to match the app icon: three offset
 * aperture rings backed by a soft neutral well so it reads on the dark backdrop.
 */
@Composable
private fun LensMark() {
    val colors = GlitchTheme.colors
    Box(
        Modifier
            .size(108.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(colors.panelElevated, colors.panel.copy(alpha = 0.0f)),
                ),
            )
            .border(1.dp, colors.stroke, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(96.dp)) {
            val r = size.minDimension * 0.32f
            val c = Offset(size.width / 2f, size.height / 2f)
            val o = size.minDimension * 0.05f
            val w = size.minDimension * 0.045f
            drawCircle(colors.accentPink, radius = r, center = c.copy(x = c.x - o), style = Stroke(w))
            drawCircle(colors.accentTeal, radius = r, center = c, style = Stroke(w))
            drawCircle(colors.accent, radius = r, center = c.copy(x = c.x + o), style = Stroke(w))
            // Centre aperture dot keeps the mark feeling like a real lens.
            drawCircle(colors.textHigh.copy(alpha = 0.9f), radius = size.minDimension * 0.03f, center = c)
        }
    }
}
