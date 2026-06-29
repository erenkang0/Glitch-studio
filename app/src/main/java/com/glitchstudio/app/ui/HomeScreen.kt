package com.glitchstudio.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.glitchstudio.app.effects.EffectRepository
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
            .background(colors.backdrop)
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
            Spacer(Modifier.height(28.dp))
            Text(
                "Glitch Studio",
                style = MaterialTheme.typography.headlineLarge,
                color = colors.textHigh,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "A shader darkroom for your photos.\n${EffectRepository.count} creative effects, each fully adjustable.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMed,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(40.dp))
            AddPhotoButton(onClick = onPickImage, enabled = !isLoading)
            Spacer(Modifier.height(16.dp))
            Text(
                "PNG · JPEG · WebP · animated GIF",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textLow,
            )
        }

        if (isLoading) {
            CircularProgressIndicator(
                color = colors.accent,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(48.dp)
                    .size(28.dp),
            )
        }
    }
}

@Composable
private fun AddPhotoButton(onClick: () -> Unit, enabled: Boolean) {
    val colors = GlitchTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.accent)
            .clickableNoRipple(enabled = enabled) { onClick() }
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "Open a photo",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
        )
    }
}

/** The RGB-split lens brand mark, drawn to match the app icon. */
@Composable
private fun LensMark() {
    val colors = GlitchTheme.colors
    Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension * 0.34f
            val c = Offset(size.width / 2f, size.height / 2f)
            val o = size.minDimension * 0.05f
            val w = size.minDimension * 0.045f
            drawCircle(colors.accentPink, radius = r, center = c.copy(x = c.x - o), style = Stroke(w))
            drawCircle(colors.accentTeal, radius = r, center = c, style = Stroke(w))
            drawCircle(colors.accent, radius = r, center = c.copy(x = c.x + o), style = Stroke(w))
        }
    }
}
