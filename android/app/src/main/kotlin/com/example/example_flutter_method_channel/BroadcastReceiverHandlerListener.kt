package com.example.example_flutter_method_channel

abstract class BroadcastReceiverHandlerListener {
    abstract fun onReportGame(detail: String?)
    abstract fun onReportInitialRoute(detail: String?)
}