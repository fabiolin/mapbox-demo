class MapData {
  String? eta;
  String? latLngJSON;
  String? type;

  MapData({this.eta, this.latLngJSON, this.type});

  MapData.fromJson(Map<String, dynamic> json) {
    eta = json['eta'];
    latLngJSON = json['latLngJSON'];
    type = json['type'];
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = new Map<String, dynamic>();
    data['eta'] = this.eta;
    data['latLngJSON'] = this.latLngJSON;
    data['type'] = this.type;
    return data;
  }
}
