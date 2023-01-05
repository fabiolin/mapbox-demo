// package com.app.mapbox_demo_flutter

// import android.content.Intent
// import android.os.Bundle
// import com.mapbox.maps.logE
// import io.flutter.embedding.android.FlutterActivity
// import io.flutter.plugin.common.MethodChannel

// class MainActivity : FlutterActivity() {

//     private val CHANNEL = "com.app.mapbox_demo_flutter"

//     override fun onCreate(savedInstanceState: Bundle?) {
//         super.onCreate(savedInstanceState)


//         flutterEngine?.dartExecutor?.let {
//             MethodChannel(it.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
//                 if (call.method == "LatLong") {
//                     val myMMap = HashMap<String, Double>()
//                     myMMap.putAll(call.arguments as HashMap<String, Double>)
//                     val intent = Intent(this, TurnByTurnExperienceActivity::class.java)
//                     intent.putExtra("startPointLat", myMMap["startPointLat"])
//                     intent.putExtra("startPointLon", myMMap["startPointLon"])
//                     intent.putExtra("endPointLat", myMMap["endPointLat"])
//                     intent.putExtra("endPointLong", myMMap["endPointLong"])
//                     startActivity(intent)
//                     result.success("success")
//                 }
//             }
//         }
//     }
// }
