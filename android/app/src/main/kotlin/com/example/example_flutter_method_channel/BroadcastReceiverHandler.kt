package com.example.example_flutter_method_channel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class BroadcastReceiverHandler : BroadcastReceiver() {

    private lateinit var callback: BroadcastReceiverHandlerListener

    fun setListener(callback: BroadcastReceiverHandlerListener) {
        this.callback = callback
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null && intent.action == "action.REPORT_GAME") {
            val info = intent.getStringExtra("reportGame")
            callback.onReportGame(info)
        }
        if (intent != null && intent.action == "action.INITIAL_ROUTE") {
            val info = intent.getStringExtra("initialRoute")
            callback.onReportInitialRoute(info)
        }
    }
}