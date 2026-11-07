package com.markfoundry.spindle.skin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.markfoundry.spindle.player.NoopControls
import com.markfoundry.spindle.player.NowPlaying
import com.markfoundry.spindle.player.PlayerControls
import com.markfoundry.spindle.player.PreviewNowPlaying
import com.markfoundry.spindle.ui.NextIcon
import com.markfoundry.spindle.ui.PauseIcon
import com.markfoundry.spindle.ui.PlayIcon
import com.markfoundry.spindle.ui.PrevIcon
import com.markfoundry.spindle.ui.SeekBar
import com.markfoundry.spindle.ui.motion.Spin
import com.markfoundry.spindle.ui.motion.rememberMotionEnabled
import com.markfoundry.spindle.ui.motion.rememberSpin
import com.markfoundry.spindle.ui.rememberHaptics
import com.markfoundry.spindle.ui.theme.AnalogType
import com.markfoundry.spindle.ui.theme.DeckPalette
import com.markfoundry.spindle.ui.theme.MotionTokens
import com.markfoundry.spindle.ui.theme.SkinColors
import com.markfoundry.spindle.ui.theme.SpindleDimens
import com.markfoundry.spindle.ui.theme.SpindleFlags
import com.markfoundry.spindle.ui.theme.analogColors

object TurntableSkin : PlayerSkin {
    override val id = "turntable"
    override val displayName = "Turntable"
    override val type = AnalogType

    @Composable
    override fun colors(): SkinColors = analogColors()

    @Composable
    override fun Render(state: NowPlaying, controls: PlayerControls) {
        TurntableScreen(state, controls)
    }

    @Composable
    override fun IdleHero(modifier: Modifier) {
        BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
            val d = min(maxWidth / ArmGeometry.DECK_WIDTH, maxHeight)
            AnalogDeck(d, analogColors(), artwork = null, isPlaying = false, progress = 0f)
        }
    }
}

@Composable
private fun TurntableScreen(state: NowPlaying, controls: PlayerControls) {
    val colors = analogColors()
    BoxWithConstraints(Modifier.fillMaxSize().background(colors.ground)) {
        // Disc fills what the tray leaves, capped so a 1.3x font shrinks the disc, not the tray.
        // The deck is wider than the disc so the parked arm stays on screen.
        val discCap = min((maxWidth - SpindleDimens.Gutter * 2) / ArmGeometry.DECK_WIDTH, maxHeight * 0.46f)
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = SpindleDimens.Gutter),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Spindle", color = colors.ink, style = AnalogType.wordmark)
            }
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = SpindleDimens.Gutter),
                contentAlignment = Alignment.Center,
            ) {
                val d = min(discCap, min(maxWidth / ArmGeometry.DECK_WIDTH, maxHeight))
                AnalogDeck(d, colors, state.artwork, state.isPlaying, state.progress)
            }
            Tray(state, controls, colors)
        }
    }
}

/** Disc plus tonearm on a deck box DECK_WIDTH x 1 disc diameters. */
@Composable
private fun AnalogDeck(d: Dp, colors: SkinColors, artwork: ImageBitmap?, isPlaying: Boolean, progress: Float) {
    val motion = rememberMotionEnabled()
    val spin = rememberSpin(isPlaying, MotionTokens.ANALOG_DEG_PER_SEC, motion)
    val arm = rememberTonearm(isPlaying, progress, motion, rememberHaptics())
    Box(Modifier.size(d * ArmGeometry.DECK_WIDTH, d)) {
        ArmBase(colors, Modifier.fillMaxSize())
        Record(artwork, spin, colors, Modifier.size(d))
        if (SpindleFlags.SHEEN) Sheen(Modifier.size(d))
        TonearmArm(arm, colors, Modifier.fillMaxSize())
    }
}

@Composable
private fun Record(artwork: ImageBitmap?, spin: Spin, colors: SkinColors, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            // Print shadow stays put while the record turns.
            .drawBehind {
                drawCircle(colors.shadow, size.minDimension / 2f, center + Offset(0f, SpindleDimens.PrintShadow.toPx()))
            }
            // Offscreen layer caches the grooves; each frame only re-composites the rotated texture.
            .graphicsLayer {
                rotationZ = spin.angle
                compositingStrategy = CompositingStrategy.Offscreen
            },
    ) {
        val c = center
        val outer = size.minDimension / 2f
        val labelR = outer * LABEL
        drawCircle(DeckPalette.Vinyl, outer, c)
        val groove = Stroke(width = 1.2.dp.toPx())
        var r = outer * 0.96f
        while (r > labelR) {
            drawCircle(DeckPalette.Groove, r, c, style = groove)
            r -= 3.5.dp.toPx()
        }
        drawCircle(colors.text.copy(alpha = 0.6f), outer * 0.9f, c, style = Stroke(1.dp.toPx()))
        drawCircle(colors.discRim, outer - 0.75.dp.toPx(), c, style = Stroke(1.5.dp.toPx()))
        if (artwork != null) {
            val d = (labelR * 2f).toInt()
            clipPath(Path().apply { addOval(Rect(c, labelR)) }) {
                drawImage(
                    image = artwork,
                    dstOffset = IntOffset((c.x - labelR).toInt(), (c.y - labelR).toInt()),
                    dstSize = IntSize(d, d),
                )
            }
        } else {
            // Blank label: a cream white-label pressing.
            drawCircle(colors.text2, labelR, c)
            drawCircle(colors.ground, labelR * 0.62f, c, style = Stroke(1.dp.toPx()))
        }
        drawCircle(colors.text, labelR, c, style = Stroke(3.dp.toPx()))
        drawCircle(colors.ground, outer * 0.035f, c)
    }
}

