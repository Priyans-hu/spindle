package com.markfoundry.spindle.player

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

/** Immutable snapshot of what's currently playing, rendered by a skin. */
@Immutable
data class NowPlaying(
    val title: String,
    val artist: String,
    val album: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    /** Album art from the media session; null when the app sends none (skins draw a placeholder). */
    val artwork: ImageBitmap? = null,
    /** True when the session offers ACTION_SEEK_TO and reports a duration. */
    val canSeek: Boolean = false,
) {
    /** Playback progress in the range 0f..1f. */
    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

/** Transport actions a skin can invoke. Backed by the media session in Phase 2. */
interface PlayerControls {
    fun playPause()
    fun next()
    fun previous()
    fun seekTo(fraction: Float)
}

/** No-op controls for previews and the not-yet-wired scaffold. */
object NoopControls : PlayerControls {
    override fun playPause() {}
    override fun next() {}
    override fun previous() {}
    override fun seekTo(fraction: Float) {}
}

/** Sample state for @Preview and the Phase 1 scaffold, before the media service exists. */
val PreviewNowPlaying = NowPlaying(
    title = "So What",
    artist = "Miles Davis",
    album = "Kind of Blue",
    isPlaying = true,
    positionMs = 238_000L,
    durationMs = 562_000L,
    canSeek = true,
)
