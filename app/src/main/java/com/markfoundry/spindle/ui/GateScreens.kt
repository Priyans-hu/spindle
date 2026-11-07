package com.markfoundry.spindle.ui

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.markfoundry.spindle.R
import com.markfoundry.spindle.skin.PlayerSkin
import com.markfoundry.spindle.ui.motion.motionSpec
import com.markfoundry.spindle.ui.motion.rememberMotionEnabled
import com.markfoundry.spindle.ui.theme.MotionTokens
import com.markfoundry.spindle.ui.theme.SpindleDimens

/** Gates use the active skin's tokens (Analog Day/Night before the first pick). */
@Composable
fun OnboardingScreen(skin: PlayerSkin, onGrantClick: () -> Unit, onHelpClick: () -> Unit) {
    val colors = skin.colors()
    val type = skin.type
    val motion = rememberMotionEnabled()
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val discScale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.92f,
        animationSpec = motionSpec(motion, MotionTokens.layout()),
        label = "onboarding-disc",
    )

    @Composable
    fun appear(index: Int): Modifier {
        val alpha by animateFloatAsState(
            targetValue = if (shown) 1f else 0f,
            animationSpec = motionSpec(motion, tween(MotionTokens.SKIN_FADE_IN_MS, delayMillis = 60 * index)),
            label = "onboarding-text-$index",
        )
        return Modifier.graphicsLayer { this.alpha = alpha }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.ground).systemBarsPadding()) {
        // Large fonts shrink the hero first so the CTA stays above the fold.
        val fontScale = LocalDensity.current.fontScale
        val heroHeight = min(maxWidth - SpindleDimens.Gutter * 2, maxHeight * 0.34f / (fontScale * fontScale))
        // Scrolls only when a large font outgrows the screen.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            WordmarkRow(colors.ink, skin.type.wordmark)
            skin.IdleHero(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpindleDimens.Gutter)
                    .height(heroHeight)
                    .graphicsLayer {
                        scaleX = discScale
                        scaleY = discScale
                    },
            )
            Column(Modifier.padding(start = SpindleDimens.Gutter, end = SpindleDimens.Gutter, top = SpindleDimens.Gutter, bottom = 16.dp)) {
                Text(stringResource(R.string.onboarding_headline), color = colors.ink, style = type.display, modifier = appear(0))
                Text(
                    stringResource(R.string.onboarding_body),
                    color = colors.ink,
                    style = type.artist,
                    modifier = appear(1).padding(top = 12.dp),
                )
                Box(
                    modifier = appear(2)
                        .padding(top = SpindleDimens.Gutter)
                        .fillMaxWidth()
                        .heightIn(min = SpindleDimens.Cta)
                        .clip(RoundedCornerShape(SpindleDimens.Cta / 2))
                        .background(colors.ctaFill)
                        .clickable(role = Role.Button, onClick = onGrantClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.onboarding_cta),
                        color = colors.ctaLabel,
                        style = type.button,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                // Sideloaded APKs on Android 13+ hit restricted settings; App info unlocks them.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Column(
                        modifier = appear(3)
                            .padding(top = 8.dp)
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(SpindleDimens.ChipRadius))
                            .clickable(role = Role.Button, onClick = onHelpClick)
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            stringResource(R.string.onboarding_help),
                            color = colors.ink,
                            style = type.button.copy(textDecoration = TextDecoration.Underline),
                        )
                        Text(stringResource(R.string.onboarding_help_hint), color = colors.ink, style = type.meta)
                    }
                }
            }
        }
    }
}

/** Nothing playing: idle disc with the arm parked, and the player's tray holding one line. */
@Composable
fun NothingPlayingScreen(skin: PlayerSkin) {
    val colors = skin.colors()
    Column(Modifier.fillMaxSize().background(colors.ground)) {
        WordmarkRow(colors.ink, skin.type.wordmark)
        skin.IdleHero(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = SpindleDimens.Gutter, end = SpindleDimens.Gutter, bottom = SpindleDimens.Gutter),
        )
        Text(
            stringResource(R.string.idle_message),
            color = colors.text2,
            style = skin.type.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .tray(colors, skin.trayRadius)
                .navigationBarsPadding()
                .padding(SpindleDimens.TrayPadding)
                .padding(bottom = SpindleDimens.Gutter),
        )
    }
}