private const val LABEL = 0.42f

/** Fixed sheen: two soft 30 deg wedges on the vinyl. Never rotates with the record. */
@Composable
private fun Sheen(modifier: Modifier) {
    Canvas(modifier) {
        val outer = size.minDimension / 2f * 0.96f
        val inner = size.minDimension / 2f * LABEL
        val clear = Color.White.copy(alpha = 0f)
        val glint = Color.White.copy(alpha = 0.10f)
        fun wedge(centerDeg: Float) = arrayOf(
            (centerDeg - 15f) / 360f to clear,
            centerDeg / 360f to glint,
            (centerDeg + 15f) / 360f to clear,
        )
        val stops = arrayOf(0f to clear) + wedge(120f) + wedge(300f) + arrayOf(1f to clear)
        drawCircle(
            brush = Brush.sweepGradient(*stops, center = center),
            radius = (outer + inner) / 2f,
            center = center,
            style = Stroke(outer - inner),
        )
    }
}

@Composable
private fun Tray(state: NowPlaying, controls: PlayerControls, colors: SkinColors) {
    val haptics = rememberHaptics()
    val radius = SpindleDimens.AnalogTrayRadius
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = radius, topEnd = radius))
            .background(colors.tray)
            .drawBehind {
                // 1dp top edge (Night); its sides fall outside the clip.
                val r = radius.toPx()
                drawRoundRect(
                    color = colors.trayEdge,
                    topLeft = Offset(0f, 0.5.dp.toPx()),
                    size = Size(size.width, size.height + r * 2),
                    cornerRadius = CornerRadius(r),
                    style = Stroke(1.dp.toPx()),
                )
            }
            .navigationBarsPadding()
            .padding(SpindleDimens.TrayPadding),
    ) {
        Text(
            state.title,
            color = colors.text,
            style = AnalogType.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (state.artist.isNotBlank()) {
            Text(
                state.artist,
                color = colors.text2,
                style = AnalogType.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (!state.album.isNullOrBlank()) {
            Text(state.album, color = colors.text2, style = AnalogType.meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        SeekBar(
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            canSeek = state.canSeek,
            onSeek = controls::seekTo,
            track = colors.seekTrack,
            fill = colors.accent,
            thumb = colors.seekThumb,
            timeColor = colors.text2,
            timeStyle = AnalogType.time,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(SpindleDimens.TrayPadding / 2))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TransportButton("Previous track", { haptics.skipTrack(); controls.previous() }, SpindleDimens.PrevNext, ring = colors.seekTrack) {
                PrevIcon(colors.text, 26.dp)
            }
            Spacer(Modifier.width(SpindleDimens.TransportGap))
            TransportButton(
                label = if (state.isPlaying) "Pause" else "Play",
                onClick = { if (state.isPlaying) haptics.pause(); controls.playPause() },
                diameter = SpindleDimens.Play,
                fill = colors.playFill,
                ring = colors.playRim,
                ringWidth = SpindleDimens.PlayRim,
            ) {
                if (state.isPlaying) PauseIcon(colors.playIcon, 30.dp) else PlayIcon(colors.playIcon, 30.dp)
            }
            Spacer(Modifier.width(SpindleDimens.TransportGap))
            TransportButton("Next track", { haptics.skipTrack(); controls.next() }, SpindleDimens.PrevNext, ring = colors.seekTrack) {
                NextIcon(colors.text, 26.dp)
            }
        }
    }
}

@Composable
private fun TransportButton(
    label: String,
    onClick: () -> Unit,
    diameter: Dp,
    fill: Color = Color.Transparent,
    ring: Color,
    ringWidth: Dp = 1.5.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(diameter)
            .clip(CircleShape)
            .background(fill)
            .border(ringWidth, ring, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun TurntablePreview() {
    TurntableScreen(PreviewNowPlaying, NoopControls)
}
