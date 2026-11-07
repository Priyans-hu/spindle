package com.markfoundry.spindle.demo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Debug-only entry point for demo mode:
 * adb shell am broadcast -n com.markfoundry.spindle/.demo.DemoReceiver --es cmd start|stop|seekable_on|seekable_off
 */
class DemoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val cmd = intent.getStringExtra("cmd")
        Log.i(TAG, "DemoReceiver: cmd=$cmd")
        when (cmd) {
            "start" -> DemoSession.start(context)
            "stop" -> DemoSession.stop()
            "seekable_on" -> DemoSession.setSeekable(true)
            "seekable_off" -> DemoSession.setSeekable(false)
            else -> Log.w(TAG, "DemoReceiver: unknown cmd=$cmd")
        }
    }

    private companion object {
        const val TAG = "SpindleDemo"
    }
}
