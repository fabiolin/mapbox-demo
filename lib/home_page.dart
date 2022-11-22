import 'dart:convert';

import 'package:geolocator/geolocator.dart';
import 'package:http/http.dart' as http;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'MapData.dart';
import 'main.dart';

class HomePage extends StatefulWidget {
  const HomePage({Key? key}) : super(key: key);

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  //static const platform = MethodChannel('com.app.mapbox_demo_flutter');

  LocationList locationList = LocationList();

  bool isResponseForDestination = true;
  bool isLoading = false;
/*
  Future<void> responseFromNativeCode() async {
    String response = "";
    try {
      final String result = await platform.invokeMethod('helloFromNativeCode');
      response = result;
    } on PlatformException catch (e) {
      response = "Failed to Invoke: '${e.message}'.";
    }
    setState(() {
      _responseFromNativeCode = response;
    });
  }
*/

  Future<void> askPermission() async {
    LocationPermission permission;

    // Test if location services are enabled.
    var serviceEnabled = await Geolocator.isLocationServiceEnabled();

    permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
      if (permission == LocationPermission.denied) {
        // Get.snackbar('', 'Location Permission Denied');
        // Permissions are denied, next time you could try
        // requesting permissions again (this is also where
        // Android's shouldShowRequestPermissionRationale
        // returned true. According to Android guidelines
        // your App should show an explanatory UI now.
        return Future.error('Location permissions are denied');
      }
    }

