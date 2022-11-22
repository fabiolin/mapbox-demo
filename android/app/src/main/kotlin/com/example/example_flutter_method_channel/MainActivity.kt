package com.example.example_flutter_method_channel

import android.content.Intent
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import android.util.Log

class MainActivity : FlutterActivity() {
    private val tag = "MainActivity"
    private var myMMap = HashMap<String, Double>()
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        val eventChannelHandler = EventChannelHandler(context = applicationContext)

        eventChannelHandler.startListening(flutterEngine, CHARGING_CHANNEL, tag)
        MethodChannel(
            flutterEngine.dartExecutor,
            BATTERY_CHANNEL
        ).setMethodCallHandler { call, result ->
            if ((call.method == "startNewActivity")) {
                myMMap.clear()
                myMMap.putAll(call.arguments as HashMap<String, Double>)

                Log.i(tag, myMMap["startPointLat"].toString())

                goHomeActivity()
            } else {
                result.notImplemented()
            }
        }
    }

    companion object {
        private const val BATTERY_CHANNEL = "samples.flutter.io/game"
        private const val CHARGING_CHANNEL = "samples.flutter.io/report"
    }

    private fun goHomeActivity() {
        val intent = Intent(this, TurnByTurnExperienceActivity::class.java)
        intent.putExtra("changePointLat", myMMap["changePointLat"])
        intent.putExtra("changePointLng", myMMap["changePointLng"])
        intent.putExtra("startPointLat", myMMap["startPointLat"])
        intent.putExtra("startPointLon", myMMap["startPointLon"])
        intent.putExtra("endPointLat", myMMap["endPointLat"])
        intent.putExtra("endPointLong", myMMap["endPointLong"])
        Log.i(tag, myMMap["startPointLat"].toString())
        startActivity(intent)
    }
}