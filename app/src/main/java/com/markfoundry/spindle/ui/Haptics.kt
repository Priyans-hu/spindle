package com.markfoundry.spindle.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/** One haptic per discrete event (spec s2 haptic policy), none for spin. Silent below the required API. */
class Haptics(private val view: View) {
    private fun perform(constant: Int) {
        view.performHapticFeedback(constant)
    }

    fun seekStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) perform(HapticFeedbackConstants.GESTURE_START)
    }

    fun seekEnd() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) perform(HapticFeedbackConstants.GESTURE_END)
    }

    /** Needle touchdown (and Flux play). */
    fun touchdown() {
        perform(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
            else HapticFeedbackConstants.CLOCK_TICK,
        )
    }

    fun pause() = perform(HapticFeedbackConstants.VIRTUAL_KEY)

    fun skipTrack() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) perform(HapticFeedbackConstants.SEGMENT_TICK)
    }

    fun skinCommit() = perform(HapticFeedbackConstants.CONTEXT_CLICK)
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}
