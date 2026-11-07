package com.markfoundry.spindle

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markfoundry.spindle.media.NotificationAccess
import com.markfoundry.spindle.skin.skinById
import com.markfoundry.spindle.ui.LooksButton
import com.markfoundry.spindle.ui.OnboardingScreen
import com.markfoundry.spindle.ui.SkinPicker
import com.markfoundry.spindle.ui.motion.rememberMotionEnabled
import com.markfoundry.spindle.ui.theme.MotionTokens
import com.markfoundry.spindle.ui.theme.SpindleFlags
import com.markfoundry.spindle.ui.theme.SpindleTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SpindleApp(viewModel) }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }
}

private const val ONBOARDING = "onboarding"
private const val PLAYER = "player:"

@Composable
private fun SpindleApp(viewModel: MainViewModel) {
    SpindleTheme {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        var granted by remember { mutableStateOf(viewModel.isAccessGranted()) }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    granted = viewModel.isAccessGranted()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        val nowPlaying by viewModel.state.collectAsStateWithLifecycle()
        val skinId by viewModel.skinId.collectAsStateWithLifecycle()
        val skin = skinById(skinId)
        val colors = skin.colors()
        val motion = rememberMotionEnabled()
        var pickerOpen by remember { mutableStateOf(false) }

        val view = LocalView.current
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = colors.ink.luminance() < 0.5f
                isAppearanceLightNavigationBars = false
            }
        }

        // Nothing playing and playing share one screen, so the disc never moves when a session arrives.
        val screen = if (granted) PLAYER + skin.id else ONBOARDING
        Box(Modifier.fillMaxSize().background(colors.ground)) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    if (SpindleFlags.SKIN_FADE && motion) {
                        fadeIn(tween(MotionTokens.SKIN_FADE_IN_MS)) togetherWith fadeOut(tween(MotionTokens.SKIN_FADE_OUT_MS))
                    } else {
                        EnterTransition.None togetherWith ExitTransition.None
                    }
                },
                label = "screen",
            ) { target ->
                when {
                    target == ONBOARDING -> OnboardingScreen(
                        skin = skin,
                        onGrantClick = { context.startActivity(NotificationAccess.settingsIntent()) },
                        onHelpClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                            )
                        },
                    )
                    else -> skinById(target.removePrefix(PLAYER)).Render(state = nowPlaying, controls = viewModel.controls)
                }
            }

            if (granted) {
                LooksButton(
                    skin = skin,
                    tint = colors.ink,
                    onClick = { pickerOpen = true },
                    modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 8.dp, end = 12.dp),
                )
            }

            if (pickerOpen) {
                SkinPicker(
                    active = skin,
                    onSelect = { viewModel.selectSkin(it); pickerOpen = false },
                    onDismiss = { pickerOpen = false },
                )
            }
        }
    }
}
