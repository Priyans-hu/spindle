package com.markfoundry.spindle.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.markfoundry.spindle.player.NowPlaying
import com.markfoundry.spindle.player.PlayerControls
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/** The best artwork a session publishes: album art, then generic art, then its display icon. */
fun MediaMetadata.artBitmap(): Bitmap? = getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
    ?: getBitmap(MediaMetadata.METADATA_KEY_ART)
    ?: getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

/**
 * Reads and controls the device's active media session (Spotify, YouTube Music, ...)
 * via [MediaSessionManager]. Requires the user to have enabled Notification access,
 * which is what authorizes [MediaSessionManager.getActiveSessions].
 */
class MediaSessionRepository(context: Context) : PlayerControls {

    private val appContext = context.applicationContext
    private val sessionManager =
        appContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val listenerComponent =
        ComponentName(appContext, SpindleNotificationListener::class.java)

    private val _state = MutableStateFlow<NowPlaying?>(null)
    val state: StateFlow<NowPlaying?> = _state.asStateFlow()

    private var controller: MediaController? = null

    // Art is decoded once per metadata change; ticks reuse the same ImageBitmap instance.
    private var artwork: ImageBitmap? = null
    private var artDirty = true

    // Optimistic seek: report the target until the session catches up (within 2s) or 3s pass.
    private var heldTargetMs: Long? = null
    private var heldAt = 0L

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            artDirty = true
            heldTargetMs = null
            update()
        }
        override fun onPlaybackStateChanged(state: PlaybackState?) = update()
        override fun onSessionDestroyed() = refreshSessions()
    }

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { refreshSessions() }

    fun start() {
        try {
            sessionManager.addOnActiveSessionsChangedListener(sessionsChangedListener, listenerComponent)
            refreshSessions()
        } catch (e: SecurityException) {
            _state.value = null
        }
    }

    fun stop() {
        runCatching { sessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener) }
        controller?.unregisterCallback(controllerCallback)
        controller = null
    }

    /** Recompute state; a 1s ticker calls this so progress advances while playing. */
    fun tick() = update()

    private fun refreshSessions() {
        val controllers = try {
            sessionManager.getActiveSessions(listenerComponent)
        } catch (e: SecurityException) {
            emptyList()
        }
        val next = controllers.firstOrNull()
        if (next?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(controllerCallback)
            controller = next
            controller?.registerCallback(controllerCallback)
            artDirty = true
            heldTargetMs = null
        }
        update()
    }

    private fun update() {
        val c = controller
        val md = c?.metadata
        if (c == null || md == null) {
            _state.value = null
            return
        }
        val ps = c.playbackState
        val playing = ps?.state == PlaybackState.STATE_PLAYING
        val now = SystemClock.elapsedRealtime()
        val reported = when {
            ps == null -> 0L
            playing -> ps.position + ((now - ps.lastPositionUpdateTime) * ps.playbackSpeed).toLong()
            else -> ps.position
        }
        val duration = md.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(0L)
        if (artDirty) {
            artDirty = false
            artwork = md.artBitmap()?.asImageBitmap()
        }
        _state.value = NowPlaying(
            title = md.getString(MediaMetadata.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() } ?: "Unknown",
            artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: md.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: "",
            album = md.getString(MediaMetadata.METADATA_KEY_ALBUM),
            isPlaying = playing,
            positionMs = heldPosition(reported, playing, now).coerceIn(0L, duration.takeIf { it > 0 } ?: Long.MAX_VALUE),
            durationMs = duration,
            artwork = artwork,
            canSeek = duration > 0L && ps != null && (ps.actions and PlaybackState.ACTION_SEEK_TO) != 0L,
        )
    }

    private fun heldPosition(reported: Long, playing: Boolean, now: Long): Long {
        val target = heldTargetMs ?: return reported
        val since = now - heldAt
        val expected = target + if (playing) since else 0L
        if (abs(reported - expected) <= HOLD_TOLERANCE_MS || since > HOLD_TIMEOUT_MS) {
            heldTargetMs = null
            return reported
        }
        return expected
    }

    override fun playPause() {
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) {
            c.transportControls.pause()
        } else {
            c.transportControls.play()
        }
    }

    override fun next() {
        controller?.transportControls?.skipToNext()
    }

    override fun previous() {
        controller?.transportControls?.skipToPrevious()
    }

    override fun seekTo(fraction: Float) {
        val c = controller ?: return
        val dur = c.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: return
        if (dur <= 0L) return
        val target = (dur * fraction.coerceIn(0f, 1f)).toLong()
        Log.i(TAG, "MediaSessionRepository.seekTo: targetMs=$target durationMs=$dur")
        heldTargetMs = target
        heldAt = SystemClock.elapsedRealtime()
        c.transportControls.seekTo(target)
        update()
    }

    private companion object {
        const val TAG = "Spindle"
        const val HOLD_TOLERANCE_MS = 2_000L
        const val HOLD_TIMEOUT_MS = 3_000L
    }
}
