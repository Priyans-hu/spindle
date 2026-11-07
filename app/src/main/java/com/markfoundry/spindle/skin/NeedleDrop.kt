package com.markfoundry.spindle.skin

import android.os.SystemClock
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.markfoundry.spindle.ui.Haptics
import com.markfoundry.spindle.ui.motion.motionSpec
import com.markfoundry.spindle.ui.theme.DeckPalette
import com.markfoundry.spindle.ui.theme.MotionTokens
import com.markfoundry.spindle.ui.theme.SkinColors
import com.markfoundry.spindle.ui.theme.SpindleFlags
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "Spindle"

/**
 * Tonearm geometry in disc-box units (D = disc diameter), spec s4. Angles are measured off
 * the pivot-to-center line; the head parks clear of the disc at REST.
 */
object ArmGeometry {
    const val PIVOT_X = 1.02f
    const val PIVOT_Y = 0.10f
    const val LENGTH = 0.52f
    const val REST = 62f
    const val GROOVE_START = 44f
    const val GROOVE_END = 21f

    /** Deck box width in D: the parked headshell reaches about 1.13D. */
    const val DECK_WIDTH = 1.14f

    fun grooveAngle(progress: Float): Float =
        if (!SpindleFlags.ARM_TRACKING) GROOVE_START
        else GROOVE_START + (GROOVE_END - GROOVE_START) * progress.coerceIn(0f, 1f)
}

/** Every value is an Animatable, so a tap mid-sequence retargets from the current state. */
@Stable
class Tonearm internal constructor(angle: Float) {
    val angle = Animatable(angle)
    val lift = Animatable(0f)
    internal var lastPlaying: Boolean? = null
}

/**
 * Needle drop (spec s4). Runs only on a paused-to-playing change seen while the player has been
 * visible for a moment (a session arriving on Nothing playing is one, on the same deck);
 * first composition (cold start, skin switch) and resume snap.
 */
@Composable
fun rememberTonearm(isPlaying: Boolean, progress: Float, motion: Boolean, haptics: Haptics): Tonearm {
    val groove by rememberUpdatedState(ArmGeometry.grooveAngle(progress))
    val arm = remember { Tonearm(if (isPlaying) groove else ArmGeometry.REST) }
    val settled = rememberSettledVisible()

    LaunchedEffect(isPlaying, motion) {
        val transition = arm.lastPlaying != null && arm.lastPlaying != isPlaying
        arm.lastPlaying = isPlaying
        val user = transition && settled()
        val animate = user && motion
        if (isPlaying) {
            if (animate) {
                coroutineScope {
                    launch { arm.lift.animateTo(1f, MotionTokens.lift()) }
                    arm.angle.animateTo(groove, tween(MotionTokens.SWING_MS, easing = MotionTokens.EmphasizedDecelerate))
                }
            } else {
                arm.angle.snapTo(groove)
            }
            if (user) {
                Log.d(TAG, "NeedleDrop: touchdown")
                haptics.touchdown()
            }
            coroutineScope {
                launch { arm.lift.animateTo(0f, motionSpec(animate, MotionTokens.touchdown())) }
                var logged = Float.NaN
                snapshotFlow { groove }.collectLatest { target ->
                    if (logged.isNaN() || abs(target - logged) >= 0.5f) {
                        Log.d(TAG, "NeedleDrop: armAngle=%.1f".format(target))
                        logged = target
                    }
                    arm.angle.animateTo(target, motionSpec(motion, MotionTokens.layout()))
                }
            }
        } else {
            if (animate) {
                coroutineScope {
                    launch { arm.lift.animateTo(1f, MotionTokens.lift()) }
                    delay(60)
                    arm.angle.animateTo(ArmGeometry.REST, tween(MotionTokens.RETURN_MS, easing = FastOutSlowInEasing))
                }
                arm.lift.animateTo(0f, MotionTokens.lift())
            } else {
                arm.angle.snapTo(ArmGeometry.REST)
                arm.lift.snapTo(0f)
            }
        }
    }
    return arm
}

/** True once the activity has been started for 500 ms, so a resume snaps instead of dropping. */
@Composable
private fun rememberSettledVisible(): () -> Boolean {
    val owner = LocalLifecycleOwner.current
    val startedAt = remember { longArrayOf(SystemClock.uptimeMillis()) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) startedAt[0] = SystemClock.uptimeMillis()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return {
        owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
            SystemClock.uptimeMillis() - startedAt[0] > 500
    }
}

