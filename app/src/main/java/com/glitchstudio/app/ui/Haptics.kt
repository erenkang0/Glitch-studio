package com.glitchstudio.app.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Lightweight haptic feedback. Uses predefined vibration effects on API 29+ and
 * falls back to short one-shot vibrations on older devices.
 */
class Haptics(context: Context) {

    private val vibrator: Vibrator? = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            mgr?.defaultVibrator
        }
        else -> @Suppress("DEPRECATION")
        (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
    }

    private val enabled get() = vibrator?.hasVibrator() == true

    /** A crisp click for confirmations (select, toggle, button press). */
    fun click() = predefinedOrOneShot(VibrationEffect.EFFECT_CLICK, 18, 90)

    /** A soft tick for continuous controls (slider steps, scrubbing). */
    fun tick() = predefinedOrOneShot(VibrationEffect.EFFECT_TICK, 10, 60)

    /** A heavier pulse for significant moments (export complete, layer added). */
    fun heavy() = predefinedOrOneShot(VibrationEffect.EFFECT_HEAVY_CLICK, 28, 140)

    @Suppress("DEPRECATION")
    private fun predefinedOrOneShot(predefined: Int, ms: Long, amplitude: Int) {
        val v = vibrator ?: return
        if (!enabled) return
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                    v.vibrate(VibrationEffect.createPredefined(predefined))
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                    v.vibrate(VibrationEffect.createOneShot(ms, amplitude))
                else -> v.vibrate(ms)
            }
        } catch (_: Throwable) {
            // Vibration is best-effort; never crash the editor over haptics.
        }
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val context = LocalContext.current
    return remember { Haptics(context.applicationContext) }
}
