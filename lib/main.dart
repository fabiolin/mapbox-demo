import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'home_page.dart';

class PlatformChannel extends StatefulWidget {
  const PlatformChannel({Key? key}) : super(key: key);

  @override
  State<PlatformChannel> createState() => _PlatformChannelState();
}

class _PlatformChannelState extends State<PlatformChannel> {
  static const MethodChannel methodChannel =
      MethodChannel('samples.flutter.io/game');
  static const EventChannel eventChannel =
      EventChannel('samples.flutter.io/report');

  String _reportStatus = 'Game status: unknown.';

  Future<void> _startNewActivity() async {
    try {

      Map data = {
        "startPointLat": double.parse("22.7788"),
        "startPointLon": double.parse("87.7788"),
        "endPointLat": double.parse("23.7788"),
        "endPointLong": double.parse("88.7788"),
      };

      eventChannel.receiveBroadcastStream().listen(_onEvent, onError: _onError);
      await methodChannel.invokeMethod('startNewActivity',data);
    } on PlatformException catch (e) {
      debugPrint("Failed to Invoke: '${e.message}'.");
    }
  }

  void _onEvent(Object? event) {
    setState(() {
      _reportStatus =
          "Game status: $event";
    });
    debugPrint(_reportStatus);
  }

  void _onError(Object error) {
    setState(() {
      _reportStatus = 'Game status: unknown.';
    });
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: <Widget>[
          Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              const Text('Open Native Activity'),
              Padding(
                padding: const EdgeInsets.all(16.0),
                child: ElevatedButton(
                  onPressed: _startNewActivity,
                  child: const Text('Start New Activity'),
                ),
              ),
            ],
          ),
          Text(_reportStatus),
        ],
      ),
    );
  }
}

void main() {
  runApp(const MaterialApp(home: HomePage()));
}
