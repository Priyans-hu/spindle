package com.markfoundry.spindle.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/** Motion tokens (spec s2). Springs are damping/stiffness pairs from M3 Expressive. */
object MotionTokens {
    const val PRESS_DAMPING = 0.6f
    const val PRESS_STIFFNESS = 800f
    const val LAYOUT_DAMPING = 0.8f
    const val LAYOUT_STIFFNESS = 380f
    const val LIFT_DAMPING = 1f
    const val LIFT_STIFFNESS = 3800f
    const val TOUCHDOWN_DAMPING = 0.45f
    const val TOUCHDOWN_STIFFNESS = 900f

    const val SPIN_UP_MS = 700
    const val COAST_MS = 1500
    const val SWING_MS = 420
    const val RETURN_MS = 380

    const val SKIN_FADE_IN_MS = 250
    const val SKIN_FADE_OUT_MS = 150
    const val ART_FADE_IN_MS = 300
    const val ART_FADE_OUT_MS = 200

    const val ANALOG_DEG_PER_SEC = 200f
    const val FLUX_DEG_PER_SEC = 60f

    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    fun <T> press(): SpringSpec<T> = spring(PRESS_DAMPING, PRESS_STIFFNESS)
    fun <T> layout(): SpringSpec<T> = spring(LAYOUT_DAMPING, LAYOUT_STIFFNESS)
    fun <T> lift(): SpringSpec<T> = spring(LIFT_DAMPING, LIFT_STIFFNESS)
    fun <T> touchdown(): SpringSpec<T> = spring(TOUCHDOWN_DAMPING, TOUCHDOWN_STIFFNESS)
}
