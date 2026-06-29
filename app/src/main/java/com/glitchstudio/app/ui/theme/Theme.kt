package com.glitchstudio.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Extended design tokens that go beyond the Material3 [androidx.compose.material3.ColorScheme].
 * Access through [GlitchTheme.colors] inside composables.
 */
@Immutable
data class GlitchColors(
    val backdrop: Color,
    val panel: Color,
    val panelElevated: Color,
    val panelPressed: Color,
    val stroke: Color,
    val strokeStrong: Color,
    val textHigh: Color,
    val textMed: Color,
    val textLow: Color,
    val accent: Color,
    val accentBright: Color,
    val accentPurple: Color,
    val accentPink: Color,
    val accentTeal: Color,
    val accentAmber: Color,
    val danger: Color,
    val success: Color,
) {
    val accentGradient: List<Color> get() = listOf(accent, accentPurple)
    val rgbGradient: List<Color> get() = listOf(accentPink, accentTeal, accent)
}

private val DefaultGlitchColors = GlitchColors(
    backdrop = Ink900,
    panel = Ink800,
    panelElevated = Ink750,
    panelPressed = Ink700,
    stroke = Stroke,
    strokeStrong = StrokeStrong,
    textHigh = TextHigh,
    textMed = TextMed,
    textLow = TextLow,
    accent = Accent,
    accentBright = AccentBright,
    accentPurple = AccentPurple,
    accentPink = AccentPink,
    accentTeal = AccentTeal,
    accentAmber = AccentAmber,
    danger = Danger,
    success = Success,
)

val LocalGlitchColors = staticCompositionLocalOf { DefaultGlitchColors }

private val GlitchM3Dark = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = AccentPurple,
    onSecondary = Color.White,
    background = Ink900,
    onBackground = TextHigh,
    surface = Ink800,
    onSurface = TextHigh,
    surfaceVariant = Ink750,
    onSurfaceVariant = TextMed,
    outline = Stroke,
    error = Danger,
)

object GlitchTheme {
    val colors: GlitchColors
        @Composable get() = LocalGlitchColors.current
}

@Composable
fun GlitchTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // The studio is intentionally always dark to keep the canvas neutral.
    androidx.compose.runtime.CompositionLocalProvider(
        LocalGlitchColors provides DefaultGlitchColors
    ) {
        MaterialTheme(
            colorScheme = GlitchM3Dark,
            typography = GlitchTypography,
            shapes = GlitchShapes,
            content = content,
        )
    }
}
