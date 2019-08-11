import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';
import 'package:meta/meta.dart';
import 'dart:io';
import 'dart:typed_data';

// 根据app 链接判断是否安装、打开
class MyNativeUri {
  final Uri uri;
  MyNativeUri({@required this.uri});
  
  Future<bool> hasOpen() async {
    if (Platform.isAndroid) {
      return await FlutterMyNative.hasOpenNativeLinkAndroid(pkgname: uri.host);
    }else if (Platform.isIOS) {
      return await FlutterMyNative.hasOpenNativeLinkIos(url: uri.toString());
    }
    return false;
  }

  Future<bool> open() async {
    return await FlutterMyNative.openNativeLink(url: uri.toString());
  }
}

class FlutterMyNative {
  static const MethodChannel _channel =
      const MethodChannel('cn.cnganen.flutter_my_native');
  static const EventChannel _eventChannel =
  const EventChannel('cn.cnganen.flutter_my_native.qiniu_upload_event');
  static const EventChannel _eventDeeplinkChannel =
  const EventChannel('cn.cnganen.flutter_my_native.deeplink');

  static Future<String> goToHome() async {
    assert(Platform.isAndroid, "回到桌面只支持android");
    final String version = await _channel.invokeMethod('goToHome');
    return version;
  }

  static Future<bool> hasOpenNativeLinkIos({@required String url}) async {
    try {
      final ret = await _channel.invokeMethod("hasOpen", {"uri": url});
      if (ret == "success") {
        return true;
      }
      return false;
    } catch(e) {
      return false;
    }
  }

  static Future<bool> hasOpenNativeLinkAndroid({@required String pkgname}) async {
    try {
      final ret = await _channel.invokeMethod("hasOpen", {"pkgname": pkgname});
      if (ret == "success") {
        return true;
      }
      return false;
    } catch(e) {
      return false;
    }
  }

  static Future<bool> openNativeLink({@required url}) async {
    try {
      final ret = await _channel.invokeMethod("openlink", {"uri": url});
      if (ret == "success") {
        return true;
      }
      return false;
    } catch(e) {
      return false;
    }
  }

  // 避免错误使用时应用try包裹 捕获 FlutterError
  static Future<String> saveFile({Uint8List fileData, String path}) async {
    String filePath = await _channel.invokeMethod(
      'saveFile',
      <String, dynamic>{
        'fileData': fileData,
        "filePath": path,
      },
    );
    debugPrint("saved filePath:" + filePath);
    //process ios return filePath
    if(filePath.startsWith("file://")){
      filePath=filePath.replaceAll("file://", "");
    }
    return  filePath;
  }
}

class Deeplink {
  List<ValueChanged<String>> _eventCallback = List<ValueChanged<String>>();
  static Deeplink _cache;
  factory Deeplink (){
    if (Deeplink._cache == null) {
      Deeplink._cache = Deeplink._();
    }
    return Deeplink._cache;
  }

  Deeplink._(){
    final _onChanged = FlutterMyNative._eventDeeplinkChannel.receiveBroadcastStream();
    _onChanged.listen((data){
      if (data["uri"] != null) {
        _eventCallback.forEach((v) => v(data["uri"] + "#" + data["fragment"]));
      }
    });
  }

  static Future<String> getInitialLink() async {
    final ret = await FlutterMyNative._channel.invokeMethod("getInitialLink");
      if (ret != null) {
        final retMap = Map<String, String>.from(ret);
        if (retMap.containsKey("uri")) {
          return retMap["uri"] + "#" + (retMap["fragment"] ?? "");
        }
      }
      return null;
  }

  addEventCallback(ValueChanged<String> call) {
    _eventCallback.add(call);
  }

  removeEventCallback(ValueChanged<String> call) {
    _eventCallback.remove(call);
  }

  void dispose() {
    _eventCallback = [];
  }
}

class Qiniu {
  static Stream _onChanged;
  static Map<String, StreamController<double>> _cache;
  Qiniu() {
    Qiniu._cache = Map<String, StreamController<double>>();
    if (_onChanged == null) {
      _onChanged = FlutterMyNative._eventChannel.receiveBroadcastStream();
      _onChanged.listen((data){
        if (Qiniu._cache != null && data["key"] != null && Qiniu._cache.containsKey(data["key"])) {
          if (Qiniu._cache[data["key"]].isClosed == false) {
            Qiniu._cache[data["key"]].add(data['percent']);
          }
        }
      });
    }
  }
  List<String> _keys = [];
  Stream<double> addOnChange(String key) {
    Qiniu._cache[key] = StreamController();
    _keys.add(key);
    return Qiniu._cache[key].stream;
  }

  void dispose() {
    _keys.forEach((item){
      Qiniu._cache[item].close();
      Qiniu._cache.remove(item);
    });
  }

  ///上传
  ///
  /// key 保存到七牛的文件名
  Future<bool> uploadByPath(String filepath, String token, String key) async {
    var res = await FlutterMyNative._channel.invokeMethod('upload',
            <String, String>{"filepath": filepath, "token": token, "key": key,  "type": "path"});
    return res;
  }

  Future<bool> uploadByByte(List<int> bytes, String token, String key) async {
    var res = await FlutterMyNative._channel.invokeMethod('upload',{"bytes": bytes, "token": token, "key": key, "type": "bytes"});
    return res;
  }

  /// 取消上传
  static cancelUpload() {
    FlutterMyNative._channel.invokeMethod('cancelUpload');
  }
}