    if (permission == LocationPermission.deniedForever) {
      // Permissions are denied forever, handle appropriately.
      return Future.error(
          'Location permissions are permanently denied, we cannot request permissions.');
    }
    return;
  }

  @override
  void initState() {
    super.initState();

    askPermission();
  }

  TextEditingController? startcontroller;
  TextEditingController? changeDestinationLngController =
      TextEditingController();
  TextEditingController? changeDestinationLatController =
      TextEditingController();
  TextEditingController? endcontroller;
  final _formKey = GlobalKey<FormState>();

  var changePointLat;
  var changePointLng;
  var startPointLat;
  var startPointLon;
  var endPointLat;
  var endPointLong;

  static const MethodChannel methodChannel =
      MethodChannel('samples.flutter.io/game');
  static const EventChannel eventChannel =
      EventChannel('samples.flutter.io/report');

  String _reportStatus = 'ETA: unknown.';
  String _initialRoute = 'Initial Route Data: unknown.';

  Future<void> postDataToNative(Map data) async {
    try {
      eventChannel.receiveBroadcastStream().listen(_onEvent, onError: _onError);
      await methodChannel.invokeMethod('startNewActivity', data);
    } on PlatformException catch (e) {
      debugPrint("Failed to Invoke: '${e.message}'.");
    }
  }

  void _onEvent(Object? event) {
    var str = event.toString();
    MapData user = MapData.fromJson(jsonDecode(str));
    var time = user.eta;
    setState(() {
      if (user.type == "initial_route") {
        _initialRoute = user.latLngJSON!;
      }
      if (user.type == "eta") {
        _reportStatus = "ETA: $time Min";
      }
    });
    debugPrint(_reportStatus);
  }

  void _onError(Object error) {
    setState(() {
      _reportStatus = 'ETA: unknown.';
    });
  }

  @override
  Widget build(BuildContext context) {
    return Form(
      key: _formKey,
      child: SafeArea(
        child: Scaffold(
          body: Padding(
            padding: const EdgeInsets.all(15.0),
            child: ListView(
              children: [
                TextFormField(
                  controller: startcontroller,
                  // initialValue: 'Avenida T-9',
                  decoration: const InputDecoration(
                      hintText: 'start point', labelText: "Start point"),
                  onChanged: (value) {
                    isResponseForDestination = true;
                    if (value.length > 2) {
                      _onChangeHandler(value);
                    }
                  },
                  autovalidateMode: AutovalidateMode.onUserInteraction,
                  validator: (value) {
                    if (value?.isEmpty ?? false) {
                      return "Field is required";
                    }
                    return null;
                  },
                ),
                const SizedBox(height: 15),
                TextFormField(
                  controller: endcontroller,
                  decoration: const InputDecoration(
                      hintText: 'end point', labelText: "End point"),
                  autovalidateMode: AutovalidateMode.onUserInteraction,
                  onChanged: (value) {
                    isResponseForDestination = false;
                    if (value.length > 2) {
                      _onChangeHandler(value);
                    }
                  },
                  validator: (value) {
                    if (value?.isEmpty ?? false) {
                      return "Field is required";
                    }
                    return null;
                  },
                ),
                const SizedBox(height: 19),
                TextFormField(
                  controller: changeDestinationLatController,
                  // initialValue: 'Avenida T-63',
                  keyboardType: const TextInputType.numberWithOptions(
                      signed: false, decimal: true),
                  decoration: const InputDecoration(
                    hintText: 'Change Destination Latitude',
                    labelText: "Destination Latitude",
                  ),
                  autovalidateMode: AutovalidateMode.onUserInteraction,
                  onChanged: (value) {},
                  /* validator: (value) {
                    if (value?.isEmpty ?? false) {
                      return "Field is required";
                    }
                    return null;
                  },*/
                ),
                const SizedBox(height: 10),
                TextFormField(
                  controller: changeDestinationLngController,
                  keyboardType: const TextInputType.numberWithOptions(
                      signed: false, decimal: true),
                  decoration: const InputDecoration(
                      hintText: 'Change Destination Longitude',
                      labelText: "Destination Longitude"),
                  autovalidateMode: AutovalidateMode.onUserInteraction,
                  onChanged: (value) {},
                  /* validator: (value) {
                    if (value?.isEmpty ?? false) {
                      return "Field is required";
                    }
                    return null;
                  },*/
                ),
                const SizedBox(height: 19),
                Text(_reportStatus),
                const SizedBox(height: 15),
                Text(_initialRoute),
                const SizedBox(height: 15),
                isLoading
                    ? const Center(child: CircularProgressIndicator())
                    : const SizedBox.shrink(),
                const SizedBox(height: 15),
                ListView.builder(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  itemCount: locationList.features?.length ?? 0,
                  itemBuilder: (BuildContext context, int index) {
                    return Column(
                      children: [
                        ListTile(
                          onTap: () {
                            String text =
                                locationList.features?[index].placeName ?? "";
                            if (isResponseForDestination) {
                              startcontroller =
                                  TextEditingController(text: text);
                              startPointLat = locationList
                                  .features?[index].geometry?.coordinates?.last;
                              startPointLon = locationList.features?[index]
                                  .geometry?.coordinates?.first;
                            } else {
                              endcontroller = TextEditingController(text: text);
                              endPointLat = locationList
                                  .features?[index].geometry?.coordinates?.last;
                              endPointLong = locationList.features?[index]
                                  .geometry?.coordinates?.first;
                            }
                            locationList.features = [];
                            setState(() {});
                            FocusManager.instance.primaryFocus?.nextFocus();
                          },
                          leading: const SizedBox(
                            height: double.infinity,
                            child: CircleAvatar(child: Icon(Icons.map)),
                          ),
                          title: Text(locationList.features?[index].text ?? "",
                              style:
                                  const TextStyle(fontWeight: FontWeight.bold)),
                          subtitle: Text(
                              (locationList.features?[index].placeName ?? ""),
                              overflow: TextOverflow.ellipsis),
                        ),
                        const Divider(),
                      ],
                    );
                  },
                ),
              ],
            ),
          ),
          bottomNavigationBar: Padding(
            padding: const EdgeInsets.all(15.0),
            child: ElevatedButton(
              onPressed: () {
                if (_formKey.currentState?.validate() ?? false) {
                  if (changeDestinationLatController!.text.isEmpty) {
                    changePointLat = 0.0;
                  } else {
                    changePointLat =
                        changeDestinationLatController!.text.toString();
                  }

                  if (changeDestinationLngController!.text.isEmpty) {
                    changePointLng = 0.0;
                  } else {
                    changePointLng =
                        changeDestinationLngController!.text.toString();
                  }

                  Map data = {
                    "changePointLat": double.parse(changePointLat.toString()),
                    "changePointLng": double.parse(changePointLng.toString()),
                    "startPointLat": double.parse(startPointLat.toString()),
                    "startPointLon": double.parse(startPointLon.toString()),
                    "endPointLat": double.parse(endPointLat.toString()),
                    "endPointLong": double.parse(endPointLong.toString()),
                  };
                  postDataToNative(data);
                }
              },
              child: const Text('Start Navigator'),
            ),
          ),
        ),
      ),
    );
  }

  _onChangeHandler(String value) async {
    // Get response using Mapbox Search API
    locationList.features = [];
    isLoading = true;
    setState(() {});
    locationList = await getParsedResponseForQuery(value);
    isLoading = false;
    setState(() {});
  }

  Future<LocationList> getParsedResponseForQuery(String value) async {
    // If empty query send blank response
    String query = getValidatedQueryFromQuery(value);
    if (query == '') return LocationList();

    // Else search and then send response
    var response = await getSearchResultsFromQueryUsingMapbox(query);

    LocationList data = LocationList.fromJson(jsonDecode(response.toString()));

    return data;
  }

  String baseUrl = 'https://api.mapbox.com/geocoding/v5/mapbox.places';
  String accessToken = 'YOU-ACCESS-TOKEN-HERE';
  String searchType =
      'place%2Cpostcode%2Caddress%2Ccountry%2Cdistrict%2Cregion%2Clocality%2Cneighborhood%2Cpoi&language=en';
  String searchResultsLimit = '30';
  String proximity = '72.7970373%2C21.195147}';

  // String proximity = '${sharedPreferences.getDouble('longitude')}%2C${sharedPreferences.getDouble('latitude')}';

  Future getSearchResultsFromQueryUsingMapbox(String query) async {
    String url =
        '$baseUrl/$query.json?limit=$searchResultsLimit&types=$searchType&&access_token=$accessToken';
    url = Uri.parse(url).toString();
    print(url);
    try {
      final responseData = await http.get(Uri.parse(url));
      return responseData.body;
    } catch (e) {
      debugPrint(e.toString());
    }
  }

