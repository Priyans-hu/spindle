package com.markfoundry.spindle.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.media.session.PlaybackState

class SpindleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) =
        SpindleWidget.updateAll(context)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            SpindleWidget.ACTION_PREV ->
                SpindleWidget.currentController(context)?.transportControls?.skipToPrevious()
            SpindleWidget.ACTION_NEXT ->
                SpindleWidget.currentController(context)?.transportControls?.skipToNext()
            SpindleWidget.ACTION_PLAY_PAUSE -> {
                val c = SpindleWidget.currentController(context) ?: return
                if (c.playbackState?.state == PlaybackState.STATE_PLAYING) {
                    c.transportControls.pause()
                } else {
                    c.transportControls.play()
                }
            }
            else -> return
        }
        SpindleWidget.updateAll(context)
    }
}
