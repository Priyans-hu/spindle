package com.markfoundry.spindle.demo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log

/**
 * A real MediaSession owned by the app itself, so the demo flows through the same
 * MediaSessionManager path (repository, listener, widget) as Spotify or YouTube Music.
 */
object DemoSession {
    private const val TAG = "SpindleDemo"

    private class Track(
        val title: String,
        val artist: String,
        val album: String,
        val durationMs: Long,
        val colors: IntArray,
    )

    // Track 3 is a live stream (duration 0) so "duration 0 hides the bar" is testable.
    private val tracks = listOf(
        Track("Harbor Lights", "Juniper Vale", "Low Tide Sessions", 562_000L, intArrayOf(0xFF2F5F96.toInt(), 0xFFD7823F.toInt(), 0xFFF2E7D0.toInt())),
        Track("Paper Satellites", "The Quiet Engines", "Orbit Club", 214_000L, intArrayOf(0xFF3B1F5C.toInt(), 0xFFFF4D8D.toInt(), 0xFF3FE6F2.toInt())),
        Track("Night Radio", "Station Nine", "Live", 0L, intArrayOf(0xFF0F3B2E.toInt(), 0xFFE9C46A.toInt(), 0xFFF4F1DE.toInt())),
    )

    private val arts = HashMap<Int, Bitmap>()
    private val main = Handler(Looper.getMainLooper())
    private var session: MediaSession? = null
    private var index = 0
    private var playing = true
    private var seekable = true
    private var positionMs = 0L
    private var anchor = 0L

    fun start(context: Context) = main.post {
        if (session == null) {
            session = MediaSession(context.applicationContext, TAG).apply {
                setCallback(callback, main)
                isActive = true
            }
        }
        index = 0
        playing = true
        publish(newTrack = true, startAt = 238_000L)
        Log.i(TAG, "DemoSession: started")
    }

    fun stop() = main.post {
        session?.run { isActive = false; release() }
        session = null
        Log.i(TAG, "DemoSession: stopped")
    }

    fun setSeekable(on: Boolean) = main.post {
        seekable = on
        publish(newTrack = false, startAt = currentPosition())
        Log.i(TAG, "DemoSession: seekable=$on")
    }

    /** Skips as if the source app did it, without going through Spindle's controls. */
    fun next() = main.post { if (session != null) skip(1) }

    private val callback = object : MediaSession.Callback() {
        override fun onPlay() = setPlaying(true)
        override fun onPause() = setPlaying(false)
        override fun onSkipToNext() = skip(1)
        override fun onSkipToPrevious() = skip(-1)
        override fun onSeekTo(pos: Long) {
            Log.i(TAG, "DemoSession: onSeekTo pos=$pos")
            publish(newTrack = false, startAt = pos.coerceIn(0L, tracks[index].durationMs))
        }
    }

    private fun setPlaying(on: Boolean) {
        val pos = currentPosition()
        playing = on
        publish(newTrack = false, startAt = pos)
        Log.i(TAG, "DemoSession: playing=$on")
    }

    private fun skip(delta: Int) {
        index = (index + delta + tracks.size) % tracks.size
        publish(newTrack = true, startAt = 0L)
        Log.i(TAG, "DemoSession: track=${tracks[index].title}")
    }

    private fun currentPosition(): Long {
        val elapsed = if (playing) SystemClock.elapsedRealtime() - anchor else 0L
        val dur = tracks[index].durationMs
        return if (dur > 0) (positionMs + elapsed).coerceAtMost(dur) else 0L
    }

    private fun publish(newTrack: Boolean, startAt: Long) {
        val s = session ?: return
        val t = tracks[index]
        if (newTrack) {
            s.setMetadata(
                MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, t.title)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, t.artist)
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, t.album)
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, t.durationMs)
                    .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, arts.getOrPut(index) { art(t.colors, index) })
                    .build(),
            )
        }
        positionMs = startAt
        anchor = SystemClock.elapsedRealtime()
        var actions = PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or
            PlaybackState.ACTION_SKIP_TO_PREVIOUS
        if (seekable) actions = actions or PlaybackState.ACTION_SEEK_TO
        s.setPlaybackState(
            PlaybackState.Builder()
                .setActions(actions)
                .setState(
                    if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                    startAt,
                    if (playing) 1f else 0f,
                    anchor,
                )
                .build(),
        )
    }

    /** Generated sleeve art: a two-color field with a sun and offset stripes. */
    private fun art(colors: IntArray, seed: Int): Bitmap {
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), colors[0], colors[1], Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), p)
        val cx = size * (0.35f + 0.15f * seed)
        val cy = size * (0.62f - 0.1f * seed)
        p.shader = RadialGradient(cx, cy, size * 0.32f, colors[2], colors[1], Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, size * 0.26f, p)
        p.shader = null
        p.color = colors[0]
        p.alpha = 150
        val band = size / 18f
        for (i in 0 until 6) {
            val y = size * 0.58f + i * band * 1.6f
            c.drawRect(0f, y, size.toFloat(), y + band * (0.35f + 0.1f * i), p)
        }
        return bmp
    }
}
