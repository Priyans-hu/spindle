package com.markfoundry.spindle.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.markfoundry.spindle.R
import com.markfoundry.spindle.player.NoopControls
import com.markfoundry.spindle.player.NowPlaying
import com.markfoundry.spindle.player.PlayerControls
import com.markfoundry.spindle.ui.motion.rememberMotionEnabled
import com.markfoundry.spindle.ui.theme.MotionTokens
import com.markfoundry.spindle.ui.theme.SkinColors
import com.markfoundry.spindle.ui.theme.SpindleDimens
import com.markfoundry.spindle.ui.theme.SpindleType

/** Bottom tray: top-rounded, filled with the tray token, 1dp top edge (Night cream / Flux glass). */
fun Modifier.tray(colors: SkinColors, radius: Dp): Modifier =
    clip(RoundedCornerShape(topStart = radius, topEnd = radius))
        .background(colors.tray)
        .drawBehind {
            // The edge's sides fall outside the clip, leaving only the top arc.
            val r = radius.toPx()
            drawRoundRect(
                color = colors.trayEdge,
                topLeft = Offset(0f, 0.5.dp.toPx()),
                size = Size(size.width, size.height + r * 2),
                cornerRadius = CornerRadius(r),
                style = Stroke(1.dp.toPx()),
            )
        }

/** Stand-in track that sizes the Nothing playing tray like a playing one (title, artist, album). */
private val IdleSizer = NowPlaying(title = "-", artist = "-", album = "-")

/**
 * Bottom tray for both states. Nothing playing lays out the playing rows invisibly (no touch, no
 * TalkBack) so the deck slot, and the disc, keep their place when a session arrives. Contents
 * fade 250/150; any height change (title wrap, album line) runs on the layout spring.
 */
@Composable
fun PlayerTray(
    state: NowPlaying?,
    controls: PlayerControls,
    colors: SkinColors,
    radius: Dp,
    type: SpindleType,
    content: @Composable (NowPlaying, PlayerControls) -> Unit,
) {
    val motion = rememberMotionEnabled()
    // The playing rows fading out after the session ends still need its last state.
    val last = remember { arrayOfNulls<NowPlaying>(1) }
    state?.let { last[0] = it }
    val sizeSpec: FiniteAnimationSpec<IntSize> = if (motion) MotionTokens.layout() else snap()
    AnimatedContent(
        targetState = state == null,
        transitionSpec = {
            if (motion) {
                fadeIn(tween(MotionTokens.SKIN_FADE_IN_MS)) togetherWith fadeOut(tween(MotionTokens.SKIN_FADE_OUT_MS)) using null
            } else {
                EnterTransition.None togetherWith ExitTransition.None using null
            }
        },
        label = "tray",
        modifier = Modifier
            .fillMaxWidth()
            .tray(colors, radius)
            .animateContentSize(sizeSpec)
            .navigationBarsPadding()
            .padding(SpindleDimens.TrayPadding),
    ) { idle ->
        if (idle) {
            Box {
                Box(Modifier.alpha(0f).clearAndSetSemantics {}.pointerInput(Unit) { blockTouches() }) {
                    content(IdleSizer, NoopControls)
                }
                Text(
                    stringResource(R.string.idle_message),
                    color = colors.text2,
                    style = type.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            (state ?: last[0])?.let { content(it, controls) }
        }
    }
}

/** Swallows every pointer event before the children see it. */
private suspend fun PointerInputScope.blockTouches() {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
        }
    }
}

/** 64dp wordmark row under the status bar; its center lines up with the 48dp Looks button. */
@Composable
fun WordmarkRow(color: Color, style: TextStyle, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = SpindleDimens.Gutter),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text("Spindle", color = color, style = style)
    }
}

/** Circular transport button with a filled face and/or a ring; labelled for TalkBack. */
@Composable
fun TransportButton(
    label: String,
    onClick: () -> Unit,
    diameter: Dp,
    fill: Brush = SolidColor(Color.Transparent),
    ring: Color = Color.Transparent,
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
