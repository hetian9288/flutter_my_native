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
  static Future<String> saveFile({@required Uint8List fileData}) async {
    assert(fileData != null);

    String filePath = await _channel.invokeMethod(
      'saveFile',
      <String, dynamic>{
        'fileData': fileData,
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

class Qiniu {
  static Stream _onChanged;
  static Map<String, StreamController> _cache;
  Qiniu() {
    _cache = Map<String, StreamController>();
    if (_onChanged == null) {
      _onChanged = FlutterMyNative._eventChannel.receiveBroadcastStream();
      _onChanged.listen((data){
        print(data);
        if (_cache != null && data["key"] && _cache.containsKey(data["key"])) {
          if (_cache[data["key"]].isClosed == false) {
            _cache[data["key"]].add(data['percent']);
          }
        }
      });
    }
  }
  List<String> _keys = [];
  Stream addOnChange(String key) {
    _cache[key] = StreamController();
    _keys.add(key);
    return _cache[key].stream;
  }

  void dispose() {
    _keys.forEach((item){
      _cache[item].close();
      _cache.remove(item);
    });
  }

  ///上传
  ///
  /// key 保存到七牛的文件名
  Future<bool> upload(String filepath, String token, String key) async {
    var res = await FlutterMyNative._channel.invokeMethod('upload',
            <String, String>{"filepath": filepath, "token": token, "key": key});
    return res;
  }

  /// 取消上传
  static cancelUpload() {
    FlutterMyNative._channel.invokeMethod('cancelUpload');
  }
}