/** Unit vector from pivot to disc center, the arm's zero-angle direction. */
private val ArmAxis = Offset(0.5f - ArmGeometry.PIVOT_X, 0.5f - ArmGeometry.PIVOT_Y).let { it / it.getDistance() }

/** Static plinth: pivot base and the arm rest. Drawn on a deck box sized DECK_WIDTH D by D. */
@Composable
fun ArmBase(colors: SkinColors, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val d = size.height
        val pivot = Offset(ArmGeometry.PIVOT_X * d, ArmGeometry.PIVOT_Y * d)
        val rad = Math.toRadians(-ArmGeometry.REST.toDouble())
        val rest = Offset(
            (ArmAxis.x * cos(rad) - ArmAxis.y * sin(rad)).toFloat(),
            (ArmAxis.x * sin(rad) + ArmAxis.y * cos(rad)).toFloat(),
        )
        drawCircle(colors.shadow, 0.09f * d, pivot + Offset(0f, 4.dp.toPx()))
        drawCircle(colors.tray, 0.09f * d, pivot)
        drawCircle(colors.trayEdge, 0.09f * d, pivot, style = Stroke(1.dp.toPx()))
        val post = pivot + rest * (0.42f * d)
        drawCircle(colors.tray, 0.03f * d, post)
        drawCircle(DeckPalette.ArmMetal, 0.03f * d, post, style = Stroke(1.dp.toPx()))
    }
}

/** The arm, drawn on the pivot-to-center axis and turned by graphicsLayer around the pivot. */
@Composable
fun TonearmArm(arm: Tonearm, colors: SkinColors, modifier: Modifier = Modifier) {
    Canvas(
        modifier.graphicsLayer {
            val s = 1f + 0.04f * arm.lift.value
            rotationZ = -arm.angle.value
            scaleX = s
            scaleY = s
            transformOrigin = TransformOrigin(ArmGeometry.PIVOT_X / ArmGeometry.DECK_WIDTH, ArmGeometry.PIVOT_Y)
        },
    ) {
        val d = size.height
        val pivot = Offset(ArmGeometry.PIVOT_X * d, ArmGeometry.PIVOT_Y * d)
        if (colors.shadow.alpha > 0f) {
            // Print shadow falls straight down on screen: undo the layer rotation for the offset.
            val drop = (2.dp + 4.dp * arm.lift.value.coerceAtLeast(0f)).toPx()
            val t = Math.toRadians(arm.angle.value.toDouble())
            val local = Offset((-drop * sin(t)).toFloat(), (drop * cos(t)).toFloat())
            drawArm(pivot + local, d, colors.shadow, colors.shadow, colors.shadow)
        }
        drawArm(pivot, d, DeckPalette.ArmMetal, DeckPalette.Vinyl, colors.playFill)
        drawCircle(DeckPalette.ArmMetal, 0.034f * d, pivot)
        drawCircle(DeckPalette.Vinyl, 0.012f * d, pivot)
    }
}

private fun DrawScope.drawArm(pivot: Offset, d: Float, metal: Color, outline: Color, cartridge: Color) {
    val perp = Offset(-ArmAxis.y, ArmAxis.x)
    fun p(u: Float, v: Float = 0f) = pivot + ArmAxis * (u * d) + perp * (v * d)
    val edge = 1.5.dp.toPx()
    // Counterweight behind the pivot: dark weight, metal collar.
    drawLine(outline, p(-0.15f), p(-0.06f), 0.06f * d, StrokeCap.Butt)
    drawLine(metal, p(-0.075f), p(-0.06f), 0.06f * d, StrokeCap.Butt)
    // Tube.
    drawLine(outline, p(0f), p(0.45f), 0.018f * d + edge, StrokeCap.Round)
    drawLine(metal, p(0f), p(0.45f), 0.018f * d, StrokeCap.Round)
    // Finger lift, headshell (slight offset angle), cartridge.
    drawLine(metal, p(0.47f, -0.02f), p(0.485f, -0.06f), 0.012f * d, StrokeCap.Round)
    drawLine(outline, p(0.44f), p(0.54f, 0.012f), 0.05f * d + edge, StrokeCap.Butt)
    drawLine(metal, p(0.44f), p(0.54f, 0.012f), 0.05f * d, StrokeCap.Butt)
    drawLine(cartridge, p(0.49f, 0.008f), p(0.535f, 0.012f), 0.032f * d, StrokeCap.Butt)
}