// ----------------------------- Mapbox Search Query -----------------------------
  String getValidatedQueryFromQuery(String query) {
    // Remove whitespaces
    String validatedQuery = query.trim();
    return validatedQuery;
  }
}

class LocationList {
  var type;
  List<dynamic>? query;
  List<Features>? features;
  var attribution;

  LocationList({this.type, this.query, this.features, this.attribution});

  LocationList.fromJson(Map<String, dynamic> json) {
    type = json['type'];
    query = json['query'];
    if (json['features'] != null) {
      features = <Features>[];
      json['features'].forEach((v) {
        features!.add(new Features.fromJson(v));
      });
    }
    attribution = json['attribution'];
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = new Map<String, dynamic>();
    data['type'] = this.type;
    data['query'] = this.query;
    if (this.features != null) {
      data['features'] = this.features!.map((v) => v.toJson()).toList();
    }
    data['attribution'] = this.attribution;
    return data;
  }
}

class Features {
  var id;
  var type;
  List<dynamic>? placeType;
  var relevance;
  Properties? properties;
  var textEn;
  var languageEn;
  var placeNameEn;
  var text;
  var language;
  var placeName;
  List<dynamic>? bbox;
  List<dynamic>? center;
  Geometry? geometry;
  List<Context>? context;

  Features(
      {this.id,
      this.type,
      this.placeType,
      this.relevance,
      this.properties,
      this.textEn,
      this.languageEn,
      this.placeNameEn,
      this.text,
      this.language,
      this.placeName,
      this.bbox,
      this.center,
      this.geometry,
      this.context});

  Features.fromJson(Map<String, dynamic> json) {
    id = json['id'];
    type = json['type'];
    placeType = json['place_type'];
    relevance = json['relevance'];
    properties = json['properties'] != null
        ? new Properties.fromJson(json['properties'])
        : null;
    textEn = json['text_en'];
    languageEn = json['language_en'];
    placeNameEn = json['place_name_en'];
    text = json['text'];
    language = json['language'];
    placeName = json['place_name'];
    bbox = json['bbox'];
    center = json['center'];
    geometry = json['geometry'] != null
        ? new Geometry.fromJson(json['geometry'])
        : null;
    if (json['context'] != null) {
      context = <Context>[];
      json['context'].forEach((v) {
        context!.add(new Context.fromJson(v));
      });
    }
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = new Map<String, dynamic>();
    data['id'] = this.id;
    data['type'] = this.type;
    data['place_type'] = this.placeType;
    data['relevance'] = this.relevance;
    if (this.properties != null) {
      data['properties'] = this.properties!.toJson();
    }
    data['text_en'] = this.textEn;
    data['language_en'] = this.languageEn;
    data['place_name_en'] = this.placeNameEn;
    data['text'] = this.text;
    data['language'] = this.language;
    data['place_name'] = this.placeName;
    data['bbox'] = this.bbox;
    data['center'] = this.center;
    if (this.geometry != null) {
      data['geometry'] = this.geometry!.toJson();
    }
    if (this.context != null) {
      data['context'] = this.context!.map((v) => v.toJson()).toList();
    }
    return data;
  }
}

class Properties {
  var wikidata;
  var accuracy;

  Properties({this.wikidata, this.accuracy});

  Properties.fromJson(Map<String, dynamic> json) {
    wikidata = json['wikidata'];
    accuracy = json['accuracy'];
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = new Map<String, dynamic>();
    data['wikidata'] = this.wikidata;
    data['accuracy'] = this.accuracy;
    return data;
  }
}

class Geometry {
  var type;
  List<dynamic>? coordinates;

  Geometry({this.type, this.coordinates});

  Geometry.fromJson(Map<String, dynamic> json) {
    type = json['type'];
    coordinates = json['coordinates'];
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = new Map<String, dynamic>();
    data['type'] = this.type;
    data['coordinates'] = this.coordinates;
    return data;
  }
}

class Context {
  var id;
  var shortCode;
  var wikidata;
  var textEn;
  var languageEn;
  var text;
  var language;

  Context(
      {this.id,
      this.shortCode,
      this.wikidata,
      this.textEn,
      this.languageEn,
      this.text,
      this.language});

  Context.fromJson(Map<String, dynamic> json) {
    id = json['id'];
    shortCode = json['short_code'];
    wikidata = json['wikidata'];
    textEn = json['text_en'];
    languageEn = json['language_en'];
    text = json['text'];
    language = json['language'];
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = new Map<String, dynamic>();
    data['id'] = this.id;
    data['short_code'] = this.shortCode;
    data['wikidata'] = this.wikidata;
    data['text_en'] = this.textEn;
    data['language_en'] = this.languageEn;
    data['text'] = this.text;
    data['language'] = this.language;
    return data;
  }
}
