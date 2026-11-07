package com.markfoundry.spindle.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.markfoundry.spindle.MainActivity
import com.markfoundry.spindle.R
import com.markfoundry.spindle.data.SettingsStore
import com.markfoundry.spindle.media.SpindleNotificationListener
import com.markfoundry.spindle.skin.FluxSkin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Builds the per-skin widget RemoteViews from the active media session; one update per change. */
object SpindleWidget {

    const val ACTION_PREV = "com.markfoundry.spindle.widget.PREV"
    const val ACTION_PLAY_PAUSE = "com.markfoundry.spindle.widget.PLAY_PAUSE"
    const val ACTION_NEXT = "com.markfoundry.spindle.widget.NEXT"

    private const val TAG = "Spindle"

    // RemoteViews bitmaps cross binder; 256px keeps each update far below the transaction limit.
    private const val ART_MAX_PX = 256

    /** Pass [skinId] when it was just saved; otherwise the stored pick is read. */
    fun updateAll(context: Context, skinId: String? = null) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val ids = manager.getAppWidgetIds(ComponentName(context, SpindleWidgetProvider::class.java))
        if (ids.isEmpty()) return
        manager.updateAppWidget(ids, buildViews(context, skinId ?: savedSkinId(context)))
    }

    // The DataStore is a process singleton, so after the first read this is an in-memory lookup.
    private fun savedSkinId(context: Context): String? =
        runBlocking { SettingsStore(context).skinId.first() }

    private fun buildViews(context: Context, skinId: String?): RemoteViews {
        val flux = skinId == FluxSkin.id
        val views = RemoteViews(context.packageName, if (flux) R.layout.widget_flux else R.layout.widget_analog)
        val controller = currentController(context)
        val md = controller?.metadata
        val playing = controller?.playbackState?.state == PlaybackState.STATE_PLAYING

        var artPx = 0
        if (md != null) {
            val title = md.getString(MediaMetadata.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }
            views.setTextViewText(R.id.widget_title, title ?: context.getString(R.string.widget_unknown_title))
            views.setTextViewText(R.id.widget_artist, md.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "")
            val art = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: md.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            if (art != null) {
                val label = labelBitmap(art)
                artPx = label.width
                views.setImageViewBitmap(R.id.widget_art, label)
            }
        }

        // The ring spins only while playing; paused shows the same drawable, static.
        views.setViewVisibility(R.id.widget_ring_spin, if (playing) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_ring_static, if (playing) View.GONE else View.VISIBLE)
        views.setImageViewResource(R.id.widget_playpause, if (playing) R.drawable.ic_widget_pause else R.drawable.ic_widget_play)
        views.setContentDescription(
            R.id.widget_playpause,
            context.getString(if (playing) R.string.widget_pause else R.string.widget_play),
        )

        views.setOnClickPendingIntent(R.id.widget_open, openAppIntent(context))
        views.setOnClickPendingIntent(R.id.widget_prev, controlIntent(context, ACTION_PREV))
        views.setOnClickPendingIntent(R.id.widget_playpause, controlIntent(context, ACTION_PLAY_PAUSE))
        views.setOnClickPendingIntent(R.id.widget_next, controlIntent(context, ACTION_NEXT))
        Log.i(TAG, "SpindleWidget.buildViews: skin=${if (flux) "flux" else "analog"} artPx=$artPx playing=$playing")
        return views
    }

    fun currentController(context: Context): MediaController? = try {
        val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val component = ComponentName(context, SpindleNotificationListener::class.java)
        msm.getActiveSessions(component).firstOrNull()
    } catch (e: SecurityException) {
        null
    }

    private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun controlIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, SpindleWidgetProvider::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Center-crops [src] to a circle no larger than [ART_MAX_PX]. */
    private fun labelBitmap(src: Bitmap): Bitmap {
        val side = minOf(src.width, src.height).coerceAtLeast(1)
        val size = minOf(side, ART_MAX_PX)
        val scale = size / side.toFloat()
        val shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(
                Matrix().apply {
                    setTranslate((side - src.width) / 2f, (side - src.height) / 2f)
                    postScale(scale, scale)
                },
            )
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { this.shader = shader }
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val r = size / 2f
        Canvas(output).drawCircle(r, r, r, paint)
        return output
    }
}
