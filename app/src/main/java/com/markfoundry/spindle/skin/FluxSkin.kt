package com.markfoundry.spindle.skin

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.markfoundry.spindle.player.NowPlaying
import com.markfoundry.spindle.player.PlayerControls
import com.markfoundry.spindle.ui.NextIcon
import com.markfoundry.spindle.ui.PauseIcon
import com.markfoundry.spindle.ui.PlayIcon
import com.markfoundry.spindle.ui.PrevIcon
import com.markfoundry.spindle.ui.SeekBar
import com.markfoundry.spindle.ui.TransportButton
import com.markfoundry.spindle.ui.WordmarkRow
import com.markfoundry.spindle.ui.PlayerTray
import com.markfoundry.spindle.ui.motion.motionSpec
import com.markfoundry.spindle.ui.motion.rememberMotionEnabled
import com.markfoundry.spindle.ui.motion.rememberSpin
import com.markfoundry.spindle.ui.rememberHaptics
import com.markfoundry.spindle.ui.theme.Flux
import com.markfoundry.spindle.ui.theme.FluxPalette
import com.markfoundry.spindle.ui.theme.FluxType
import com.markfoundry.spindle.ui.theme.MotionTokens
import com.markfoundry.spindle.ui.theme.SkinColors
import com.markfoundry.spindle.ui.theme.SpindleDimens

object FluxSkin : PlayerSkin {
    override val id = "flux"
    override val displayName = "Flux"
    override val type = FluxType
    override val trayRadius = SpindleDimens.FluxTrayRadius

    /** Dark-only by identity (spec s1). */
    @Composable
    override fun colors(): SkinColors = Flux

    @Composable
    override fun Render(state: NowPlaying?, controls: PlayerControls) {
        FluxScreen(state, controls)
    }

    @Composable
    override fun IdleHero(modifier: Modifier) {
        BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
            FluxDisc(min(maxWidth, maxHeight), artwork = null, isPlaying = false)
        }
    }
}

private val FluxGround = Brush.verticalGradient(listOf(FluxPalette.GroundTop, FluxPalette.Ground))

@Composable
private fun FluxScreen(state: NowPlaying?, controls: PlayerControls) {
    BoxWithConstraints(Modifier.fillMaxSize().background(FluxGround)) {
        val discCap = min(maxWidth - SpindleDimens.Gutter * 2, maxHeight * 0.46f)
        Column(Modifier.fillMaxSize()) {
            WordmarkRow(Flux.ink, FluxType.wordmark)
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = SpindleDimens.Gutter),
                contentAlignment = Alignment.Center,
            ) {
                FluxDisc(min(discCap, min(maxWidth, maxHeight)), state?.artwork, state?.isPlaying == true)
            }
            PlayerTray(state, controls, Flux, FluxSkin.trayRadius, FluxType) { s, c -> GlassTrayRows(s, c) }
        }
    }
}

