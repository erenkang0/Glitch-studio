package com.glitchstudio.app.ui.theme

import androidx.compose.ui.graphics.Color

// Neutral, photo-centric surfaces in the spirit of Adobe Lightroom mobile:
// pure-grey darks with no colour tint so the image stays colour-accurate.
val Ink900 = Color(0xFF0D0D0D)   // app backdrop / filmstrip wells
val Ink850 = Color(0xFF151515)
val Ink800 = Color(0xFF1C1C1C)   // tool panels / bottom bar
val Ink750 = Color(0xFF242424)   // elevated cards / selected chip
val Ink700 = Color(0xFF2E2E2E)   // pressed / hover
val Stroke = Color(0xFF333333)   // hairline borders
val StrokeStrong = Color(0xFF454545)

// Text -------------------------------------------------------------------------
val TextHigh = Color(0xFFF2F2F2)
val TextMed = Color(0xFF9C9C9C)
val TextLow = Color(0xFF666666)

// Single restrained accent (Lightroom's active-slider blue). Used for active
// slider fills, the selected tool, and focus rings — sparingly, never on chrome.
val Accent = Color(0xFF4B9FE1)
val AccentBright = Color(0xFF6FB4ED)

// Category hints — used only as small colour dots on effect groups, never as
// large fills, so the chrome stays neutral.
val AccentPurple = Color(0xFFB16BFF)
val AccentPink = Color(0xFFFF4D8D)
val AccentTeal = Color(0xFF34D6B0)
val AccentAmber = Color(0xFFFFB23E)

// State ------------------------------------------------------------------------
val Danger = Color(0xFFFF5C5C)
val Success = Color(0xFF35D49A)
