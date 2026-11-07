package com.markfoundry.spindle.skin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.markfoundry.spindle.player.NowPlaying
import com.markfoundry.spindle.player.PlayerControls
import com.markfoundry.spindle.ui.theme.SkinColors
import com.markfoundry.spindle.ui.theme.SpindleType

/**
 * A swappable visual identity for the player. Every skin renders the same
 * [NowPlaying] state and drives the same [PlayerControls], so adding a skin
 * never touches the media/core layer. Gates and the Looks sheet read its tokens.
 */
interface PlayerSkin {
    val id: String
    val displayName: String
    val type: SpindleType
    val trayRadius: Dp

    @Composable
    fun colors(): SkinColors

    /** The player; [state] null is Nothing playing, drawn on the same deck and tray. */
    @Composable
    fun Render(state: NowPlaying?, controls: PlayerControls)

    /** Static hero for the gates: parked, blank label, no frame loop. */
    @Composable
    fun IdleHero(modifier: Modifier)
}
