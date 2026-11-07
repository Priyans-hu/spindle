package com.markfoundry.spindle.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import com.markfoundry.spindle.ui.theme.MotionTokens
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Platter / ring angle in degrees. Read [angle] only inside graphicsLayer or draw lambdas. */
@Stable
class Spin internal constructor() {
    internal val angleState = mutableFloatStateOf(0f)
    val angle: Float get() = angleState.floatValue
}

/**
 * Velocity engine (spec s2/s4): play spins up over SPIN_UP_MS, pause coasts over COAST_MS.
 * The frame loop runs only while moving; reduced motion holds velocity at 0 and runs no loop.
 * The first composition starts at the target speed, so a cold start or skin switch never replays.
 */
@Composable
fun rememberSpin(playing: Boolean, degPerSec: Float, motion: Boolean): Spin {
    val spin = remember { Spin() }
    val velocity = remember { Animatable(if (playing && motion) degPerSec else 0f) }
    LaunchedEffect(playing, motion, degPerSec) {
        if (!motion) {
            velocity.snapTo(0f)
            return@LaunchedEffect
        }
        coroutineScope {
            launch {
                velocity.animateTo(
                    targetValue = if (playing) degPerSec else 0f,
                    animationSpec = if (playing) {
                        tween(MotionTokens.SPIN_UP_MS, easing = MotionTokens.EmphasizedDecelerate)
                    } else {
                        tween(MotionTokens.COAST_MS, easing = LinearOutSlowInEasing)
                    },
                )
            }
            var last = withFrameNanos { it }
            while (playing || velocity.isRunning || velocity.value > 0f) {
                withFrameNanos { now ->
                    val dt = (now - last) / 1_000_000_000f
                    last = now
                    spin.angleState.floatValue = (spin.angle + velocity.value * dt) % 360f
                }
            }
        }
    }
    return spin
}