/** Art disc inside the dichroic foil ring; colour appears only where light catches the foil. */
@Composable
private fun FluxDisc(size: Dp, artwork: ImageBitmap?, isPlaying: Boolean) {
    val motion = rememberMotionEnabled()
    val spin = rememberSpin(isPlaying, MotionTokens.FLUX_DEG_PER_SEC, motion)
    // Pause breath: art settles to 0.94 on the layout spring, play returns it on the press spring.
    val breath by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.94f,
        animationSpec = motionSpec(motion, if (isPlaying) MotionTokens.press() else MotionTokens.layout()),
        label = "flux-breath",
    )
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        // Ring and glow turn together: the glow takes each foil arc's colour, none between arcs.
        Canvas(
            Modifier.fillMaxSize().graphicsLayer {
                rotationZ = spin.angle
                compositingStrategy = CompositingStrategy.Offscreen
            },
        ) {
            val glow = GLOW.toPx()
            val outer = this.size.minDimension / 2f - glow
            val stroke = outer * 2f * RING
            val r = outer - stroke / 2f
            drawCircle(FoilGlow, outer, style = Stroke(glow * 2f))
            // Radial falloff over 24dp each side of the ring's outer edge; DstIn masks only the glow drawn so far.
            drawCircle(
                brush = Brush.radialGradient(
                    0f to Color.Transparent,
                    (outer - glow) / (outer + glow) to Color.Transparent,
                    outer / (outer + glow) to Color.Black,
                    1f to Color.Transparent,
                    center = center,
                    radius = outer + glow,
                ),
                radius = outer + glow,
                blendMode = BlendMode.DstIn,
            )
            drawCircle(FoilRing, r, style = Stroke(stroke))
            drawCircle(Flux.trayEdge, r + stroke / 2f, style = Stroke(1.dp.toPx()))
            drawCircle(Flux.trayEdge, r - stroke / 2f, style = Stroke(1.dp.toPx()))
        }
        // Art fills the ring to a 4dp gap; no border, so light shows only on the foil.
        Box(
            modifier = Modifier
                .size((size - GLOW * 2) * (1f - RING * 2) - ART_GAP * 2)
                .graphicsLayer {
                    scaleX = breath
                    scaleY = breath
                }
                .clip(CircleShape)
                .background(FluxPalette.GroundTop),
        ) {
            if (artwork != null) {
                Image(artwork, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
    }
}

private val GLOW = 24.dp
private val ART_GAP = 4.dp

/** Ring stroke as a fraction of the ring's outer diameter. */
private const val RING = 0.075f

private val FoilBase = FluxPalette.SeekTrack.copy(alpha = 0.30f)

private fun glowOf(c: Color) = c.copy(alpha = 0.15f)

private val GlowClear = FluxPalette.Accent.copy(alpha = 0f)

/** The foil arcs at 15%, clear between them. Same stops as [FoilRing]. */
private val FoilGlow = Brush.sweepGradient(
    0f to GlowClear,
    0.05f to GlowClear,
    0.10f to glowOf(FluxPalette.Accent),
    0.15f to glowOf(FluxPalette.Violet),
    0.20f to glowOf(FluxPalette.Magenta),
    0.27f to GlowClear,
    0.55f to GlowClear,
    0.60f to glowOf(FluxPalette.Accent),
    0.64f to glowOf(FluxPalette.Violet),
    0.68f to GlowClear,
    1f to GlowClear,
)

private val FoilRing = Brush.sweepGradient(
    0f to FoilBase,
    0.05f to FoilBase,
    0.10f to FluxPalette.Accent,
    0.15f to FluxPalette.Violet,
    0.20f to FluxPalette.Magenta,
    0.27f to FoilBase,
    0.55f to FoilBase,
    0.60f to FluxPalette.Accent,
    0.64f to FluxPalette.Violet,
    0.68f to FoilBase,
    1f to FoilBase,
)

private val FoilButton = Brush.sweepGradient(
    listOf(FluxPalette.Accent, FluxPalette.Violet, FluxPalette.Magenta, FluxPalette.Accent),
)

@Composable
private fun GlassTrayRows(state: NowPlaying, controls: PlayerControls) {
    val haptics = rememberHaptics()
    Column(Modifier.fillMaxWidth()) {
        Text(state.title, color = Flux.text, style = FluxType.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (state.artist.isNotBlank()) {
            Text(
                state.artist,
                color = Flux.text2,
                style = FluxType.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (!state.album.isNullOrBlank()) {
            Text(state.album, color = Flux.text2, style = FluxType.meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        SeekBar(
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            canSeek = state.canSeek,
            onSeek = controls::seekTo,
            track = Flux.seekTrack,
            fill = Flux.accent,
            thumb = Flux.seekThumb,
            timeColor = Flux.text2,
            timeStyle = FluxType.time,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(SpindleDimens.TrayPadding / 2))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TransportButton("Previous track", { haptics.skipTrack(); controls.previous() }, SpindleDimens.PrevNext, ring = Flux.seekTrack) {
                PrevIcon(Flux.text, 26.dp)
            }
            Spacer(Modifier.width(SpindleDimens.TransportGap))
            TransportButton(
                label = if (state.isPlaying) "Pause" else "Play",
                onClick = {
                    if (state.isPlaying) haptics.pause() else haptics.touchdown()
                    controls.playPause()
                },
                diameter = SpindleDimens.Play,
                fill = FoilButton,
            ) {
                if (state.isPlaying) PauseIcon(Flux.playIcon, 30.dp) else PlayIcon(Flux.playIcon, 30.dp)
            }
            Spacer(Modifier.width(SpindleDimens.TransportGap))
            TransportButton("Next track", { haptics.skipTrack(); controls.next() }, SpindleDimens.PrevNext, ring = Flux.seekTrack) {
                NextIcon(Flux.text, 26.dp)
            }
        }
    }
}
