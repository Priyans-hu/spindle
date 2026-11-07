package com.markfoundry.spindle.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.markfoundry.spindle.ui.motion.motionSpec
import com.markfoundry.spindle.ui.motion.rememberMotionEnabled
import com.markfoundry.spindle.ui.theme.MotionTokens
import com.markfoundry.spindle.ui.theme.SpindleDimens

/**
 * Draggable seek bar on a 48dp row. Position is carried by the thumb (fill vs track is
 * under 3:1 by design). Not seekable: a static 8dp marker. Duration 0: nothing renders.
 */
@Composable
fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    canSeek: Boolean,
    onSeek: (Float) -> Unit,
    track: Color,
    fill: Color,
    thumb: Color,
    timeColor: Color,
    timeStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    if (durationMs <= 0L) return
    val haptics = rememberHaptics()
    val seek by rememberUpdatedState(onSeek)
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val fraction = dragFraction ?: (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    val shownMs = dragFraction?.let { (it * durationMs).toLong() } ?: positionMs
    val thumbSize by animateDpAsState(
        targetValue = if (dragFraction != null) SpindleDimens.ThumbPressed else SpindleDimens.Thumb,
        animationSpec = motionSpec(rememberMotionEnabled(), MotionTokens.press()),
        label = "seek-thumb",
    )

    Column(modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(SpindleDimens.SeekRow)
                .clearAndSetSemantics {
                    contentDescription = "Seek"
                    stateDescription = "${formatTime(shownMs)} of ${formatTime(durationMs)}"
                    progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                    if (canSeek) {
                        setProgress { target -> seek(target.coerceIn(0f, 1f)); true }
                    }
                }
                .then(
                    if (!canSeek) Modifier else Modifier.pointerInput(Unit) {
                        fun at(x: Float) = (x / size.width).coerceIn(0f, 1f)
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()
                            haptics.seekStart()
                            dragFraction = at(down.position.x)
                            try {
                                while (true) {
                                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    change.consume()
                                    dragFraction = at(change.position.x)
                                }
                                dragFraction?.let(seek)
                                haptics.seekEnd()
                            } finally {
                                dragFraction = null
                            }
                        }
                    },
                ),
        ) {
            val y = size.height / 2f
            val x = size.width * fraction
            val stroke = SpindleDimens.SeekTrack.toPx()
            drawLine(track, Offset(0f, y), Offset(size.width, y), stroke, StrokeCap.Round)
            drawLine(fill, Offset(0f, y), Offset(x, y), stroke, StrokeCap.Round)
            val d = if (canSeek) thumbSize else SpindleDimens.StaticMarker
            drawCircle(thumb, d.toPx() / 2f, Offset(x, y))
        }
        // Pull the times up under the track; the 48dp touch row keeps its full height.
        // TalkBack already hears both times in the Seek node's state.
        Row(
            modifier = Modifier.fillMaxWidth().offset(y = (-8).dp).clearAndSetSemantics {},
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatTime(shownMs), color = timeColor, style = timeStyle)
            Text(formatTime(durationMs), color = timeColor, style = timeStyle)
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000L).toInt()
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
