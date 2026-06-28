package com.glitchstudio.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Flat, Figma-inspired dark palette. */
object GlitchColors {
    val background = Color(0xFF0B0B10)
    val surface = Color(0xFF15151D)
    val surfaceHigh = Color(0xFF1E1E28)
    val surfaceTop = Color(0xFF232330)
    val border = Color(0xFF2A2A38)
    val borderStrong = Color(0xFF3A3A4D)

    val textPrimary = Color(0xFFECECF2)
    val textSecondary = Color(0xFF9A9AAB)
    val textMuted = Color(0xFF63637A)

    val accent = Color(0xFF7C5CFF)
    val accentSoft = Color(0xFF2A2540)
    val cyan = Color(0xFF2DE2E6)
    val magenta = Color(0xFFFF3DCB)
    val danger = Color(0xFFFF5C7A)
    val success = Color(0xFF3DE08A)

    val brand: Brush get() = Brush.horizontalGradient(listOf(cyan, magenta))
    val accentGradient: Brush get() = Brush.horizontalGradient(listOf(accent, magenta))
}

private val glitchTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 0.5.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.3.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.2.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp)
)

private val glitchScheme = darkColorScheme(
    primary = GlitchColors.accent,
    onPrimary = Color.White,
    secondary = GlitchColors.cyan,
    background = GlitchColors.background,
    onBackground = GlitchColors.textPrimary,
    surface = GlitchColors.surface,
    onSurface = GlitchColors.textPrimary,
    surfaceVariant = GlitchColors.surfaceHigh,
    onSurfaceVariant = GlitchColors.textSecondary,
    outline = GlitchColors.border,
    error = GlitchColors.danger
)

// Expressive-language shapes: large, rounded, friendly corners.
private val expressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun GlitchTheme(content: @Composable () -> Unit) {
    // Stable Material 3 styled in the Material 3 Expressive language (large rounded
    // shapes, vibrant accents, springy motion applied at the component level).
    MaterialTheme(
        colorScheme = glitchScheme,
        typography = glitchTypography,
        shapes = expressiveShapes,
        content = content
    )
}
